const puppeteer = require('puppeteer');

async function diagnoseBlankScreen() {
    const browser = await puppeteer.launch({ 
        headless: false,
        devtools: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    
    const page = await browser.newPage();
    
    console.log('🔍 Diagnosing blank screen issue...\n');
    
    // Capture ALL console messages
    const consoleLogs = [];
    page.on('console', msg => {
        const log = `[${msg.type()}] ${msg.text()}`;
        consoleLogs.push(log);
        console.log(log);
    });
    
    // Capture page errors
    const pageErrors = [];
    page.on('pageerror', error => {
        const err = `PAGE ERROR: ${error.message}`;
        pageErrors.push(err);
        console.error(err);
    });
    
    // Capture failed requests
    page.on('requestfailed', request => {
        console.error('REQUEST FAILED:', request.url());
    });
    
    // Capture responses
    page.on('response', response => {
        if (response.status() >= 400) {
            console.error(`HTTP ${response.status()} - ${response.url()}`);
        }
    });
    
    try {
        console.log('Loading http://localhost:3001...\n');
        await page.goto('http://localhost:3001', { 
            waitUntil: 'networkidle0',
            timeout: 30000 
        });
        
        // Wait for potential React rendering
        await page.waitForTimeout(5000);
        
        // Get comprehensive page state
        const pageState = await page.evaluate(() => {
            const root = document.getElementById('root');
            const scripts = Array.from(document.scripts);
            
            // Check React
            const hasReact = !!(window.React || window.__REACT_DEVTOOLS_GLOBAL_HOOK__);
            const reactVersion = window.React?.version || 'unknown';
            
            // Get all errors from window
            const windowErrors = [];
            if (window.addEventListener) {
                window.addEventListener('error', (e) => {
                    windowErrors.push(e.message);
                });
            }
            
            return {
                // Basic checks
                title: document.title,
                url: window.location.href,
                readyState: document.readyState,
                
                // Root element
                rootExists: !!root,
                rootId: root?.id || 'no-root',
                rootClasses: root?.className || '',
                rootChildren: root?.children.length || 0,
                rootHTML: root?.innerHTML.substring(0, 500) || 'NO ROOT CONTENT',
                
                // React
                hasReact,
                reactVersion,
                
                // Body
                bodyClasses: document.body.className,
                bodyChildren: document.body.children.length,
                bodyText: (document.body.innerText || '').substring(0, 200),
                
                // Scripts
                scriptCount: scripts.length,
                scriptSrcs: scripts.map(s => s.src).filter(s => s),
                
                // Styles
                styleSheets: document.styleSheets.length,
                
                // Any visible elements
                visibleElements: {
                    forms: document.querySelectorAll('form').length,
                    inputs: document.querySelectorAll('input').length,
                    buttons: document.querySelectorAll('button').length,
                    divs: document.querySelectorAll('div').length,
                    spans: document.querySelectorAll('span').length,
                    headers: document.querySelectorAll('h1,h2,h3,h4,h5,h6').length
                }
            };
        });
        
        // Take screenshot
        await page.screenshot({ path: 'blank-screen-diagnosis.png', fullPage: true });
        
        console.log('\n📊 PAGE STATE ANALYSIS:');
        console.log('=======================');
        console.log('URL:', pageState.url);
        console.log('Title:', pageState.title);
        console.log('Ready State:', pageState.readyState);
        
        console.log('\n🎯 ROOT ELEMENT:');
        console.log('Exists:', pageState.rootExists);
        console.log('Children:', pageState.rootChildren);
        console.log('Content:', pageState.rootHTML ? 'Has content' : 'EMPTY');
        
        console.log('\n⚛️ REACT:');
        console.log('Detected:', pageState.hasReact);
        console.log('Version:', pageState.reactVersion);
        
        console.log('\n📦 RESOURCES:');
        console.log('Scripts loaded:', pageState.scriptCount);
        console.log('Stylesheets:', pageState.styleSheets);
        
        console.log('\n🔍 VISIBLE ELEMENTS:');
        Object.entries(pageState.visibleElements).forEach(([el, count]) => {
            console.log(`${el}: ${count}`);
        });
        
        console.log('\n📝 BODY TEXT:');
        console.log(pageState.bodyText || '(No visible text)');
        
        console.log('\n🚨 ERRORS SUMMARY:');
        console.log('Console errors:', pageErrors.length);
        console.log('Page errors:', pageErrors);
        
        // Check for specific issues
        if (pageState.rootExists && pageState.rootChildren === 0) {
            console.log('\n❌ ISSUE: React root exists but has no children - React app not rendering!');
        }
        
        if (!pageState.hasReact) {
            console.log('\n❌ ISSUE: React not detected on the page!');
        }
        
        // Try to get React render errors
        const reactErrors = await page.evaluate(() => {
            try {
                const errorInfo = document.querySelector('#root')?.textContent;
                return errorInfo || null;
            } catch (e) {
                return null;
            }
        });
        
        if (reactErrors) {
            console.log('\n🔴 React Error:', reactErrors);
        }
        
        console.log('\n📸 Screenshot saved as blank-screen-diagnosis.png');
        console.log('\n🌐 Browser DevTools are open. Check the Console tab for more details.');
        console.log('Keep this window open to inspect further.');
        
        // Keep browser open
        await new Promise(() => {});
        
    } catch (error) {
        console.error('\n❌ Fatal error:', error.message);
    }
}

diagnoseBlankScreen();