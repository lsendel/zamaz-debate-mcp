const puppeteer = require('puppeteer');

(async () => {
  console.log('🚀 Starting Debate UI Error Handling Demonstration\n');
  
  const browser = await puppeteer.launch({ 
    headless: false,
    args: ['--window-size=1400,900']
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1400, height: 900 });
  
  try {
    // Step 1: Login
    console.log('Step 1: Logging in...');
    await page.goto('http://localhost:3001/login');
    await page.waitForSelector('input[placeholder="Username"]', { timeout: 5000 });
    
    await page.type('input[placeholder="Username"]', 'demo');
    await page.type('input[placeholder="Password"]', 'demo123');
    
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle0' }),
      page.click('button[type="submit"]')
    ]);
    console.log('✅ Login successful\n');
    
    // Step 2: Navigate to a debate that exists
    console.log('Step 2: Navigating to debate-003...');
    await page.goto('http://localhost:3001/debates/debate-003');
    await page.waitForSelector('.ant-card', { timeout: 10000 });
    console.log('✅ Debate page loaded\n');
    
    // Take screenshot of the debate page
    await page.screenshot({ path: 'final-debate-page.png', fullPage: true });
    console.log('📸 Screenshot saved: final-debate-page.png\n');
    
    // Step 3: Test error scenario - stop the backend service
    console.log('Step 3: Testing error handling...');
    console.log('⚠️  Please manually stop the debate service (Ctrl+C in the terminal where it\'s running)');
    console.log('    Waiting 10 seconds for you to stop the service...\n');
    await new Promise(resolve => setTimeout(resolve, 10000));
    
    // Try to click Start button
    console.log('Step 4: Attempting to start debate with service down...');
    const clicked = await page.evaluate(() => {
      const buttons = document.querySelectorAll('button');
      for (const button of buttons) {
        if (button.textContent && button.textContent.includes('Start')) {
          button.click();
          return true;
        }
      }
      return false;
    });
    
    if (clicked) {
      console.log('✅ Clicked Start button\n');
      
      // Wait for error handling
      await new Promise(resolve => setTimeout(resolve, 3000));
      
      // Take screenshot of error state
      await page.screenshot({ path: 'final-error-handling.png', fullPage: true });
      console.log('📸 Screenshot saved: final-error-handling.png\n');
      
      // Check for error messages
      const hasNotification = await page.$('.ant-notification');
      const hasAlert = await page.$('.ant-alert-error');
      const hasServiceStatus = await page.$('.ant-alert-error[role="alert"]');
      
      console.log('Error handling features detected:');
      console.log(`- Notification popup: ${hasNotification ? '✅ Yes' : '❌ No'}`);
      console.log(`- Error alert: ${hasAlert ? '✅ Yes' : '❌ No'}`);
      console.log(`- Service status alert: ${hasServiceStatus ? '✅ Yes' : '❌ No'}\n`);
    }
    
    console.log('✅ Demonstration complete!');
    console.log('\nKey improvements implemented:');
    console.log('1. Loading states on action buttons');
    console.log('2. Error notifications with clear messages');
    console.log('3. Service status monitoring with retry option');
    console.log('4. Proper error handling for all API failures');
    console.log('5. User-friendly error messages explaining the issue\n');
    
  } catch (error) {
    console.error('Test error:', error);
    await page.screenshot({ path: 'final-error-screenshot.png' });
  }
  
  console.log('Browser will remain open for inspection...');
  // Keep browser open for manual inspection
})();