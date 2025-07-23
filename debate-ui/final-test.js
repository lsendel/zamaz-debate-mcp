const puppeteer = require('puppeteer');

(async () => {
    try {
        console.log('Testing UI at http://localhost:3001...');
        
        const browser = await puppeteer.launch({ 
            headless: false,
            defaultViewport: { width: 1280, height: 800 }
        });
        
        const page = await browser.newPage();
        
        await page.goto('http://localhost:3001');
        console.log('✅ Page loaded');
        
        // Wait for React
        await new Promise(resolve => setTimeout(resolve, 3000));
        
        // Take screenshot
        await page.screenshot({ path: 'ui-final.png' });
        console.log('📸 Screenshot saved as ui-final.png');
        
        // Check content
        const hasLogin = await page.$('form, input[type="email"], input[type="password"], button') !== null;
        console.log('Has login elements:', hasLogin);
        
        console.log('\n🌐 Browser window is open. You should see the UI.');
        console.log('If you see a blank page, check the browser console for errors.');
        console.log('The UI is available at: http://localhost:3001\n');
        
        // Keep browser open for 30 seconds
        await new Promise(resolve => setTimeout(resolve, 30000));
        
        await browser.close();
    } catch (error) {
        console.error('Error:', error.message);
    }
})();