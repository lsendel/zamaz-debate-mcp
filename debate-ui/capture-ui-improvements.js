const puppeteer = require('puppeteer');

async function captureScreenshots() {
  const browser = await puppeteer.launch({ 
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  try {
    const page = await browser.newPage();
    
    // Set viewport for desktop
    await page.setViewport({ width: 1280, height: 720 });
    
    console.log('Capturing login page...');
    await page.goto('http://localhost:3001/login', { waitUntil: 'networkidle0' });
    await page.screenshot({ path: 'ui-improvements-login.png', fullPage: true });
    
    // Login
    console.log('Logging in...');
    await page.waitForSelector('input[placeholder="Username"]', { timeout: 5000 });
    await page.type('input[placeholder="Username"]', 'demo');
    await page.type('input[placeholder="Password"]', 'demo123');
    await page.click('button[type="submit"]');
    
    // Wait for navigation
    await page.waitForNavigation({ waitUntil: 'networkidle0' });
    await page.waitForTimeout(2000);
    
    console.log('Capturing debates page...');
    await page.screenshot({ path: 'ui-improvements-debates.png', fullPage: true });
    
    // Navigate to organization management
    console.log('Navigating to organization management...');
    await page.click('text=Admin');
    await page.waitForTimeout(500);
    await page.click('text=Organizations');
    await page.waitForTimeout(2000);
    
    console.log('Capturing organization management page...');
    await page.screenshot({ path: 'ui-improvements-org-management.png', fullPage: true });
    
    // Mobile view
    console.log('Capturing mobile view...');
    await page.setViewport({ width: 375, height: 667 });
    await page.goto('http://localhost:3001/login', { waitUntil: 'networkidle0' });
    await page.screenshot({ path: 'ui-improvements-mobile.png', fullPage: true });
    
    console.log('Screenshots captured successfully!');
    
  } catch (error) {
    console.error('Error capturing screenshots:', error);
  } finally {
    await browser.close();
  }
}

captureScreenshots();