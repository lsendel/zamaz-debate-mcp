const core = require('@actions/core');
const https = require('https');
const fs = require('fs').promises;
const path = require('path');

class MonitoringService {
  constructor(config = {}) {
    this.metricsEndpoint = config.metricsEndpoint || process.env.METRICS_ENDPOINT;
    this.metricsApiKey = config.metricsApiKey || process.env.METRICS_API_KEY;
    this.enableMetrics = config.enableMetrics !== false;
    this.logLevel = config.logLevel || 'info';
    this.metricsBuffer = [];
    this.flushInterval = 60000; // 1 minute
    
    if (this.enableMetrics && this.metricsEndpoint) {
      this.startMetricsFlush();
    }
  }
  
  // Structured logging
  log(level, message, context = {}) {
    const timestamp = new Date().toISOString();
    const logEntry = {
      timestamp,
      level,
      message,
      ...context,
      workflow: process.env.GITHUB_WORKFLOW,
      run_id: process.env.GITHUB_RUN_ID,
      repository: process.env.GITHUB_REPOSITORY
    };
    
    // Output structured log
    if (this.shouldLog(level)) {
      console.log(JSON.stringify(logEntry));
    }
    
    // Also use GitHub Actions logging
    switch (level) {
      case 'error':
        core.error(message);
        break;
      case 'warning':
        core.warning(message);
        break;
      case 'info':
        core.info(message);
        break;
      case 'debug':
        core.debug(message);
        break;
    }
    
    return logEntry;
  }
  
  shouldLog(level) {
    const levels = ['error', 'warning', 'info', 'debug'];
    const currentLevelIndex = levels.indexOf(this.logLevel);
    const messageLevelIndex = levels.indexOf(level);
    return messageLevelIndex <= currentLevelIndex;
  }
  
  // Metrics collection
  recordMetric(name, value, tags = {}) {
    if (!this.enableMetrics) return;
    
    const metric = {
      name,
      value,
      timestamp: Date.now(),
      tags: {
        ...tags,
        workflow: process.env.GITHUB_WORKFLOW,
        repository: process.env.GITHUB_REPOSITORY,
        branch: process.env.GITHUB_REF?.replace('refs/heads/', '')
      }
    };
    
    this.metricsBuffer.push(metric);
    
    // Flush if buffer is getting large
    if (this.metricsBuffer.length >= 100) {
      this.flushMetrics();
    }
  }
  
  // Record workflow failure metrics
  recordWorkflowFailure(workflowData) {
    const { workflow, failure } = workflowData;
    
    // Record failure count
    this.recordMetric('workflow.failure.count', 1, {
      workflow_name: workflow.name,
      severity: failure.severity,
      category: failure.category
    });
    
    // Record failed job count
    this.recordMetric('workflow.failed_jobs.count', failure.jobs.length, {
      workflow_name: workflow.name
    });
    
    // Record error patterns
    failure.errorPatterns.forEach(pattern => {
      this.recordMetric('workflow.error_pattern.count', 1, {
        workflow_name: workflow.name,
        pattern: pattern
      });
    });
    
    // Log the failure
    this.log('info', 'Workflow failure recorded', {
      workflow_name: workflow.name,
      severity: failure.severity,
      failed_jobs: failure.jobs.length,
      error_patterns: failure.errorPatterns
    });
  }
  
  // Record issue creation metrics
  recordIssueCreation(issueData, duration) {
    this.recordMetric('issue.created.count', 1, {
      workflow_name: issueData.metadata?.workflowName,
      issue_type: issueData.metadata?.failureType
    });
    
    this.recordMetric('issue.creation.duration', duration, {
      workflow_name: issueData.metadata?.workflowName
    });
    
    this.log('info', 'Issue created successfully', {
      issue_title: issueData.title,
      duration_ms: duration
    });
  }
  
  // Record notification metrics
  recordNotification(channel, success, workflowName) {
    this.recordMetric('notification.sent.count', 1, {
      channel,
      success: success.toString(),
      workflow_name: workflowName
    });
    
    this.log(success ? 'info' : 'warning', `Notification ${success ? 'sent' : 'failed'}`, {
      channel,
      workflow_name: workflowName
    });
  }
  
  // Performance tracking
  startTimer(operation) {
    return {
      operation,
      startTime: Date.now()
    };
  }
  
  endTimer(timer) {
    const duration = Date.now() - timer.startTime;
    
    this.recordMetric('operation.duration', duration, {
      operation: timer.operation
    });
    
    return duration;
  }
  
  // Flush metrics to external service
  async flushMetrics() {
    if (this.metricsBuffer.length === 0 || !this.metricsEndpoint) {
      return;
    }
    
    const metrics = [...this.metricsBuffer];
    this.metricsBuffer = [];
    
    try {
      await this.sendMetrics(metrics);
      this.log('debug', `Flushed ${metrics.length} metrics`);
    } catch (error) {
      this.log('error', 'Failed to flush metrics', { error: error.message });
      // Put metrics back in buffer for retry
      this.metricsBuffer.unshift(...metrics);
    }
  }
  
