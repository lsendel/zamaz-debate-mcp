#!/usr/bin/env node;

const { chromium } = require('playwright');

async function testApiDirect() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  try {
    console.log('🌐 Testing Direct API Calls...');
    console.log('===============================');

    // Step 1: Navigate to app;
    await page.goto('http://localhost:3001');

    // Step 2: Test API calls from browser console;
    console.log('📡 Testing API calls from browser console...');

    // Test organization API;
    const orgResponse = await page.evaluate(async () => {
      try {
        const response = await fetch('/api/v1/organizations');
        return {
          status: response.status,
          data: await response.json();
        }
      } catch (error) {
        return { error: error.message }
      }
    });

    console.log('🏢 Organization API response:', orgResponse);

    // Test LLM providers API;
    const llmResponse = await page.evaluate(async () => {
      try {
        const response = await fetch('/api/v1/providers');
        return {
          status: response.status,
          data: await response.json();
        }
      } catch (error) {
        return { error: error.message }
      }
    });

    console.log('🤖 LLM API response:', llmResponse);

    // Test debate API;
    const debateResponse = await page.evaluate(async () => {
      try {
        const response = await fetch('/api/v1/debates');
        return {
          status: response.status,
          data: await response.json();
        }
      } catch (error) {
        return { error: error.message }
      }
    });

    console.log('🏛️ Debate API response:', debateResponse);

    if (llmResponse.data && Array.isArray(llmResponse.data)) {
      console.log('✅ LLM API is working correctly!');
      console.log('📋 Available providers:', llmResponse.data.map(p => p.name));
    } else {
      console.log('❌ LLM API is not working correctly');
    }

  } catch (error) {
    console.error('❌ Error during API test:', error);
  } finally {
    await browser.close();
  }
}

// Run the test
testApiDirect().catch(console.error);
