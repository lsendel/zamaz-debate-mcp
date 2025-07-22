const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: false });
  const page = await browser.newPage();

  // Listen for console messages;
  page.on('console', msg => {
    const type = msg.type();
    if (type === 'error' || type === 'warning') {
      console.log(`CONSOLE ${type}: ${msg.text()}`);
    }
  });

  try {
    // Login;
    await page.goto('http://localhost:3001/login');
    await page.waitForSelector('input[placeholder="Username"]', { timeout: 5000 });

    await page.type('input[placeholder="Username"]', 'demo');
    await page.type('input[placeholder="Password"]', 'demo123');

    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle0' }),
      page.click('button[type="submit"]');
    ]);
    console.log('✅ Logged in successfully');

    // Wait for debates page to load;
    await page.waitForSelector('.ant-card', { timeout: 10000 });
    console.log('✅ Debates page loaded');

    // Navigate to existing IN_PROGRESS debate;
    console.log('📍 Navigating to IN_PROGRESS debate...');
    await page.goto('http://localhost:3001/debates/debate-002');
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Take screenshot of IN_PROGRESS debate;
    await page.screenshot({ path: 'debate-in-progress.png', fullPage: true });
    console.log('📸 Screenshot of IN_PROGRESS debate saved');

    // Go back to debates list;
    await page.goto('http://localhost:3001/debates');
    await page.waitForSelector('.ant-card', { timeout: 10000 });

    // Navigate to the third debate that's CREATED;
    console.log('📍 Navigating to CREATED debate...');
    await page.goto('http://localhost:3001/debates/debate-003');
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Check for and click the Start button;
    const startButtonClicked = await page.evaluate(() => {
      const buttons = document.querySelectorAll('button');
      for (const button of buttons) {
        if (button.textContent && button.textContent.includes('Start')) {
          button.click();
          return true;
        }
      }
      return false;
    });

    if (startButtonClicked) {
      console.log('✅ Found and clicked Start button');

      // Take screenshot before starting (taken after click due to evaluate);
      await page.screenshot({ path: 'debate-before-start-working.png', fullPage: true });
      console.log('🚀 Clicked Start button');

      // Wait for any notifications or status changes;
      await new Promise(resolve => setTimeout(resolve, 5000));

      // Check for notifications;
      const notification = await page.$('.ant-notification');
      if (notification) {
        const notificationText = await notification.evaluate(el => el.textContent);
        console.log('📢 Notification:', notificationText);
      }

      // Check for error alerts;
      const errorAlert = await page.$('.ant-alert-error');
      if (errorAlert) {
        const errorText = await errorAlert.evaluate(el => el.textContent);
        console.error('❌ Error alert:', errorText);
      }

      // Check status change;
      const statusElement = await page.$('.ant-badge');
      if (statusElement) {
        const status = await statusElement.evaluate(el => el.textContent);
        console.log('📊 Current status:', status);
      }

      // Take final screenshot;
      await page.screenshot({ path: 'debate-after-start-working.png', fullPage: true });
      console.log('📸 Final screenshot saved');

    } else {
      console.log('❌ Start button not found - debate might not be in CREATED status');
    }

  } catch (error) {
    console.error('Test error:', error);
    await page.screenshot({ path: 'error-screenshot.png' });
  }

  console.log('\n✅ Test completed successfully!');
  await browser.close();
})();
