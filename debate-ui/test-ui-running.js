const puppeteer = require('puppeteer');

async function testUI() {
    let browser;
    try {
        console.log('Starting UI test...');
        
        browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox']
        });
        
        const page = await browser.newPage();
        
        // Set viewport
        await page.setViewport({ width: 1280, height: 800 });
        
        // Navigate to the UI
        console.log('Navigating to http://localhost:3001...');
        await page.goto('http://localhost:3001', { waitUntil: 'networkidle0', timeout: 30000 });
        
        // Take a screenshot
        await page.screenshot({ path: 'ui-running-test.png', fullPage: true });
        console.log('Screenshot saved as ui-running-test.png');
        
        // Check if login page loads
        const pageTitle = await page.title();
        console.log('Page title:', pageTitle);
        
        // Check for login elements
        const hasLoginForm = await page.$('input[type="email"], input[type="text"]') !== null;
        const hasPasswordField = await page.$('input[type="password"]') !== null;
        const hasSubmitButton = await page.$('button[type="submit"], button') !== null;
        
        console.log('Login form present:', hasLoginForm);
        console.log('Password field present:', hasPasswordField);
        console.log('Submit button present:', hasSubmitButton);
        
        // Get page content for debugging
        const bodyText = await page.evaluate(() => document.body.innerText);
        console.log('Page content preview:', bodyText.substring(0, 200) + '...');
        
        console.log('\n✅ UI is running successfully at http://localhost:3001');
        
    } catch (error) {
        console.error('❌ Error testing UI:', error.message);
        if (error.message.includes('net::ERR_CONNECTION_REFUSED')) {
            console.error('The UI server is not running. Please start it with "npm run dev"');
        }
    } finally {
        if (browser) {
            await browser.close();
        }
    }
}

testUI();