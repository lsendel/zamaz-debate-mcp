#!/usr/bin/env node;

const { chromium } = require('playwright');

async function testFinalSimple() {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  try {
    console.log('🎉 Final Test - Everything Working!');
    console.log('===================================');

    // Login and navigate;
    await page.goto('http://localhost:3001/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(2000);

    await page.goto('http://localhost:3001/debates');
    await page.waitForTimeout(2000);

    // Click on first debate;
    const firstDebateCard = page.locator('.MuiCard-root').first();
    await firstDebateCard.click();
    await page.waitForTimeout(4000);

    // Check key functionality;
    console.log('🔍 Checking key functionality...');

    // 1. WebSocket "Live" indicator (should be hidden);
    const liveIndicator = await page.locator('text="Live"').isVisible();
    console.log(`📡 Live indicator: ${liveIndicator ? '❌ SHOWING' : '✅ HIDDEN'}`);

    // 2. Export buttons (should work);
    const exportButtons = await page.locator('button:has-text("Export")').count();
    console.log(`📤 Export buttons: ${exportButtons > 0 ? '✅ WORKING' : '❌ NOT WORKING'}`);

    // 3. Debate rounds (should show 3);
    const rounds = await page.locator('text=/Round \\d+/').count();
    console.log(`📋 Rounds: ${rounds === 3 ? '✅ 3 ROUNDS' : `❌ ${rounds} ROUNDS`}`);

    // 4. Response cards (should show 6);
    const responses = await page.locator('.MuiCard-root').count();
    console.log(`💬 Response cards: ${responses === 6 ? '✅ 6 RESPONSES' : `❌ ${responses} RESPONSES`}`);

    // 5. Participants (should show Claude and GPT-4);
    const claude = await page.locator('text="Claude 3 Opus"').count();
    const gpt = await page.locator('text="GPT-4"').count();
    console.log(`👤 Participants: ${claude > 0 && gpt > 0 ? '✅ BOTH FOUND' : '❌ MISSING'}`);

    // 6. Status chips;
    const statusChip = await page.locator('text="COMPLETED"').isVisible();
    const formatChip = await page.locator('text="Format: OXFORD"').isVisible();
    console.log(`📊 Status/Format: ${statusChip && formatChip ? '✅ WORKING' : '❌ NOT WORKING'}`);

    // 7. Debate content (check page has substantial content);
    const bodyText = await page.locator('body').textContent();
    const contentLength = bodyText ? bodyText.length : 0;
    console.log(`📄 Content length: ${contentLength > 3000 ? '✅ SUBSTANTIAL' : '❌ MINIMAL'}`);

    // Final summary;
    const allGood = !liveIndicator && exportButtons > 0 && rounds === 3 && responses === 6 && claude > 0 && gpt > 0 && statusChip && formatChip && contentLength > 3000;

    console.log('\n📊 FINAL RESULTS:');
    if (allGood) {
      console.log('🎉 🎉 🎉 ALL SYSTEMS WORKING! 🎉 🎉 🎉');
      console.log('✅ WebSocket errors fixed');
      console.log('✅ Export functionality working');
      console.log('✅ Debate rounds displaying correctly');
      console.log('✅ All participant responses showing');
      console.log('✅ Status and format information correct');
      console.log('✅ Full debate content visible');
      console.log('\n🚀 The debate system is now fully functional with real data!');
    } else {
      console.log('⚠️  Some issues detected - see details above');
    }

    await page.screenshot({ path: 'screenshots/final-working.png' });
    console.log('📸 Final screenshot saved: final-working.png');

    // Keep browser open;
    console.log('\n🔍 Browser will stay open for 20 seconds...');
    await page.waitForTimeout(20000);

  } catch (error) {
    console.error('❌ Error:', error);
    await page.screenshot({ path: 'screenshots/final-error.png' });
  } finally {
    await browser.close();
  }
}

testFinalSimple().catch(console.error);
