const { describe, it, expect } = require('@jest/globals');
const yaml = require('js-yaml');
const fs = require('fs').promises;
const path = require('path');
const Ajv = require('ajv');

// Configuration schema
const configSchema = {
  type: 'object',
  properties: {
    global: {
      type: 'object',
      properties: {
        enabled: { type: 'boolean' },
        default_assignees: {
          type: 'array',
          items: { type: 'string' }
        },
        default_labels: {
          type: 'array',
          items: { type: 'string' }
        },
        notification_channels: {
          type: 'object',
          properties: {
            slack: { type: 'string' },
            email: {
              oneOf: [
                { type: 'string' },
                { type: 'array', items: { type: 'string' } }
              ]
            },
            teams: { type: 'boolean' }
          }
        },
        escalation_threshold: {
          type: 'integer',
          minimum: 1,
          maximum: 100
        },
        duplicate_detection: { type: 'boolean' }
      },
      required: ['enabled']
    },
    workflows: {
      type: 'object',
      patternProperties: {
        '.*': {
          type: 'object',
          properties: {
            severity: {
              type: 'string',
              enum: ['low', 'medium', 'high', 'critical']
            },
            assignees: {
              type: 'array',
              items: { type: 'string' }
            },
            labels: {
              type: 'array',
              items: { type: 'string' }
            },
            template: { type: 'string' },
            escalation_threshold: {
              type: 'integer',
              minimum: 1
            },
            notification_channels: {
              type: 'object'
            },
            create_issue_on_pr_only: { type: 'boolean' }
          },
          required: ['severity']
        }
      }
    },
    templates: {
      type: 'object',
      properties: {
        default_template: { type: 'string' },
        custom_template_path: { type: 'string' }
      }
    },
    notifications: {
      type: 'object',
      properties: {
        enabled: { type: 'boolean' },
        throttle_minutes: {
          type: 'integer',
          minimum: 1,
          maximum: 1440
        },
        escalation_delay_minutes: {
          type: 'integer',
          minimum: 1
        },
        max_notifications_per_hour: {
          type: 'integer',
          minimum: 1,
          maximum: 1000
        }
      }
    }
  },
  required: ['global', 'workflows']
};

