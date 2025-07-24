const { chromium } = require('@playwright/test');

async function waitAndCheckWorkflows(commitSha, waitMinutes = 2) {
  console.log(`⏳ Waiting ${waitMinutes} minutes for workflows to start...`);
  await new Promise(resolve => setTimeout(resolve, waitMinutes * 60 * 1000));
  
  console.log('\n🔍 Checking workflow runs...');
  
  const browser = await chromium.launch({ 
    headless: true
  });
  
  const page = await browser.newContext().then(ctx => ctx.newPage());
  
  try {
    await page.goto('https://github.com/lsendel/zamaz-debate-mcp/actions', { 
      waitUntil: 'networkidle' 
    });
    
    await page.waitForTimeout(3000);
    
    // Take screenshot
    await page.screenshot({ 
      path: `workflow-status-${Date.now()}.png`, 
      fullPage: true 
    });
    
    // Find runs from our commit
    const pageText = await page.textContent('body');
    
    if (pageText.includes(commitSha.substring(0, 7))) {
      console.log('✅ Found workflow runs from our commit!');
      
      // Check for any failures
      const runElements = await page.locator('a[href*="/actions/runs/"]').all();
      
      for (const element of runElements.slice(0, 10)) {
        const href = await element.getAttribute('href');
        const text = await element.textContent();
        const parent = await element.locator('../..').first();
        const parentText = await parent.textContent();
        
        if (parentText.includes(commitSha.substring(0, 7))) {
          const hasError = parentText.includes('failure') || 
                          await parent.locator('.octicon-x').count() > 0;
          const isInProgress = parentText.includes('in_progress') || 
                              await parent.locator('.octicon-dot-fill').count() > 0;
          
          if (hasError) {
            console.log(`   ❌ FAILED: ${text.trim()} - https://github.com${href}`);
          } else if (isInProgress) {
            console.log(`   🔄 IN PROGRESS: ${text.trim()}`);
          } else {
            console.log(`   ✅ SUCCESS: ${text.trim()}`);
          }
        }
      }
    } else {
      console.log('⏳ Workflows not started yet. May need to wait longer.');
    }
    
  } finally {
    await browser.close();
  }
}

// Get the latest commit SHA
const { execSync } = require('child_process');
const commitSha = execSync('git rev-parse HEAD', { encoding: 'utf8' }).trim();
console.log(`📋 Monitoring workflows for commit: ${commitSha}`);

waitAndCheckWorkflows(commitSha).catch(console.error);