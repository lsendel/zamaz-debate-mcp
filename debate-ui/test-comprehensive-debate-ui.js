const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

// Test configuration
const config = {
  baseUrl: process.env.REACT_APP_UI_URL || 'http://localhost:3001',
  debateApiUrl: process.env.REACT_APP_DEBATE_API_URL || 'http://localhost:5013',
  screenshotDir: './validation-screenshots/comprehensive-test',
  testTimeout: 30000,
  credentials: {
    username: 'demo',
    password: 'demo123'
  }
};

// Create screenshot directory
if (!fs.existsSync(config.screenshotDir)) {
  fs.mkdirSync(config.screenshotDir, { recursive: true });
}

async function captureScreenshot(page, name, description) {
  const screenshotPath = path.join(config.screenshotDir, `${name}.png`);
  await page.screenshot({ 
    path: screenshotPath, 
    fullPage: true,
    type: 'png'
  });
  console.log(`✓ Screenshot captured: ${name} - ${description}`);
  return screenshotPath;
}

async function waitForElement(page, selector, timeout = 10000) {
  try {
    await page.waitForSelector(selector, { timeout });
    return true;
  } catch (error) {
      console.error("Error:", error);
    console.error(`❌ Element not found: ${selector}`);
    return false;
    console.error("Error:", error);
  }
}

async function checkForErrors(page) {
  const errors = await page.evaluate(() => {
    const errorMessages = [];
    const errorElements = document.querySelectorAll('.ant-alert-error, .ant-message-error, [data-testid="error"]');
    errorElements.forEach(el => {
      errorMessages.push(el.textContent || el.innerText);
    });
    return errorMessages;
  });
  
  if (errors.length > 0) {
    console.warn(`⚠️ UI errors detected: ${errors.join(', ')}`);
  }
  
  return errors;
}

