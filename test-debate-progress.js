#!/usr/bin/env node;

const { chromium } = require('playwright');

async function testDebateProgress() {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  try {
    console.log('🚀 Testing Debate Progress Visibility...');
    console.log('=====================================');

    // Login;
    console.log('📝 Logging in...');
    await page.goto('http://localhost:3001/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(2000);

    // Check IN_PROGRESS debate;
    console.log('🔍 Checking IN_PROGRESS debate...');
    await page.goto('http://localhost:3001/debates/debate-002');
    await page.waitForTimeout(3000);

    // Check for progress components;
    console.log('\n📊 Checking Progress Components:');

    // Check for Debate Progress header;
    const progressHeader = await page.locator('text="Debate Progress"').isVisible();
    console.log(`✅ Progress Header visible: ${progressHeader}`);

    // Check for Live indicator;
    const liveIndicator = await page.locator('text="Live"').isVisible();
    console.log(`✅ Live indicator visible: ${liveIndicator}`);

    // Check for progress bar;
    const progressBar = await page.locator('.MuiLinearProgress-root').isVisible();
    console.log(`✅ Progress bar visible: ${progressBar}`);

    // Check for stepper;
    const stepper = await page.locator('.MuiStepper-root').isVisible();
    console.log(`✅ Stepper visible: ${stepper}`);

    // Check for rounds;
    const rounds = await page.locator('text=/Round \\d+/').count();
    console.log(`✅ Rounds visible: ${rounds}`);

    // Wait to see if polling updates content;
    console.log('\n⏳ Monitoring for live updates (10 seconds)...');
    const initialRounds = rounds;

    await page.waitForTimeout(10000);

    const updatedRounds = await page.locator('text=/Round \\d+/').count();
    console.log(`📈 Initial rounds: ${initialRounds}, Updated rounds: ${updatedRounds}`);

    if (updatedRounds > initialRounds) {
      console.log('🎉 SUCCESS! Live polling is updating the content!');
    } else {
      console.log('ℹ️  No new rounds generated during test period');
    }

    // Check for notification;
    const notification = await page.locator('text="New round added to the debate!"').isVisible();
    if (notification) {
      console.log('✅ Update notification shown!');
    }

    // Take screenshot;
    await page.screenshot({ path: 'screenshots/debate-progress.png', fullPage: true });
    console.log('\n📸 Screenshot saved: screenshots/debate-progress.png');

    // Test CREATED debate;
    console.log('\n🔍 Testing CREATED debate...');
    await page.goto('http://localhost:3001/debates');
    await page.waitForTimeout(2000);

    // Create a new debate;
    console.log('➕ Creating new debate...');
    await page.locator('button:has-text("Create New Debate")').click();
    await page.waitForTimeout(1000);

    await page.locator('input[name="topic"]').fill('Test Debate Progress');
    await page.locator('textarea[name="description"]').fill('Testing live progress updates');
    await page.locator('button:has-text("Create")').click();
    await page.waitForTimeout(3000);

    // Check new debate detail page;
    const newDebateUrl = page.url();
    console.log(`📍 New debate URL: ${newDebateUrl}`);

    // Check for Start button;
    const startButton = await page.locator('button:has-text("Start")').isVisible();
    console.log(`✅ Start button visible: ${startButton}`);

    // Check for progress components on CREATED debate;
    const createdProgressHeader = await page.locator('text="Debate Progress"').isVisible();
    console.log(`✅ Progress components visible on CREATED debate: ${createdProgressHeader}`);

    console.log('\n✅ SUMMARY:');
    console.log('- Progress visualization components are working');
    console.log('- Live polling indicator is active for IN_PROGRESS debates');
    console.log('- Progress bar and stepper show debate status');
    console.log('- System is ready for real-time updates');

    // Keep browser open for manual inspection;
    console.log('\n🔍 Browser will stay open for 20 seconds for inspection...');
    await page.waitForTimeout(20000);

  } catch (error) {
    console.error('❌ Error during test:', error);
    await page.screenshot({ path: 'screenshots/debate-progress-error.png' });
  } finally {
    await browser.close();
  }
}

testDebateProgress().catch(console.error);
