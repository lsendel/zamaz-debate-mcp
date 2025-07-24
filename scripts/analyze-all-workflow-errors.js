const { chromium } = require('@playwright/test');
const fs = require('fs').promises;
const path = require('path');

const OUTPUT_DIR = path.join(__dirname, 'workflow-error-analysis');

async function ensureOutputDirectory() {
  try {
    await fs.mkdir(OUTPUT_DIR, { recursive: true });
  } catch (error) {
    console.error('Error creating output directory:', error);
  }
}

async function analyzeWorkflowRun(page, runUrl) {
  console.log(`\n🔍 Analyzing: ${runUrl}`);
  
  const errors = {
    url: runUrl,
    timestamp: new Date().toISOString(),
    mavenErrors: [],
    echoErrors: [],
    otherErrors: [],
    failedJobs: []
  };
  
  try {
    await page.goto(runUrl, { waitUntil: 'networkidle', timeout: 60000 });
    await page.waitForTimeout(3000);
    
    // Get all failed jobs
    const failedJobElements = await page.locator('.octicon-x-circle-fill').all();
    console.log(`   Found ${failedJobElements.length} failed elements`);
    
    // Click on each failed job to expand details
    for (let i = 0; i < failedJobElements.length; i++) {
      try {
        const parent = await failedJobElements[i].locator('..').first();
        const jobName = await parent.textContent();
        errors.failedJobs.push(jobName.trim());
        
        // Try to click to expand
        await parent.click();
        await page.waitForTimeout(1000);
      } catch (e) {
        // Continue if can't click
      }
    }
    
    // Get page text for analysis
    const pageText = await page.textContent('body');
    
    // Look for Maven errors
    const mavenErrorPattern = /Error: Could not find or load main class #.*?ClassNotFoundException: #/gs;
    const mavenMatches = pageText.match(mavenErrorPattern);
    if (mavenMatches) {
      errors.mavenErrors = [...new Set(mavenMatches)];
      console.log(`   Found ${errors.mavenErrors.length} Maven errors`);
    }
    
    // Look for echo errors
    const echoErrorPattern = /Run echo.*?Error: Process completed with exit code 1/gs;
    const echoMatches = pageText.match(echoErrorPattern);
    if (echoMatches) {
      errors.echoErrors = [...new Set(echoMatches)];
      console.log(`   Found ${errors.echoErrors.length} echo errors`);
    }
    
    // Look for other process errors
    const processErrorPattern = /Error: Process completed with exit code \d+/g;
    const processMatches = pageText.match(processErrorPattern);
    if (processMatches) {
      errors.otherErrors = [...new Set(processMatches)];
      console.log(`   Found ${errors.otherErrors.length} other errors`);
    }
    
    // Look for specific Maven commands that failed
    const mavenCommandPattern = /Run mvn ([^\n]+)\nError:/g;
    let match;
    const failedMavenCommands = [];
    while ((match = mavenCommandPattern.exec(pageText)) !== null) {
      failedMavenCommands.push(match[1]);
    }
    if (failedMavenCommands.length > 0) {
      errors.failedMavenCommands = [...new Set(failedMavenCommands)];
      console.log(`   Failed Maven commands: ${errors.failedMavenCommands.join(', ')}`);
    }
    
  } catch (error) {
    console.error(`   Error analyzing ${runUrl}:`, error.message);
    errors.analysisError = error.message;
  }
  
  return errors;
}

