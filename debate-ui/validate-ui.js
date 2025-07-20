const puppeteer = require('puppeteer');
const path = require('path');

async function validateUI() {
  console.log('Starting UI validation...');
  const browser = await puppeteer.launch({ 
    headless: false,
    defaultViewport: { width: 1280, height: 800 }
  });
  
  const page = await browser.newPage();
  const findings = [];
  
  // Monitor console messages
  page.on('console', msg => {
    if (msg.type() === 'error') {
      findings.push(`Console Error: ${msg.text()}`);
    }
  });

  try {
    // 1. Login Page
    console.log('\n1. Validating Login Page...');
    await page.goto('http://localhost:3001/login', { waitUntil: 'networkidle0' });
    await page.screenshot({ path: 'validation-screenshots/01-login.png' });
    
    // Check for Ant Design components
    const loginFormExists = await page.$('.ant-form') !== null;
    const tabsExist = await page.$('.ant-tabs') !== null;
    const inputsExist = await page.$('.ant-input') !== null;
    
    console.log(`  - Ant Form: ${loginFormExists ? '✅' : '❌'}`);
    console.log(`  - Ant Tabs: ${tabsExist ? '✅' : '❌'}`);
    console.log(`  - Ant Input: ${inputsExist ? '✅' : '❌'}`);
    
    // Login with demo credentials
    await page.type('input[type="text"]', 'demo');
    await page.type('input[type="password"]', 'demo123');
    await page.screenshot({ path: 'validation-screenshots/02-login-filled.png' });
    
    // Click login button
    await page.click('button[type="submit"]');
    await page.waitForNavigation({ waitUntil: 'networkidle0' });
    
    // 2. Main Layout & Debates Page
    console.log('\n2. Validating Main Layout & Debates Page...');
    await page.screenshot({ path: 'validation-screenshots/03-debates.png' });
    
    const layoutExists = await page.$('.ant-layout') !== null;
    const siderExists = await page.$('.ant-layout-sider') !== null;
    const menuExists = await page.$('.ant-menu') !== null;
    
    console.log(`  - Ant Layout: ${layoutExists ? '✅' : '❌'}`);
    console.log(`  - Ant Sider: ${siderExists ? '✅' : '❌'}`);
    console.log(`  - Ant Menu: ${menuExists ? '✅' : '❌'}`);
    
    // Check for any Tailwind classes
    const tailwindClasses = await page.evaluate(() => {
      const elements = document.querySelectorAll('*');
      const tailwindFound = [];
      elements.forEach(el => {
        const classes = el.className.split(' ');
        classes.forEach(cls => {
          if (cls && (cls.includes('flex') || cls.includes('grid') || cls.includes('text-') || 
              cls.includes('bg-') || cls.includes('p-') || cls.includes('m-'))) {
            tailwindFound.push(cls);
          }
        });
      });
      return [...new Set(tailwindFound)];
    });
    
    if (tailwindClasses.length > 0) {
      console.log(`  - Found Tailwind classes: ${tailwindClasses.slice(0, 5).join(', ')}...`);
      findings.push(`Tailwind classes found: ${tailwindClasses.join(', ')}`);
    }
    
    // 3. Create Debate Dialog
    console.log('\n3. Validating Create Debate Dialog...');
    const createButton = await page.$('button:has-text("Create Debate")');
    if (createButton) {
      await createButton.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ path: 'validation-screenshots/04-create-debate.png' });
      
      const modalExists = await page.$('.ant-modal') !== null;
      const formExists = await page.$('.ant-modal .ant-form') !== null;
      
      console.log(`  - Ant Modal: ${modalExists ? '✅' : '❌'}`);
      console.log(`  - Ant Form in Modal: ${formExists ? '✅' : '❌'}`);
      
      // Close modal
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);
    }
    
    // 4. Navigate to other pages
    const pagesToCheck = [
      { name: 'Workflow Editor', url: '/workflow', selector: 'a[href="/workflow"]' },
      { name: 'Analytics', url: '/analytics', selector: 'a[href="/analytics"]' },
      { name: 'Organizations', url: '/organizations', selector: 'a[href="/organizations"]' },
      { name: 'Settings', url: '/settings', selector: 'a[href="/settings"]' }
    ];
    
    for (const pageInfo of pagesToCheck) {
      console.log(`\n4. Validating ${pageInfo.name}...`);
      const link = await page.$(pageInfo.selector);
      if (link) {
        await link.click();
        await page.waitForTimeout(1000);
        await page.screenshot({ 
          path: `validation-screenshots/0${5 + pagesToCheck.indexOf(pageInfo)}-${pageInfo.name.toLowerCase().replace(' ', '-')}.png` 
        });
        
        // Check for Ant Design components
        const antComponents = await page.evaluate(() => {
          const components = [];
          if (document.querySelector('.ant-card')) components.push('Card');
          if (document.querySelector('.ant-table')) components.push('Table');
          if (document.querySelector('.ant-form')) components.push('Form');
          if (document.querySelector('.ant-progress')) components.push('Progress');
          if (document.querySelector('.ant-badge')) components.push('Badge');
          if (document.querySelector('.ant-select')) components.push('Select');
          return components;
        });
        
        console.log(`  - Ant Design components found: ${antComponents.join(', ') || 'None'}`);
      }
    }
    
    // Summary
    console.log('\n=== Validation Summary ===');
    console.log(`Total findings: ${findings.length}`);
    findings.forEach(f => console.log(`- ${f}`));
    
  } catch (error) {
    console.error('Validation error:', error);
  } finally {
    await browser.close();
  }
}

// Create screenshots directory
const fs = require('fs');
if (!fs.existsSync('validation-screenshots')) {
  fs.mkdirSync('validation-screenshots');
}

validateUI();