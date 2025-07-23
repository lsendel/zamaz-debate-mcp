const puppeteer = require('puppeteer');

(async () => {
    const browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();
    
    page.on('console', msg => console.log('Browser:', msg.text()));
    page.on('pageerror', err => console.log('Error:', err.message));
    
    await page.goto('http://localhost:3001');
    await page.waitForTimeout(2000);
    
    await page.screenshot({ path: 'ui-now.png' });
    
    const hasContent = await page.evaluate(() => {
        const root = document.getElementById('root');
        return root && root.innerHTML.length > 10;
    });
    
    console.log('UI has content:', hasContent);
    console.log('Screenshot: ui-now.png');
    
    await browser.close();
})();