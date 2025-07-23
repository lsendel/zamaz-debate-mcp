const puppeteer = require('puppeteer');

async function debugReactRender() {
    const browser = await puppeteer.launch({ 
        headless: false,
        devtools: true 
    });
    
    const page = await browser.newPage();
    
    // Enable detailed console logging
    page.on('console', msg => {
        console.log(`[${msg.type()}] ${msg.text()}`);
        for (let i = 0; i < msg.args().length; ++i)
            console.log(`  arg${i}: ${msg.args()[i]}`);
    });
    
    page.on('pageerror', ({ message }) => console.log('PAGE ERROR:', message));
    
    console.log('Opening http://localhost:3001...\n');
    
    await page.goto('http://localhost:3001', { waitUntil: 'domcontentloaded' });
    
    // Wait a bit
    await page.waitForTimeout(2000);
    
    // Inject debugging code
    const debugInfo = await page.evaluate(() => {
        const root = document.getElementById('root');
        
        // Check React
        const reactInfo = {
            hasReact: !!(window.React || window._React || window.__REACT_DEVTOOLS_GLOBAL_HOOK__),
            reactRoot: root?._reactRootContainer || root?._reactRootContainer || 'none',
            reactFiber: root?._reactRootContainer?._internalRoot || 'none'
        };
        
        // Check for common issues
        const issues = [];
        
        if (!root) {
            issues.push('No root element found');
        } else if (root.innerHTML === '') {
            issues.push('Root element is empty');
        }
        
        // Check if any error boundary caught errors
        const errorBoundaryErrors = document.querySelector('[data-error-boundary]');
        if (errorBoundaryErrors) {
            issues.push('Error boundary triggered');
        }
        
        // Try to manually check for React errors
        if (window.__REACT_DEVTOOLS_GLOBAL_HOOK__) {
            const hook = window.__REACT_DEVTOOLS_GLOBAL_HOOK__;
            if (hook.renderers && hook.renderers.size > 0) {
                issues.push(`React renderers found: ${hook.renderers.size}`);
            }
        }
        
        return {
            url: window.location.href,
            rootExists: !!root,
            rootId: root?.id,
            rootHTML: root?.innerHTML || 'empty',
            rootTextContent: root?.textContent || 'no text',
            ...reactInfo,
            issues,
            bodyClasses: document.body.className,
            scripts: Array.from(document.scripts).map(s => s.src || 'inline').filter(s => s.includes('localhost'))
        };
    });
    
    console.log('\n=== DEBUG INFO ===');
    console.log(JSON.stringify(debugInfo, null, 2));
    
    // Try to trigger React render manually
    const triggerResult = await page.evaluate(() => {
        try {
            // Force React to render
            if (window.React && window.ReactDOM) {
                const root = document.getElementById('root');
                window.ReactDOM.render(
                    window.React.createElement('div', null, 'Manual React Test'),
                    root
                );
                return 'Manual render attempted';
            }
            return 'React/ReactDOM not found on window';
        } catch (e) {
            return `Error: ${e.message}`;
        }
    });
    
    console.log('\nManual render result:', triggerResult);
    
    // Take screenshot
    await page.screenshot({ path: 'debug-react-state.png' });
    console.log('\nScreenshot saved as debug-react-state.png');
    
    console.log('\nBrowser is open. Check DevTools Console for more details.');
    console.log('The issue appears to be that React is not rendering into the root element.');
    
    // Keep open for manual inspection
    await new Promise(() => {});
}

debugReactRender().catch(console.error);