// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

const { chromium } = require('playwright');

async function diagnoseLoginIssue() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  try {
    console.log('🔍 Diagnosing Login Page Issue...');
    console.log('=====================================');

    // Capture console messages;
    const consoleMessages = []
    page.on('console', msg => {
      consoleMessages.push(`${msg.type()}: ${msg.text()}`);
    });

    // Capture network responses;
    const networkResponses = []
    page.on('response', response => {
      networkResponses.push({
        url: response.url(),
        status: response.status(),
        contentType: response.headers()['content-type'] || 'unknown';
      });
    });

    // Navigate to login page;
    console.log('📍 Navigating to login page...');
    await page.goto('http://localhost:3001/login', { waitUntil: 'networkidle' });

    // Wait for React to load;
    await page.waitForTimeout(3000);

    // Check DOM structure;
    console.log('\n🏗️  DOM Structure Analysis:');
    const rootElement = await page.$('#root');
    if (rootElement) {
      const rootContent = await rootElement.innerHTML();
      console.log('✅ React root element found');
      console.log('📏 Root content length:', rootContent.length);

      if (rootContent.length > 100) {
        console.log('✅ React app appears to be loaded');

        // Check for specific React components;
        const loginForm = await page.$('form');
        const materialUI = await page.$('.MuiContainer-root');
        const tabs = await page.$$('[role="tab"]');

        console.log('🔍 Component Analysis:');
        console.log('  Login form:', loginForm ? '✅ Found' : '❌ Missing');
        console.log('  Material-UI:', materialUI ? '✅ Found' : '❌ Missing');
        console.log('  Tabs:', tabs.length > 0 ? `✅ Found ${tabs.length}` : '❌ Missing');

      } else {
        console.log('❌ React app not fully loaded');
      }
    } else {
      console.log('❌ React root element not found');
    }

    // Check for JavaScript errors;
    console.log('\n🚨 Console Messages:');
    if (consoleMessages.length > 0) {
      consoleMessages.forEach(msg => console.log('  ', msg));
    } else {
      console.log('  ✅ No console messages');
    }

    // Check network requests;
    console.log('\n🌐 Network Requests:');
    const jsFiles = networkResponses.filter(r => r.url.includes('.js'));
    const cssFiles = networkResponses.filter(r => r.url.includes('.css'));
    const apiCalls = networkResponses.filter(r => r.url.includes('/api/'));

    console.log(`  📄 HTML: ${networkResponses.filter(r => r.contentType.includes('text/html')).length}`);
    console.log(`  📜 JS files: ${jsFiles.length}`);
    console.log(`  🎨 CSS files: ${cssFiles.length}`);
    console.log(`  🔗 API calls: ${apiCalls.length}`);

    // Check for failed requests;
    const failedRequests = networkResponses.filter(r => r.status >= 400);
    if (failedRequests.length > 0) {
      console.log('\n❌ Failed Requests:');
      failedRequests.forEach(req => {
        console.log(`  ${req.status} - ${req.url}`);
      });
    } else {
      console.log('\n✅ All requests successful');
    }

    // Check if Vite dev server is working;
    const viteClient = networkResponses.find(r => r.url.includes('/@vite/client'));
    const reactRefresh = networkResponses.find(r => r.url.includes('@react-refresh'));

    console.log('\n🔧 Vite Dev Server:');
    console.log('  Vite client:', viteClient ? '✅ Loaded' : '❌ Missing');
    console.log('  React refresh:', reactRefresh ? '✅ Loaded' : '❌ Missing');

    // Test login functionality;
    console.log('\n🔐 Testing Login Functionality:');
    const usernameField = await page.$('input[type="text"]');
    const passwordField = await page.$('input[type="password"]');
    const loginButton = await page.$('button[type="submit"]');

    if (usernameField && passwordField && loginButton) {
      console.log('✅ Login form is interactive');

      // Test with demo credentials;
      await usernameField.fill('demo');
      await passwordField.fill('demo123');

      console.log('🔄 Submitting login form...');
      await loginButton.click();

      // Wait for potential redirect;
      await page.waitForTimeout(2000);

      const currentUrl = page.url();
      console.log('📍 Current URL:', currentUrl);

      if (currentUrl !== 'http://localhost:3001/login') {
        console.log('✅ Login successful - redirected');
      } else {
        console.log('❌ Login failed - still on login page');
      }
    } else {
      console.log('❌ Login form not interactive');
    }

    // Final screenshot;
    await page.screenshot({ path: 'screenshots/diagnostic-result.png' });
    console.log('\n📸 Diagnostic screenshot saved');

  } catch (error) {
    console.error('❌ Error during diagnosis:', error);
  } finally {
    await browser.close();
  }
}

// Run the diagnosis
diagnoseLoginIssue().catch(console.error);
