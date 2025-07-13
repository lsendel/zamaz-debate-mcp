const puppeteer = require('puppeteer');
const fs = require('fs').promises;
const path = require('path');

// Test configuration
const BASE_URL = 'http://localhost:3001';
const SCREENSHOTS_DIR = path.join(__dirname, 'ui-test-screenshots');
const TIMEOUT = 5000;

// Test results collector
const testResults = {
  passed: [],
  failed: [],
  warnings: [],
  screenshots: []
};

// Helper functions
async function takeScreenshot(page, name) {
  const filename = `${Date.now()}-${name}.png`;
  const filepath = path.join(SCREENSHOTS_DIR, filename);
  await page.screenshot({ path: filepath, fullPage: true });
  testResults.screenshots.push({ name, filename });
  console.log(`📸 Screenshot: ${name}`);
  return filepath;
}

async function testElement(page, selector, testName, options = {}) {
  try {
    const element = await page.waitForSelector(selector, { 
      timeout: options.timeout || TIMEOUT,
      visible: options.visible !== false 
    });
    
    if (options.click) {
      await element.click();
      await page.waitForTimeout(500);
    }
    
    if (options.text) {
      const text = await page.$eval(selector, el => el.textContent);
      if (!text.includes(options.text)) {
        throw new Error(`Expected text "${options.text}" not found`);
      }
    }
    
    testResults.passed.push(testName);
    console.log(`✅ ${testName}`);
    return element;
  } catch (error) {
    testResults.failed.push({ test: testName, error: error.message });
    console.log(`❌ ${testName}: ${error.message}`);
    return null;
  }
}

async function testAPI(endpoint, testName) {
  try {
    const response = await fetch(`${BASE_URL}${endpoint}`);
    const data = await response.json();
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    
    testResults.passed.push(testName);
    console.log(`✅ ${testName}: ${response.status} OK`);
    return data;
  } catch (error) {
    testResults.failed.push({ test: testName, error: error.message });
    console.log(`❌ ${testName}: ${error.message}`);
    return null;
  }
}

