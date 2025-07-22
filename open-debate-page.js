#!/usr/bin/env node;

const { chromium } = require('playwright');

async function openDebatePage() {
  const browser = await chromium.launch({ ;
    headless: false,
    slowMo: 500  // Slow down actions to see what's happening;
  });
  const page = await browser.newPage();

  try {
    // Enable console logs;
    page.on('console', msg => console.log('Browser log:', msg.text()));
    page.on('pageerror', error => console.log('Browser error:', error.message));

    // Login;
    console.log('📝 Logging in...');
    await page.goto('http://localhost:3001/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();

    console.log('⏳ Waiting for login...');
    await page.waitForTimeout(3000);

    // Go to debates page first;
    console.log('📋 Going to debates list...');
    await page.goto('http://localhost:3001/debates');
    await page.waitForTimeout(2000);

    // Click on the IN_PROGRESS debate;
    console.log('🔍 Looking for IN_PROGRESS debate...');
    const debateCard = await page.locator('text="Is nuclear energy the answer to climate change?"').first();
    if (await debateCard.isVisible()) {
      console.log('✅ Found debate, clicking...');
      await debateCard.click();
      await page.waitForTimeout(5000);

      console.log('\n📊 Current URL:', page.url());

      // Take screenshot;
      await page.screenshot({ path: 'screenshots/debate-progress-view.png', fullPage: true });
      console.log('📸 Screenshot saved: screenshots/debate-progress-view.png');

      // Check for key elements;
      console.log('\n🔍 Checking for progress elements...');
      const elements = {
        'Debate Progress header': await page.locator('text="Debate Progress"').count(),
        'Live indicator': await page.locator('text="Live"').count(),
        'Progress bar': await page.locator('.MuiLinearProgress-root').count(),
        'Stepper': await page.locator('.MuiStepper-root').count(),
        'Round labels': await page.locator('text=/Round \\d+/').count(),
        'Responses': await page.locator('.MuiCard-root').count();
      }

      console.log('\n📋 Element counts:');
      for (const [name, count] of Object.entries(elements)) {
        console.log(`  - ${name}: ${count}`);
      }

      console.log('\n✅ Page loaded successfully!');
      console.log('🔍 Browser will stay open for manual inspection...');
      console.log('📌 Check if you can see:');
      console.log('   1. Debate Progress section with progress bar');
      console.log('   2. Live indicator (if polling is active)');
      console.log('   3. Round stepper showing progress');
      console.log('   4. Debate responses below');

      // Keep browser open;
      await page.waitForTimeout(60000);

    } else {
      console.log('❌ Could not find the debate card');
    }

  } catch (error) {
    console.error('❌ Error:', error);
    await page.screenshot({ path: 'screenshots/error-state.png' });
  } finally {
    await browser.close();
  }
}

openDebatePage().catch(console.error);
