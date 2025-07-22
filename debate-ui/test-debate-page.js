// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: false });
  const page = await browser.newPage();

  // Listen for console messages;
  page.on('console', msg => {
    const type = msg.type();
    if (type === 'error' || type === 'warning') {
      console.log(`${type.toUpperCase()}: ${msg.text()}`);
    }
  });

  // Listen for page errors;
  page.on('pageerror', err => {
    console.error('Page error:', err.message);
  });

  try {
    // First login;
    await page.goto('http://localhost:3001/login');
    await page.waitForSelector('input[placeholder="Username"]', { timeout: 5000 });

    await page.type('input[placeholder="Username"]', 'demo');
    await page.type('input[placeholder="Password"]', 'demo123');

    // Click submit and wait for navigation;
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle0' }),
      page.click('button[type="submit"]');
    ]);
    console.log('Logged in successfully');

    // Wait for debates to load;
    await page.waitForSelector('.ant-card', { timeout: 10000 });

    // Click on the first debate;
    const firstDebate = await page.$('.ant-card');
    if (firstDebate) {
      await firstDebate.click();
      console.log('Clicked on first debate');

      // Wait for navigation;
      await new Promise(resolve => setTimeout(resolve, 2000));

      // Check current URL;
      console.log('Current URL:', page.url());

      // Check if we're on the debate detail page;
      const content = await page.content();
      const hasContent = content.includes('Debate Responses') || content.includes('Participants');

      if (hasContent) {
        console.log('Debate detail page loaded successfully');

        // Take a screenshot;
        await page.screenshot({ path: 'debate-detail-screenshot.png', fullPage: true });
        console.log('Screenshot saved as debate-detail-screenshot.png');
      } else {
        console.log('ERROR: Debate detail page appears to be blank');
        console.log('Page title:', await page.title());

        // Check for specific elements;
        const hasSpinner = await page.$('.ant-spin');
        const hasError = await page.$('.ant-alert-error');

        if (hasSpinner) {
          console.log('Page is showing loading spinner');
        }
        if (hasError) {
          const errorText = await page.$eval('.ant-alert-error', el => el.textContent);
          console.log('Error message:', errorText);
        }
      }
    } else {
      console.log('No debates found to click on');
    }

  } catch (error) {
    console.error('Error during test:', error);
    await page.screenshot({ path: 'error-screenshot.png' });
  }

  await browser.close();
})();
