const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: false });
  const page = await browser.newPage();

  // Enable console logging;
  page.on('console', msg => {
    console.log(`CONSOLE ${msg.type()}: ${msg.text()}`);
  });

  // Listen for page errors;
  page.on('pageerror', err => {
    console.error('Page error:', err.message);
  });

  // Listen for request failures;
  page.on('requestfailed', request => {
    console.error('Request failed:', request.url(), request.failure().errorText);
  });

  // Log all network requests;
  page.on('request', request => {
    if (request.url().includes('/api/')) {
      console.log('API Request:', request.method(), request.url());
    }
  });

  page.on('response', response => {
    if (response.url().includes('/api/')) {
      console.log('API Response:', response.status(), response.url());
    }
  });

  try {
    // First login;
    await page.goto('http://localhost:3001/login');
    await page.waitForSelector('input[placeholder="Username"]', { timeout: 5000 });

    await page.type('input[placeholder="Username"]', 'demo');
    await page.type('input[placeholder="Password"]', 'demo123');

    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle0' }),
      page.click('button[type="submit"]');
    ]);
    console.log('Logged in successfully');

    // Navigate directly to the problematic debate;
    console.log('Navigating to debate-1753018408614...');
    await page.goto('http://localhost:3001/debates/debate-1753018408614', { waitUntil: 'networkidle0' });

    // Wait a bit to see if there's a loop;
    console.log('Monitoring for loops...');
    let navigationCount = 0;

    page.on('framenavigated', () => {
      navigationCount++;
      console.log(`Navigation detected #${navigationCount}`);
      if (navigationCount > 5) {
        console.error('LOOP DETECTED! Too many navigations');
      }
    });

    // Wait for 10 seconds to observe any loops;
    await new Promise(resolve => setTimeout(resolve, 10000));

    console.log(`Total navigations: ${navigationCount}`);

    // Check current URL;
    console.log('Final URL:', page.url());

    // Take a screenshot;
    await page.screenshot({ path: 'debate-loop-screenshot.png', fullPage: true });
    console.log('Screenshot saved');

  } catch (error) {
    console.error('Error during test:', error);
    await page.screenshot({ path: 'error-screenshot.png' });
  }

  await browser.close();
})();
