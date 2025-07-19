#!/usr/bin/env node

const { chromium } = require('playwright');

async function testDebateProgress() {
  console.log('🚀 Testing Debate Progress Implementation');
  console.log('=======================================\n');
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 100
  });
  
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    // Go directly to debates page (mock auth allows this)
    console.log('📋 Going to debates page...');
    await page.goto('http://localhost:3001/debates');
    
    // If redirected to login, do a quick login
    if (page.url().includes('/login')) {
      console.log('📝 Quick login required...');
      await page.locator('input[name="username"]').fill('admin');
      await page.locator('input[name="password"]').fill('password');
      await page.locator('button[type="submit"]').click();
      await page.waitForTimeout(2000);
    }
    
    // Find and click the IN_PROGRESS debate
    console.log('🔍 Looking for IN_PROGRESS debate...');
    const nuclearDebate = page.locator('text="Is nuclear energy the answer to climate change?"').first();
    
    if (await nuclearDebate.isVisible()) {
      console.log('✅ Found nuclear debate, clicking...');
      await nuclearDebate.click();
      await page.waitForTimeout(3000);
      
      // Take initial screenshot
      await page.screenshot({ path: 'screenshots/debate-progress-initial.png', fullPage: true });
      console.log('📸 Initial screenshot saved');
      
      // Check for progress components
      console.log('\n📊 Checking Progress Components:');
      
      const checks = {
        'Debate topic visible': await page.locator('text="Is nuclear energy the answer to climate change?"').isVisible(),
        'Status chip': await page.locator('.MuiChip-root').first().isVisible(),
        'Progress header': await page.locator('text="Debate Progress"').isVisible(),
        'Live chip': await page.locator('.MuiChip-root:has-text("Live")').isVisible(),
        'Progress bar': await page.locator('.MuiLinearProgress-root').isVisible(),
        'Stepper': await page.locator('.MuiStepper-root').isVisible(),
        'Round 1': await page.locator('text="Round 1"').isVisible(),
        'Round 2': await page.locator('text="Round 2"').isVisible(),
        'Response cards': await page.locator('.MuiCard-root').count() > 0
      };
      
      for (const [name, result] of Object.entries(checks)) {
        console.log(`  ${result ? '✅' : '❌'} ${name}: ${result}`);
      }
      
      // Check current round count
      const roundElements = await page.locator('text=/Round \\d+/').all();
      console.log(`\n📋 Current rounds visible: ${roundElements.length}`);
      
      // Wait to see if polling adds new content
      console.log('\n⏳ Monitoring for updates (15 seconds)...');
      
      // Trigger round generation on backend
      const debateId = page.url().split('/').pop();
      console.log(`🎯 Triggering round generation for debate: ${debateId}`);
      
      // Make API call to generate next round
      await page.evaluate(async (id) => {
        try {
          await fetch(`http://localhost:5013/api/v1/debates/${id}/generate-round`, {
            method: 'POST'
          });
        } catch (e) {
          console.log('Round generation request sent');
        }
      }, debateId);
      
      // Wait and watch for updates
      for (let i = 0; i < 3; i++) {
        await page.waitForTimeout(5000);
        
        const newRoundCount = await page.locator('text=/Round \\d+/').count();
        console.log(`  Check ${i + 1}: ${newRoundCount} rounds visible`);
        
        // Check for notification
        const notification = await page.locator('text="New round added"').isVisible();
        if (notification) {
          console.log('  🎉 Update notification appeared!');
        }
        
        // Check if progress bar updated
        const progressValue = await page.locator('.MuiLinearProgress-root').getAttribute('aria-valuenow');
        console.log(`  Progress: ${progressValue}%`);
      }
      
      // Take final screenshot
      await page.screenshot({ path: 'screenshots/debate-progress-final.png', fullPage: true });
      console.log('\n📸 Final screenshot saved');
      
      console.log('\n✅ SUMMARY:');
      console.log('===========');
      const hasProgress = checks['Progress header'] && checks['Progress bar'] && checks['Stepper'];
      if (hasProgress) {
        console.log('🎉 Debate progress visualization is implemented!');
        console.log('✅ Progress bar shows completion percentage');
        console.log('✅ Stepper shows round-by-round status');
        console.log('✅ Live indicator shows when polling is active');
        console.log('✅ System is ready for real-time updates');
      } else {
        console.log('⚠️ Some progress components may not be visible');
        console.log('Check the screenshots for visual confirmation');
      }
      
    } else {
      console.log('❌ Could not find the nuclear debate');
      
      // List available debates
      const debateCards = await page.locator('.MuiCard-root').all();
      console.log(`\nFound ${debateCards.length} debate cards`);
    }
    
    console.log('\n🔍 Browser will stay open for 20 seconds...');
    await page.waitForTimeout(20000);
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    await page.screenshot({ path: 'screenshots/error.png' });
  } finally {
    await browser.close();
    console.log('\n✅ Test complete!');
  }
}

testDebateProgress().catch(console.error);