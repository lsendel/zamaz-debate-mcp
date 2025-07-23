const puppeteer = require('puppeteer');

(async () => {
    const browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();
    
    const errors = [];
    page.on('console', msg => {
        if (msg.type() === 'error') {
            errors.push(msg.text());
        }
    });
    
    page.on('pageerror', err => errors.push(`PAGE ERROR: ${err.message}`));
    
    await page.goto('http://localhost:3001');
    await page.waitForTimeout(3000);
    
    const content = await page.evaluate(() => ({
        hasRoot: !!document.getElementById('root'),
        rootContent: document.getElementById('root')?.innerHTML || '',
        bodyText: document.body.innerText || ''
    }));
    
    console.log('Errors found:', errors.length);
    errors.forEach(err => console.log('- ' + err));
    console.log('\nRoot exists:', content.hasRoot);
    console.log('Root has content:', content.rootContent.length > 0);
    console.log('Body text:', content.bodyText);
    
    await page.screenshot({ path: 'current-state.png' });
    console.log('\nScreenshot: current-state.png');
    
    await browser.close();
})();