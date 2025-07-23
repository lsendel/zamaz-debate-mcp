const { describe, it, expect, beforeEach, jest } = require('@jest/globals');
const IssueManager = require('../../scripts/issue-manager');
const { Octokit } = require('@octokit/rest');

// Mock Octokit
jest.mock('@octokit/rest');

describe('Duplicate Issue Detection Integration Tests', () => {
  let issueManager;
  let mockOctokit;
  let mockContext;
  
  beforeEach(() => {
    // Reset mocks
    jest.clearAllMocks();
    
    // Create mock Octokit instance
    mockOctokit = {
      rest: {
        issues: {
          listForRepo: jest.fn(),
          create: jest.fn(),
          update: jest.fn(),
          createComment: jest.fn(),
          addLabels: jest.fn()
        },
        search: {
          issuesAndPullRequests: jest.fn()
        }
      }
    };
    
    // Mock context
    mockContext = {
      repo: {
        owner: 'test-owner',
        repo: 'test-repo'
      }
    };
    
    Octokit.mockReturnValue(mockOctokit);
    
    // Create issue manager instance
    issueManager = new IssueManager(mockOctokit, mockContext);
  });
  
  describe('Duplicate Detection Logic', () => {
    it('should detect duplicate by workflow name and failure type', async () => {
      // Mock existing issues
      mockOctokit.rest.search.issuesAndPullRequests.mockResolvedValue({
        data: {
          items: [
            {
              number: 123,
              title: '[Workflow Failure] CI Pipeline - build-failure',
              state: 'open',
              labels: [{ name: 'workflow-failure' }, { name: 'bug' }],
              body: 'Previous failure details...'
            }
          ]
        }
      });
      
      // Check for duplicate
      const duplicate = await issueManager.checkDuplicateIssue('CI Pipeline', 'build-failure');
      
      expect(duplicate).toBeTruthy();
      expect(duplicate.number).toBe(123);
      expect(duplicate.state).toBe('open');
      
      // Verify search query
      expect(mockOctokit.rest.search.issuesAndPullRequests).toHaveBeenCalledWith({
        q: expect.stringContaining('CI Pipeline'),
        sort: 'created',
        order: 'desc',
        per_page: 10
      });
    });
    
    it('should not consider closed issues older than 7 days as duplicates', async () => {
      const eightDaysAgo = new Date();
      eightDaysAgo.setDate(eightDaysAgo.getDate() - 8);
      
      mockOctokit.rest.search.issuesAndPullRequests.mockResolvedValue({
        data: {
          items: [
            {
              number: 456,
              title: '[Workflow Failure] CI Pipeline - build-failure',
              state: 'closed',
              closed_at: eightDaysAgo.toISOString(),
              labels: [{ name: 'workflow-failure' }]
            }
          ]
        }
      });
      
      const duplicate = await issueManager.checkDuplicateIssue('CI Pipeline', 'build-failure');
      
      expect(duplicate).toBeFalsy();
    });
    
    it('should consider recently closed issues as duplicates', async () => {
      const twoDaysAgo = new Date();
      twoDaysAgo.setDate(twoDaysAgo.getDate() - 2);
      
      mockOctokit.rest.search.issuesAndPullRequests.mockResolvedValue({
        data: {
          items: [
            {
              number: 789,
              title: '[Workflow Failure] CI Pipeline - build-failure',
              state: 'closed',
              closed_at: twoDaysAgo.toISOString(),
              labels: [{ name: 'workflow-failure' }]
            }
          ]
        }
      });
      
      const duplicate = await issueManager.checkDuplicateIssue('CI Pipeline', 'build-failure');
      
      expect(duplicate).toBeTruthy();
      expect(duplicate.number).toBe(789);
      expect(duplicate.shouldReopen).toBe(true);
    });
  });
  
  describe('Issue Update Flow', () => {
    it('should update existing issue with new failure information', async () => {
      const existingIssue = {
        number: 100,
        title: '[Workflow Failure] Deploy - deployment-failure',
        state: 'open',
        body: '## Workflow Failure Details\n\nFailure Count: 2\n\nLast Failure: 2024-01-15'
      };
      
      mockOctokit.rest.issues.createComment.mockResolvedValue({
        data: { id: 1001 }
      });
      
      mockOctokit.rest.issues.update.mockResolvedValue({
        data: { ...existingIssue, body: 'updated body' }
      });
      
      const updateData = {
        issueNumber: 100,
        failureCount: 3,
        newFailure: {
          timestamp: '2024-01-16T10:00:00Z',
          runId: 98765,
          runUrl: 'https://github.com/test/runs/98765',
          commit: 'def456',
          errorSummary: 'Connection timeout to production server'
        }
      };
      
      await issueManager.updateExistingIssue(100, updateData);
      
      // Verify comment was added
      expect(mockOctokit.rest.issues.createComment).toHaveBeenCalledWith({
        owner: 'test-owner',
        repo: 'test-repo',
        issue_number: 100,
        body: expect.stringContaining('occurred again')
      });
      
      const commentBody = mockOctokit.rest.issues.createComment.mock.calls[0][0].body;
      expect(commentBody).toContain('98765');
      expect(commentBody).toContain('def456');
      expect(commentBody).toContain('Connection timeout');
      
      // Verify issue body was updated
      expect(mockOctokit.rest.issues.update).toHaveBeenCalledWith({
        owner: 'test-owner',
        repo: 'test-repo',
        issue_number: 100,
        body: expect.stringContaining('Failure Count: 3')
      });
    });
    
    it('should reopen closed issues with escalation', async () => {
      mockOctokit.rest.issues.update.mockResolvedValue({
        data: { number: 200, state: 'open' }
      });
      
      mockOctokit.rest.issues.addLabels.mockResolvedValue({
        data: []
      });
      
      await issueManager.reopenIssue(200, {
        escalate: true,
        previousSeverity: 'medium'
      });
      
      // Verify issue was reopened
      expect(mockOctokit.rest.issues.update).toHaveBeenCalledWith({
        owner: 'test-owner',
        repo: 'test-repo',
        issue_number: 200,
        state: 'open',
        body: expect.stringContaining('REOPENED')
      });
      
      // Verify escalation label was added
      expect(mockOctokit.rest.issues.addLabels).toHaveBeenCalledWith({
        owner: 'test-owner',
        repo: 'test-repo',
        issue_number: 200,
        labels: ['escalated', 'high-priority']
      });
    });
  });
  
  describe('Failure Pattern Tracking', () => {
    it('should track failure patterns across multiple occurrences', async () => {
      // First failure
      const issueData1 = {
        workflow: { name: 'Test Suite' },
        failure: {
          errorPatterns: ['test-failure', 'timeout-error'],
          analysis: {
            likelyRootCause: 'timeout-error',
            commonPatterns: { 'timeout-error': 1, 'test-failure': 1 }
          }
        }
      };
      
      mockOctokit.rest.issues.create.mockResolvedValue({
        data: { number: 300, body: 'First failure' }
      });
      
      await issueManager.createWorkflowIssue(issueData1);
      
      // Second failure with similar pattern
      const issueData2 = {
        workflow: { name: 'Test Suite' },
        failure: {
          errorPatterns: ['test-failure', 'timeout-error', 'network-error'],
          analysis: {
            likelyRootCause: 'timeout-error',
            commonPatterns: { 'timeout-error': 2, 'test-failure': 1, 'network-error': 1 }
          }
        }
      };
      
      // Mock finding the existing issue
      mockOctokit.rest.search.issuesAndPullRequests.mockResolvedValue({
        data: {
          items: [{
            number: 300,
            title: '[Workflow Failure] Test Suite - timeout-error',
            state: 'open',
            body: expect.stringContaining('Pattern History')
          }]
        }
      });
      
      await issueManager.updateExistingIssue(300, {
        newFailure: issueData2.failure,
        failureCount: 2
      });
      
      // Verify pattern tracking in update
      const updateCall = mockOctokit.rest.issues.update.mock.calls[0][0];
      expect(updateCall.body).toContain('timeout-error (2 occurrences)');
      expect(updateCall.body).toContain('Persistent Issue');
    });
  });
  
  describe('Notification Integration', () => {
    it('should trigger notifications based on escalation rules', async () => {
      const notificationService = {
        sendEscalationNotification: jest.fn()
      };
      
      issueManager.setNotificationService(notificationService);
      
      // Simulate multiple failures hitting escalation threshold
      const failureData = {
        workflow: { name: 'Critical Deploy' },
        failure: {
          severity: 'critical',
          errorPatterns: ['deployment-failure']
        },
        context: {
          previousFailures: 2 // This will be the 3rd failure
        }
      };
      
      mockOctokit.rest.issues.create.mockResolvedValue({
        data: { number: 400 }
      });
      
      await issueManager.createWorkflowIssue(failureData, {
        escalationThreshold: 3
      });
      
      // Verify escalation notification was triggered
      expect(notificationService.sendEscalationNotification).toHaveBeenCalledWith({
        issueNumber: 400,
        workflowName: 'Critical Deploy',
        failureCount: 3,
        severity: 'critical'
      });
    });
  });
  
  describe('Performance Tests', () => {
    it('should handle high volume of concurrent issue operations', async () => {
      // Mock successful responses
      mockOctokit.rest.search.issuesAndPullRequests.mockResolvedValue({
        data: { items: [] }
      });
      
      mockOctokit.rest.issues.create.mockResolvedValue({
        data: { number: 500 }
      });
      
      // Create multiple issues concurrently
      const promises = [];
      for (let i = 0; i < 20; i++) {
        promises.push(
          issueManager.createWorkflowIssue({
            workflow: { name: `Workflow ${i}` },
            failure: {
              errorPatterns: ['test-failure'],
              severity: 'medium'
            }
          })
        );
      }
      
      const startTime = Date.now();
      const results = await Promise.all(promises);
      const duration = Date.now() - startTime;
      
      // All should succeed
      expect(results).toHaveLength(20);
      expect(results.every(r => r.success)).toBe(true);
      
      // Should complete reasonably quickly (with rate limiting)
      expect(duration).toBeLessThan(10000); // Less than 10 seconds
      
      // Verify rate limiting was applied
      const callTimings = mockOctokit.rest.issues.create.mock.calls.map(
        call => call.timestamp
      );
      
      // Check that calls were spaced out
      for (let i = 1; i < callTimings.length; i++) {
        const gap = callTimings[i] - callTimings[i - 1];
        expect(gap).toBeGreaterThanOrEqual(50); // At least 50ms between calls
      }
    });
  });
});

