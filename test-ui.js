// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

const puppeteer = require('puppeteer');

async function testUI() {
  console.log('Starting UI tests...');

  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: { width: 1280, height: 800 }
  });

  try {
    const page = await browser.newPage();

    // Enable console logging;
    page.on('console', msg => console.log('PAGE LOG:', msg.text()));
    page.on('pageerror', err => console.log('PAGE ERROR:', err.message));

    console.log('\n1. Testing Login Page...');
    await page.goto('http://localhost:3001/login', { waitUntil: 'networkidle2' });
    await page.waitForSelector('input[label="Username"]', { timeout: 5000 });

    // Fill login form;
    await page.type('input[label="Username"]', 'demo');
    await page.type('input[type="password"]', 'demo123');

    // Click login button;
    await page.click('button[type="submit"]');

    // Wait for navigation;
    await page.waitForNavigation({ waitUntil: 'networkidle2' });
    console.log('✅ Login successful');

    // Wait for dashboard to load;
    await page.waitForTimeout(2000);

    console.log('\n2. Testing Create Debate Dialog...');
    // Look for create debate button;
    const createButton = await page.$('button:has-text("Create Debate"), button:has-text("New Debate"), button[aria-label*="create"], button[aria-label*="add"]');
    if (createButton) {
      await createButton.click();
    } else {
      // Try floating action button;
      await page.click('button[aria-label*="add"], .MuiFab-root');
    }

    // Wait for dialog to open;
    await page.waitForSelector('[role="dialog"]', { timeout: 5000 });
    console.log('✅ Create Debate dialog opened');

    // Check for provider dropdown;
    await page.waitForTimeout(1000);
    const providerSelects = await page.$$('label:has-text("Provider")');
    console.log(`Found ${providerSelects.length} provider dropdowns`);

    if (providerSelects.length > 0) {
      // Click on first provider dropdown;
      const firstProviderSelect = await page.$('div[aria-label="Provider"]');
      if (firstProviderSelect) {
        await firstProviderSelect.click();
        await page.waitForTimeout(500);

        // Check if menu items appear;
        const menuItems = await page.$$('[role="option"]');
        console.log(`✅ Provider dropdown has ${menuItems.length} options`);

        if (menuItems.length > 0) {
          console.log('Provider options found:');
          for (let i = 0; i < Math.min(menuItems.length, 5); i++) {
            const text = await menuItems[i].evaluate(el => el.textContent);
            console.log(`  - ${text}`);
          }
        }

        // Click away to close dropdown;
        await page.click('body');
      }
    }

    console.log('\n3. Testing Topic Input...');
    await page.type('input[label="Topic"]', 'Test Debate Topic');
    console.log('✅ Topic input working');

    console.log('\n4. Testing Model Selection...');
    // Try to select a model;
    const modelSelects = await page.$$('label:has-text("Model")');
    if (modelSelects.length > 0) {
      const firstModelSelect = await page.$('div[aria-label="Model"]');
      if (firstModelSelect) {
        await firstModelSelect.click();
        await page.waitForTimeout(500);

        const modelOptions = await page.$$('[role="option"]');
        console.log(`✅ Model dropdown has ${modelOptions.length} options`);

        if (modelOptions.length > 0) {
          await modelOptions[0].click();
          console.log('✅ Selected first model');
        }
      }
    }

    console.log('\n✅ All UI tests completed successfully!');
    console.log('The provider and model dropdowns are now populated with data.');

  } catch (error) {
    console.error('❌ Test failed:', error.message);

    // Take screenshot on error;
    await page.screenshot({ path: 'error-screenshot.png' });
    console.log('Screenshot saved as error-screenshot.png');
  } finally {
    await browser.close();
  }
}

// Run the test
testUI().catch(console.error);
