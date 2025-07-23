const puppeteer = require('puppeteer');

(async () => {
    const browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();
    
    await page.goto('http://localhost:3001');
    await page.waitForTimeout(2000);
    
    const content = await page.evaluate(() => {
        const body = document.body;
        return {
            hasText: (body.innerText || '').includes('React is Working'),
            text: body.innerText || 'No text',
            hasButton: !!document.querySelector('button'),
            rootHTML: document.getElementById('root')?.innerHTML.substring(0, 200) || 'No root'
        };
    });
    
    console.log('Page contains "React is Working":', content.hasText);
    console.log('Has button:', content.hasButton);
    console.log('Text:', content.text);
    console.log('\nRoot HTML preview:', content.rootHTML);
    
    await page.screenshot({ path: 'test-app-screenshot.png' });
    console.log('\nScreenshot saved as test-app-screenshot.png');
    
    await browser.close();
})();