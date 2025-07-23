const { describe, it, expect, beforeAll, afterAll, jest } = require('@jest/globals');
const { exec } = require('child_process');
const { promisify } = require('util');
const fs = require('fs').promises;
const path = require('path');
const yaml = require('js-yaml');

const execAsync = promisify(exec);

// Mock GitHub API responses
const mockGitHubAPI = require('../mocks/github-api');
const { createMockWorkflowRun, createMockJobs, createMockIssues } = require('../mocks/data-generators');

describe('Workflow Failure Handler E2E Tests', () => {
  let testWorkflowPath;
  let originalEnv;
  
  beforeAll(async () => {
    // Save original environment
    originalEnv = { ...process.env };
    
    // Set up test environment
    process.env.GITHUB_TOKEN = 'test-token';
    process.env.GITHUB_REPOSITORY = 'test-owner/test-repo';
    process.env.GITHUB_RUN_ID = '12345';
    process.env.GITHUB_WORKFLOW = 'Test Workflow';
    
    // Create test workflow directory
    testWorkflowPath = path.join(__dirname, '../../test-workflows');
    await fs.mkdir(testWorkflowPath, { recursive: true });
  });
  
  afterAll(async () => {
    // Restore environment
    process.env = originalEnv;
    
    // Clean up test artifacts
    if (testWorkflowPath) {
      await fs.rm(testWorkflowPath, { recursive: true, force: true });
    }
  });
  
  describe('Issue Creation Flow', () => {
    it('should create an issue when a workflow fails', async () => {
      // Create a test workflow that simulates failure
      const testWorkflow = `
name: Test CI Pipeline
on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Simulate build failure
        run: exit 1
        
  handle-failure:
    if: failure()
    needs: [build]
    uses: ../.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "Test CI Pipeline"
      severity: "medium"
`;
      
      await fs.writeFile(
        path.join(testWorkflowPath, 'test-ci.yml'),
        testWorkflow
      );
      
      // Mock the GitHub API calls
      const mockWorkflowRun = createMockWorkflowRun({
        conclusion: 'failure',
        name: 'Test CI Pipeline'
      });
      
      const mockJobs = createMockJobs([
        { name: 'build', conclusion: 'failure' }
      ]);
      
      mockGitHubAPI.mockWorkflowRun(mockWorkflowRun);
      mockGitHubAPI.mockJobs(mockJobs);
      mockGitHubAPI.mockCreateIssue();
      
      // Simulate workflow execution
      const result = await simulateWorkflowExecution('test-ci.yml');
      
      // Verify issue was created
      expect(mockGitHubAPI.createIssueCalls).toHaveLength(1);
      const createdIssue = mockGitHubAPI.createIssueCalls[0];
      
      expect(createdIssue.title).toContain('Test CI Pipeline');
      expect(createdIssue.labels).toContain('workflow-failure');
      expect(createdIssue.body).toContain('build');
      expect(createdIssue.body).toContain('exit 1');
    });
    
    it('should update existing issue for duplicate failures', async () => {
      // Mock existing issues
      const existingIssue = {
        number: 42,
        title: '[Workflow Failure] Test CI Pipeline - build-failure',
        state: 'open',
        labels: ['workflow-failure', 'bug']
      };
      
      mockGitHubAPI.mockSearchIssues([existingIssue]);
      mockGitHubAPI.mockUpdateIssue();
      
      // Simulate second failure
      const result = await simulateWorkflowExecution('test-ci.yml');
      
      // Verify issue was updated, not created
      expect(mockGitHubAPI.createIssueCalls).toHaveLength(0);
      expect(mockGitHubAPI.updateIssueCalls).toHaveLength(1);
      
      const updateCall = mockGitHubAPI.updateIssueCalls[0];
      expect(updateCall.issue_number).toBe(42);
      expect(updateCall.comment).toContain('occurred again');
    });
    
    it('should reopen closed issues when workflow fails again', async () => {
      // Mock closed issue
      const closedIssue = {
        number: 99,
        title: '[Workflow Failure] Test CI Pipeline - build-failure',
        state: 'closed',
        labels: ['workflow-failure', 'bug']
      };
      
      mockGitHubAPI.mockSearchIssues([closedIssue]);
      mockGitHubAPI.mockReopenIssue();
      
      // Simulate failure after fix
      const result = await simulateWorkflowExecution('test-ci.yml');
      
      // Verify issue was reopened
      expect(mockGitHubAPI.reopenIssueCalls).toHaveLength(1);
      expect(mockGitHubAPI.reopenIssueCalls[0].issue_number).toBe(99);
    });
  });
  
  describe('Notification Delivery', () => {
    beforeAll(() => {
      process.env.SLACK_WEBHOOK = 'https://hooks.slack.com/test';
      process.env.SMTP_HOST = 'smtp.test.com';
      process.env.SMTP_USER = 'test@example.com';
      process.env.SMTP_PASS = 'test-pass';
    });
    
    it('should send Slack notification for critical failures', async () => {
      const mockSlackAPI = jest.fn().mockResolvedValue({ ok: true });
      
      // Create deployment workflow
      const deployWorkflow = `
name: Production Deployment
on: [push]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to production
        run: exit 1
        
  handle-failure:
    if: failure()
    needs: [deploy]
    uses: ../.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "Production Deployment"
      severity: "critical"
      notify-slack: true
`;
      
      await fs.writeFile(
        path.join(testWorkflowPath, 'deploy.yml'),
        deployWorkflow
      );
      
      // Mock critical failure
      const mockWorkflowRun = createMockWorkflowRun({
        conclusion: 'failure',
        name: 'Production Deployment'
      });
      
      mockGitHubAPI.mockWorkflowRun(mockWorkflowRun);
      mockGitHubAPI.mockNotificationAPIs({ slack: mockSlackAPI });
      
      // Execute workflow
      await simulateWorkflowExecution('deploy.yml');
      
      // Verify Slack notification sent
      expect(mockSlackAPI).toHaveBeenCalled();
      const slackPayload = mockSlackAPI.mock.calls[0][0];
      
      expect(slackPayload.text).toContain('Critical');
      expect(slackPayload.text).toContain('Production Deployment');
      expect(slackPayload.attachments).toBeDefined();
    });
    
    it('should send email notification for security failures', async () => {
      const mockEmailAPI = jest.fn().mockResolvedValue({ accepted: ['test@example.com'] });
      
      // Create security scan workflow
      const securityWorkflow = `
name: Security Scanning
on: [schedule]

jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - name: Run security scan
        run: exit 1
        
  handle-failure:
    if: failure()
    needs: [scan]
    uses: ../.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "Security Scanning"
      severity: "high"
      notify-email: true
`;
      
      await fs.writeFile(
        path.join(testWorkflowPath, 'security.yml'),
        securityWorkflow
      );
      
      mockGitHubAPI.mockNotificationAPIs({ email: mockEmailAPI });
      
      // Execute workflow
      await simulateWorkflowExecution('security.yml');
      
      // Verify email sent
      expect(mockEmailAPI).toHaveBeenCalled();
      const emailData = mockEmailAPI.mock.calls[0][0];
      
      expect(emailData.to).toContain('security@company.com');
      expect(emailData.subject).toContain('Security Scanning');
      expect(emailData.html).toContain('High Severity');
    });
    
    it('should throttle notifications to prevent spam', async () => {
      const mockSlackAPI = jest.fn().mockResolvedValue({ ok: true });
      mockGitHubAPI.mockNotificationAPIs({ slack: mockSlackAPI });
      
      // Simulate multiple rapid failures
      for (let i = 0; i < 5; i++) {
        await simulateWorkflowExecution('test-ci.yml', {
          notifySlack: true
        });
      }
      
      // Should throttle after initial notifications
      expect(mockSlackAPI.mock.calls.length).toBeLessThan(5);
      expect(mockSlackAPI.mock.calls.length).toBeGreaterThan(0);
    });
  });
  
  describe('Template Rendering', () => {
    it('should use correct template based on workflow type', async () => {
      const testCases = [
        { 
          workflowName: 'CI/CD Pipeline',
          expectedTemplate: 'ci-cd',
          expectedContent: ['Build Status', 'Failed Steps', 'Commit Information']
        },
        {
          workflowName: 'Security Scanning',
          expectedTemplate: 'security',
          expectedContent: ['Security Vulnerabilities', 'Compliance', 'Risk Assessment']
        },
        {
          workflowName: 'Code Quality Check',
          expectedTemplate: 'linting',
          expectedContent: ['Linting Errors', 'Code Style', 'Technical Debt']
        },
        {
          workflowName: 'Production Deploy',
          expectedTemplate: 'deployment',
          expectedContent: ['Deployment Status', 'Rollback', 'Environment']
        }
      ];
      
      for (const testCase of testCases) {
        mockGitHubAPI.reset();
        mockGitHubAPI.mockCreateIssue();
        
        await simulateWorkflowExecution('test.yml', {
          workflowName: testCase.workflowName
        });
        
        const createdIssue = mockGitHubAPI.createIssueCalls[0];
        
        // Verify correct template was used
        for (const expectedText of testCase.expectedContent) {
          expect(createdIssue.body).toContain(expectedText);
        }
      }
    });
    
    it('should include failure analysis in issue body', async () => {
      // Mock complex failure with patterns
      const mockJobs = createMockJobs([
        {
          name: 'test',
          conclusion: 'failure',
          steps: [
            {
              name: 'Run Tests',
              conclusion: 'failure',
              logs: `
FAIL src/components/Button.test.js
  Ï Button : should render correctly
  
  TypeError: Cannot read property 'props' of undefined
    at Button.render (src/components/Button.js:15:20)
    at TestRenderer.render (node_modules/react-test-renderer/lib/ReactTestRenderer.js:120:37)
`
            }
          ]
        }
      ]);
      
      mockGitHubAPI.mockJobs(mockJobs);
      mockGitHubAPI.mockCreateIssue();
      
      await simulateWorkflowExecution('test.yml');
      
      const createdIssue = mockGitHubAPI.createIssueCalls[0];
      
      // Verify failure analysis included
      expect(createdIssue.body).toContain('Likely Root Cause');
      expect(createdIssue.body).toContain('test-failure');
      expect(createdIssue.body).toContain('TypeError');
      expect(createdIssue.body).toContain('Button.test.js');
    });
  });
  
  describe('Configuration Validation', () => {
    it('should validate workflow configuration', async () => {
      // Test invalid configuration
      const invalidConfig = {
        workflows: {
          'Test Workflow': {
            severity: 'invalid-severity',
            assignees: 'not-an-array',
            labels: 123 // Should be array
          }
        }
      };
      
      const configPath = path.join(testWorkflowPath, 'invalid-config.yml');
      await fs.writeFile(configPath, yaml.dump(invalidConfig));
      
      // Attempt to use invalid config
      const result = await simulateWorkflowExecution('test.yml', {
        configPath
      });
      
      expect(result.error).toBeDefined();
      expect(result.error).toContain('configuration');
    });
    
    it('should apply workflow-specific overrides', async () => {
      // Create custom config
      const customConfig = {
        workflows: {
          'Custom Workflow': {
            severity: 'critical',
            assignees: ['custom-team'],
            labels: ['custom-label', 'urgent'],
            template: 'custom',
            escalation_threshold: 1
          }
        }
      };
      
      const configPath = path.join(testWorkflowPath, 'custom-config.yml');
      await fs.writeFile(configPath, yaml.dump(customConfig));
      
      mockGitHubAPI.mockCreateIssue();
      
      await simulateWorkflowExecution('test.yml', {
        workflowName: 'Custom Workflow',
        configPath
      });
      
      const createdIssue = mockGitHubAPI.createIssueCalls[0];
      
      expect(createdIssue.assignees).toContain('custom-team');
      expect(createdIssue.labels).toContain('custom-label');
      expect(createdIssue.labels).toContain('urgent');
    });
  });
  
  describe('Error Recovery', () => {
    it('should handle API rate limiting gracefully', async () => {
      // Mock rate limit error
      mockGitHubAPI.mockRateLimit();
      
      const startTime = Date.now();
      await simulateWorkflowExecution('test.yml');
      const duration = Date.now() - startTime;
      
      // Should have retried with backoff
      expect(duration).toBeGreaterThan(1000); // At least 1 second delay
      expect(mockGitHubAPI.apiCalls.length).toBeGreaterThan(1); // Multiple attempts
    });
    
    it('should continue execution when non-critical steps fail', async () => {
      // Mock partial failures
      mockGitHubAPI.mockSearchIssues([]); // Success
      mockGitHubAPI.mockCreateIssue(); // Success
      mockGitHubAPI.mockNotificationError('slack'); // Fail
      
      const result = await simulateWorkflowExecution('test.yml', {
        notifySlack: true
      });
      
      // Issue should still be created despite notification failure
      expect(mockGitHubAPI.createIssueCalls).toHaveLength(1);
      expect(result.warnings).toContain('notification');
    });
  });
});

