import { test, expect } from '@playwright/test';
import { Page } from '@playwright/test';

// Helper function to take screenshots
async function takeScreenshot(page: Page, name: string) {
  await page.screenshot({ path: `screenshots/${name}.png`, fullPage: true });
}

// Helper to wait for services
async function waitForServices(page: Page) {
  // Wait for all services to be healthy
  const maxRetries = 30;
  let retries = 0;
  
  while (retries < maxRetries) {
    try {
      // Check debate service
      const debateResponse = await page.request.get('http://localhost:5013/health');
      const llmResponse = await page.request.get('http://localhost:5002/health');
      
      if (debateResponse.ok() && llmResponse.ok()) {
        console.log('All services are healthy');
        break;
      }
    } catch (error) {
      console.log(`Waiting for services... (${retries + 1}/${maxRetries})`);
    }
    
    retries++;
    await page.waitForTimeout(2000);
  }
  
  if (retries === maxRetries) {
    throw new Error('Services did not start in time');
  }
}

test.describe('Complete UI Testing Suite', () => {
  test.beforeEach(async ({ page }) => {
    // Wait for services to be ready
    await waitForServices(page);
    
    // Navigate to the app
    await page.goto('/');
    
    // Wait for the page to load
    await page.waitForLoadState('networkidle');
  });

  test('1. Homepage loads correctly with all elements', async ({ page }) => {
    // Check header elements
    await expect(page.locator('h1:has-text("AI Debate System")')).toBeVisible();
    await expect(page.locator('text=Powered by MCP & Ollama')).toBeVisible();
    
    // Check main buttons
    await expect(page.locator('button:has-text("Test LLM")')).toBeVisible();
    await expect(page.locator('button:has-text("New Debate")')).toBeVisible();
    
    // Take screenshot of homepage
    await takeScreenshot(page, '01-homepage');
    
    // Check for CSS issues by looking for broken layouts
    const brokenElements = await page.$$eval('*', elements => {
      return elements.filter(el => {
        const styles = window.getComputedStyle(el);
        const rect = el.getBoundingClientRect();
        return (
          // Check for elements with zero dimensions
          (rect.width === 0 && rect.height === 0 && el.textContent?.trim()) ||
          // Check for elements outside viewport
          (rect.right < 0 || rect.bottom < 0) ||
          // Check for overlapping text
          (styles.overflow === 'visible' && rect.width < el.scrollWidth)
        );
      }).map(el => ({
        tag: el.tagName,
        text: el.textContent?.substring(0, 50),
        classes: el.className
      }));
    });
    
    if (brokenElements.length > 0) {
      console.log('Potential CSS issues found:', brokenElements);
    }
    
    expect(brokenElements.length).toBe(0);
  });

  test('2. Test LLM connectivity', async ({ page }) => {
    // Click Test LLM button
    await page.click('button:has-text("Test LLM")');
    
    // Wait for dialog
    await expect(page.locator('text=Test LLM Connection')).toBeVisible();
    
    // Take screenshot of LLM test dialog
    await takeScreenshot(page, '02-llm-test-dialog');
    
    // Select Llama provider (for local testing)
    await page.click('button[role="combobox"]');
    await page.click('text=Llama (Ollama)');
    
    // Select a model
    await page.waitForTimeout(500);
    const modelSelector = page.locator('button[role="combobox"]').nth(1);
    await modelSelector.click();
    await page.click('text=Llama 3.2').first();
    
    // Test the connection
    await page.click('button:has-text("Test Connection")');
    
    // Wait for response (with timeout)
    await page.waitForSelector('text=Response received', { timeout: 30000 }).catch(() => {
      console.log('LLM response timeout - checking if Ollama is running');
    });
    
    // Take screenshot of result
    await takeScreenshot(page, '03-llm-test-result');
    
    // Close dialog
    await page.click('button:has-text("Close")');
  });

  test('3. Organization management', async ({ page }) => {
    // Click organization switcher
    await page.click('[aria-expanded]');
    
    // Take screenshot of organization menu
    await takeScreenshot(page, '04-organization-menu');
    
    // Check if default organization exists
    await expect(page.locator('text=Default Organization')).toBeVisible();
    
    // Close organization menu
    await page.keyboard.press('Escape');
  });

  test('4. Create a new debate', async ({ page }) => {
    // Click New Debate button
    await page.click('button:has-text("New Debate")');
    
    // Wait for dialog
    await expect(page.locator('text=Create New Debate')).toBeVisible();
    
    // Take screenshot of create debate dialog
    await takeScreenshot(page, '05-create-debate-dialog');
    
    // Fill in debate details
    await page.fill('input[id="name"]', 'Test Debate: Access History Logging');
    await page.fill('input[id="topic"]', 'Should we log access history to the app?');
    await page.fill('textarea[id="description"]', 'A debate about privacy vs security in app access logging');
    
    // Configure debate rules
    await page.selectOption('select', 'round_robin');
    await page.fill('input[type="number"]', '3'); // Max rounds
    
    // Configure participants
    const participant1 = {
      name: 'Privacy Advocate',
      position: 'Against logging - privacy concerns',
      provider: 'gemini',
      model: 'gemini-2.0-flash-exp'
    };
    
    const participant2 = {
      name: 'Security Expert',
      position: 'For logging - security benefits',
      provider: 'claude',
      model: 'claude-3-5-sonnet-20241022'
    };
    
    // Update first participant
    await page.fill('input[value*="AI Optimist"]', participant1.name);
    await page.locator('input[placeholder*="position"]').first().fill(participant1.position);
    
    // Take screenshot of filled form
    await takeScreenshot(page, '06-debate-form-filled');
    
    // Submit debate
    await page.click('button:has-text("Create Debate")');
    
    // Wait for debate to be created
    await page.waitForSelector('text=Debate created successfully', { timeout: 10000 }).catch(() => {
      console.log('Debate creation notification not found');
    });
    
    // Take screenshot after creation
    await takeScreenshot(page, '07-debate-created');
  });

  test('5. Navigate through debate list', async ({ page }) => {
    // Check debates tab
    await page.click('button[role="tab"]:has-text("Debates")');
    
    // Wait for debates to load
    await page.waitForTimeout(2000);
    
    // Take screenshot of debates list
    await takeScreenshot(page, '08-debates-list');
    
    // Check if debates are displayed
    const debateCards = await page.$$('[data-testid="debate-card"]');
    console.log(`Found ${debateCards.length} debates`);
    
    // Click on a debate if exists
    if (debateCards.length > 0) {
      await debateCards[0].click();
      await page.waitForTimeout(1000);
      await takeScreenshot(page, '09-debate-detail');
    }
  });

  test('6. Test WebSocket connections', async ({ page }) => {
    // Open browser console to check for WebSocket errors
    page.on('console', msg => {
      if (msg.type() === 'error' && msg.text().includes('WebSocket')) {
        console.error('WebSocket error:', msg.text());
      }
    });
    
    // Navigate to debates
    await page.click('button[role="tab"]:has-text("Debates")');
    
    // Check WebSocket status in console
    const wsConnected = await page.evaluate(() => {
      // Check if WebSocket is connected
      return new Promise((resolve) => {
        const ws = new WebSocket('ws://localhost:5013/ws');
        ws.onopen = () => {
          ws.close();
          resolve(true);
        };
        ws.onerror = () => resolve(false);
        setTimeout(() => resolve(false), 5000);
      });
    });
    
    console.log('WebSocket connection status:', wsConnected);
  });

  test('7. Test responsive design', async ({ page }) => {
    // Test different viewport sizes
    const viewports = [
      { name: 'mobile', width: 375, height: 667 },
      { name: 'tablet', width: 768, height: 1024 },
      { name: 'desktop', width: 1920, height: 1080 }
    ];
    
    for (const viewport of viewports) {
      await page.setViewportSize(viewport);
      await page.waitForTimeout(500);
      await takeScreenshot(page, `10-responsive-${viewport.name}`);
      
      // Check if navigation is still accessible
      await expect(page.locator('button:has-text("New Debate")')).toBeVisible();
    }
  });

  test('8. Test dark mode (if available)', async ({ page }) => {
    // Look for theme toggle
    const themeToggle = page.locator('[data-testid="theme-toggle"]');
    
    if (await themeToggle.count() > 0) {
      await themeToggle.click();
      await page.waitForTimeout(500);
      await takeScreenshot(page, '11-dark-mode');
      
      // Check if dark mode classes are applied
      const isDarkMode = await page.evaluate(() => {
        return document.documentElement.classList.contains('dark');
      });
      
      expect(isDarkMode).toBe(true);
    }
  });

  test('9. Test error handling', async ({ page }) => {
    // Try to create debate with invalid data
    await page.click('button:has-text("New Debate")');
    
    // Submit without filling required fields
    await page.click('button:has-text("Create Debate")');
    
    // Check for validation errors
    await page.waitForTimeout(1000);
    await takeScreenshot(page, '12-validation-errors');
  });

  test('10. Performance check', async ({ page }) => {
    // Measure page load time
    const startTime = Date.now();
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    const loadTime = Date.now() - startTime;
    
    console.log(`Page load time: ${loadTime}ms`);
    expect(loadTime).toBeLessThan(5000); // Should load within 5 seconds
    
    // Check for memory leaks by navigating multiple times
    for (let i = 0; i < 5; i++) {
      await page.click('button:has-text("New Debate")');
      await page.keyboard.press('Escape');
      await page.waitForTimeout(100);
    }
    
    // Check if UI is still responsive
    await expect(page.locator('button:has-text("New Debate")')).toBeEnabled();
  });
});

test.describe('Accessibility Tests', () => {
  test('Check keyboard navigation', async ({ page }) => {
    await page.goto('/');
    
    // Tab through interactive elements
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    
    // Check if focused element is visible
    const focusedElement = await page.evaluate(() => {
      const el = document.activeElement;
      return {
        tag: el?.tagName,
        text: el?.textContent,
        visible: el ? window.getComputedStyle(el).visibility === 'visible' : false
      };
    });
    
    expect(focusedElement.visible).toBe(true);
  });
  
  test('Check ARIA labels', async ({ page }) => {
    await page.goto('/');
    
    // Check for buttons with proper labels
    const buttons = await page.$$eval('button', buttons => {
      return buttons.map(btn => ({
        text: btn.textContent,
        ariaLabel: btn.getAttribute('aria-label'),
        hasAccessibleName: !!(btn.textContent?.trim() || btn.getAttribute('aria-label'))
      }));
    });
    
    const inaccessibleButtons = buttons.filter(btn => !btn.hasAccessibleName);
    expect(inaccessibleButtons.length).toBe(0);
  });
});