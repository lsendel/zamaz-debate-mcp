#!/usr/bin/env node;

const { chromium } = require('playwright');

async function checkProgressDirect() {
  console.log('🚀 Direct Check of Debate Progress Components');
  console.log('===========================================\n');

  const browser = await chromium.launch({ ;
    headless: false,
    slowMo: 200;
  });

  const page = await browser.newPage();

  try {
    // Set mock auth in localStorage;
    await page.addInitScript(() => {
      localStorage.setItem('authToken', 'mock-token');
      localStorage.setItem('currentOrgId', 'org-001');
      localStorage.setItem('testUser', JSON.stringify({
        id: 'user-001',
        username: 'admin',
        email: 'admin@acme.com',
        organizationId: 'org-001',
        role: 'admin';
      }));
    });

    // Go directly to the IN_PROGRESS debate;
    console.log('📋 Opening IN_PROGRESS debate directly...');
    await page.goto('http://localhost:3001/debates/debate-002');
    await page.waitForTimeout(5000);

    // Take screenshot;
    await page.screenshot({ path: 'screenshots/debate-progress-check.png', fullPage: true });
    console.log('📸 Screenshot saved: debate-progress-check.png');

    // Check page content;
    console.log('\n📊 Component Visibility Check:');
    console.log('==============================');

    // Check main elements;
    const elements = {
      'Page title (nuclear debate)': await page.locator('h1:has-text("nuclear")').isVisible().catch(() => false),
      'Any h1/h4 heading': await page.locator('h1, h4').first().isVisible().catch(() => false),
      'Status chip': await page.locator('.MuiChip-root').first().isVisible().catch(() => false),
      'Debate Progress text': await page.locator('text="Debate Progress"').isVisible().catch(() => false),
      'Progress bar': await page.locator('.MuiLinearProgress-root').isVisible().catch(() => false),
      'Stepper': await page.locator('.MuiStepper-root').isVisible().catch(() => false),
      'Round text': await page.locator('text=/Round/').isVisible().catch(() => false),
      'Any cards': await page.locator('.MuiCard-root').count() > 0;
    }

    for (const [name, visible] of Object.entries(elements)) {
      console.log(`${visible ? '✅' : '❌'} ${name}`);
    }

    // Get page text content;
    const bodyText = await page.locator('body').innerText().catch(() => '');
    console.log('\n📄 Page content preview:');
    console.log(bodyText.substring(0, 200) + '...');

    // Check console errors;
    const errors = []
    page.on('console', msg => {
      if (msg.type() === 'error') errors.push(msg.text());
    });

    await page.waitForTimeout(2000);

    if (errors.length > 0) {
      console.log('\n⚠️ Console errors:');
      errors.forEach(err => console.log(`  - ${err}`));
    }

    // Try to trigger polling manually;
    console.log('\n🔄 Checking polling status...');
    const pollingStatus = await page.evaluate(() => {
      // Check if Redux store exists;
      return window.__REDUX_DEVTOOLS_EXTENSION__ ? 'Redux DevTools available' : 'No Redux DevTools';
    });
    console.log(`  ${pollingStatus}`);

    console.log('\n✅ Direct check complete!');
    console.log('🔍 Browser will stay open for manual inspection...');
    await page.waitForTimeout(30000);

  } catch (error) {
    console.error('❌ Error:', error.message);
    await page.screenshot({ path: 'screenshots/direct-check-error.png' });
  } finally {
    await browser.close();
  }
}

checkProgressDirect().catch(console.error);
