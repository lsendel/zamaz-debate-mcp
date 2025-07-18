import { test, expect } from '@playwright/test';

test.describe('Simple UI Screenshot Capture', () => {
  test('Capture working UI screenshots', async ({ page }) => {
    // Configure for better reliability
    page.setDefaultTimeout(10000);
    
    // Homepage
    await page.goto('http://localhost:3000');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000); // Give time for React to render
    await page.screenshot({ 
      path: 'screenshots/01-homepage-working.png', 
      fullPage: true 
    });

    // Try to identify what pages exist by checking what's loaded
    const pageTitle = await page.title();
    console.log('Page title:', pageTitle);
    
    // Check for common React app elements
    const appElement = page.locator('#root, #app, .app').first();
    const hasApp = await appElement.isVisible().catch(() => false);
    console.log('Has app element:', hasApp);
    
    if (hasApp) {
      await appElement.screenshot({ path: 'screenshots/02-app-container.png' });
    }

    // Check for navigation elements
    const navElements = await page.locator('nav, header, .nav, .navbar, [role="navigation"]').count();
    console.log('Navigation elements found:', navElements);
    
    if (navElements > 0) {
      await page.locator('nav, header, .nav, .navbar, [role="navigation"]').first()
        .screenshot({ path: 'screenshots/03-navigation.png' });
    }

    // Check for main content
    const mainContent = page.locator('main, .main, .content, [role="main"]').first();
    const hasMain = await mainContent.isVisible().catch(() => false);
    console.log('Has main content:', hasMain);
    
    if (hasMain) {
      await mainContent.screenshot({ path: 'screenshots/04-main-content.png' });
    }

    // Try clicking around to see different states
    const buttons = await page.locator('button').count();
    console.log('Buttons found:', buttons);
    
    if (buttons > 0) {
      await page.screenshot({ path: 'screenshots/05-before-interaction.png', fullPage: true });
      
      // Try clicking the first button
      try {
        await page.locator('button').first().click();
        await page.waitForTimeout(1000);
        await page.screenshot({ path: 'screenshots/06-after-button-click.png', fullPage: true });
      } catch (e) {
        console.log('Could not click button:', e.message);
      }
    }

    // Try different viewport sizes
    await page.setViewportSize({ width: 768, height: 1024 }); // Tablet
    await page.screenshot({ path: 'screenshots/07-tablet-view.png', fullPage: true });
    
    await page.setViewportSize({ width: 375, height: 667 }); // Mobile
    await page.screenshot({ path: 'screenshots/08-mobile-view.png', fullPage: true });

    // Back to desktop
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.screenshot({ path: 'screenshots/09-desktop-final.png', fullPage: true });

    console.log('Screenshots captured successfully!');
  });
});