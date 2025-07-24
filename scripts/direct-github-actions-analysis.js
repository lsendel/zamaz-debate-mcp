const { chromium } = require('@playwright/test');
const fs = require('fs').promises;
const path = require('path');

// Configuration
const GITHUB_REPO_URL = 'https://github.com/lsendel/zamaz-debate-mcp';
const ACTIONS_URL = `${GITHUB_REPO_URL}/actions`;
const ISSUES_URL = `${GITHUB_REPO_URL}/issues`;
const OUTPUT_DIR = path.join(__dirname, 'github-actions-analysis');

async function ensureOutputDirectory() {
  try {
    await fs.mkdir(OUTPUT_DIR, { recursive: true });
  } catch (error) {
    console.error('Error creating output directory:', error);
  }
}

async function takeScreenshot(page, name) {
  const screenshotPath = path.join(OUTPUT_DIR, `${name}-${Date.now()}.png`);
  await page.screenshot({ path: screenshotPath, fullPage: true });
  console.log(`Screenshot saved: ${screenshotPath}`);
  return screenshotPath;
}

async function analyzePage() {
  console.log('Starting Direct GitHub Actions Analysis...');
  
  const browser = await chromium.launch({ 
    headless: false, // Show browser for debugging
    slowMo: 1000 // Slow down for visibility
  });
  
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    // Navigate to GitHub Actions page
    console.log(`Navigating to ${ACTIONS_URL}`);
    await page.goto(ACTIONS_URL, { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);
    
    // Take screenshot of main actions page
    await takeScreenshot(page, 'actions-main-page');
    
    // Look for workflow runs
    console.log('Looking for workflow runs...');
    
    // Try different selectors for workflow run entries
    const workflowSelectors = [
      '[data-testid="workflow-run-list"] .Box-row',
      '.js-navigation-item',
      '.workflow-list-item',
      '[aria-label*="workflow"] a',
      '.js-workflow-run-list .Box-row'
    ];
    
    let workflowRuns = [];
    
    for (const selector of workflowSelectors) {
      try {
        await page.waitForSelector(selector, { timeout: 5000 });
        workflowRuns = await page.$$(selector);
        console.log(`Found ${workflowRuns.length} workflow runs with selector: ${selector}`);
        if (workflowRuns.length > 0) break;
      } catch (error) {
        console.log(`Selector ${selector} not found, trying next...`);
      }
    }
    
    if (workflowRuns.length === 0) {
      console.log('No workflow runs found, checking page content...');
      const pageContent = await page.content();
      
      // Look for specific workflow names in page content
      const workflowNames = [
        'Security Scanning',
        'CI/CD Pipeline', 
        'Code Quality',
        'Workflow Health Monitor',
        'Build Validation'
      ];
      
      console.log('Searching for workflow names in page content...');
      for (const workflowName of workflowNames) {
        if (pageContent.includes(workflowName)) {
          console.log(`Found workflow: ${workflowName}`);
          
          // Try to find and click the workflow
          try {
            const workflowLink = await page.locator(`text="${workflowName}"`).first();
            if (await workflowLink.isVisible()) {
              console.log(`Clicking on ${workflowName}...`);
              await workflowLink.click();
              await page.waitForTimeout(3000);
              
              await takeScreenshot(page, `workflow-${workflowName.replace(/\s+/g, '-').toLowerCase()}`);
              
              // Check for failures
              const failureElements = await page.$$('.octicon-x, .octicon-stop, [data-testid="failed"]');
              console.log(`Found ${failureElements.length} failure indicators`);
              
              // Go back to main actions page
              await page.goBack();
              await page.waitForTimeout(2000);
            }
          } catch (error) {
            console.log(`Error analyzing ${workflowName}: ${error.message}`);
          }
        }
      }
    }
    
    // Look for specific workflow failure indicators
    console.log('Looking for failure indicators...');
    const failureSelectors = [
      '.octicon-x',
      '.octicon-stop', 
      '[data-testid="failed"]',
      '.color-fg-danger',
      '.text-red'
    ];
    
    for (const selector of failureSelectors) {
      try {
        const failures = await page.$$(selector);
        console.log(`Found ${failures.length} elements with failure selector: ${selector}`);
      } catch (error) {
        // Selector not found, continue
      }
    }
    
    // Check Issues tab
    console.log('Checking Issues tab...');
    await page.goto(ISSUES_URL, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await takeScreenshot(page, 'issues-page');
    
    // Look for workflow-related issues
    const issueSelectors = [
      '.js-issue-row',
      '[data-testid="issue"]',
      '.Link--primary'
    ];
    
    let issues = [];
    for (const selector of issueSelectors) {
      try {
        issues = await page.$$(selector);
        console.log(`Found ${issues.length} issues with selector: ${selector}`);
        if (issues.length > 0) break;
      } catch (error) {
        console.log(`Issues selector ${selector} not found`);
      }
    }
    
    // Check for workflow-related keywords in issues
    const pageText = await page.textContent('body');
    const workflowKeywords = ['workflow', 'failed', 'Security Scanning', 'CI/CD', 'startup failure'];
    const foundKeywords = workflowKeywords.filter(keyword => 
      pageText.toLowerCase().includes(keyword.toLowerCase())
    );
    
    console.log('Workflow keywords found in page:', foundKeywords);
    
    // Navigate back to Actions and look for specific workflow files
    console.log('Checking individual workflow files...');
    await page.goto(`${GITHUB_REPO_URL}/actions/workflows`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await takeScreenshot(page, 'workflows-list');
    
    // Look for workflow health monitor specifically
    try {
      const healthMonitorLink = await page.locator('text="Workflow Health Monitor"').first();
      if (await healthMonitorLink.isVisible()) {
        console.log('Found Workflow Health Monitor, clicking...');
        await healthMonitorLink.click();
        await page.waitForTimeout(3000);
        await takeScreenshot(page, 'workflow-health-monitor-runs');
        
        // Check for any runs
        const runs = await page.$$('[data-testid="workflow-run-list"] .Box-row');
        console.log(`Workflow Health Monitor has ${runs.length} runs`);
      }
    } catch (error) {
      console.log(`Error checking Workflow Health Monitor: ${error.message}`);
    }
    
    console.log('Analysis complete!');
    
  } catch (error) {
    console.error('Error during analysis:', error);
    await takeScreenshot(page, 'error-state');
  } finally {
    await browser.close();
  }
}

async function main() {
  await ensureOutputDirectory();
  await analyzePage();
}

if (require.main === module) {
  main().catch(console.error);
}