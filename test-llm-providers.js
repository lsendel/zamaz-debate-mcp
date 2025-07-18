#!/usr/bin/env node

const { chromium } = require('playwright');

async function testLLMProviders() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  try {
    console.log('🤖 Testing LLM Provider Integration...');
    console.log('=====================================');
    
    // Step 1: Login as admin
    console.log('📝 Logging in as admin...');
    await page.goto('http://localhost:3001/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();
    
    // Wait for redirect
    await page.waitForTimeout(2000);
    
    // Step 2: Navigate to debates
    console.log('🏛️ Navigating to debates page...');
    await page.goto('http://localhost:3001/debates');
    await page.waitForTimeout(2000);
    
    // Step 3: Open Create Debate dialog
    console.log('➕ Opening Create Debate dialog...');
    const createButton = await page.locator('button:has-text("Create Debate")');
    await createButton.click();
    await page.waitForTimeout(2000);
    
    // Step 4: Check if dialog opened
    const dialogVisible = await page.locator('[role="dialog"]').isVisible();
    console.log('📋 Create Debate dialog visible:', dialogVisible ? '✅' : '❌');
    
    if (dialogVisible) {
      // Step 5: Check for provider dropdowns
      console.log('\\n🔍 Checking LLM provider selection...');
      
      // Check if provider selects are present
      const providerSelects = await page.locator('label:has-text("Provider")').count();
      console.log('🎯 Provider selection controls found:', providerSelects);
      
      // Check if model selects are present
      const modelSelects = await page.locator('label:has-text("Model")').count();
      console.log('🎯 Model selection controls found:', modelSelects);
      
      // Step 6: Check if providers are loaded
      if (providerSelects > 0) {
        console.log('\\n📋 Testing provider dropdown options...');
        
        // Click on first provider dropdown
        const firstProviderSelect = await page.locator('label:has-text("Provider")').first();
        await firstProviderSelect.click();
        await page.waitForTimeout(1000);
        
        // Check if options are loaded
        const providerOptions = await page.locator('[role="option"]').count();
        console.log('🎯 Provider options available:', providerOptions);
        
        if (providerOptions > 0) {
          // Get the provider option texts
          const providerTexts = await page.locator('[role="option"]').allTextContents();
          console.log('🎯 Available providers:', providerTexts);
          
          // Select first provider
          await page.locator('[role="option"]').first().click();
          await page.waitForTimeout(1000);
          
          // Check if models are loaded after provider selection
          console.log('\\n🔧 Testing model dropdown after provider selection...');
          
          // Click on first model dropdown
          const firstModelSelect = await page.locator('label:has-text("Model")').first();
          await firstModelSelect.click();
          await page.waitForTimeout(1000);
          
          const modelOptions = await page.locator('[role="option"]').count();
          console.log('🎯 Model options available:', modelOptions);
          
          if (modelOptions > 0) {
            const modelTexts = await page.locator('[role="option"]').allTextContents();
            console.log('🎯 Available models:', modelTexts);
            console.log('✅ LLM provider and model selection working correctly!');
          } else {
            console.log('❌ No model options available after provider selection');
          }
        } else {
          console.log('❌ No provider options available');
        }
      } else {
        console.log('❌ No provider selection controls found');
      }
      
      // Step 7: Check for system prompt fields
      console.log('\\n📝 Checking system prompt fields...');
      const systemPromptFields = await page.locator('label:has-text("System Prompt")').count();
      console.log('📝 System prompt fields found:', systemPromptFields);
      
      // Step 8: Check for parameter controls
      console.log('\\n⚙️ Checking parameter controls...');
      const temperatureControls = await page.locator('text=Temperature').count();
      console.log('🌡️ Temperature controls found:', temperatureControls);
      
      const maxTokensControls = await page.locator('text=Max Tokens').count();
      console.log('🎯 Max tokens controls found:', maxTokensControls);
      
      // Take screenshot
      await page.screenshot({ path: 'screenshots/create-debate-dialog.png' });
      console.log('📸 Screenshot saved: create-debate-dialog.png');
      
    } else {
      console.log('❌ Create Debate dialog did not open');
    }
    
    console.log('\\n✅ LLM provider test completed!');
    
  } catch (error) {
    console.error('❌ Error during LLM provider test:', error);
    await page.screenshot({ path: 'screenshots/llm-provider-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testLLMProviders().catch(console.error);