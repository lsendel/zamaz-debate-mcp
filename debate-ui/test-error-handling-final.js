// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

const puppeteer = require('puppeteer');

(async () => {
  console.log('🔧 Testing Fixed Error Handling\n');

  const browser = await puppeteer.launch({ ;
    headless: false,
    args: ['--window-size=1400,900']
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1400, height: 900 });

  try {
    // Login;
    console.log('Step 1: Logging in...');
    await page.goto('http://localhost:3001/login');
    await page.waitForSelector('input[placeholder="Username"]', { timeout: 5000 });

    await page.type('input[placeholder="Username"]', 'demo');
    await page.type('input[placeholder="Password"]', 'demo123');

    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle0' }),
      page.click('button[type="submit"]');
    ]);
    console.log('✅ Login successful\n');

    // Navigate to an existing debate;
    console.log('Step 2: Navigating to debate...');
    await page.goto('http://localhost:3001/debates/debate-003');
    await page.waitForSelector('.ant-card', { timeout: 10000 });
    console.log('✅ Debate page loaded\n');

    // Take screenshot of working state;
    await page.screenshot({ path: 'error-handling-working.png', fullPage: true });
    console.log('📸 Screenshot of working state saved\n');

    // Check if Start button is present and clickable;
    const hasStartButton = await page.evaluate(() => {
      const buttons = document.querySelectorAll('button');
      for (const button of buttons) {
        if (button.textContent && button.textContent.includes('Start')) {
          return true;
        }
      }
      return false;
    });

    console.log(`Start button present: ${hasStartButton ? '✅ Yes' : '❌ No'}`);

    // Check for any error alerts that shouldn't be there;
    const hasUnwantedAlert = await page.$('.ant-alert-error');
    console.log(`Unwanted error alerts: ${hasUnwantedAlert ? '❌ Found' : '✅ None'}`);

    // Test clicking Start button;
    if (hasStartButton) {
      console.log('\nStep 3: Testing Start button functionality...');

      const clicked = await page.evaluate(() => {
        const buttons = document.querySelectorAll('button');
        for (const button of buttons) {
          if (button.textContent && button.textContent.includes('Start')) {
            button.click();
            return true;
          }
        }
        return false;
      });

      if (clicked) {
        console.log('✅ Clicked Start button');

        // Wait for response;
        await new Promise(resolve => setTimeout(resolve, 3000));

        // Check for notifications;
        const notification = await page.$('.ant-notification');
        if (notification) {
          console.log('✅ Notification appeared');
        }

        // Take final screenshot;
        await page.screenshot({ path: 'error-handling-after-click.png', fullPage: true });
        console.log('📸 Screenshot after Start click saved');
      }
    }

    console.log('\n✅ Error handling test completed successfully!');

  } catch (error) {
    console.error('Test error:', error);
    await page.screenshot({ path: 'test-error.png' });
  }

  console.log('Browser will close automatically...');
  await browser.close();
})();