// Additional test for search optimization
describe('Search Query Optimization', () => {
  let issueManager;
  let mockOctokit;
  
  beforeEach(() => {
    mockOctokit = {
      rest: {
        search: {
          issuesAndPullRequests: jest.fn()
        }
      }
    };
    
    issueManager = new IssueManager(mockOctokit, {
      repo: { owner: 'test', repo: 'repo' }
    });
  });
  
  it('should use optimized search queries for different scenarios', async () => {
    const testCases = [
      {
        workflowName: 'CI/CD Pipeline',
        failureType: 'build-failure',
        expectedQuery: 'is:issue label:workflow-failure "CI/CD Pipeline" "build-failure" in:title,body repo:test/repo'
      },
      {
        workflowName: 'Security Scan',
        failureType: 'security-failure',
        expectedQuery: 'is:issue label:workflow-failure,security "Security Scan" "security-failure" in:title,body repo:test/repo'
      },
      {
        workflowName: 'Deploy-Prod',
        failureType: 'deployment-failure',
        expectedQuery: 'is:issue label:workflow-failure,critical "Deploy-Prod" "deployment-failure" in:title,body repo:test/repo'
      }
    ];
    
    for (const testCase of testCases) {
      mockOctokit.rest.search.issuesAndPullRequests.mockResolvedValue({
        data: { items: [] }
      });
      
      await issueManager.checkDuplicateIssue(
        testCase.workflowName,
        testCase.failureType
      );
      
      const searchCall = mockOctokit.rest.search.issuesAndPullRequests.mock.calls[0][0];
      
      // Verify optimized query structure
      expect(searchCall.q).toContain('is:issue');
      expect(searchCall.q).toContain('label:workflow-failure');
      expect(searchCall.q).toContain(testCase.workflowName);
      expect(searchCall.q).toContain(testCase.failureType);
      expect(searchCall.sort).toBe('created');
      expect(searchCall.order).toBe('desc');
      
      jest.clearAllMocks();
    }
  });
});