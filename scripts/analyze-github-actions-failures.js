const { chromium } = require('@playwright/test');
const fs = require('fs').promises;
const path = require('path');

// Configuration
const GITHUB_REPO_URL = 'https://github.com/lsendel/zamaz-debate-mcp';
const ACTIONS_URL = `${GITHUB_REPO_URL}/actions`;
const ISSUES_URL = `${GITHUB_REPO_URL}/issues`;
const OUTPUT_DIR = path.join(__dirname, 'github-actions-analysis');
const ANALYSIS_FILE = path.join(OUTPUT_DIR, 'workflow-failures-analysis.json');
const REPORT_FILE = path.join(OUTPUT_DIR, 'workflow-failures-report.md');

// Analysis data structure
const analysisData = {
  timestamp: new Date().toISOString(),
  repoUrl: GITHUB_REPO_URL,
  workflowRuns: [],
  healthMonitorRuns: [],
  issueCreationAttempts: [],
  errorPatterns: {},
  summary: {
    totalWorkflowsAnalyzed: 0,
    failingWorkflows: 0,
    healthMonitorRuns: 0,
    successfulIssueCreations: 0,
    failedIssueCreations: 0,
    commonErrors: []
  }
};

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
  return screenshotPath;
}

async function extractWorkflowErrors(page) {
  const errors = [];
  
  try {
    // Look for error annotations
    const errorAnnotations = await page.$$('.annotation-error');
    for (const annotation of errorAnnotations) {
      const errorText = await annotation.textContent();
      if (errorText) {
        errors.push({
          type: 'annotation',
          message: errorText.trim()
        });
      }
    }

    // Look for job failure messages
    const failureMessages = await page.$$('.job-step-error-message');
    for (const message of failureMessages) {
      const errorText = await message.textContent();
      if (errorText) {
        errors.push({
          type: 'job-step',
          message: errorText.trim()
        });
      }
    }

    // Look for workflow error summaries
    const errorSummaries = await page.$$('[aria-label*="error"], [aria-label*="failure"]');
    for (const summary of errorSummaries) {
      const errorText = await summary.textContent();
      if (errorText) {
        errors.push({
          type: 'summary',
          message: errorText.trim()
        });
      }
    }
  } catch (error) {
    console.error('Error extracting workflow errors:', error);
  }

  return errors;
}

