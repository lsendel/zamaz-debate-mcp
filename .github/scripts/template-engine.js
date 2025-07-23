const fs = require('fs').promises;
const path = require('path');
const core = require('@actions/core');

class TemplateEngine {
  constructor(templatesDir = '.github/templates/workflow-issues') {
    this.templatesDir = templatesDir;
    this.templateCache = new Map();
  }

  async loadTemplate(templateName) {
    // Check cache first
    if (this.templateCache.has(templateName)) {
      return this.templateCache.get(templateName);
    }

    try {
      // Try to load specific template
      let templatePath = path.join(this.templatesDir, `${templateName}.md`);
      let template = await fs.readFile(templatePath, 'utf8');
      
      this.templateCache.set(templateName, template);
      return template;
    } catch (error) {
      // Fall back to default template
      if (templateName !== 'default') {
        core.warning(`Template ${templateName} not found, using default template`);
        return this.loadTemplate('default');
      }
      throw error;
    }
  }

  async render(templateName, data) {
    const template = await this.loadTemplate(templateName);
    return this.renderTemplate(template, data);
  }

  renderTemplate(template, data) {
    // Simple template engine implementation
    let rendered = template;

    // Replace variables {{variable}}
    rendered = rendered.replace(/\{\{([^}]+)\}\}/g, (match, key) => {
      const value = this.getNestedValue(data, key.trim());
      return value !== undefined ? value : match;
    });

    // Handle conditionals {{#if condition}}...{{/if}}
    rendered = this.handleConditionals(rendered, data);

    // Handle loops {{#each array}}...{{/each}}
    rendered = this.handleLoops(rendered, data);

    // Handle includes helper
    rendered = this.handleIncludes(rendered, data);

    // Handle truncate helper {{value|truncate:length}}
    rendered = this.handleTruncate(rendered);

