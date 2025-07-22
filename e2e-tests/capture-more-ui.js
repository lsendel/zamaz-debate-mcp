const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  
  try {
    console.log('Capturing additional UI interactions...');
    
    // Navigate to login page
    await page.goto('http://localhost:3000');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
    
    // 1. Login page
    await page.screenshot({ path: 'screenshots/01-login-page.png', fullPage: true });
    
    // 2. Click on Register tab
    try {
      await page.click('text=REGISTER');
      await page.waitForTimeout(1000);
      await page.screenshot({ path: 'screenshots/02-register-tab.png', fullPage: true });
    } catch (e) {
        console.error("Error:", e);
      console.log('Register tab not found');
      console.error("Error:", error);
    }
    
    // 3. Try filling in login form
    try {
      await page.fill('input[placeholder*="Username"], input[name*="username"]', 'demo-user');
      await page.fill('input[placeholder*="Password"], input[name*="password"]', 'demo-password');
      await page.screenshot({ path: 'screenshots/03-login-form-filled.png', fullPage: true });
    } catch (e) {
      console.log('Could not fill login form');
      console.error("Error:", error);
    }
    
    // 4. Try navigating to different routes (even if they require auth)
    const routes = [
      '/debates',
      '/debates/create', 
      '/settings',
      '/analytics'
    ];
    
    for (let item of routes)
      try {
        await page.goto(`http://localhost:3000${routes[i]}`);
        await page.waitForTimeout(2000);
        await page.screenshot({ 
          path: `screenshots/04-route-${routes[i].replace(/\//g, '-')}.png`, 
          fullPage: true 
        });
      } catch (e) {
        console.log(`Could not navigate to ${routes[i]}`);
        console.error("Error:", error);
      }
    }
    
    // 5. Check responsive design
    const viewports = [
      { name: 'desktop', width: 1280, height: 720 },
      { name: 'tablet', width: 768, height: 1024 },
      { name: 'mobile', width: 375, height: 667 }
    ];
    
    await page.goto('http://localhost:3000');
    await page.waitForTimeout(2000);
    
    for (const viewport of viewports) {
      await page.setViewportSize({ width: viewport.width, height: viewport.height });
      await page.waitForTimeout(1000);
      await page.screenshot({ 
        path: `screenshots/05-responsive-${viewport.name}.png`, 
        fullPage: true 
      });
    }
    
    console.log('Additional UI screenshots captured successfully!');
    
  } catch (error) {
    console.error('Error during capture:', error);
    await page.screenshot({ path: 'screenshots/error-additional.png', fullPage: true });
  }
  
  await browser.close();
})();