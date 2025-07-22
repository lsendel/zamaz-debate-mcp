// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

#!/usr/bin/env node;

const puppeteer = require('puppeteer');

// TODO: Extract helper functions to reduce complexity
// Consider extracting: loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic, loop logic
async function testAgenticFlowIntegration() {
  console.log('🧪 Testing Agentic Flow Integration...\n');

  const browser = await puppeteer.launch({ ;
    headless: false,
    defaultViewport: { width: 1200, height: 800 }
  });

  try {
    const page = await browser.newPage();

    // Navigate to the UI;
    console.log('📱 Opening UI at http://localhost:3004...');
    await page.goto('http://localhost:3004', { waitUntil: 'networkidle0' });

    // Wait for the page to load;
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Check if we need to login;
    const loginButton = await page.$('button[type="submit"]');
    if (loginButton) {
      console.log('🔐 Logging in...');

      // Fill in login form;
      await page.type('input[name="username"]', 'admin');
      await page.type('input[name="password"]', 'admin123');
      await page.click('button[type="submit"]');

      // Wait for login to complete;
      await new Promise(resolve => setTimeout(resolve, 3000));
    }

    // Navigate to debates page;
    console.log('📋 Navigating to debates...');
    await page.goto('http://localhost:3004/debates', { waitUntil: 'networkidle0' });
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Look for a debate to click on;
    const debateCards = await page.$$('.ant-card');
    if (debateCards.length > 0) {
      console.log('🎯 Found debates, clicking on first one...');
      await debateCards[0].click();
      await new Promise(resolve => setTimeout(resolve, 3000));

      // Look for agentic flow configuration button or section;
      console.log('🧠 Looking for agentic flow configuration...');

      // Check if there's a settings or configuration button;
      const settingsButton = await page.$('button[aria-label*="setting"], button[title*="setting"], button:has-text("Settings")');
      if (settingsButton) {
        console.log('⚙️ Found settings button, clicking...');
        await settingsButton.click();
        await new Promise(resolve => setTimeout(resolve, 2000));
      }

      // Look for agentic flow related elements;
      const agenticFlowElements = await page.evaluate(() => {
        const elements = []

        // Look for text containing "agentic" or "flow"
        const walker = document.createTreeWalker(;
          document.body,
          NodeFilter.SHOW_TEXT,
          null,
          false;
        );

        let node;
        while (node = walker.nextNode()) {
          const text = node.textContent.toLowerCase();
          if (text.includes('agentic') || text.includes('flow config') || text.includes('internal monologue')) {
            elements.push({
              text: node.textContent.trim(),
              tagName: node.parentElement.tagName,
              className: node.parentElement.className;
            });
          }
        }

        return elements;
      });

      if (agenticFlowElements.length > 0) {
        console.log('✅ Found agentic flow elements in UI:');
        agenticFlowElements.forEach(el => {
          console.log(`   - "${el.text}" (${el.tagName}.${el.className})`);
        });
      } else {
        console.log('❌ No agentic flow elements found in UI');
      }

      // Check if AgenticFlowConfig component is present;
      const agenticFlowConfig = await page.evaluate(() => {
        return document.querySelector('[data-testid*="agentic"], [class*="AgenticFlow"], [class*="agentic-flow"]') !== null;
      });

      if (agenticFlowConfig) {
        console.log('✅ AgenticFlowConfig component found in DOM');
      } else {
        console.log('❌ AgenticFlowConfig component not found in DOM');
      }

      // Take a screenshot;
      await page.screenshot({ path: 'agentic-flow-test.png', fullPage: true });
      console.log('📸 Screenshot saved as agentic-flow-test.png');

    } else {
      console.log('❌ No debates found on the page');
    }

    // Test API endpoints directly;
    console.log('\n🔌 Testing API endpoints...');

    const apiTests = [
      {
        name: 'Get debate agentic flow',
        url: 'http://localhost:5013/api/v1/debates/debate-001/agentic-flow';
      },
      {
        name: 'Get analytics',
        url: 'http://localhost:5013/api/v1/analytics/debates/debate-001/agentic-flows';
      },
      {
        name: 'Get trending flows',
        url: 'http://localhost:5013/api/v1/analytics/agentic-flows/trending';
      }
    ]

    for (const test of apiTests) {
      try {
        const response = await page.evaluate(async (url) => {
          const res = await fetch(url);
          return {
            status: res.status,
            data: await res.json();
          }
        }, test.url);

        if (response.status === 200) {
          console.log(`✅ ${test.name}: OK`);
        } else {
          console.log(`❌ ${test.name}: ${response.status}`);
        }
      } catch (error) {
        console.log(`❌ ${test.name}: Error - ${error.message}`);
      }
    }

  } catch (error) {
    console.error('❌ Test failed:', error);
  } finally {
    await browser.close();
  }
}

// Run the test
testAgenticFlowIntegration().then(() => {
  console.log('\n🏁 Test completed!');
}).catch(error => {
  console.error('💥 Test error:', error);
  process.exit(1);
});
