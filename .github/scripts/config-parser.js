const fs = require('fs');
const path = require('path');
const yaml = require('js-yaml');

class ConfigParser {
  constructor(configPath = '.github/config/workflow-issue-config.yml') {
    this.configPath = path.isAbsolute(configPath) ? configPath : path.join(__dirname, '..', configPath.replace('.github/', ''));
    this.config = null;
    this.schema = this.getConfigSchema();
  }

  /**
   * Load and parse the configuration file
   */
  loadConfig() {
    try {
      if (!fs.existsSync(this.configPath)) {
        throw new Error(`Configuration file not found: ${this.configPath}`);
      }

      const configContent = fs.readFileSync(this.configPath, 'utf8');
      this.config = yaml.load(configContent);
      
      this.validateConfig();
      return this.config;
    } catch (error) {
      throw new Error(`Failed to load configuration: ${error.message}`);
    }
  }

  /**
   * Get configuration for a specific workflow
   */
  getWorkflowConfig(workflowName) {
    if (!this.config) {
      this.loadConfig();
    }

    const globalConfig = this.config.global || {};
    const workflowConfig = this.config.workflows?.[workflowName] || {};

    // Merge global and workflow-specific configuration
    return {
      enabled: workflowConfig.enabled !== undefined ? workflowConfig.enabled : globalConfig.enabled,
      severity: workflowConfig.severity || 'medium',
      assignees: workflowConfig.assignees || globalConfig.default_assignees || [],
      labels: workflowConfig.labels || globalConfig.default_labels || ['workflow-failure'],
      template: workflowConfig.template || 'default',
      escalation_threshold: workflowConfig.escalation_threshold || globalConfig.escalation_threshold || 3,
      notification_channels: {
        ...globalConfig.notification_channels,
        ...workflowConfig.notification_channels
      }
    };
  }

  /**
   * Get global configuration
   */
  getGlobalConfig() {
    if (!this.config) {
      this.loadConfig();
    }
    return this.config.global || {};
  }

  /**
   * Get notification settings
   */
  getNotificationConfig() {
    if (!this.config) {
      this.loadConfig();
    }
    return this.config.notifications || {};
  }

  /**
   * Get template configuration
   */
  getTemplateConfig() {
    if (!this.config) {
      this.loadConfig();
    }
    return this.config.templates || {};
  }

  /**
   * Validate configuration against schema
   */
  validateConfig() {
    if (!this.config) {
      throw new Error('Configuration not loaded');
    }

    // Validate global section
    if (this.config.global) {
      this.validateGlobalConfig(this.config.global);
    }

    // Validate workflows section
    if (this.config.workflows) {
      this.validateWorkflowsConfig(this.config.workflows);
    }

    // Validate notifications section
    if (this.config.notifications) {
      this.validateNotificationsConfig(this.config.notifications);
    }
  }

  /**
   * Validate global configuration section
   */
  validateGlobalConfig(globalConfig) {
    const requiredFields = ['enabled'];
    const validSeverities = ['critical', 'high', 'medium', 'low'];

    for (const field of requiredFields) {
      if (globalConfig[field] === undefined) {
        throw new Error(`Global configuration missing required field: ${field}`);
      }
    }

    if (typeof globalConfig.enabled !== 'boolean') {
      throw new Error('Global enabled field must be boolean');
    }

    if (globalConfig.default_assignees && !Array.isArray(globalConfig.default_assignees)) {
      throw new Error('Global default_assignees must be an array');
    }

    if (globalConfig.default_labels && !Array.isArray(globalConfig.default_labels)) {
      throw new Error('Global default_labels must be an array');
    }
  }

  /**
   * Validate workflows configuration section
   */
  validateWorkflowsConfig(workflowsConfig) {
    const validSeverities = ['critical', 'high', 'medium', 'low'];

    for (const [workflowName, workflowConfig] of Object.entries(workflowsConfig)) {
      if (workflowConfig.severity && !validSeverities.includes(workflowConfig.severity)) {
        throw new Error(`Invalid severity for workflow ${workflowName}: ${workflowConfig.severity}`);
      }

      if (workflowConfig.assignees && !Array.isArray(workflowConfig.assignees)) {
        throw new Error(`Assignees for workflow ${workflowName} must be an array`);
      }

      if (workflowConfig.labels && !Array.isArray(workflowConfig.labels)) {
        throw new Error(`Labels for workflow ${workflowName} must be an array`);
      }

      if (workflowConfig.escalation_threshold && typeof workflowConfig.escalation_threshold !== 'number') {
        throw new Error(`Escalation threshold for workflow ${workflowName} must be a number`);
      }
    }
  }

  /**
   * Validate notifications configuration section
   */
  validateNotificationsConfig(notificationsConfig) {
    if (notificationsConfig.enabled !== undefined && typeof notificationsConfig.enabled !== 'boolean') {
      throw new Error('Notifications enabled field must be boolean');
    }

    const numericFields = ['throttle_minutes', 'escalation_delay_minutes', 'max_notifications_per_hour'];
    for (const field of numericFields) {
      if (notificationsConfig[field] !== undefined && typeof notificationsConfig[field] !== 'number') {
        throw new Error(`Notifications ${field} must be a number`);
      }
    }
  }

  /**
   * Get configuration schema for validation
   */
  getConfigSchema() {
    return {
      global: {
        enabled: { type: 'boolean', required: true },
        default_assignees: { type: 'array', required: false },
        default_labels: { type: 'array', required: false },
        notification_channels: { type: 'object', required: false },
        escalation_threshold: { type: 'number', required: false }
      },
      workflows: {
        type: 'object',
        properties: {
          severity: { type: 'string', enum: ['critical', 'high', 'medium', 'low'] },
          assignees: { type: 'array' },
          labels: { type: 'array' },
          template: { type: 'string' },
          escalation_threshold: { type: 'number' },
          notification_channels: { type: 'object' }
        }
      },
      notifications: {
        enabled: { type: 'boolean' },
        throttle_minutes: { type: 'number' },
        escalation_delay_minutes: { type: 'number' },
        max_notifications_per_hour: { type: 'number' }
      }
    };
  }

  /**
   * Check if workflow is enabled for issue creation
   */
  isWorkflowEnabled(workflowName) {
    const workflowConfig = this.getWorkflowConfig(workflowName);
    return workflowConfig.enabled;
  }

  /**
   * Get all configured workflow names
   */
  getConfiguredWorkflows() {
    if (!this.config) {
      this.loadConfig();
    }
    return Object.keys(this.config.workflows || {});
  }
}

module.exports = ConfigParser;