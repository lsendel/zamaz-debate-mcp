const puppeteer = require('puppeteer');
const fs = require('fs').promises;
const path = require('path');

describe('Quick UI Test', () => {
  let browser;
  let page;
  let evidenceDir;

  beforeAll(async () => {
    const timestamp = new Date().toISOString().replace(/:/g, '-');
    evidenceDir = path.join(__dirname, 'evidence', timestamp);
    await fs.mkdir(evidenceDir, { recursive: true });

    browser = await puppeteer.launch({
      headless: 'new',
      args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    page = await browser.newPage();
    await page.setViewport({ width: 1920, height: 1080 });
  });

  afterAll(async () => {
    await browser.close();
  });

  test('should load the UI and check basic elements', async () => {
    console.log('🚀 Quick UI Test\n');

    // Test 1: Check if UI is running
    console.log('1. Checking UI...');
    const port = process.env.UI_PORT || 3001;
    const response = await page.goto(`http://localhost:${port}`, {
      waitUntil: 'domcontentloaded',
      timeout: 10000
    });
    expect(response.ok()).toBe(true);
    console.log('✅ UI is running');
    await page.waitForSelector('h1');
    await page.screenshot({
      path: path.join(evidenceDir, '01-ui-running.png'),
      fullPage: true
    });

    // Test 2: Check page content
    console.log('2. Checking page content...');
    await new Promise(resolve => setTimeout(resolve, 3000));

    const pageInfo = await page.evaluate(() => {
      return {
        title: document.querySelector('h1')?.textContent,
        hasOrgSwitcher: !!document.querySelector('[aria-expanded]'),
        orgText: document.querySelector('[aria-expanded] span')?.textContent,
        hasNewDebateBtn: !!Array.from(document.querySelectorAll('button'))
          .find(btn => btn.textContent?.includes('New Debate')),
        debateCount: document.querySelectorAll('[class*="CardTitle"]').length,
        hasSkeleton: !!document.querySelector('.animate-pulse')
      };
    });

    console.log('Page Info:', JSON.stringify(pageInfo, null, 2));
    await fs.writeFile(
      path.join(evidenceDir, 'page-info.json'),
      JSON.stringify(pageInfo, null, 2)
    );
    expect(pageInfo.title).toBeTruthy();

    // Test 3: Check APIs
    console.log('3. Checking APIs...');
    const apiStatus = await page.evaluate(async () => {
      const results = {};
      try {
        const debateRes = await fetch('/api/debate/resources?uri=debate://debates');
        const debateData = await debateRes.json();
        results.debates = {
          status: debateRes.status,
          count: debateData.debates?.length || 0
        };
      } catch (e) {
        results.debates = { error: e.message };
      }
      try {
        const llmRes = await fetch('/api/llm/providers');
        const llmData = await llmRes.json();
        results.llm = {
          status: llmRes.status,
          count: llmData.providers?.length || 0
        };
      } catch (e) {
        results.llm = { error: e.message };
      }
      return results;
    });

    console.log('API Status:', JSON.stringify(apiStatus, null, 2));
    await fs.writeFile(
      path.join(evidenceDir, 'api-status.json'),
      JSON.stringify(apiStatus, null, 2)
    );
    expect(apiStatus.debates.status).toBe(200);
    expect(apiStatus.llm.status).toBe(200);

    // Test 4: Take final screenshot
    console.log('4. Taking final screenshot...');
    await page.screenshot({
      path: path.join(evidenceDir, '02-final-state.png'),
      fullPage: true
    });

    // Summary
    console.log('\n✅ UI Test Complete!');
    console.log(`Evidence saved to: ${evidenceDir}`);
  }, 60000);
});