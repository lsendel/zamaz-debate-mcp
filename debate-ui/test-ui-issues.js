const http = require('http');

// Test the UI endpoints
async function testEndpoints() {
  console.log('Testing UI endpoints...\n');
  
  // Test homepage
  console.log('1. Testing homepage (http://localhost:3001/)...');
  http.get('http://localhost:3001/', (res) => {
    console.log(`   Status: ${res.statusCode}`);
    console.log(`   Headers: ${JSON.stringify(res.headers['content-type'])}`);
  }).on('error', (err) => {
    console.error(`   Error: ${err.message}`);
  });
  
  // Wait a bit
  await new Promise(resolve => setTimeout(resolve, 1000));
  
  // Test API endpoints
  console.log('\n2. Testing debate API endpoint...');
  http.get('http://localhost:3001/api/debate/resources?uri=debate://debates', (res) => {
    console.log(`   Status: ${res.statusCode}`);
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
      try {
        const json = JSON.parse(data);
        console.log(`   Response: ${JSON.stringify(json, null, 2)}`);
      } catch (e) {
        console.log(`   Raw response: ${data}`);
      }
    });
  }).on('error', (err) => {
    console.error(`   Error: ${err.message}`);
  });
  
  // Wait a bit
  await new Promise(resolve => setTimeout(resolve, 1000));
  
  // Test LLM API endpoint
  console.log('\n3. Testing LLM API endpoint...');
  http.get('http://localhost:3001/api/llm/providers', (res) => {
    console.log(`   Status: ${res.statusCode}`);
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
      try {
        const json = JSON.parse(data);
        console.log(`   Response: ${JSON.stringify(json, null, 2)}`);
      } catch (e) {
        console.log(`   Raw response: ${data}`);
      }
    });
  }).on('error', (err) => {
    console.error(`   Error: ${err.message}`);
  });
}

testEndpoints();