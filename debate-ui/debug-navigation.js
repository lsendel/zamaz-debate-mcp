const puppeteer = require('puppeteer');

async function debugNavigation() {
    const browser = await puppeteer.launch({ 
        headless: false,
        devtools: true
    });
    
    const page = await browser.newPage();
    
    console.log('1. Loading login page...');
    await page.goto('http://localhost:3001/login');
    await page.waitForSelector('form');
    
    console.log('2. Logging in...');
    await page.fill('input[name="username"], #username', 'demo');
    await page.fill('input[name="password"], input[type="password"]', 'demo123');
    await page.click('button:has-text("Login")');
    
    console.log('3. Waiting for navigation...');
    await page.waitForTimeout(3000);
    
    console.log('4. Checking page structure...');
    const pageInfo = await page.evaluate(() => {
        // Check what elements exist
        const elements = {
            url: window.location.href,
            hasAntMenu: !!document.querySelector('.ant-menu'),
            hasAntLayoutSider: !!document.querySelector('.ant-layout-sider'),
            hasNav: !!document.querySelector('nav'),
            hasHeader: !!document.querySelector('header'),
            hasSidebar: !!document.querySelector('[class*="sidebar"]'),
            
            // Get all elements with 'menu' in class
            menuElements: Array.from(document.querySelectorAll('[class*="menu"]')).map(el => ({
                tag: el.tagName,
                classes: el.className
            })),
            
            // Get all elements with 'nav' in class
            navElements: Array.from(document.querySelectorAll('[class*="nav"]')).map(el => ({
                tag: el.tagName,
                classes: el.className
            })),
            
            // Check specific links
            hasDebatesLink: !!document.querySelector('a[href*="debate"], button:contains("Debates"), [class*="debate"]'),
            hasOrgLink: !!document.querySelector('a[href*="organization"], button:contains("Organization")'),
            
            // Get page structure
            bodyClasses: document.body.className,
            rootContent: document.getElementById('root')?.innerHTML.substring(0, 500)
        };
        
        return elements;
    });
    
    console.log('\nPage Analysis:', JSON.stringify(pageInfo, null, 2));
    
    await page.screenshot({ path: 'debug-navigation.png' });
    console.log('\nScreenshot saved as debug-navigation.png');
    
    // Keep browser open for inspection
    console.log('\nBrowser remains open for manual inspection...');
    await new Promise(() => {});
}

debugNavigation().catch(console.error);