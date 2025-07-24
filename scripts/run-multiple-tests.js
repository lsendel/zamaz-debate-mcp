const { execSync } = require('child_process');

async function runMultipleTests() {
  console.log('🔄 Running 4 test iterations to verify stability...\n');
  
  const results = [];
  
  for (let i = 1; i <= 4; i++) {
    console.log(`\n🧪 Test iteration ${i}/4`);
    console.log('=' + '='.repeat(60));
    
    try {
      // Wait 30 seconds between tests
      if (i > 1) {
        console.log('⏳ Waiting 30 seconds before next test...');
        await new Promise(resolve => setTimeout(resolve, 30000));
      }
      
      // Run the verification
      const output = execSync('node verify-maven-fix.js', { 
        encoding: 'utf8',
        cwd: __dirname 
      });
      
      // Check if the test passed
      const passed = output.includes('SUCCESS! Maven class # error has been fixed!') &&
                    output.includes('CI Pipeline Summary error has been fixed!');
      
      results.push({
        iteration: i,
        passed: passed,
        hadMavenError: output.includes('MAVEN ERROR STILL PRESENT'),
        hadCIPipelineError: output.includes('CI Pipeline Summary error still present')
      });
      
      console.log(output);
      
    } catch (error) {
      console.error(`Error in iteration ${i}:`, error.message);
      results.push({
        iteration: i,
        passed: false,
        error: error.message
      });
    }
  }
  
  // Summary
  console.log('\n' + '='.repeat(80));
  console.log('📊 FINAL TEST SUMMARY');
  console.log('='.repeat(80));
  
  const passedCount = results.filter(r => r.passed).length;
  console.log(`\nTotal tests run: ${results.length}`);
  console.log(`Tests passed: ${passedCount}/${results.length}`);
  
  results.forEach(r => {
    if (r.passed) {
      console.log(`  ✅ Iteration ${r.iteration}: PASSED`);
    } else {
      console.log(`  ❌ Iteration ${r.iteration}: FAILED`);
      if (r.hadMavenError) console.log('     - Maven error detected');
      if (r.hadCIPipelineError) console.log('     - CI Pipeline error detected');
      if (r.error) console.log(`     - Error: ${r.error}`);
    }
  });
  
  if (passedCount === results.length) {
    console.log('\n🎉 ALL TESTS PASSED! The fixes are stable and working correctly.');
  } else {
    console.log('\n⚠️  Some tests failed. The fixes may need further investigation.');
  }
}

runMultipleTests().catch(console.error);