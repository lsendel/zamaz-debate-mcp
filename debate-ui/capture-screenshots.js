const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

async function captureScreenshots() {
  const browser = await puppeteer.launch({ 
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  const page = await browser.newPage();
  
  // Create screenshots directory
  const screenshotsDir = path.join(__dirname, 'screenshots');
  if (!fs.existsSync(screenshotsDir)) {
    fs.mkdirSync(screenshotsDir);
  }
  
  try {
    // Set viewport
    await page.setViewport({ width: 1920, height: 1080 });
    
    // Navigate to the page
    console.log('Navigating to http://localhost:3001...');
    await page.goto('http://localhost:3001', { waitUntil: 'networkidle2', timeout: 30000 });
    
    // Wait for the page to stabilize
    await page.waitForTimeout(3000);
    
    // 1. Homepage screenshot
    console.log('Capturing homepage...');
    await page.screenshot({ 
      path: path.join(screenshotsDir, '1-homepage.png'), 
      fullPage: true 
    });
    
    // 2. Check for "New Debate" button and click it
    try {
      const newDebateButton = await page.$('button:has-text("New Debate")');
      if (newDebateButton) {
        console.log('Clicking New Debate button...');
        await newDebateButton.click();
        await page.waitForTimeout(2000);
        await page.screenshot({ 
          path: path.join(screenshotsDir, '2-create-debate-dialog.png'), 
          fullPage: true 
        });
        // Close dialog
        await page.keyboard.press('Escape');
        await page.waitForTimeout(1000);
      }
    } catch (e) {
      console.log('Could not find/click New Debate button:', e.message);
    }
    
    // 3. Check for tabs
    try {
      // Click Gallery tab
      const galleryTab = await page.$('button[role="tab"]:has-text("Gallery")');
      if (galleryTab) {
        console.log('Clicking Gallery tab...');
        await galleryTab.click();
        await page.waitForTimeout(2000);
        await page.screenshot({ 
          path: path.join(screenshotsDir, '3-gallery-tab.png'), 
          fullPage: true 
        });
      }
      
      // Click Setup tab
      const setupTab = await page.$('button[role="tab"]:has-text("Setup")');
      if (setupTab) {
        console.log('Clicking Setup tab...');
        await setupTab.click();
        await page.waitForTimeout(2000);
        await page.screenshot({ 
          path: path.join(screenshotsDir, '4-setup-tab.png'), 
          fullPage: true 
        });
      }
    } catch (e) {
      console.log('Error with tabs:', e.message);
    }
    
    // 4. Mobile view
    console.log('Capturing mobile view...');
    await page.setViewport({ width: 375, height: 667 });
    await page.waitForTimeout(1000);
    await page.screenshot({ 
      path: path.join(screenshotsDir, '5-mobile-view.png'), 
      fullPage: true 
    });
    
    // 5. Dark mode
    console.log('Capturing dark mode...');
    await page.setViewport({ width: 1920, height: 1080 });
    await page.emulateMediaFeatures([{ name: 'prefers-color-scheme', value: 'dark' }]);
    await page.waitForTimeout(1000);
    await page.screenshot({ 
      path: path.join(screenshotsDir, '6-dark-mode.png'), 
      fullPage: true 
    });
    
    console.log('Screenshots captured successfully!');
    
  } catch (error) {
    console.error('Error capturing screenshots:', error);
  } finally {
    await browser.close();
  }
}

captureScreenshots();