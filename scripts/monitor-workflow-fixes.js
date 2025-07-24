#!/usr/bin/env node

const { execSync } = require('child_process');

console.log('🔄 Monitoring workflow fixes in real-time...\n');

// Function to check workflow status
function checkWorkflowStatus() {
  try {
    const runs = JSON.parse(execSync('gh run list --limit 20 --json databaseId,name,conclusion,status,event,createdAt', { encoding: 'utf8' }));
    
    console.clear();
    console.log('GitHub Actions Status - ' + new Date().toLocaleTimeString());
    console.log('='.repeat(80));
    
    const statusEmoji = {
      'success': '✅',
      'failure': '❌',
      'cancelled': '⚫',
      'in_progress': '🔄',
      'queued': '⏳',
      'completed': '✅'
    };
    
    // Group by status
    const grouped = {
      running: [],
      failed: [],
      passed: [],
      other: []
    };
    
    runs.forEach(run => {
      const status = run.conclusion || run.status;
      if (status === 'in_progress' || status === 'queued') {
        grouped.running.push(run);
      } else if (status === 'failure') {
        grouped.failed.push(run);
      } else if (status === 'success') {
        grouped.passed.push(run);
      } else {
        grouped.other.push(run);
      }
    });
    
    // Show running workflows
    if (grouped.running.length > 0) {
      console.log('\n🏃 RUNNING:');
      grouped.running.forEach(run => {
        console.log(`  ${statusEmoji[run.status]} ${run.name}`);
      });
    }
    
    // Show failed workflows
    if (grouped.failed.length > 0) {
      console.log('\n❌ FAILED:');
      grouped.failed.forEach(run => {
        console.log(`  ${statusEmoji[run.conclusion]} ${run.name}`);
      });
    }
    
    // Show passed workflows
    if (grouped.passed.length > 0) {
      console.log('\n✅ PASSED:');
      grouped.passed.forEach(run => {
        console.log(`  ${statusEmoji[run.conclusion]} ${run.name}`);
      });
    }
    
    // Summary
    console.log('\n' + '─'.repeat(80));
    console.log(`Summary: ${grouped.running.length} running, ${grouped.failed.length} failed, ${grouped.passed.length} passed`);
    
  } catch (error) {
    console.error('Error fetching workflow status:', error.message);
  }
}

// Check every 5 seconds
setInterval(checkWorkflowStatus, 5000);
checkWorkflowStatus();