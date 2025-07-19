#!/usr/bin/env node

const { chromium } = require('playwright');

async function testSpecificIssues() {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    console.log('🔍 Testing Specific Issues...');
    console.log('============================');
    
    // Listen for console errors
    page.on('console', msg => {
      if (msg.type() === 'error') {
        console.log(`[BROWSER ERROR] ${msg.text()}`);
      }
    });
    
    // Step 1: Login and navigate to debate
    console.log('📝 Logging in and navigating...');
    await page.goto('http://localhost:3001/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(2000);
    
    await page.goto('http://localhost:3001/debates');
    await page.waitForTimeout(2000);
    
    const firstDebateCard = page.locator('.MuiCard-root').first();
    await firstDebateCard.click();
    await page.waitForTimeout(4000);
    
    // Step 2: Check for specific issues
    console.log('🔍 Checking for specific issues...');
    
    // Check if participants section has duplicates
    const participantSection = page.locator('h6:has-text("Participants")').locator('..').locator('..');
    const participantNames = await participantSection.locator('h6').allTextContents();
    console.log(`👥 Participant names in section: ${participantNames.length}`);
    participantNames.forEach((name, index) => {
      console.log(`   ${index + 1}: "${name}"`);
    });
    
    // Check if system prompts are showing
    const systemPromptElements = await page.locator('text*="You are arguing"').count();
    console.log(`📝 System prompt elements: ${systemPromptElements}`);
    
    // Check if the "Live" indicator is showing (it shouldn't since WebSocket failed)
    const liveIndicator = await page.locator('text="Live"').isVisible();
    console.log(`📡 Live indicator showing: ${liveIndicator}`);
    
    // Check if Start button is showing for completed debate (it shouldn't)
    const startButton = await page.locator('button:has-text("Start")').isVisible();
    console.log(`▶️ Start button showing: ${startButton}`);
    
    // Check if the round/format chip is correct
    const formatChip = await page.locator('text="Format: OXFORD"').isVisible();
    console.log(`🎯 Format chip showing: ${formatChip}`);
    
    // Check if export buttons are working
    const exportButtons = await page.locator('button:has-text("Export")').count();
    console.log(`📤 Export buttons found: ${exportButtons}`);
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/specific-issues-test.png' });
    console.log('📸 Screenshot saved: specific-issues-test.png');
    
    // Test clicking on export
    if (exportButtons > 0) {
      console.log('🔍 Testing export functionality...');
      try {
        await page.locator('button:has-text("Export as JSON")').click();
        await page.waitForTimeout(2000);
        console.log('✅ Export button clicked successfully');
      } catch (error) {
        console.log('❌ Export button click failed:', error.message);
      }
    }
    
    // Keep browser open for inspection
    console.log('\n🔍 Browser will stay open for 30 seconds for inspection...');
    await page.waitForTimeout(30000);
    
  } catch (error) {
    console.error('❌ Error during test:', error);
    await page.screenshot({ path: 'screenshots/specific-issues-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testSpecificIssues().catch(console.error);