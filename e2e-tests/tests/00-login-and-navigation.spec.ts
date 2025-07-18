import { test, expect } from '@playwright/test';

test.describe('Login and Navigation Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the app first to access localStorage
    await page.goto('/');
    
    // Clear any existing authentication
    await page.evaluate(() => {
      try {
        localStorage.clear();
        sessionStorage.clear();
      } catch (e) {
        // Ignore security errors
      }
    });
  });

  test('should login successfully and navigate to home page', async ({ page }) => {
    // Step 1: Navigate to login page
    await page.goto('/login');
    
    // Step 2: Verify login page loads
    await expect(page).toHaveTitle(/Zamaz Debate System/);
    await expect(page.locator('h1')).toContainText('Zamaz Debate System');
    
    // Step 3: Verify login form is present
    await expect(page.locator('input[type="text"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
    
    // Step 4: Fill in login credentials
    await page.locator('input[type="text"]').fill('demo');
    await page.locator('input[type="password"]').fill('demo123');
    
    // Step 5: Submit login form
    await page.locator('button[type="submit"]').click();
    
    // Step 6: Verify successful login redirect
    await expect(page).toHaveURL('/');
    
    // Step 7: Verify home page loads without errors
    await page.waitForTimeout(2000); // Wait for API calls to complete
    
    // Check that the page is not blank
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).toBeTruthy();
    expect(bodyText!.length).toBeGreaterThan(50);
    
    // Check for navigation elements
    await expect(page.locator('nav, [role="navigation"]')).toBeVisible();
    
    // Step 8: Verify no JavaScript errors
    const logs = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        logs.push(msg.text());
      }
    });
    
    // Wait a bit more to catch any delayed errors
    await page.waitForTimeout(1000);
    
    // Should not have any critical errors
    const criticalErrors = logs.filter(log => 
      log.includes('map is not a function') || 
      log.includes('Cannot read properties of undefined') ||
      log.includes('TypeError')
    );
    
    expect(criticalErrors).toHaveLength(0);
  });

  test('should navigate to debates page and load debates', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.locator('input[type="text"]').fill('demo');
    await page.locator('input[type="password"]').fill('demo123');
    await page.locator('button[type="submit"]').click();
    
    // Navigate to debates page
    await page.goto('/debates');
    
    // Wait for page to load
    await page.waitForTimeout(2000);
    
    // Check that debates page loads
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).toBeTruthy();
    expect(bodyText!.length).toBeGreaterThan(50);
    
    // Look for debate-related elements
    await expect(page.locator('button')).toContainText('Create Debate');
    
    // Check for no critical errors
    const logs = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        logs.push(msg.text());
      }
    });
    
    await page.waitForTimeout(1000);
    
    const criticalErrors = logs.filter(log => 
      log.includes('map is not a function') || 
      log.includes('Cannot read properties of undefined')
    );
    
    expect(criticalErrors).toHaveLength(0);
  });

  test('should navigate to organization management page', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.locator('input[type="text"]').fill('demo');
    await page.locator('input[type="password"]').fill('demo123');
    await page.locator('button[type="submit"]').click();
    
    // Navigate to organization management
    await page.goto('/organization-management');
    
    // Wait for page to load
    await page.waitForTimeout(3000);
    
    // Check that organization management page loads
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).toBeTruthy();
    expect(bodyText!.length).toBeGreaterThan(100);
    
    // Look for organization-related elements
    await expect(page.locator('h1, h2, h3, h4')).toContainText('Organization Management');
    
    // Check for tabs
    const tabs = page.locator('[role="tab"]');
    await expect(tabs).toHaveCount(4);
    
    // Check for organization data
    await expect(page.locator('body')).toContainText('Acme Corporation');
  });

  test('should handle API errors gracefully', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.locator('input[type="text"]').fill('demo');
    await page.locator('input[type="password"]').fill('demo123');
    await page.locator('button[type="submit"]').click();
    
    // Navigate to home page
    await page.goto('/');
    
    // Wait for API calls
    await page.waitForTimeout(2000);
    
    // Check that page doesn't crash even if API fails
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).toBeTruthy();
    
    // Should not have blank content
    expect(bodyText!.length).toBeGreaterThan(50);
  });

  test('should maintain authentication state across page navigation', async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.locator('input[type="text"]').fill('demo');
    await page.locator('input[type="password"]').fill('demo123');
    await page.locator('button[type="submit"]').click();
    
    // Navigate to different pages
    const pages = ['/debates', '/analytics', '/settings', '/organization-management'];
    
    for (const pagePath of pages) {
      await page.goto(pagePath);
      await page.waitForTimeout(1000);
      
      // Should not redirect to login
      expect(page.url()).not.toContain('/login');
      
      // Should have content
      const bodyText = await page.locator('body').textContent();
      expect(bodyText).toBeTruthy();
      expect(bodyText!.length).toBeGreaterThan(50);
    }
  });
});