// Helper function to simulate workflow execution
async function simulateWorkflowExecution(workflowFile, options = {}) {
  const workflowPath = path.join(testWorkflowPath, workflowFile);
  
  // Set up environment for workflow execution
  const env = {
    ...process.env,
    GITHUB_WORKFLOW: options.workflowName || 'Test Workflow',
    GITHUB_EVENT_NAME: options.eventName || 'push',
    GITHUB_REF: options.ref || 'refs/heads/main',
    GITHUB_SHA: options.sha || 'abc123',
    GITHUB_ACTOR: options.actor || 'test-user'
  };
  
  if (options.configPath) {
    env.WORKFLOW_CONFIG_PATH = options.configPath;
  }
  
  // Execute the workflow handler
  try {
    const { stdout, stderr } = await execAsync(
      `node ${path.join(__dirname, '../../../scripts/test-workflow-handler.js')}`,
      { env, cwd: path.dirname(workflowPath) }
    );
    
    return {
      stdout,
      stderr,
      success: true,
      warnings: extractWarnings(stdout + stderr)
    };
  } catch (error) {
    return {
      error: error.message,
      stdout: error.stdout,
      stderr: error.stderr,
      success: false
    };
  }
}

function extractWarnings(output) {
  const warnings = [];
  const lines = output.split('\n');
  
  for (const line of lines) {
    if (line.toLowerCase().includes('warning') || line.includes(' ')) {
      warnings.push(line);
    }
  }
  
  return warnings;
}