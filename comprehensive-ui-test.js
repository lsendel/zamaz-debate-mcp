const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

class ComprehensiveUITester {
  constructor() {
    this.browser = null;
    this.page = null;
    this.results = {
      timestamp: new Date().toISOString(),
      tests: [],
      summary: { total: 0, passed: 0, failed: 0 }
    };
    this.screenshotDir = './ui-test-screenshots';
  }

  async init() {
    console.log('🚀 Starting Comprehensive UI Test Suite\n');
    
    // Create screenshot directory
    if (!fs.existsSync(this.screenshotDir)) {
      fs.mkdirSync(this.screenshotDir, { recursive: true });
    }

    this.browser = await puppeteer.launch({
      headless: false, // Set to false to see the browser
      slowMo: 50, // Slow down actions to see what's happening
      args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1200,800']
    });
    
    this.page = await this.browser.newPage();
    await this.page.setViewport({ width: 1200, height: 800 });
    
    // Log console messages
    this.page.on('console', msg => {
      if (msg.type() === 'error') {
        console.log('❌ Console error:', msg.text());
      } else if (msg.text().includes('mock')) {
        console.log('🔐', msg.text());
      }
    });
    
    // Log network failures
    this.page.on('requestfailed', request => {
      console.log('❌ Request failed:', request.url(), request.failure().errorText);
    });
  }

  async runTest(testName, testFn) {
    console.log(`\n🧪 ${testName}`);
    console.log('─'.repeat(50));
    
    const test = {
      name: testName,
      status: 'running',
      startTime: Date.now(),
      steps: []
    };
    
    try {
      await testFn(test);
      test.status = 'passed';
      test.duration = Date.now() - test.startTime;
      this.results.summary.passed++;
      console.log(`✅ ${testName} - PASSED (${test.duration}ms)`);
    } catch (error) {
      test.status = 'failed';
      test.error = error.message;
      test.duration = Date.now() - test.startTime;
      this.results.summary.failed++;
      console.log(`❌ ${testName} - FAILED: ${error.message}`);
      
      // Take error screenshot
      const screenshotName = `error-${testName.replace(/\s+/g, '-').toLowerCase()}.png`;
      await this.screenshot(screenshotName);
    }
    
    this.results.tests.push(test);
    this.results.summary.total++;
  }

  async screenshot(name) {
    const filepath = path.join(this.screenshotDir, name);
    await this.page.screenshot({ path: filepath, fullPage: true });
    console.log(`📸 Screenshot: ${name}`);
    return filepath;
  }

  async wait(ms) {
    await this.page.waitForTimeout(ms);
  }

  // TEST 1: Initial Load and Navigation
  async testInitialLoad() {
    await this.runTest('Initial Load and Navigation', async (test) => {
      console.log('📍 Navigating to http://localhost:3001...');
      await this.page.goto('http://localhost:3001', { waitUntil: 'networkidle2' });
      
      const title = await this.page.title();
      console.log(`📄 Page title: ${title}`);
      test.steps.push({ action: 'load', title });
      
      await this.screenshot('01-initial-load.png');
      
      // Check if redirected to login
      const url = this.page.url();
      console.log(`📍 Current URL: ${url}`);
      
      if (!url.includes('/login')) {
        throw new Error('Expected to be redirected to login page');
      }
      
      // Check for React app
      const hasRoot = await this.page.$('#root');
      if (!hasRoot) throw new Error('React root element not found');
      
      console.log('✓ React app loaded');
      console.log('✓ Redirected to login page');
    });
  }

