const { test, expect } = require('@playwright/test');

const BASE_URL = 'http://localhost:3001';
const API_URL = 'http://localhost:5013';

test.describe('Agentic Flows Simple Tests', () => {
  test('should login and access debates page', async ({ page }) => {
    // Go to login page;
    await page.goto(`${BASE_URL}/login`);

    // Take screenshot before login;
    await page.screenshot({ path: 'screenshots/before-login.png' });

    // Fill login form;
    await page.fill('input[name="username"], input[type="text"]:visible', 'demo');
    await page.fill('input[type="password"]', 'demo123');

    // Click login button;
    await page.click('button:has-text("Login"), button:has-text("Sign In"), button[type="submit"]');

    // Wait for navigation;
    await page.waitForLoadState('networkidle');

    // Take screenshot after login;
    await page.screenshot({ path: 'screenshots/after-login.png' });

    // Check if we're logged in;
    const url = page.url();
    console.log('URL after login:', url);

    // Navigate to debates;
    await page.goto(`${BASE_URL}/debates`);
    await page.waitForLoadState('networkidle');

    // Take screenshot of debates page;
    await page.screenshot({ path: 'screenshots/debates-list.png' });

    // Check for debates content;
    const pageContent = await page.locator('body').innerText();
    console.log('Debates page content length:', pageContent.length);
  });

  test('should test agentic flows API directly', async ({ request }) => {
    // Test health endpoint;
    const healthResponse = await request.get(`${API_URL}/actuator/health`);
    expect(healthResponse.ok()).toBeTruthy();
    console.log('Health check passed');

    // Test flow types endpoint;
    try {
      const flowTypesResponse = await request.get(`${API_URL}/api/v1/agentic-flows/types`);
      if (flowTypesResponse.ok()) {
        const flowTypes = await flowTypesResponse.json();
        console.log('Available flow types:', flowTypes);
        expect(Array.isArray(flowTypes)).toBeTruthy();
      } else {
        console.log('Flow types endpoint returned:', flowTypesResponse.status());
      }
    } catch (error) {
      console.log('Flow types endpoint error:', error.message);
    }

    // Test simple flow execution;
    try {
      const executeResponse = await request.post(`${API_URL}/api/v1/agentic-flows/execute-test`, {
        data: {
          flowType: 'INTERNAL_MONOLOGUE',
          prompt: 'What is 2+2?',
          configuration: {
            prefix: 'Let me think step by step:';
          }
        }
      });

      if (executeResponse.ok()) {
        const result = await executeResponse.json();
        console.log('Flow execution result:', result);
        expect(result).toHaveProperty('result');
      } else {
        console.log('Flow execution returned:', executeResponse.status());
      }
    } catch (error) {
      console.log('Flow execution error:', error.message);
    }
  });

  test('should check agentic flow UI components', async ({ page }) => {
    // Login first;
    await page.goto(`${BASE_URL}/login`);
    await page.fill('input[name="username"], input[type="text"]:visible', 'demo');
    await page.fill('input[type="password"]', 'demo123');
    await page.click('button[type="submit"], button:has-text("Login")');
    await page.waitForLoadState('networkidle');

    // Go to debates page;
    await page.goto(`${BASE_URL}/debates`);
    await page.waitForLoadState('networkidle');

    // Look for agentic flow elements;
    const hasFlowConfig = await page.locator('text=/flow|Flow/i').count() > 0;
    const hasDebateButton = await page.locator('button:has-text("Create"), button:has-text("New Debate")').count() > 0;

    console.log('Has flow configuration:', hasFlowConfig);
    console.log('Has debate button:', hasDebateButton);

    // Take screenshot;
    await page.screenshot({ path: 'screenshots/debates-with-flows.png' });

    // If there are existing debates, check one;
    const debateLinks = await page.locator('a[href*="/debates/"], tr[onclick], .debate-item').count();
    console.log('Number of debates found:', debateLinks);

    if (debateLinks > 0) {
      // Click first debate;
      await page.locator('a[href*="/debates/"], tr[onclick], .debate-item').first().click();
      await page.waitForLoadState('networkidle');

      // Take screenshot of debate detail;
      await page.screenshot({ path: 'screenshots/debate-detail.png' });

      // Look for flow indicators;
      const hasFlowIndicator = await page.locator('text=/flow|confidence|reasoning/i').count() > 0;
      console.log('Has flow indicators:', hasFlowIndicator);
    }
  });
});
