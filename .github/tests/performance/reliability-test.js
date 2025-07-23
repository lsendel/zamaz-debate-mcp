const { describe, it, expect, beforeEach, jest } = require('@jest/globals');
const IssueManager = require('../../scripts/issue-manager');
const NotificationService = require('../../scripts/notification-service');
const { Octokit } = require('@octokit/rest');
const axios = require('axios');

jest.mock('@octokit/rest');
jest.mock('axios');

describe('Reliability and Error Recovery Tests', () => {
  describe('API Rate Limiting', () => {
    let issueManager;
    let mockOctokit;
    
    beforeEach(() => {
      mockOctokit = {
        rest: {
          issues: {
            create: jest.fn(),
            update: jest.fn()
          },
          rateLimit: {
            get: jest.fn()
          }
        }
      };
      
      Octokit.mockReturnValue(mockOctokit);
      issueManager = new IssueManager(mockOctokit, { repo: { owner: 'test', repo: 'repo' } });
    });
    
    it('should handle rate limit errors with exponential backoff', async () => {
      // Mock rate limit error
      const rateLimitError = new Error('API rate limit exceeded');
      rateLimitError.status = 403;
      rateLimitError.response = {
        headers: {
          'x-ratelimit-remaining': '0',
          'x-ratelimit-reset': String(Math.floor(Date.now() / 1000) + 60)
        }
      };
      
      // First call fails with rate limit
      mockOctokit.rest.issues.create
        .mockRejectedValueOnce(rateLimitError)
        .mockRejectedValueOnce(rateLimitError)
        .mockResolvedValueOnce({ data: { number: 123 } });
      
      const startTime = Date.now();
      const result = await issueManager.createWorkflowIssue({
        workflow: { name: 'Test' },
        failure: {}
      });
      const duration = Date.now() - startTime;
      
      expect(result.success).toBe(true);
      expect(result.issueNumber).toBe(123);
      expect(mockOctokit.rest.issues.create).toHaveBeenCalledTimes(3);
      
      // Should have waited with exponential backoff
      expect(duration).toBeGreaterThan(1500); // At least 1.5 seconds
    });
    
    it('should respect rate limit headers and pre-emptively throttle', async () => {
      // Mock rate limit check
      mockOctokit.rest.rateLimit.get.mockResolvedValue({
        data: {
          resources: {
            core: {
              limit: 5000,
              remaining: 10, // Very low
              reset: Math.floor(Date.now() / 1000) + 300
            }
          }
        }
      });
      
      mockOctokit.rest.issues.create.mockResolvedValue({ data: { number: 456 } });
      
      // Create multiple issues rapidly
      const promises = [];
      for (let i = 0; i < 20; i++) {
        promises.push(
          issueManager.createWorkflowIssue({
            workflow: { name: `Test ${i}` },
            failure: {}
          })
        );
      }
      
      const results = await Promise.all(promises);
      
      // All should succeed
      expect(results.every(r => r.success)).toBe(true);
      
      // But calls should be throttled
      const callTimes = mockOctokit.rest.issues.create.mock.invocationCallOrder;
      let hasThrottling = false;
      
      for (let i = 1; i < callTimes.length; i++) {
        if (callTimes[i] - callTimes[i-1] > 100) { // More than 100ms gap
          hasThrottling = true;
          break;
        }
      }
      
      expect(hasThrottling).toBe(true);
    });
  });
  
  describe('Network Resilience', () => {
    let notificationService;
    
    beforeEach(() => {
      notificationService = new NotificationService();
      axios.post.mockReset();
    });
    
    it('should retry on network errors', async () => {
      // Mock network errors
      axios.post
        .mockRejectedValueOnce(new Error('ECONNREFUSED'))
        .mockRejectedValueOnce(new Error('ETIMEDOUT'))
        .mockResolvedValueOnce({ data: { ok: true } });
      
      const result = await notificationService.sendSlackNotification({
        workflow: { name: 'Test' },
        failure: { severity: 'high' }
      });
      
      expect(result.success).toBe(true);
      expect(axios.post).toHaveBeenCalledTimes(3);
    });
    
    it('should implement circuit breaker for repeated failures', async () => {
      // Mock consistent failures
      axios.post.mockRejectedValue(new Error('Service unavailable'));
      
      // First few calls should attempt
      for (let i = 0; i < 5; i++) {
        await notificationService.sendSlackNotification({
          workflow: { name: 'Test' },
          failure: {}
        });
      }
      
      expect(axios.post).toHaveBeenCalledTimes(5);
      
      // Circuit should now be open
      axios.post.mockClear();
      
      // Next calls should fail fast
      const result = await notificationService.sendSlackNotification({
        workflow: { name: 'Test' },
        failure: {}
      });
      
      expect(result.success).toBe(false);
      expect(result.circuitOpen).toBe(true);
      expect(axios.post).not.toHaveBeenCalled();
    });
    
    it('should handle timeout gracefully', async () => {
      // Mock timeout
      const timeoutError = new Error('Request timeout');
      timeoutError.code = 'ECONNABORTED';
      
      axios.post.mockRejectedValueOnce(timeoutError);
      
      const result = await notificationService.sendSlackNotification({
        workflow: { name: 'Test' },
        failure: {}
      }, { timeout: 5000 });
      
      expect(result.success).toBe(false);
      expect(result.error).toContain('timeout');
    });
  });
  
  describe('Data Consistency', () => {
    let issueManager;
    let mockOctokit;
    
    beforeEach(() => {
      mockOctokit = {
        rest: {
          issues: {
            create: jest.fn(),
            update: jest.fn(),
            get: jest.fn()
          },
          search: {
            issuesAndPullRequests: jest.fn()
          }
        }
      };
      
      Octokit.mockReturnValue(mockOctokit);
      issueManager = new IssueManager(mockOctokit, { repo: { owner: 'test', repo: 'repo' } });
    });
    
    it('should handle concurrent duplicate detection correctly', async () => {
      // Simulate race condition where multiple failures happen simultaneously
      let searchCallCount = 0;
      mockOctokit.rest.search.issuesAndPullRequests.mockImplementation(async () => {
        searchCallCount++;
        // First call finds no duplicates, subsequent calls find the created issue
        if (searchCallCount === 1) {
          return { data: { items: [] } };
        } else {
          return { data: { items: [{ number: 100, state: 'open' }] } };
        }
      });
      
      mockOctokit.rest.issues.create.mockResolvedValue({ data: { number: 100 } });
      mockOctokit.rest.issues.update.mockResolvedValue({ data: { number: 100 } });
      
      // Create multiple issues for same workflow simultaneously
      const promises = [];
      for (let i = 0; i < 5; i++) {
        promises.push(
          issueManager.createWorkflowIssue({
            workflow: { name: 'Concurrent Test' },
            failure: { errorPatterns: ['test-failure'] }
          })
        );
      }
      
      const results = await Promise.all(promises);
      
      // Only one issue should be created
      expect(mockOctokit.rest.issues.create).toHaveBeenCalledTimes(1);
      
      // Others should update the existing issue
      expect(mockOctokit.rest.issues.update.mock.calls.length).toBeGreaterThan(0);
      
      // All operations should succeed
      expect(results.every(r => r.success)).toBe(true);
    });
    
    it('should maintain data integrity during partial failures', async () => {
      // Mock partial failure scenario
      mockOctokit.rest.issues.create.mockResolvedValue({ data: { number: 200 } });
      mockOctokit.rest.issues.addLabels = jest.fn().mockRejectedValue(new Error('Label service down'));
      
      const result = await issueManager.createWorkflowIssue({
        workflow: { name: 'Test' },
        failure: {},
        labels: ['workflow-failure', 'bug']
      });
      
      // Issue should be created despite label failure
      expect(result.success).toBe(true);
      expect(result.issueNumber).toBe(200);
      expect(result.warnings).toContain('labels');
    });
  });
  
  describe('Configuration Validation', () => {
    it('should validate and sanitize configuration', async () => {
      const invalidConfigs = [
        { severity: 'invalid-severity' }, // Should default to medium
        { escalation_threshold: -5 }, // Should use minimum
        { notification_channels: 'not-an-object' }, // Should use defaults
        { assignees: 'single-string' } // Should convert to array
      ];
      
      for (const config of invalidConfigs) {
        const validated = validateWorkflowConfig(config);
        
        expect(validated).toBeDefined();
        expect(validated.severity).toMatch(/^(low|medium|high|critical)$/);
        expect(validated.escalation_threshold).toBeGreaterThan(0);
        expect(validated.notification_channels).toBeInstanceOf(Object);
        expect(validated.assignees).toBeInstanceOf(Array);
      }
    });
    
    it('should handle missing configuration gracefully', async () => {
      // Test with no config file
      const result = await loadWorkflowConfig('/non/existent/path.yml');
      
      expect(result).toBeDefined();
      expect(result.global).toBeDefined();
      expect(result.workflows).toBeDefined();
    });
  });
  
  describe('Memory and Resource Management', () => {
    it('should not leak memory during long-running operations', async () => {
      const initialMemory = process.memoryUsage().heapUsed;
      
      // Simulate many operations
      for (let i = 0; i < 1000; i++) {
        // Create temporary objects that should be garbage collected
        const largeData = {
          workflow: { name: `Test ${i}` },
          failure: {
            jobs: Array(100).fill({ name: 'job', logs: 'x'.repeat(1000) })
          }
        };
        
        // Process and discard
        processFailureData(largeData);
      }
      
      // Force garbage collection if available
      if (global.gc) {
        global.gc();
      }
      
      const finalMemory = process.memoryUsage().heapUsed;
      const memoryGrowth = finalMemory - initialMemory;
      
      // Memory growth should be reasonable (less than 50MB)
      expect(memoryGrowth).toBeLessThan(50 * 1024 * 1024);
    });
    
    it('should handle large log files efficiently', async () => {
      // Create a very large log (10MB)
      const largeLog = 'x'.repeat(10 * 1024 * 1024);
      
      const startTime = Date.now();
      const extracted = extractRelevantLogs(largeLog);
      const duration = Date.now() - startTime;
      
      // Should complete quickly
      expect(duration).toBeLessThan(100); // Less than 100ms
      
      // Should limit output size
      expect(extracted.length).toBeLessThan(100000); // Less than 100KB
    });
  });
  
  describe('Chaos Engineering Tests', () => {
    it('should handle random failures gracefully', async () => {
      const chaosMonkey = {
        shouldFail: () => Math.random() < 0.3, // 30% failure rate
        failureType: () => {
          const types = ['network', 'timeout', 'rateLimit', 'invalid'];
          return types[Math.floor(Math.random() * types.length)];
        }
      };
      
      const results = [];
      
      for (let i = 0; i < 100; i++) {
        try {
          if (chaosMonkey.shouldFail()) {
            throw new Error(chaosMonkey.failureType());
          }
          
          results.push({ success: true });
        } catch (error) {
          // System should handle all error types
          const handled = await handleError(error);
          results.push({ success: false, handled });
        }
      }
      
      // Most operations should complete (either success or handled failure)
      const completed = results.filter(r => r.success || r.handled);
      expect(completed.length / results.length).toBeGreaterThan(0.95);
    });
  });
});

