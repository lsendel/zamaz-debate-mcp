import { test, expect } from '@playwright/test';

test.describe('Debate UI Regression Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3001');
  });

  test('should load the homepage', async ({ page }) => {
    // Check main title
    await expect(page.locator('h1')).toContainText('AI Debate System');
    await expect(page.locator('text=Powered by MCP & Ollama')).toBeVisible();
  });

  test('should show all main UI sections', async ({ page }) => {
    // Check header buttons
    await expect(page.getByRole('button', { name: 'Test LLM' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'New Debate' })).toBeVisible();
    
    // Check quick action cards
    await expect(page.locator('text=Start New Debate')).toBeVisible();
    await expect(page.locator('text=Continue Active')).toBeVisible();
    await expect(page.locator('text=View History')).toBeVisible();
    await expect(page.locator('text=Manage Teams')).toBeVisible();
    
    // Check stats cards
    await expect(page.locator('text=Total Debates')).toBeVisible();
    await expect(page.locator('text=Active')).toBeVisible();
    await expect(page.locator('text=Completed')).toBeVisible();
    await expect(page.locator('text=AI Models')).toBeVisible();
  });

  test('should have all tabs', async ({ page }) => {
    // Check tab navigation
    await expect(page.getByRole('tab', { name: 'Debates' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Gallery' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Library' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Active' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Setup' })).toBeVisible();
  });

  test('should show connection status', async ({ page }) => {
    // Check for disconnected status (since backend is not running)
    await expect(page.locator('text=Disconnected')).toBeVisible();
  });

  test('should open create debate dialog', async ({ page }) => {
    // Click New Debate button
    await page.getByRole('button', { name: 'New Debate' }).click();
    
    // Check dialog appears
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.locator('text=Create New Debate')).toBeVisible();
    
    // Take screenshot
    await page.screenshot({ path: 'playwright-tests/screenshots/create-debate-dialog.png' });
  });

  test('should open LLM test dialog', async ({ page }) => {
    // Click Test LLM button
    await page.getByRole('button', { name: 'Test LLM' }).click();
    
    // Check dialog appears
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.locator('text=Test LLM Connection')).toBeVisible();
    
    // Take screenshot
    await page.screenshot({ path: 'playwright-tests/screenshots/llm-test-dialog.png' });
  });

  test('should navigate between tabs', async ({ page }) => {
    // Click Templates tab
    await page.getByRole('tab', { name: 'Gallery' }).click();
    await expect(page.locator('text=Debate Templates')).toBeVisible();
    
    // Click Library tab
    await page.getByRole('tab', { name: 'Library' }).click();
    await expect(page.locator('text=Template Library')).toBeVisible();
    
    // Click Setup tab
    await page.getByRole('tab', { name: 'Setup' }).click();
    await expect(page.locator('text=LLM Service Health')).toBeVisible();
    
    // Take screenshots
    await page.screenshot({ path: 'playwright-tests/screenshots/setup-tab.png' });
  });

  test('should show organization switcher', async ({ page }) => {
    // Check if organization switcher skeleton is visible (loading state)
    const orgSwitcher = page.locator('.h-10.w-48.bg-gray-200');
    await expect(orgSwitcher).toBeVisible();
    
    // Take screenshot
    await page.screenshot({ path: 'playwright-tests/screenshots/org-switcher-loading.png' });
  });

  test('should handle keyboard shortcuts', async ({ page }) => {
    // Press Ctrl+N to open new debate
    await page.keyboard.press('Control+n');
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.locator('text=Create New Debate')).toBeVisible();
    
    // Close dialog
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).not.toBeVisible();
  });

  test('should show loading state for debates', async ({ page }) => {
    // Check loading spinner in debates tab
    await expect(page.locator('text=Loading debates...')).toBeVisible();
    
    // Take screenshot
    await page.screenshot({ path: 'playwright-tests/screenshots/debates-loading.png', fullPage: true });
  });

  test('check create debate form fields', async ({ page }) => {
    await page.getByRole('button', { name: 'New Debate' }).click();
    
    // Check form fields
    await expect(page.getByLabel('Debate Name')).toBeVisible();
    await expect(page.getByLabel('Topic')).toBeVisible();
    await expect(page.getByLabel('Description (Optional)')).toBeVisible();
    
    // Check debate rules section
    await expect(page.locator('text=Debate Rules')).toBeVisible();
    await expect(page.getByLabel('Format')).toBeVisible();
    await expect(page.getByLabel('Max Rounds')).toBeVisible();
    
    // Check participants section
    await expect(page.locator('text=Participants')).toBeVisible();
    
    // Take screenshot
    await page.screenshot({ path: 'playwright-tests/screenshots/create-debate-form.png', fullPage: true });
  });

  test('check notification bell', async ({ page }) => {
    // Check notification bell is visible
    const notificationBell = page.locator('button[aria-haspopup="dialog"]').first();
    await expect(notificationBell).toBeVisible();
    
    // Click notification bell
    await notificationBell.click();
    
    // Check if notification panel opens
    await page.waitForTimeout(500);
    await page.screenshot({ path: 'playwright-tests/screenshots/notifications.png' });
  });

  test('check responsive design', async ({ page }) => {
    // Test mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await page.screenshot({ path: 'playwright-tests/screenshots/mobile-view.png', fullPage: true });
    
    // Test tablet viewport
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.screenshot({ path: 'playwright-tests/screenshots/tablet-view.png', fullPage: true });
    
    // Test desktop viewport
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.screenshot({ path: 'playwright-tests/screenshots/desktop-view.png', fullPage: true });
  });

  test('check dark mode support', async ({ page }) => {
    // Emulate dark mode
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.screenshot({ path: 'playwright-tests/screenshots/dark-mode.png', fullPage: true });
    
    // Emulate light mode
    await page.emulateMedia({ colorScheme: 'light' });
    await page.screenshot({ path: 'playwright-tests/screenshots/light-mode.png', fullPage: true });
  });
});