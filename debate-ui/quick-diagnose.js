const puppeteer = require('puppeteer');

async function quickDiagnose() {
    const browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();
    
    // Capture console and errors
    const logs = [];
    page.on('console', msg => logs.push(`Console: ${msg.text()}`));
    page.on('pageerror', err => logs.push(`Error: ${err.message}`));
    
    try {
        await page.goto('http://localhost:3001', { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(2000);
        
        // Screenshot
        await page.screenshot({ path: 'current-ui-state.png' });
        
        // Get page info
        const info = await page.evaluate(() => ({
            title: document.title,
            hasContent: document.body.innerHTML.length > 100,
            rootExists: !!document.getElementById('root'),
            rootHTML: document.getElementById('root')?.innerHTML.substring(0, 200) || 'No root',
            bodyText: document.body.innerText || 'No text'
        }));
        
        console.log('Page loaded:', info.title);
        console.log('Has content:', info.hasContent);
        console.log('Root exists:', info.rootExists);
        console.log('Root HTML:', info.rootHTML);
        console.log('Body text:', info.bodyText);
        console.log('\nConsole logs:', logs.join('\n'));
        console.log('\nScreenshot saved as current-ui-state.png');
        
    } catch (e) {
        console.error('Error:', e.message);
    }
    
    await browser.close();
}

quickDiagnose();