const puppeteer = require('puppeteer');

async function finalCheck() {
    let browser;
    try {
        browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox']
        });
        
        const page = await browser.newPage();
        await page.setViewport({ width: 1280, height: 800 });
        
        console.log('Loading UI...');
        await page.goto('http://localhost:3001', { 
            waitUntil: 'networkidle2', 
            timeout: 30000 
        });
        
        // Wait for React to render
        await page.waitForTimeout(3000);
        
        // Take screenshot
        await page.screenshot({ path: 'ui-final-state.png', fullPage: true });
        console.log('✅ Screenshot saved as ui-final-state.png');
        
        // Check for login page
        const hasLoginForm = await page.$('form') !== null;
        const hasEmailInput = await page.$('input[type="email"], input[name="email"], input[name="username"]') !== null;
        const hasPasswordInput = await page.$('input[type="password"]') !== null;
        
        console.log('\nUI Status:');
        console.log('- Page loaded: ✅');
        console.log(`- Login form: ${hasLoginForm ? '✅' : '❌'}`);
        console.log(`- Email input: ${hasEmailInput ? '✅' : '❌'}`);
        console.log(`- Password input: ${hasPasswordInput ? '✅' : '❌'}`);
        
        // Get page title
        const title = await page.title();
        console.log(`- Page title: "${title}"`);
        
        // Get visible text
        const text = await page.evaluate(() => {
            const body = document.body;
            return body ? body.innerText || body.textContent || '' : '';
        });
        
        if (text && text.trim()) {
            console.log('\nVisible content:');
            console.log(text.substring(0, 300) + (text.length > 300 ? '...' : ''));
        } else {
            console.log('\n⚠️  No visible text content found');
        }
        
        console.log('\n🌐 UI is accessible at: http://localhost:3001');
        
    } catch (error) {
        console.error('❌ Error:', error.message);
    } finally {
        if (browser) await browser.close();
    }
}

finalCheck();