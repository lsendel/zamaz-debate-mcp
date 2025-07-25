const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: false });
  const page = await browser.newPage();
  
  // Capture console messages
  page.on('console', msg => {
    const type = msg.type();
    if (type === 'error') {
      console.log('Console Error:', msg.text());
    }
  });
  
  // Capture page errors
  page.on('pageerror', error => {
    console.log('Page Error:', error.message);
  });
  
  try {
    await page.goto('http://localhost:3001', { waitUntil: 'networkidle0' });
    
    // Take screenshot
    await page.screenshot({ path: 'browser-check.png', fullPage: true });
    console.log('Screenshot saved as browser-check.png');
    
    // Wait a bit to capture any delayed errors
    await page.waitForTimeout(2000);
    
  } catch (error) {
    console.log('Navigation Error:', error.message);
  }
  
  await browser.close();
})();