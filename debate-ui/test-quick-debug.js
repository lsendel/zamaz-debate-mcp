const puppeteer = require('puppeteer');

async function quickDebug() {
  console.log('🔧 Quick Debug: Testing debate creation with real backend');
  
  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: null,
    args: ['--start-maximized']
  });
  
  const page = await browser.newPage();
  
  // Listen for API requests
  page.on('request', request => {
    if (request.url().includes('5013')) {
      console.log(`→ API Request: ${request.method()} ${request.url()}`);
    }
  });
  
  page.on('response', response => {
    if (response.url().includes('5013')) {
      console.log(`← API Response: ${response.status()} ${response.url()}`);
      if (!response.ok()) {
        console.error(`  ERROR: ${response.status()} ${response.statusText()}`);
      }
    }
  });
  
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.error(`🖥️ Console Error: ${msg.text()}`);
    }
  });
  
  try {
    console.log('→ Going to debates page...');
    await page.goto('http://localhost:3003/debates', { waitUntil: 'networkidle0' });
    
    console.log('→ Taking screenshot...');
    await page.screenshot({ path: './debug-debates.png', fullPage: true });
    
    // Check what's actually rendered
    const content = await page.evaluate(() => {
      return {
        title: document.title,
        bodyText: document.body.innerText.substring(0, 500),
        hasReactRoot: !!document.getElementById('root'),
        antComponents: document.querySelectorAll('[class*="ant-"]').length,
        errors: Array.from(document.querySelectorAll('.ant-alert-error, .error')).map(el => el.textContent),
        debates: Array.from(document.querySelectorAll('.ant-card, .debate-item')).length
      };
    });
    
    console.log('📊 Page Content Analysis:');
    console.log(`  Title: ${content.title}`);
    console.log(`  React Root: ${content.hasReactRoot}`);
    console.log(`  Ant Components: ${content.antComponents}`);
    console.log(`  Debates Found: ${content.debates}`);
    console.log(`  Errors: ${content.errors.length > 0 ? content.errors.join(', ') : 'None'}`);
    console.log(`  Body Preview: ${content.bodyText.substring(0, 200)}...`);
    
    // Try login if we're on login page
    if (content.bodyText.includes('Login') || content.bodyText.includes('Username')) {
      console.log('→ Detected login page, attempting login...');
      
      try {
        await page.type('input[placeholder="Username"]', 'demo', { delay: 100 });
        await page.type('input[placeholder="Password"]', 'demo123', { delay: 100 });
        await page.click('button[type="submit"]');
        
        await page.waitForNavigation({ waitUntil: 'networkidle0', timeout: 10000 });
        console.log('✓ Login successful');
        
        await page.screenshot({ path: './debug-after-login.png', fullPage: true });
        
        // Check debates after login
        const debatesAfterLogin = await page.evaluate(() => {
          return {
            debates: Array.from(document.querySelectorAll('.ant-card, .debate-item')).length,
            bodyText: document.body.innerText.substring(0, 300)
          };
        });
        
        console.log(`  Debates after login: ${debatesAfterLogin.debates}`);
        console.log(`  Content: ${debatesAfterLogin.bodyText}`);
        
      } catch (loginError) {
        console.error('❌ Login failed:', loginError.message);
      }
    }
    
    // Wait for manual inspection
    console.log('\n⏸️ Browser staying open for inspection. Check the screenshots and browser.');
    console.log('Press Ctrl+C to close when done.');
    
    await new Promise(resolve => {
      process.on('SIGINT', () => {
        console.log('\n👋 Closing browser...');
        resolve();
      });
    });
    
  } catch (error) {
    console.error('❌ Debug failed:', error.message);
  } finally {
    await browser.close();
  }
}

quickDebug().catch(console.error);