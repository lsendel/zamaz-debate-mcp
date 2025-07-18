#!/usr/bin/env node

const { chromium } = require('playwright');

async function debugDebateDetail() {
  const browser = await chromium.launch({ headless: false }); // visible for debugging
  const page = await browser.newPage();
  
  try {
    console.log('🔍 Debugging Debate Detail Page...');
    console.log('===================================');
    
    // Listen for all console messages
    page.on('console', msg => {
      console.log(`[BROWSER ${msg.type().toUpperCase()}] ${msg.text()}`);
    });
    
    // Listen for page errors
    page.on('pageerror', error => {
      console.log(`[PAGE ERROR] ${error.message}`);
      console.log(`[PAGE ERROR STACK] ${error.stack}`);
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
    
    // Step 3: Click on first debate
    console.log('🖱️ Clicking on first debate...');
    const firstDebateCard = page.locator('.MuiCard-root').first();
    await firstDebateCard.click();
    await page.waitForTimeout(5000); // Wait longer for component to render
    
    // Step 4: Debug what's on the page
    console.log('🔍 Debugging page content...');
    
    // Check if the component is rendered
    const reactRoot = await page.locator('#root').innerHTML();
    console.log('📄 React root HTML length:', reactRoot.length);
    
    // Check for specific elements
    const headings = await page.locator('h1, h2, h3, h4, h5, h6').allTextContents();
    console.log('📋 Headings found:', headings);
    
    const chips = await page.locator('.MuiChip-root').allTextContents();
    console.log('🏷️ Chips found:', chips);
    
    const papers = await page.locator('.MuiPaper-root').count();
    console.log('📄 Paper components:', papers);
    
    const loadingIndicator = await page.locator('.MuiLinearProgress-root').isVisible();
    console.log('⏳ Loading indicator visible:', loadingIndicator);
    
    // Check if currentDebate data is loaded
    const currentDebateInfo = await page.evaluate(() => {
      // Try to access Redux store
      const store = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || window.store;
      if (store) {
        return 'Redux store found';
      }
      return 'No Redux store found';
    });
    console.log('🔍 Redux store info:', currentDebateInfo);
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/debug-debate-detail.png' });
    console.log('📸 Screenshot saved: debug-debate-detail.png');
    
    // Keep browser open for manual inspection
    console.log('🔍 Browser will stay open for 30 seconds for manual inspection...');
    await page.waitForTimeout(30000);
    
  } catch (error) {
    console.error('❌ Error during debug:', error);
    await page.screenshot({ path: 'screenshots/debug-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the debug
debugDebateDetail().catch(console.error);