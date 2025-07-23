const core = require('@actions/core');
const https = require('https');
const nodemailer = require('nodemailer');

class NotificationService {
  constructor(config) {
    this.config = config || {};
    this.throttleMap = new Map();
  }

  async sendNotifications(issueData, workflowData, notificationChannels) {
    const notifications = [];

    try {
      // Send to configured channels
      if (notificationChannels.slack) {
        notifications.push(this.sendSlackNotification(
          notificationChannels.slack,
          issueData,
          workflowData
        ));
      }

      if (notificationChannels.email && notificationChannels.email.length > 0) {
        notifications.push(this.sendEmailNotification(
          notificationChannels.email,
          issueData,
          workflowData
        ));
      }

      if (notificationChannels.teams) {
        notifications.push(this.sendTeamsNotification(
          notificationChannels.teams,
          issueData,
          workflowData
        ));
      }

      // GitHub mentions are handled by the issue creation itself
      
      // Wait for all notifications to complete
      const results = await Promise.allSettled(notifications);
      
      // Log results
      results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          core.info(`Notification ${index + 1} sent successfully`);
        } else {
          core.warning(`Notification ${index + 1} failed: ${result.reason}`);
        }
      });

    } catch (error) {
      core.error(`Notification service error: ${error.message}`);
    }
  }

  async sendSlackNotification(webhookUrl, issueData, workflowData) {
    // Check throttling
    if (this.isThrottled('slack', workflowData.workflow.name)) {
      core.info('Slack notification throttled');
      return;
    }

    const message = this.formatSlackMessage(issueData, workflowData);
    
    return new Promise((resolve, reject) => {
      const data = JSON.stringify(message);
      const url = new URL(webhookUrl);
      
      const options = {
        hostname: url.hostname,
        port: 443,
        path: url.pathname,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': data.length
        }
      };

      const req = https.request(options, (res) => {
        let responseData = '';
        
        res.on('data', (chunk) => {
          responseData += chunk;
        });
        
        res.on('end', () => {
          if (res.statusCode === 200) {
            resolve();
          } else {
            reject(new Error(`Slack API returned ${res.statusCode}: ${responseData}`));
          }
        });
      });

      req.on('error', reject);
      req.write(data);
      req.end();
    });
  }

  formatSlackMessage(issueData, workflowData) {
    const severity = workflowData.failure.severity;
    const severityEmoji = {
      critical: '🔴',
      high: '🟠',
      medium: '🟡',
      low: '🟢'
    }[severity] || '⚪';

    const categoryEmoji = {
      'ci-cd': '🔧',
      'security': '🔒',
      'code-quality': '📋',
      'deployment': '🚀',
      'testing': '🧪'
    }[workflowData.failure.category] || '📌';

    return {
      text: `Workflow Failure: ${workflowData.workflow.name}`,
      blocks: [
        {
          type: 'header',
          text: {
            type: 'plain_text',
            text: `${severityEmoji} Workflow Failure Alert`,
            emoji: true
          }
        },
        {
          type: 'section',
          fields: [
            {
              type: 'mrkdwn',
              text: `*Workflow:*\n${workflowData.workflow.name}`
            },
            {
              type: 'mrkdwn',
              text: `*Category:*\n${categoryEmoji} ${workflowData.failure.category}`
            },
            {
              type: 'mrkdwn',
              text: `*Branch:*\n\`${workflowData.workflow.branch}\``
            },
            {
              type: 'mrkdwn',
              text: `*Severity:*\n${severity.toUpperCase()}`
            }
          ]
        },
        {
          type: 'section',
          text: {
            type: 'mrkdwn',
            text: `*Failed Jobs:* ${workflowData.failure.jobs.map(j => j.name).join(', ')}`
          }
        },
        {
          type: 'actions',
          elements: [
            {
              type: 'button',
              text: {
                type: 'plain_text',
                text: 'View Issue',
                emoji: true
              },
              url: issueData.html_url || '#',
              style: 'primary'
            },
            {
              type: 'button',
              text: {
                type: 'plain_text',
                text: 'View Workflow Run',
                emoji: true
              },
              url: workflowData.workflow.url
            }
          ]
        },
        {
          type: 'context',
          elements: [
            {
              type: 'mrkdwn',
              text: `Triggered by @${workflowData.workflow.triggeredBy} • ${new Date(workflowData.failure.timestamp).toLocaleString()}`
            }
          ]
        }
      ]
    };
  }

  async sendEmailNotification(recipients, issueData, workflowData) {
    // Check throttling
    if (this.isThrottled('email', workflowData.workflow.name)) {
      core.info('Email notification throttled');
      return;
    }

    // Configure email transport
    const transporter = nodemailer.createTransport({
      host: process.env.SMTP_HOST || 'smtp.gmail.com',
      port: process.env.SMTP_PORT || 587,
      secure: false,
      auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS
      }
    });

    const emailContent = this.formatEmailContent(issueData, workflowData);

    const mailOptions = {
      from: process.env.SMTP_FROM || 'workflow-notifications@company.com',
      to: recipients.join(', '),
      subject: emailContent.subject,
      html: emailContent.html,
      text: emailContent.text
    };

    try {
      await transporter.sendMail(mailOptions);
      core.info(`Email sent to ${recipients.length} recipients`);
    } catch (error) {
      throw new Error(`Email send failed: ${error.message}`);
    }
  }

  formatEmailContent(issueData, workflowData) {
    const severity = workflowData.failure.severity;
    const severityColor = {
      critical: '#ff0000',
      high: '#ff8800',
      medium: '#ffcc00',
      low: '#00cc00'
    }[severity] || '#888888';

    const subject = `[${severity.toUpperCase()}] Workflow Failure: ${workflowData.workflow.name}`;

    const html = `
      <!DOCTYPE html>
      <html>
      <head>
        <style>
          body { font-family: Arial, sans-serif; line-height: 1.6; }
          .header { background-color: ${severityColor}; color: white; padding: 20px; }
          .content { padding: 20px; }
          .footer { background-color: #f0f0f0; padding: 10px; text-align: center; }
          .button { display: inline-block; padding: 10px 20px; background-color: #0066cc; color: white; text-decoration: none; border-radius: 5px; }
          table { border-collapse: collapse; width: 100%; }
          td, th { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
        </style>
      </head>
      <body>
        <div class="header">
          <h2>Workflow Failure Alert</h2>
          <p>${workflowData.workflow.name} - ${severity.toUpperCase()}</p>
        </div>
        <div class="content">
          <h3>Failure Details</h3>
          <table>
            <tr><td><strong>Workflow:</strong></td><td>${workflowData.workflow.name}</td></tr>
            <tr><td><strong>Branch:</strong></td><td>${workflowData.workflow.branch}</td></tr>
            <tr><td><strong>Commit:</strong></td><td>${workflowData.workflow.commit.sha.substring(0, 7)}</td></tr>
            <tr><td><strong>Triggered By:</strong></td><td>${workflowData.workflow.triggeredBy}</td></tr>
            <tr><td><strong>Category:</strong></td><td>${workflowData.failure.category}</td></tr>
            <tr><td><strong>Failed Jobs:</strong></td><td>${workflowData.failure.jobs.map(j => j.name).join(', ')}</td></tr>
          </table>
          
          <h3>Actions</h3>
          <p>
            <a href="${issueData.html_url || '#'}" class="button">View Issue</a>
            <a href="${workflowData.workflow.url}" class="button">View Workflow Run</a>
          </p>
        </div>
        <div class="footer">
          <p>This is an automated notification from the Workflow Failure Handler</p>
        </div>
      </body>
      </html>
    `;

    const text = `
Workflow Failure Alert

Workflow: ${workflowData.workflow.name}
Severity: ${severity.toUpperCase()}
Branch: ${workflowData.workflow.branch}
Triggered By: ${workflowData.workflow.triggeredBy}
Failed Jobs: ${workflowData.failure.jobs.map(j => j.name).join(', ')}

View Issue: ${issueData.html_url || 'N/A'}
View Workflow Run: ${workflowData.workflow.url}

This is an automated notification from the Workflow Failure Handler
    `;

    return { subject, html, text };
  }

  async sendTeamsNotification(webhookUrl, issueData, workflowData) {
    // Check throttling
    if (this.isThrottled('teams', workflowData.workflow.name)) {
      core.info('Teams notification throttled');
      return;
    }

    const message = this.formatTeamsMessage(issueData, workflowData);
    
    return new Promise((resolve, reject) => {
      const data = JSON.stringify(message);
      const url = new URL(webhookUrl);
      
      const options = {
        hostname: url.hostname,
        port: 443,
        path: url.pathname,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': data.length
        }
      };

      const req = https.request(options, (res) => {
        let responseData = '';
        
        res.on('data', (chunk) => {
          responseData += chunk;
        });
        
        res.on('end', () => {
          if (res.statusCode === 200) {
            resolve();
          } else {
            reject(new Error(`Teams API returned ${res.statusCode}: ${responseData}`));
          }
        });
      });

      req.on('error', reject);
      req.write(data);
      req.end();
    });
  }

  formatTeamsMessage(issueData, workflowData) {
    const severity = workflowData.failure.severity;
    const themeColor = {
      critical: 'ff0000',
      high: 'ff8800',
      medium: 'ffcc00',
      low: '00cc00'
    }[severity] || '888888';

    return {
      '@type': 'MessageCard',
      '@context': 'https://schema.org/extensions',
      themeColor: themeColor,
      summary: `Workflow Failure: ${workflowData.workflow.name}`,
      sections: [
        {
          activityTitle: `Workflow Failure: ${workflowData.workflow.name}`,
          activitySubtitle: `Severity: ${severity.toUpperCase()}`,
          facts: [
            {
              name: 'Workflow',
              value: workflowData.workflow.name
            },
            {
              name: 'Branch',
              value: workflowData.workflow.branch
            },
            {
              name: 'Category',
              value: workflowData.failure.category
            },
            {
              name: 'Failed Jobs',
              value: workflowData.failure.jobs.map(j => j.name).join(', ')
            },
            {
              name: 'Triggered By',
              value: workflowData.workflow.triggeredBy
            }
          ]
        }
      ],
      potentialAction: [
        {
          '@type': 'OpenUri',
          name: 'View Issue',
          targets: [
            {
              os: 'default',
              uri: issueData.html_url || '#'
            }
          ]
        },
        {
          '@type': 'OpenUri',
          name: 'View Workflow Run',
          targets: [
            {
              os: 'default',
              uri: workflowData.workflow.url
            }
          ]
        }
      ]
    };
  }

  isThrottled(channel, workflowName) {
    const key = `${channel}:${workflowName}`;
    const now = Date.now();
    const throttleWindow = 300000; // 5 minutes
    
    const lastSent = this.throttleMap.get(key);
    if (lastSent && (now - lastSent) < throttleWindow) {
      return true;
    }
    
    this.throttleMap.set(key, now);
    return false;
  }

  async handleEscalation(workflowData, failureCount) {
    const escalationThreshold = workflowData.escalationThreshold || 3;
    
    if (failureCount >= escalationThreshold) {
      core.warning(`Workflow has failed ${failureCount} times - escalating notifications`);
      
      // Send escalation notifications
      const escalationChannels = {
        slack: process.env.ESCALATION_SLACK_WEBHOOK,
        email: process.env.ESCALATION_EMAIL?.split(',') || [],
        teams: process.env.ESCALATION_TEAMS_WEBHOOK
      };
      
      // Create escalation message
      const escalationData = {
        ...workflowData,
        escalation: true,
        failureCount: failureCount
      };
      
      await this.sendNotifications(
        { title: `ESCALATION: ${workflowData.workflow.name}` },
        escalationData,
        escalationChannels
      );
    }
  }
}

