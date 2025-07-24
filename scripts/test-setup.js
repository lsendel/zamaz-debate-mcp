// Quick test to validate the setup before running the full analysis
const fs = require('fs').promises;
const path = require('path');

async function testSetup() {
  console.log('Testing GitHub Actions Analysis Setup...\n');
  
  const checks = [];
  
  // Check Node.js version
  const nodeVersion = process.version;
  console.log(`✓ Node.js version: ${nodeVersion}`);
  checks.push({ name: 'Node.js', status: 'OK', details: nodeVersion });
  
  // Check if main script exists
  const scriptPath = path.join(__dirname, 'analyze-github-actions-failures.js');
  try {
    await fs.access(scriptPath);
    console.log('✓ Main analysis script exists');
    checks.push({ name: 'Analysis Script', status: 'OK', details: scriptPath });
  } catch (error) {
    console.log('✗ Main analysis script missing');
    checks.push({ name: 'Analysis Script', status: 'MISSING', details: error.message });
  }
  
  // Check if package.json exists
  const packagePath = path.join(__dirname, 'package.json');
  try {
    await fs.access(packagePath);
    const packageData = JSON.parse(await fs.readFile(packagePath, 'utf8'));
    console.log('✓ Package.json exists');
    console.log(`  Dependencies: ${Object.keys(packageData.dependencies || {}).join(', ')}`);
    checks.push({ name: 'Package.json', status: 'OK', details: `Dependencies: ${Object.keys(packageData.dependencies || {}).length}` });
  } catch (error) {
    console.log('✗ Package.json missing or invalid');
    checks.push({ name: 'Package.json', status: 'ERROR', details: error.message });
  }
  
  // Check if run script exists
  const runScriptPath = path.join(__dirname, 'run-analysis.sh');
  try {
    await fs.access(runScriptPath);
    console.log('✓ Run script exists and is executable');
    checks.push({ name: 'Run Script', status: 'OK', details: runScriptPath });
  } catch (error) {
    console.log('✗ Run script missing');
    checks.push({ name: 'Run Script', status: 'MISSING', details: error.message });
  }
  
  // Test internet connectivity (basic check)
  try {
    const https = require('https');
    await new Promise((resolve, reject) => {
      const req = https.get('https://github.com', { timeout: 5000 }, (res) => {
        console.log('✓ Internet connectivity to GitHub');
        checks.push({ name: 'GitHub Connectivity', status: 'OK', details: `Status: ${res.statusCode}` });
        resolve();
      });
      req.on('error', reject);
      req.on('timeout', () => reject(new Error('Timeout')));
    });
  } catch (error) {
    console.log('✗ Internet connectivity issue');
    checks.push({ name: 'GitHub Connectivity', status: 'ERROR', details: error.message });
  }
  
  console.log('\n=== Setup Status ===');
  const allOk = checks.every(check => check.status === 'OK');
  
  if (allOk) {
    console.log('🎉 All checks passed! Ready to run the analysis.');
    console.log('\nTo run the analysis:');
    console.log('  ./run-analysis.sh');
    console.log('\nOr manually:');
    console.log('  npm install');
    console.log('  npx playwright install chromium');
    console.log('  node analyze-github-actions-failures.js');
  } else {
    console.log('⚠️  Some issues found. Please address them before running the analysis.');
    console.log('\nIssues:');
    checks.filter(check => check.status !== 'OK').forEach(check => {
      console.log(`  - ${check.name}: ${check.status} - ${check.details}`);
    });
  }
  
  return allOk;
}

testSetup().catch(console.error);