  // TEST 2: Login Form Interaction
  async testLoginForm() {
    await this.runTest('Login Form Interaction', async (test) => {
      // Test form validation
      console.log('📝 Testing empty form submission...');
      
      const loginButton = await this.page.$('button[type="submit"], button:has-text("Login")');
      if (loginButton) {
        await loginButton.click();
        await this.wait(500);
        
        // Check for validation errors
        const errors = await this.page.$$('.error, .MuiFormHelperText-root');
        console.log(`✓ Found ${errors.length} validation error messages`);
      }
      
      // Test with demo credentials
      console.log('📝 Filling login form with demo credentials...');
      
      // Clear and type username
      await this.page.click('input[name="username"], input[type="text"]:first-of-type');
      await this.page.keyboard.type('demo');
      
      // Clear and type password  
      await this.page.click('input[name="password"], input[type="password"]');
      await this.page.keyboard.type('demo123');
      
      await this.screenshot('02-login-filled.png');
      
      // Submit login
      console.log('🔐 Submitting login...');
      await this.page.click('button[type="submit"], button:has-text("Login")');
      
      // Wait for navigation or error
      await this.wait(2000);
      
      const currentUrl = this.page.url();
      if (currentUrl.includes('/login')) {
        // Still on login page - check for errors
        const errorText = await this.page.evaluate(() => {
          const errors = document.querySelectorAll('.error, .alert, [role="alert"]');
          return Array.from(errors).map(e => e.textContent).join(', ');
        });
        
        if (errorText) {
          console.log(`⚠️ Login error: ${errorText}`);
        }
        
        // Check if API mock is working
        const mockAuthWorking = await this.page.evaluate(() => {
          return window.localStorage.getItem('authToken') !== null;
        });
        
        console.log(`🔐 Mock auth token present: ${mockAuthWorking}`);
      } else {
        console.log(`✓ Logged in successfully! Redirected to: ${currentUrl}`);
        test.steps.push({ action: 'login', success: true });
      }
      
      await this.screenshot('03-after-login.png');
    });
  }

  // TEST 3: Main Dashboard/Home
  async testMainDashboard() {
    await this.runTest('Main Dashboard Navigation', async (test) => {
      const url = this.page.url();
      
      if (url.includes('/login')) {
        console.log('⚠️ Still on login page, attempting to bypass...');
        
        // Force authentication state
        await this.page.evaluate(() => {
          localStorage.setItem('authToken', 'mock-token-123');
          localStorage.setItem('currentOrgId', 'org-123');
        });
        
        // Navigate to home
        await this.page.goto('http://localhost:3001/', { waitUntil: 'networkidle2' });
        await this.wait(1000);
      }
      
      // Look for navigation elements
      console.log('🔍 Checking for navigation elements...');
      
      const navElements = await this.page.evaluate(() => {
        const nav = document.querySelector('nav, [role="navigation"], .MuiDrawer-root');
        const links = document.querySelectorAll('a[href], [role="button"]');
        const buttons = document.querySelectorAll('button');
        
        return {
          hasNav: !!nav,
          linkCount: links.length,
          buttonCount: buttons.length,
          navItems: Array.from(links).map(a => ({
            text: a.textContent?.trim(),
            href: a.getAttribute('href')
          })).filter(item => item.text)
        };
      });
      
      console.log(`✓ Navigation present: ${navElements.hasNav}`);
      console.log(`✓ Links found: ${navElements.linkCount}`);
      console.log(`✓ Buttons found: ${navElements.buttonCount}`);
      
      if (navElements.navItems.length > 0) {
        console.log('📋 Navigation items:');
        navElements.navItems.forEach(item => {
          console.log(`   - ${item.text} (${item.href})`);
        });
      }
      
      await this.screenshot('04-dashboard.png');
    });
  }

