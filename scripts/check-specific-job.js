const { chromium } = require('@playwright/test');

async function checkSpecificJob() {
  console.log('🔍 Checking specific job for Maven error...\n');
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 300
  });
  
  const page = await browser.newContext().then(ctx => ctx.newPage());
  
  // The specific job URL from the user
  const jobUrl = 'https://github.com/lsendel/zamaz-debate-mcp/actions/runs/16488838359/job/46618932096';
  
  console.log(`Opening: ${jobUrl}`);
  await page.goto(jobUrl, { waitUntil: 'networkidle' });
  await page.waitForTimeout(3000);
  
  // Take screenshot
  await page.screenshot({ path: 'maven-error-screenshot.png', fullPage: true });
  console.log('Screenshot saved: maven-error-screenshot.png');
  
  // Get the text content
  const pageText = await page.textContent('body');
  
  // Look for Maven commands and errors
  const lines = pageText.split('\n');
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].includes('Run mvn')) {
      console.log(`\nFound Maven command at line ${i}:`);
      console.log(lines[i]);
      
      // Check next few lines for error
      for (let j = 1; j <= 5; j++) {
        if (lines[i + j]) {
          console.log(`  +${j}: ${lines[i + j].substring(0, 100)}`);
          if (lines[i + j].includes('Could not find or load main class')) {
            console.log('\n❌ FOUND ERROR!');
          }
        }
      }
    }
  }
  
  // Look for environment variables
  if (pageText.includes('MAVEN_')) {
    console.log('\n📋 Maven-related environment variables found in output');
  }
  
  await browser.close();
}

checkSpecificJob().catch(console.error);