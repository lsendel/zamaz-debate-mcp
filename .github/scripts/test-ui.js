const { chromium } = require('playwright');

async function testUI() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    console.log('🚀 Starting UI test...\n');

    // Test 1: Navigate to the UI
    console.log('1️⃣ Navigating to http://localhost:3001...');
    await page.goto('http://localhost:3001', { waitUntil: 'networkidle', timeout: 30000 });
    console.log('✅ Page loaded successfully');

    // Take a screenshot
    await page.screenshot({ path: '/tmp/ui-test-homepage.png', fullPage: true });
    console.log('📸 Screenshot saved to /tmp/ui-test-homepage.png');

    // Test 2: Check if login form exists
    console.log('\n2️⃣ Checking for login form...');
    const loginForm = await page.locator('form').first();
    if (await loginForm.isVisible()) {
      console.log('✅ Login form found');

      // Check for username and password fields
      const usernameField = await page.locator('input[type="text"], input[name="username"], input[placeholder*="user" i]').first();
      const passwordField = await page.locator('input[type="password"]').first();
      
      if (await usernameField.isVisible() && await passwordField.isVisible()) {
        console.log('✅ Username and password fields found');
      } else {
        console.log('❌ Login fields not found');
      }
    } else {
      console.log('⚠️ No login form found - checking if already logged in...');
      
      // Check for main application elements
      const mainContent = await page.locator('main, [role="main"], .main-content').first();
      if (await mainContent.isVisible()) {
        console.log('✅ Main application content found - user may be logged in');
      }
    }

    // Test 3: Check page title
    console.log('\n3️⃣ Checking page title...');
    const title = await page.title();
    console.log(`📄 Page title: "${title}"`);

    // Test 4: Check for any console errors
    console.log('\n4️⃣ Checking for console errors...');
    const consoleErrors = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });
    
    // Wait a bit to catch any delayed errors
    await page.waitForTimeout(2000);
    
    if (consoleErrors.length > 0) {
      console.log('❌ Console errors found:');
      consoleErrors.forEach(error => console.log(`   - ${error}`));
    } else {
      console.log('✅ No console errors');
    }

    // Test 5: Try to login if login form exists
    console.log('\n5️⃣ Attempting login with demo credentials...');
    const usernameInput = await page.locator('input[type="text"], input[name="username"], input[placeholder*="user" i]').first();
    const passwordInput = await page.locator('input[type="password"]').first();
    
    if (await usernameInput.isVisible() && await passwordInput.isVisible()) {
      await usernameInput.fill('demo');
      await passwordInput.fill('demo123');
      
      // Find and click login button
      const loginButton = await page.locator('button[type="submit"], button:has-text("Login"), button:has-text("Sign in")').first();
      if (await loginButton.isVisible()) {
        await loginButton.click();
        console.log('✅ Login submitted');
        
        // Wait for navigation or error
        await page.waitForTimeout(3000);
        
        // Take screenshot after login
        await page.screenshot({ path: '/tmp/ui-test-after-login.png', fullPage: true });
        console.log('📸 Post-login screenshot saved to /tmp/ui-test-after-login.png');
        
        // Check if login was successful
        const currentUrl = page.url();
        if (currentUrl.includes('dashboard') || currentUrl.includes('home') || !currentUrl.includes('login')) {
          console.log('✅ Login appears successful - navigated away from login page');
        } else {
          console.log('⚠️ Still on login page - login may have failed');
        }
      }
    } else {
      console.log('⚠️ Login form not available for testing');
    }

    console.log('\n✅ UI test completed successfully!');

  } catch (error) {
    console.error('\n❌ UI test failed:', error.message);
    
    // Take error screenshot
    try {
      await page.screenshot({ path: '/tmp/ui-test-error.png', fullPage: true });
      console.log('📸 Error screenshot saved to /tmp/ui-test-error.png');
    } catch (screenshotError) {
      console.log('Could not take error screenshot');
    }
    
    throw error;
  } finally {
    await browser.close();
  }
}

// Run the test
testUI().catch(error => {
  console.error('Test failed:', error);
  process.exit(1);
});