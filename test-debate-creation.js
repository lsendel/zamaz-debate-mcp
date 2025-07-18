#!/usr/bin/env node

const { chromium } = require('playwright');

async function testDebateCreation() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  try {
    console.log('🏛️ Testing Debate Creation...');
    console.log('==============================');
    
    // Listen for console messages
    page.on('console', msg => {
      if (msg.type() === 'error') {
        console.log(`[BROWSER ERROR] ${msg.text()}`);
      }
    });
    
    // Listen for network responses
    page.on('response', response => {
      if (response.url().includes('/api/v1/debates')) {
        console.log(`[NETWORK] ${response.url()} - Status: ${response.status()}`);
      }
    });
    
    // Step 1: Login
    console.log('📝 Logging in...');
    await page.goto('http://localhost:3001/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(2000);
    
    // Step 2: Navigate to debates
    console.log('🏛️ Navigating to debates...');
    await page.goto('http://localhost:3001/debates');
    await page.waitForTimeout(2000);
    
    // Step 3: Open Create Debate dialog
    console.log('➕ Opening Create Debate dialog...');
    await page.locator('button:has-text("Create Debate")').click();
    await page.waitForTimeout(3000);
    
    // Step 4: Fill in debate details
    console.log('📝 Filling debate details...');
    await page.locator('input[label="Topic"]').fill('Should AI be regulated?');
    await page.locator('textarea[label="Description"]').fill('A test debate about AI regulation policies');
    
    // Step 5: Wait for providers to load and check
    console.log('⏳ Waiting for providers to load...');
    await page.waitForTimeout(3000);
    
    // Step 6: Try to create the debate
    console.log('🚀 Creating debate...');
    const createButton = await page.locator('button:has-text("Create Debate")').last();
    await createButton.click();
    
    // Wait for the API call
    await page.waitForTimeout(5000);
    
    // Step 7: Check if we got an error or success
    const notifications = await page.locator('.MuiSnackbar-root, .MuiAlert-root').all();
    console.log(`📋 Found ${notifications.length} notifications`);
    
    for (let i = 0; i < notifications.length; i++) {
      const notification = notifications[i];
      const isVisible = await notification.isVisible();
      if (isVisible) {
        const text = await notification.textContent();
        console.log(`📢 Notification ${i}: "${text}"`);
      }
    }
    
    // Step 8: Check if dialog closed (success) or still open (error)
    const dialogStillOpen = await page.locator('[role="dialog"]').isVisible();
    console.log(`📋 Dialog still open: ${dialogStillOpen}`);
    
    if (!dialogStillOpen) {
      console.log('✅ Debate creation appears successful!');
      
      // Wait for page to reload or update
      await page.waitForTimeout(2000);
      
      // Check if we can see the new debate in the list
      const debateCards = await page.locator('.MuiCard-root').all();
      console.log(`📋 Found ${debateCards.length} debate cards`);
      
    } else {
      console.log('❌ Debate creation failed - dialog still open');
    }
    
    await page.screenshot({ path: 'screenshots/debate-creation-result.png' });
    console.log('📸 Screenshot saved: debate-creation-result.png');
    
  } catch (error) {
    console.error('❌ Error during debate creation test:', error);
    await page.screenshot({ path: 'screenshots/debate-creation-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testDebateCreation().catch(console.error);