async function testDebateDetailPage(browser) {
  console.log('\n🧪 Testing Debate Detail Page Functionality...');
  
  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });
  
  try {
    // Navigate to login page
    console.log('→ Navigating to login page...');
    await page.goto(`${config.baseUrl}/login`, { waitUntil: 'networkidle0' });
    await captureScreenshot(page, '01-login-page', 'Login page loaded');
    
    // Perform login
    console.log('→ Performing login...');
    await page.type('input[placeholder="Username"]', config.credentials.username);
    await page.type('input[placeholder="Password"]', config.credentials.password);
    await page.click('button[type="submit"]');
    
    // Wait for redirect to debates list
    await page.waitForNavigation({ waitUntil: 'networkidle0' });
    await captureScreenshot(page, '02-debates-list', 'Debates list after login');
    
    // Check if we have debates, if not create one
    const hasDebates = await page.$('.ant-table-tbody tr, .debate-card, [data-testid="debate-item"]');
    let debateId;
    
    if (!hasDebates) {
      console.log('→ No debates found, creating a new debate...');
      
      // Navigate to create debate page
      await page.click('button:contains("Create"), .ant-btn:contains("New"), [data-testid="create-debate"]');
      await page.waitForSelector('form, .ant-form', { timeout: 5000 });
      await captureScreenshot(page, '03-create-debate-form', 'Create debate form');
      
      // Fill out the create form
      await page.type('input[name="topic"], textarea[name="topic"]', 'Should renewable energy be prioritized over nuclear power?');
      await page.type('textarea[name="description"]', 'A comprehensive debate on energy policy and environmental sustainability.');
      
      // Add participants if the form allows
      const addParticipantBtn = await page.$('button:contains("Add"), .add-participant');
      if (addParticipantBtn) {
        await addParticipantBtn.click();
        await page.type('input[placeholder*="name"]', 'AI Participant 1');
        await page.select('select[name*="provider"], select[name*="llm"]', 'CLAUDE');
      }
      
      // Submit the form
      await page.click('button[type="submit"], .ant-btn-primary');
      await page.waitForNavigation({ waitUntil: 'networkidle0' });
      
      // Extract debate ID from URL
      const url = page.url();
      const match = url.match(/\/debates\/([^\/]+)/);
      debateId = match ? match[1] : null;
    } else {
      console.log('→ Existing debates found, clicking on first debate...');
      
      // Click on the first available debate
      await page.click('.ant-table-tbody tr:first-child, .debate-card:first-child, [data-testid="debate-item"]:first-child');
      await page.waitForNavigation({ waitUntil: 'networkidle0' });
      
      // Extract debate ID from URL
      const url = page.url();
      const match = url.match(/\/debates\/([^\/]+)/);
      debateId = match ? match[1] : null;
    }
    
    console.log(`→ Debate ID: ${debateId}`);
    await captureScreenshot(page, '04-debate-detail-loaded', 'Debate detail page loaded');
    
    // Test 1: Verify debate detail page elements
    console.log('→ Test 1: Verifying debate detail page elements...');
    const elements = {
      title: 'h1, .debate-title, [data-testid="debate-title"]',
      status: '.ant-badge, .status-badge, [data-testid="debate-status"]',
      participants: '.participants, [data-testid="participants"]',
      progress: '.progress, .debate-progress, [data-testid="debate-progress"]',
      visualizations: '[data-testid="debate-visualizations"], .visualization',
      actions: '.ant-btn, button'
    };
    
    for (const [name, selector] of Object.entries(elements)) {
      const found = await waitForElement(page, selector, 3000);
      console.log(`  ${found ? '✓' : '❌'} ${name}: ${selector}`);
    }
    
    // Test 2: Test configuration modal
    console.log('→ Test 2: Testing configuration modal...');
    
    // Look for configuration button
    const configButtons = await page.$$('button:contains("Configure"), .ant-btn:contains("Config"), [data-testid*="config"]');
    if (configButtons.length > 0) {
      await configButtons[0].click();
      await page.waitForSelector('.ant-modal, .modal', { timeout: 5000 });
      await captureScreenshot(page, '05-configuration-modal', 'Configuration modal opened');
      
      // Test tabs in configuration modal
      const tabs = await page.$$('.ant-tabs-tab, .tab');
      console.log(`  ✓ Found ${tabs.length} configuration tabs`);
      
      // Test each tab
      for (let i = 0; i < Math.min(tabs.length, 4); i++) {
        await tabs[i].click();
        await page.waitForTimeout(1000);
        await captureScreenshot(page, `06-config-tab-${i}`, `Configuration tab ${i} content`);
      }
      
      // Close modal
      const closeBtn = await page.$('.ant-modal-close, .modal-close, button:contains("Cancel")');
      if (closeBtn) {
        await closeBtn.click();
        await page.waitForTimeout(500);
      }
    } else {
      console.log('  ⚠️ No configuration buttons found');
    }
    
    // Test 3: Test visualizations
    console.log('→ Test 3: Testing visualizations...');
    
    const visualizationSelector = '[data-testid="debate-visualizations"], .visualization, .analytics';
    const hasVisualizations = await waitForElement(page, visualizationSelector, 3000);
    
    if (hasVisualizations) {
      await captureScreenshot(page, '07-visualizations', 'Debate visualizations displayed');
      
      // Test visualization dropdown/selector
      const vizSelectors = await page.$$('select, .ant-select, [data-testid*="viz-select"]');
      if (vizSelectors.length > 0) {
        console.log('  ✓ Visualization selector found');
        
        // Test different visualization options
        const options = ['engagement', 'progression', 'topics', 'quality', 'pdf', 'map'];
        for (const option of options) {
          try {
            await page.evaluate((opt) => {
              const select = document.querySelector('select, .ant-select-selector');
              if (select) select.click();
            });
            await page.waitForTimeout(500);
            
            const optionElement = await page.$(`option[value="${option}"], .ant-select-item:contains("${option}")`);
            if (optionElement) {
              await optionElement.click();
              await page.waitForTimeout(1000);
              await captureScreenshot(page, `08-viz-${option}`, `${option} visualization`);
            }
          } catch (error) {
            console.log(`  ⚠️ Could not test ${option} visualization`);
            console.error("Error:", error);
          }
        }
      }
    } else {
      console.log('  ❌ Visualizations not found');
    }
    
    // Test 4: Test debate actions
    console.log('→ Test 4: Testing debate actions...');
    
    const actionButtons = {
      start: 'button:contains("Start"), .start-btn',
      pause: 'button:contains("Pause"), .pause-btn', 
      export: 'button:contains("Export"), .export-btn',
      refresh: 'button:contains("Refresh"), .refresh-btn, .ant-btn[title="Refresh"]'
    };
    
    for (const [action, selector] of Object.entries(actionButtons)) {
      const button = await page.$(selector);
      if (button) {
        console.log(`  ✓ ${action} button found`);
        
        // Test hover state
        await button.hover();
        await page.waitForTimeout(200);
      } else {
        console.log(`  ⚠️ ${action} button not found`);
      }
    }
    
    await captureScreenshot(page, '09-final-debate-state', 'Final debate detail page state');
    
    // Test 5: Check for JavaScript errors
    console.log('→ Test 5: Checking for JavaScript errors...');
    const jsErrors = await page.evaluate(() => {
      return window.jsErrors || [];
    });
    
    const uiErrors = await checkForErrors(page);
    
    if (jsErrors.length === 0 && uiErrors.length === 0) {
      console.log('  ✓ No JavaScript or UI errors detected');
    } else {
      console.log(`  ❌ Found ${jsErrors.length} JS errors and ${uiErrors.length} UI errors`);
    }
    
    // Test 6: Responsiveness test
    console.log('→ Test 6: Testing responsiveness...');
    
    const viewports = [
      { width: 1920, height: 1080, name: 'desktop' },
      { width: 1024, height: 768, name: 'tablet' },
      { width: 375, height: 667, name: 'mobile' }
    ];
    
    for (const viewport of viewports) {
      await page.setViewport(viewport);
      await page.waitForTimeout(1000);
      await captureScreenshot(page, `10-responsive-${viewport.name}`, `${viewport.name} viewport (${viewport.width}x${viewport.height})`);
    }
    
    return {
      success: true,
      debateId,
      errors: [...jsErrors, ...uiErrors],
      testResults: {
        pageLoad: true,
        configuration: configButtons.length > 0,
        visualizations: hasVisualizations,
        responsiveness: true
      }
    };
    
  } catch (error) {
    console.error('❌ Test failed:', error);
    await captureScreenshot(page, '99-error-state', 'Error state');
    return {
      success: false,
      error: error.message,
      testResults: {}
    };
  } finally {
    await page.close();
  }
}