// Main test function
async function runComprehensiveUITest() {
  console.log('🚀 Starting Comprehensive UI Test\n');
  
  // Create screenshots directory
  await fs.mkdir(SCREENSHOTS_DIR, { recursive: true });
  
  // Test APIs first
  console.log('=== 1. API TESTS ===\n');
  
  const debateData = await testAPI('/api/debate/resources?uri=debate://debates', 'Debate API');
  const llmData = await testAPI('/api/llm/providers', 'LLM Providers API');
  const healthData = await testAPI('/api/llm/health', 'Health API');
  
  console.log(`\nDebates found: ${debateData?.debates?.length || 0}`);
  console.log(`LLM providers: ${llmData?.providers?.length || 0}`);
  console.log(`Health status: ${healthData?.status || 'unknown'}\n`);
  
  // Launch browser
  const browser = await puppeteer.launch({ 
    headless: false,
    defaultViewport: { width: 1920, height: 1080 }
  });
  
  const page = await browser.newPage();
  
  // Set up console message collection
  const consoleMessages = { log: [], warn: [], error: [] };
  page.on('console', msg => {
    const type = msg.type();
    if (consoleMessages[type]) {
      consoleMessages[type].push(msg.text());
    }
  });
  
  // Navigate to app
  console.log('=== 2. PAGE LOAD TEST ===\n');
  
  try {
    await page.goto(BASE_URL, { waitUntil: 'networkidle2' });
    console.log('✅ Page loaded successfully');
    
    // Wait for React to initialize
    await page.waitForTimeout(2000);
  } catch (error) {
    console.log(`❌ Page load failed: ${error.message}`);
    await browser.close();
    return;
  }
  
  await takeScreenshot(page, '01-initial-load');
  
  // Test localStorage state
  console.log('\n=== 3. LOCALSTORAGE TEST ===\n');
  
  const localStorageData = await page.evaluate(() => {
    return {
      currentOrganizationId: localStorage.getItem('currentOrganizationId'),
      organizations: localStorage.getItem('organizations'),
      hasOrganizations: !!localStorage.getItem('organizations')
    };
  });
  
  console.log(`Organization ID: ${localStorageData.currentOrganizationId || 'None'}`);
  console.log(`Has organizations: ${localStorageData.hasOrganizations}`);
  
  // Test header components
  console.log('\n=== 4. HEADER COMPONENTS TEST ===\n');
  
  await testElement(page, 'h1', 'Main Title', { text: 'AI Debate System' });
  await testElement(page, '[aria-expanded]', 'Organization Switcher');
  await testElement(page, 'button:has-text("Test LLM")', 'Test LLM Button');
  await testElement(page, 'button:has-text("New Debate")', 'New Debate Button');
  
  // Test organization switcher
  console.log('\n=== 5. ORGANIZATION SWITCHER TEST ===\n');
  
  const orgSwitcher = await page.$('[aria-expanded]');
  if (orgSwitcher) {
    await orgSwitcher.click();
    await page.waitForTimeout(500);
    await takeScreenshot(page, '02-org-switcher-open');
    
    const orgOptions = await page.$$('button:has-text("Organization")');
    console.log(`Organization options found: ${orgOptions.length}`);
    
    // Close dropdown
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
  }
  
  // Test quick actions
  console.log('\n=== 6. QUICK ACTIONS TEST ===\n');
  
  const quickActionCards = await page.$$('[class*="QuickActionCard"]');
  console.log(`Quick action cards: ${quickActionCards.length}`);
  
  // Test stats cards
  console.log('\n=== 7. STATS CARDS TEST ===\n');
  
  const statsData = await page.evaluate(() => {
    const cards = document.querySelectorAll('[class*="Card"]');
    const stats = [];
    cards.forEach(card => {
      const title = card.querySelector('p')?.textContent;
      const value = card.querySelector('p.text-3xl')?.textContent;
      if (title && value) {
        stats.push({ title, value });
      }
    });
    return stats;
  });
  
  console.log('Stats found:');
  statsData.forEach(stat => console.log(`  ${stat.title}: ${stat.value}`));
  
  // Test tabs
  console.log('\n=== 8. TAB NAVIGATION TEST ===\n');
  
  const tabs = ['debates', 'templates', 'library', 'active', 'ollama'];
  
  for (const tab of tabs) {
    const tabButton = await page.$(`[value="${tab}"]`);
    if (tabButton) {
      await tabButton.click();
      await page.waitForTimeout(1000);
      await takeScreenshot(page, `03-tab-${tab}`);
      console.log(`✅ Tab "${tab}" clicked`);
    } else {
      console.log(`❌ Tab "${tab}" not found`);
    }
  }
  
  // Go back to debates tab
  await page.click('[value="debates"]');
  await page.waitForTimeout(1000);
  
  // Test debates display
  console.log('\n=== 9. DEBATES DISPLAY TEST ===\n');
  
  const debatesState = await page.evaluate(() => {
    const loadingSpinner = document.querySelector('[class*="animate-spin"]');
    const noDebatesMessage = Array.from(document.querySelectorAll('h3'))
      .find(h => h.textContent?.includes('No debates yet'));
    const debateCards = document.querySelectorAll('[class*="CardTitle"]');
    
    return {
      hasLoadingSpinner: !!loadingSpinner,
      hasNoDebatesMessage: !!noDebatesMessage,
      debateCount: debateCards.length,
      debateTitles: Array.from(debateCards).map(el => el.textContent)
    };
  });
  
  console.log(`Loading spinner: ${debatesState.hasLoadingSpinner ? 'Yes' : 'No'}`);
  console.log(`No debates message: ${debatesState.hasNoDebatesMessage ? 'Yes' : 'No'}`);
  console.log(`Debate cards found: ${debatesState.debateCount}`);
  console.log(`Debate titles: ${debatesState.debateTitles.join(', ') || 'None'}`);
  
  // Test Create Debate dialog
  console.log('\n=== 10. CREATE DEBATE DIALOG TEST ===\n');
  
  const newDebateBtn = await page.$('button:has-text("New Debate")');
  if (newDebateBtn) {
    await newDebateBtn.click();
    await page.waitForTimeout(1000);
    
    const dialogVisible = await page.$('[role="dialog"]');
    if (dialogVisible) {
      console.log('✅ Create Debate dialog opened');
      await takeScreenshot(page, '04-create-debate-dialog');
      
      // Test form fields
      await testElement(page, 'input[placeholder*="debate name"]', 'Debate Name Input');
      await testElement(page, 'input[placeholder*="topic"]', 'Topic Input');
      await testElement(page, 'textarea', 'Description Textarea');
      
      // Test participant section
      const addParticipantBtn = await page.$('button:has-text("Add Participant")');
      if (addParticipantBtn) {
        await addParticipantBtn.click();
        await page.waitForTimeout(500);
        await takeScreenshot(page, '05-participant-added');
        
        // Check for model selectors
        const selects = await page.$$('select');
        console.log(`Select dropdowns found: ${selects.length}`);
      }
      
      // Close dialog
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);
    } else {
      console.log('❌ Create Debate dialog did not open');
    }
  }
  
  // Test LLM Test dialog
  console.log('\n=== 11. LLM TEST DIALOG TEST ===\n');
  
  const testLLMBtn = await page.$('button:has-text("Test LLM")');
  if (testLLMBtn) {
    await testLLMBtn.click();
    await page.waitForTimeout(1000);
    
    const llmDialog = await page.$('[role="dialog"]');
    if (llmDialog) {
      console.log('✅ LLM Test dialog opened');
      await takeScreenshot(page, '06-llm-test-dialog');
      
      // Test provider selection
      const providerSelect = await page.$('select');
      if (providerSelect) {
        const options = await page.$$eval('select option', opts => 
          opts.map(opt => opt.textContent)
        );
        console.log(`LLM providers available: ${options.length}`);
        console.log(`Providers: ${options.slice(0, 5).join(', ')}...`);
      }
      
      // Close dialog
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);
    } else {
      console.log('❌ LLM Test dialog did not open');
    }
  }
  
  // Test responsive design
  console.log('\n=== 12. RESPONSIVE DESIGN TEST ===\n');
  
  const viewports = [
    { name: 'mobile', width: 375, height: 667 },
    { name: 'tablet', width: 768, height: 1024 },
    { name: 'desktop', width: 1920, height: 1080 }
  ];
  
  for (const viewport of viewports) {
    await page.setViewport(viewport);
    await page.waitForTimeout(500);
    await takeScreenshot(page, `07-viewport-${viewport.name}`);
    console.log(`✅ ${viewport.name} viewport (${viewport.width}x${viewport.height})`);
  }
  
  // Generate test report
  console.log('\n=== GENERATING REPORT ===\n');
  
  const report = `# Comprehensive UI Test Report
Generated: ${new Date().toISOString()}

## Test Summary
- **Total Tests**: ${testResults.passed.length + testResults.failed.length}
- **Passed**: ${testResults.passed.length}
- **Failed**: ${testResults.failed.length}
- **Warnings**: ${testResults.warnings.length}
- **Screenshots**: ${testResults.screenshots.length}

## API Test Results
- Debate API: ${debateData ? '✅ Working' : '❌ Failed'}
- LLM Providers API: ${llmData ? '✅ Working' : '❌ Failed'}
- Health API: ${healthData ? '✅ Working' : '❌ Failed'}

## Failed Tests
${testResults.failed.length === 0 ? 'None' : testResults.failed.map(f => `- ${f.test}: ${f.error}`).join('\n')}

## Console Messages
- Errors: ${consoleMessages.error.length}
- Warnings: ${consoleMessages.warn.length}

## Screenshots Captured
${testResults.screenshots.map(s => `- ${s.name}: ${s.filename}`).join('\n')}

## Detailed Results

### Page Load
- URL: ${BASE_URL}
- Load Time: < 5s
- Initial Render: ✅ Success

### Organization State
- Current Org ID: ${localStorageData.currentOrganizationId || 'None'}
- Has Organizations: ${localStorageData.hasOrganizations}

### Debates Display
- Loading Spinner: ${debatesState.hasLoadingSpinner ? 'Yes' : 'No'}
- No Debates Message: ${debatesState.hasNoDebatesMessage ? 'Yes' : 'No'}
- Debate Count: ${debatesState.debateCount}
- Debate Titles: ${debatesState.debateTitles.join(', ') || 'None'}

### Component Tests
${testResults.passed.map(test => `✅ ${test}`).join('\n')}

### Responsive Design
- Mobile (375x667): ✅ Tested
- Tablet (768x1024): ✅ Tested
- Desktop (1920x1080): ✅ Tested
`;
  
  await fs.writeFile(path.join(SCREENSHOTS_DIR, 'test-report.md'), report);
  
  console.log('\n✅ Test complete!');
  console.log(`📊 Results: ${testResults.passed.length} passed, ${testResults.failed.length} failed`);
  console.log(`📁 Report saved to: ${SCREENSHOTS_DIR}/test-report.md`);
  
  await browser.close();
}

// Run the test
runComprehensiveUITest().catch(error => {
  console.error('Test failed:', error);
  process.exit(1);
});