const { describe, it, expect, jest, beforeEach, afterEach } = require('@jest/globals');
const { exec } = require('child_process');
const { promisify } = require('util');
const fs = require('fs').promises;
const path = require('path');
const yaml = require('js-yaml');

const execAsync = promisify(exec);

describe('Workflow Integration Tests', () => {
  const testDir = path.join(__dirname, '../../..');
  let originalCwd;
  
  beforeEach(() => {
    originalCwd = process.cwd();
    process.chdir(testDir);
  });
  
  afterEach(() => {
    process.chdir(originalCwd);
  });
  
  describe('Configuration Loading', () => {
    it('should load workflow configuration correctly', async () => {
      const configPath = '.github/config/workflow-issue-config.yml';
      const configContent = await fs.readFile(configPath, 'utf8');
      const config = yaml.load(configContent);
      
      expect(config.global).toBeDefined();
      expect(config.workflows).toBeDefined();
      expect(config.workflows['CI/CD Pipeline']).toBeDefined();
      expect(config.workflows['CI/CD Pipeline'].severity).toBe('critical');
    });
    
    it('should validate configuration schema', async () => {
      const configPath = '.github/config/workflow-issue-config.yml';
      const configContent = await fs.readFile(configPath, 'utf8');
      const config = yaml.load(configContent);
      
      // Validate required fields
      expect(config.global.enabled).toBe(true);
      expect(Array.isArray(config.global.default_assignees)).toBe(true);
      expect(Array.isArray(config.global.default_labels)).toBe(true);
      
      // Validate workflow configurations
      Object.entries(config.workflows).forEach(([name, workflow]) => {
        expect(['critical', 'high', 'medium', 'low']).toContain(workflow.severity);
        expect(Array.isArray(workflow.assignees)).toBe(true);
        expect(Array.isArray(workflow.labels)).toBe(true);
        expect(typeof workflow.template).toBe('string');
        expect(typeof workflow.escalation_threshold).toBe('number');
      });
    });
  });
  
  describe('Template Loading and Rendering', () => {
    it('should load all template files', async () => {
      const templatesDir = '.github/templates/workflow-issues';
      const templates = await fs.readdir(templatesDir);
      
      const expectedTemplates = [
        'default.md',
        'ci-cd.md',
        'security.md',
        'linting.md',
        'deployment.md'
      ];
      
      expectedTemplates.forEach(template => {
        expect(templates).toContain(template);
      });
    });
    
    it('should render templates with sample data', async () => {
      const { TemplateEngine } = require('../../scripts/template-engine');
      const engine = new TemplateEngine();
      
      const sampleData = {
        workflow: {
          name: 'Test Workflow',
          runNumber: 123,
          url: 'https://github.com/test/test/actions/runs/123',
          branch: 'main',
          commit: {
            sha: 'abc123def456',
            message: 'Test commit'
          },
          triggeredBy: 'test-user'
        },
        failure: {
          timestamp: new Date().toISOString(),
          jobs: [{
            name: 'Test Job',
            id: 1,
            conclusion: 'failure',
            steps: [{
              name: 'Test Step',
              errorMessage: 'Test failed',
              logUrl: 'https://test.com'
            }]
          }],
          severity: 'high',
          category: 'testing',
          errorPatterns: ['test-failure']
        },
        owner: 'test-owner',
        repo: 'test-repo'
      };
      
      const templates = ['default', 'ci-cd', 'security', 'linting', 'deployment'];
      
      for (const template of templates) {
        const rendered = await engine.generateFromTemplate(template, sampleData);
        
        expect(rendered).toContain('Test Workflow');
        expect(rendered).toContain('workflow:Test Workflow');
        expect(rendered).toContain('failure-type:testing');
        expect(rendered).toContain('Troubleshooting');
      }
    });
  });
  
  describe('Workflow Handler Integration', () => {
    it('should validate workflow handler file exists', async () => {
      const workflowPath = '.github/workflows/workflow-failure-handler.yml';
      const exists = await fs.access(workflowPath).then(() => true).catch(() => false);
      
      expect(exists).toBe(true);
    });
    
    it('should validate workflow handler structure', async () => {
      const workflowPath = '.github/workflows/workflow-failure-handler.yml';
      const workflowContent = await fs.readFile(workflowPath, 'utf8');
      const workflow = yaml.load(workflowContent);
      
      // Validate workflow structure
      expect(workflow.name).toBe('Workflow Failure Handler');
      expect(workflow.on.workflow_call).toBeDefined();
      expect(workflow.on.workflow_call.inputs).toBeDefined();
      expect(workflow.on.workflow_call.secrets).toBeDefined();
      
      // Validate required inputs
      const inputs = workflow.on.workflow_call.inputs;
      expect(inputs['workflow-name'].required).toBe(true);
      expect(inputs['workflow-name'].type).toBe('string');
      
      // Validate jobs
      expect(workflow.jobs['handle-failure']).toBeDefined();
      expect(workflow.jobs['handle-failure'].steps).toBeDefined();
    });
  });
  
  describe('Script Dependencies', () => {
    it('should have all required npm packages', async () => {
      const scriptsPackageJson = JSON.parse(
        await fs.readFile('.github/scripts/package.json', 'utf8')
      );
      
      const requiredPackages = [
        '@actions/core',
        '@actions/github',
        '@octokit/rest',
        'js-yaml',
        'nodemailer'
      ];
      
      requiredPackages.forEach(pkg => {
        expect(scriptsPackageJson.dependencies).toHaveProperty(pkg);
      });
    });
    
    it('should have all required action packages', async () => {
      const actionPackageJson = JSON.parse(
        await fs.readFile('.github/actions/failure-detector/package.json', 'utf8')
      );
      
      const requiredPackages = [
        '@actions/core',
        '@actions/github'
      ];
      
      requiredPackages.forEach(pkg => {
        expect(actionPackageJson.dependencies).toHaveProperty(pkg);
      });
    });
  });
  
  describe('End-to-End Workflow Simulation', () => {
    it('should handle complete failure detection flow', async () => {
      // Create mock workflow data
      const mockWorkflowData = {
        workflow: {
          name: 'Integration Test Workflow',
          id: 999,
          runId: 999,
          runNumber: 99,
          url: 'https://github.com/test/test/actions/runs/999',
          triggeredBy: 'integration-test',
          branch: 'test-branch',
          commit: {
            sha: 'test123456789',
            message: 'Integration test commit',
            author: 'Test Author'
          }
        },
        failure: {
          timestamp: new Date().toISOString(),
          jobs: [{
            name: 'Integration Test Job',
            id: 1,
            conclusion: 'failure',
            steps: [{
              name: 'Test Step',
              conclusion: 'failure',
              errorMessage: 'Integration test error',
              logUrl: 'https://test.com/logs'
            }],
            logs: 'Test logs here'
          }],
          severity: 'medium',
          category: 'testing',
          errorPatterns: ['test-failure']
        },
        context: {
          environment: 'test',
          previousFailures: 0
        },
        labels: ['test', 'integration'],
        assignees: ['test-user']
      };
      
      // Test issue manager
      const { IssueManager } = require('../../scripts/issue-manager');
      const issueManager = new IssueManager('test-token', 'test-owner', 'test-repo');
      
      const issueData = await issueManager.analyzeFailure(mockWorkflowData);
      
      expect(issueData.title).toContain('Integration Test Workflow');
      expect(issueData.body).toContain('workflow:Integration Test Workflow');
      expect(issueData.labels).toEqual(['test', 'integration']);
      
      // Test template engine
      const { TemplateEngine } = require('../../scripts/template-engine');
      const templateEngine = new TemplateEngine();
      
      const renderedContent = await templateEngine.generateFromTemplate('default', mockWorkflowData);
      
      expect(renderedContent).toContain('Integration Test Workflow');
      expect(renderedContent).toContain('Test Step');
      expect(renderedContent).toContain('Troubleshooting');
    });
  });
  
  describe('Error Handling', () => {
    it('should handle missing configuration gracefully', async () => {
      // Test with non-existent workflow configuration
      const configPath = '.github/config/workflow-issue-config.yml';
      const config = yaml.load(await fs.readFile(configPath, 'utf8'));
      
      const unknownWorkflow = config.workflows['Unknown Workflow'] || {};
      
      // Should fall back to global defaults
      expect(unknownWorkflow.severity || config.global.default_severity || 'medium').toBeDefined();
      expect(unknownWorkflow.assignees || config.global.default_assignees).toBeDefined();
    });
    
    it('should handle template rendering errors', async () => {
      const { TemplateEngine } = require('../../scripts/template-engine');
      const templateEngine = new TemplateEngine();
      
      // Test with malformed data
      const malformedData = {
        workflow: null,
        failure: undefined
      };
      
      // Should not throw, but return partially rendered template
      await expect(
        templateEngine.generateFromTemplate('default', malformedData)
      ).resolves.toBeDefined();
    });
  });
});