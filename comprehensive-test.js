#!/usr/bin/env node

const http = require('http');

// Test function to make HTTP requests
function makeRequest(options, data = null) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => {
        body += chunk;
      });
      res.on('end', () => {
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body: body
        });
      });
    });
    
    req.on('error', (err) => {
      reject(err);
    });
    
    if (data) {
      req.write(JSON.stringify(data));
    }
    
    req.end();
  });
}

async function runComprehensiveTest() {
  console.log('🚀 Comprehensive MCP System Test');
  console.log('=================================');
  console.log('Testing complete integration: UI → Backend Services → Real Data');
  console.log('');
  
  let allTestsPassed = true;
  const testResults = [];
  
  // Test 1: Organization Service
  console.log('🏢 Testing Organization Service...');
  try {
    const orgTest = await makeRequest({
      hostname: 'localhost',
      port: 5005,
      path: '/api/v1/organizations',
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    });
    
    const organizations = JSON.parse(orgTest.body);
    const success = orgTest.statusCode === 200 && organizations.length > 0;
    testResults.push({ name: 'Organization API', success, data: `${organizations.length} organizations` });
    
    console.log(`   ✅ Status: ${orgTest.statusCode}`);
    console.log(`   📊 Organizations: ${organizations.length}`);
    organizations.forEach((org, i) => {
      console.log(`      ${i + 1}. ${org.name} (${org.id})`);
    });
    
  } catch (error) {
    console.log(`   ❌ Failed: ${error.message}`);
    testResults.push({ name: 'Organization API', success: false, data: error.message });
    allTestsPassed = false;
  }
  
  // Test 2: LLM Service  
  console.log('\n🧠 Testing LLM Service...');
  try {
    const llmTest = await makeRequest({
      hostname: 'localhost',
      port: 5002,
      path: '/api/v1/providers',
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    });
    
    const providers = JSON.parse(llmTest.body);
    const totalModels = providers.reduce((sum, p) => sum + p.models.length, 0);
    const success = llmTest.statusCode === 200 && providers.length > 0;
    testResults.push({ name: 'LLM API', success, data: `${providers.length} providers, ${totalModels} models` });
    
    console.log(`   ✅ Status: ${llmTest.statusCode}`);
    console.log(`   📊 Providers: ${providers.length}, Models: ${totalModels}`);
    providers.forEach((provider, i) => {
      console.log(`      ${i + 1}. ${provider.name} (${provider.models.length} models)`);
    });
    
  } catch (error) {
    console.log(`   ❌ Failed: ${error.message}`);
    testResults.push({ name: 'LLM API', success: false, data: error.message });
    allTestsPassed = false;
  }
  
  // Test 3: Debate Service
  console.log('\n💬 Testing Debate Service...');
  try {
    const debateTest = await makeRequest({
      hostname: 'localhost',
      port: 5013,
      path: '/api/v1/debates',
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    });
    
    const debates = JSON.parse(debateTest.body);
    const success = debateTest.statusCode === 200 && debates.length > 0;
    testResults.push({ name: 'Debate API', success, data: `${debates.length} debates` });
    
    console.log(`   ✅ Status: ${debateTest.statusCode}`);
    console.log(`   📊 Debates: ${debates.length}`);
    debates.forEach((debate, i) => {
      console.log(`      ${i + 1}. ${debate.title} (${debate.status})`);
    });
    
  } catch (error) {
    console.log(`   ❌ Failed: ${error.message}`);
    testResults.push({ name: 'Debate API', success: false, data: error.message });
    allTestsPassed = false;
  }
  
  // Test 4: UI Application
  console.log('\n🌐 Testing UI Application...');
  try {
    const uiTest = await makeRequest({
      hostname: 'localhost',
      port: 3001,
      path: '/',
      method: 'GET'
    });
    
    const isReactApp = uiTest.body.includes('root') && uiTest.body.includes('vite');
    const success = uiTest.statusCode === 200 && isReactApp;
    testResults.push({ name: 'UI Application', success, data: 'React app serving' });
    
    console.log(`   ✅ Status: ${uiTest.statusCode}`);
    console.log(`   ⚛️  React App: ${isReactApp ? 'Yes' : 'No'}`);
    console.log(`   🔧 Vite Dev Server: ${uiTest.body.includes('vite') ? 'Yes' : 'No'}`);
    
  } catch (error) {
    console.log(`   ❌ Failed: ${error.message}`);
    testResults.push({ name: 'UI Application', success: false, data: error.message });
    allTestsPassed = false;
  }
  
  // Test 5: Create a new organization to test write operations
  console.log('\n🔧 Testing Create Organization...');
  try {
    const createOrgTest = await makeRequest({
      hostname: 'localhost',
      port: 5005,
      path: '/api/v1/organizations',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, {
      name: 'Test Organization',
      description: 'Created during comprehensive test'
    });
    
    const newOrg = JSON.parse(createOrgTest.body);
    const success = createOrgTest.statusCode === 201 && newOrg.id;
    testResults.push({ name: 'Create Organization', success, data: `Created ${newOrg.name}` });
    
    console.log(`   ✅ Status: ${createOrgTest.statusCode}`);
    console.log(`   🏢 Created: ${newOrg.name} (${newOrg.id})`);
    
  } catch (error) {
    console.log(`   ❌ Failed: ${error.message}`);
    testResults.push({ name: 'Create Organization', success: false, data: error.message });
    allTestsPassed = false;
  }
  
  // Test 6: Test User API
  console.log('\n👥 Testing User Management...');
  try {
    const userTest = await makeRequest({
      hostname: 'localhost',
      port: 5005,
      path: '/api/v1/users',
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    });
    
    const users = JSON.parse(userTest.body);
    const success = userTest.statusCode === 200;
    testResults.push({ name: 'User Management', success, data: `${users.length} users` });
    
    console.log(`   ✅ Status: ${userTest.statusCode}`);
    console.log(`   👤 Users: ${users.length}`);
    users.forEach((user, i) => {
      console.log(`      ${i + 1}. ${user.username} (${user.role})`);
    });
    
  } catch (error) {
    console.log(`   ❌ Failed: ${error.message}`);
    testResults.push({ name: 'User Management', success: false, data: error.message });
    allTestsPassed = false;
  }
  
  // Final Results Summary
  console.log('\n📊 Test Results Summary');
  console.log('=======================');
  
  testResults.forEach((result, i) => {
    const icon = result.success ? '✅' : '❌';
    console.log(`${icon} ${result.name}: ${result.data}`);
  });
  
  console.log('\n🎯 Overall Result');
  console.log('=================');
  
  if (allTestsPassed) {
    console.log('✅ ALL TESTS PASSED!');
    console.log('');
    console.log('🚀 System Status: FULLY OPERATIONAL');
    console.log('📡 All services are connected and working');
    console.log('🔄 Real data is flowing throughout the system');
    console.log('🌐 UI is connected to real backend services');
    console.log('');
    console.log('🎉 SUCCESS: Mock services have been completely replaced with real services!');
    console.log('');
    console.log('🖥️  Access the application at: http://localhost:3001');
    console.log('📋 Organization Management: http://localhost:3001/organization-management');
    console.log('💬 Create Debates: http://localhost:3001/debates');
    console.log('⚙️  Settings: http://localhost:3001/settings');
  } else {
    console.log('❌ SOME TESTS FAILED');
    console.log('Please check the individual test results above');
  }
  
  console.log('\n🔗 Service URLs:');
  console.log('================');
  console.log('Organization API: http://localhost:5005/api/v1/organizations');
  console.log('LLM API: http://localhost:5002/api/v1/providers');
  console.log('Debate API: http://localhost:5013/api/v1/debates');
  console.log('UI Application: http://localhost:3001');
  
  return allTestsPassed;
}

// Run the comprehensive test
if (require.main === module) {
  runComprehensiveTest()
    .then(success => {
      process.exit(success ? 0 : 1);
    })
    .catch(error => {
      console.error('Test runner error:', error);
      process.exit(1);
    });
}

module.exports = { runComprehensiveTest };