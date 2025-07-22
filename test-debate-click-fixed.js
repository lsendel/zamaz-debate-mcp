#!/usr/bin/env node;

const { chromium } = require('playwright');

async function testDebateClick() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  try {
    console.log('🏛️ Testing Fixed Debate Click...');
    console.log('=================================');

    // Listen for console messages;
    page.on('console', msg => {
      if (msg.type() === 'error') {
        console.log(`[BROWSER ERROR] ${msg.text()}`);
      }
    });

    // Step 1: Login;
    console.log('📝 Logging in...');
    await page.goto('http://localhost:3001/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(2000);

    // Step 2: Navigate to debates;
    console.log('🏛️ Navigating to debates...');
    await page.goto('http://localhost:3001/debates');
    await page.waitForTimeout(2000);

    // Step 3: Click on first debate;
    console.log('🖱️ Clicking on first debate...');
    const firstDebateCard = page.locator('.MuiCard-root').first();
    await firstDebateCard.click();
    await page.waitForTimeout(3000);

    // Step 4: Check if page loaded correctly;
    console.log('📍 Checking debate detail page...');
    const currentUrl = page.url();
    console.log(`Current URL: ${currentUrl}`);

    // Check for content;
    const bodyText = await page.locator('body').textContent();
    const contentLength = bodyText ? bodyText.length : 0;
    console.log(`Page content length: ${contentLength}`);

    if (contentLength > 500) {
      console.log('✅ Page has substantial content');

      // Check for specific elements;
      const debateTitle = await page.locator('h1, h2, h3, h4').first().textContent();
      console.log(`Debate title: "${debateTitle}"`);

      const statusChip = await page.locator('.MuiChip-root').first().textContent();
      console.log(`Status chip: "${statusChip}"`);

      const participantsSection = await page.locator('text=Participants').isVisible();
      console.log(`Participants section visible: ${participantsSection}`);

      const debateProgress = await page.locator('text=Debate Progress').isVisible();
      console.log(`Debate progress section visible: ${debateProgress}`);

      const noRoundsMessage = await page.locator('text=No debate rounds yet').isVisible();
      console.log(`No rounds message visible: ${noRoundsMessage}`);

    } else {
      console.log('❌ Page appears to have minimal content');
    }

    // Take screenshot;
    await page.screenshot({ path: 'screenshots/debate-detail-fixed.png' });
    console.log('📸 Screenshot saved: debate-detail-fixed.png');

  } catch (error) {
    console.error('❌ Error during test:', error);
    await page.screenshot({ path: 'screenshots/debate-detail-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testDebateClick().catch(console.error);