async function analyzeWorkflowRun(page, runElement, index) {
  const runData = {
    index,
    name: '',
    status: '',
    url: '',
    timestamp: '',
    duration: '',
    errors: [],
    isHealthMonitor: false,
    issueCreationInfo: null,
    screenshots: []
  };

  try {
    // Get workflow name
    const nameElement = await runElement.$('.ActionListItem-label');
    if (nameElement) {
      runData.name = await nameElement.textContent();
      runData.isHealthMonitor = runData.name.toLowerCase().includes('workflow health monitor');
    }

    // Get status
    const statusElement = await runElement.$('[class*="State--"]');
    if (statusElement) {
      const statusClass = await statusElement.getAttribute('class');
      runData.status = statusClass.includes('failure') ? 'failed' : 
                      statusClass.includes('success') ? 'success' : 'unknown';
    }

    // Get timestamp and duration
    const timeElement = await runElement.$('relative-time');
    if (timeElement) {
      runData.timestamp = await timeElement.getAttribute('datetime');
    }

    // Click into the workflow run
    console.log(`Analyzing workflow run ${index + 1}: ${runData.name}`);
    await runElement.click();
    await page.waitForLoadState('networkidle');
    
    runData.url = page.url();
    
    // Take screenshot of workflow run page
    const screenshotPath = await takeScreenshot(page, `workflow-run-${index}`);
    runData.screenshots.push(screenshotPath);

    // Extract errors from the workflow run page
    runData.errors = await extractWorkflowErrors(page);

    // Look for job logs
    const jobElements = await page.$$('.job-list-item');
    for (let jobIndex = 0; jobIndex < jobElements.length; jobIndex++) {
      const jobElement = jobElements[jobIndex];
      const jobName = await jobElement.$eval('.job-name', el => el.textContent);
      
      // Check if this is a failed job
      const jobStatus = await jobElement.$eval('[class*="State--"]', el => el.className);
      if (jobStatus.includes('failure')) {
        console.log(`  Examining failed job: ${jobName}`);
        
        // Click on the job to view details
        await jobElement.click();
        await page.waitForTimeout(2000);
        
        // Look for specific error patterns in logs
        const logLines = await page.$$('.log-line');
        const jobErrors = [];
        
        for (const logLine of logLines.slice(-50)) { // Check last 50 lines
          const lineText = await logLine.textContent();
          
          // Look for specific error patterns
          if (lineText.includes('Error:') || 
              lineText.includes('Failed to') || 
              lineText.includes('Permission denied') ||
              lineText.includes('token') ||
              lineText.includes('authentication') ||
              lineText.includes('issue creation')) {
            jobErrors.push(lineText.trim());
          }
        }
        
        if (jobErrors.length > 0) {
          runData.errors.push({
            type: 'job-logs',
            job: jobName,
            messages: jobErrors
          });
        }
        
        // Take screenshot of job logs
        const jobScreenshotPath = await takeScreenshot(page, `job-${index}-${jobIndex}`);
        runData.screenshots.push(jobScreenshotPath);
      }
    }

    // Check for issue creation attempts in logs
    if (runData.isHealthMonitor) {
      const pageContent = await page.content();
      
      // Look for issue creation patterns
      if (pageContent.includes('Creating issue') || 
          pageContent.includes('Issue created') ||
          pageContent.includes('Failed to create issue')) {
        
        runData.issueCreationInfo = {
          attempted: true,
          successful: pageContent.includes('Issue created successfully'),
          errors: []
        };
        
        // Extract issue creation errors
        const issueErrorPatterns = [
          /Failed to create issue: (.+)/g,
          /Issue creation error: (.+)/g,
          /Permission denied.*issue/gi,
          /Bad credentials/gi,
          /Resource not accessible by integration/gi
        ];
        
        for (const pattern of issueErrorPatterns) {
          const matches = pageContent.matchAll(pattern);
          for (const match of matches) {
            runData.issueCreationInfo.errors.push(match[0]);
          }
        }
      }
    }

    // Go back to actions list
    await page.goto(ACTIONS_URL);
    await page.waitForLoadState('networkidle');

  } catch (error) {
    console.error(`Error analyzing workflow run ${index + 1}:`, error);
    runData.errors.push({
      type: 'analysis-error',
      message: error.message
    });
  }

  return runData;
}

async function checkIssuesTab(page) {
  console.log('\nChecking Issues tab for created issues...');
  
  const issuesData = {
    url: ISSUES_URL,
    totalIssues: 0,
    workflowHealthIssues: [],
    screenshots: []
  };
  
  try {
    await page.goto(ISSUES_URL);
    await page.waitForLoadState('networkidle');
    
    // Take screenshot of issues page
    const screenshotPath = await takeScreenshot(page, 'issues-page');
    issuesData.screenshots.push(screenshotPath);
    
    // Look for workflow health monitor issues
    const issueElements = await page.$$('.js-issue-row');
    issuesData.totalIssues = issueElements.length;
    
    for (const issueElement of issueElements) {
      const titleElement = await issueElement.$('.js-navigation-open');
      if (titleElement) {
        const title = await titleElement.textContent();
        if (title.toLowerCase().includes('workflow') && 
            (title.toLowerCase().includes('health') || title.toLowerCase().includes('failure'))) {
          
          const issueUrl = await titleElement.getAttribute('href');
          const timeElement = await issueElement.$('relative-time');
          const timestamp = timeElement ? await timeElement.getAttribute('datetime') : null;
          
          issuesData.workflowHealthIssues.push({
            title: title.trim(),
            url: `${GITHUB_REPO_URL}${issueUrl}`,
            timestamp
          });
        }
      }
    }
    
    // Search specifically for workflow health issues
    const searchInput = await page.$('#js-issues-search');
    if (searchInput) {
      await searchInput.fill('workflow health monitor');
      await page.keyboard.press('Enter');
      await page.waitForTimeout(2000);
      
      const searchScreenshotPath = await takeScreenshot(page, 'issues-search-workflow-health');
      issuesData.screenshots.push(searchScreenshotPath);
    }
    
  } catch (error) {
    console.error('Error checking issues tab:', error);
  }
  
  return issuesData;
}

