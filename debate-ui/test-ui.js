const puppeteer = require('puppeteer');

async function testUI() {
  console.log('Starting UI test...');
  const browser = await puppeteer.launch({ 
    headless: false,
    defaultViewport: { width: 1280, height: 800 }
  });
  
  try {
    const page = await browser.newPage();
    
    // Enable console log monitoring
    page.on('console', msg => console.log('Browser console:', msg.text()));
    page.on('pageerror', error => console.log('Page error:', error.message));
    
    console.log('Navigating to http://localhost:3001...');
    await page.goto('http://localhost:3001', { waitUntil: 'networkidle2' });
    
    // Wait a bit for React to render
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    console.log('Page loaded, taking screenshot...');
    await page.screenshot({ path: 'ui-home-screenshot.png', fullPage: true });
    
    // Get page title
    const pageTitle = await page.title();
    console.log('Page title:', pageTitle);
    
    // Check for any content
    const hasContent = await page.$('*').catch(() => false);
    console.log('Page has content:', !!hasContent);
    
    // Get all text content
    const allText = await page.evaluate(() => document.body.innerText || document.body.textContent || '');
    console.log('Page text content:', allText ? allText.substring(0, 500) : 'No text content');
    
    // Check specifically for login form
    const loginForm = await page.$('form');
    if (loginForm) {
      console.log('Found login form!');
      
      // Try different selectors for username field
      const usernameSelectors = [
        'input[name="username"]',
        'input[name="email"]',
        'input[type="text"]',
        'input[placeholder*="username" i]',
        'input[placeholder*="email" i]',
        '#username',
        '#email'
      ];
      
      let usernameField = null;
      for (const selector of usernameSelectors) {
        usernameField = await page.$(selector);
        if (usernameField) {
          console.log(`Found username field with selector: ${selector}`);
          break;
        }
      }
      
      if (usernameField) {
        await usernameField.type('demo');
        
        // Find password field
        const passwordField = await page.$('input[type="password"]');
        if (passwordField) {
          await passwordField.type('demo123');
          
          // Find submit button
          const submitButton = await page.$('button[type="submit"], button:contains("Login"), button:contains("Sign in")');
          if (submitButton) {
            console.log('Clicking submit button...');
            await submitButton.click();
            
            await new Promise(resolve => setTimeout(resolve, 3000));
            console.log('After login URL:', page.url());
            await page.screenshot({ path: 'ui-after-login-screenshot.png', fullPage: true });
          }
        }
      }
    } else {
      console.log('No login form found - checking if already logged in or different page structure');
      
      // Check for app navigation or main content
      const mainContent = await page.$('main, [role="main"], .app-content, #root > div');
      if (mainContent) {
        console.log('Found main content area');
      }
    }
    
    // Get HTML structure
    const htmlStructure = await page.evaluate(() => {
      const root = document.getElementById('root') || document.body;
      return root.innerHTML.substring(0, 1000);
    });
    console.log('HTML structure preview:', htmlStructure);
    
  } catch (error) {
    console.error('Test error:', error);
  } finally {
    await browser.close();
    console.log('Test completed');
  }
}

testUI();