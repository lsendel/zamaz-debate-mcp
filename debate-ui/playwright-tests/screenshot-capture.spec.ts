import { test } from '@playwright/test';

test.describe('UI Screenshot Capture', () => {
  test('capture all UI states', async ({ page }) => {
    // Navigate to homepage
    await page.goto('http://localhost:3001');
    await page.waitForTimeout(2000);
    
    // 1. Homepage
    await page.screenshot({ 
      path: 'playwright-tests/screenshots/1-homepage.png', 
      fullPage: true 
    });
    
    // 2. Test LLM Dialog
    const testLLMButton = page.getByRole('button', { name: 'Test LLM' });
    if (await testLLMButton.isVisible()) {
      await testLLMButton.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ 
        path: 'playwright-tests/screenshots/2-llm-test-dialog.png', 
        fullPage: true 
      });
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);
    }
    
    // 3. Create Debate Dialog
    const newDebateButton = page.getByRole('button', { name: 'New Debate' });
    if (await newDebateButton.isVisible()) {
      await newDebateButton.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ 
        path: 'playwright-tests/screenshots/3-create-debate-dialog.png', 
        fullPage: true 
      });
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);
    }
    
    // 4. Gallery Tab
    const galleryTab = page.getByRole('tab', { name: 'Gallery' });
    if (await galleryTab.isVisible()) {
      await galleryTab.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ 
        path: 'playwright-tests/screenshots/4-gallery-tab.png', 
        fullPage: true 
      });
    }
    
    // 5. Library Tab
    const libraryTab = page.getByRole('tab', { name: 'Library' });
    if (await libraryTab.isVisible()) {
      await libraryTab.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ 
        path: 'playwright-tests/screenshots/5-library-tab.png', 
        fullPage: true 
      });
    }
    
    // 6. Setup Tab
    const setupTab = page.getByRole('tab', { name: 'Setup' });
    if (await setupTab.isVisible()) {
      await setupTab.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ 
        path: 'playwright-tests/screenshots/6-setup-tab.png', 
        fullPage: true 
      });
    }
    
    // 7. Mobile View
    await page.setViewportSize({ width: 375, height: 667 });
    await page.waitForTimeout(1000);
    await page.screenshot({ 
      path: 'playwright-tests/screenshots/7-mobile-view.png', 
      fullPage: true 
    });
    
    // 8. Dark Mode
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.waitForTimeout(1000);
    await page.screenshot({ 
      path: 'playwright-tests/screenshots/8-dark-mode.png', 
      fullPage: true 
    });
  });
});