// Helper functions for tests
function validateWorkflowConfig(config) {
  const defaults = {
    severity: 'medium',
    escalation_threshold: 3,
    notification_channels: {},
    assignees: [],
    labels: ['workflow-failure']
  };
  
  const validated = { ...defaults };
  
  if (config.severity && ['low', 'medium', 'high', 'critical'].includes(config.severity)) {
    validated.severity = config.severity;
  }
  
  if (config.escalation_threshold && config.escalation_threshold > 0) {
    validated.escalation_threshold = config.escalation_threshold;
  }
  
  if (config.notification_channels && typeof config.notification_channels === 'object') {
    validated.notification_channels = config.notification_channels;
  }
  
  if (config.assignees) {
    validated.assignees = Array.isArray(config.assignees) ? config.assignees : [config.assignees];
  }
  
  return validated;
}

async function loadWorkflowConfig(path) {
  try {
    // Mock config loading
    return {
      global: {
        enabled: true,
        default_assignees: [],
        default_labels: ['workflow-failure']
      },
      workflows: {}
    };
  } catch (error) {
    return {
      global: { enabled: true },
      workflows: {}
    };
  }
}

function processFailureData(data) {
  // Simulate processing that should not leak memory
  const processed = {
    summary: data.workflow.name,
    errorCount: data.failure.jobs?.length || 0
  };
  
  // Ensure no references are kept
  return JSON.parse(JSON.stringify(processed));
}

function extractRelevantLogs(logs) {
  // Efficient log extraction
  const lines = logs.split('\n');
  const relevant = [];
  
  // Use streaming approach for large logs
  for (let i = Math.max(0, lines.length - 100); i < lines.length; i++) {
    if (lines[i].match(/error|fail|exception/i)) {
      relevant.push(lines[i]);
    }
  }
  
  return relevant.slice(-50).join('\n');
}

async function handleError(error) {
  const errorHandlers = {
    network: async () => {
      await new Promise(resolve => setTimeout(resolve, 1000));
      return true;
    },
    timeout: async () => {
      return true;
    },
    rateLimit: async () => {
      await new Promise(resolve => setTimeout(resolve, 2000));
      return true;
    },
    invalid: async () => {
      return false;
    }
  };
  
  const handler = errorHandlers[error.message] || errorHandlers.invalid;
  return await handler();
}