describe('Configuration Validation and Error Handling', () => {
  const ajv = new Ajv();
  const validate = ajv.compile(configSchema);
  
  describe('Schema Validation', () => {
    it('should validate correct configuration', () => {
      const validConfig = {
        global: {
          enabled: true,
          default_assignees: ['team-lead'],
          default_labels: ['workflow-failure'],
          escalation_threshold: 3,
          duplicate_detection: true
        },
        workflows: {
          'Test Workflow': {
            severity: 'medium',
            assignees: ['dev-team'],
            labels: ['test']
          }
        }
      };
      
      const valid = validate(validConfig);
      expect(valid).toBe(true);
    });
    
    it('should reject invalid severity levels', () => {
      const invalidConfig = {
        global: { enabled: true },
        workflows: {
          'Test': {
            severity: 'extreme' // Invalid
          }
        }
      };
      
      const valid = validate(invalidConfig);
      expect(valid).toBe(false);
      expect(validate.errors[0].message).toContain('enum');
    });
    
    it('should reject negative escalation thresholds', () => {
      const invalidConfig = {
        global: {
          enabled: true,
          escalation_threshold: -1
        },
        workflows: {}
      };
      
      const valid = validate(invalidConfig);
      expect(valid).toBe(false);
      expect(validate.errors[0].message).toContain('minimum');
    });
    
    it('should allow flexible notification channel configuration', () => {
      const configs = [
        { slack: '#channel' },
        { email: 'test@example.com' },
        { email: ['test1@example.com', 'test2@example.com'] },
        { slack: '#channel', email: ['test@example.com'], teams: true }
      ];
      
      for (const channelConfig of configs) {
        const config = {
          global: {
            enabled: true,
            notification_channels: channelConfig
          },
          workflows: {}
        };
        
        const valid = validate(config);
        expect(valid).toBe(true);
      }
    });
  });
  
  describe('Configuration Loading', () => {
    it('should handle missing configuration file gracefully', async () => {
      const loader = new ConfigLoader();
      const config = await loader.load('/non/existent/config.yml');
      
      expect(config).toBeDefined();
      expect(config.global.enabled).toBe(true);
      expect(config.workflows).toEqual({});
    });
    
    it('should merge environment variable overrides', async () => {
      process.env.WORKFLOW_FAILURE_DEFAULT_SEVERITY = 'high';
      process.env.WORKFLOW_FAILURE_ESCALATION_THRESHOLD = '5';
      
      const loader = new ConfigLoader();
      const baseConfig = {
        global: {
          enabled: true,
          escalation_threshold: 3
        },
        workflows: {
          'Test': {
            severity: 'low'
          }
        }
      };
      
      const merged = loader.mergeWithEnv(baseConfig);
      
      expect(merged.global.default_severity).toBe('high');
      expect(merged.global.escalation_threshold).toBe(5);
      
      delete process.env.WORKFLOW_FAILURE_DEFAULT_SEVERITY;
      delete process.env.WORKFLOW_FAILURE_ESCALATION_THRESHOLD;
    });
    
    it('should validate merged configuration', async () => {
      const loader = new ConfigLoader();
      
      // Invalid env override
      process.env.WORKFLOW_FAILURE_DEFAULT_SEVERITY = 'invalid-severity';
      
      const baseConfig = {
        global: { enabled: true },
        workflows: {}
      };
      
      expect(() => {
        loader.mergeWithEnv(baseConfig, { validate: true });
      }).toThrow('Invalid configuration');
      
      delete process.env.WORKFLOW_FAILURE_DEFAULT_SEVERITY;
    });
  });
  
  describe('Workflow-Specific Configuration', () => {
    it('should apply workflow-specific overrides correctly', () => {
      const config = {
        global: {
          enabled: true,
          default_assignees: ['default-team'],
          default_labels: ['workflow-failure'],
          escalation_threshold: 5
        },
        workflows: {
          'Critical Deploy': {
            severity: 'critical',
            assignees: ['devops-team', 'oncall'],
            escalation_threshold: 1
          },
          'Daily Report': {
            severity: 'low',
            create_issue_on_pr_only: false
          }
        }
      };
      
      const resolver = new ConfigResolver(config);
      
      // Critical deploy should override defaults
      const criticalConfig = resolver.getWorkflowConfig('Critical Deploy');
      expect(criticalConfig.severity).toBe('critical');
      expect(criticalConfig.assignees).toEqual(['devops-team', 'oncall']);
      expect(criticalConfig.escalation_threshold).toBe(1);
      expect(criticalConfig.labels).toEqual(['workflow-failure']); // Inherited
      
      // Daily report should use some defaults
      const reportConfig = resolver.getWorkflowConfig('Daily Report');
      expect(reportConfig.severity).toBe('low');
      expect(reportConfig.assignees).toEqual(['default-team']); // Inherited
      expect(reportConfig.escalation_threshold).toBe(5); // Inherited
    });
    
    it('should handle wildcard workflow patterns', () => {
      const config = {
        global: { enabled: true },
        workflows: {
          'Deploy-*': {
            severity: 'high',
            assignees: ['devops-team']
          },
          'Test-*': {
            severity: 'low',
            labels: ['test', 'automated']
          },
          'Deploy-Production': {
            severity: 'critical' // More specific overrides wildcard
          }
        }
      };
      
      const resolver = new ConfigResolver(config);
      
      expect(resolver.getWorkflowConfig('Deploy-Staging').severity).toBe('high');
      expect(resolver.getWorkflowConfig('Deploy-Production').severity).toBe('critical');
      expect(resolver.getWorkflowConfig('Test-Unit').labels).toContain('test');
    });
  });
  
  describe('Configuration Performance', () => {
    it('should cache configuration lookups', () => {
      const config = {
        global: { enabled: true },
        workflows: {}
      };
      
      // Generate many workflow configs
      for (let i = 0; i < 1000; i++) {
        config.workflows[`Workflow-${i}`] = {
          severity: 'medium',
          assignees: [`team-${i % 10}`]
        };
      }
      
      const resolver = new ConfigResolver(config);
      
      // First lookup
      const start1 = performance.now();
      for (let i = 0; i < 1000; i++) {
        resolver.getWorkflowConfig(`Workflow-${i}`);
      }
      const duration1 = performance.now() - start1;
      
      // Second lookup (should be cached)
      const start2 = performance.now();
      for (let i = 0; i < 1000; i++) {
        resolver.getWorkflowConfig(`Workflow-${i}`);
      }
      const duration2 = performance.now() - start2;
      
      // Cached lookup should be much faster
      expect(duration2).toBeLessThan(duration1 / 10);
    });
  });
  
  describe('Error Recovery', () => {
    it('should provide sensible defaults for corrupted config', () => {
      const corruptedConfigs = [
        null,
        undefined,
        {},
        { workflows: null },
        { global: null },
        'not an object',
        []
      ];
      
      for (const corrupted of corruptedConfigs) {
        const resolver = new ConfigResolver(corrupted);
        const config = resolver.getWorkflowConfig('Any Workflow');
        
        expect(config).toBeDefined();
        expect(config.severity).toBe('medium');
        expect(config.assignees).toEqual([]);
        expect(config.labels).toEqual(['workflow-failure']);
      }
    });
    
    it('should log warnings for invalid configurations', () => {
      const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
      
      const invalidConfig = {
        global: { enabled: true },
        workflows: {
          'Test': {
            severity: 'invalid',
            escalation_threshold: -5
          }
        }
      };
      
      const resolver = new ConfigResolver(invalidConfig);
      resolver.getWorkflowConfig('Test');
      
      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('Invalid configuration')
      );
      
      consoleSpy.mockRestore();
    });
  });
});

