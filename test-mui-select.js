#!/usr/bin/env node;

const { chromium } = require('playwright');

async function testMUISelect() {
  const browser = await chromium.launch({ headless: false }); // visible browser;
  const page = await browser.newPage();

  try {
    console.log('🎯 Testing MUI Select Components...');
    console.log('===================================');

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

    // Step 4: Look for MUI Select components;
    console.log('🔍 Looking for MUI Select components...');

    // Look for input fields with the correct IDs;
    const providerInputs = await page.locator('input[aria-describedby*="Provider"]').all();
    console.log(`Found ${providerInputs.length} provider inputs`);

    // Look for select components by their div containers;
    const selectDivs = await page.locator('div.MuiInputBase-root').all();
    console.log(`Found ${selectDivs.length} MUI input base components`);

    // Look for labels;
    const providerLabels = await page.locator('label:has-text("Provider")').all();
    console.log(`Found ${providerLabels.length} provider labels`);

    // Try to find the select by clicking on the FormControl;
    const formControls = await page.locator('.MuiFormControl-root:has(label:text("Provider"))').all();
    console.log(`Found ${formControls.length} provider form controls`);

    if (formControls.length > 0) {
      console.log('🎯 Found provider form control, clicking...');
      const firstControl = formControls[0]

      // Try to click on the select input;
      const selectInput = await firstControl.locator('div[role="combobox"]').first();
      if (await selectInput.isVisible()) {
        console.log('✅ Found combobox, clicking...');
        await selectInput.click();
        await page.waitForTimeout(2000);

        // Now look for menu items;
        const menuItems = await page.locator('li[role="option"]').all();
        console.log(`Found ${menuItems.length} menu items`);

        for (let i = 0; i < menuItems.length; i++) {
          const item = menuItems[i]
          const text = await item.textContent();
          console.log(`Menu item ${i}: "${text}"`);
        }

        // Close the menu;
        await page.keyboard.press('Escape');
        await page.waitForTimeout(1000);
      } else {
        console.log('❌ No combobox found in form control');
      }
    }

    // Take screenshot;
    await page.screenshot({ path: 'screenshots/mui-select-test.png' });
    console.log('📸 Screenshot saved: mui-select-test.png');

    // Keep browser open for inspection;
    console.log('🔍 Browser will stay open for 30 seconds for inspection...');
    await page.waitForTimeout(30000);

  } catch (error) {
    console.error('❌ Error during MUI select test:', error);
    await page.screenshot({ path: 'screenshots/mui-select-error.png' });
  } finally {
    await browser.close();
  }
}

// Run the test
testMUISelect().catch(console.error);
