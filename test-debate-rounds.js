#!/usr/bin/env node

const { chromium } = require('playwright');

async function testDebateRounds() {
  const browser = await chromium.launch({ headless: false }); // visible to see results
  const page = await browser.newPage();
  
  try {
    console.log('🏛️ Testing Debate Rounds and Responses...');
    console.log('==========================================');
    
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
    
    // Step 3: Click on first debate (AI Ethics in Healthcare)
    console.log('🖱️ Clicking on first debate (AI Ethics in Healthcare)...');
    const firstDebateCard = page.locator('.MuiCard-root').first();
    await firstDebateCard.click();
    await page.waitForTimeout(3000);
    
    // Step 4: Check for debate rounds
    console.log('🔍 Checking for debate rounds...');
    
    // Check if we have round headings
    const roundHeadings = await page.locator('text=Round').allTextContents();
    console.log(`📋 Round headings found: ${roundHeadings.length}`);
    roundHeadings.forEach((heading, index) => {
      console.log(`   Round ${index + 1}: "${heading}"`);
    });
    
    // Check for response cards
    const responseCards = await page.locator('.MuiCard-root').count();
    console.log(`💬 Response cards found: ${responseCards}`);
    
    // Check for participant avatars
    const avatars = await page.locator('.MuiAvatar-root').count();
    console.log(`👤 Participant avatars found: ${avatars}`);
    
    // Check for specific content
    const hasAIContent = await page.locator('text=AI should make medical decisions').isVisible();
    console.log(`🤖 AI argument content visible: ${hasAIContent}`);
    
    const hasHumanContent = await page.locator('text=Medicine requires empathy').isVisible();
    console.log(`👨‍⚕️ Human argument content visible: ${hasHumanContent}`);
    
    // Check for token counts
    const tokenCounts = await page.locator('text=tokens').allTextContents();
    console.log(`📊 Token count displays: ${tokenCounts.length}`);
    
    // Check for timestamps
    const timestamps = await page.locator('text~=\\d{1,2}:\\d{2}').allTextContents();
    console.log(`⏰ Timestamps found: ${timestamps.length}`);
    
    // Check participants section
    const participantSection = await page.locator('text=Participants').isVisible();
    console.log(`👥 Participants section visible: ${participantSection}`);
    
    const claudeParticipant = await page.locator('text=Claude 3 Opus').isVisible();
    console.log(`🤖 Claude 3 Opus participant: ${claudeParticipant}`);
    
    const gptParticipant = await page.locator('text=GPT-4').isVisible();
    console.log(`🤖 GPT-4 participant: ${gptParticipant}`);
    
    // Check for system prompts
    const systemPrompts = await page.locator('text=You are arguing').count();
    console.log(`📝 System prompt texts: ${systemPrompts}`);
    
    // Take screenshot
    await page.screenshot({ path: 'screenshots/debate-rounds-test.png' });
    console.log('📸 Screenshot saved: debate-rounds-test.png');
    
    // Summary
    console.log('\n📊 SUMMARY:');
    console.log(`✅ Rounds: ${roundHeadings.length > 0 ? 'FOUND' : 'NOT FOUND'}`);
    console.log(`✅ Responses: ${responseCards > 0 ? 'FOUND' : 'NOT FOUND'}`);
    console.log(`✅ Participants: ${avatars > 0 ? 'FOUND' : 'NOT FOUND'}`);
    console.log(`✅ Content: ${hasAIContent && hasHumanContent ? 'FOUND' : 'NOT FOUND'}`);
    console.log(`✅ Timestamps: ${timestamps.length > 0 ? 'FOUND' : 'NOT FOUND'}`);
    
    if (roundHeadings.length > 0 && responseCards > 0) {
      console.log('\n🎉 SUCCESS: Debate rounds and responses are displaying correctly!');
    } else {
      console.log('\n❌ ISSUE: Debate rounds or responses are not displaying properly.');
    }
    
    // Keep browser open for inspection
    console.log('\n🔍 Browser will stay open for 30 seconds for inspection...');
    await page.waitForTimeout(30000);
    
  } catch (error) {
    console.error('❌ Error during test:', error);
    await page.screenshot({ path: 'screenshots/debate-rounds-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testDebateRounds().catch(console.error);