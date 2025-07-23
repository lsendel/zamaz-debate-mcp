const ConfigParser = require('./config-parser');
const fs = require('fs');
const path = require('path');

/**
 * Test suite for configuration parser
 */
function runTests() {
  console.log('🧪 Running configuration parser tests...\n');
  
  let passed = 0;
  let failed = 0;
  
  function test(name, testFn) {
    try {
      testFn();
      console.log(`✅ ${name}`);
      passed++;
    } catch (error) {
      console.log(`❌ ${name}: ${error.message}`);
      failed++;
    }
  }
  
  // Test 1: Configuration loading
  test('Configuration loads successfully', () => {
    const parser = new ConfigParser();
    const config = parser.loadConfig();
    if (!config) throw new Error('Configuration not loaded');
    if (!config.global) throw new Error('Global configuration missing');
  });
  
  // Test 2: Global configuration retrieval
  test('Global configuration retrieval', () => {
    const parser = new ConfigParser();
    const globalConfig = parser.getGlobalConfig();
    if (typeof globalConfig.enabled !== 'boolean') {
      throw new Error('Global enabled field should be boolean');
    }
  });
  
  // Test 3: Workflow configuration retrieval
  test('Workflow configuration retrieval', () => {
    const parser = new ConfigParser();
    const workflowConfig = parser.getWorkflowConfig('CI/CD Pipeline');
    if (!workflowConfig.severity) throw new Error('Severity not found');
    if (!Array.isArray(workflowConfig.assignees)) throw new Error('Assignees should be array');
    if (!Array.isArray(workflowConfig.labels)) throw new Error('Labels should be array');
  });
  
  // Test 4: Non-existent workflow configuration
  test('Non-existent workflow uses defaults', () => {
    const parser = new ConfigParser();
    const workflowConfig = parser.getWorkflowConfig('Non-Existent Workflow');
    if (workflowConfig.severity !== 'medium') throw new Error('Should use default severity');
    if (workflowConfig.template !== 'default') throw new Error('Should use default template');
  });
  
  // Test 5: Workflow enabled check
  test('Workflow enabled check', () => {
    const parser = new ConfigParser();
    const isEnabled = parser.isWorkflowEnabled('CI/CD Pipeline');
    if (typeof isEnabled !== 'boolean') throw new Error('Should return boolean');
  });
  
  // Test 6: Get configured workflows
  test('Get configured workflows', () => {
    const parser = new ConfigParser();
    const workflows = parser.getConfiguredWorkflows();
    if (!Array.isArray(workflows)) throw new Error('Should return array');
    if (workflows.length === 0) throw new Error('Should have configured workflows');
  });
  
  // Test 7: Notification configuration
  test('Notification configuration retrieval', () => {
    const parser = new ConfigParser();
    const notificationConfig = parser.getNotificationConfig();
    if (typeof notificationConfig !== 'object') throw new Error('Should return object');
  });
  
  // Test 8: Template configuration
  test('Template configuration retrieval', () => {
    const parser = new ConfigParser();
    const templateConfig = parser.getTemplateConfig();
    if (typeof templateConfig !== 'object') throw new Error('Should return object');
  });
  
  // Test 9: Configuration validation
  test('Configuration validation', () => {
    const parser = new ConfigParser();
    parser.loadConfig(); // Should not throw if validation passes
  });
  
  // Test 10: Invalid severity validation
  test('Invalid severity validation', () => {
    // Create temporary invalid config
    const invalidConfig = {
      global: { enabled: true },
      workflows: {
        'Test Workflow': {
          severity: 'invalid-severity'
        }
      }
    };
    
    const parser = new ConfigParser();
    parser.config = invalidConfig;
    
    try {
      parser.validateConfig();
      throw new Error('Should have thrown validation error');
    } catch (error) {
      if (!error.message.includes('Invalid severity')) {
        throw new Error('Should validate severity');
      }
    }
  });
  
  console.log(`\n📊 Test Results: ${passed} passed, ${failed} failed`);
  
  if (failed > 0) {
    process.exit(1);
  } else {
    console.log('🎉 All tests passed!');
  }
}

// Run tests if script is executed directly
if (require.main === module) {
  runTests();
}

module.exports = { runTests };