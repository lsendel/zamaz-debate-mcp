#!/usr/bin/env node

const ConfigParser = require('./config-parser');
const path = require('path');

/**
 * Validate workflow issue configuration
 */
function validateConfiguration() {
  try {
    console.log('🔍 Validating workflow issue configuration...');
    
    const configPath = path.join(__dirname, '../config/workflow-issue-config.yml');
    const parser = new ConfigParser(configPath);
    
    // Load and validate configuration
    const config = parser.loadConfig();
    console.log('✅ Configuration loaded successfully');
    
    // Validate global configuration
    const globalConfig = parser.getGlobalConfig();
    console.log(`✅ Global configuration validated - enabled: ${globalConfig.enabled}`);
    
    // Validate workflow configurations
    const workflows = parser.getConfiguredWorkflows();
    console.log(`✅ Found ${workflows.length} configured workflows:`);
    
    for (const workflowName of workflows) {
      const workflowConfig = parser.getWorkflowConfig(workflowName);
      console.log(`   - ${workflowName}: severity=${workflowConfig.severity}, template=${workflowConfig.template}`);
      
      // Validate workflow is enabled
      if (!parser.isWorkflowEnabled(workflowName)) {
        console.log(`   ⚠️  Workflow "${workflowName}" is disabled`);
      }
    }
    
    // Validate notification configuration
    const notificationConfig = parser.getNotificationConfig();
    if (notificationConfig.enabled) {
      console.log('✅ Notifications are enabled');
    } else {
      console.log('⚠️  Notifications are disabled');
    }
    
    // Validate template configuration
    const templateConfig = parser.getTemplateConfig();
    console.log(`✅ Template configuration: default=${templateConfig.default_template || 'default'}`);
    
    console.log('\n🎉 Configuration validation completed successfully!');
    return true;
    
  } catch (error) {
    console.error('❌ Configuration validation failed:');
    console.error(error.message);
    process.exit(1);
  }
}

/**
 * Test workflow configuration retrieval
 */
function testWorkflowConfig() {
  try {
    console.log('\n🧪 Testing workflow configuration retrieval...');
    
    const parser = new ConfigParser();
    
    // Test some example workflows
    const testWorkflows = ['CI/CD Pipeline', 'Code Quality', 'Security Scanning', 'NonExistent Workflow'];
    
    for (const workflowName of testWorkflows) {
      const config = parser.getWorkflowConfig(workflowName);
      console.log(`\n📋 Configuration for "${workflowName}":`);
      console.log(`   - Enabled: ${config.enabled}`);
      console.log(`   - Severity: ${config.severity}`);
      console.log(`   - Template: ${config.template}`);
      console.log(`   - Assignees: ${config.assignees.join(', ') || 'none'}`);
      console.log(`   - Labels: ${config.labels.join(', ')}`);
      console.log(`   - Escalation threshold: ${config.escalation_threshold}`);
    }
    
    console.log('\n✅ Workflow configuration testing completed');
    
  } catch (error) {
    console.error('❌ Workflow configuration testing failed:');
    console.error(error.message);
  }
}

// Run validation if script is executed directly
if (require.main === module) {
  const args = process.argv.slice(2);
  
  if (args.includes('--test')) {
    validateConfiguration();
    testWorkflowConfig();
  } else {
    validateConfiguration();
  }
}

module.exports = { validateConfiguration, testWorkflowConfig };