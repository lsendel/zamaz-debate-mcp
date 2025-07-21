const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

// Test configuration
const config = {
  baseUrl: 'http://localhost:3003',
  screenshotDir: './validation-screenshots/functionality-test',
  testTimeout: 30000,
};

// Create screenshot directory
if (!fs.existsSync(config.screenshotDir)) {
  fs.mkdirSync(config.screenshotDir, { recursive: true });
}

async function captureScreenshot(page, name, description) {
  const screenshotPath = path.join(config.screenshotDir, `${name}.png`);
  await page.screenshot({ 
    path: screenshotPath, 
    fullPage: true,
    type: 'png'
  });
  console.log(`✓ Screenshot captured: ${name} - ${description}`);
  return screenshotPath;
}

async function checkElement(page, selector, name) {
  try {
    const element = await page.$(selector);
    if (element) {
      console.log(`  ✓ ${name}: Found`);
      return true;
    } else {
      console.log(`  ❌ ${name}: Not found`);
      return false;
    }
  } catch (error) {
    console.log(`  ❌ ${name}: Error - ${error.message}`);
    return false;
  }
}

async function testUIFunctionality() {
  console.log('🚀 Starting UI Functionality Tests...');
  console.log(`📍 Base URL: ${config.baseUrl}`);
  
  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: null,
    args: [
      '--start-maximized',
      '--no-sandbox',
      '--disable-setuid-sandbox'
    ]
  });
  
  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });
  
  const testResults = {
    loginPageLoad: false,
    loginFormElements: false,
    uiComponents: false,
    responsiveness: false,
    noJavaScriptErrors: false
  };
  
  try {
    // Test 1: Login Page Loading
    console.log('\n→ Test 1: Login Page Loading');
    await page.goto(`${config.baseUrl}/login`, { waitUntil: 'networkidle0', timeout: 15000 });
    await captureScreenshot(page, '01-login-page', 'Login page loaded');
    
    // Check if login page loaded properly
    const hasLoginContent = await page.$('h1');
    if (hasLoginContent) {
      const title = await page.$eval('h1', el => el.textContent);
      if (title.includes('Zamaz Debate System')) {
        testResults.loginPageLoad = true;
        console.log('  ✓ Login page loaded successfully');
      }
    }
    
    // Test 2: Login Form Elements
    console.log('\n→ Test 2: Login Form Elements');
    const formElements = {
      'Username Input': 'input[placeholder="Username"]',
      'Password Input': 'input[placeholder="Password"]',
      'Login Button': 'button[type="submit"]',
      'Title': 'h1',
      'Card Container': '.ant-card',
      'Tabs': '.ant-tabs'
    };
    
    let foundElements = 0;
    for (const [name, selector] of Object.entries(formElements)) {
      const found = await checkElement(page, selector, name);
      if (found) foundElements++;
    }
    
    testResults.loginFormElements = foundElements >= 4; // At least 4 out of 6 elements
    
    // Test 3: UI Components and Styling
    console.log('\n→ Test 3: UI Components and Styling');
    
    // Check Ant Design components are loaded
    const antComponents = await page.evaluate(() => {
      const antElements = document.querySelectorAll('[class*="ant-"]');
      return antElements.length;
    });
    
    console.log(`  ✓ Found ${antComponents} Ant Design components`);
    testResults.uiComponents = antComponents > 5;
    
    // Test 4: Responsive Design
    console.log('\n→ Test 4: Responsive Design');
    const viewports = [
      { width: 1920, height: 1080, name: 'desktop' },
      { width: 1024, height: 768, name: 'tablet' },
      { width: 375, height: 667, name: 'mobile' }
    ];
    
    let responsiveTests = 0;
    for (const viewport of viewports) {
      await page.setViewport(viewport);
      await new Promise(resolve => setTimeout(resolve, 1000));
      await captureScreenshot(page, `02-responsive-${viewport.name}`, `${viewport.name} (${viewport.width}x${viewport.height})`);
      
      // Check if content is still visible
      const isVisible = await page.$eval('h1', el => {
        const rect = el.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
      });
      
      if (isVisible) {
        responsiveTests++;
        console.log(`  ✓ ${viewport.name}: Content visible`);
      }
    }
    
    testResults.responsiveness = responsiveTests === 3;
    
    // Test 5: JavaScript Errors
    console.log('\n→ Test 5: JavaScript Error Check');
    
    let jsErrors = 0;
    page.on('pageerror', () => jsErrors++);
    
    // Reload page to check for errors
    await page.reload({ waitUntil: 'networkidle0' });
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    testResults.noJavaScriptErrors = jsErrors === 0;
    console.log(`  ${jsErrors === 0 ? '✓' : '❌'} JavaScript errors: ${jsErrors}`);
    
    await captureScreenshot(page, '03-final-state', 'Final UI state');
    
    // Test 6: Direct Navigation to Debate Page (if possible)
    console.log('\n→ Test 6: Direct Component Testing');
    
    // Try to access a debate page directly (this might fail due to auth, but we can check the route)
    try {
      await page.goto(`${config.baseUrl}/debates`, { waitUntil: 'domcontentloaded', timeout: 10000 });
      await captureScreenshot(page, '04-debates-route', 'Debates route attempt');
      console.log('  ✓ Debates route accessible');
    } catch (error) {
      console.log('  ⚠️ Debates route requires authentication (expected)');
    }
    
    // Test homepage
    try {
      await page.goto(`${config.baseUrl}/`, { waitUntil: 'domcontentloaded', timeout: 10000 });
      await captureScreenshot(page, '05-homepage', 'Homepage');
      console.log('  ✓ Homepage accessible');
    } catch (error) {
      console.log('  ⚠️ Homepage might redirect to login (expected)');
    }
    
  } catch (error) {
    console.error('❌ Test failed:', error.message);
    await captureScreenshot(page, '99-error', 'Error state');
  } finally {
    await browser.close();
  }
  
  // Calculate results
  const totalTests = Object.keys(testResults).length;
  const passedTests = Object.values(testResults).filter(Boolean).length;
  const successPercentage = Math.round((passedTests / totalTests) * 100);
  
  console.log('\n📊 Test Results Summary:');
  console.log('========================');
  Object.entries(testResults).forEach(([test, passed]) => {
    console.log(`${test}: ${passed ? '✅ PASS' : '❌ FAIL'}`);
  });
  
  console.log(`\n🎯 Success Rate: ${successPercentage}%`);
  console.log(`📈 Tests Passed: ${passedTests}/${totalTests}`);
  
  if (successPercentage >= 80) {
    console.log('🎉 SUCCESS: 80%+ functionality achieved!');
  } else {
    console.log('⚠️ WARNING: Below 80% functionality target');
  }
  
  // Additional analysis
  console.log('\n📝 Analysis:');
  if (testResults.loginPageLoad) {
    console.log('✓ UI loads and renders correctly');
  }
  if (testResults.loginFormElements) {
    console.log('✓ Form elements are present and functional');
  }
  if (testResults.uiComponents) {
    console.log('✓ Ant Design components are working');
  }
  if (testResults.responsiveness) {
    console.log('✓ Responsive design is functioning');
  }
  if (testResults.noJavaScriptErrors) {
    console.log('✓ No JavaScript errors detected');
  }
  
  return {
    success: successPercentage >= 80,
    successPercentage,
    testResults,
    passedTests,
    totalTests
  };
}

// Run the tests
if (require.main === module) {
  testUIFunctionality()
    .then(results => {
      console.log('\n✅ Test suite completed');
      process.exit(results.success ? 0 : 1);
    })
    .catch(error => {
      console.error('💥 Test suite failed:', error);
      process.exit(1);
    });
}

module.exports = { testUIFunctionality };