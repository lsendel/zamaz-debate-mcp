const { exec } = require('child_process');
const util = require('util');
const execAsync = util.promisify(exec);

async function testWorkflowHealthMonitor() {
  console.log('🧪 Testing Workflow Health Monitor...');
  
  try {
    // Trigger the debug workflow manually
    console.log('Triggering workflow health monitor debug...');
    const { stdout, stderr } = await execAsync('gh workflow run workflow-health-monitor-debug.yml -f test_mode=true');
    
    if (stderr && !stderr.includes('successfully queued')) {
      console.error('❌ Error triggering workflow:', stderr);
      return;
    }
    
    console.log('✅ Workflow triggered successfully!');
    console.log('Output:', stdout);
    
    // Wait a bit then check the workflow status
    console.log('⏳ Waiting 30 seconds for workflow to start...');
    await new Promise(resolve => setTimeout(resolve, 30000));
    
    // Check workflow runs
    console.log('📊 Checking workflow runs...');
    const { stdout: runsOutput } = await execAsync('gh run list --workflow=workflow-health-monitor-debug.yml --limit 5');
    console.log('Recent runs:');
    console.log(runsOutput);
    
    // Check for any new issues
    console.log('🔍 Checking for new issues...');
    const { stdout: issuesOutput } = await execAsync('gh issue list --label "workflow-health-test" --limit 5');
    console.log('Workflow health test issues:');
    console.log(issuesOutput);
    
    if (issuesOutput.trim()) {
      console.log('✅ SUCCESS: Issues were created by the workflow health monitor!');
    } else {
      console.log('⚠️  No test issues found yet. The workflow may still be running.');
      console.log('💡 Check https://github.com/lsendel/zamaz-debate-mcp/actions manually');
    }
    
  } catch (error) {
    console.error('❌ Error testing workflow:', error.message);
    console.log('💡 Make sure you have gh CLI installed and authenticated');
    console.log('💡 Run: gh auth login');
  }
}

if (require.main === module) {
  testWorkflowHealthMonitor();
}

module.exports = { testWorkflowHealthMonitor };