const puppeteer = require('puppeteer');

async function testDebateCreation() {
  console.log('🧪 Testing Debate Creation Flow\n');
  
  const browser = await puppeteer.launch({
    headless: false, // Show browser so we can see what happens
    slowMo: 100, // Slow down actions to observe
    devtools: true // Open devtools to see network requests
  });
  
  try {
    const page = await browser.newPage();
    
    // Monitor console logs
    page.on('console', msg => {
      console.log('Browser console:', msg.type(), msg.text());
    });
    
    // Monitor network requests
    page.on('request', request => {
      if (request.url().includes('/api/')) {
        console.log('🌐 API Request:', request.method(), request.url());
      }
    });
    
    page.on('response', response => {
      if (response.url().includes('/api/')) {
        console.log('📥 API Response:', response.status(), response.url());
      }
    });
    
    // Step 1: Navigate to the app
    console.log('1️⃣ Navigating to app...');
    await page.goto('http://localhost:3001', { waitUntil: 'networkidle2' });
    await page.waitForTimeout(2000);
    
    // Step 2: Check current debates
    console.log('\n2️⃣ Checking current debates...');
    const debateCount = await page.evaluate(() => {
      const cards = document.querySelectorAll('[class*="CardTitle"]');
      console.log('Found debate cards:', cards.length);
      cards.forEach(card => console.log('Debate:', card.textContent));
      return cards.length;
    });
    console.log(`Current debates: ${debateCount}`);
    
    // Step 3: Click New Debate button
    console.log('\n3️⃣ Clicking New Debate button...');
    const newDebateBtn = await page.evaluateHandle(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const btn = buttons.find(b => b.textContent?.includes('New Debate'));
      if (btn) {
        console.log('Found New Debate button');
        return btn;
      }
      throw new Error('New Debate button not found');
    });
    
    await newDebateBtn.click();
    await page.waitForTimeout(1000);
    
    // Step 4: Check if dialog opened
    console.log('\n4️⃣ Checking if dialog opened...');
    const dialogExists = await page.evaluate(() => {
      const dialog = document.querySelector('[role="dialog"]');
      if (dialog) {
        console.log('Dialog found');
        const title = dialog.querySelector('h2')?.textContent;
        console.log('Dialog title:', title);
        return true;
      }
      return false;
    });
    
    if (!dialogExists) {
      throw new Error('Create Debate dialog did not open');
    }
    
    // Step 5: Fill in debate details
    console.log('\n5️⃣ Filling debate details...');
    
    // Fill debate name
    await page.type('input[id="name"]', 'Test Debate from Puppeteer');
    
    // Fill topic
    await page.type('input[id="topic"]', 'Should AI be regulated?');
    
    // Check for subject field (if it exists)
    const hasSubject = await page.$('input[id="subject"]');
    if (hasSubject) {
      await page.type('input[id="subject"]', 'AI Ethics');
    }
    
    // Fill description
    const descriptionField = await page.$('textarea[id="description"]');
    if (descriptionField) {
      await page.type('textarea[id="description"]', 'Testing debate creation');
    }
    
    // Step 6: Check participants
    console.log('\n6️⃣ Checking participants...');
    const participantCount = await page.evaluate(() => {
      const cards = document.querySelectorAll('[class*="Card"]');
      let count = 0;
      cards.forEach(card => {
        if (card.textContent?.includes('Participant')) {
          count++;
        }
      });
      console.log('Participant count:', count);
      return count;
    });
    
    if (participantCount < 2) {
      console.log('Adding participants...');
      const addParticipantBtn = await page.evaluateHandle(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        return buttons.find(b => b.textContent?.includes('Add Participant'));
      });
      
      if (addParticipantBtn) {
        await addParticipantBtn.click();
        await page.waitForTimeout(500);
      }
    }
    
    // Step 7: Find and click submit button
    console.log('\n7️⃣ Looking for submit button...');
    const submitButton = await page.evaluateHandle(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      // Try different button texts
      const possibleTexts = ['Create Debate', 'Create & Start Debate', 'Submit'];
      
      for (const text of possibleTexts) {
        const btn = buttons.find(b => b.textContent?.includes(text));
        if (btn) {
          console.log('Found submit button with text:', btn.textContent);
          return btn;
        }
      }
      
      console.log('Available buttons:', buttons.map(b => b.textContent));
      throw new Error('Submit button not found');
    });
    
    // Monitor for the API call
    let apiCalled = false;
    page.on('request', request => {
      if (request.url().includes('create_debate') || request.url().includes('debates')) {
        apiCalled = true;
        console.log('✅ Debate creation API called!');
      }
    });
    
    await submitButton.click();
    await page.waitForTimeout(3000);
    
    // Step 8: Check if debate was created
    console.log('\n8️⃣ Checking if debate was created...');
    
    // Check if dialog closed
    const dialogClosed = await page.evaluate(() => {
      return !document.querySelector('[role="dialog"]');
    });
    
    console.log('Dialog closed:', dialogClosed);
    console.log('API was called:', apiCalled);
    
    // Check new debate count
    const newDebateCount = await page.evaluate(() => {
      const cards = document.querySelectorAll('[class*="CardTitle"]');
      return cards.length;
    });
    
    console.log(`New debate count: ${newDebateCount}`);
    console.log(`Debates added: ${newDebateCount - debateCount}`);
    
    // Final report
    console.log('\n📊 Test Results:');
    console.log('- Dialog opened: ✅');
    console.log('- Form filled: ✅');
    console.log(`- API called: ${apiCalled ? '✅' : '❌'}`);
    console.log(`- Dialog closed: ${dialogClosed ? '✅' : '❌'}`);
    console.log(`- Debate created: ${newDebateCount > debateCount ? '✅' : '❌'}`);
    
    // Keep browser open for inspection
    console.log('\n⏸️  Browser will stay open for 30 seconds for inspection...');
    await page.waitForTimeout(30000);
    
  } catch (error) {
    console.error('❌ Test failed:', error.message);
    console.log('\n⏸️  Browser will stay open for 30 seconds for debugging...');
    await page.waitForTimeout(30000);
  } finally {
    await browser.close();
  }
}

// Run the test
testDebateCreation().catch(console.error);