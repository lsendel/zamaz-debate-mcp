#!/usr/bin/env node

const { chromium } = require('playwright');

async function quickTest() {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    // Enable console logs
    page.on('console', msg => {
      if (msg.type() === 'error') {
        console.log('❌ Browser error:', msg.text());
      }
    });
    
    // Login
    console.log('📝 Logging in...');
    await page.goto('http://localhost:3001/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(2000);
    
    // Go directly to IN_PROGRESS debate
    console.log('🔍 Opening IN_PROGRESS debate...');
    await page.goto('http://localhost:3001/debates/debate-002');
    await page.waitForTimeout(5000);
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/debate-detail-current.png', fullPage: true });
    console.log('📸 Screenshot saved: screenshots/debate-detail-current.png');
    
    // Check page content
    const pageContent = await page.content();
    console.log('\n📋 Page Analysis:');
    console.log('- Has "Debate Progress":', pageContent.includes('Debate Progress'));
    console.log('- Has "Live":', pageContent.includes('Live'));
    console.log('- Has "Round":', pageContent.includes('Round'));
    console.log('- Has MuiStepper:', pageContent.includes('MuiStepper'));
    console.log('- Has LinearProgress:', pageContent.includes('LinearProgress'));
    
    // Check for error messages
    const errorMessages = await page.locator('.MuiAlert-message').allTextContents();
    if (errorMessages.length > 0) {
      console.log('\n⚠️ Error messages found:', errorMessages);
    }
    
    // Wait a bit more to see if content loads
    console.log('\n⏳ Waiting for content to load...');
    await page.waitForTimeout(5000);
    
    // Check visible text
    const visibleText = await page.locator('body').innerText();
    console.log('\n📄 Page contains nuclear debate:', visibleText.includes('nuclear'));
    console.log('📄 Page contains climate:', visibleText.includes('climate'));
    
    console.log('\n✅ Test complete. Check the screenshot for visual confirmation.');
    
    // Keep open for manual inspection
    await page.waitForTimeout(15000);
    
  } catch (error) {
    console.error('❌ Error:', error);
  } finally {
    await browser.close();
  }
}

quickTest().catch(console.error);