// Export for use in other scripts
module.exports = { NotificationService };

// CLI interface for testing
if (require.main === module) {
  async function main() {
    try {
      const action = process.argv[2];
      const dataFile = process.argv[3];
      
      if (!action || !dataFile) {
        console.error('Usage: node notification-service.js <test-slack|test-email|test-teams> <data-file.json>');
        process.exit(1);
      }
      
      const data = JSON.parse(require('fs').readFileSync(dataFile, 'utf8'));
      const service = new NotificationService();
      
      switch (action) {
        case 'test-slack':
          await service.sendSlackNotification(
            process.env.SLACK_WEBHOOK,
            { html_url: 'https://github.com/test/test/issues/1' },
            data
          );
          console.log('Slack notification sent');
          break;
          
        case 'test-email':
          await service.sendEmailNotification(
            ['test@example.com'],
            { html_url: 'https://github.com/test/test/issues/1' },
            data
          );
          console.log('Email notification sent');
          break;
          
        case 'test-teams':
          await service.sendTeamsNotification(
            process.env.TEAMS_WEBHOOK,
            { html_url: 'https://github.com/test/test/issues/1' },
            data
          );
          console.log('Teams notification sent');
          break;
          
        default:
          console.error('Unknown action:', action);
          process.exit(1);
      }
      
    } catch (error) {
      console.error('Notification test failed:', error.message);
      process.exit(1);
    }
  }
  
  main();
}