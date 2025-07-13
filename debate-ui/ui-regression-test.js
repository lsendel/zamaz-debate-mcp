const puppeteer = require('puppeteer');
const fs = require('fs').promises;
const path = require('path');

async function captureUIState() {
  const browser = await puppeteer.launch({ headless: false });
  const page = await browser.newPage();
  
  // Set viewport
  await page.setViewport({ width: 1920, height: 1080 });
  
  // Navigate to the app
  console.log('Navigating to http://localhost:3001...');
  await page.goto('http://localhost:3001', { waitUntil: 'networkidle2' });
  
  // Wait for initial load
  await page.waitForTimeout(3000);
  
  // Create screenshots directory
  const screenshotsDir = path.join(__dirname, 'ui-screenshots');
  await fs.mkdir(screenshotsDir, { recursive: true });
  
  // Capture initial state
  await page.screenshot({ 
    path: path.join(screenshotsDir, '1-initial-load.png'),
    fullPage: true 
  });
  console.log('✓ Captured initial load');
  
  // Check for organization state
  const orgState = await page.evaluate(() => {
    const orgData = {
      currentOrg: localStorage.getItem('currentOrganizationId'),
      organizations: localStorage.getItem('organizations'),
      hasOrgSwitcher: !!document.querySelector('[aria-expanded]'),
      orgSwitcherText: document.querySelector('[aria-expanded] span')?.textContent
    };
    
    // Check debates state
    const debatesTab = document.querySelector('[value="debates"]');
    if (debatesTab) debatesTab.click();
    
    return orgData;
  });
  
  console.log('Organization State:', orgState);
  
  // Wait for debates to load
  await page.waitForTimeout(2000);
  
  // Check debates state
  const debatesState = await page.evaluate(() => {
    const debateCards = document.querySelectorAll('[class*="Card"]');
    const loadingSpinner = document.querySelector('[class*="animate-spin"]');
    const noDebatesMessage = Array.from(document.querySelectorAll('h3')).find(h => h.textContent?.includes('No debates yet'));
    
    return {
      debateCount: debateCards.length,
      hasLoadingSpinner: !!loadingSpinner,
      hasNoDebatesMessage: !!noDebatesMessage,
      visibleDebateTitles: Array.from(document.querySelectorAll('[class*="CardTitle"]')).map(el => el.textContent)
    };
  });
  
  console.log('Debates State:', debatesState);
  
  await page.screenshot({ 
    path: path.join(screenshotsDir, '2-debates-tab.png'),
    fullPage: true 
  });
  console.log('✓ Captured debates tab');
  
  // Check console errors
  const consoleMessages = [];
  page.on('console', msg => {
    if (msg.type() === 'error') {
      consoleMessages.push(msg.text());
    }
  });
  
  // Try to open the New Debate dialog
  await page.evaluate(() => {
    const newDebateBtn = Array.from(document.querySelectorAll('button')).find(btn => btn.textContent?.includes('New Debate'));
    if (newDebateBtn) newDebateBtn.click();
  });
  
  await page.waitForTimeout(1000);
  
  await page.screenshot({ 
    path: path.join(screenshotsDir, '3-new-debate-dialog.png'),
    fullPage: true 
  });
  console.log('✓ Captured new debate dialog');
  
  // Check LLM providers in dialog
  const dialogState = await page.evaluate(() => {
    const dialog = document.querySelector('[role="dialog"]');
    const providerSelects = dialog ? dialog.querySelectorAll('select') : [];
    
    return {
      hasDialog: !!dialog,
      selectCount: providerSelects.length,
      dialogTitle: dialog?.querySelector('h2')?.textContent
    };
  });
  
  console.log('Dialog State:', dialogState);
  
  // Close dialog
  await page.keyboard.press('Escape');
  await page.waitForTimeout(500);
  
  // Test LLM button
  await page.evaluate(() => {
    const testLLMBtn = Array.from(document.querySelectorAll('button')).find(btn => btn.textContent?.includes('Test LLM'));
    if (testLLMBtn) testLLMBtn.click();
  });
  
  await page.waitForTimeout(1000);
  
  await page.screenshot({ 
    path: path.join(screenshotsDir, '4-test-llm-dialog.png'),
    fullPage: true 
  });
  console.log('✓ Captured test LLM dialog');
  
  // Generate summary report
  const report = `
# UI Regression Test Report
Generated: ${new Date().toISOString()}

## Organization State
- Current Org ID: ${orgState.currentOrg || 'None'}
- Has Org Switcher: ${orgState.hasOrgSwitcher}
- Org Switcher Text: ${orgState.orgSwitcherText || 'Not visible'}

## Debates State
- Debate Count: ${debatesState.debateCount}
- Has Loading Spinner: ${debatesState.hasLoadingSpinner}
- Has No Debates Message: ${debatesState.hasNoDebatesMessage}
- Visible Debates: ${debatesState.visibleDebateTitles.join(', ') || 'None'}

## Dialog Tests
- New Debate Dialog Opens: ${dialogState.hasDialog}
- Dialog Title: ${dialogState.dialogTitle || 'N/A'}
- Select Count: ${dialogState.selectCount}

## Console Errors
${consoleMessages.length > 0 ? consoleMessages.join('\n') : 'No errors detected'}

## Screenshots
- 1-initial-load.png - Initial page load
- 2-debates-tab.png - Debates tab view
- 3-new-debate-dialog.png - Create debate dialog
- 4-test-llm-dialog.png - Test LLM dialog
`;
  
  await fs.writeFile(path.join(screenshotsDir, 'test-report.md'), report);
  console.log('\n✅ Test complete! Report saved to ui-screenshots/test-report.md');
  
  await browser.close();
}

captureUIState().catch(console.error);