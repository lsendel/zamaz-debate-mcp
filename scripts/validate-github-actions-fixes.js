const { chromium } = require('@playwright/test');
const fs = require('fs').promises;
const path = require('path');

const GITHUB_REPO_URL = 'https://github.com/lsendel/zamaz-debate-mcp';
const OUTPUT_DIR = path.join(__dirname, 'github-actions-validation');
const ERRORS_WE_FIXED = [
  'eslint-security.sarif',
  'semgrep-results.sarif',
  'HEALTHCHECK',
  '@microsoft/sarif-tools',
  'shell-lint',
  'workflow-health-monitor.yml',
  'package-lock.json',
  'actions/checkout@v3'
];

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

async function checkWorkflowRun(page, runUrl) {
  console.log(`\n📋 Checking workflow run: ${runUrl}`);
  await page.goto(runUrl, { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);

  // Check for errors
  const pageText = await page.textContent('body');
  const foundErrors = [];

  // Check for specific errors we fixed
  for (const error of ERRORS_WE_FIXED) {
    if (pageText.includes(error) && pageText.includes('Error')) {
      foundErrors.push({
        type: error,
        stillPresent: true
      });
    }
  }

  // Check for generic error indicators
  const errorIndicators = await page.locator('.color-fg-danger, .octicon-x-circle-fill').count();
  
  return {
    url: runUrl,
    hasErrors: errorIndicators > 0 || foundErrors.length > 0,
    specificErrors: foundErrors,
    errorCount: errorIndicators
  };
}

async function validateGitHubActions() {
  console.log('🔍 Validating GitHub Actions Fixes...\n');
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 300
  });
  
  const context = await browser.newContext();
  const page = await context.newPage();
  
  const report = {
    timestamp: new Date().toISOString(),
    fixedIssues: [],
    remainingIssues: [],
    newIssues: [],
    workflowStatus: {}
  };

  try {
    // Navigate to Actions page
    console.log(`📋 Navigating to: ${GITHUB_REPO_URL}/actions`);
    await page.goto(`${GITHUB_REPO_URL}/actions`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);
    
    await takeScreenshot(page, 'actions-overview');
    
    // Get all workflow links
    const workflowLinks = await page.locator('a[href*="/actions/workflows/"]').all();
    console.log(`\nFound ${workflowLinks.length} workflows\n`);
    
    // Check each workflow
    for (const link of workflowLinks) {
      try {
        const workflowName = await link.textContent();
        const workflowHref = await link.getAttribute('href');
        
        if (workflowName && workflowHref) {
          console.log(`\n🔸 Checking workflow: ${workflowName.trim()}`);
          
          // Check for status indicators near the workflow
          const parent = await link.locator('..').first();
          const hasError = await parent.locator('.octicon-x, .color-fg-danger').count() > 0;
          const hasSuccess = await parent.locator('.octicon-check, .color-fg-success').count() > 0;
          
          report.workflowStatus[workflowName.trim()] = {
            status: hasError ? 'failing' : (hasSuccess ? 'passing' : 'unknown'),
            href: workflowHref
          };
          
          // If workflow has errors, check recent runs
          if (hasError) {
            console.log(`  ⚠️ Workflow has errors, checking recent runs...`);
            
            // Navigate to workflow page
            await page.goto(`https://github.com${workflowHref}`, { waitUntil: 'networkidle' });
            await page.waitForTimeout(2000);
            
            // Get recent run links (up to 3)
            const runLinks = await page.locator('a[href*="/actions/runs/"]').all();
            const recentRuns = runLinks.slice(0, 3);
            
            for (const runLink of recentRuns) {
              const runHref = await runLink.getAttribute('href');
              if (runHref) {
                const runResult = await checkWorkflowRun(page, `https://github.com${runHref}`);
                
                if (runResult.specificErrors.length > 0) {
                  report.remainingIssues.push({
                    workflow: workflowName.trim(),
                    run: runResult.url,
                    errors: runResult.specificErrors
                  });
                }
              }
            }
            
            // Go back to main actions page
            await page.goto(`${GITHUB_REPO_URL}/actions`, { waitUntil: 'networkidle' });
            await page.waitForTimeout(2000);
          }
        }
      } catch (error) {
        console.error(`Error checking workflow: ${error.message}`);
      }
    }
    
    // Check for any workflow with invalid file errors
    console.log('\n📋 Checking for workflow validation errors...');
    const pageText = await page.textContent('body');
    
    if (pageText.includes('Invalid workflow file')) {
      console.log('  ❌ Found workflow validation errors!');
      
      // Try to extract specific error details
      const errorMatches = pageText.match(/Invalid workflow file:.*?\.yml.*?line \d+/g);
      if (errorMatches) {
        for (const match of errorMatches) {
          report.remainingIssues.push({
            type: 'workflow-validation',
            error: match
          });
        }
      }
    } else {
      console.log('  ✅ No workflow validation errors found');
    }
    
    // Generate summary
    console.log('\n' + '='.repeat(60));
    console.log('📊 VALIDATION SUMMARY');
    console.log('='.repeat(60));
    
    // Check which issues are fixed
    const fixedIssueChecks = {
      'package-lock.json': !pageText.includes('package-lock.json') || !pageText.includes('Error'),
      'eslint-security.sarif': !pageText.includes('eslint-security.sarif') || !pageText.includes('not exist'),
      'semgrep-results.sarif': !pageText.includes('semgrep-results.sarif') || !pageText.includes('not exist'),
      'HEALTHCHECK': !pageText.includes('Unknown type "HEALTHCHECK"'),
      '@microsoft/sarif-tools': !pageText.includes('@microsoft/sarif-tools') || !pageText.includes('404'),
      'shell-lint dependency': !pageText.includes('depends on unknown job \'shell-lint\''),
      'workflow YAML syntax': !pageText.includes('expected \':\''),
      'checkout@v3': !pageText.includes('actions/checkout@v3')
    };
    
    console.log('\n✅ FIXED ISSUES:');
    for (const [issue, isFixed] of Object.entries(fixedIssueChecks)) {
      if (isFixed) {
        console.log(`  ✓ ${issue}`);
        report.fixedIssues.push(issue);
      }
    }
    
    console.log('\n❌ REMAINING ISSUES:');
    if (report.remainingIssues.length === 0) {
      console.log('  None detected! 🎉');
    } else {
      report.remainingIssues.forEach(issue => {
        console.log(`  - ${JSON.stringify(issue)}`);
      });
    }
    
    console.log('\n📈 WORKFLOW STATUS:');
    let passingCount = 0;
    let failingCount = 0;
    
    for (const [name, status] of Object.entries(report.workflowStatus)) {
      const icon = status.status === 'passing' ? '✅' : status.status === 'failing' ? '❌' : '❓';
      console.log(`  ${icon} ${name}: ${status.status}`);
      
      if (status.status === 'passing') passingCount++;
      if (status.status === 'failing') failingCount++;
    }
    
    console.log(`\nTotal: ${passingCount} passing, ${failingCount} failing`);
    
    // Save report
    const reportPath = path.join(OUTPUT_DIR, 'validation-report.json');
    await fs.writeFile(reportPath, JSON.stringify(report, null, 2));
    console.log(`\n📄 Report saved to: ${reportPath}`);
    
    // Final screenshot
    await takeScreenshot(page, 'final-state');
    
    return report;
    
  } catch (error) {
    console.error('Error during validation:', error);
    await takeScreenshot(page, 'error-state');
  } finally {
    await browser.close();
  }
}

async function main() {
  await ensureOutputDirectory();
  const report = await validateGitHubActions();
  
  // Determine if we need to iterate
  if (report && report.remainingIssues.length > 0) {
    console.log('\n🔧 NEXT STEPS:');
    console.log('There are still issues that need to be fixed.');
    console.log('Review the remaining issues and create fixes for each one.');
  } else {
    console.log('\n✅ ALL ISSUES APPEAR TO BE FIXED!');
    console.log('The workflows should now run successfully.');
  }
}

if (require.main === module) {
  main().catch(console.error);
}

module.exports = { validateGitHubActions };