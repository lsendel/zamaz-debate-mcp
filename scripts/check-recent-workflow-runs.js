const { chromium } = require('@playwright/test');
const fs = require('fs').promises;
const path = require('path');

const GITHUB_REPO_URL = 'https://github.com/lsendel/zamaz-debate-mcp';
const OUTPUT_DIR = path.join(__dirname, 'github-actions-validation');

async function ensureOutputDirectory() {
  try {
    await fs.mkdir(OUTPUT_DIR, { recursive: true });
  } catch (error) {
    console.error('Error creating output directory:', error);
  }
}

async function checkRecentWorkflowRuns() {
  console.log('🔍 Checking Recent Workflow Runs...\n');
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 200
  });
  
  const context = await browser.newContext();
  const page = await context.newPage();
  
  const results = {
    timestamp: new Date().toISOString(),
    recentRuns: [],
    summary: {
      total: 0,
      success: 0,
      failure: 0,
      inProgress: 0,
      cancelled: 0
    }
  };

  try {
    // Navigate to Actions page
    console.log(`📋 Navigating to: ${GITHUB_REPO_URL}/actions`);
    await page.goto(`${GITHUB_REPO_URL}/actions`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);
    
    // Look for workflow run items
    const runItems = await page.locator('div[data-test-selector="workflows-list"] a[href*="/actions/runs/"]').all();
    console.log(`Found ${runItems.length} recent workflow runs\n`);
    
    // Limit to first 10 runs for analysis
    const runsToCheck = Math.min(runItems.length, 10);
    
    for (let i = 0; i < runsToCheck; i++) {
      try {
        const runItem = runItems[i];
        const runHref = await runItem.getAttribute('href');
        
        // Get run details from the list view
        const parent = await runItem.locator('../..').first();
        const runText = await parent.textContent();
        
        // Extract workflow name
        const workflowName = runText.match(/([^:]+):/)?.[1]?.trim() || 'Unknown';
        
        // Check status indicators
        const hasSuccess = runText.includes('Success') || await parent.locator('.octicon-check').count() > 0;
        const hasFailure = runText.includes('Failure') || await parent.locator('.octicon-x').count() > 0;
        const isInProgress = runText.includes('In progress') || await parent.locator('.octicon-dot-fill').count() > 0;
        const isCancelled = runText.includes('Cancelled');
        
        let status = 'unknown';
        if (hasSuccess) status = 'success';
        else if (hasFailure) status = 'failure';
        else if (isInProgress) status = 'in_progress';
        else if (isCancelled) status = 'cancelled';
        
        const runInfo = {
          workflow: workflowName,
          url: `https://github.com${runHref}`,
          status: status,
          errors: []
        };
        
        console.log(`${i + 1}. ${workflowName}: ${status}`);
        
        // If failed, check for specific errors
        if (status === 'failure') {
          console.log(`   Checking failure details...`);
          await page.goto(runInfo.url, { waitUntil: 'networkidle' });
          await page.waitForTimeout(2000);
          
          const pageText = await page.textContent('body');
          
          // Check for our specific fixed errors
          if (pageText.includes('eslint-security.sarif')) {
            runInfo.errors.push('ESLint SARIF issue (should be fixed)');
          }
          if (pageText.includes('semgrep-results.sarif')) {
            runInfo.errors.push('Semgrep SARIF issue (should be fixed)');
          }
          if (pageText.includes('HEALTHCHECK')) {
            runInfo.errors.push('Dockerfile HEALTHCHECK issue (should be fixed)');
          }
          if (pageText.includes('@microsoft/sarif-tools')) {
            runInfo.errors.push('Missing npm package issue (should be fixed)');
          }
          if (pageText.includes('Invalid workflow file')) {
            runInfo.errors.push('Workflow validation error');
          }
          
          // Go back to actions page
          await page.goto(`${GITHUB_REPO_URL}/actions`, { waitUntil: 'networkidle' });
          await page.waitForTimeout(1500);
        }
        
        results.recentRuns.push(runInfo);
        results.summary[status]++;
        results.summary.total++;
        
      } catch (error) {
        console.error(`Error checking run ${i + 1}: ${error.message}`);
      }
    }
    
    // Generate summary
    console.log('\n' + '='.repeat(60));
    console.log('📊 RECENT RUNS SUMMARY');
    console.log('='.repeat(60));
    console.log(`Total runs analyzed: ${results.summary.total}`);
    console.log(`✅ Success: ${results.summary.success}`);
    console.log(`❌ Failure: ${results.summary.failure}`);
    console.log(`🔄 In Progress: ${results.summary.in_progress}`);
    console.log(`🚫 Cancelled: ${results.summary.cancelled}`);
    
    if (results.summary.failure > 0) {
      console.log('\n❌ FAILED RUNS WITH ERRORS:');
      results.recentRuns.filter(r => r.status === 'failure' && r.errors.length > 0).forEach(run => {
        console.log(`\n${run.workflow}:`);
        run.errors.forEach(error => console.log(`  - ${error}`));
      });
    }
    
    // Save results
    const reportPath = path.join(OUTPUT_DIR, 'recent-runs-report.json');
    await fs.writeFile(reportPath, JSON.stringify(results, null, 2));
    console.log(`\n📄 Report saved to: ${reportPath}`);
    
    // Take final screenshot
    const screenshotPath = path.join(OUTPUT_DIR, `recent-runs-${Date.now()}.png`);
    await page.screenshot({ path: screenshotPath, fullPage: true });
    console.log(`Screenshot saved: ${screenshotPath}`);
    
    return results;
    
  } catch (error) {
    console.error('Error during validation:', error);
  } finally {
    await browser.close();
  }
}

async function main() {
  await ensureOutputDirectory();
  const results = await checkRecentWorkflowRuns();
  
  if (results && results.summary.failure > 0) {
    const hasOurErrors = results.recentRuns.some(run => 
      run.errors.some(e => e.includes('should be fixed'))
    );
    
    if (hasOurErrors) {
      console.log('\n⚠️  WARNING: Some of our fixes may not have taken effect yet.');
      console.log('The failed runs might be from before the fixes were applied.');
      console.log('Consider triggering new workflow runs to validate the fixes.');
    }
  }
}

if (require.main === module) {
  main().catch(console.error);
}

module.exports = { checkRecentWorkflowRuns };