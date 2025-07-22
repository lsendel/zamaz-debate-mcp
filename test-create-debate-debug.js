#!/usr/bin/env node;

const { chromium } = require('playwright');

async function testCreateDebateDebug() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  try {
    console.log('🐛 Debugging Create Debate Dialog...');
    console.log('====================================');

    // Listen for console messages;
    page.on('console', msg => {
      console.log(`[BROWSER] ${msg.type()}: ${msg.text()}`);
    });

    // Listen for network responses;
    page.on('response', response => {
      if (response.url().includes('/api/v1/providers')) {
        console.log(`[NETWORK] ${response.url()} - Status: ${response.status()}`);
      }
    });

    // Step 1: Login;
    console.log('📝 Logging in...');
    await page.goto('http://localhost:3001/login');
    await page.locator('input[type="text"]').fill('admin');
    await page.locator('input[type="password"]').fill('password');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(2000);

    // Step 2: Navigate to debates;
    console.log('🏛️ Navigating to debates...');
    await page.goto('http://localhost:3001/debates');
    await page.waitForTimeout(2000);

    // Step 3: Open Create Debate dialog;
    console.log('➕ Opening Create Debate dialog...');
    await page.locator('button:has-text("Create Debate")').click();
    await page.waitForTimeout(3000);

    // Step 4: Wait for providers to load;
    console.log('⏳ Waiting for providers to load...');
    await page.waitForTimeout(5000);

    // Step 5: Check provider dropdown;
    console.log('🔍 Checking provider dropdown...');

    // Get all provider select elements;
    const providerSelects = await page.locator('div[role="button"][aria-haspopup="listbox"]').all();
    console.log(`Found ${providerSelects.length} dropdown elements`);

    for (let i = 0; i < providerSelects.length; i++) {
      const select = providerSelects[i]
      const isVisible = await select.isVisible();
      const text = await select.textContent();
      console.log(`Dropdown ${i}: visible=${isVisible}, text="${text}"`);

      if (isVisible && text && text.includes('Provider')) {
        console.log('🎯 Found provider dropdown, clicking...');
        await select.click();
        await page.waitForTimeout(2000);

        // Check for options;
        const options = await page.locator('[role="option"]').all();
        console.log(`Found ${options.length} options`);

        for (let j = 0; j < options.length; j++) {
          const option = options[j]
          const optionText = await option.textContent();
          console.log(`Option ${j}: "${optionText}"`);
        }

        // Close dropdown;
        await page.keyboard.press('Escape');
        await page.waitForTimeout(1000);
        break;
      }
    }

    // Step 6: Check if data is loaded in component state;
    console.log('🔍 Checking component state...');

    const providersData = await page.evaluate(() => {
      // Try to access React DevTools or component state;
      const reactFiber = document.querySelector('#root')._reactInternalInstance;
      if (reactFiber) {
        return 'React fiber found';
      }
      return 'No React fiber found';
    });

    console.log('🔍 React state:', providersData);

  } catch (error) {
    console.error('❌ Error during debug test:', error);
    await page.screenshot({ path: 'screenshots/debug-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testCreateDebateDebug().catch(console.error);
