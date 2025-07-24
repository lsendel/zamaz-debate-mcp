const { chromium } = require('@playwright/test');
const fs = require('fs').promises;
const path = require('path');

const GITHUB_REPO_URL = 'https://github.com/lsendel/zamaz-debate-mcp';
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

async function checkWorkflowErrors() {
  console.log('🔍 Checking Workflow Errors...\n');
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 500
  });
  
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    // Navigate to specific workflow run
    const workflowRunUrl = 'https://github.com/lsendel/zamaz-debate-mcp/actions/runs/16486896059';
    console.log(`📋 Navigating to workflow run: ${workflowRunUrl}`);
    await page.goto(workflowRunUrl, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    
    await takeScreenshot(page, 'workflow-error-page');
    
    // Extract error message
    const errorText = await page.textContent('body');
    console.log('\n🔴 Error found:');
    if (errorText.includes('Invalid workflow file')) {
      const errorMatch = errorText.match(/Invalid workflow file:.*?\n.*?The workflow is not valid\.[^\.]+\./);
      if (errorMatch) {
        console.log(errorMatch[0]);
      }
    }
    
    // Navigate to Actions page to see all workflows
    console.log('\n📋 Checking all workflows...');
    await page.goto(`${GITHUB_REPO_URL}/actions`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    
    await takeScreenshot(page, 'actions-page-errors');
    
    // Look for workflows with errors
    const workflowsWithErrors = [];
    
    // Check for error indicators
    const errorElements = await page.locator('.octicon-x, .color-fg-danger').all();
    console.log(`\nFound ${errorElements.length} error indicators`);
    
    // Try to find specific workflow files with errors
    const workflowLinks = await page.locator('a[href*="/workflows/"]').all();
    console.log(`\nFound ${workflowLinks.length} workflow links`);
    
    for (const link of workflowLinks) {
      try {
        const href = await link.getAttribute('href');
        const text = await link.textContent();
        if (href && text) {
          console.log(`- Workflow: ${text.trim()} (${href})`);
          
          // Check if this workflow has errors
          const parent = await link.locator('..').first();
          const hasError = await parent.locator('.octicon-x, .color-fg-danger').count() > 0;
          if (hasError) {
            workflowsWithErrors.push({ name: text.trim(), href });
            console.log('  ⚠️ Has errors!');
          }
        }
      } catch (error) {
        // Skip if element is not accessible
      }
    }
    
    // Generate report
    const report = {
      timestamp: new Date().toISOString(),
      specificError: {
        file: '.github/workflows/shell-linting.yml',
        line: 112,
        col: 13,
        error: "Job 'handle-shell-linting-failure' depends on unknown job 'shell-lint'",
        fix: "The job dependency should be 'lint-shell' not 'shell-lint'"
      },
      workflowsWithErrors,
      recommendations: [
        "Fix shell-linting.yml: Change 'needs: [shell-lint]' to 'needs: [lint-shell]'",
        "Check all workflow files for similar job dependency errors",
        "Validate all workflow files locally before pushing"
      ]
    };
    
    const reportPath = path.join(OUTPUT_DIR, 'workflow-errors-report.json');
    await fs.writeFile(reportPath, JSON.stringify(report, null, 2));
    
    console.log('\n✅ Analysis Complete!');
    console.log('\n🔧 FIX REQUIRED:');
    console.log('File: .github/workflows/shell-linting.yml');
    console.log('Line 112: Change "needs: [shell-lint]" to "needs: [lint-shell]"');
    console.log('The job name is "lint-shell" but the dependency references "shell-lint"');
    
  } catch (error) {
    console.error('Error during analysis:', error);
    await takeScreenshot(page, 'error-state');
  } finally {
    await browser.close();
  }
}

async function main() {
  await ensureOutputDirectory();
  await checkWorkflowErrors();
}

if (require.main === module) {
  main().catch(console.error);
}

module.exports = { checkWorkflowErrors };