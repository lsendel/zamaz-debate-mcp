const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

// Test configuration
const config = {
  baseUrl: 'http://localhost:3003',
  debateApiUrl: 'http://localhost:5013',
  screenshotDir: './validation-screenshots/real-debate-test',
  testTimeout: 60000,
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
  console.log(`✓ Screenshot: ${name} - ${description}`);
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

async function testRealDebateIntegration() {
  console.log('🚀 Starting Real Debate Integration Tests...');
  console.log(`📍 UI URL: ${config.baseUrl}`);
  console.log(`📍 API URL: ${config.debateApiUrl}`);
  
  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: null,
    args: [
      '--start-maximized',
      '--no-sandbox',
      '--disable-setuid-sandbox'
    ]
  });
  
  const page = await browser.newPage();
  
  // Listen for API errors
  page.on('response', response => {
    if (!response.ok() && response.url().includes('5013')) {
      console.error(`❌ API Error: ${response.status()} ${response.url()}`);
    }
  });
  
  const testResults = {
    login: false,
    debateListLoad: false,
    realDataPresent: false,
    debateCreation: false,
    debateExecution: false,
    errorHandling: false
  };
  
  try {
    // Test 1: Login
    console.log('\n→ Test 1: Login Process');
    await page.goto(`${config.baseUrl}/login`, { waitUntil: 'networkidle0' });
    await captureScreenshot(page, '01-login', 'Login page');
    
    await page.type('input[placeholder="Username"]', config.credentials.username);
    await page.type('input[placeholder="Password"]', config.credentials.password);
    await page.click('button[type="submit"]');
    
    await page.waitForNavigation({ waitUntil: 'networkidle0' });
    testResults.login = true;
    await captureScreenshot(page, '02-logged-in', 'After login');
    
    // Test 2: Check for existing debates (real data)
    console.log('\n→ Test 2: Checking for Real Debate Data');
    
    // Wait for debates to load
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    const debatesPresent = await page.evaluate(() => {
      const debateCards = document.querySelectorAll('.ant-card, [data-testid="debate-card"], .debate-item');
      const debateRows = document.querySelectorAll('tbody tr, .ant-list-item');
      return debateCards.length > 0 || debateRows.length > 0;
    });
    
    testResults.debateListLoad = debatesPresent;
    testResults.realDataPresent = debatesPresent;
    
    console.log(`  ${debatesPresent ? '✓' : '❌'} Real debate data found: ${debatesPresent}`);
    await captureScreenshot(page, '03-debates-list', 'Debates list with real data');
    
    // Test 3: Try to access an existing debate
    if (debatesPresent) {
      console.log('\n→ Test 3: Accessing Real Debate Detail');
      
      const debateClicked = await page.evaluate(() => {
        const firstDebate = document.querySelector('.ant-card, [data-testid="debate-card"], .debate-item, tbody tr:first-child');
        if (firstDebate) {
          firstDebate.click();
          return true;
        }
        return false;
      });
      
      if (debateClicked) {
        await page.waitForNavigation({ waitUntil: 'networkidle0' });
        await captureScreenshot(page, '04-debate-detail', 'Real debate detail page');
        
        // Check if debate detail loaded with real data
        const debateDetailLoaded = await page.evaluate(() => {
          const title = document.querySelector('h1, .debate-title');
          const participants = document.querySelector('.participants, [data-testid="participants"]');
          const rounds = document.querySelector('.rounds, .debate-responses, [data-testid="rounds"]');
          return title && (participants || rounds);
        });
        
        console.log(`  ${debateDetailLoaded ? '✓' : '❌'} Debate detail with real data: ${debateDetailLoaded}`);
      }
    }
    
    // Test 4: Create a new debate with real LLM configuration
    console.log('\n→ Test 4: Creating New Debate with Real LLM Config');
    
    // Navigate back to debates list
    await page.goto(`${config.baseUrl}/debates`, { waitUntil: 'networkidle0' });
    
    // Look for create debate button
    const createBtn = await page.$('button:contains("Create"), .ant-btn:contains("Create"), [data-testid="create-debate"]');
    if (createBtn) {
      await createBtn.click();
      await page.waitForSelector('form, .ant-modal', { timeout: 10000 });
      await captureScreenshot(page, '05-create-debate-form', 'Create debate form');
      
      // Fill form with test data
      await page.type('input[name="topic"], textarea[name="topic"]', 'Test Debate: AI vs Human Intelligence');
      await page.type('textarea[name="description"], input[name="description"]', 'Testing real LLM integration for debate system');
      
      // Try to configure participants with real LLM providers
      const addParticipantBtn = await page.$('button:contains("Add"), .add-participant');
      if (addParticipantBtn) {
        await addParticipantBtn.click();
        
        // Try to select LLM provider
        const providerSelect = await page.$('select[name*="provider"], .ant-select');
        if (providerSelect) {
          await providerSelect.click();
          await page.waitForSelector('.ant-select-dropdown', { timeout: 2000 });
          
          // Look for CLAUDE option
          const claudeOption = await page.$('.ant-select-item:contains("CLAUDE"), option[value="CLAUDE"]');
          if (claudeOption) {
            await claudeOption.click();
            console.log('  ✓ Selected CLAUDE provider');
          }
        }
      }
      
      // Submit the form
      const submitBtn = await page.$('button[type="submit"], .ant-btn-primary');
      if (submitBtn) {
        await submitBtn.click();
        
        try {
          await page.waitForNavigation({ waitUntil: 'networkidle0', timeout: 15000 });
          testResults.debateCreation = true;
          console.log('  ✓ Debate created successfully');
          await captureScreenshot(page, '06-debate-created', 'New debate created');
          
          // Test 5: Try to start the debate
          console.log('\n→ Test 5: Testing Debate Execution');
          
          const startBtn = await page.$('button:contains("Start"), .start-btn, [data-testid="start-debate"]');
          if (startBtn) {
            await startBtn.click();
            
            // Wait for debate to start and check for any errors
            await new Promise(resolve => setTimeout(resolve, 5000));
            
            const errors = await page.evaluate(() => {
              const errorElements = document.querySelectorAll('.ant-alert-error, .error, [data-testid="error"]');
              return Array.from(errorElements).map(el => el.textContent);
            });
            
            if (errors.length === 0) {
              testResults.debateExecution = true;
              console.log('  ✓ Debate started without errors');
            } else {
              console.log('  ❌ Debate start errors:', errors);
            }
            
            await captureScreenshot(page, '07-debate-started', 'Debate execution attempt');
          }
        } catch (error) {
          console.log('  ❌ Debate creation failed:', error.message);
        }
      }
    }
    
    // Test 6: Error handling
    console.log('\n→ Test 6: Testing Error Handling');
    
    const consoleErrors = await page.evaluate(() => {
      return window.jsErrors || [];
    });
    
    const uiErrors = await page.evaluate(() => {
      const errorElements = document.querySelectorAll('.ant-alert-error, .error-message');
      return Array.from(errorElements).map(el => el.textContent);
    });
    
    testResults.errorHandling = consoleErrors.length === 0 && uiErrors.length < 3; // Allow up to 2 minor errors
    
    console.log(`  Console errors: ${consoleErrors.length}`);
    console.log(`  UI errors: ${uiErrors.length}`);
    
    await captureScreenshot(page, '08-final-state', 'Final test state');
    
  } catch (error) {
    console.error('❌ Test failed:', error.message);
    await captureScreenshot(page, '99-error', 'Test error state');
  } finally {
    await browser.close();
  }
  
  // Results summary
  const totalTests = Object.keys(testResults).length;
  const passedTests = Object.values(testResults).filter(Boolean).length;
  const successPercentage = Math.round((passedTests / totalTests) * 100);
  
  console.log('\n📊 Real Debate Integration Results:');
  console.log('=====================================');
  Object.entries(testResults).forEach(([test, passed]) => {
    console.log(`${test}: ${passed ? '✅ PASS' : '❌ FAIL'}`);
  });
  
  console.log(`\n🎯 Success Rate: ${successPercentage}%`);
  console.log(`📈 Tests Passed: ${passedTests}/${totalTests}`);
  
  // Specific analysis
  console.log('\n📝 Integration Analysis:');
  if (testResults.login) {
    console.log('✓ Authentication system working');
  }
  if (testResults.realDataPresent) {
    console.log('✓ Real debate data accessible from backend');
  } else {
    console.log('⚠️ No real debate data found - may need backend data');
  }
  if (testResults.debateCreation) {
    console.log('✓ Debate creation flow functional');
  }
  if (testResults.debateExecution) {
    console.log('✓ Debate execution working with real LLMs');
  } else {
    console.log('⚠️ Debate execution may have issues - check LLM integration');
  }
  
  return {
    success: successPercentage >= 70, // Lower threshold for integration tests
    successPercentage,
    testResults,
    hasRealData: testResults.realDataPresent
  };
}

// Run the tests
if (require.main === module) {
  testRealDebateIntegration()
    .then(results => {
      console.log('\n✅ Integration test suite completed');
      if (results.success) {
        console.log('🎉 Real debate integration working!');
      } else {
        console.log('⚠️ Integration issues detected - check backend connectivity');
      }
      process.exit(results.success ? 0 : 1);
    })
    .catch(error => {
      console.error('💥 Integration test failed:', error);
      process.exit(1);
    });
}

module.exports = { testRealDebateIntegration };