async function generateReport() {
  let report = `# GitHub Actions Workflow Failures Analysis Report

Generated: ${analysisData.timestamp}
Repository: ${analysisData.repoUrl}

## Executive Summary

- **Total Workflows Analyzed**: ${analysisData.summary.totalWorkflowsAnalyzed}
- **Failing Workflows**: ${analysisData.summary.failingWorkflows}
- **Workflow Health Monitor Runs Found**: ${analysisData.summary.healthMonitorRuns}
- **Successful Issue Creations**: ${analysisData.summary.successfulIssueCreations}
- **Failed Issue Creations**: ${analysisData.summary.failedIssueCreations}

## Detailed Workflow Analysis

`;

  // Add failing workflows section
  const failingWorkflows = analysisData.workflowRuns.filter(run => run.status === 'failed');
  
  report += `### Failing Workflows (${failingWorkflows.length})\n\n`;
  
  for (const workflow of failingWorkflows) {
    report += `#### ${workflow.name}\n`;
    report += `- **Status**: ${workflow.status}\n`;
    report += `- **URL**: ${workflow.url}\n`;
    report += `- **Timestamp**: ${workflow.timestamp}\n`;
    report += `- **Is Health Monitor**: ${workflow.isHealthMonitor ? 'Yes' : 'No'}\n`;
    
    if (workflow.errors.length > 0) {
      report += `- **Errors Found**:\n`;
      for (const error of workflow.errors) {
        report += `  - **Type**: ${error.type}\n`;
        if (error.job) {
          report += `    - **Job**: ${error.job}\n`;
        }
        if (Array.isArray(error.messages)) {
          for (const msg of error.messages) {
            report += `    - ${msg}\n`;
          }
        } else if (error.message) {
          report += `    - ${error.message}\n`;
        }
      }
    }
    
    if (workflow.issueCreationInfo) {
      report += `- **Issue Creation**:\n`;
      report += `  - **Attempted**: ${workflow.issueCreationInfo.attempted}\n`;
      report += `  - **Successful**: ${workflow.issueCreationInfo.successful}\n`;
      if (workflow.issueCreationInfo.errors.length > 0) {
        report += `  - **Errors**:\n`;
        for (const err of workflow.issueCreationInfo.errors) {
          report += `    - ${err}\n`;
        }
      }
    }
    
    report += `\n`;
  }

  // Add health monitor specific section
  report += `## Workflow Health Monitor Analysis\n\n`;
  
  const healthMonitorRuns = analysisData.workflowRuns.filter(run => run.isHealthMonitor);
  
  if (healthMonitorRuns.length === 0) {
    report += `**WARNING**: No Workflow Health Monitor runs were found in the analyzed workflows!\n\n`;
  } else {
    report += `Found ${healthMonitorRuns.length} Workflow Health Monitor runs:\n\n`;
    
    for (const run of healthMonitorRuns) {
      report += `### Health Monitor Run - ${run.timestamp}\n`;
      report += `- **Status**: ${run.status}\n`;
      report += `- **URL**: ${run.url}\n`;
      
      if (run.issueCreationInfo) {
        report += `- **Issue Creation Attempted**: ${run.issueCreationInfo.attempted}\n`;
        report += `- **Issue Creation Successful**: ${run.issueCreationInfo.successful}\n`;
        
        if (run.issueCreationInfo.errors.length > 0) {
          report += `- **Issue Creation Errors**:\n`;
          for (const err of run.issueCreationInfo.errors) {
            report += `  - ${err}\n`;
          }
        }
      } else {
        report += `- **Issue Creation**: No evidence of issue creation attempt found\n`;
      }
      
      report += `\n`;
    }
  }

  // Add error patterns section
  report += `## Common Error Patterns\n\n`;
  
  const errorPatterns = {};
  for (const workflow of analysisData.workflowRuns) {
    for (const error of workflow.errors) {
      const key = error.type;
      if (!errorPatterns[key]) {
        errorPatterns[key] = [];
      }
      if (Array.isArray(error.messages)) {
        errorPatterns[key].push(...error.messages);
      } else if (error.message) {
        errorPatterns[key].push(error.message);
      }
    }
  }
  
  for (const [type, messages] of Object.entries(errorPatterns)) {
    report += `### ${type} errors:\n`;
    const uniqueMessages = [...new Set(messages)];
    for (const msg of uniqueMessages.slice(0, 10)) { // Show top 10
      report += `- ${msg}\n`;
    }
    report += `\n`;
  }

  // Add issues tab findings
  if (analysisData.issuesTabData) {
    report += `## Issues Tab Analysis\n\n`;
    report += `- **Total Issues**: ${analysisData.issuesTabData.totalIssues}\n`;
    report += `- **Workflow Health Issues Found**: ${analysisData.issuesTabData.workflowHealthIssues.length}\n\n`;
    
    if (analysisData.issuesTabData.workflowHealthIssues.length > 0) {
      report += `### Workflow Health Issues:\n`;
      for (const issue of analysisData.issuesTabData.workflowHealthIssues) {
        report += `- **${issue.title}**\n`;
        report += `  - URL: ${issue.url}\n`;
        report += `  - Created: ${issue.timestamp}\n`;
      }
    } else {
      report += `**WARNING**: No workflow health related issues were found in the Issues tab!\n`;
    }
  }

  // Add recommendations
  report += `\n## Key Findings and Recommendations\n\n`;
  
  // Analyze findings
  const hasHealthMonitorRuns = healthMonitorRuns.length > 0;
  const hasFailedHealthMonitor = healthMonitorRuns.some(run => run.status === 'failed');
  const hasIssueCreationAttempts = healthMonitorRuns.some(run => run.issueCreationInfo?.attempted);
  const hasSuccessfulIssueCreation = healthMonitorRuns.some(run => run.issueCreationInfo?.successful);
  const hasPermissionErrors = JSON.stringify(analysisData).includes('Permission denied') || 
                              JSON.stringify(analysisData).includes('Resource not accessible');
  const hasTokenErrors = JSON.stringify(analysisData).includes('token') || 
                        JSON.stringify(analysisData).includes('credentials');

  if (!hasHealthMonitorRuns) {
    report += `1. **Workflow Health Monitor is not running**: The workflow may be disabled or not triggered properly.\n`;
    report += `   - Check if the workflow file exists and is properly configured\n`;
    report += `   - Verify the cron schedule or trigger conditions\n\n`;
  }
  
  if (hasFailedHealthMonitor && !hasIssueCreationAttempts) {
    report += `2. **Health Monitor runs are failing before issue creation**: The workflow is erroring out before it can attempt to create issues.\n`;
    report += `   - Review the workflow logs to identify the failure point\n`;
    report += `   - Check if all required secrets and permissions are configured\n\n`;
  }
  
  if (hasIssueCreationAttempts && !hasSuccessfulIssueCreation) {
    report += `3. **Issue creation is being attempted but failing**: The workflow is running but cannot create issues.\n`;
    
    if (hasPermissionErrors) {
      report += `   - **Permission errors detected**: The GitHub token lacks necessary permissions\n`;
      report += `     - Ensure the token has 'issues: write' permission\n`;
      report += `     - Check if the workflow has the correct permissions block\n`;
    }
    
    if (hasTokenErrors) {
      report += `   - **Token/credential errors detected**: Authentication issues present\n`;
      report += `     - Verify GITHUB_TOKEN is properly configured\n`;
      report += `     - Check if using the correct token (secrets.GITHUB_TOKEN)\n`;
    }
    
    report += `\n`;
  }
  
  if (!analysisData.issuesTabData?.workflowHealthIssues?.length) {
    report += `4. **No workflow health issues found in Issues tab**: Confirms that issue creation is not working.\n\n`;
  }

  // Add screenshots section
  report += `\n## Screenshots\n\n`;
  report += `Screenshots have been saved to: ${OUTPUT_DIR}\n\n`;
  
  const allScreenshots = [];
  for (const workflow of analysisData.workflowRuns) {
    allScreenshots.push(...workflow.screenshots);
  }
  if (analysisData.issuesTabData?.screenshots) {
    allScreenshots.push(...analysisData.issuesTabData.screenshots);
  }
  
  for (const screenshot of allScreenshots) {
    report += `- ${path.basename(screenshot)}\n`;
  }

  return report;
}

