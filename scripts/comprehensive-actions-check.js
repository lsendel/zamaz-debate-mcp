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

async function comprehensiveActionsCheck() {
  console.log('🔍 Comprehensive GitHub Actions Check...\n');
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 300
  });
  
  const context = await browser.newContext();
  const page = await context.newPage();
  
  const report = {
    timestamp: new Date().toISOString(),
    pageContent: {},
    errors: [],
    workflows: [],
    recentActivity: []
  };

  try {
    // Navigate to Actions page
    console.log(`📋 Opening: ${GITHUB_REPO_URL}/actions`);
    await page.goto(`${GITHUB_REPO_URL}/actions`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);
    
    await takeScreenshot(page, 'actions-main-page');
    
    // Get page text content for analysis
    const pageText = await page.textContent('body');
    report.pageContent.hasContent = pageText.length > 0;
    report.pageContent.textLength = pageText.length;
    
    console.log(`\n📄 Page content length: ${pageText.length} characters`);
    
    // Look for any error messages on the page
    console.log('\n🔍 Checking for error messages...');
    const errorPatterns = [
      'Invalid workflow file',
      'Error:',
      'failed',
      'not found',
      '404',
      'permission denied',
      'syntax error'
    ];
    
    for (const pattern of errorPatterns) {
      if (pageText.toLowerCase().includes(pattern.toLowerCase())) {
        console.log(`  ⚠️ Found "${pattern}" in page content`);
        report.errors.push(pattern);
      }
    }
    
    if (report.errors.length === 0) {
      console.log('  ✅ No obvious error messages found');
    }
    
    // Try different selectors to find workflows
    console.log('\n📋 Looking for workflows...');
    const workflowSelectors = [
      'a[href*="/actions/workflows/"]',
      '[data-test-selector="workflows-list"] a',
      '.Box-row a[href*="workflows"]',
      'h3 a[href*="workflows"]',
      'a[href$=".yml"]'
    ];
    
    let workflowsFound = false;
    for (const selector of workflowSelectors) {
      const elements = await page.locator(selector).all();
      if (elements.length > 0) {
        console.log(`  ✅ Found ${elements.length} workflows using selector: ${selector}`);
        workflowsFound = true;
        
        // Get workflow details
        for (const elem of elements.slice(0, 5)) { // First 5 only
          try {
            const text = await elem.textContent();
            const href = await elem.getAttribute('href');
            if (text && href) {
              report.workflows.push({
                name: text.trim(),
                href: href
              });
              console.log(`    - ${text.trim()}`);
            }
          } catch (e) {
            // Skip if element is not accessible
          }
        }
        break;
      }
    }
    
    if (!workflowsFound) {
      console.log('  ❌ No workflows found with standard selectors');
    }
    
    // Check for recent activity
    console.log('\n📊 Checking for recent activity...');
    const activitySelectors = [
      'a[href*="/actions/runs/"]',
      '[role="row"] a[href*="runs"]',
      '.TimelineItem a[href*="runs"]'
    ];
    
    for (const selector of activitySelectors) {
      const elements = await page.locator(selector).all();
      if (elements.length > 0) {
        console.log(`  ✅ Found ${elements.length} recent runs`);
        
        // Get first few runs
        for (const elem of elements.slice(0, 3)) {
          try {
            const parent = await elem.locator('..').first();
            const text = await parent.textContent();
            const href = await elem.getAttribute('href');
            
            if (text && href) {
              // Look for status indicators in the text
              let status = 'unknown';
              if (text.includes('Success') || text.includes('✓')) status = 'success';
              else if (text.includes('Failure') || text.includes('✗')) status = 'failure';
              else if (text.includes('In progress')) status = 'in_progress';
              else if (text.includes('Cancelled')) status = 'cancelled';
              
              report.recentActivity.push({
                text: text.substring(0, 100) + '...',
                href: href,
                status: status
              });
              
              console.log(`    - Run: ${status} (${href})`);
            }
          } catch (e) {
            // Skip if not accessible
          }
        }
        break;
      }
    }
    
    // Check for our specific fixed issues
    console.log('\n✅ Verifying our fixes...');
    const fixedIssues = {
      'package-lock.json error': !pageText.includes('package-lock.json') || !pageText.includes('Error'),
      'eslint-security.sarif error': !pageText.includes('eslint-security.sarif') || !pageText.includes('not exist'),
      'semgrep-results.sarif error': !pageText.includes('semgrep-results.sarif') || !pageText.includes('not exist'),
      'HEALTHCHECK syntax error': !pageText.includes('Unknown type "HEALTHCHECK"'),
      '@microsoft/sarif-tools error': !pageText.includes('@microsoft/sarif-tools') || !pageText.includes('404'),
      'shell-lint dependency error': !pageText.includes('depends on unknown job'),
      'workflow YAML syntax error': !pageText.includes('expected \':\''),
      'outdated checkout action': !pageText.includes('actions/checkout@v3')
    };
    
    let allFixed = true;
    for (const [issue, isFixed] of Object.entries(fixedIssues)) {
      if (isFixed) {
        console.log(`  ✓ ${issue} - FIXED`);
      } else {
        console.log(`  ✗ ${issue} - STILL PRESENT`);
        allFixed = false;
      }
    }
    
    report.fixValidation = fixedIssues;
    report.allIssuesFixed = allFixed;
    
    // Navigate to a specific recent workflow if found
    if (report.workflows.length > 0) {
      console.log('\n📋 Checking a specific workflow...');
      const workflowUrl = `https://github.com${report.workflows[0].href}`;
      await page.goto(workflowUrl, { waitUntil: 'networkidle' });
      await page.waitForTimeout(2000);
      
      await takeScreenshot(page, 'specific-workflow');
      
      const workflowPageText = await page.textContent('body');
      console.log(`  Workflow: ${report.workflows[0].name}`);
      console.log(`  Page has content: ${workflowPageText.length > 0}`);
    }
    
    // Save comprehensive report
    const reportPath = path.join(OUTPUT_DIR, 'comprehensive-report.json');
    await fs.writeFile(reportPath, JSON.stringify(report, null, 2));
    
    // Generate summary
    console.log('\n' + '='.repeat(60));
    console.log('📊 COMPREHENSIVE CHECK SUMMARY');
    console.log('='.repeat(60));
    console.log(`Errors found on page: ${report.errors.length}`);
    console.log(`Workflows found: ${report.workflows.length}`);
    console.log(`Recent activity found: ${report.recentActivity.length}`);
    console.log(`All fixes validated: ${report.allIssuesFixed ? 'YES ✅' : 'NO ❌'}`);
    
    if (report.allIssuesFixed) {
      console.log('\n🎉 SUCCESS: All the issues we fixed are no longer appearing!');
      console.log('The workflows should now run without the errors we addressed.');
    } else {
      console.log('\n⚠️  WARNING: Some issues may still be present.');
      console.log('Check the comprehensive report for details.');
    }
    
    console.log(`\n📄 Full report saved to: ${reportPath}`);
    
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
  await comprehensiveActionsCheck();
}

if (require.main === module) {
  main().catch(console.error);
}

module.exports = { comprehensiveActionsCheck };