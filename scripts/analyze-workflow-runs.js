const { chromium } = require('@playwright/test');
const fs = require('fs').promises;
const path = require('path');

const GITHUB_REPO_URL = 'https://github.com/lsendel/zamaz-debate-mcp';
const OUTPUT_DIR = path.join(__dirname, 'workflow-run-analysis');

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
  console.log(`📸 Screenshot saved: ${screenshotPath}`);
  return screenshotPath;
}

async function analyzeWorkflowRun(page, runId) {
  const runUrl = `${GITHUB_REPO_URL}/actions/runs/${runId}`;
  console.log(`\n🔍 Analyzing workflow run: ${runId}`);
  console.log(`   URL: ${runUrl}`);
  
  const runAnalysis = {
    runId,
    url: runUrl,
    timestamp: new Date().toISOString(),
    workflowName: '',
    status: '',
    errors: [],
    failedJobs: [],
    logExcerpts: [],
    rootCauses: [],
    suggestedFixes: []
  };
  
  try {
    await page.goto(runUrl, { waitUntil: 'networkidle', timeout: 60000 });
    await page.waitForTimeout(3000);
    
    // Get workflow name and status
    const headerText = await page.textContent('h1, .PageHeader-title');
    if (headerText) {
      runAnalysis.workflowName = headerText.trim();
      console.log(`   Workflow: ${runAnalysis.workflowName}`);
    }
    
    // Check overall status
    const pageText = await page.textContent('body');
    if (pageText.includes('failure') || pageText.includes('failed')) {
      runAnalysis.status = 'failed';
      console.log(`   Status: ❌ FAILED`);
    } else if (pageText.includes('success') || pageText.includes('completed successfully')) {
      runAnalysis.status = 'success';
      console.log(`   Status: ✅ SUCCESS`);
    } else if (pageText.includes('cancelled')) {
      runAnalysis.status = 'cancelled';
      console.log(`   Status: ⏹️ CANCELLED`);
    } else {
      runAnalysis.status = 'unknown';
      console.log(`   Status: ❓ UNKNOWN`);
    }
    
    // Look for error patterns in the page
    const errorPatterns = [
      { pattern: /Error calling workflow.*?requesting '([^']+)'.*?but is only allowed '([^']+)'/g, type: 'permissions', extractor: (match) => `Permission error: requesting '${match[1]}' but only allowed '${match[2]}'` },
      { pattern: /npm error.*?404 Not Found.*?GET.*?\/([^\/\s]+)/g, type: 'npm-package', extractor: (match) => `NPM package not found: ${match[1]}` },
      { pattern: /Unrecognized function: '([^']+)'/g, type: 'syntax', extractor: (match) => `Unrecognized function: ${match[1]}` },
      { pattern: /Invalid workflow file:.*?\.github\/workflows\/([^#]+)#L(\d+)/g, type: 'workflow-syntax', extractor: (match) => `Invalid workflow file: ${match[1]} at line ${match[2]}` },
      { pattern: /Process completed with exit code (\d+)/g, type: 'exit-code', extractor: (match) => `Process exited with code ${match[1]}` },
      { pattern: /Error: ([^\.]+\.yml): No such file or directory/g, type: 'missing-file', extractor: (match) => `Missing file: ${match[1]}` },
      { pattern: /error.*?command not found: ([^\s]+)/g, type: 'missing-command', extractor: (match) => `Command not found: ${match[1]}` },
      { pattern: /Cannot find module '([^']+)'/g, type: 'missing-module', extractor: (match) => `Missing module: ${match[1]}` },
      { pattern: /SyntaxError: Unexpected token/g, type: 'syntax-error', extractor: () => 'JavaScript syntax error' },
      { pattern: /here-document at line (\d+) delimited by end-of-file/g, type: 'bash-syntax', extractor: (match) => `Bash heredoc error at line ${match[1]}` }
    ];
    
    errorPatterns.forEach(({ pattern, type, extractor }) => {
      let match;
      while ((match = pattern.exec(pageText)) !== null) {
        const errorDetail = extractor(match);
        runAnalysis.errors.push({ type, detail: errorDetail });
        console.log(`   Found error: ${type} - ${errorDetail}`);
      }
    });
    
    // Find failed jobs
    const jobElements = await page.locator('[data-testid="job-step-header"], .job-step-header, details summary').all();
    
    for (const jobElement of jobElements) {
      try {
        const jobText = await jobElement.textContent();
        const hasError = await jobElement.locator('.octicon-x, .color-fg-danger').count() > 0 ||
                        jobText.includes('failure') ||
                        jobText.includes('failed');
        
        if (hasError) {
          runAnalysis.failedJobs.push(jobText.trim());
          console.log(`   Failed job: ${jobText.trim()}`);
          
          // Try to expand the job to get more details
          try {
            await jobElement.click();
            await page.waitForTimeout(1000);
            
            // Look for error logs in the expanded section
            const expandedContent = await jobElement.locator('..').textContent();
            if (expandedContent && expandedContent.length > 100) {
              const excerpt = expandedContent.substring(0, 500).replace(/\s+/g, ' ').trim();
              runAnalysis.logExcerpts.push(excerpt);
            }
          } catch (e) {
            // Ignore if we can't expand
          }
        }
      } catch (e) {
        // Skip if element is not accessible
      }
    }
    
    // Take screenshot of the failed run
    if (runAnalysis.status === 'failed') {
      await takeScreenshot(page, `run-${runId}-failed`);
    }
    
    // Analyze root causes
    if (runAnalysis.errors.length > 0) {
      const errorTypes = [...new Set(runAnalysis.errors.map(e => e.type))];
      
      errorTypes.forEach(errorType => {
        switch (errorType) {
          case 'permissions':
            runAnalysis.rootCauses.push('Workflow permissions are incorrectly configured');
            runAnalysis.suggestedFixes.push('Add required permissions to the workflow file');
            break;
          case 'npm-package':
            runAnalysis.rootCauses.push('NPM packages are not available or incorrectly specified');
            runAnalysis.suggestedFixes.push('Install packages using correct package manager (npm, pip, binary)');
            break;
          case 'syntax':
          case 'workflow-syntax':
            runAnalysis.rootCauses.push('Syntax errors in workflow or script files');
            runAnalysis.suggestedFixes.push('Fix YAML/script syntax errors');
            break;
          case 'missing-file':
            runAnalysis.rootCauses.push('Required files are missing');
            runAnalysis.suggestedFixes.push('Create missing files or fix file paths');
            break;
          case 'missing-command':
            runAnalysis.rootCauses.push('Required commands/tools are not installed');
            runAnalysis.suggestedFixes.push('Install missing tools in the workflow');
            break;
          case 'bash-syntax':
            runAnalysis.rootCauses.push('Bash script syntax errors');
            runAnalysis.suggestedFixes.push('Fix bash heredoc or script syntax');
            break;
        }
      });
    }
    
  } catch (error) {
    console.error(`   Error analyzing run ${runId}:`, error.message);
    runAnalysis.errors.push({ type: 'analysis-error', detail: error.message });
  }
  
  return runAnalysis;
}

async function findRecentFailedRuns(page, limit = 10) {
  console.log('\n🔍 Finding recent failed runs...');
  await page.goto(`${GITHUB_REPO_URL}/actions`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);
  
  const failedRuns = [];
  const runLinks = await page.locator('a[href*="/actions/runs/"]').all();
  
  for (let i = 0; i < Math.min(runLinks.length, limit * 2); i++) {
    try {
      const runLink = runLinks[i];
      const href = await runLink.getAttribute('href');
      const runId = href.match(/\/runs\/(\d+)/)?.[1];
      
      if (runId) {
        const parent = await runLink.locator('../..').first();
        const parentText = await parent.textContent();
        
        const isFailed = parentText.includes('failure') || 
                        parentText.includes('failed') ||
                        await parent.locator('.octicon-x').count() > 0;
        
        if (isFailed) {
          failedRuns.push(runId);
          if (failedRuns.length >= limit) break;
        }
      }
    } catch (e) {
      // Skip inaccessible elements
    }
  }
  
  console.log(`   Found ${failedRuns.length} recent failed runs`);
  return failedRuns;
}

async function generateComprehensivePlan(analyses) {
  console.log('\n' + '='.repeat(80));
  console.log('📋 COMPREHENSIVE ANALYSIS PLAN');
  console.log('='.repeat(80));
  
  const plan = {
    summary: {
      totalRuns: analyses.length,
      failedRuns: analyses.filter(a => a.status === 'failed').length,
      errorTypes: {},
      affectedWorkflows: new Set()
    },
    commonIssues: [],
    prioritizedFixes: [],
    implementation: []
  };
  
  // Collect statistics
  analyses.forEach(analysis => {
    if (analysis.status === 'failed') {
      plan.summary.affectedWorkflows.add(analysis.workflowName);
      
      analysis.errors.forEach(error => {
        plan.summary.errorTypes[error.type] = (plan.summary.errorTypes[error.type] || 0) + 1;
      });
    }
  });
  
  // Identify common issues
  Object.entries(plan.summary.errorTypes)
    .sort((a, b) => b[1] - a[1])
    .forEach(([type, count]) => {
      plan.commonIssues.push({ type, count, percentage: ((count / plan.summary.failedRuns) * 100).toFixed(1) });
    });
  
  console.log('\n📊 SUMMARY:');
  console.log(`   Total runs analyzed: ${plan.summary.totalRuns}`);
  console.log(`   Failed runs: ${plan.summary.failedRuns}`);
  console.log(`   Affected workflows: ${plan.summary.affectedWorkflows.size}`);
  
  console.log('\n🔍 COMMON ISSUES:');
  plan.commonIssues.forEach(issue => {
    console.log(`   - ${issue.type}: ${issue.count} occurrences (${issue.percentage}% of failures)`);
  });
  
  // Generate prioritized fixes
  console.log('\n🔧 PRIORITIZED FIXES:');
  
  if (plan.summary.errorTypes['permissions']) {
    plan.prioritizedFixes.push({
      priority: 1,
      issue: 'Workflow permission errors',
      action: 'Audit and fix all workflow permissions',
      impact: 'High - blocking multiple workflows'
    });
  }
  
  if (plan.summary.errorTypes['npm-package']) {
    plan.prioritizedFixes.push({
      priority: 2,
      issue: 'Missing or incorrect package installations',
      action: 'Fix package manager usage (npm vs pip vs binary)',
      impact: 'High - preventing tool execution'
    });
  }
  
  if (plan.summary.errorTypes['syntax'] || plan.summary.errorTypes['workflow-syntax']) {
    plan.prioritizedFixes.push({
      priority: 3,
      issue: 'Syntax errors in workflows',
      action: 'Fix YAML and script syntax errors',
      impact: 'High - preventing workflow execution'
    });
  }
  
  plan.prioritizedFixes.forEach(fix => {
    console.log(`   ${fix.priority}. ${fix.issue}`);
    console.log(`      Action: ${fix.action}`);
    console.log(`      Impact: ${fix.impact}`);
  });
  
  // Generate implementation steps
  console.log('\n📝 IMPLEMENTATION PLAN:');
  console.log('   Phase 1: Critical Fixes (Immediate)');
  console.log('   - Fix all permission errors in workflows');
  console.log('   - Correct package installation commands');
  console.log('   - Fix syntax errors preventing workflow execution');
  console.log('\n   Phase 2: Stabilization (Next)');
  console.log('   - Add missing files and configurations');
  console.log('   - Install missing tools and dependencies');
  console.log('   - Fix bash script syntax errors');
  console.log('\n   Phase 3: Validation (Final)');
  console.log('   - Run comprehensive tests on all workflows');
  console.log('   - Verify issue creation mechanism works');
  console.log('   - Document any remaining issues');
  
  return plan;
}

async function main() {
  await ensureOutputDirectory();
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 200
  });
  
  const context = await browser.newContext({
    viewport: { width: 1280, height: 800 }
  });
  const page = await context.newPage();
  
  const allAnalyses = [];
  
  try {
    // Analyze the specific run requested
    console.log('📋 Starting workflow run analysis...');
    const specificRun = await analyzeWorkflowRun(page, '16487845275');
    allAnalyses.push(specificRun);
    
    // Find and analyze recent failed runs
    const recentFailedRuns = await findRecentFailedRuns(page, 5);
    
    for (const runId of recentFailedRuns) {
      if (runId !== '16487845275') { // Don't re-analyze
        const analysis = await analyzeWorkflowRun(page, runId);
        allAnalyses.push(analysis);
      }
    }
    
    // Generate comprehensive plan
    const plan = await generateComprehensivePlan(allAnalyses);
    
    // Save detailed report
    const report = {
      timestamp: new Date().toISOString(),
      analyses: allAnalyses,
      plan: plan
    };
    
    const reportPath = path.join(OUTPUT_DIR, 'workflow-analysis-report.json');
    await fs.writeFile(reportPath, JSON.stringify(report, null, 2));
    console.log(`\n📄 Detailed report saved to: ${reportPath}`);
    
    // Save summary for quick reference
    const summary = {
      timestamp: new Date().toISOString(),
      totalAnalyzed: allAnalyses.length,
      failures: allAnalyses.filter(a => a.status === 'failed').map(a => ({
        runId: a.runId,
        workflow: a.workflowName,
        errors: a.errors.map(e => e.type),
        fixes: a.suggestedFixes
      })),
      plan: plan.prioritizedFixes
    };
    
    const summaryPath = path.join(OUTPUT_DIR, 'analysis-summary.json');
    await fs.writeFile(summaryPath, JSON.stringify(summary, null, 2));
    console.log(`📄 Summary saved to: ${summaryPath}`);
    
  } catch (error) {
    console.error('Error during analysis:', error);
    await takeScreenshot(page, 'analysis-error');
  } finally {
    await browser.close();
  }
}

if (require.main === module) {
  main().catch(console.error);
}

module.exports = { analyzeWorkflowRun, findRecentFailedRuns };