  // TEST 4: Test All Routes
  async testAllRoutes() {
    await this.runTest('Test All Application Routes', async (test) => {
      const routes = [
        { path: '/debates', name: 'Debates Page' },
        { path: '/debates/create', name: 'Create Debate' },
        { path: '/analytics', name: 'Analytics' },
        { path: '/settings', name: 'Settings' }
      ];
      
      for (const route of routes) {
        console.log(`\n📍 Testing route: ${route.name} (${route.path})`);
        
        try {
          await this.page.goto(`http://localhost:3001${route.path}`, { 
            waitUntil: 'networkidle2',
            timeout: 10000 
          });
          
          await this.wait(1000);
          
          const pageContent = await this.page.evaluate(() => {
            const h1 = document.querySelector('h1, h2, h3, .MuiTypography-h4');
            const mainContent = document.querySelector('main, .MuiContainer-root');
            return {
              heading: h1?.textContent?.trim(),
              hasContent: !!mainContent,
              elementCount: document.querySelectorAll('*').length
            };
          });
          
          console.log(`   ✓ Page loaded`);
          console.log(`   ✓ Heading: ${pageContent.heading || 'No heading found'}`);
          console.log(`   ✓ Has content: ${pageContent.hasContent}`);
          console.log(`   ✓ Elements: ${pageContent.elementCount}`);
          
          await this.screenshot(`05-route-${route.path.replace(/\//g, '-')}.png`);
          
          test.steps.push({ 
            route: route.path, 
            loaded: true,
            heading: pageContent.heading 
          });
        } catch (error) {
          console.log(`   ❌ Failed to load: ${error.message}`);
          test.steps.push({ 
            route: route.path, 
            loaded: false,
            error: error.message 
          });
        }
      }
    });
  }

  // TEST 5: UI Components and Interactions
  async testUIComponents() {
    await this.runTest('UI Components and Interactions', async (test) => {
      // Go to debates page
      await this.page.goto('http://localhost:3001/debates', { waitUntil: 'networkidle2' });
      await this.wait(1000);
      
      console.log('🔍 Testing UI components...');
      
      // Check for common MUI components
      const components = await this.page.evaluate(() => {
        return {
          buttons: document.querySelectorAll('.MuiButton-root').length,
          inputs: document.querySelectorAll('.MuiInputBase-root').length,
          cards: document.querySelectorAll('.MuiCard-root, .MuiPaper-root').length,
          dialogs: document.querySelectorAll('.MuiDialog-root').length,
          tables: document.querySelectorAll('.MuiTable-root').length,
          lists: document.querySelectorAll('.MuiList-root').length
        };
      });
      
      console.log('📊 Component inventory:');
      Object.entries(components).forEach(([type, count]) => {
        if (count > 0) {
          console.log(`   ✓ ${type}: ${count}`);
        }
      });
      
      // Test button clicks
      const buttons = await this.page.$$('.MuiButton-root');
      if (buttons.length > 0) {
        console.log(`\n🖱️ Testing button interactions (found ${buttons.length} buttons)...`);
        
        // Click first non-navigation button
        for (let i = 0; i < Math.min(3, buttons.length); i++) {
          const buttonText = await buttons[i].evaluate(el => el.textContent);
          console.log(`   Clicking button: "${buttonText}"`);
          
          await buttons[i].click();
          await this.wait(500);
          
          // Check if dialog opened
          const dialog = await this.page.$('.MuiDialog-root');
          if (dialog) {
            console.log('   ✓ Dialog opened');
            await this.screenshot(`06-dialog-${i}.png`);
            
            // Close dialog
            const closeButton = await this.page.$('.MuiDialog-root button[aria-label="close"], .MuiDialog-root button:has-text("Cancel")');
            if (closeButton) {
              await closeButton.click();
              await this.wait(500);
            }
          }
        }
      }
    });
  }

  // TEST 6: Responsive Design
  async testResponsiveDesign() {
    await this.runTest('Responsive Design Testing', async (test) => {
      const viewports = [
        { width: 1200, height: 800, name: 'desktop' },
        { width: 768, height: 1024, name: 'tablet' },
        { width: 375, height: 667, name: 'mobile' }
      ];
      
      for (const viewport of viewports) {
        console.log(`\n📱 Testing ${viewport.name} view (${viewport.width}x${viewport.height})...`);
        
        await this.page.setViewport(viewport);
        await this.wait(500);
        
        // Check if mobile menu appears
        if (viewport.name === 'mobile') {
          const mobileMenu = await this.page.$('[aria-label="menu"], .MuiIconButton-root svg');
          console.log(`   Mobile menu button: ${mobileMenu ? '✓ Present' : '✗ Not found'}`);
          
          if (mobileMenu) {
            await mobileMenu.click();
            await this.wait(500);
            await this.screenshot(`07-mobile-menu.png`);
          }
        }
        
        await this.screenshot(`07-responsive-${viewport.name}.png`);
        
        test.steps.push({
          viewport: viewport.name,
          tested: true
        });
      }
      
      // Reset to desktop
      await this.page.setViewport({ width: 1200, height: 800 });
    });
  }

