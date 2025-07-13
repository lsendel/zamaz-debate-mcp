const puppeteer = require('puppeteer');
const fs = require('fs').promises;
const path = require('path');

async function quickUITest() {
  console.log('🚀 Quick UI Test\n');
  
  const timestamp = new Date().toISOString().replace(/:/g, '-');
  const evidenceDir = path.join(__dirname, 'evidence', timestamp);
  await fs.mkdir(evidenceDir, { recursive: true });
  
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1920, height: 1080 });
    
    // Test 1: Check if UI is running
    console.log('1. Checking UI...');
    const response = await page.goto('http://localhost:3001', {
      waitUntil: 'domcontentloaded',
      timeout: 10000
    });
    
    if (response.ok()) {
      console.log('✅ UI is running');
      await page.screenshot({ 
        path: path.join(evidenceDir, '01-ui-running.png'),
        fullPage: true 
      });
    } else {
      throw new Error('UI not responding');
    }
    
    // Test 2: Check page content
    console.log('2. Checking page content...');
    await page.waitForTimeout(3000);
    
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
    
    // Test 4: Take final screenshot
    console.log('4. Taking final screenshot...');
    await page.screenshot({ 
      path: path.join(evidenceDir, '02-final-state.png'),
      fullPage: true 
    });
    
    // Summary
    console.log('\n✅ UI Test Complete!');
    console.log(`Evidence saved to: ${evidenceDir}`);
    console.log('\nSummary:');
    console.log(`- UI Running: ✅`);
    console.log(`- Title: ${pageInfo.title || 'Not found'}`);
    console.log(`- Organization: ${pageInfo.orgText || 'Not set'}`);
    console.log(`- Debates: ${pageInfo.debateCount}`);
    console.log(`- APIs: Debates=${apiStatus.debates?.status}, LLM=${apiStatus.llm?.status}`);
    
  } catch (error) {
    console.error('❌ Test failed:', error.message);
  } finally {
    await browser.close();
  }
}

quickUITest().catch(console.error);