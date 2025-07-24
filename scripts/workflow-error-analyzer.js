#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');

console.log('🔍 Comprehensive GitHub Actions Error Analysis\n');

// Get all recent workflow runs
try {
  // First, get list of all workflows
  const workflows = JSON.parse(execSync('gh api repos/$GITHUB_REPOSITORY/actions/workflows --paginate', { encoding: 'utf8' }));
  
  console.log(`Found ${workflows.workflows.length} workflows\n`);
  
  // Get recent runs for all workflows
  const allRuns = JSON.parse(execSync('gh run list --limit 50 --json databaseId,name,conclusion,status,event,headBranch,createdAt,url', { encoding: 'utf8' }));
  
  // Group by workflow name
  const workflowGroups = {};
  allRuns.forEach(run => {
    if (!workflowGroups[run.name]) {
      workflowGroups[run.name] = [];
    }
    workflowGroups[run.name].push(run);
  });
  
  // Analyze each workflow
  const errorSummary = [];
  
  for (const [workflowName, runs] of Object.entries(workflowGroups)) {
    const latestRun = runs[0]; // Most recent run
    
    if (latestRun.conclusion === 'failure' || latestRun.status === 'failure') {
      console.log(`\n❌ ${workflowName}`);
      console.log(`   Latest run: ${latestRun.conclusion || latestRun.status}`);
      console.log(`   URL: ${latestRun.url}`);
      
      try {
        // Get detailed job information
        const jobs = JSON.parse(execSync(`gh run view ${latestRun.databaseId} --json jobs`, { encoding: 'utf8' }));
        
        const failedJobs = jobs.jobs.filter(job => job.conclusion === 'failure');
        
        if (failedJobs.length > 0) {
          console.log(`   Failed jobs:`);
          
          failedJobs.forEach(job => {
            console.log(`   - ${job.name}`);
            
            // Get failed steps
            const failedSteps = job.steps.filter(step => step.conclusion === 'failure');
            failedSteps.forEach(step => {
              console.log(`     ❌ ${step.name}`);
            });
            
            // Try to get error logs
            try {
              const logs = execSync(`gh run view ${latestRun.databaseId} --job ${job.databaseId} --log-failed 2>/dev/null | tail -20`, { encoding: 'utf8' });
              
              // Look for common error patterns
              if (logs.includes('Could not find or load main class #')) {
                console.log('     🐛 Maven ClassNotFoundException detected');
                errorSummary.push({ workflow: workflowName, error: 'Maven class # error', job: job.name });
              }
              if (logs.includes('unable to cache dependencies')) {
                console.log('     🐛 NPM cache error detected');
                errorSummary.push({ workflow: workflowName, error: 'NPM cache error', job: job.name });
              }
              if (logs.includes('No pom.xml found')) {
                console.log('     🐛 Missing pom.xml');
                errorSummary.push({ workflow: workflowName, error: 'Missing pom.xml', job: job.name });
              }
              if (logs.includes('npm ERR!')) {
                console.log('     🐛 NPM error detected');
                errorSummary.push({ workflow: workflowName, error: 'NPM error', job: job.name });
              }
              if (logs.includes('docker: command not found')) {
                console.log('     🐛 Docker not available');
                errorSummary.push({ workflow: workflowName, error: 'Docker not found', job: job.name });
              }
              if (logs.includes('mvn: command not found')) {
                console.log('     🐛 Maven not available');
                errorSummary.push({ workflow: workflowName, error: 'Maven not found', job: job.name });
              }
            } catch (e) {
              // Can't get logs, skip
            }
          });
        }
      } catch (e) {
        console.log(`   ⚠️  Could not get job details: ${e.message}`);
      }
    } else if (latestRun.conclusion === 'success') {
      console.log(`\n✅ ${workflowName} - Passing`);
    }
  }
  
  // Print summary
  console.log('\n' + '═'.repeat(80));
  console.log('📊 ERROR SUMMARY\n');
  
  if (errorSummary.length > 0) {
    // Group errors by type
    const errorTypes = {};
    errorSummary.forEach(item => {
      if (!errorTypes[item.error]) {
        errorTypes[item.error] = [];
      }
      errorTypes[item.error].push(`${item.workflow} (${item.job})`);
    });
    
    console.log('Common error patterns found:\n');
    for (const [error, workflows] of Object.entries(errorTypes)) {
      console.log(`${error}:`);
      workflows.forEach(w => console.log(`  - ${w}`));
      console.log();
    }
  } else {
    console.log('No specific error patterns detected in logs.');
  }
  
  // Save detailed report
  const report = {
    timestamp: new Date().toISOString(),
    totalWorkflows: workflows.workflows.length,
    failedWorkflows: Object.keys(workflowGroups).filter(name => {
      const latest = workflowGroups[name][0];
      return latest.conclusion === 'failure';
    }),
    errorPatterns: errorSummary,
    recommendations: []
  };
  
  // Add recommendations based on errors
  if (errorSummary.some(e => e.error.includes('Maven'))) {
    report.recommendations.push('Check Maven configuration and MAVEN_BATCH_MODE usage');
  }
  if (errorSummary.some(e => e.error.includes('NPM'))) {
    report.recommendations.push('Review npm cache configuration and package-lock.json paths');
  }
  if (errorSummary.some(e => e.error.includes('Docker'))) {
    report.recommendations.push('Ensure Docker is properly set up in workflow');
  }
  
  fs.writeFileSync('workflow-error-report.json', JSON.stringify(report, null, 2));
  console.log('\nDetailed report saved to workflow-error-report.json');
  
} catch (error) {
  console.error('Error analyzing workflows:', error.message);
  console.log('\nMake sure you have GitHub CLI installed and authenticated.');
}