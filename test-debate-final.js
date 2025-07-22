#!/usr/bin/env node;

const { chromium } = require('playwright');

async function testDebateFinal() {
  const browser = await chromium.launch({ headless: false }); // visible;
  const page = await browser.newPage();

  try {
    console.log('🎉 Testing Final Debate Implementation...');
    console.log('=======================================');

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

    // Step 3: Click on first debate ;
    console.log('🖱️ Clicking on AI Ethics debate...');
    const firstDebateCard = page.locator('.MuiCard-root').first();
    await firstDebateCard.click();
    await page.waitForTimeout(4000); // Wait for data to load;

    // Step 4: Check for actual debate content;
    console.log('🔍 Checking for debate content...');

    // Wait for the page to fully load;
    await page.waitForTimeout(2000);

    // Check the page content;
    const bodyText = await page.locator('body').textContent();
    console.log(`📄 Page content length: ${bodyText ? bodyText.length : 0}`);

    // Look for round indicators;
    const roundElements = await page.locator('text=/Round \\d+/').allTextContents();
    console.log(`📋 Round indicators found: ${roundElements.length}`);
    roundElements.forEach((round, index) => {
      console.log(`   ${index + 1}: "${round}"`);
    });

    // Look for participant responses;
    const aiResponse = await page.locator('text=AI should make medical decisions').isVisible();
    console.log(`🤖 AI response visible: ${aiResponse}`);

    const humanResponse = await page.locator('text=Medicine requires empathy').isVisible();
    console.log(`👨‍⚕️ Human response visible: ${humanResponse}`);

    // Look for specific debate content;
    const diagnosticContent = await page.locator('text=diagnostic systems').isVisible();
    console.log(`🔬 Diagnostic content visible: ${diagnosticContent}`);

    const doctorPatientContent = await page.locator('text=doctor-patient relationship').isVisible();
    console.log(`👥 Doctor-patient content visible: ${doctorPatientContent}`);

    // Check participants section;
    const claude = await page.locator('text=Claude 3 Opus').isVisible();
    console.log(`🤖 Claude 3 Opus participant: ${claude}`);

    const gpt4 = await page.locator('text=GPT-4').isVisible();
    console.log(`🤖 GPT-4 participant: ${gpt4}`);

    // Check for system prompts;
    const systemPromptFor = await page.locator('text=arguing FOR AI').isVisible();
    console.log(`📝 System prompt FOR visible: ${systemPromptFor}`);

    const systemPromptAgainst = await page.locator('text=arguing AGAINST AI').isVisible();
    console.log(`📝 System prompt AGAINST visible: ${systemPromptAgainst}`);

    // Take screenshot;
    await page.screenshot({ path: 'screenshots/debate-final-test.png' });
    console.log('📸 Screenshot saved: debate-final-test.png');

    // Summary check;
    const hasRounds = roundElements.length > 0;
    const hasResponses = aiResponse && humanResponse;
    const hasParticipants = claude && gpt4;
    const hasSystemPrompts = systemPromptFor && systemPromptAgainst;

    console.log('\n📊 FINAL RESULTS:');
    console.log(`✅ Round displays: ${hasRounds ? 'WORKING' : 'NOT WORKING'}`);
    console.log(`✅ Response content: ${hasResponses ? 'WORKING' : 'NOT WORKING'}`);
    console.log(`✅ Participants: ${hasParticipants ? 'WORKING' : 'NOT WORKING'}`);
    console.log(`✅ System prompts: ${hasSystemPrompts ? 'WORKING' : 'NOT WORKING'}`);

    if (hasRounds && hasResponses && hasParticipants) {
      console.log('\n🎉 SUCCESS: Debate details are now working with real data!');
      console.log('🔍 You can now see the actual debate discussion and rounds.');
    } else {
      console.log('\n❌ ISSUE: Some debate features are still not working.');
    }

    // Keep browser open for inspection;
    console.log('\n🔍 Browser will stay open for 30 seconds for inspection...');
    await page.waitForTimeout(30000);

  } catch (error) {
    console.error('❌ Error during test:', error);
    await page.screenshot({ path: 'screenshots/debate-final-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testDebateFinal().catch(console.error);
