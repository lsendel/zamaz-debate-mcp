const { chromium } = require('playwright');

async function testCompleteAppFlow() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  try {
    console.log('🚀 Testing Complete App Flow...');
    console.log('===============================');
    
    // Set up console and error tracking
    const consoleMessages = [];
    const errors = [];
    
    page.on('console', msg => {
      consoleMessages.push(`[${msg.type()}] ${msg.text()}`);
    });
    
    page.on('pageerror', error => {
      errors.push(`Page Error: ${error.message}`);
    });
    
    // Step 1: Test Login
    console.log('\n🔐 STEP 1: Testing Login...');
    await page.goto('http://localhost:3001/login', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    
    // Clear any existing auth
    await page.evaluate(() => {
      localStorage.clear();
    });
    
    // Take screenshot of login page
    await page.screenshot({ path: 'screenshots/01-login-page.png' });
    
    // Fill login form
    const usernameField = await page.$('input[type="text"]');
    const passwordField = await page.$('input[type="password"]');
    const loginButton = await page.$('button[type="submit"]');
    
    if (!usernameField || !passwordField || !loginButton) {
      console.log('❌ Login form elements not found');
      return;
    }
    
    await usernameField.fill('demo');
    await passwordField.fill('demo123');
    await loginButton.click();
    
    // Wait for redirect
    await page.waitForTimeout(3000);
    
    const currentUrl = page.url();
    console.log('📍 URL after login:', currentUrl);
    
    // Step 2: Test Home Page
    console.log('\n🏠 STEP 2: Testing Home Page...');
    await page.screenshot({ path: 'screenshots/02-home-page.png' });
    
    // Check if page is blank
    const bodyContent = await page.textContent('body');
    console.log('📄 Home page content length:', bodyContent ? bodyContent.length : 0);
    
    if (!bodyContent || bodyContent.trim().length < 50) {
      console.log('❌ Home page appears blank!');
      
      // Check for any errors
      console.log('🚨 Checking for errors...');
      if (errors.length > 0) {
        console.log('❌ JavaScript Errors found:');
        errors.forEach(error => console.log('  ', error));
      }
      
      // Check console messages
      console.log('📝 Console messages:');
      consoleMessages.slice(-10).forEach(msg => console.log('  ', msg));
      
      // Check if React root has content
      const rootElement = await page.$('#root');
      if (rootElement) {
        const rootContent = await rootElement.innerHTML();
        console.log('⚛️  React root content length:', rootContent.length);
        
        if (rootContent.length > 0) {
          console.log('🔍 React root preview:', rootContent.substring(0, 200) + '...');
        }
      }
      
      // Check for loading states
      const loadingElements = await page.$$('[data-testid*="loading"], .loading, .spinner');
      console.log('⏳ Loading elements found:', loadingElements.length);
      
    } else {
      console.log('✅ Home page has content');
      
      // Look for specific elements
      const navigation = await page.$('nav, [role="navigation"]');
      const mainContent = await page.$('main, [role="main"]');
      
      console.log('🧭 Navigation found:', navigation ? '✅' : '❌');
      console.log('📄 Main content found:', mainContent ? '✅' : '❌');
      
      // Check for page title
      const pageTitle = await page.$('h1, h2, h3, h4');
      if (pageTitle) {
        const titleText = await pageTitle.textContent();
        console.log('📋 Page title:', titleText);
      }
    }
    
    // Step 3: Test Navigation
    console.log('\n🧭 STEP 3: Testing Navigation...');
    
    // Try to find navigation links
    const navLinks = await page.$$('a[href*="/"], button[role="tab"]');
    console.log('🔗 Navigation links found:', navLinks.length);
    
    // Test specific routes
    const routesToTest = [
      { path: '/debates', name: 'Debates' },
      { path: '/analytics', name: 'Analytics' },
      { path: '/settings', name: 'Settings' },
      { path: '/organization-management', name: 'Organization Management' }
    ];
    
    for (const route of routesToTest) {
      console.log(`\n📍 Testing ${route.name} page...`);
      
      try {
        await page.goto(`http://localhost:3001${route.path}`, { waitUntil: 'networkidle' });
        await page.waitForTimeout(2000);
        
        const content = await page.textContent('body');
        const contentLength = content ? content.length : 0;
        
        console.log(`📄 ${route.name} content length:`, contentLength);
        
        if (contentLength > 50) {
          console.log(`✅ ${route.name} page loaded successfully`);
          
          // Look for specific elements
          const title = await page.$('h1, h2, h3, h4');
          if (title) {
            const titleText = await title.textContent();
            console.log(`📋 ${route.name} title:`, titleText);
          }
          
        } else {
          console.log(`❌ ${route.name} page appears blank`);
        }
        
        // Take screenshot
        const filename = `screenshots/03-${route.name.toLowerCase().replace(/\s+/g, '-')}.png`;
        await page.screenshot({ path: filename });
        
      } catch (error) {
        console.log(`❌ Error testing ${route.name}:`, error.message);
      }
    }
    
    // Step 4: Test Organization Management Specifically
    console.log('\n🏢 STEP 4: Testing Organization Management...');
    
    await page.goto('http://localhost:3001/organization-management', { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);
    
    const orgContent = await page.textContent('body');
    console.log('📄 Organization page content length:', orgContent ? orgContent.length : 0);
    
    if (orgContent && orgContent.length > 100) {
      // Check for specific organization management elements
      const tabs = await page.$$('[role="tab"]');
      const orgCards = await page.$$('.MuiCard-root');
      const createButton = await page.$('button:has-text("Create Organization")');
      
      console.log('📑 Tabs found:', tabs.length);
      console.log('🏢 Organization cards found:', orgCards.length);
      console.log('➕ Create button found:', createButton ? '✅' : '❌');
      
      // Check for organization data
      const orgText = await page.textContent('body');
      const hasAcmeCorp = orgText.includes('Acme Corporation');
      const hasTechSolutions = orgText.includes('Tech Solutions');
      
      console.log('🏢 Acme Corporation found:', hasAcmeCorp ? '✅' : '❌');
      console.log('🏢 Tech Solutions found:', hasTechSolutions ? '✅' : '❌');
      
    } else {
      console.log('❌ Organization management page is blank');
    }
    
    await page.screenshot({ path: 'screenshots/04-organization-management.png' });
    
    // Step 5: Check API Connectivity
    console.log('\n🌐 STEP 5: Testing API Connectivity...');
    
    // Check if API calls are being made
    const apiCalls = [];
    page.on('response', response => {
      if (response.url().includes('/api/')) {
        apiCalls.push({
          url: response.url(),
          status: response.status()
        });
      }
    });
    
    // Refresh the organization page to trigger API calls
    await page.reload({ waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    
    console.log('🔗 API calls made:', apiCalls.length);
    apiCalls.forEach(call => {
      console.log(`  ${call.status} - ${call.url}`);
    });
    
    // Final summary
    console.log('\n📊 FINAL SUMMARY:');
    console.log('=================');
    console.log('JavaScript Errors:', errors.length);
    console.log('Console Messages:', consoleMessages.length);
    console.log('API Calls Made:', apiCalls.length);
    
    if (errors.length > 0) {
      console.log('\n🚨 JavaScript Errors:');
      errors.forEach(error => console.log('  ❌', error));
    }
    
    console.log('\n📸 Screenshots saved:');
    console.log('  - 01-login-page.png');
    console.log('  - 02-home-page.png');
    console.log('  - 03-debates.png');
    console.log('  - 03-analytics.png');
    console.log('  - 03-settings.png');
    console.log('  - 03-organization-management.png');
    console.log('  - 04-organization-management.png');
    
  } catch (error) {
    console.error('❌ Error during complete app test:', error);
  } finally {
    await browser.close();
  }
}

// Run the complete test
testCompleteAppFlow().catch(console.error);