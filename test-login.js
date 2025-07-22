const puppeteer = require('puppeteer');

async function testLogin() {
  console.log('🧪 Testing login functionality...\n');
  
  let browser;
  try {
    browser = await puppeteer.launch({ 
      headless: true,
      args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    const page = await browser.newPage();
    
    // Enable console logging
    page.on('console', msg => {
      if (msg.type() === 'error') {
        console.log('❌ Console error:', msg.text());
      }
    });
    
    // Navigate to login page
    console.log('1. Navigating to http://localhost:3001...');
    await page.goto('http://localhost:3001', { waitUntil: 'networkidle2' });
    
    // Take screenshot of login page
    await page.screenshot({ path: 'login-page-test.png' });
    console.log('📸 Screenshot saved: login-page-test.png');
    
    // Check if we're on login page
    const url = page.url();
    console.log('📍 Current URL:', url);
    
    // Wait for login form
    console.log('\n2. Looking for login form...');
    await page.waitForSelector('input[type="text"], input[name="username"], #username', { timeout: 5000 });
    
    // Find username and password fields
    const usernameSelector = await page.evaluateHandle(() => {
      const inputs = document.querySelectorAll('input');
      for (let input of inputs) {
        if (input.type === 'text' || input.name === 'username' || input.id === 'username' || 
            input.placeholder?.toLowerCase().includes('username')) {
          return input;
        }
      }
      return null;
    });
    
    const passwordSelector = await page.evaluateHandle(() => {
      const inputs = document.querySelectorAll('input');
      for (let input of inputs) {
        if (input.type === 'password' || input.name === 'password' || input.id === 'password' ||
            input.placeholder?.toLowerCase().includes('password')) {
          return input;
        }
      }
      return null;
    });
    
    if (!usernameSelector || !passwordSelector) {
      throw new Error('Could not find username or password fields');
    }
    
    console.log('✅ Found username and password fields');
    
    // Type credentials
    console.log('\n3. Entering credentials...');
    console.log('   Username: demo');
    console.log('   Password: demo123');
    
    await usernameSelector.click();
    await page.keyboard.type('demo');
    
    await passwordSelector.click();
    await page.keyboard.type('demo123');
    
    // Take screenshot before clicking login
    await page.screenshot({ path: 'login-filled-test.png' });
    console.log('📸 Screenshot saved: login-filled-test.png');
    
    // Find and click login button
    console.log('\n4. Looking for login button...');
    const loginButton = await page.evaluateHandle(() => {
      const buttons = document.querySelectorAll('button');
      for (let button of buttons) {
        if (button.textContent?.toLowerCase().includes('login') ||
            button.textContent?.toLowerCase().includes('sign in')) {
          return button;
        }
      }
      // Also check for input type submit
      const submits = document.querySelectorAll('input[type="submit"]');
      if (submits.length > 0) return submits[0];
      return null;
    });
    
    if (!loginButton) {
      throw new Error('Could not find login button');
    }
    
    console.log('✅ Found login button');
    
    // Set up response listener
    let loginResponse = null;
    page.on('response', response => {
      if (response.url().includes('/auth/login') || response.url().includes('/login')) {
        loginResponse = {
          url: response.url(),
          status: response.status(),
          statusText: response.statusText()
        };
      }
    });
    
    // Click login
    console.log('5. Clicking login button...');
    await loginButton.click();
    
    // Wait for navigation or response
    await page.waitForTimeout(3000);
    
    // Check results
    console.log('\n📊 Results:');
    if (loginResponse) {
      console.log(`   API Response: ${loginResponse.status} ${loginResponse.statusText}`);
      console.log(`   URL: ${loginResponse.url}`);
    }
    
    const finalUrl = page.url();
    console.log(`   Final URL: ${finalUrl}`);
    
    // Take final screenshot
    await page.screenshot({ path: 'login-result-test.png', fullPage: true });
    console.log('📸 Screenshot saved: login-result-test.png');
    
    // Check if login was successful
    if (finalUrl.includes('/login')) {
      console.log('\n❌ Login failed - still on login page');
      
      // Check for error messages
      const errorText = await page.evaluate(() => {
        const errors = document.querySelectorAll('.error, .alert, [role="alert"]');
        return Array.from(errors).map(e => e.textContent).join(', ');
      });
      
      if (errorText) {
        console.log('   Error message:', errorText);
      }
    } else {
      console.log('\n✅ Login successful! Redirected to:', finalUrl);
    }
    
  } catch (error) {
    console.error('\n❌ Test failed:', error.message);
    if (browser) {
      const page = await browser.newPage();
      await page.screenshot({ path: 'error-test.png' });
    }
  } finally {
    if (browser) {
      await browser.close();
    }
  }
}

// Check if puppeteer is available
try {
  require.resolve('puppeteer');
  testLogin();
} catch (e) {
    console.error("Error:", e);
  console.log('Installing puppeteer first...');
  const { execSync } = require('child_process');
  execSync('npm install puppeteer', { stdio: 'inherit' });
  testLogin();
  console.error("Error:", error);
}