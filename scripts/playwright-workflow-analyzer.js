#!/usr/bin/env node

const { chromium } = require('playwright');
const fs = require('fs');

async function analyzeWorkflows() {
  console.log('🔍 Analyzing GitHub Actions workflows with Playwright...\n');
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 500 
  });
  
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });
  
  const page = await context.newPage();
  
  const errors = [];
  const screenshots = [];
  
  try {
    // Navigate to GitHub Actions
    console.log('Navigating to GitHub Actions...');
    await page.goto('https://github.com/lsendel/zamaz-debate-mcp/actions');
    
    // Wait for workflow list to load
    await page.waitForSelector('[data-testid="workflow-run-row"]', { timeout: 10000 });
    
    // Get all failed workflow runs
    const failedRuns = await page.$$eval('[data-testid="workflow-run-row"]', rows => {
      return rows
        .filter(row => {
          const status = row.querySelector('[data-testid="workflow-run-status"]');
          return status && (status.textContent.includes('Failure') || 
                           status.querySelector('svg[aria-label*="failure"]') ||
                           status.querySelector('.octicon-x-circle'));
        })
        .map(row => {
          const link = row.querySelector('a[href*="/actions/runs/"]');
          const name = row.querySelector('[data-testid="workflow-run-title"]');
          return {
            url: link ? link.href : null,
            name: name ? name.textContent.trim() : 'Unknown',
            runId: link ? link.href.match(/runs\/(\d+)/)?.[1] : null
          };
        })
        .filter(item => item.url);
    });
    
    console.log(`Found ${failedRuns.length} failed workflow runs to analyze\n`);
    
    // Analyze each failed run
    for (const run of failedRuns.slice(0, 5)) { // Limit to 5 for performance
      console.log(`\nAnalyzing: ${run.name}`);
      console.log(`URL: ${run.url}`);
      
      await page.goto(run.url);
      await page.waitForSelector('.Box-body', { timeout: 10000 });
      
      // Take screenshot
      const screenshotPath = `workflow-error-${run.runId}.png`;
      await page.screenshot({ 
        path: screenshotPath,
        fullPage: true 
      });
      screenshots.push(screenshotPath);
      
      // Get failed jobs
      const failedJobs = await page.$$eval('.Box-row', rows => {
        return rows
          .filter(row => {
            const status = row.querySelector('.octicon-x-circle');
            return status !== null;
          })
          .map(row => {
            const jobName = row.querySelector('h4')?.textContent?.trim() || 'Unknown job';
            const steps = Array.from(row.querySelectorAll('.Box-body .Box-row')).map(step => {
              const stepName = step.querySelector('.text-bold')?.textContent?.trim();
              const hasError = step.querySelector('.octicon-x-circle') !== null;
              return { stepName, hasError };
            }).filter(step => step.hasError);
            
            return { jobName, failedSteps: steps };
          });
      });
      
      // Look for error messages
      const errorMessages = await page.$$eval('.ansi-red, .text-danger, pre', elements => {
        return elements
          .map(el => el.textContent?.trim())
          .filter(text => text && text.length > 0 && text.length < 500)
          .filter(text => 
            text.includes('error') || 
            text.includes('Error') || 
            text.includes('failed') ||
            text.includes('Failed') ||
            text.includes('npm ERR!') ||
            text.includes('mvn') ||
            text.includes('Process completed with exit code')
          );
      });
      
      const runErrors = {
        workflow: run.name,
        url: run.url,
        failedJobs: failedJobs,
        errorMessages: [...new Set(errorMessages)].slice(0, 5), // Unique messages, limit 5
        screenshot: screenshotPath
      };
      
      errors.push(runErrors);
      
      // Check for specific error patterns
      const pageContent = await page.content();
      if (pageContent.includes('Could not find or load main class #')) {
        runErrors.specificErrors = runErrors.specificErrors || [];
        runErrors.specificErrors.push('Maven ClassNotFoundException');
      }
      if (pageContent.includes('unable to cache dependencies')) {
        runErrors.specificErrors = runErrors.specificErrors || [];
        runErrors.specificErrors.push('NPM cache error');
      }
      if (pageContent.includes('No pom.xml found')) {
        runErrors.specificErrors = runErrors.specificErrors || [];
        runErrors.specificErrors.push('Missing pom.xml');
      }
    }
    
  } catch (error) {
    console.error('Error during analysis:', error);
  } finally {
    await browser.close();
  }
  
  // Generate report
  const report = {
    timestamp: new Date().toISOString(),
    totalErrorsAnalyzed: errors.length,
    errors: errors,
    commonPatterns: {},
    recommendations: []
  };
  
  // Analyze common patterns
  errors.forEach(error => {
    error.specificErrors?.forEach(specificError => {
      report.commonPatterns[specificError] = (report.commonPatterns[specificError] || 0) + 1;
    });
  });
  
  // Generate recommendations
  if (report.commonPatterns['Maven ClassNotFoundException']) {
    report.recommendations.push('Multiple workflows have Maven class errors - check MAVEN_BATCH_MODE configuration');
  }
  if (report.commonPatterns['NPM cache error']) {
    report.recommendations.push('NPM cache errors detected - verify package-lock.json paths');
  }
  if (report.commonPatterns['Missing pom.xml']) {
    report.recommendations.push('Some modules are missing pom.xml files');
  }
  
  // Save report
  fs.writeFileSync('playwright-workflow-report.json', JSON.stringify(report, null, 2));
  console.log('\n✅ Analysis complete!');
  console.log(`📊 Report saved to: playwright-workflow-report.json`);
  console.log(`📸 Screenshots saved: ${screenshots.join(', ')}`);
  
  // Print summary
  console.log('\n📋 SUMMARY:');
  console.log(`Total failed workflows analyzed: ${errors.length}`);
  console.log('\nCommon error patterns:');
  Object.entries(report.commonPatterns).forEach(([pattern, count]) => {
    console.log(`  - ${pattern}: ${count} occurrences`);
  });
  console.log('\nRecommendations:');
  report.recommendations.forEach(rec => {
    console.log(`  - ${rec}`);
  });
}

// Run the analysis
analyzeWorkflows().catch(console.error);