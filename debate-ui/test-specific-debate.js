const puppeteer = require('puppeteer');

(async () => {
  console.log('🔍 Testing Specific Debate: debate-1753026915347\n');
  
  const browser = await puppeteer.launch({ 
    headless: false,
    args: ['--window-size=1400,900']
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1400, height: 900 });
  
  // Listen for console errors
  page.on('console', msg => {
    const type = msg.type();
    console.log(`CONSOLE ${type}: ${msg.text()}`);
  });
  
  // Listen for page errors
  page.on('pageerror', err => {
    console.error('PAGE ERROR:', err.message);
  });
  
  // Listen for network requests
  page.on('response', response => {
    if (response.url().includes('/api/')) {
      console.log(`API ${response.status()}: ${response.url()}`);
      if (response.status() >= 400) {
        response.text().then(body => {
          console.error('API ERROR BODY:', body);
        });
      }
    }
  });
  
  try {
    // Login
    console.log('Step 1: Logging in...');
    await page.goto('http://localhost:3001/login');
    await page.waitForSelector('input[placeholder="Username"]', { timeout: 5000 });
    
    await page.type('input[placeholder="Username"]', 'demo');
    await page.type('input[placeholder="Password"]', 'demo123');
    
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle0' }),
      page.click('button[type="submit"]')
    ]);
    console.log('✅ Login successful\n');
    
    // Navigate to the specific debate
    console.log('Step 2: Navigating to debate-1753026915347...');
    await page.goto('http://localhost:3001/debates/debate-1753026915347');
    
    // Wait for content or timeout
    console.log('Step 3: Waiting for page content...');
    
    try {
      // Wait for either content to load or an error to appear
      await Promise.race([
        page.waitForSelector('.ant-card', { timeout: 10000 }),
        page.waitForSelector('.ant-alert', { timeout: 10000 }),
        page.waitForSelector('.ant-spin', { timeout: 10000 })
      ]);
      console.log('✅ Some content appeared\n');
    } catch (err) {
      // Log error for debugging
      console.error('[test-specific-debate] Error:', err);
      // Rethrow if critical
      if (err.critical) throw err;
        console.error("Error:", e);
      console.log('❌ No content appeared within timeout\n');
      console.error("Error:", error);
    }
    
    // Take screenshot of current state
    await page.screenshot({ path: 'specific-debate-state.png', fullPage: true });
    console.log('📸 Screenshot saved: specific-debate-state.png\n');
    
    // Check what's visible on the page
    const pageContent = await page.evaluate(() => {
      const body = document.body;
      const hasCard = !!document.querySelector('.ant-card');
      const hasAlert = !!document.querySelector('.ant-alert');
      const hasSpinner = !!document.querySelector('.ant-spin');
      const hasError = !!document.querySelector('.ant-alert-error');
      const bodyText = body.innerText.slice(0, 200);
      
      return {
        hasCard,
        hasAlert,
        hasSpinner,
        hasError,
        bodyText,
        childCount: body.children.length
      };
    });
    
    console.log('Page Analysis:');
    console.log(`- Has Cards: ${pageContent.hasCard ? '✅' : '❌'}`);
    console.log(`- Has Alerts: ${pageContent.hasAlert ? '✅' : '❌'}`);
    console.log(`- Has Spinner: ${pageContent.hasSpinner ? '✅' : '❌'}`);
    console.log(`- Has Error: ${pageContent.hasError ? '✅' : '❌'}`);
    console.log(`- Body Children: ${pageContent.childCount}`);
    console.log(`- Visible Text: "${pageContent.bodyText}"\n`);
    
    // Check if React rendered properly
    const reactRoot = await page.$('#root');
    if (reactRoot) {
      const rootContent = await reactRoot.evaluate(el => el.innerHTML.length);
      console.log(`React Root Content Length: ${rootContent} characters`);
      if (rootContent < 100) {
        console.log('❌ React root appears empty or minimal');
      }
    }
    
  } catch (error) {
    console.error('Test error:', error);
    await page.screenshot({ path: 'specific-debate-error.png' });
  }
  
  console.log('\nKeeping browser open for 30 seconds for manual inspection...');
  await new Promise(resolve => setTimeout(resolve, 30000));
  
  await browser.close();
})();