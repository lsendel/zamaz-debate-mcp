const puppeteer = require('puppeteer');

async function checkUIErrors() {
  const browser = await puppeteer.launch({ 
    headless: false, // Show browser
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
    devtools: true
  });
  
  const page = await browser.newPage();
  
  // Collect console messages
  const consoleMessages = [];
  page.on('console', msg => {
    consoleMessages.push({
      type: msg.type(),
      text: msg.text()
    });
  });
  
  // Collect errors
  const pageErrors = [];
  page.on('error', err => {
    pageErrors.push(err.toString());
  });
  
  page.on('pageerror', err => {
    pageErrors.push(err.toString());
  });
  
  try {
    console.log('Opening browser to check for errors...');
    await page.goto('http://localhost:3001', { waitUntil: 'domcontentloaded' });
    
    // Wait for the page to load
    await page.waitForTimeout(5000);
    
    // Check if debates are loading
    const debatesLoading = await page.$eval('body', body => body.textContent.includes('Loading debates...'));
    console.log('\nDebates still loading:', debatesLoading);
    
    // Check if organization switcher is loading
    const hasOrgSkeleton = await page.$('.h-10.w-48.bg-gray-200');
    console.log('Organization switcher loading:', !!hasOrgSkeleton);
    
    // Try to get the actual debate count
    const debateCount = await page.evaluate(() => {
      const debateCountEl = document.querySelector('p.text-3xl.font-bold');
      return debateCountEl ? debateCountEl.textContent : 'not found';
    });
    console.log('Debate count shown:', debateCount);
    
    // Check localStorage
    const localStorage = await page.evaluate(() => {
      const items = {};
      for (let i = 0; i < window.localStorage.length; i++) {
        const key = window.localStorage.key(i);
        items[key] = window.localStorage.getItem(key);
      }
      return items;
    });
    console.log('\nLocalStorage:', JSON.stringify(localStorage, null, 2));
    
    // Check network requests
    const failedRequests = [];
    page.on('requestfailed', request => {
      failedRequests.push({
        url: request.url(),
        failure: request.failure()
      });
    });
    
    // Wait a bit more
    await page.waitForTimeout(3000);
    
    console.log('\n=== Console Messages ===');
    consoleMessages.forEach(msg => {
      console.log(`${msg.type}: ${msg.text}`);
    });
    
    console.log('\n=== Page Errors ===');
    if (pageErrors.length === 0) {
      console.log('No page errors');
    } else {
      pageErrors.forEach(err => console.log(err));
    }
    
    console.log('\n=== Failed Requests ===');
    if (failedRequests.length === 0) {
      console.log('No failed requests');
    } else {
      failedRequests.forEach(req => console.log(req));
    }
    
    console.log('\nPress Ctrl+C to close the browser...');
    
  } catch (error) {
    console.error('Error:', error);
  }
  
  // Keep browser open
  await new Promise(() => {});
}

checkUIErrors();