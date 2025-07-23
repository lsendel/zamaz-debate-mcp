const { describe, it, expect, jest, beforeEach } = require('@jest/globals');
const { IssueManager } = require('../../scripts/issue-manager');

describe('IssueManager', () => {
  let issueManager;
  let mockOctokit;
  
  beforeEach(() => {
    mockOctokit = {
      rest: {
        issues: {
          create: jest.fn(),
          update: jest.fn(),
          createComment: jest.fn(),
          addLabels: jest.fn(),
          get: jest.fn()
        },
        search: {
          issuesAndPullRequests: jest.fn()
        }
      }
    };
    
    issueManager = new IssueManager('test-token', 'test-owner', 'test-repo');
    issueManager.octokit = mockOctokit;
  });
  
  describe('analyzeFailure', () => {
    it('should generate issue data correctly', async () => {
      const workflowData = {
        workflow: {
          name: 'Test Workflow',
          branch: 'main',
          commit: { sha: 'abc123' }
        },
        failure: {
          category: 'ci-cd',
          jobs: [{ name: 'Build' }]
        },
        labels: ['bug', 'ci-cd'],
        assignees: ['user1', 'user2']
      };
      
      const result = await issueManager.analyzeFailure(workflowData);
      
      expect(result.title).toContain('Test Workflow');
      expect(result.labels).toEqual(['bug', 'ci-cd']);
      expect(result.assignees).toEqual(['user1', 'user2']);
      expect(result.metadata.workflowName).toBe('Test Workflow');
    });
  });
  
  describe('checkDuplicateIssue', () => {
    it('should find existing open issues', async () => {
      const existingIssue = {
        number: 123,
        title: 'Test Workflow failed',
        body: 'workflow:Test Workflow\nfailure-type:ci-cd'
      };
      
      mockOctokit.rest.search.issuesAndPullRequests.mockResolvedValue({
        data: {
          total_count: 1,
          items: [existingIssue]
        }
      });
      
      const result = await issueManager.checkDuplicateIssue('Test Workflow', 'ci-cd');
      
      expect(result).toEqual(existingIssue);
      expect(mockOctokit.rest.search.issuesAndPullRequests).toHaveBeenCalledWith({
        q: expect.stringContaining('workflow:Test Workflow'),
        sort: 'created',
        order: 'desc',
        per_page: 10
      });
    });
    
    it('should return null when no duplicates found', async () => {
      mockOctokit.rest.search.issuesAndPullRequests.mockResolvedValue({
        data: {
          total_count: 0,
          items: []
        }
      });
      
      const result = await issueManager.checkDuplicateIssue('Test Workflow', 'ci-cd');
      
      expect(result).toBeNull();
    });
    
    it('should handle search errors gracefully', async () => {
      mockOctokit.rest.search.issuesAndPullRequests.mockRejectedValue(
        new Error('Search failed')
      );
      
      const result = await issueManager.checkDuplicateIssue('Test Workflow', 'ci-cd');
      
      expect(result).toBeNull();
    });
  });
  
  describe('createWorkflowIssue', () => {
    it('should create issue successfully', async () => {
      const issueData = {
        title: 'Test Issue',
        body: 'Test body',
        labels: ['bug'],
        assignees: ['user1']
      };
      
      mockOctokit.rest.issues.create.mockResolvedValue({
        data: {
          number: 123,
          html_url: 'https://github.com/test/test/issues/123'
        }
      });
      
      const result = await issueManager.createWorkflowIssue(issueData);
      
      expect(result.number).toBe(123);
      expect(mockOctokit.rest.issues.create).toHaveBeenCalledWith({
        owner: 'test-owner',
        repo: 'test-repo',
        title: 'Test Issue',
        body: 'Test body',
        labels: ['bug'],
        assignees: ['user1']
      });
    });
    
    it('should retry on rate limit errors', async () => {
      issueManager.sleep = jest.fn().mockResolvedValue();
      
      mockOctokit.rest.issues.create
        .mockRejectedValueOnce({ status: 403 })
        .mockResolvedValueOnce({
          data: { number: 123 }
        });
      
      await issueManager.createWorkflowIssue({ title: 'Test' });
      
      expect(mockOctokit.rest.issues.create).toHaveBeenCalledTimes(2);
      expect(issueManager.sleep).toHaveBeenCalledWith(60000);
    });
    
    it('should retry on server errors with exponential backoff', async () => {
      issueManager.sleep = jest.fn().mockResolvedValue();
      
      mockOctokit.rest.issues.create
        .mockRejectedValueOnce({ status: 500 })
        .mockRejectedValueOnce({ status: 502 })
        .mockResolvedValueOnce({
          data: { number: 123 }
        });
      
      await issueManager.createWorkflowIssue({ title: 'Test' });
      
      expect(mockOctokit.rest.issues.create).toHaveBeenCalledTimes(3);
      expect(issueManager.sleep).toHaveBeenCalledWith(2000); // 2^1 * 1000
      expect(issueManager.sleep).toHaveBeenCalledWith(4000); // 2^2 * 1000
    });
    
    it('should throw after max retries', async () => {
      issueManager.sleep = jest.fn().mockResolvedValue();
      
      mockOctokit.rest.issues.create.mockRejectedValue({ status: 500 });
      
      await expect(
        issueManager.createWorkflowIssue({ title: 'Test' })
      ).rejects.toThrow('Failed to create issue after 3 attempts');
      
      expect(mockOctokit.rest.issues.create).toHaveBeenCalledTimes(3);
    });
  });
  
  describe('updateExistingIssue', () => {
    it('should update issue with comment and labels', async () => {
      const updateData = {
        workflow: { name: 'Test Workflow' },
        failure: {
          timestamp: '2024-01-15T10:00:00Z',
          jobs: [{ name: 'Build' }]
        },
        labels: ['new-label']
      };
      
      mockOctokit.rest.issues.get.mockResolvedValue({
        data: {
          body: '<!-- failure-count:2 -->\n<!-- last-failure:old -->'
        }
      });
      
      mockOctokit.rest.issues.createComment.mockResolvedValue({});
      mockOctokit.rest.issues.addLabels.mockResolvedValue({});
      mockOctokit.rest.issues.update.mockResolvedValue({});
      
      await issueManager.updateExistingIssue(123, updateData);
      
      expect(mockOctokit.rest.issues.createComment).toHaveBeenCalled();
      expect(mockOctokit.rest.issues.addLabels).toHaveBeenCalledWith({
        owner: 'test-owner',
        repo: 'test-repo',
        issue_number: 123,
        labels: ['new-label']
      });
      expect(mockOctokit.rest.issues.update).toHaveBeenCalled();
    });
    
    it('should increment failure count correctly', async () => {
      mockOctokit.rest.issues.get.mockResolvedValue({
        data: {
          body: 'Test body\n<!-- failure-count:5 -->\n<!-- last-failure:old -->'
        }
      });
      
      mockOctokit.rest.issues.createComment.mockResolvedValue({});
      mockOctokit.rest.issues.update.mockResolvedValue({});
      
      await issueManager.updateExistingIssue(123, {
        workflow: { name: 'Test' },
        failure: { timestamp: '2024-01-15T10:00:00Z', jobs: [] }
      });
      
      const updateCall = mockOctokit.rest.issues.update.mock.calls[0][0];
      expect(updateCall.body).toContain('<!-- failure-count:6 -->');
      expect(updateCall.body).toContain('<!-- last-failure:2024-01-15T10:00:00Z -->');
    });
  });
  
  describe('generateTroubleshootingSteps', () => {
    it('should generate category-specific steps', () => {
      const categories = ['ci-cd', 'security', 'code-quality', 'deployment', 'testing'];
      
      categories.forEach(category => {
        const steps = issueManager.generateTroubleshootingSteps(category, []);
        expect(steps).toBeTruthy();
        expect(steps.split('\n').length).toBeGreaterThan(3);
      });
    });
    
    it('should add pattern-specific steps', () => {
      const steps = issueManager.generateTroubleshootingSteps(
        'general',
        ['test-failure', 'build-failure', 'linting-failure']
      );
      
      expect(steps).toContain('verbose output');
      expect(steps).toContain('build cache');
      expect(steps).toContain('auto-fix');
    });
  });
  
  describe('Issue Title Generation', () => {
    it('should generate appropriate titles', () => {
      const workflowData = {
        workflow: {
          name: 'CI/CD Pipeline',
          branch: 'feature/new-feature'
        },
        failure: {
          severity: 'critical'
        }
      };
      
      const title = issueManager.generateIssueTitle(workflowData);
      
      expect(title).toBe('[CRITICAL] CI/CD Pipeline workflow failed on feature/new-feature');
    });
  });
});