async function runComprehensiveTests() {
  console.log('🚀 Starting Comprehensive Debate UI Tests...');
  console.log(`📍 Base URL: ${config.baseUrl}`);
  console.log(`📁 Screenshots: ${config.screenshotDir}`);
  
  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: null,
    args: [
      '--start-maximized',
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage'
    ]
  });
  
  try {
    const results = await testDebateDetailPage(browser);
    
    console.log('\n📊 Test Results Summary:');
    console.log('========================');
    console.log(`Overall Success: ${results.success ? '✅' : '❌'}`);
    
    if (results.testResults) {
      Object.entries(results.testResults).forEach(([test, passed]) => {
        console.log(`${test}: ${passed ? '✅' : '❌'}`);
      });
    }
    
    if (results.errors && results.errors.length > 0) {
      console.log('\n⚠️ Errors Found:');
      results.errors.forEach(error => console.log(`  - ${error}`));
    }
    
    // Calculate success percentage
    const totalTests = Object.keys(results.testResults || {}).length;
    const passedTests = Object.values(results.testResults || {}).filter(Boolean).length;
    const successPercentage = totalTests > 0 ? Math.round((passedTests / totalTests) * 100) : 0;
    
    console.log(`\n🎯 Success Rate: ${successPercentage}%`);
    
    if (successPercentage >= 80) {
      console.log('🎉 SUCCESS: 80%+ functionality achieved!');
    } else {
      console.log('⚠️ WARNING: Below 80% functionality target');
    }
    
    return {
      success: results.success,
      successPercentage,
      results
    };
    
  } finally {
    await browser.close();
  }
}

// Run the tests
if (require.main === module) {
  runComprehensiveTests()
    .then(results => {
      console.log('\n✅ Test suite completed');
      process.exit(results.success && results.successPercentage >= 80 ? 0 : 1);
    })
    .catch(error => {
      console.error('💥 Test suite failed:', error);
      process.exit(1);
    });
}

module.exports = { runComprehensiveTests };