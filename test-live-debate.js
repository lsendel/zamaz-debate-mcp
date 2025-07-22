#!/usr/bin/env node

const { chromium } = require('playwright');

// TODO: Extract helper functions to reduce complexity
// Consider extracting: loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic
async function testLiveDebate() {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    console.log('🚀 Testing Live Debate Generation...');
    console.log('===================================');
    
    // Login and navigate
    await page.goto('http://localhost:3001/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(2000);
    
    await page.goto('http://localhost:3001/debates');
    await page.waitForTimeout(2000);
    
    console.log('🎯 Looking for a debate to start...');
    
    // Look for a debate with "CREATED" status to start
    const debateCards = await page.locator('.MuiCard-root').all();
    let foundCreatedDebate = false;
    
    for (let item of debateCards)
      const card = debateCards[i];
      const cardText = await card.textContent();
      
      if (cardText && cardText.includes('CREATED')) {
        console.log(`✅ Found CREATED debate, clicking on it...`);
        await card.click();
        foundCreatedDebate = true;
        break;
      }
    }
    
    if (!foundCreatedDebate) {
      console.log('❌ No CREATED debate found to start');
      return;
    }
    
    await page.waitForTimeout(3000);
    
    // Look for Start button
    console.log('🔍 Looking for Start button...');
    const startButton = await page.locator('button:has-text("Start")').isVisible();
    console.log(`▶️ Start button visible: ${startButton}`);
    
    if (startButton) {
      console.log('🚀 Starting the debate...');
      await page.locator('button:has-text("Start")').click();
      await page.waitForTimeout(2000);
      
      console.log('⏳ Waiting for debate generation (this may take 15-30 seconds)...');
      
      // Wait and check for progress
      for (let i = 0; i < 10; i++) {
        await page.waitForTimeout(3000);
        
        // Refresh the page to see new content
        await page.reload();
        await page.waitForTimeout(2000);
        
        // Check for rounds
        const rounds = await page.locator('text=/Round \\d+/').count();
        console.log(`📋 Round ${i * 3 + 3}s: ${rounds} rounds found`);
        
        if (rounds > 0) {
          console.log('✅ Rounds generated! Checking content...');
          
          // Check for realistic content
          const bodyText = await page.locator('body').textContent();
          const hasAutomation = bodyText?.includes('automation') || false;
          const hasJobs = bodyText?.includes('jobs') || false;
          const hasEvidence = bodyText?.includes('evidence') || false;
          
          console.log(`🔍 Content check:`);
          console.log(`   - Contains "automation": ${hasAutomation}`);
          console.log(`   - Contains "jobs": ${hasJobs}`);
          console.log(`   - Contains "evidence": ${hasEvidence}`);
          
          if (hasAutomation && hasJobs) {
            console.log('🎉 SUCCESS! Realistic debate content generated!');
            break;
          }
        }
      }
      
      // Final check
      const finalRounds = await page.locator('text=/Round \\d+/').count();
      const finalResponses = await page.locator('.MuiCard-root').count();
      
      console.log('\n📊 FINAL RESULTS:');
      console.log(`✅ Rounds generated: ${finalRounds}`);
      console.log(`✅ Response cards: ${finalResponses}`);
      
      if (finalRounds >= 3 && finalResponses >= 6) {
        console.log('🎉 🎉 LIVE DEBATE GENERATION WORKING! 🎉 🎉');
        console.log('✅ The system now generates real debate content in real-time!');
      } else {
        console.log('⚠️ Some issues with debate generation');
      }
      
    } else {
      console.log('❌ No Start button found');
    }
    
    await page.screenshot({ path: 'screenshots/live-debate-test.png' });
    console.log('📸 Screenshot saved: live-debate-test.png');
    
    // Keep browser open for inspection
    console.log('\n🔍 Browser will stay open for 30 seconds for inspection...');
    await page.waitForTimeout(30000);
    
  } catch (error) {
    console.error('❌ Error during test:', error);
    await page.screenshot({ path: 'screenshots/live-debate-error.png' });
  } finally {
    await browser.close();
  }
}

testLiveDebate().catch(console.error);