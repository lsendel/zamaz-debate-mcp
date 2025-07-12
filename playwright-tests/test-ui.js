const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  try {
    console.log('Testing localhost:3000...');
    await page.goto('http://localhost:3000', { 
      waitUntil: 'networkidle2',
      timeout: 10000 
    });
    
    const title = await page.title();
    console.log('✅ UI is running! Page title:', title);
    
    // Take screenshot
    await page.screenshot({ path: 'ui-screenshot.png' });
    console.log('📸 Screenshot saved as ui-screenshot.png');
    
  } catch (error) {
    console.error('❌ UI is not accessible:', error.message);
    
    // Check if UI dev server is running
    console.log('\nChecking if UI dev server is running...');
    const { exec } = require('child_process');
    exec('lsof -i :3000', (err, stdout) => {
      if (stdout) {
        console.log('Process on port 3000:', stdout);
      } else {
        console.log('No process found on port 3000');
        console.log('\n💡 To start the UI, run: make ui');
      }
    });
  }
  
  await browser.close();
})();