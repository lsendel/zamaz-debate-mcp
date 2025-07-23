const puppeteer = require('puppeteer');

async function quickCheck() {
    let browser;
    try {
        browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox']
        });
        
        const page = await browser.newPage();
        await page.setViewport({ width: 1280, height: 800 });
        
        // Enable request interception to see what's happening
        await page.setRequestInterception(true);
        page.on('request', request => {
            console.log('Request:', request.method(), request.url().substring(0, 80));
            request.continue();
        });
        
        console.log('Navigating to UI...');
        const response = await page.goto('http://localhost:3001', { 
            waitUntil: 'domcontentloaded', 
            timeout: 10000 
        });
        
        console.log('Response status:', response.status());
        
        // Wait a bit for React to render
        await page.waitForTimeout(2000);
        
        // Take screenshot
        await page.screenshot({ path: 'ui-current-state.png', fullPage: true });
        console.log('Screenshot saved as ui-current-state.png');
        
        // Get page content
        const content = await page.content();
        console.log('\nPage has content:', content.length > 1000 ? 'Yes' : 'No');
        
        // Check for React root
        const rootContent = await page.$eval('#root', el => el.innerHTML).catch(() => 'No root element');
        console.log('Root element content:', rootContent.substring(0, 100));
        
        // Check for login form specifically
        const hasLogin = await page.$('form') !== null;
        console.log('Has form element:', hasLogin);
        
        // Get all visible text
        const text = await page.evaluate(() => document.body.innerText);
        console.log('\nVisible text:', text || '(none)');
        
    } catch (error) {
        console.error('Error:', error.message);
    } finally {
        if (browser) await browser.close();
    }
}

quickCheck();