const puppeteer = require('puppeteer');

async function diagnoseUI() {
    let browser;
    try {
        browser = await puppeteer.launch({
            headless: false, // Show browser to see what's happening
            args: ['--no-sandbox', '--disable-setuid-sandbox'],
            defaultViewport: null
        });
        
        const page = await browser.newPage();
        
        // Capture console logs
        page.on('console', msg => {
            console.log(`Browser console [${msg.type()}]:`, msg.text());
        });
        
        // Capture errors
        page.on('pageerror', error => {
            console.error('Page error:', error.message);
        });
        
        // Capture failed requests
        page.on('requestfailed', request => {
            console.error('Request failed:', request.url(), '-', request.failure().errorText);
        });
        
        console.log('Opening http://localhost:3001...');
        
        try {
            await page.goto('http://localhost:3001', { 
                waitUntil: 'networkidle0', 
                timeout: 30000 
            });
            console.log('✅ Page loaded');
        } catch (e) {
            console.log('⚠️  Page load timeout, continuing...');
        }
        
        // Wait a bit for React
        await page.waitForTimeout(3000);
        
        // Take screenshot
        await page.screenshot({ path: 'ui-diagnosis.png', fullPage: true });
        console.log('📸 Screenshot saved as ui-diagnosis.png');
        
        // Check page content
        const pageContent = await page.evaluate(() => {
            return {
                title: document.title,
                bodyHTML: document.body.innerHTML.substring(0, 500),
                hasRoot: document.getElementById('root') !== null,
                rootContent: document.getElementById('root')?.innerHTML || 'No root content',
                scripts: Array.from(document.scripts).map(s => s.src || 'inline'),
                errors: window.errors || [],
                bodyText: document.body.innerText || document.body.textContent || ''
            };
        });
        
        console.log('\n📋 Page Analysis:');
        console.log('Title:', pageContent.title);
        console.log('Has React root:', pageContent.hasRoot);
        console.log('Scripts loaded:', pageContent.scripts.length);
        console.log('\nBody text:', pageContent.bodyText);
        
        if (pageContent.rootContent === 'No root content' || pageContent.rootContent === '') {
            console.log('\n❌ React root is empty - app not rendering');
        }
        
        // Check for specific elements
        const elements = await page.evaluate(() => {
            return {
                forms: document.querySelectorAll('form').length,
                inputs: document.querySelectorAll('input').length,
                buttons: document.querySelectorAll('button').length,
                divs: document.querySelectorAll('div').length,
                errors: document.querySelectorAll('.error, [class*="error"]').length
            };
        });
        
        console.log('\n🔍 Element count:');
        console.log('Forms:', elements.forms);
        console.log('Inputs:', elements.inputs);
        console.log('Buttons:', elements.buttons);
        console.log('Divs:', elements.divs);
        console.log('Error elements:', elements.errors);
        
        // Check network activity
        const resources = await page.evaluate(() => 
            performance.getEntriesByType('resource').map(r => ({
                name: r.name,
                status: r.responseStatus || 'unknown'
            }))
        );
        
        const failedResources = resources.filter(r => r.name.includes('localhost'));
        if (failedResources.length > 0) {
            console.log('\n⚠️  Failed resources:', failedResources);
        }
        
        console.log('\n🌐 Browser window will remain open for inspection.');
        console.log('Press Ctrl+C to close when done.');
        
        // Keep browser open
        await new Promise(() => {});
        
    } catch (error) {
        console.error('❌ Diagnosis error:', error.message);
    } finally {
        if (browser) {
            // Browser won't close automatically
        }
    }
}

diagnoseUI();