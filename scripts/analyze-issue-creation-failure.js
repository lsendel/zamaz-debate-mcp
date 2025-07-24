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

async function analyzeIssueCreationFailure() {
  console.log('🔍 Analyzing Issue Creation Failure...\n');
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 500
  });
  
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    // 1. Check Labels Page
    console.log('📌 Step 1: Checking repository labels...');
    const labelsUrl = `${GITHUB_REPO_URL}/labels`;
    await page.goto(labelsUrl, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    
    await takeScreenshot(page, 'labels-page');
    
    // Search for specific labels
    const labelNames = ['security', 'workflow-failure', 'automated', 'ci-cd', 'critical'];
    const existingLabels = [];
    const missingLabels = [];
    
    for (const labelName of labelNames) {
      try {
        const labelExists = await page.locator(`text="${labelName}"`).first().isVisible({ timeout: 1000 });
        if (labelExists) {
          existingLabels.push(labelName);
          console.log(`✅ Label exists: ${labelName}`);
        } else {
          missingLabels.push(labelName);
          console.log(`❌ Label NOT found: ${labelName}`);
        }
      } catch (error) {
        missingLabels.push(labelName);
        console.log(`❌ Label NOT found: ${labelName}`);
      }
    }
    
    console.log('\n📊 Label Summary:');
    console.log(`Existing labels: ${existingLabels.join(', ') || 'none'}`);
    console.log(`Missing labels: ${missingLabels.join(', ') || 'none'}`);
    
    // 2. Check Recent Workflow Runs
    console.log('\n📋 Step 2: Checking recent workflow runs...');
    const actionsUrl = `${GITHUB_REPO_URL}/actions`;
    await page.goto(actionsUrl, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    
    await takeScreenshot(page, 'actions-page-latest');
    
    // Look for Workflow Health Monitor runs
    console.log('\n🔧 Step 3: Looking for Workflow Health Monitor runs...');
    try {
      const healthMonitorLink = await page.locator('text="Workflow Health Monitor Debug"').first();
      if (await healthMonitorLink.isVisible()) {
        console.log('✅ Found Workflow Health Monitor Debug');
        await healthMonitorLink.click();
        await page.waitForTimeout(3000);
        
        await takeScreenshot(page, 'health-monitor-runs');
        
        // Click on the most recent run
        const firstRun = await page.locator('[data-testid="workflow-run-list"] .Box-row').first();
        if (await firstRun.isVisible()) {
          await firstRun.click();
          await page.waitForTimeout(3000);
          
          await takeScreenshot(page, 'health-monitor-run-details');
          
          // Look for error messages
          const errorElements = await page.locator('.color-fg-danger, .text-red, [aria-label*="failed"]').all();
          console.log(`Found ${errorElements.length} error indicators`);
        }
      }
    } catch (error) {
      console.log('Could not find Workflow Health Monitor Debug');
    }
    
    // 3. Check Issues Page
    console.log('\n📝 Step 4: Checking Issues page...');
    const issuesUrl = `${GITHUB_REPO_URL}/issues`;
    await page.goto(issuesUrl, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    
    await takeScreenshot(page, 'issues-page-check');
    
    // Count open issues
    const issueCount = await page.locator('[data-testid="issue"], .js-issue-row').count();
    console.log(`Total open issues: ${issueCount}`);
    
    // 4. Check Settings for Labels (if accessible)
    console.log('\n⚙️ Step 5: Attempting to check repository settings...');
    const settingsUrl = `${GITHUB_REPO_URL}/settings`;
    try {
      await page.goto(settingsUrl, { waitUntil: 'networkidle', timeout: 10000 });
      await page.waitForTimeout(2000);
      
      const hasAccess = await page.url().includes('/settings');
      if (hasAccess) {
        console.log('✅ Has access to settings');
        await takeScreenshot(page, 'settings-page');
      } else {
        console.log('❌ No access to repository settings (expected for non-owners)');
      }
    } catch (error) {
      console.log('❌ Cannot access repository settings (expected for non-owners)');
    }
    
    // Generate Analysis Report
    console.log('\n📄 Generating Analysis Report...');
    
    const report = {
      timestamp: new Date().toISOString(),
      findings: {
        missingLabels,
        existingLabels,
        issueCount,
        rootCause: 'The workflow is trying to create issues with labels that do not exist in the repository',
        specificError: "could not add label: 'security' not found",
        solution: 'Either create the missing labels or update the workflow to use only existing labels'
      },
      recommendations: [
        'Create missing labels: ' + missingLabels.join(', '),
        'Or update workflow to remove non-existent labels from issue creation',
        'Use basic labels like "bug" or "help wanted" that likely exist',
        'Consider using workflow_dispatch to manually test with different labels'
      ]
    };
    
    const reportPath = path.join(OUTPUT_DIR, 'issue-creation-failure-analysis.json');
    await fs.writeFile(reportPath, JSON.stringify(report, null, 2));
    
    console.log('\n✅ Analysis Complete!');
    console.log('\n🔍 ROOT CAUSE IDENTIFIED:');
    console.log('The workflow is failing because it\'s trying to use labels that don\'t exist in the repository.');
    console.log(`Missing labels: ${missingLabels.join(', ')}`);
    console.log('\n💡 SOLUTION:');
    console.log('1. Create the missing labels in the repository, OR');
    console.log('2. Update the workflow to use only existing labels, OR');
    console.log('3. Remove label assignment from the issue creation command');
    
  } catch (error) {
    console.error('Error during analysis:', error);
    await takeScreenshot(page, 'error-state');
  } finally {
    await browser.close();
  }
}

async function main() {
  await ensureOutputDirectory();
  await analyzeIssueCreationFailure();
}

if (require.main === module) {
  main().catch(console.error);
}

module.exports = { analyzeIssueCreationFailure };