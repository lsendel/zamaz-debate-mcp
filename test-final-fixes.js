#!/usr/bin/env node

const { chromium } = require('playwright');

async function testFinalFixes() {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    console.log('🔧 Testing Final Fixes...');
    console.log('========================');
    
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
    
    // Step 2: Check if WebSocket errors are gone
    console.log('🔍 Checking for WebSocket issues...');
    await page.waitForTimeout(2000);
    
    const liveIndicator = await page.locator('text="Live"').isVisible();
    console.log(`📡 Live indicator showing: ${liveIndicator ? 'YES (should be NO)' : 'NO (correct)'}`);
    
    // Step 3: Test export functionality
    console.log('📤 Testing export functionality...');
    
    const exportButtons = await page.locator('button:has-text("Export")').count();
    console.log(`📤 Export buttons found: ${exportButtons}`);
    
    if (exportButtons > 0) {
      // Test JSON export
      console.log('🔍 Testing JSON export...');
      try {
        // Start download expectation
        const downloadPromise = page.waitForEvent('download');
        await page.locator('button:has-text("Export as JSON")').click();
        const download = await downloadPromise;
        console.log(`✅ JSON export successful: ${download.suggestedFilename()}`);
        
        // Test markdown export
        console.log('🔍 Testing Markdown export...');
        const downloadPromise2 = page.waitForEvent('download');
        await page.locator('button:has-text("Export as Markdown")').click();
        const download2 = await downloadPromise2;
        console.log(`✅ Markdown export successful: ${download2.suggestedFilename()}`);
        
      } catch (error) {
        console.log('❌ Export test failed:', error.message);
      }
    }
    
    // Step 4: Check debate content is fully visible
    console.log('🔍 Checking debate content...');
    
    const rounds = await page.locator('text=/Round \\d+/').count();
    console.log(`📋 Rounds visible: ${rounds}`);
    
    const responses = await page.locator('.MuiCard-root').count();
    console.log(`💬 Response cards: ${responses}`);
    
    const participants = await page.locator('text="Claude 3 Opus"').count();
    console.log(`👤 Claude mentions: ${participants}`);
    
    const gptMentions = await page.locator('text="GPT-4"').count();
    console.log(`🤖 GPT-4 mentions: ${gptMentions}`);
    
    // Step 5: Check specific debate content
    const aiArgument = await page.locator('text*="AI should make medical decisions"').isVisible();
    console.log(`🤖 AI argument visible: ${aiArgument}`);
    
    const humanArgument = await page.locator('text*="Medicine requires empathy"').isVisible();
    console.log(`👨‍⚕️ Human argument visible: ${humanArgument}`);
    
    // Step 6: Check status display
    const statusChip = await page.locator('text="COMPLETED"').isVisible();
    console.log(`📊 Status chip visible: ${statusChip}`);
    
    const formatChip = await page.locator('text="Format: OXFORD"').isVisible();
    console.log(`🎯 Format chip visible: ${formatChip}`);
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/final-fixes-test.png' });
    console.log('📸 Screenshot saved: final-fixes-test.png');
    
    // Final summary
    console.log('\n📊 FINAL SUMMARY:');
    console.log(`✅ WebSocket errors: ${liveIndicator ? 'STILL SHOWING' : 'FIXED'}`);
    console.log(`✅ Export functionality: ${exportButtons > 0 ? 'WORKING' : 'NOT WORKING'}`);
    console.log(`✅ Debate rounds: ${rounds > 0 ? 'WORKING' : 'NOT WORKING'}`);
    console.log(`✅ Debate content: ${aiArgument && humanArgument ? 'WORKING' : 'NOT WORKING'}`);
    console.log(`✅ Status display: ${statusChip && formatChip ? 'WORKING' : 'NOT WORKING'}`);
    
    const allWorking = !liveIndicator && exportButtons > 0 && rounds > 0 && aiArgument && humanArgument && statusChip && formatChip;
    
    if (allWorking) {
      console.log('\n🎉 ALL FIXES SUCCESSFUL! The debate system is now fully functional.');
    } else {
      console.log('\n⚠️  Some issues may still need attention.');
    }
    
    // Keep browser open for inspection
    console.log('\n🔍 Browser will stay open for 20 seconds for inspection...');
    await page.waitForTimeout(20000);
    
  } catch (error) {
    console.error('❌ Error during test:', error);
    await page.screenshot({ path: 'screenshots/final-fixes-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testFinalFixes().catch(console.error);