const puppeteer = require('puppeteer');
const fs = require('fs').promises;

async function fixBlankScreen() {
    console.log('🔧 Starting blank screen diagnosis and fix...\n');
    
    const browser = await puppeteer.launch({ 
        headless: false,
        devtools: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    
    const page = await browser.newPage();
    
    // Create evidence directory
    try {
        await fs.mkdir('evidence', { recursive: true });
    } catch (e) {}
    
    const errors = [];
    const logs = [];
    
    // Capture everything
    page.on('console', msg => {
        const entry = `[${msg.type()}] ${msg.text()}`;
        logs.push(entry);
        if (msg.type() === 'error') {
            errors.push(entry);
            console.log('❌ Console Error:', msg.text());
        }
    });
    
    page.on('pageerror', error => {
        errors.push(`PAGE ERROR: ${error.message}`);
        console.log('🚨 Page Error:', error.message);
    });
    
    page.on('requestfailed', request => {
        errors.push(`REQUEST FAILED: ${request.url()}`);
        console.log('🔴 Request Failed:', request.url());
    });
    
    console.log('📍 Loading http://localhost:3001...\n');
    
    try {
        await page.goto('http://localhost:3001', { 
            waitUntil: 'networkidle0',
            timeout: 15000 
        });
        
        // Wait for React
        await page.waitForTimeout(3000);
        
        // Take initial screenshot
        await page.screenshot({ path: 'evidence/01-initial-load.png' });
        
        // Check what's actually on the page
        const pageState = await page.evaluate(() => {
            const root = document.getElementById('root');
            const body = document.body;
            
            // Check for React
            const hasReact = !!(window.React || window.__REACT_DEVTOOLS_GLOBAL_HOOK__);
            
            // Get any error messages
            const errorElements = document.querySelectorAll('.error, [class*="error"], [id*="error"]');
            const errorTexts = Array.from(errorElements).map(el => el.textContent);
            
            // Check imports
            const scripts = Array.from(document.scripts).map(s => s.src);
            
            return {
                rootExists: !!root,
                rootEmpty: root ? root.innerHTML === '' : null,
                rootContent: root ? root.innerHTML.substring(0, 200) : 'NO ROOT',
                bodyText: body.innerText || body.textContent || '',
                hasReact,
                errorTexts,
                scripts: scripts.filter(s => s.includes('localhost')),
                hasLoginForm: !!document.querySelector('form'),
                hasAnyContent: body.innerHTML.length > 100
            };
        });
        
        console.log('\n📊 Page Analysis:');
        console.log('Root exists:', pageState.rootExists);
        console.log('Root empty:', pageState.rootEmpty);
        console.log('Has React:', pageState.hasReact);
        console.log('Has any content:', pageState.hasAnyContent);
        console.log('Has login form:', pageState.hasLoginForm);
        console.log('Error messages found:', pageState.errorTexts);
        
        if (pageState.rootEmpty) {
            console.log('\n⚠️  ROOT IS EMPTY - React not rendering!');
            
            // Try to find the issue
            const reactIssue = await page.evaluate(() => {
                // Check if modules loaded
                const modules = {
                    React: typeof React !== 'undefined',
                    ReactDOM: typeof ReactDOM !== 'undefined',
                    App: typeof App !== 'undefined'
                };
                
                // Try to get error from React
                let reactError = null;
                try {
                    const root = document.getElementById('root');
                    if (root && root._reactRootContainer) {
                        const fiber = root._reactRootContainer._internalRoot;
                        if (fiber && fiber.current && fiber.current.memoizedState) {
                            reactError = fiber.current.memoizedState.error;
                        }
                    }
                } catch (e) {
                    reactError = e.message;
                }
                
                return { modules, reactError };
            });
            
            console.log('\nModule check:', reactIssue.modules);
            console.log('React error:', reactIssue.reactError);
            
            // Try a different approach - check network tab
            const resources = await page.evaluate(() => 
                performance.getEntriesByType('resource')
                    .filter(r => r.name.includes('.js') || r.name.includes('.tsx'))
                    .map(r => ({
                        url: r.name.split('/').pop(),
                        status: r.responseStatus || 'unknown',
                        duration: r.duration
                    }))
            );
            
            console.log('\n📦 JavaScript Resources:');
            resources.forEach(r => {
                console.log(`  ${r.url}: ${r.status} (${r.duration.toFixed(0)}ms)`);
            });
        }
        
        // If still blank, try to inject a test
        if (pageState.rootEmpty) {
            console.log('\n🔧 Attempting to manually trigger React render...');
            
            const manualRender = await page.evaluate(() => {
                try {
                    const root = document.getElementById('root');
                    if (!root) return 'No root element';
                    
                    // Create a simple test element
                    const testDiv = document.createElement('div');
                    testDiv.innerHTML = '<h1>Manual Test - If you see this, DOM manipulation works</h1>';
                    testDiv.style.padding = '20px';
                    root.appendChild(testDiv);
                    
                    return 'Test element added';
                } catch (e) {
                    return `Error: ${e.message}`;
                }
            });
            
            console.log('Manual render result:', manualRender);
            await page.screenshot({ path: 'evidence/02-manual-test.png' });
        }
        
        // Check for specific import errors
        const importErrors = errors.filter(e => 
            e.includes('import') || 
            e.includes('export') || 
            e.includes('module') ||
            e.includes('Cannot find')
        );
        
        if (importErrors.length > 0) {
            console.log('\n⚠️  Import/Module Errors Found:');
            importErrors.forEach(err => console.log('  ', err));
        }
        
        console.log('\n📸 Evidence saved in ./evidence/');
        console.log('\n🔍 Summary:');
        console.log(`Total errors: ${errors.length}`);
        console.log(`Page loaded: ${pageState.hasAnyContent ? 'Yes' : 'No'}`);
        console.log(`React detected: ${pageState.hasReact ? 'Yes' : 'No'}`);
        
        if (errors.length > 0) {
            console.log('\n❌ Errors found - saving to errors.log');
            await fs.writeFile('evidence/errors.log', errors.join('\n'));
        }
        
        console.log('\n✅ Diagnosis complete. Browser remains open for inspection.');
        console.log('Check the Console and Network tabs in DevTools.');
        
        // Keep browser open
        await new Promise(() => {});
        
    } catch (error) {
        console.error('\n❌ Fatal error:', error.message);
        await page.screenshot({ path: 'evidence/fatal-error.png' });
    }
}

fixBlankScreen().catch(console.error);