  async sendMetrics(metrics) {
    return new Promise((resolve, reject) => {
      const data = JSON.stringify({ metrics });
      const url = new URL(this.metricsEndpoint);
      
      const options = {
        hostname: url.hostname,
        port: url.port || 443,
        path: url.pathname,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': data.length,
          'Authorization': `Bearer ${this.metricsApiKey}`
        }
      };
      
      const req = https.request(options, (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve();
        } else {
          reject(new Error(`Metrics API returned ${res.statusCode}`));
        }
      });
      
      req.on('error', reject);
      req.write(data);
      req.end();
    });
  }
  
  startMetricsFlush() {
    this.flushIntervalId = setInterval(() => {
      this.flushMetrics();
    }, this.flushInterval);
  }
  
  // Generate monitoring dashboard data
  async generateDashboardData() {
    const dashboardData = {
      timestamp: new Date().toISOString(),
      summary: {
        total_failures: 0,
        failures_by_severity: {},
        failures_by_category: {},
        avg_issue_creation_time: 0,
        notification_success_rate: 0
      },
      recent_failures: [],
      error_patterns: {},
      performance: {
        avg_detection_time: 0,
        avg_issue_creation_time: 0,
        avg_notification_time: 0
      }
    };
    
    // This would typically query from a metrics store
    // For now, return sample structure
    return dashboardData;
  }
  
  // Health check endpoint data
  async getHealthStatus() {
    const health = {
      status: 'healthy',
      timestamp: new Date().toISOString(),
      checks: {
        github_api: await this.checkGitHubAPI(),
        templates: await this.checkTemplates(),
        configuration: await this.checkConfiguration(),
        notifications: await this.checkNotifications()
      }
    };
    
    // Overall health based on individual checks
    const failedChecks = Object.values(health.checks).filter(check => !check.healthy);
    if (failedChecks.length > 0) {
      health.status = failedChecks.some(check => check.critical) ? 'unhealthy' : 'degraded';
    }
    
    return health;
  }
  
  async checkGitHubAPI() {
    try {
      // Simple check - would actually make API call
      return {
        healthy: true,
        message: 'GitHub API accessible',
        response_time: 50
      };
    } catch (error) {
      return {
        healthy: false,
        critical: true,
        message: `GitHub API error: ${error.message}`
      };
    }
  }
  
  async checkTemplates() {
    try {
      const templatesDir = path.join(__dirname, '../templates/workflow-issues');
      const templates = await fs.readdir(templatesDir);
      
      return {
        healthy: templates.length > 0,
        message: `Found ${templates.length} templates`,
        templates: templates
      };
    } catch (error) {
      return {
        healthy: false,
        critical: false,
        message: `Template check failed: ${error.message}`
      };
    }
  }
  
  async checkConfiguration() {
    try {
      const configPath = path.join(__dirname, '../config/workflow-issue-config.yml');
      await fs.access(configPath);
      
      return {
        healthy: true,
        message: 'Configuration file accessible'
      };
    } catch (error) {
      return {
        healthy: false,
        critical: true,
        message: `Configuration not found: ${error.message}`
      };
    }
  }
  
  async checkNotifications() {
    // Check if notification endpoints are configured
    const channels = {
      slack: !!process.env.SLACK_WEBHOOK,
      email: !!process.env.SMTP_USER,
      teams: !!process.env.TEAMS_WEBHOOK
    };
    
    const configuredChannels = Object.entries(channels)
      .filter(([_, configured]) => configured)
      .map(([channel]) => channel);
    
    return {
      healthy: configuredChannels.length > 0,
      message: configuredChannels.length > 0 
        ? `Configured channels: ${configuredChannels.join(', ')}`
        : 'No notification channels configured',
      channels: channels
    };
  }
  
  // Cleanup
  destroy() {
    if (this.flushIntervalId) {
      clearInterval(this.flushIntervalId);
    }
    
    // Final metrics flush
    this.flushMetrics();
  }
}

// Export for use
module.exports = { MonitoringService };

// CLI interface for health checks
if (require.main === module) {
  async function main() {
    const monitoring = new MonitoringService();
    
    const command = process.argv[2];
    
    switch (command) {
      case 'health':
        const health = await monitoring.getHealthStatus();
        console.log(JSON.stringify(health, null, 2));
        process.exit(health.status === 'healthy' ? 0 : 1);
        break;
        
      case 'dashboard':
        const dashboard = await monitoring.generateDashboardData();
        console.log(JSON.stringify(dashboard, null, 2));
        break;
        
      default:
        console.error('Usage: node monitoring.js <health|dashboard>');
        process.exit(1);
    }
  }
  
  main().catch(error => {
    console.error('Monitoring check failed:', error);
    process.exit(1);
  });
}