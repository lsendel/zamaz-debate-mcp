const puppeteer = require('puppeteer');
const fs = require('fs').promises;
const path = require('path');

// Configuration
const BASE_URL = 'http://localhost:3001';
const EVIDENCE_DIR = path.join(__dirname, 'e2e-evidence', new Date().toISOString().replace(/:/g, '-'));

// Test results
const testResults = {
  passed: [],
  failed: [],
  evidence: [],
  timestamp: new Date().toISOString()
};

// Helper to take screenshots
async function screenshot(page, name, description) {
  const filename = `${name}-${Date.now()}.png`;
  const filepath = path.join(EVIDENCE_DIR, 'screenshots', filename);
  await page.screenshot({ path: filepath, fullPage: true });
  testResults.evidence.push({ type: 'screenshot', name, description, file: filename });
  console.log(`📸 Screenshot: ${name}`);
  return filepath;
}

// Helper to test and log
async function test(name, fn) {
  try {
    console.log(`\n🧪 Testing: ${name}`);
    await fn();
    testResults.passed.push(name);
    console.log(`✅ PASS: ${name}`);
  } catch (error) {
    testResults.failed.push({ test: name, error: error.message });
    console.log(`❌ FAIL: ${name} - ${error.message}`);
  }
}

// Main test function
async function runE2ETests() {
  console.log('🚀 Starting Comprehensive E2E Tests\n');
  console.log(`URL: ${BASE_URL}`);
  console.log(`Evidence: ${EVIDENCE_DIR}\n`);
  
  // Create evidence directories
  await fs.mkdir(path.join(EVIDENCE_DIR, 'screenshots'), { recursive: true });
  await fs.mkdir(path.join(EVIDENCE_DIR, 'data'), { recursive: true });
  
  // Launch browser
  const browser = await puppeteer.launch({ 
    headless: false,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
    defaultViewport: { width: 1920, height: 1080 }
  });
  
  const page = await browser.newPage();
  
  // Collect console logs
  const consoleLogs = [];
  page.on('console', msg => {
    consoleLogs.push({ type: msg.type(), text: msg.text() });
  });
  
  try {
    // Test 1: Page Load
    await test('Page loads successfully', async () => {
      await page.goto(BASE_URL, { waitUntil: 'networkidle2' });
      await page.waitForTimeout(2000);
      await screenshot(page, '01-initial-load', 'Initial page load');
      
      const title = await page.$eval('h1', el => el.textContent);
      if (!title.includes('AI Debate System')) {
        throw new Error('Title not found');
      }
    });
    
    // Test 2: Organization Management
    await test('Organization switcher works', async () => {
      // Check localStorage
      const orgData = await page.evaluate(() => {
        return {
          currentOrgId: localStorage.getItem('currentOrganizationId'),
          organizations: JSON.parse(localStorage.getItem('organizations') || '[]')
        };
      });
      
      await fs.writeFile(
        path.join(EVIDENCE_DIR, 'data', 'org-data.json'),
        JSON.stringify(orgData, null, 2)
      );
      
      // Click organization switcher
      const orgSwitcher = await page.waitForSelector('[aria-expanded]', { timeout: 5000 });
      await orgSwitcher.click();
      await page.waitForTimeout(500);
      await screenshot(page, '02-org-dropdown', 'Organization dropdown open');
      
      // Close dropdown
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);
    });
    
    // Test 3: Navigation
    await test('Tab navigation works', async () => {
      const tabs = ['debates', 'templates', 'library', 'active', 'ollama'];
      
      for (const tab of tabs) {
        const tabButton = await page.$(`[value="${tab}"]`);
        if (tabButton) {
          await tabButton.click();
          await page.waitForTimeout(1000);
          await screenshot(page, `03-tab-${tab}`, `Tab: ${tab}`);
        }
      }
      
      // Go back to debates tab
      await page.click('[value="debates"]');
      await page.waitForTimeout(1000);
    });
    
    // Test 4: API Status
    await test('APIs are responding', async () => {
      const apiData = await page.evaluate(async () => {
        const results = {};
        
        try {
          const debateRes = await fetch('/api/debate/resources?uri=debate://debates');
          results.debates = {
            status: debateRes.status,
            data: await debateRes.json()
          };
        } catch (e) {
          results.debates = { error: e.message };
        }
        
        try {
          const llmRes = await fetch('/api/llm/providers');
          results.llm = {
            status: llmRes.status,
            data: await llmRes.json()
          };
        } catch (e) {
          results.llm = { error: e.message };
        }
        
        return results;
      });
      
      await fs.writeFile(
        path.join(EVIDENCE_DIR, 'data', 'api-responses.json'),
        JSON.stringify(apiData, null, 2)
      );
      
      if (apiData.debates?.status !== 200 || apiData.llm?.status !== 200) {
        throw new Error('API not responding correctly');
      }
    });
    
    // Test 5: Debates Display
    await test('Debates display correctly', async () => {
      // Check for debates
      const debatesInfo = await page.evaluate(() => {
        const loadingSpinner = document.querySelector('[class*="animate-spin"]');
        const noDebatesMsg = Array.from(document.querySelectorAll('h3'))
          .find(h => h.textContent?.includes('No debates yet'));
        const debateCards = document.querySelectorAll('[class*="CardTitle"]');
        
        return {
          hasLoadingSpinner: !!loadingSpinner,
          hasNoDebatesMessage: !!noDebatesMsg,
          debateCount: debateCards.length,
          debateTitles: Array.from(debateCards).map(el => el.textContent)
        };
      });
      
      await fs.writeFile(
        path.join(EVIDENCE_DIR, 'data', 'debates-info.json'),
        JSON.stringify(debatesInfo, null, 2)
      );
      
      if (debatesInfo.hasLoadingSpinner) {
        await page.waitForSelector('[class*="animate-spin"]', { 
          hidden: true, 
          timeout: 10000 
        });
      }
      
      await screenshot(page, '04-debates-loaded', 'Debates display');
    });
    
    // Test 6: Create Debate Dialog
    await test('Create Debate dialog opens', async () => {
      const newDebateBtn = await page.waitForSelector('button:has-text("New Debate")', {
        timeout: 5000
      });
      await newDebateBtn.click();
      await page.waitForTimeout(1000);
      
      const dialog = await page.waitForSelector('[role="dialog"]', { timeout: 5000 });
      await screenshot(page, '05-create-debate-dialog', 'Create debate dialog');
      
      // Check form fields
      const formInfo = await page.evaluate(() => {
        const inputs = document.querySelectorAll('input');
        const textareas = document.querySelectorAll('textarea');
        const selects = document.querySelectorAll('select');
        
        return {
          inputCount: inputs.length,
          textareaCount: textareas.length,
          selectCount: selects.length,
          hasNameInput: !!document.querySelector('input[placeholder*="debate name"]'),
          hasTopicInput: !!document.querySelector('input[placeholder*="topic"]'),
          hasAddParticipant: !!Array.from(document.querySelectorAll('button'))
            .find(btn => btn.textContent?.includes('Add Participant'))
        };
      });
      
      await fs.writeFile(
        path.join(EVIDENCE_DIR, 'data', 'create-dialog-info.json'),
        JSON.stringify(formInfo, null, 2)
      );
      
      // Add participant
      const addParticipantBtn = await page.evaluateHandle(() => {
        return Array.from(document.querySelectorAll('button'))
          .find(btn => btn.textContent?.includes('Add Participant'));
      });
      
      if (addParticipantBtn) {
        await addParticipantBtn.click();
        await page.waitForTimeout(500);
        await screenshot(page, '06-participant-added', 'Participant form added');
      }
      
      // Close dialog
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);
    });
    
    // Test 7: Test LLM Dialog
    await test('Test LLM dialog works', async () => {
      const testLLMBtn = await page.evaluateHandle(() => {
        return Array.from(document.querySelectorAll('button'))
          .find(btn => btn.textContent?.includes('Test LLM'));
      });
      
      if (testLLMBtn) {
        await testLLMBtn.click();
        await page.waitForTimeout(1000);
        
        const llmDialog = await page.$('[role="dialog"]');
        if (llmDialog) {
          await screenshot(page, '07-test-llm-dialog', 'Test LLM dialog');
          
          // Check provider dropdown
          const providersInfo = await page.evaluate(() => {
            const selects = document.querySelectorAll('select');
            const firstSelect = selects[0];
            if (firstSelect) {
              const options = Array.from(firstSelect.querySelectorAll('option'));
              return {
                hasProviderSelect: true,
                optionCount: options.length,
                providers: options.map(opt => opt.textContent)
              };
            }
            return { hasProviderSelect: false };
          });
          
          await fs.writeFile(
            path.join(EVIDENCE_DIR, 'data', 'llm-providers.json'),
            JSON.stringify(providersInfo, null, 2)
          );
          
          // Close dialog
          await page.keyboard.press('Escape');
          await page.waitForTimeout(500);
        }
      }
    });
    
    // Test 8: Responsive Design
    await test('Responsive design works', async () => {
      const viewports = [
        { name: 'desktop', width: 1920, height: 1080 },
        { name: 'tablet', width: 768, height: 1024 },
        { name: 'mobile', width: 375, height: 667 }
      ];
      
      for (const viewport of viewports) {
        await page.setViewport(viewport);
        await page.waitForTimeout(500);
        await screenshot(page, `08-viewport-${viewport.name}`, `${viewport.name} view`);
      }
      
      // Reset to desktop
      await page.setViewport({ width: 1920, height: 1080 });
    });
    
    // Test 9: Quick Actions
    await test('Quick actions are visible', async () => {
      await page.goto(BASE_URL, { waitUntil: 'networkidle2' });
      await page.waitForTimeout(2000);
      
      const quickActionsInfo = await page.evaluate(() => {
        const cards = document.querySelectorAll('[class*="Card"]');
        const quickActions = Array.from(cards).filter(card => {
          const text = card.textContent || '';
          return text.includes('Create New Debate') || 
                 text.includes('View History') ||
                 text.includes('Browse Templates') ||
                 text.includes('Manage Organizations');
        });
        
        return {
          totalCards: cards.length,
          quickActionCount: quickActions.length,
          quickActionTexts: quickActions.map(card => card.textContent?.trim())
        };
      });
      
      await fs.writeFile(
        path.join(EVIDENCE_DIR, 'data', 'quick-actions.json'),
        JSON.stringify(quickActionsInfo, null, 2)
      );
      
      await screenshot(page, '09-quick-actions', 'Quick actions display');
    });
    
    // Test 10: Final State
    await test('Final UI state check', async () => {
      const finalState = await page.evaluate(() => {
        return {
          title: document.querySelector('h1')?.textContent,
          hasOrgSwitcher: !!document.querySelector('[aria-expanded]'),
          orgText: document.querySelector('[aria-expanded] span')?.textContent,
          debateCount: document.querySelectorAll('[class*="CardTitle"]').length,
          statsCards: Array.from(document.querySelectorAll('p.text-3xl')).map(el => ({
            value: el.textContent,
            label: el.previousElementSibling?.textContent
          })),
          activeTab: document.querySelector('[data-state="active"]')?.textContent,
          hasNewDebateBtn: !!document.querySelector('button:has-text("New Debate")'),
          hasTestLLMBtn: !!document.querySelector('button:has-text("Test LLM")')
        };
      });
      
      await fs.writeFile(
        path.join(EVIDENCE_DIR, 'data', 'final-state.json'),
        JSON.stringify(finalState, null, 2)
      );
      
      await screenshot(page, '10-final-state', 'Final UI state');
    });
    
  } finally {
    // Save console logs
    await fs.writeFile(
      path.join(EVIDENCE_DIR, 'data', 'console-logs.json'),
      JSON.stringify(consoleLogs, null, 2)
    );
    
    // Save test results
    const summary = {
      ...testResults,
      totalTests: testResults.passed.length + testResults.failed.length,
      passRate: testResults.passed.length / (testResults.passed.length + testResults.failed.length) * 100
    };
    
    await fs.writeFile(
      path.join(EVIDENCE_DIR, 'test-summary.json'),
      JSON.stringify(summary, null, 2)
    );
    
    // Generate HTML report
    const htmlReport = `
<!DOCTYPE html>
<html>
<head>
  <title>E2E Test Report - ${testResults.timestamp}</title>
  <style>
    body { font-family: Arial, sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; }
    .header { background: #f0f0f0; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
    .summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin: 20px 0; }
    .summary-card { background: white; border: 1px solid #ddd; padding: 20px; border-radius: 8px; text-align: center; }
    .passed { color: #28a745; }
    .failed { color: #dc3545; }
    .test-result { margin: 10px 0; padding: 10px; border-radius: 4px; }
    .test-passed { background: #d4edda; }
    .test-failed { background: #f8d7da; }
    .screenshot { max-width: 300px; margin: 10px; display: inline-block; }
    .screenshot img { width: 100%; border: 1px solid #ddd; }
  </style>
</head>
<body>
  <div class="header">
    <h1>E2E Test Report</h1>
    <p>Generated: ${testResults.timestamp}</p>
    <p>URL: ${BASE_URL}</p>
  </div>
  
  <div class="summary">
    <div class="summary-card">
      <h2>Total Tests</h2>
      <p style="font-size: 2em;">${summary.totalTests}</p>
    </div>
    <div class="summary-card">
      <h2 class="passed">Passed</h2>
      <p style="font-size: 2em;" class="passed">${testResults.passed.length}</p>
    </div>
    <div class="summary-card">
      <h2 class="failed">Failed</h2>
      <p style="font-size: 2em;" class="failed">${testResults.failed.length}</p>
    </div>
  </div>
  
  <h2>Test Results</h2>
  ${testResults.passed.map(test => `
    <div class="test-result test-passed">✅ ${test}</div>
  `).join('')}
  ${testResults.failed.map(fail => `
    <div class="test-result test-failed">❌ ${fail.test}: ${fail.error}</div>
  `).join('')}
  
  <h2>Screenshots</h2>
  <div class="screenshots">
    ${testResults.evidence.filter(e => e.type === 'screenshot').map(screenshot => `
      <div class="screenshot">
        <img src="screenshots/${screenshot.file}" alt="${screenshot.description}" />
        <p>${screenshot.description}</p>
      </div>
    `).join('')}
  </div>
</body>
</html>
    `;
    
    await fs.writeFile(
      path.join(EVIDENCE_DIR, 'report.html'),
      htmlReport
    );
    
    console.log('\n' + '='.repeat(50));
    console.log(`Total Tests: ${summary.totalTests}`);
    console.log(`Passed: ${testResults.passed.length}`);
    console.log(`Failed: ${testResults.failed.length}`);
    console.log(`Pass Rate: ${summary.passRate.toFixed(2)}%`);
    console.log('\nEvidence saved to:', EVIDENCE_DIR);
    console.log('Open report.html to view detailed results');
    console.log('='.repeat(50));
    
    await browser.close();
  }
}

// Run the tests
runE2ETests().catch(console.error);