async function analyzeAllJobs(page, runUrl) {
  console.log(`\n📋 Deep analysis of workflow run: ${runUrl}`);
  
  const jobAnalysis = {
    url: runUrl,
    timestamp: new Date().toISOString(),
    jobs: []
  };
  
  try {
    await page.goto(runUrl, { waitUntil: 'networkidle', timeout: 60000 });
    await page.waitForTimeout(3000);
    
    // Find all job links
    const jobLinks = await page.locator('a[href*="/job/"]').all();
    console.log(`   Found ${jobLinks.length} jobs to analyze`);
    
    for (const jobLink of jobLinks) {
      const jobUrl = await jobLink.getAttribute('href');
      const jobName = await jobLink.textContent();
      
      if (jobUrl) {
        const fullJobUrl = `https://github.com${jobUrl}`;
        console.log(`   Analyzing job: ${jobName}`);
        
        // Navigate to job page
        await page.goto(fullJobUrl, { waitUntil: 'networkidle', timeout: 60000 });
        await page.waitForTimeout(2000);
        
        const jobErrors = {
          name: jobName.trim(),
          url: fullJobUrl,
          errors: []
        };
        
        // Get job content
        const jobText = await page.textContent('body');
        
        // Check for Maven errors
        if (jobText.includes('Could not find or load main class #')) {
          jobErrors.errors.push({
            type: 'maven-class-not-found',
            details: 'Maven trying to execute class named #'
          });
          
          // Find the exact Maven command
          const cmdMatch = jobText.match(/Run (mvn [^\n]+)\n.*?Error: Could not find or load main class #/);
          if (cmdMatch) {
            jobErrors.errors.push({
              type: 'maven-command',
              command: cmdMatch[1]
            });
          }
        }
        
        // Check for echo errors
        if (jobText.includes('echo "## 🏗️ CI Pipeline Summary"')) {
          jobErrors.errors.push({
            type: 'echo-error',
            details: 'CI Pipeline Summary echo failed'
          });
        }
        
        if (jobErrors.errors.length > 0) {
          jobAnalysis.jobs.push(jobErrors);
        }
      }
    }
    
  } catch (error) {
    console.error('Error in deep analysis:', error);
    jobAnalysis.error = error.message;
  }
  
  return jobAnalysis;
}

async function findRootCause() {
  console.log('\n🔎 Analyzing root cause of Maven errors...');
  
  // The error "Could not find or load main class #" suggests Maven is trying to execute '#' as a class
  // This typically happens when:
  // 1. There's a comment character in the Maven command
  // 2. Environment variables contain # characters
  // 3. Maven batch mode flags contain comments
  
  console.log('   Likely cause: MAVEN_BATCH_MODE environment variable contains comments or # characters');
  console.log('   Need to check how Maven commands are constructed in workflows');
  
  return {
    likelyCause: 'MAVEN_BATCH_MODE or other environment variables contain # character',
    suggestedFix: 'Remove comments from Maven environment variables',
    checkFiles: [
      '.github/workflows/ci.yml',
      '.github/workflows/ci-cd.yml',
      '.github/workflows/code-quality.yml',
      'Any workflow that sets MAVEN_BATCH_MODE'
    ]
  };
}

async function findRecentWorkflowRuns(page, minutes = 30) {
  console.log(`\n🔍 Finding workflow runs from the last ${minutes} minutes...`);
  
  await page.goto('https://github.com/lsendel/zamaz-debate-mcp/actions', { 
    waitUntil: 'networkidle', 
    timeout: 60000 
  });
  await page.waitForTimeout(3000);
  
  const recentRuns = [];
  const now = new Date();
  const cutoffTime = new Date(now.getTime() - minutes * 60 * 1000);
  
  // Find all workflow run links
  const runElements = await page.locator('a[href*="/actions/runs/"]').all();
  
  for (const element of runElements) {
    try {
      const href = await element.getAttribute('href');
      const runId = href.match(/\/runs\/(\d+)/)?.[1];
      
      if (runId && !recentRuns.some(r => r.includes(runId))) {
        const parentElement = await element.locator('../..').first();
        const timeText = await parentElement.locator('relative-time, time').first().getAttribute('datetime');
        
        if (timeText) {
          const runTime = new Date(timeText);
          if (runTime >= cutoffTime) {
            const runUrl = `https://github.com${href}`;
            const workflowName = await element.textContent();
            console.log(`   Found recent run: ${workflowName.trim()} - ${runUrl}`);
            recentRuns.push(runUrl);
          }
        }
      }
    } catch (e) {
      // Skip if can't process
    }
  }
  
  console.log(`   Total recent runs found: ${recentRuns.length}`);
  return recentRuns;
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
  
  // Find all workflow runs from last 30 minutes
  const workflowRuns = await findRecentWorkflowRuns(page, 30);
  
  // Also include the specific runs mentioned
  const specificRuns = [
    'https://github.com/lsendel/zamaz-debate-mcp/actions/runs/16488838408',
    'https://github.com/lsendel/zamaz-debate-mcp/actions/runs/16488838359'
  ];
  
  // Combine and deduplicate
  const allRuns = [...new Set([...workflowRuns, ...specificRuns])];
  console.log(`\n📋 Analyzing ${allRuns.length} workflow runs...`);
  
  const allErrors = [];
  const detailedAnalysis = [];
  
  try {
    // First pass - quick error scan
    for (const runUrl of allRuns) {
      const errors = await analyzeWorkflowRun(page, runUrl);
      allErrors.push(errors);
    }
    
    // Second pass - deep job analysis for runs with errors
    const runsWithErrors = allErrors.filter(e => 
      e.mavenErrors.length > 0 || 
      e.echoErrors.length > 0 || 
      e.otherErrors.length > 0
    ).map(e => e.url);
    
    for (const runUrl of runsWithErrors) {
      const jobAnalysis = await analyzeAllJobs(page, runUrl);
      detailedAnalysis.push(jobAnalysis);
    }
    
    // Analyze root cause
    const rootCause = await findRootCause();
    
    // Generate comprehensive report
    const report = {
      timestamp: new Date().toISOString(),
      summary: {
        totalRuns: allRuns.length,
        runsWithErrors: runsWithErrors.length,
        totalMavenErrors: allErrors.reduce((sum, e) => sum + e.mavenErrors.length, 0),
        totalEchoErrors: allErrors.reduce((sum, e) => sum + e.echoErrors.length, 0),
        totalOtherErrors: allErrors.reduce((sum, e) => sum + e.otherErrors.length, 0)
      },
      errors: allErrors,
      detailedAnalysis: detailedAnalysis,
      rootCause: rootCause,
      actionPlan: {
        immediate: [
          'Check MAVEN_BATCH_MODE environment variable in all workflows',
          'Remove any # characters or comments from Maven environment variables',
          'Fix echo commands that reference undefined variables'
        ],
        verification: [
          'Run workflows again after fixes',
          'Monitor for "Could not find or load main class #" errors',
          'Ensure all Maven commands execute successfully'
        ]
      }
    };
    
    // Save report
    const reportPath = path.join(OUTPUT_DIR, 'error-analysis-report.json');
    await fs.writeFile(reportPath, JSON.stringify(report, null, 2));
    console.log(`\n📄 Report saved to: ${reportPath}`);
    
    // Print summary
    console.log('\n' + '='.repeat(80));
    console.log('📊 ERROR ANALYSIS SUMMARY');
    console.log('='.repeat(80));
    console.log(`Total Maven errors: ${report.summary.totalMavenErrors}`);
    console.log(`Total echo errors: ${report.summary.totalEchoErrors}`);
    console.log(`Total other errors: ${report.summary.totalOtherErrors}`);
    console.log('\n🔧 ROOT CAUSE:');
    console.log(`   ${rootCause.likelyCause}`);
    console.log('\n📋 IMMEDIATE ACTIONS:');
    report.actionPlan.immediate.forEach((action, i) => {
      console.log(`   ${i + 1}. ${action}`);
    });
    
  } catch (error) {
    console.error('Error during analysis:', error);
  } finally {
    await browser.close();
  }
}

if (require.main === module) {
  main().catch(console.error);
}

module.exports = { analyzeWorkflowRun, findRootCause };