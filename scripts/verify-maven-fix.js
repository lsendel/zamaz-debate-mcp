const { chromium } = require('@playwright/test');

async function verifyMavenFix() {
  console.log('🔍 Verifying Maven fixes in recent runs...\n');
  
  const browser = await chromium.launch({ 
    headless: true
  });
  
  const page = await browser.newContext().then(ctx => ctx.newPage());
  
  try {
    // Get latest commit
    const { execSync } = require('child_process');
    const commitSha = execSync('git rev-parse HEAD', { encoding: 'utf8' }).trim();
    
    await page.goto('https://github.com/lsendel/zamaz-debate-mcp/actions', { 
      waitUntil: 'networkidle' 
    });
    
    await page.waitForTimeout(3000);
    
    // Find runs from our commit
    const runLinks = await page.locator('a[href*="/actions/runs/"]').all();
    const ourRuns = [];
    
    for (const link of runLinks) {
      const parent = await link.locator('../..').first();
      const parentText = await parent.textContent();
      
      if (parentText.includes(commitSha.substring(0, 7))) {
        const href = await link.getAttribute('href');
        const name = await link.textContent();
        ourRuns.push({ 
          url: `https://github.com${href}`, 
          name: name.trim() 
        });
      }
    }
    
    console.log(`Found ${ourRuns.length} runs from commit ${commitSha.substring(0, 7)}\n`);
    
    let mavenErrorFound = false;
    let ciPipelineErrorFound = false;
    
    // Check each run for Maven errors
    for (const run of ourRuns.slice(0, 5)) {
      console.log(`Checking ${run.name}...`);
      await page.goto(run.url, { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(2000);
      
      const pageText = await page.textContent('body');
      
      // Check for Maven class not found error
      if (pageText.includes('Could not find or load main class #')) {
        console.log('   ❌ MAVEN ERROR STILL PRESENT!');
        mavenErrorFound = true;
      } else if (pageText.includes('mvn validate')) {
        console.log('   ✅ Maven commands running without # error');
      }
      
      // Check for CI Pipeline Summary error
      if (pageText.includes('CI Pipeline Summary') && pageText.includes('exit code 1')) {
        console.log('   ❌ CI Pipeline Summary error still present!');
        ciPipelineErrorFound = true;
      }
      
      // Check for any failures
      if (pageText.includes('failure') || pageText.includes('failed')) {
        console.log('   ⚠️  Some failures detected (checking details...)');
        
        // Look for job links to get more details
        const jobLinks = await page.locator('a[href*="/job/"]').all();
        for (const jobLink of jobLinks.slice(0, 3)) {
          const jobName = await jobLink.textContent();
          const parent = await jobLink.locator('..').first();
          const hasError = await parent.locator('.octicon-x').count() > 0;
          
          if (hasError) {
            console.log(`      Failed job: ${jobName.trim()}`);
          }
        }
      } else {
        console.log('   ✅ No failures detected');
      }
    }
    
    console.log('\n' + '='.repeat(60));
    console.log('VERIFICATION SUMMARY:');
    console.log('='.repeat(60));
    
    if (!mavenErrorFound && !ciPipelineErrorFound) {
      console.log('✅ SUCCESS! Maven class # error has been fixed!');
      console.log('✅ CI Pipeline Summary error has been fixed!');
      console.log('\nAll critical errors appear to be resolved.');
    } else {
      if (mavenErrorFound) {
        console.log('❌ Maven class # error is STILL PRESENT');
      }
      if (ciPipelineErrorFound) {
        console.log('❌ CI Pipeline Summary error is STILL PRESENT');
      }
      console.log('\nSome errors persist. Further investigation needed.');
    }
    
  } finally {
    await browser.close();
  }
}

verifyMavenFix().catch(console.error);