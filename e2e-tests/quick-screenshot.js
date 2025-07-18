const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  
  try {
    console.log('Navigating to UI...');
    await page.goto('http://localhost:3002');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000); // Give React time to render
    
    console.log('Taking screenshots...');
    await page.screenshot({ path: 'screenshots/ui-homepage.png', fullPage: true });
    
    // Get page info
    const title = await page.title();
    const url = await page.url();
    console.log('Page title:', title);
    console.log('Page URL:', url);
    
    // Try to get some text content
    const bodyText = await page.textContent('body');
    console.log('Body text preview:', bodyText.substring(0, 200));
    
    // Mobile view
    await page.setViewportSize({ width: 375, height: 667 });
    await page.screenshot({ path: 'screenshots/ui-mobile.png', fullPage: true });
    
    console.log('Screenshots saved to screenshots/ directory');
    
  } catch (error) {
    console.error('Error:', error);
    await page.screenshot({ path: 'screenshots/error-capture.png', fullPage: true });
  }
  
  await browser.close();
})();