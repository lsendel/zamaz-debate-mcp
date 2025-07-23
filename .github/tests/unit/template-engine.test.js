const { describe, it, expect, jest, beforeEach } = require('@jest/globals');
const { TemplateEngine } = require('../../scripts/template-engine');
const fs = require('fs').promises;

jest.mock('fs', () => ({
  promises: {
    readFile: jest.fn()
  }
}));

describe('TemplateEngine', () => {
  let templateEngine;
  
  beforeEach(() => {
    templateEngine = new TemplateEngine();
    jest.clearAllMocks();
  });
  
  describe('Template Loading', () => {
    it('should load and cache templates', async () => {
      const templateContent = 'Hello {{name}}!';
      fs.readFile.mockResolvedValue(templateContent);
      
      const template1 = await templateEngine.loadTemplate('test');
      const template2 = await templateEngine.loadTemplate('test');
      
      expect(template1).toBe(templateContent);
      expect(template2).toBe(templateContent);
      expect(fs.readFile).toHaveBeenCalledTimes(1); // Should use cache
    });
    
    it('should fall back to default template if not found', async () => {
      fs.readFile
        .mockRejectedValueOnce(new Error('File not found'))
        .mockResolvedValueOnce('Default template');
      
      const template = await templateEngine.loadTemplate('nonexistent');
      
      expect(template).toBe('Default template');
      expect(fs.readFile).toHaveBeenCalledTimes(2);
    });
  });
  
  describe('Variable Replacement', () => {
    it('should replace simple variables', () => {
      const template = 'Hello {{name}}, you are {{age}} years old!';
      const data = { name: 'John', age: 30 };
      
      const result = templateEngine.renderTemplate(template, data);
      
      expect(result).toBe('Hello John, you are 30 years old!');
    });
    
    it('should handle nested properties', () => {
      const template = 'Workflow {{workflow.name}} on branch {{workflow.branch}}';
      const data = {
        workflow: {
          name: 'CI/CD',
          branch: 'main'
        }
      };
      
      const result = templateEngine.renderTemplate(template, data);
      
      expect(result).toBe('Workflow CI/CD on branch main');
    });
    
    it('should handle missing variables', () => {
      const template = 'Hello {{name}}, {{missing}} value';
      const data = { name: 'John' };
      
      const result = templateEngine.renderTemplate(template, data);
      
      expect(result).toBe('Hello John, {{missing}} value');
    });
  });
  
  describe('Conditionals', () => {
    it('should handle simple if conditions', () => {
      const template = '{{#if hasError}}Error found!{{/if}}';
      
      const result1 = templateEngine.renderTemplate(template, { hasError: true });
      const result2 = templateEngine.renderTemplate(template, { hasError: false });
      
      expect(result1).toBe('Error found!');
      expect(result2).toBe('');
    });
    
    it('should handle nested conditionals', () => {
      const template = '{{#if user}}Hello {{user.name}}{{#if user.admin}} (Admin){{/if}}{{/if}}';
      const data = {
        user: {
          name: 'John',
          admin: true
        }
      };
      
      const result = templateEngine.renderTemplate(template, data);
      
      expect(result).toBe('Hello John (Admin)');
    });
    
    it('should handle includes helper', () => {
      const template = '{{#if (includes tags "critical")}}CRITICAL ISSUE{{/if}}';
      
      const result1 = templateEngine.renderTemplate(template, {
        tags: ['bug', 'critical', 'production']
      });
      const result2 = templateEngine.renderTemplate(template, {
        tags: ['bug', 'minor']
      });
      
      expect(result1).toBe('CRITICAL ISSUE');
      expect(result2).toBe('');
    });
    
    it('should handle equals helper', () => {
      const template = '{{#if (equals status "failed")}}Build Failed{{/if}}';
      
      const result1 = templateEngine.renderTemplate(template, { status: 'failed' });
      const result2 = templateEngine.renderTemplate(template, { status: 'success' });
      
      expect(result1).toBe('Build Failed');
      expect(result2).toBe('');
    });
  });
  
  describe('Loops', () => {
    it('should handle each loops', () => {
      const template = '{{#each items}}- {{this}}\n{{/each}}';
      const data = {
        items: ['Item 1', 'Item 2', 'Item 3']
      };
      
      const result = templateEngine.renderTemplate(template, data);
      
      expect(result).toBe('- Item 1\n- Item 2\n- Item 3\n');
    });
    
    it('should handle object arrays in loops', () => {
      const template = '{{#each jobs}}Job: {{name}} - {{status}}\n{{/each}}';
      const data = {
        jobs: [
          { name: 'Build', status: 'failed' },
          { name: 'Test', status: 'success' }
        ]
      };
      
      const result = templateEngine.renderTemplate(template, data);
      
      expect(result).toBe('Job: Build - failed\nJob: Test - success\n');
    });
    
    it('should handle nested loops', () => {
      const template = '{{#each workflows}}{{name}}:\n{{#each jobs}}  - {{this}}\n{{/each}}{{/each}}';
      const data = {
        workflows: [
          { name: 'CI', jobs: ['Build', 'Test'] },
          { name: 'CD', jobs: ['Deploy'] }
        ]
      };
      
      const result = templateEngine.renderTemplate(template, data);
      
      expect(result).toBe('CI:\n  - Build\n  - Test\nCD:\n  - Deploy\n');
    });
    
    it('should handle empty arrays', () => {
      const template = 'Items:{{#each items}} {{this}}{{/each}}';
      const data = { items: [] };
      
      const result = templateEngine.renderTemplate(template, data);
      
      expect(result).toBe('Items:');
    });
  });
  
  describe('Truncate Helper', () => {
    it('should truncate long strings', () => {
      const template = 'Commit: {{commit|truncate:7}}';
      const data = { commit: 'abc123def456789' };
      
      const result = templateEngine.renderTemplate(template, data);
      
      expect(result).toBe('Commit: abc123d');
    });
    
    it('should not truncate short strings', () => {
      const template = 'Commit: {{commit|truncate:10}}';
      const data = { commit: 'abc123' };
      
      const result = templateEngine.renderTemplate(template, data);
      
      expect(result).toBe('Commit: abc123');
    });
  });
  
  describe('generateFromTemplate', () => {
    it('should generate complete issue content', async () => {
      const templateContent = `## {{workflow.name}} Failed

{{#if failure.jobs}}
Failed Jobs:
{{#each failure.jobs}}
- {{name}}: {{conclusion}}
{{/each}}
{{/if}}

Troubleshooting:
{{troubleshootingSteps}}`;
      
      fs.readFile.mockResolvedValue(templateContent);
      
      const workflowData = {
        workflow: { name: 'CI/CD Pipeline' },
        failure: {
          category: 'ci-cd',
          jobs: [
            { name: 'Build', conclusion: 'failure' },
            { name: 'Test', conclusion: 'failure' }
          ],
          errorPatterns: ['build-failure']
        }
      };
      
      const result = await templateEngine.generateFromTemplate('ci-cd', workflowData);
      
      expect(result).toContain('CI/CD Pipeline Failed');
      expect(result).toContain('- Build: failure');
      expect(result).toContain('- Test: failure');
      expect(result).toContain('Check build dependencies');
    });
  });
  
  describe('Complex Template Scenarios', () => {
    it('should handle mixed conditionals and loops', () => {
      const template = `{{#if failure}}
## Failure Report
{{#each failure.jobs}}
{{#if (equals conclusion "failure")}}
- ❌ {{name}}
{{/if}}
{{/each}}
{{/if}}`;
      
      const data = {
        failure: {
          jobs: [
            { name: 'Build', conclusion: 'failure' },
            { name: 'Lint', conclusion: 'success' },
            { name: 'Test', conclusion: 'failure' }
          ]
        }
      };
      
      const result = templateEngine.renderTemplate(template, data);
      
      expect(result).toContain('## Failure Report');
      expect(result).toContain('- ❌ Build');
      expect(result).toContain('- ❌ Test');
      expect(result).not.toContain('Lint');
    });
  });
});