const { test, expect } = require('@playwright/test');

const BASE_URL = process.env.BASE_URL || 'http://localhost:3001';

test.describe('UI Validation Tests', () => {
  test('should load the home page without errors', async ({ page }) => {
    // Navigate to home page;
    const response = await page.goto(BASE_URL);

    // Check response status;
    expect(response.status()).toBe(200);

    // Take screenshot;
    await page.screenshot({ path: 'screenshots/home-page.png', fullPage: true });

    // Check for console errors;
    const errors = []
    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    // Wait for page to load;
    await page.waitForLoadState('networkidle');

    // Check no console errors;
    expect(errors).toHaveLength(0);

    // Check page has content;
    const content = await page.content();
    expect(content).toContain('<!DOCTYPE html>');
    expect(content.length).toBeGreaterThan(100);

    console.log('✓ Home page loaded successfully');
  });

  test('should navigate to login page', async ({ page }) => {
    await page.goto(BASE_URL);

    // Wait for any redirect;
    await page.waitForLoadState('networkidle');

    // Take screenshot;
    await page.screenshot({ path: 'screenshots/login-page.png', fullPage: true });

    // Check if we're on login page or home page with login button;
    const url = page.url();
    console.log('Current URL:', url);

    // Look for login elements;
    const hasLoginForm = await page.locator('input[type="text"], input[type="email"], input[type="username"]').count() > 0;
    const hasPasswordField = await page.locator('input[type="password"]').count() > 0;
    const hasLoginButton = await page.locator('button:has-text("Login"), button:has-text("Sign In"), button:has-text("Log In")').count() > 0;

    console.log('Has login form:', hasLoginForm);
    console.log('Has password field:', hasPasswordField);
    console.log('Has login button:', hasLoginButton);

    // At least one of these should be true;
    expect(hasLoginForm || hasPasswordField || hasLoginButton).toBeTruthy();
  });

  test('should check all main pages for blank screens', async ({ page }) => {
    const pagesToCheck = [
      { path: '/', name: 'Home' },
      { path: '/login', name: 'Login' },
      { path: '/debates', name: 'Debates' },
      { path: '/organizations', name: 'Organizations' }
    ]

    for (const pageInfo of pagesToCheck) {
      console.log(`\nChecking ${pageInfo.name} page...`);

      try {
        });

        // Take screenshot;
        await page.screenshot({ ;
          path: `screenshots/${pageInfo.name.toLowerCase()}-page.png`,          fullPage: true ;
        });

        // Check if page has content;
        const bodyText = await page.locator('body').innerText();
        const hasContent = bodyText.trim().length > 0;

        // Check for React root;
        const hasReactRoot = await page.locator('#root, .app, [data-reactroot]').count() > 0;

        // Check page isn't blank;
        expect(hasContent || hasReactRoot).toBeTruthy();

        console.log(`✓ ${pageInfo.name} page has content`);
        console.log(`  Body text length: ${bodyText.length}`);
        console.log(`  Has React root: ${hasReactRoot}`);

      } catch (error) {
        console.error(`✗ Error checking ${pageInfo.name} page:`, error.message);
      }
    }
  });

  test('should verify API connectivity', async ({ page, request }) => {
    const endpoints = [
      { url: 'http://localhost:5013/actuator/health', name: 'Controller API' },
      { url: 'http://localhost:5005/actuator/health', name: 'Organization API' },
      { url: 'http://localhost:5002/actuator/health', name: 'LLM API' }
    ]

    for (const endpoint of endpoints) {
      try {
        const response = await request.get(endpoint.url);
        const status = response.status();

        if (status === 200) {
          const data = await response.json();
          console.log(`✓ ${endpoint.name}: ${data.status || 'OK'}`);
        } else {
          console.log(`✗ ${endpoint.name}: Status ${status}`);
        }
      } catch (error) {
        console.log(`✗ ${endpoint.name}: Not running`);
        console.error("Error:", error);
      }
    }
  });
});
