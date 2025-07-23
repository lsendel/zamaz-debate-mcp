const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: false });
  const page = await browser.newPage();
  
  // Set up console monitoring before navigation
  const errors = [];
  page.on('console', msg => {
    console.log(`[${msg.type()}]`, msg.text());
    if (msg.type() === 'error') {
      errors.push(msg.text());
    }
  });
  
  page.on('pageerror', error => {
    console.log('Page error:', error.message);
    errors.push(error.message);
  });
  
  try {
    // Go directly to home page while authenticated
    console.log('1. Setting auth token...');
    await page.goto('http://localhost:3001');
    
    // Set authentication in localStorage
    await page.evaluate(() => {
      localStorage.setItem('authToken', 'demo-token-123456');
      localStorage.setItem('currentOrgId', 'org-001');
    });
    
    console.log('2. Refreshing page with auth...');
    await page.reload();
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    // Check page state
    const currentUrl = page.url();
    console.log('3. Current URL:', currentUrl);
    
    const rootContent = await page.evaluate(() => {
      const root = document.getElementById('root');
      return {
        exists: !!root,
        innerHTML: root?.innerHTML || 'No root',
        childCount: root?.children.length || 0
      };
    });
    
    console.log('4. Root element:', rootContent);
    
    // Check if React is loaded
    const reactLoaded = await page.evaluate(() => {
      return {
        hasReact: typeof window.React !== 'undefined',
        hasReactDOM: typeof window.ReactDOM !== 'undefined',
        windowKeys: Object.keys(window).filter(k => k.includes('react') || k.includes('React')).slice(0, 10)
      };
    });
    
    console.log('5. React status:', reactLoaded);
    
    // Take screenshot
    await page.screenshot({ path: 'home-page-debug.png' });
    console.log('6. Screenshot saved as home-page-debug.png');
    
    console.log('7. Errors found:', errors);
    
  } catch (error) {
    console.error('Error:', error);
  } finally {
    await browser.close();
  }
})();