#!/usr/bin/env node;

const { chromium } = require('playwright');

// TODO: Extract helper functions to reduce complexity
// Consider extracting: loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic
async function testDebateDetails() {
  const browser = await chromium.launch({ headless: false }); // visible for debugging;
  const page = await browser.newPage();

  try {
    console.log('🏛️ Testing Debate Details View...');
    console.log('==================================');

    // Listen for console messages;
    page.on('console', msg => {
      console.log(`[BROWSER ${msg.type().toUpperCase()}] ${msg.text()}`);
    });

    // Listen for network responses;
    page.on('response', response => {
      if (response.url().includes('/api/v1/')) {
        console.log(`[NETWORK] ${response.url()} - Status: ${response.status()}`);
      }
    });

    // Listen for page errors;
    page.on('pageerror', error => {
      console.log(`[PAGE ERROR] ${error.message}`);
    });

    // Step 1: Login;
    console.log('📝 Logging in...');
    await page.goto('http://localhost:3001/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(2000);

    // Step 2: Navigate to debates;
    console.log('🏛️ Navigating to debates page...');
    await page.goto('http://localhost:3001/debates');
    await page.waitForTimeout(3000);

    // Step 3: Check if debates are loaded;
    console.log('📋 Checking debate list...');
    const debateCards = await page.locator('.MuiCard-root').all();
    console.log(`Found ${debateCards.length} debate cards`);

    // Step 4: Try to click on a debate;
    if (debateCards.length > 0) {
      console.log('🖱️ Clicking on first debate...');
      const firstDebate = debateCards[0]

      // Check if it's clickable;
      const isClickable = await firstDebate.locator('button, [role="button"], a').count();
      console.log(`Clickable elements in first debate: ${isClickable}`);

      // Try different ways to click;
      try {
        // Try clicking on the card itself;
        await firstDebate.click();
        console.log('✅ Clicked on debate card');
      } catch (error) {
        console.log('❌ Failed to click on debate card:', error.message);

        // Try clicking on a button inside the card;
        const buttons = await firstDebate.locator('button').all();
        if (buttons.length > 0) {
          console.log(`Trying to click on button inside card (${buttons.length} buttons found)`);
          await buttons[0].click();
          console.log('✅ Clicked on button inside card');
        }
      }

      // Wait for navigation or modal;
      await page.waitForTimeout(3000);

      // Step 5: Check what happened after click;
      console.log('📍 Checking current state after click...');
      const currentUrl = page.url();
      console.log(`Current URL: ${currentUrl}`);

      // Check for modal/dialog;
      const modalVisible = await page.locator('[role="dialog"], .MuiModal-root').isVisible();
      console.log(`Modal visible: ${modalVisible}`);

      // Check for loading states;
      const loadingIndicators = await page.locator('.MuiCircularProgress-root, [role="progressbar"]').count();
      console.log(`Loading indicators: ${loadingIndicators}`);

      // Check for error messages;
      const errorMessages = await page.locator('.MuiAlert-root, .error, [role="alert"]').all();
      console.log(`Error messages found: ${errorMessages.length}`);

      for (let i = 0; i < errorMessages.length; i++) {
        const error = errorMessages[i]
        const isVisible = await error.isVisible();
        if (isVisible) {
          const text = await error.textContent();
          console.log(`❌ Error ${i}: "${text}"`);
        }
      }

      // Check page content;
      const bodyText = await page.locator('body').textContent();
      const contentLength = bodyText ? bodyText.length : 0;
      console.log(`Page content length: ${contentLength}`);

      if (contentLength < 100) {
        console.log('❌ Page appears to be blank or has minimal content');
      } else {
        console.log('✅ Page has content');
      }

      // Look for specific debate-related elements;
      const debateTitle = await page.locator('h1, h2, h3').first().textContent();
      console.log(`Page title/heading: "${debateTitle}"`);

    } else {
      console.log('❌ No debate cards found to click');
    }

    // Take screenshots;
    await page.screenshot({ path: 'screenshots/debate-details-test.png' });
    console.log('📸 Screenshot saved: debate-details-test.png');

    // Keep browser open for inspection;
    console.log('🔍 Browser will stay open for 15 seconds for inspection...');
    await page.waitForTimeout(15000);

  } catch (error) {
    console.error('❌ Error during debate details test:', error);
    await page.screenshot({ path: 'screenshots/debate-details-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testDebateDetails().catch(console.error);