    return rendered;
  }

  getNestedValue(obj, path) {
    const keys = path.split('.');
    let value = obj;

    for (const key of keys) {
      if (value && typeof value === 'object' && key in value) {
        value = value[key];
      } else {
        return undefined;
      }
    }

    return value;
  }

  handleConditionals(template, data) {
    const conditionalRegex = /\{\{#if\s+([^}]+)\}\}([\s\S]*?)\{\{\/if\}\}/g;
    
    return template.replace(conditionalRegex, (match, condition, content) => {
      const conditionValue = this.evaluateCondition(condition.trim(), data);
      return conditionValue ? content : '';
    });
  }

  evaluateCondition(condition, data) {
    // Handle simple conditions
    if (condition.startsWith('(') && condition.includes(')')) {
      // Handle function calls like (includes array value)
      const funcMatch = condition.match(/\((\w+)\s+([^)]+)\)/);
      if (funcMatch) {
        const [, func, args] = funcMatch;
        const argsList = args.split(/\s+/).map(arg => {
          const value = this.getNestedValue(data, arg);
          return value !== undefined ? value : arg.replace(/['"]/g, '');
        });

        switch (func) {
          case 'includes':
            const [array, value] = argsList;
            return Array.isArray(array) && array.includes(value);
          case 'equals':
            return argsList[0] === argsList[1];
          default:
            return false;
        }
      }
    }

    // Simple property check
    const value = this.getNestedValue(data, condition);
    return !!value;
  }

  handleLoops(template, data) {
    const loopRegex = /\{\{#each\s+([^}]+)\}\}([\s\S]*?)\{\{\/each\}\}/g;
    
    return template.replace(loopRegex, (match, arrayPath, content) => {
      const array = this.getNestedValue(data, arrayPath.trim());
      
      if (!Array.isArray(array)) {
        return '';
      }

      return array.map((item, index) => {
        // Create a new context with the current item
        const loopData = {
          ...data,
          this: item,
          '@index': index,
          '@first': index === 0,
          '@last': index === array.length - 1
        };

        // Replace item properties
        let rendered = content;
        
        // Handle {{this}} for current item
        rendered = rendered.replace(/\{\{this\}\}/g, item);
        
        // Handle nested properties
        rendered = rendered.replace(/\{\{([^}]+)\}\}/g, (m, key) => {
          const k = key.trim();
          
          // Check if it's a property of the current item
          if (typeof item === 'object' && k in item) {
            return item[k];
          }
          
          // Otherwise, look in the parent data
          const value = this.getNestedValue(loopData, k);
          return value !== undefined ? value : m;
        });

        // Handle nested conditionals within loops
        rendered = this.handleConditionals(rendered, loopData);

        return rendered;
      }).join('');
    });
  }

  handleIncludes(template, data) {
    const includesRegex = /\(includes\s+([^\s]+)\s+([^)]+)\)/g;
    
    return template.replace(includesRegex, (match, arrayPath, value) => {
      const array = this.getNestedValue(data, arrayPath);
      const checkValue = value.replace(/['"]/g, '');
      
      if (Array.isArray(array)) {
        return array.includes(checkValue);
      }
      return false;
    });
  }

  handleTruncate(template) {
    const truncateRegex = /\{\{([^|]+)\|truncate:(\d+)\}\}/g;
    
    return template.replace(truncateRegex, (match, value, length) => {
      const val = value.trim();
      const len = parseInt(length, 10);
      
      if (val.length > len) {
        return val.substring(0, len);
      }
      return val;
    });
  }

  async generateFromTemplate(templateName, workflowData) {
    // Enrich data with additional context
    const enrichedData = {
      ...workflowData,
      owner: process.env.GITHUB_REPOSITORY?.split('/')[0] || 'owner',
      repo: process.env.GITHUB_REPOSITORY?.split('/')[1] || 'repo',
      troubleshootingSteps: this.generateTroubleshootingSteps(
        workflowData.failure.category,
        workflowData.failure.errorPatterns
      )
    };

    // Render the template
    const rendered = await this.render(templateName, enrichedData);
    
    return rendered;
  }

  generateTroubleshootingSteps(category, errorPatterns) {
    const steps = [];
    
    // Category-specific troubleshooting
    switch (category) {
      case 'ci-cd':
        steps.push('- [ ] Check build dependencies and versions');
        steps.push('- [ ] Verify environment variables are correctly set');
        steps.push('- [ ] Review recent dependency updates');
        steps.push('- [ ] Check for disk space and resource availability');
        break;
      
      case 'security':
        steps.push('- [ ] Review security scan results and vulnerabilities');
        steps.push('- [ ] Check for newly introduced dependencies');
        steps.push('- [ ] Verify security policies are up to date');
        steps.push('- [ ] Review code changes for security best practices');
        break;
      
      case 'code-quality':
        steps.push('- [ ] Run linting locally to reproduce issues');
        steps.push('- [ ] Check linting configuration files');
        steps.push('- [ ] Review code formatting standards');
        steps.push('- [ ] Verify pre-commit hooks are working');
        break;
      
      case 'deployment':
        steps.push('- [ ] Verify deployment credentials and permissions');
        steps.push('- [ ] Check target environment availability');
        steps.push('- [ ] Review deployment configuration');
        steps.push('- [ ] Check for infrastructure changes');
        break;
      
      case 'testing':
        steps.push('- [ ] Run tests locally to reproduce failures');
        steps.push('- [ ] Check for flaky tests');
        steps.push('- [ ] Review test dependencies and fixtures');
        steps.push('- [ ] Verify test environment setup');
        break;
      
      default:
        steps.push('- [ ] Review the error logs above');
        steps.push('- [ ] Check recent commits for potential causes');
        steps.push('- [ ] Verify workflow configuration is correct');
        steps.push('- [ ] Run workflow locally if possible');
    }
    
    // Error pattern-specific troubleshooting
    if (errorPatterns && errorPatterns.includes('test-failure')) {
      steps.push('- [ ] Consider running tests with verbose output');
      steps.push('- [ ] Check if tests are environment-dependent');
    }
    
    if (errorPatterns && errorPatterns.includes('build-failure')) {
      steps.push('- [ ] Clear build cache and retry');
      steps.push('- [ ] Check for breaking changes in dependencies');
    }
    
    if (errorPatterns && errorPatterns.includes('linting-failure')) {
      steps.push('- [ ] Run auto-fix commands if available');
      steps.push('- [ ] Update linting rules if needed');
    }
    
    return steps.join('\n');
  }
}

// Export for use in other scripts
module.exports = { TemplateEngine };

// CLI interface for testing
if (require.main === module) {
  async function main() {
    try {
      const templateName = process.argv[2] || 'default';
      const dataFile = process.argv[3];
      
      if (!dataFile) {
        console.error('Usage: node template-engine.js <template-name> <data-file.json>');
        process.exit(1);
      }
      
      const data = JSON.parse(await fs.readFile(dataFile, 'utf8'));
      const engine = new TemplateEngine();
      
      const rendered = await engine.generateFromTemplate(templateName, data);
      console.log(rendered);
      
    } catch (error) {
      console.error('Template rendering failed:', error.message);
      process.exit(1);
    }
  }
  
  main();
}