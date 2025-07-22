// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: false, devtools: true });
  const page = await browser.newPage();

  // Enable request interception to capture errors;
  await page.setRequestInterception(true);

  const apiErrors = []
  const consoleErrors = []

  // Listen for console messages;
  page.on('console', msg => {
    const type = msg.type();
    const text = msg.text();
    console.log(`CONSOLE ${type}: ${text}`);
    if (type === 'error' || type === 'warning') {
      consoleErrors.push({ type, text });
    }
  });

  // Listen for page errors;
  page.on('pageerror', err => {
    console.error('PAGE ERROR:', err.message);
    consoleErrors.push({ type: 'pageerror', text: err.message });
  });

  // Intercept requests;
  page.on('request', request => {
    if (request.url().includes('/api/')) {
      console.log('API Request:', request.method(), request.url());
    }
    request.continue();
  });

  // Listen for responses and capture errors;
  page.on('response', response => {
    if (response.url().includes('/api/')) {
      console.log('API Response:', response.status(), response.url());
      if (response.status() >= 400) {
        response.text().then(body => {
          console.error('API ERROR RESPONSE:', body);
          apiErrors.push({
            url: response.url(),
            status: response.status(),
            body: body;
          });
        });
      }
    }
  });

  // Listen for failed requests;
  page.on('requestfailed', request => {
    console.error('REQUEST FAILED:', request.url(), request.failure().errorText);
    apiErrors.push({
      url: request.url(),
      error: request.failure().errorText;
    });
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
    console.log('Logged in successfully');

    // Navigate to the debate;
    console.log('Navigating to debate...');
    await page.goto('http://localhost:3001/debates/debate-1753018408614', { waitUntil: 'networkidle0' });

    // Wait for the page to load;
    await page.waitForSelector('.ant-card', { timeout: 10000 });
    console.log('Debate page loaded');

    // Take initial screenshot;
    await page.screenshot({ path: 'debate-before-start.png', fullPage: true });

    // Find and click the Start button using XPath;
    const [startButton] = await page.$x('//button[contains(., "Start")]');
    if (startButton) {
      console.log('Found Start button, clicking...');

      // Clear previous errors;
      apiErrors.length = 0;
      consoleErrors.length = 0;

      await startButton.click();
      console.log('Clicked Start button');

      // Wait for any network activity;
      await new Promise(resolve => setTimeout(resolve, 5000));

      // Check if the debate status changed;
      const statusElement = await page.$('.ant-badge-status-text');
      if (statusElement) {
        const status = await statusElement.evaluate(el => el.textContent);
        console.log('Current status:', status);
      }

      // Take screenshot after clicking start;
      await page.screenshot({ path: 'debate-after-start.png', fullPage: true });

      // Check for any loading indicators;
      const hasSpinner = await page.$('.ant-spin');
      if (hasSpinner) {
        console.log('Loading spinner detected');
      }

      // Check for error alerts;
      const errorAlert = await page.$('.ant-alert-error');
      if (errorAlert) {
        const errorText = await errorAlert.evaluate(el => el.textContent);
        console.error('ERROR ALERT FOUND:', errorText);
      }

      // Check for success notifications;
      const successNotification = await page.$('.ant-notification-notice-success');
      if (successNotification) {
        const successText = await successNotification.evaluate(el => el.textContent);
        console.log('Success notification:', successText);
      }

      // Print summary;
      console.log('\n=== SUMMARY ===');
      console.log('API Errors:', apiErrors.length);
      apiErrors.forEach(err => console.error('- API Error:', err));
      console.log('\nConsole Errors:', consoleErrors.length);
      consoleErrors.forEach(err => console.error('- Console Error:', err));

    } else {
      console.error('Start button not found!');
    }

  } catch (error) {
    console.error('Test error:', error);
    await page.screenshot({ path: 'error-screenshot.png' });
  }

  // Keep browser open for inspection;
  console.log('\nTest completed. Browser will remain open for 30 seconds...');
  await new Promise(resolve => setTimeout(resolve, 30000));

  await browser.close();
})();