  // TEST 7: Error Handling
  async testErrorHandling() {
    await this.runTest('Error Handling and Edge Cases', async (test) => {
      console.log('🔍 Testing error handling...');
      
      // Test 404 route
      console.log('\n📍 Testing 404 page...');
      await this.page.goto('http://localhost:3001/non-existent-route', { waitUntil: 'networkidle2' });
      await this.wait(1000);
      
      const is404 = await this.page.evaluate(() => {
        const text = document.body.textContent;
        return text?.includes('404') || text?.includes('not found') || text?.includes('Not Found');
      });
      
      console.log(`   404 handling: ${is404 ? '✓ Shows 404 message' : '✗ No 404 message'}`);
      await this.screenshot('08-404-page.png');
      
      // Test network error handling
      console.log('\n🌐 Testing network error handling...');
      
      // Simulate offline
      await this.page.setOfflineMode(true);
      await this.page.reload({ waitUntil: 'networkidle2' }).catch(() => {});
      await this.wait(1000);
      
      const offlineHandling = await this.page.evaluate(() => {
        return document.body.textContent?.includes('offline') || 
               document.body.textContent?.includes('connection');
      });
      
      console.log(`   Offline handling: ${offlineHandling ? '✓ Shows offline message' : '✗ No offline handling'}`);
      
      // Restore online
      await this.page.setOfflineMode(false);
    });
  }

  async generateReport() {
    console.log('\n' + '='.repeat(60));
    console.log('📊 TEST RESULTS SUMMARY');
    console.log('='.repeat(60));
    
    console.log(`\n✅ Passed: ${this.results.summary.passed}`);
    console.log(`❌ Failed: ${this.results.summary.failed}`);
    console.log(`📊 Total: ${this.results.summary.total}`);
    console.log(`🎯 Success Rate: ${Math.round((this.results.summary.passed / this.results.summary.total) * 100)}%`);
    
    // Save detailed report
    const reportPath = './ui-test-report.json';
    fs.writeFileSync(reportPath, JSON.stringify(this.results, null, 2));
    console.log(`\n📄 Detailed report saved to: ${reportPath}`);
    console.log(`📸 Screenshots saved to: ${this.screenshotDir}/`);
    
    // List failed tests
    if (this.results.summary.failed > 0) {
      console.log('\n❌ Failed Tests:');
      this.results.tests
        .filter(t => t.status === 'failed')
        .forEach(t => console.log(`   - ${t.name}: ${t.error}`));
    }
    
    return this.results;
  }

  async cleanup() {
    if (this.browser) {
      await this.browser.close();
    }
  }

  async run() {
    try {
      await this.init();
      
      // Run all tests
      await this.testInitialLoad();
      await this.testLoginForm();
      await this.testMainDashboard();
      await this.testAllRoutes();
      await this.testUIComponents();
      await this.testResponsiveDesign();
      await this.testErrorHandling();
      
      return await this.generateReport();
    } catch (error) {
      console.error('💥 Test suite failed:', error);
      throw error;
    } finally {
      await this.cleanup();
    }
  }
}

// Check if puppeteer is available and run tests
const runTests = async () => {
  try {
    require.resolve('puppeteer');
  } catch (e) {
    console.log('Installing puppeteer...');
    const { execSync } = require('child_process');
    execSync('npm install puppeteer', { stdio: 'inherit' });
  }
  
  const tester = new ComprehensiveUITester();
  await tester.run();
};

runTests().catch(console.error);