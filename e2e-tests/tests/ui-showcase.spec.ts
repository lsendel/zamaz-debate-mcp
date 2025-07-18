import { test, expect, Page } from '@playwright/test';

test.describe('UI Showcase - Complete Application Flow', () => {
  test('Capture screenshots of all UI functionality', async ({ page }) => {
    // Configure longer timeouts for UI navigation
    test.setTimeout(120000);
    
    try {
      // Step 1: Homepage/Landing page
      await page.goto('http://localhost:3000');
      await page.waitForLoadState('networkidle', { timeout: 10000 });
      await page.screenshot({ path: 'screenshots/01-homepage.png', fullPage: true });

      // Step 2: Try login page
      try {
        await page.goto('http://localhost:3000/login');
        await page.waitForLoadState('networkidle', { timeout: 5000 });
        await page.screenshot({ path: 'screenshots/02-login-page.png', fullPage: true });
      } catch (e) {
        console.log('Login page not accessible:', e.message);
      }

      // Step 3: Try debates listing
      try {
        await page.goto('http://localhost:3000/debates');
        await page.waitForLoadState('networkidle', { timeout: 5000 });
        await page.screenshot({ path: 'screenshots/03-debates-listing.png', fullPage: true });
      } catch (e) {
        console.log('Debates listing not accessible:', e.message);
      }

      // Step 4: Try settings page
      try {
        await page.goto('http://localhost:3000/settings');
        await page.waitForLoadState('networkidle', { timeout: 5000 });
        await page.screenshot({ path: 'screenshots/04-settings-page.png', fullPage: true });
      } catch (e) {
        console.log('Settings page not accessible:', e.message);
      }

      // Step 5: Try create debate page
      try {
        await page.goto('http://localhost:3000/debates/create');
        await page.waitForLoadState('networkidle', { timeout: 5000 });
        await page.screenshot({ path: 'screenshots/05-create-debate.png', fullPage: true });
      } catch (e) {
        console.log('Create debate page not accessible:', e.message);
      }

      // Step 6: Try analytics page
      try {
        await page.goto('http://localhost:3000/analytics');
        await page.waitForLoadState('networkidle', { timeout: 5000 });
        await page.screenshot({ path: 'screenshots/06-analytics.png', fullPage: true });
      } catch (e) {
        console.log('Analytics page not accessible:', e.message);
      }

      // Step 7: Navigation menu interactions (if available)
      await page.goto('http://localhost:3000');
      await page.waitForLoadState('networkidle', { timeout: 10000 });
      
      // Try to interact with navigation menu
      const menuButton = page.locator('[data-testid="menu-button"], .menu-button, [aria-label*="menu"], button[class*="menu"]').first();
      if (await menuButton.isVisible()) {
        await menuButton.click();
        await page.waitForTimeout(1000);
        await page.screenshot({ path: 'screenshots/07-navigation-menu-open.png', fullPage: true });
      }

      // Step 8: Mobile/responsive view
      await page.setViewportSize({ width: 375, height: 667 }); // iPhone viewport
      await page.goto('http://localhost:3000');
      await page.waitForLoadState('networkidle', { timeout: 10000 });
      await page.screenshot({ path: 'screenshots/08-mobile-homepage.png', fullPage: true });

      // Reset to desktop view
      await page.setViewportSize({ width: 1280, height: 720 });

      // Step 9: Error states (try invalid routes)
      try {
        await page.goto('http://localhost:3000/invalid-route');
        await page.waitForLoadState('networkidle', { timeout: 5000 });
        await page.screenshot({ path: 'screenshots/09-404-error.png', fullPage: true });
      } catch (e) {
        console.log('404 page not accessible:', e.message);
      }

      // Step 10: Application shell/layout
      await page.goto('http://localhost:3000');
      await page.waitForLoadState('networkidle', { timeout: 10000 });
      
      // Capture just the main application shell
      const appContainer = page.locator('#root, #app, .app, main, [role="main"]').first();
      if (await appContainer.isVisible()) {
        await appContainer.screenshot({ path: 'screenshots/10-app-shell.png' });
      }

    } catch (error) {
      console.error('Error during UI showcase:', error);
      // Take a final screenshot of whatever is currently displayed
      await page.screenshot({ path: 'screenshots/error-state.png', fullPage: true });
    }
  });

  test('Interactive UI element testing', async ({ page }) => {
    await page.goto('http://localhost:3000');
    await page.waitForLoadState('networkidle', { timeout: 10000 });
    
    // Take screenshot of initial state
    await page.screenshot({ path: 'screenshots/interactive-01-initial.png', fullPage: true });

    // Try to find and interact with common UI elements
    const commonSelectors = [
      'button',
      'input[type="text"]',
      'input[type="email"]',
      'select',
      'textarea',
      'a[href]',
      '[role="button"]',
      '[role="link"]'
    ];

    for (const selector of commonSelectors) {
      const elements = page.locator(selector);
      const count = await elements.count();
      if (count > 0) {
        console.log(`Found ${count} elements with selector: ${selector}`);
        await page.screenshot({ 
          path: `screenshots/interactive-elements-${selector.replace(/[^a-zA-Z0-9]/g, '_')}.png`, 
          fullPage: true 
        });
      }
    }
  });
});