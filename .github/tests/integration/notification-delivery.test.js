const { describe, it, expect, beforeEach, afterEach, jest } = require('@jest/globals');
const NotificationService = require('../../scripts/notification-service');
const axios = require('axios');
const nodemailer = require('nodemailer');

// Mock external dependencies
jest.mock('axios');
jest.mock('nodemailer');

describe('Notification Delivery Integration Tests', () => {
  let notificationService;
  let mockTransporter;
  let originalEnv;
  
  beforeEach(() => {
    // Save and set up environment
    originalEnv = { ...process.env };
    process.env.SLACK_WEBHOOK = 'https://hooks.slack.com/test-webhook';
    process.env.TEAMS_WEBHOOK = 'https://outlook.office.com/webhook/test';
    process.env.SMTP_HOST = 'smtp.test.com';
    process.env.SMTP_PORT = '587';
    process.env.SMTP_USER = 'test@example.com';
    process.env.SMTP_PASS = 'test-password';
    
    // Mock email transporter
    mockTransporter = {
      sendMail: jest.fn().mockResolvedValue({ messageId: 'test-123' })
    };
    nodemailer.createTransport.mockReturnValue(mockTransporter);
    
    // Reset axios mocks
    axios.post.mockReset();
    
    // Create service instance
    notificationService = new NotificationService();
  });
  
  afterEach(() => {
    process.env = originalEnv;
    jest.clearAllMocks();
  });
  
  describe('Slack Notifications', () => {
    it('should send formatted Slack notification for workflow failure', async () => {
      axios.post.mockResolvedValue({ data: { ok: true } });
      
      const notificationData = {
        workflow: {
          name: 'Production Deploy',
          url: 'https://github.com/org/repo/actions/runs/12345',
          branch: 'main',
          triggeredBy: 'john.doe'
        },
        failure: {
          severity: 'critical',
          category: 'deployment',
          jobs: [
            {
              name: 'deploy-prod',
              errorSummary: {
                primaryErrors: ['Connection timeout to prod server']
              }
            }
          ]
        },
        issue: {
          number: 42,
          url: 'https://github.com/org/repo/issues/42'
        }
      };
      
      await notificationService.sendSlackNotification(notificationData);
      
      expect(axios.post).toHaveBeenCalledWith(
        'https://hooks.slack.com/test-webhook',
        expect.objectContaining({
          attachments: expect.arrayContaining([
            expect.objectContaining({
              color: 'danger',
              title: expect.stringContaining('Production Deploy'),
              fields: expect.arrayContaining([
                expect.objectContaining({
                  title: 'Severity',
                  value: 'Critical'
                }),
                expect.objectContaining({
                  title: 'Branch',
                  value: 'main'
                })
              ])
            })
          ])
        })
      );
      
      // Verify critical severity formatting
      const payload = axios.post.mock.calls[0][1];
      expect(payload.text).toContain('=¨');
      expect(payload.text).toContain('Critical');
    });
    
    it('should include actionable buttons in Slack message', async () => {
      axios.post.mockResolvedValue({ data: { ok: true } });
      
      await notificationService.sendSlackNotification({
        workflow: { name: 'Test', url: 'https://example.com/run' },
        issue: { number: 1, url: 'https://example.com/issue' }
      });
      
      const payload = axios.post.mock.calls[0][1];
      const actions = payload.attachments[0].actions;
      
      expect(actions).toHaveLength(2);
      expect(actions[0]).toMatchObject({
        type: 'button',
        text: 'View Issue',
        url: 'https://example.com/issue'
      });
      expect(actions[1]).toMatchObject({
        type: 'button',
        text: 'View Workflow Run',
        url: 'https://example.com/run'
      });
    });
    
    it('should handle Slack webhook errors gracefully', async () => {
      axios.post.mockRejectedValue(new Error('Webhook failed'));
      
      const result = await notificationService.sendSlackNotification({
        workflow: { name: 'Test' }
      });
      
      expect(result.success).toBe(false);
      expect(result.error).toContain('Webhook failed');
      
      // Should not throw
      expect(async () => {
        await notificationService.sendSlackNotification({});
      }).not.toThrow();
    });
  });
  
  describe('Email Notifications', () => {
    it('should send HTML email with workflow failure details', async () => {
      const notificationData = {
        workflow: {
          name: 'Security Scan',
          url: 'https://github.com/org/repo/actions/runs/67890',
          branch: 'develop',
          commit: {
            sha: 'abc123',
            message: 'Update dependencies',
            author: 'jane.doe'
          }
        },
        failure: {
          severity: 'high',
          category: 'security',
          analysis: {
            likelyRootCause: 'security-vulnerability',
            suggestedActions: [
              'Update vulnerable dependencies',
              'Run security audit locally'
            ]
          }
        },
        recipients: ['security@company.com', 'devops@company.com']
      };
      
      await notificationService.sendEmailNotification(notificationData);
      
      expect(mockTransporter.sendMail).toHaveBeenCalledWith({
        from: 'test@example.com',
        to: 'security@company.com, devops@company.com',
        subject: expect.stringContaining('Security Scan'),
        html: expect.stringContaining('Security Scan'),
        text: expect.any(String)
      });
      
      const emailContent = mockTransporter.sendMail.mock.calls[0][0];
      
      // Verify HTML content
      expect(emailContent.html).toContain('High Severity');
      expect(emailContent.html).toContain('Update dependencies');
      expect(emailContent.html).toContain('abc123');
      expect(emailContent.html).toContain('Update vulnerable dependencies');
      
      // Verify styling for high severity
      expect(emailContent.html).toContain('background-color: #ff9800'); // Orange for high
    });
    
    it('should use appropriate email templates for different failure types', async () => {
      const testCases = [
        {
          category: 'deployment',
          severity: 'critical',
          expectedSubject: '=¨ Critical Deployment Failure',
          expectedColor: '#f44336' // Red
        },
        {
          category: 'security',
          severity: 'high',
          expectedSubject: '  High Severity Security Issue',
          expectedColor: '#ff9800' // Orange
        },
        {
          category: 'testing',
          severity: 'medium',
          expectedSubject: 'Test Failure',
          expectedColor: '#2196f3' // Blue
        }
      ];
      
      for (const testCase of testCases) {
        mockTransporter.sendMail.mockClear();
        
        await notificationService.sendEmailNotification({
          workflow: { name: 'Test Workflow' },
          failure: {
            category: testCase.category,
            severity: testCase.severity
          },
          recipients: ['test@example.com']
        });
        
        const emailCall = mockTransporter.sendMail.mock.calls[0][0];
        
        expect(emailCall.subject).toContain(testCase.expectedSubject);
        expect(emailCall.html).toContain(testCase.expectedColor);
      }
    });
  });
  
  describe('Microsoft Teams Notifications', () => {
    it('should send Teams adaptive card for workflow failure', async () => {
      axios.post.mockResolvedValue({ data: { success: true } });
      
      const notificationData = {
        workflow: {
          name: 'Nightly Build',
          url: 'https://github.com/org/repo/actions/runs/11111',
          branch: 'main'
        },
        failure: {
          severity: 'medium',
          jobs: [
            { name: 'build', errorSummary: { primaryErrors: ['Compilation failed'] } },
            { name: 'test', errorSummary: { primaryErrors: ['5 tests failed'] } }
          ]
        }
      };
      
      await notificationService.sendTeamsNotification(notificationData);
      
      expect(axios.post).toHaveBeenCalledWith(
        'https://outlook.office.com/webhook/test',
        expect.objectContaining({
          '@type': 'MessageCard',
          '@context': 'http://schema.org/extensions',
          themeColor: expect.any(String),
          summary: expect.stringContaining('Nightly Build'),
          sections: expect.arrayContaining([
            expect.objectContaining({
              facts: expect.arrayContaining([
                expect.objectContaining({
                  name: 'Workflow',
                  value: 'Nightly Build'
                }),
                expect.objectContaining({
                  name: 'Failed Jobs',
                  value: '2'
                })
              ])
            })
          ])
        })
      );
      
      // Verify theme color matches severity
      const payload = axios.post.mock.calls[0][1];
      expect(payload.themeColor).toBe('FF9800'); // Orange for medium
    });
    
    it('should include action buttons in Teams card', async () => {
      axios.post.mockResolvedValue({ data: { success: true } });
      
      await notificationService.sendTeamsNotification({
        workflow: { name: 'Test', url: 'https://example.com/run' },
        issue: { number: 99, url: 'https://example.com/issue/99' }
      });
      
      const payload = axios.post.mock.calls[0][1];
      const actions = payload.potentialAction;
      
      expect(actions).toHaveLength(2);
      expect(actions[0]).toMatchObject({
        '@type': 'OpenUri',
        name: 'View Issue #99',
        targets: [{ uri: 'https://example.com/issue/99' }]
      });
    });
  });
  
  describe('Notification Throttling', () => {
    it('should throttle notifications per workflow', async () => {
      axios.post.mockResolvedValue({ data: { ok: true } });
      
      const baseNotification = {
        workflow: { name: 'Flaky Test Suite' },
        failure: { severity: 'low' }
      };
      
      // Send multiple notifications rapidly
      const results = [];
      for (let i = 0; i < 5; i++) {
        results.push(
          await notificationService.sendSlackNotification(baseNotification)
        );
      }
      
      // First should succeed, others should be throttled
      expect(results[0].success).toBe(true);
      expect(results[0].throttled).toBeFalsy();
      
      for (let i = 1; i < 5; i++) {
        expect(results[i].success).toBe(true);
        expect(results[i].throttled).toBe(true);
      }
      
      // Only one actual notification sent
      expect(axios.post).toHaveBeenCalledTimes(1);
    });
    
    it('should respect throttle window configuration', async () => {
      // Configure 1 minute throttle window
      notificationService = new NotificationService({
        throttleMinutes: 1
      });
      
      axios.post.mockResolvedValue({ data: { ok: true } });
      
      const notification = {
        workflow: { name: 'Test' },
        failure: { severity: 'medium' }
      };
      
      // First notification
      await notificationService.sendSlackNotification(notification);
      expect(axios.post).toHaveBeenCalledTimes(1);
      
      // Immediate retry - should be throttled
      const result2 = await notificationService.sendSlackNotification(notification);
      expect(result2.throttled).toBe(true);
      expect(axios.post).toHaveBeenCalledTimes(1);
      
      // Simulate time passing (mock timer)
      jest.advanceTimersByTime(61000); // 61 seconds
      
      // Should allow notification after throttle window
      const result3 = await notificationService.sendSlackNotification(notification);
      expect(result3.throttled).toBeFalsy();
      expect(axios.post).toHaveBeenCalledTimes(2);
    });
  });
  
  describe('Escalation Notifications', () => {
    it('should send escalated notifications with higher priority', async () => {
      axios.post.mockResolvedValue({ data: { ok: true } });
      
      const escalationData = {
        workflow: { name: 'Critical Service' },
        failure: {
          severity: 'critical',
          previousFailures: 5
        },
        escalation: {
          level: 2,
          reason: 'Repeated failures exceeding threshold',
          additionalRecipients: ['manager@company.com', 'oncall@company.com']
        }
      };
      
      await notificationService.sendEscalationNotification(escalationData);
      
      // Should send to all channels
      expect(axios.post).toHaveBeenCalledTimes(2); // Slack + Teams
      expect(mockTransporter.sendMail).toHaveBeenCalled();
      
      // Verify escalation formatting
      const slackPayload = axios.post.mock.calls[0][1];
      expect(slackPayload.text).toContain('ESCALATED');
      expect(slackPayload.attachments[0].color).toBe('danger');
      
      // Verify additional recipients in email
      const emailCall = mockTransporter.sendMail.mock.calls[0][0];
      expect(emailCall.to).toContain('manager@company.com');
      expect(emailCall.subject).toContain('ESCALATED');
    });
  });
  
  describe('Batch Notifications', () => {
    it('should batch multiple failures into digest notification', async () => {
      axios.post.mockResolvedValue({ data: { ok: true } });
      
      // Enable batch mode
      notificationService = new NotificationService({
        batchMode: true,
        batchWindowMinutes: 5
      });
      
      // Queue multiple failures
      const failures = [
        { workflow: { name: 'CI Pipeline' }, failure: { severity: 'medium' } },
        { workflow: { name: 'Security Scan' }, failure: { severity: 'high' } },
        { workflow: { name: 'Deploy Staging' }, failure: { severity: 'low' } }
      ];
      
      for (const failure of failures) {
        await notificationService.queueNotification(failure);
      }
      
      // Trigger batch send
      await notificationService.sendBatchNotifications();
      
      expect(axios.post).toHaveBeenCalledTimes(1);
      
      const payload = axios.post.mock.calls[0][1];
      expect(payload.text).toContain('3 workflow failures');
      expect(payload.attachments).toHaveLength(3);
      
      // Verify failures are ordered by severity
      expect(payload.attachments[0].title).toContain('Security Scan');
      expect(payload.attachments[1].title).toContain('CI Pipeline');
      expect(payload.attachments[2].title).toContain('Deploy Staging');
    });
  });
});