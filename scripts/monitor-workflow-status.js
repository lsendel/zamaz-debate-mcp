#!/usr/bin/env node

const { execSync } = require('child_process');

console.log('🔍 Monitoring GitHub Actions workflow status...\n');

// Get recent workflow runs
try {
  const runs = execSync('gh run list --limit 10 --json conclusion,name,status,url,createdAt', { encoding: 'utf8' });
  const runData = JSON.parse(runs);
  
  console.log('Recent workflow runs:');
  console.log('═'.repeat(80));
  
  const statusEmoji = {
    'success': '✅',
    'failure': '❌',
    'cancelled': '⚫',
    'in_progress': '🔄',
    'queued': '⏳'
  };
  
  runData.forEach(run => {
    const emoji = statusEmoji[run.conclusion || run.status] || '❓';
    const status = run.conclusion || run.status;
    const time = new Date(run.createdAt).toLocaleString();
    
    console.log(`${emoji} ${run.name}`);
    console.log(`   Status: ${status}`);
    console.log(`   Time: ${time}`);
    console.log(`   URL: ${run.url}`);
    console.log('─'.repeat(80));
  });
  
  // Check for specific errors in recent failed runs
  const failedRuns = runData.filter(run => run.conclusion === 'failure');
  
  if (failedRuns.length > 0) {
    console.log('\n⚠️  Failed workflows detected!');
    console.log('Checking for common errors...\n');
    
    failedRuns.forEach(run => {
      console.log(`Checking ${run.name}...`);
      try {
        // Get run ID from URL
        const runId = run.url.split('/').pop();
        const jobs = execSync(`gh run view ${runId} --json jobs`, { encoding: 'utf8' });
        const jobData = JSON.parse(jobs);
        
        jobData.jobs.forEach(job => {
          if (job.conclusion === 'failure') {
            console.log(`  ❌ Failed job: ${job.name}`);
            
            // Check for specific errors
            const logs = execSync(`gh run view ${runId} --job ${job.id} --log 2>/dev/null || echo ""`, { encoding: 'utf8' });
            
            if (logs.includes('Could not find or load main class #')) {
              console.log('     🐛 Maven ClassNotFoundException detected!');
            }
            if (logs.includes('unable to cache dependencies')) {
              console.log('     🐛 NPM cache error detected!');
            }
            if (logs.includes('Process completed with exit code 1')) {
              console.log('     🐛 Process failed with exit code 1');
            }
          }
        });
      } catch (e) {
        console.log(`  ⚠️  Could not get details: ${e.message}`);
      }
    });
  } else {
    console.log('\n✅ All recent workflows completed successfully!');
  }
  
} catch (error) {
  console.error('Error fetching workflow runs:', error.message);
  console.log('\nMake sure you have GitHub CLI installed and authenticated.');
}