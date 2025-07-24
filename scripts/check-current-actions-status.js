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

async function takeScreenshot(page, name) {
  const screenshotPath = path.join(OUTPUT_DIR, `${name}-${Date.now()}.png`);
  await page.screenshot({ path: screenshotPath, fullPage: true });
  console.log(`📸 Screenshot: ${screenshotPath}`);
  return screenshotPath;
}

async function checkActionsStatus() {
  console.log('🔍 Checking Current GitHub Actions Status...\n');
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 300
  });
  
  const context = await browser.newContext();
  const page = await context.newPage();
  
  const report = {
    timestamp: new Date().toISOString(),
    failingWorkflows: [],
    errorMessages: [],
    recentRuns: [],
    fixesNeeded: []
  };

  try {
    // Navigate to Actions page
    console.log(`📋 Opening: ${GITHUB_REPO_URL}/actions`);
    await page.goto(`${GITHUB_REPO_URL}/actions`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);
    
    await takeScreenshot(page, 'actions-current-status');
    
    // Look for any error indicators
    const pageText = await page.textContent('body');
    
    // Check for workflow validation errors
    if (pageText.includes('Invalid workflow file')) {
      console.log('❌ Found workflow validation errors');
      const errorMatches = pageText.match(/Invalid workflow file:.*?\.yml.*?[^\n]+/g);
      if (errorMatches) {
        errorMatches.forEach(match => {
          report.errorMessages.push(match);
          console.log(`  - ${match}`);
        });
      }
    }
    
    // Look for failed workflows
    console.log('\n📊 Checking workflow statuses...');
    
    // Check for workflow items with error indicators
    const workflowItems = await page.locator('a[href*="/actions/workflows/"]').all();
    
    for (let i = 0; i < Math.min(workflowItems.length, 10); i++) {
      try {
        const item = workflowItems[i];
        const workflowName = await item.textContent();
        const href = await item.getAttribute('href');
        
        // Check parent element for status indicators
        const parent = await item.locator('../..').first();
        const parentText = await parent.textContent();
        
        const hasError = parentText.includes('failing') || 
                        await parent.locator('.octicon-x, .color-fg-danger').count() > 0;
        const hasSuccess = parentText.includes('passing') || 
                          await parent.locator('.octicon-check, .color-fg-success').count() > 0;
        
        if (hasError) {
          report.failingWorkflows.push({
            name: workflowName.trim(),
            href: href
          });
          console.log(`  ❌ ${workflowName.trim()} - FAILING`);
        } else if (hasSuccess) {
          console.log(`  ✅ ${workflowName.trim()} - PASSING`);
        } else {
          console.log(`  ❓ ${workflowName.trim()} - UNKNOWN`);
        }
      } catch (e) {
        // Skip if element is not accessible
      }
    }
    
    // Check for recent failed runs
    console.log('\n🔍 Checking recent runs...');
    const runLinks = await page.locator('a[href*="/actions/runs/"]').all();
    
    for (let i = 0; i < Math.min(runLinks.length, 5); i++) {
      try {
        const runLink = runLinks[i];
        const runHref = await runLink.getAttribute('href');
        const parent = await runLink.locator('../..').first();
        const runText = await parent.textContent();
        
        // Extract workflow name and status
        const isFailure = runText.includes('failure') || 
                         runText.includes('failed') ||
                         await parent.locator('.octicon-x').count() > 0;
        
        if (isFailure) {
          // Click on the failed run to get more details
          console.log(`\n  Checking failed run: ${runHref}`);
          await runLink.click();
          await page.waitForTimeout(2000);
          
          const runPageText = await page.textContent('body');
          
          // Look for specific error patterns
          const errorPatterns = [
            { pattern: 'Error calling workflow', type: 'workflow-call' },
            { pattern: 'is requesting.*but is only allowed', type: 'permissions' },
            { pattern: 'Path does not exist', type: 'missing-file' },
            { pattern: 'Process completed with exit code', type: 'script-error' },
            { pattern: 'syntax error', type: 'syntax' },
            { pattern: 'not found', type: 'not-found' },
            { pattern: 'Resource not accessible', type: 'permissions' }
          ];
          
          const foundErrors = [];
          errorPatterns.forEach(({ pattern, type }) => {
            if (runPageText.includes(pattern)) {
              foundErrors.push(type);
            }
          });
          
          report.recentRuns.push({
            url: `https://github.com${runHref}`,
            errors: foundErrors,
            snippet: runPageText.substring(0, 500)
          });
          
          console.log(`    Found error types: ${foundErrors.join(', ')}`);
          
          // Go back to actions page
          await page.goBack();
          await page.waitForTimeout(1500);
        }
      } catch (e) {
        console.error(`Error checking run: ${e.message}`);
      }
    }
    
    // Generate recommendations
    console.log('\n' + '='.repeat(60));
    console.log('📊 CURRENT STATUS SUMMARY');
    console.log('='.repeat(60));
    
    if (report.errorMessages.length > 0) {
      console.log('\n❌ WORKFLOW VALIDATION ERRORS:');
      report.errorMessages.forEach(msg => console.log(`  - ${msg}`));
      report.fixesNeeded.push('Fix workflow validation errors');
    }
    
    if (report.failingWorkflows.length > 0) {
      console.log('\n❌ FAILING WORKFLOWS:');
      report.failingWorkflows.forEach(wf => console.log(`  - ${wf.name}`));
    }
    
    if (report.recentRuns.some(run => run.errors.length > 0)) {
      console.log('\n⚠️  RECENT RUN ERRORS:');
      const errorTypes = new Set();
      report.recentRuns.forEach(run => {
        run.errors.forEach(error => errorTypes.add(error));
      });
      
      errorTypes.forEach(type => {
        console.log(`  - ${type} errors detected`);
        
        if (type === 'permissions') {
          report.fixesNeeded.push('Check and fix workflow permissions');
        } else if (type === 'syntax') {
          report.fixesNeeded.push('Fix syntax errors in workflows or scripts');
        } else if (type === 'missing-file') {
          report.fixesNeeded.push('Create missing files or fix file paths');
        }
      });
    }
    
    if (report.fixesNeeded.length > 0) {
      console.log('\n🔧 RECOMMENDED FIXES:');
      report.fixesNeeded.forEach((fix, i) => console.log(`  ${i + 1}. ${fix}`));
    } else {
      console.log('\n✅ No immediate fixes needed!');
    }
    
    // Save report
    const reportPath = path.join(OUTPUT_DIR, 'current-status-report.json');
    await fs.writeFile(reportPath, JSON.stringify(report, null, 2));
    console.log(`\n📄 Report saved to: ${reportPath}`);
    
    return report;
    
  } catch (error) {
    console.error('Error during check:', error);
    await takeScreenshot(page, 'error-state');
  } finally {
    await browser.close();
  }
}

async function main() {
  await ensureOutputDirectory();
  const report = await checkActionsStatus();
  
  if (report && report.fixesNeeded.length > 0) {
    console.log('\n🚨 ACTION REQUIRED: There are issues that need fixing!');
  } else {
    console.log('\n✅ GitHub Actions appear to be in good shape!');
  }
}

if (require.main === module) {
  main().catch(console.error);
}

module.exports = { checkActionsStatus };