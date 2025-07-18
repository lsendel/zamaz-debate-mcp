#!/usr/bin/env node

const http = require('http');

console.log('Testing URI fix...\n');

// Test various potentially problematic URLs
const testUrls = [
  'http://localhost:3001/',
  'http://localhost:3001/favicon.ico',
  'http://localhost:3001/manifest.json',
  'http://localhost:3001/%PUBLIC_URL%/favicon.ico', // This would cause the error
  'http://localhost:3001/test%20space.html',
  'http://localhost:3001/test%2Fslash.html',
  'http://localhost:3001/test%25percent.html'
];

async function testUrl(url) {
  return new Promise((resolve) => {
    const parsedUrl = new URL(url);
    const options = {
      hostname: parsedUrl.hostname,
      port: parsedUrl.port,
      path: parsedUrl.pathname,
      method: 'GET'
    };

    const req = http.request(options, (res) => {
      console.log(`✅ ${url} - Status: ${res.statusCode}`);
      resolve({ url, status: res.statusCode });
    });

    req.on('error', (error) => {
      console.log(`❌ ${url} - Error: ${error.message}`);
      resolve({ url, error: error.message });
    });

    req.end();
  });
}

async function runTests() {
  console.log('Running URI tests...\n');
  
  for (const url of testUrls) {
    await testUrl(url);
    await new Promise(resolve => setTimeout(resolve, 100)); // Small delay
  }
  
  console.log('\n✅ Testing complete!');
  console.log('\nIf you see any 400 errors above, those URLs were properly handled instead of crashing the server.');
}

runTests();