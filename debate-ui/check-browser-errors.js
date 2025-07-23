const puppeteer = require('puppeteer');

async function checkErrors() {
    const browser = await puppeteer.launch({ 
        headless: false,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    
    const page = await browser.newPage();
    
    console.log('🔍 Monitoring browser console...\n');
    
    // Capture all console messages
    page.on('console', msg => {
        const type = msg.type();
        const text = msg.text();
        
        if (type === 'error') {
            console.error('❌ ERROR:', text);
        } else if (type === 'warning') {
            console.warn('⚠️  WARNING:', text);
        } else {
            console.log(`📝 ${type.toUpperCase()}:`, text);
        }
    });
    
    // Capture page errors
    page.on('pageerror', error => {
        console.error('🚨 PAGE ERROR:', error.message);
    });
    
    // Capture failed requests
    page.on('requestfailed', request => {
        console.error('🔴 REQUEST FAILED:', request.url());
    });
    
    console.log('Opening http://localhost:3001...\n');
    
    try {
        await page.goto('http://localhost:3001', { 
            waitUntil: 'domcontentloaded',
            timeout: 30000 
        });
        
        // Wait for potential React rendering
        await page.waitForTimeout(5000);
        
        // Check if anything rendered
        const content = await page.evaluate(() => {
            const root = document.getElementById('root');
            return {
                rootExists: !!root,
                rootEmpty: root ? root.innerHTML === '' : true,
                rootContent: root ? root.innerHTML.substring(0, 100) : 'No root',
                hasReactApp: !!window.React || !!document.querySelector('[data-reactroot]'),
                bodyClasses: document.body.className,
                documentReady: document.readyState
            };
        });
        
        console.log('\n📊 Page State:');
        console.log('- Root element exists:', content.rootExists);
        console.log('- Root is empty:', content.rootEmpty);
        console.log('- React detected:', content.hasReactApp);
        console.log('- Document ready:', content.documentReady);
        console.log('- Root content preview:', content.rootContent);
        
        // Take screenshot
        await page.screenshot({ path: 'browser-state.png' });
        console.log('\n📸 Screenshot saved as browser-state.png');
        
        console.log('\n✅ Browser window is open. Check the console for any errors.');
        console.log('The page should be visible at http://localhost:3001');
        console.log('Press Ctrl+C to close the browser.\n');
        
        // Keep browser open
        await new Promise(() => {});
        
    } catch (error) {
        console.error('\n❌ Failed to load page:', error.message);
    }
}

checkErrors();