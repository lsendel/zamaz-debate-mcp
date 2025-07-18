const { chromium } = require('playwright');

async function testLoginPage() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  try {
    console.log('🔍 Testing Login Page...');
    
    // Navigate to login page
    console.log('📍 Navigating to login page...');
    await page.goto('http://localhost:3001/login');
    
    // Clear any existing auth data after navigation
    await page.evaluate(() => {
      try {
        localStorage.clear();
        sessionStorage.clear();
      } catch (e) {
        console.log('Could not clear storage:', e);
      }
    });
    
    // Wait for page to load
    await page.waitForTimeout(3000);
    
    // Take a screenshot
    await page.screenshot({ path: 'screenshots/login-page-initial.png' });
    console.log('📸 Screenshot taken: login-page-initial.png');
    
    // Check if page is completely blank
    const bodyContent = await page.textContent('body');
    console.log('📄 Page body content length:', bodyContent ? bodyContent.length : 0);
    
    if (!bodyContent || bodyContent.trim().length === 0) {
      console.log('❌ Page is completely blank!');
      
      // Check for any console errors
      const consoleErrors = [];
      page.on('console', msg => {
        if (msg.type() === 'error') {
          consoleErrors.push(msg.text());
        }
      });
      
      // Wait a bit more and check again
      await page.waitForTimeout(2000);
      
      console.log('🔍 Console errors found:', consoleErrors.length);
      consoleErrors.forEach(error => console.log('  ❌', error));
      
      // Check if React is loading
      const reactRoot = await page.$('#root');
      if (reactRoot) {
        const rootContent = await reactRoot.textContent();
        console.log('⚛️  React root content:', rootContent ? rootContent.substring(0, 100) + '...' : 'empty');
      } else {
        console.log('❌ React root element not found');
      }
      
      // Check if there are any network errors
      const networkErrors = [];
      page.on('response', response => {
        if (response.status() >= 400) {
          networkErrors.push(`${response.url()} - ${response.status()}`);
        }
      });
      
      await page.waitForTimeout(1000);
      console.log('🌐 Network errors:', networkErrors);
      
    } else {
      console.log('✅ Page has content');
      
      // Look for login form elements
      const loginForm = await page.$('form');
      const usernameField = await page.$('input[label="Username"], input[type="text"]');
      const passwordField = await page.$('input[type="password"]');
      const loginButton = await page.$('button[type="submit"]');
      
      console.log('🔍 Login form elements found:');
      console.log('  Form:', loginForm ? '✅' : '❌');
      console.log('  Username field:', usernameField ? '✅' : '❌');
      console.log('  Password field:', passwordField ? '✅' : '❌');
      console.log('  Login button:', loginButton ? '✅' : '❌');
      
      // Check for any visible text
      const pageText = await page.textContent('body');
      console.log('📝 Page text preview:', pageText.substring(0, 200) + '...');
      
      // Look for specific elements
      const title = await page.$('h1, h2, h3, h4');
      if (title) {
        const titleText = await title.textContent();
        console.log('📋 Page title:', titleText);
      }
      
      // Check for tabs
      const tabs = await page.$$('[role="tab"]');
      console.log('📑 Number of tabs found:', tabs.length);
      
      // Test login with demo credentials
      if (usernameField && passwordField && loginButton) {
        console.log('🔐 Testing login with demo credentials...');
        
        await usernameField.fill('demo');
        await passwordField.fill('demo123');
        await page.screenshot({ path: 'screenshots/login-page-filled.png' });
        
        await loginButton.click();
        await page.waitForTimeout(2000);
        
        await page.screenshot({ path: 'screenshots/login-page-after-submit.png' });
        console.log('📸 Screenshots taken for login flow');
        
        // Check current URL
        const currentUrl = page.url();
        console.log('🧭 Current URL after login:', currentUrl);
      }
    }
    
    // Take a final screenshot
    await page.screenshot({ path: 'screenshots/login-page-final.png' });
    console.log('📸 Final screenshot taken');
    
  } catch (error) {
    console.error('❌ Error during login page test:', error);
    await page.screenshot({ path: 'screenshots/login-page-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testLoginPage().catch(console.error);