async function main() {
  console.log('Starting GitHub Actions Workflow Analysis...');
  await ensureOutputDirectory();
  
  const browser = await chromium.launch({ 
    headless: false,
    viewport: { width: 1920, height: 1080 }
  });
  
  try {
    const context = await browser.newContext({
      viewport: { width: 1920, height: 1080 }
    });
    const page = await context.newPage();
    
    // Navigate to GitHub Actions page
    console.log(`Navigating to ${ACTIONS_URL}`);
    await page.goto(ACTIONS_URL);
    await page.waitForLoadState('networkidle');
    
    // Take initial screenshot
    await takeScreenshot(page, 'actions-overview');
    
    // Get all workflow runs
    const workflowRuns = await page.$$('.ActionListItem');
    console.log(`Found ${workflowRuns.length} workflow runs to analyze`);
    
    // Analyze each workflow run (limit to first 20 for performance)
    const runsToAnalyze = Math.min(workflowRuns.length, 20);
    for (let i = 0; i < runsToAnalyze; i++) {
      // Re-get the elements as page navigation invalidates them
      await page.goto(ACTIONS_URL);
      await page.waitForLoadState('networkidle');
      const currentRuns = await page.$$('.ActionListItem');
      
      if (i < currentRuns.length) {
        const runData = await analyzeWorkflowRun(page, currentRuns[i], i);
        analysisData.workflowRuns.push(runData);
        
        // Update summary
        analysisData.summary.totalWorkflowsAnalyzed++;
        if (runData.status === 'failed') {
          analysisData.summary.failingWorkflows++;
        }
        if (runData.isHealthMonitor) {
          analysisData.summary.healthMonitorRuns++;
          analysisData.healthMonitorRuns.push(runData);
          
          if (runData.issueCreationInfo?.attempted) {
            analysisData.issueCreationAttempts.push(runData);
            if (runData.issueCreationInfo.successful) {
              analysisData.summary.successfulIssueCreations++;
            } else {
              analysisData.summary.failedIssueCreations++;
            }
          }
        }
      }
    }
    
    // Check the Issues tab
    analysisData.issuesTabData = await checkIssuesTab(page);
    
    // Save raw analysis data
    await fs.writeFile(ANALYSIS_FILE, JSON.stringify(analysisData, null, 2));
    console.log(`\nAnalysis data saved to: ${ANALYSIS_FILE}`);
    
    // Generate and save report
    const report = await generateReport();
    await fs.writeFile(REPORT_FILE, report);
    console.log(`Report saved to: ${REPORT_FILE}`);
    
    // Print summary to console
    console.log('\n=== ANALYSIS SUMMARY ===');
    console.log(`Total Workflows Analyzed: ${analysisData.summary.totalWorkflowsAnalyzed}`);
    console.log(`Failing Workflows: ${analysisData.summary.failingWorkflows}`);
    console.log(`Health Monitor Runs: ${analysisData.summary.healthMonitorRuns}`);
    console.log(`Issue Creation Attempts: ${analysisData.issueCreationAttempts.length}`);
    console.log(`Successful Issue Creations: ${analysisData.summary.successfulIssueCreations}`);
    console.log(`Failed Issue Creations: ${analysisData.summary.failedIssueCreations}`);
    console.log(`Issues Found in Issues Tab: ${analysisData.issuesTabData?.workflowHealthIssues?.length || 0}`);
    
  } catch (error) {
    console.error('Fatal error during analysis:', error);
  } finally {
    await browser.close();
  }
}

// Run the analysis
main().catch(console.error);