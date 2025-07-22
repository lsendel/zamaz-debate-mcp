const puppeteer = require('puppeteer');

async function debugUI() {
  console.log('🔍 Debugging UI Loading...');

  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: null,
    args: ['--start-maximized']
  });

  const page = await browser.newPage();

  // Listen for console messages;
  page.on('console', msg => {
    console.log(`🖥️ Console (${msg.type()}):`, msg.text());
  });

  // Listen for errors;
  page.on('pageerror', err => {
    console.error('❌ Page Error:', err.message);
  });

  // Listen for failed requests;
  page.on('requestfailed', request => {
    console.error('❌ Failed Request:', request.url(), request.failure().errorText);
  });

  try {
    console.log('→ Going to http://localhost:3003');
    await page.goto('http://localhost:3003', { waitUntil: 'networkidle0', timeout: 30000 });

    await page.screenshot({ path: './debug-homepage.png', fullPage: true });
    console.log('✓ Homepage screenshot saved');

    // Check if React is loaded;
    const reactExists = await page.evaluate(() => {
      return typeof window.React !== 'undefined' || document.getElementById('root') !== null;
    });

    console.log(`React/Root element found: ${reactExists}`);

    // Check the document body content;
    const bodyContent = await page.evaluate(() => {
      return document.body.innerHTML.substring(0, 500);
    });

    console.log('Body content preview:', bodyContent);

    // Check for login page specifically;
    console.log('→ Going to http://localhost:3003/login');
    await page.goto('http://localhost:3003/login', { waitUntil: 'networkidle0', timeout: 30000 });

    await page.screenshot({ path: './debug-login.png', fullPage: true });
    console.log('✓ Login screenshot saved');

    const loginContent = await page.evaluate(() => {
      return document.body.innerHTML.substring(0, 500);
    });

    console.log('Login page content preview:', loginContent);

    // Wait for user interaction;
    console.log('⏸️ Browser will stay open for manual inspection. Press any key to close...');
    await new Promise(resolve => {
      process.stdin.setRawMode(true);
      process.stdin.resume();
      process.stdin.on('data', resolve);
    });

  } catch (error) {
    console.error('❌ Debug failed:', error);
  } finally {
    await browser.close();
  }
}

debugUI().catch(console.error);
