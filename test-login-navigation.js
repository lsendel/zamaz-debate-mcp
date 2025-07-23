const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    // Go to login page
    await page.goto('http://localhost:3001/login');
    await page.waitForSelector('form', { timeout: 5000 });
    
    console.log('1. On login page');
    
    // Fill login form
    await page.type('input[name="username"], #username', 'demo');
    await page.type('input[name="password"], input[type="password"]', 'demo123');
    
    console.log('2. Filled login form');
    
    // Submit form
    await page.click('button[type="submit"]');
    
    console.log('3. Clicked login button');
    
    // Wait for navigation or error
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    const currentUrl = page.url();
    console.log('4. Current URL:', currentUrl);
    
    // Check what's on the page
    const pageContent = await page.evaluate(() => document.body.innerText);
    console.log('5. Page content preview:', pageContent.substring(0, 200));
    
    // Check for console errors
    page.on('console', msg => {
      if (msg.type() === 'error') {
        console.log('Console error:', msg.text());
      }
    });
    
    // Check page HTML
    const pageHTML = await page.evaluate(() => document.body.innerHTML);
    console.log('5b. Page HTML preview:', pageHTML.substring(0, 300));
    
    // Check root element
    const rootHTML = await page.evaluate(() => document.getElementById('root')?.innerHTML || 'No root element');
    console.log('5c. Root element:', rootHTML.substring(0, 200));
    
    // Check for navigation elements
    const hasSider = await page.$('.ant-layout-sider') !== null;
    const hasMenu = await page.$('.ant-menu') !== null;
    
    console.log('6. Has sider:', hasSider);
    console.log('7. Has menu:', hasMenu);
    
    // Take screenshot
    await page.screenshot({ path: 'login-navigation-debug.png' });
    console.log('8. Screenshot saved as login-navigation-debug.png');
    
  } catch (error) {
    console.error('Error:', error);
  } finally {
    await browser.close();
  }
})();