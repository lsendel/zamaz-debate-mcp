const { chromium } = require('@playwright/test');
const fs = require('fs').promises;
const path = require('path');

async function quickAnalyze() {
  console.log('🚀 Quick Maven Error Analysis...\n');
  
  const browser = await chromium.launch({ 
    headless: true,  // Run in headless mode for speed
    timeout: 30000
  });
  
  const page = await browser.newContext().then(ctx => ctx.newPage());
  
  // Specific runs to check
  const runs = [
    'https://github.com/lsendel/zamaz-debate-mcp/actions/runs/16488838408',
    'https://github.com/lsendel/zamaz-debate-mcp/actions/runs/16488838359'
  ];
  
  const findings = [];
  
  for (const runUrl of runs) {
    console.log(`Checking ${runUrl}...`);
    
    try {
      await page.goto(runUrl, { waitUntil: 'domcontentloaded', timeout: 30000 });
      await page.waitForTimeout(2000);
      
      // Get page text
      const pageText = await page.textContent('body');
      
      // Check for Maven error
      if (pageText.includes('Could not find or load main class #')) {
        console.log('   ❌ Found Maven class # error!');
        
        // Try to find the exact location
        const jobLinks = await page.locator('a[href*="/job/"]').all();
        
        for (const jobLink of jobLinks.slice(0, 5)) { // Check first 5 jobs
          const jobUrl = await jobLink.getAttribute('href');
          const jobName = await jobLink.textContent();
          
          await page.goto(`https://github.com${jobUrl}`, { waitUntil: 'domcontentloaded' });
          const jobText = await page.textContent('body');
          
          if (jobText.includes('Could not find or load main class #')) {
            // Find the exact Maven command
            const lines = jobText.split('\n');
            for (let i = 0; i < lines.length; i++) {
              if (lines[i].includes('Run mvn')) {
                const mvnCommand = lines[i];
                if (lines[i + 1] && lines[i + 1].includes('Could not find or load main class #')) {
                  findings.push({
                    run: runUrl,
                    job: jobName.trim(),
                    command: mvnCommand,
                    error: 'Maven trying to execute class #'
                  });
                  console.log(`   Found in job: ${jobName.trim()}`);
                  console.log(`   Command: ${mvnCommand}`);
                  break;
                }
              }
            }
          }
        }
      }
      
      // Check for echo error
      if (pageText.includes('echo "## 🏗️ CI Pipeline Summary"')) {
        console.log('   ❌ Found CI Pipeline Summary echo error!');
        findings.push({
          run: runUrl,
          error: 'CI Pipeline Summary echo failed'
        });
      }
      
    } catch (error) {
      console.error(`   Error checking ${runUrl}:`, error.message);
    }
  }
  
  await browser.close();
  
  console.log('\n' + '='.repeat(60));
  console.log('FINDINGS:');
  console.log('='.repeat(60));
  
  if (findings.length === 0) {
    console.log('No Maven class # errors found in the specified runs.');
  } else {
    findings.forEach(f => {
      console.log(`\nRun: ${f.run}`);
      if (f.job) console.log(`Job: ${f.job}`);
      if (f.command) console.log(`Command: ${f.command}`);
      console.log(`Error: ${f.error}`);
    });
  }
  
  // Now check the workflows for the root cause
  console.log('\n' + '='.repeat(60));
  console.log('CHECKING WORKFLOW FILES FOR ROOT CAUSE:');
  console.log('='.repeat(60));
  
  // The most likely cause is MAVEN_BATCH_MODE containing a # character
  const { execSync } = require('child_process');
  
  try {
    const result = execSync('grep -n "MAVEN_BATCH_MODE" .github/workflows/*.yml', { encoding: 'utf8' });
    console.log('\nMAVEN_BATCH_MODE definitions found:');
    console.log(result);
    
    // Check if any contain # character
    if (result.includes('#')) {
      console.log('\n⚠️  WARNING: Found # character in MAVEN_BATCH_MODE!');
      console.log('This is likely causing the "Could not find or load main class #" error.');
    }
  } catch (e) {
    console.log('No MAVEN_BATCH_MODE found or error searching.');
  }
}

quickAnalyze().catch(console.error);