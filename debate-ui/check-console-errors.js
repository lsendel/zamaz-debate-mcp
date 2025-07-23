const puppeteer = require('puppeteer');

async function checkConsoleErrors() {
    const browser = await puppeteer.launch({ 
        headless: false,
        devtools: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    
    const page = await browser.newPage();
    
    const logs = [];
    const errors = [];
    
    // Capture ALL console output
    page.on('console', async msg => {
        const type = msg.type();
        const text = msg.text();
        const args = [];
        
        // Try to get arguments
        for (const arg of msg.args()) {
            try {
                const val = await arg.jsonValue();
                args.push(val);
            } catch (e) {
                args.push('<Complex Object>');
            }
        }
        
        const logEntry = {
            type,
            text,
            args,
            location: msg.location()
        };
        
        logs.push(logEntry);
        
        if (type === 'error' || text.includes('Error') || text.includes('error')) {
            errors.push(logEntry);
            console.log(`\n❌ ERROR FOUND:`);
            console.log(`   Type: ${type}`);
            console.log(`   Text: ${text}`);
            if (args.length > 0) {
                console.log(`   Args:`, args);
            }
            if (msg.location().url) {
                console.log(`   Location: ${msg.location().url}:${msg.location().lineNumber}`);
            }
        }
    });
    
    page.on('pageerror', error => {
        console.log('\n🚨 PAGE ERROR:', error.message);
        console.log('   Stack:', error.stack);
        errors.push({ type: 'pageerror', message: error.message, stack: error.stack });
    });
    
    page.on('requestfailed', request => {
        console.log('\n🔴 REQUEST FAILED:', request.url());
        console.log('   Reason:', request.failure().errorText);
    });
    
    console.log('Loading http://localhost:3001...\n');
    
    try {
        await page.goto('http://localhost:3001', { 
            waitUntil: 'domcontentloaded',
            timeout: 10000 
        });
        
        console.log('✅ Page loaded\n');
        
        // Wait for React to attempt rendering
        await page.waitForTimeout(3000);
        
        // Check if React is in the page
        const reactCheck = await page.evaluate(() => {
            const checkReact = () => {
                const hasReact = !!(window.React || window.__REACT_DEVTOOLS_GLOBAL_HOOK__);
                const hasReactDOM = !!window.ReactDOM;
                const root = document.getElementById('root');
                const rootContent = root ? root.innerHTML : 'no root';
                
                // Try to find React fiber
                let fiber = null;
                if (root && root._reactRootContainer) {
                    fiber = 'Found _reactRootContainer';
                } else if (root && root.__reactContainer) {
                    fiber = 'Found __reactContainer';
                }
                
                return {
                    hasReact,
                    hasReactDOM,
                    rootExists: !!root,
                    rootEmpty: root ? root.innerHTML === '' : null,
                    rootContent: rootContent.substring(0, 100),
                    fiber
                };
            };
            
            return checkReact();
        });
        
        console.log('\n📊 React Status:');
        console.log('Has React:', reactCheck.hasReact);
        console.log('Has ReactDOM:', reactCheck.hasReactDOM);
        console.log('Root exists:', reactCheck.rootExists);
        console.log('Root empty:', reactCheck.rootEmpty);
        console.log('React Fiber:', reactCheck.fiber);
        console.log('Root content preview:', reactCheck.rootContent);
        
        // Try to check for errors in React DevTools
        const devToolsCheck = await page.evaluate(() => {
            if (window.__REACT_DEVTOOLS_GLOBAL_HOOK__) {
                const hook = window.__REACT_DEVTOOLS_GLOBAL_HOOK__;
                return {
                    hasHook: true,
                    renderers: hook.renderers ? hook.renderers.size : 0,
                    hasError: hook.hasError || false
                };
            }
            return { hasHook: false };
        });
        
        console.log('\n🔧 React DevTools:');
        console.log(devToolsCheck);
        
        console.log('\n📝 Console Logs Summary:');
        console.log(`Total logs: ${logs.length}`);
        console.log(`Errors found: ${errors.length}`);
        
        if (errors.length > 0) {
            console.log('\n❌ ERRORS DETAIL:');
            errors.forEach((err, i) => {
                console.log(`\n${i + 1}. ${err.type}: ${err.text || err.message}`);
            });
        }
        
        // Take screenshot
        await page.screenshot({ path: 'console-check.png' });
        console.log('\n📸 Screenshot saved as console-check.png');
        
        console.log('\n🔍 Browser DevTools are open. Check the Console tab.');
        console.log('Keep browser open to inspect...\n');
        
        // Keep browser open
        await new Promise(() => {});
        
    } catch (error) {
        console.error('\n❌ Error during check:', error.message);
    }
}

checkConsoleErrors();