// Helper classes for tests
class ConfigLoader {
  async load(filePath) {
    try {
      const content = await fs.readFile(filePath, 'utf8');
      return yaml.load(content);
    } catch (error) {
      return this.getDefaultConfig();
    }
  }
  
  mergeWithEnv(config, options = {}) {
    const merged = JSON.parse(JSON.stringify(config));
    
    // Apply environment overrides
    if (process.env.WORKFLOW_FAILURE_DEFAULT_SEVERITY) {
      merged.global.default_severity = process.env.WORKFLOW_FAILURE_DEFAULT_SEVERITY;
    }
    
    if (process.env.WORKFLOW_FAILURE_ESCALATION_THRESHOLD) {
      merged.global.escalation_threshold = parseInt(
        process.env.WORKFLOW_FAILURE_ESCALATION_THRESHOLD
      );
    }
    
    if (options.validate) {
      const valid = validate(merged);
      if (!valid) {
        throw new Error('Invalid configuration: ' + JSON.stringify(validate.errors));
      }
    }
    
    return merged;
  }
  
  getDefaultConfig() {
    return {
      global: {
        enabled: true,
        default_assignees: [],
        default_labels: ['workflow-failure'],
        escalation_threshold: 3,
        duplicate_detection: true
      },
      workflows: {},
      notifications: {
        enabled: true,
        throttle_minutes: 30
      }
    };
  }
}

class ConfigResolver {
  constructor(config) {
    this.config = this.normalizeConfig(config);
    this.cache = new Map();
  }
  
  normalizeConfig(config) {
    if (!config || typeof config !== 'object') {
      return new ConfigLoader().getDefaultConfig();
    }
    
    return {
      global: config.global || { enabled: true },
      workflows: config.workflows || {}
    };
  }
  
  getWorkflowConfig(workflowName) {
    // Check cache
    if (this.cache.has(workflowName)) {
      return this.cache.get(workflowName);
    }
    
    // Start with global defaults
    const config = {
      severity: 'medium',
      assignees: this.config.global.default_assignees || [],
      labels: this.config.global.default_labels || ['workflow-failure'],
      escalation_threshold: this.config.global.escalation_threshold || 3
    };
    
    // Apply wildcard patterns
    for (const [pattern, workflowConfig] of Object.entries(this.config.workflows)) {
      if (this.matchesPattern(workflowName, pattern)) {
        Object.assign(config, workflowConfig);
      }
    }
    
    // Apply exact match (overrides wildcards)
    if (this.config.workflows[workflowName]) {
      Object.assign(config, this.config.workflows[workflowName]);
    }
    
    // Validate and fix
    this.validateWorkflowConfig(config, workflowName);
    
    // Cache result
    this.cache.set(workflowName, config);
    
    return config;
  }
  
  matchesPattern(name, pattern) {
    if (pattern === name) return true;
    if (!pattern.includes('*')) return false;
    
    const regex = new RegExp('^' + pattern.replace('*', '.*') + '$');
    return regex.test(name);
  }
  
  validateWorkflowConfig(config, workflowName) {
    const validSeverities = ['low', 'medium', 'high', 'critical'];
    if (!validSeverities.includes(config.severity)) {
      console.warn(`Invalid configuration for workflow "${workflowName}": Invalid severity "${config.severity}"`);
      config.severity = 'medium';
    }
    
    if (config.escalation_threshold && config.escalation_threshold < 1) {
      console.warn(`Invalid configuration for workflow "${workflowName}": Invalid escalation_threshold`);
      config.escalation_threshold = 3;
    }
    
    if (!Array.isArray(config.assignees)) {
      config.assignees = [];
    }
    
    if (!Array.isArray(config.labels)) {
      config.labels = ['workflow-failure'];
    }
  }
}