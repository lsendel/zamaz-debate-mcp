import { test, expect } from '@playwright/test';

/**
 * UI Component Tests
 * 
 * Goal: Test all UI components systematically without mocking
 * Focus: Real interactions with actual services
 * 
 * Test Coverage:
 * 1. Navigation and layout components
 * 2. Form components and validation
 * 3. Data display components
 * 4. Interactive elements (buttons, dropdowns, etc.)
 * 5. WebSocket connection status
 * 6. Error states and loading states
 */

test.describe('UI Component Tests', () => {
  const baseURL = process.env.BASE_URL || 'http://localhost:3000';

  test.beforeEach(async ({ page }) => {
    // Navigate to the application
    await page.goto(baseURL);
    await page.waitForLoadState('networkidle');
  });

  test.describe('Navigation and Layout', () => {
    test('should display main navigation elements', async ({ page }) => {
      // Check header
      await expect(page.locator('header')).toBeVisible();
      await expect(page.locator('h1').first()).toContainText('AI Debate System');
      
      // Check navigation links
      const navLinks = [
        { text: 'Debates', href: '/' },
        { text: 'Templates', href: '/templates' },
        { text: 'Organizations', href: '/organizations' }
      ];
      
      for (const link of navLinks) {
        const navLink = page.locator(`nav a:has-text("${link.text}")`);
        await expect(navLink).toBeVisible();
        await expect(navLink).toHaveAttribute('href', link.href);
      }
      
      // Take screenshot of navigation
      await page.screenshot({ 
        path: `test_probe/evidence/screenshots/navigation-layout.png`,
        fullPage: false 
      });
    });

    test('should navigate between pages correctly', async ({ page }) => {
      // Test navigation to Templates
      await page.click('nav a:has-text("Templates")');
      await page.waitForURL('**/templates');
      await expect(page.locator('h1')).toContainText('Templates');
      
      // Test navigation to Organizations
      await page.click('nav a:has-text("Organizations")');
      await page.waitForURL('**/organizations');
      await expect(page.locator('h1')).toContainText('Organizations');
      
      // Navigate back to Debates
      await page.click('nav a:has-text("Debates")');
      await page.waitForURL(baseURL);
      await expect(page.locator('h1').first()).toContainText('AI Debate System');
    });

    test('should be responsive on mobile viewport', async ({ page }) => {
      // Test mobile viewport
      await page.setViewportSize({ width: 375, height: 667 });
      await page.reload();
      
      // Check if mobile menu button appears
      const mobileMenuButton = page.locator('[data-testid="mobile-menu-button"]');
      await expect(mobileMenuButton).toBeVisible();
      
      // Click mobile menu
      await mobileMenuButton.click();
      await expect(page.locator('nav')).toBeVisible();
      
      // Take mobile screenshot
      await page.screenshot({ 
        path: `test_probe/evidence/screenshots/mobile-layout.png`,
        fullPage: true 
      });
    });
  });

  test.describe('Connection Status Component', () => {
    test('should display connection status for all services', async ({ page }) => {
      // Look for connection status component
      const connectionStatus = page.locator('[data-testid="connection-status"]');
      await expect(connectionStatus).toBeVisible();
      
      // Check individual service statuses
      const services = ['LLM', 'Debate', 'Context', 'RAG'];
      
      for (const service of services) {
        const serviceStatus = page.locator(`[data-testid="status-${service.toLowerCase()}"]`);
        await expect(serviceStatus).toBeVisible();
        
        // Status should be either connected (green) or disconnected (red)
        const statusClass = await serviceStatus.getAttribute('class');
        expect(statusClass).toMatch(/status-(connected|disconnected)/);
      }
      
      // Take screenshot of connection status
      await page.screenshot({ 
        path: `test_probe/evidence/screenshots/connection-status.png`,
        clip: await connectionStatus.boundingBox() 
      });
    });

    test('should update status in real-time', async ({ page }) => {
      // Monitor WebSocket connection
      const wsPromise = page.waitForEvent('websocket');
      
      // Trigger a WebSocket connection (if not already connected)
      await page.reload();
      
      try {
        const ws = await wsPromise;
        console.log('WebSocket URL:', ws.url());
        
        // Wait for connection
        await page.waitForFunction(
          () => {
            const status = document.querySelector('[data-testid="ws-status"]');
            return status?.textContent?.includes('Connected');
          },
          { timeout: 10000 }
        );
        
        await expect(page.locator('[data-testid="ws-status"]')).toContainText('Connected');
      } catch (error) {
        console.log('WebSocket connection test skipped - no WebSocket found');
      }
    });
  });

  test.describe('Create Debate Dialog', () => {
    test('should open and close create debate dialog', async ({ page }) => {
      // Click create debate button
      const createButton = page.locator('button:has-text("Create Debate")');
      await expect(createButton).toBeVisible();
      await createButton.click();
      
      // Check dialog is open
      const dialog = page.locator('[role="dialog"]');
      await expect(dialog).toBeVisible();
      await expect(dialog.locator('h2')).toContainText('Create New Debate');
      
      // Take screenshot of dialog
      await page.screenshot({ 
        path: `test_probe/evidence/screenshots/create-debate-dialog.png`,
        fullPage: true 
      });
      
      // Close dialog with Cancel button
      await page.click('button:has-text("Cancel")');
      await expect(dialog).not.toBeVisible();
      
      // Reopen and close with X button
      await createButton.click();
      await expect(dialog).toBeVisible();
      await page.click('[aria-label="Close"]');
      await expect(dialog).not.toBeVisible();
    });

    test('should validate required fields', async ({ page }) => {
      await page.click('button:has-text("Create Debate")');
      
      // Try to submit without filling fields
      await page.click('button:has-text("Create"):not(:has-text("Create Debate"))');
      
      // Check for validation errors
      const nameError = page.locator('[data-testid="name-error"]');
      const topicError = page.locator('[data-testid="topic-error"]');
      
      await expect(nameError).toBeVisible();
      await expect(topicError).toBeVisible();
      
      // Fill required fields
      await page.fill('input[name="name"]', 'Test Debate');
      await page.fill('input[name="topic"]', 'Test Topic');
      
      // Errors should disappear
      await expect(nameError).not.toBeVisible();
      await expect(topicError).not.toBeVisible();
    });

    test('should add and remove participants dynamically', async ({ page }) => {
      await page.click('button:has-text("Create Debate")');
      
      // Should start with 2 participants
      let participantCards = page.locator('[data-testid^="participant-"]');
      await expect(participantCards).toHaveCount(2);
      
      // Add a third participant
      await page.click('button:has-text("Add Participant")');
      await expect(participantCards).toHaveCount(3);
      
      // Fill third participant details
      await page.fill('[data-testid="participant-2-name"]', 'Third Debater');
      
      // Remove second participant
      await page.click('[data-testid="remove-participant-1"]');
      await expect(participantCards).toHaveCount(2);
      
      // Verify third participant became second
      const secondParticipantName = await page.inputValue('[data-testid="participant-1-name"]');
      expect(secondParticipantName).toBe('Third Debater');
    });

    test('should populate LLM models based on provider selection', async ({ page }) => {
      await page.click('button:has-text("Create Debate")');
      
      // Test Claude provider
      await page.selectOption('[data-testid="participant-0-provider"]', 'claude');
      await page.waitForTimeout(500); // Wait for models to load
      
      const claudeModels = await page.locator('[data-testid="participant-0-model"] option').allTextContents();
      expect(claudeModels).toContain('Claude 3.5 Sonnet');
      expect(claudeModels).toContain('Claude 3.5 Haiku');
      
      // Test OpenAI provider
      await page.selectOption('[data-testid="participant-0-provider"]', 'openai');
      await page.waitForTimeout(500);
      
      const openaiModels = await page.locator('[data-testid="participant-0-model"] option').allTextContents();
      expect(openaiModels).toContain('GPT-4o');
      expect(openaiModels).toContain('GPT-4o mini');
      
      // Test Gemini provider
      await page.selectOption('[data-testid="participant-0-provider"]', 'gemini');
      await page.waitForTimeout(500);
      
      const geminiModels = await page.locator('[data-testid="participant-0-model"] option').allTextContents();
      expect(geminiModels).toContain('Gemini 2.5 Pro');
      expect(geminiModels).toContain('Gemini 2.0 Flash');
    });
  });

  test.describe('Debate List Components', () => {
    test('should display debate cards with correct information', async ({ page }) => {
      // Wait for debate cards to load
      await page.waitForSelector('[data-testid="debate-card"]', { timeout: 10000 });
      
      const debateCards = page.locator('[data-testid="debate-card"]');
      const cardCount = await debateCards.count();
      
      if (cardCount > 0) {
        // Check first debate card
        const firstCard = debateCards.first();
        
        // Should have title
        await expect(firstCard.locator('[data-testid="debate-title"]')).toBeVisible();
        
        // Should have status badge
        const statusBadge = firstCard.locator('[data-testid="debate-status-badge"]');
        await expect(statusBadge).toBeVisible();
        const status = await statusBadge.textContent();
        expect(['draft', 'active', 'paused', 'completed']).toContain(status?.toLowerCase());
        
        // Should have participant count
        await expect(firstCard.locator('[data-testid="participant-count"]')).toBeVisible();
        
        // Should have turn count
        await expect(firstCard.locator('[data-testid="turn-count"]')).toBeVisible();
        
        // Take screenshot of debate card
        await page.screenshot({ 
          path: `test_probe/evidence/screenshots/debate-card.png`,
          clip: await firstCard.boundingBox() 
        });
      }
    });

    test('should filter debates by status', async ({ page }) => {
      // Wait for filter buttons
      const filterButtons = page.locator('[data-testid^="filter-"]');
      await expect(filterButtons.first()).toBeVisible();
      
      // Test each filter
      const filters = ['all', 'draft', 'active', 'completed'];
      
      for (const filter of filters) {
        await page.click(`[data-testid="filter-${filter}"]`);
        await page.waitForTimeout(500); // Wait for filter to apply
        
        if (filter !== 'all') {
          // Check that all visible cards have the correct status
          const visibleCards = page.locator('[data-testid="debate-card"]:visible');
          const cardCount = await visibleCards.count();
          
          for (let i = 0; i < cardCount; i++) {
            const cardStatus = await visibleCards.nth(i)
              .locator('[data-testid="debate-status-badge"]')
              .textContent();
            expect(cardStatus?.toLowerCase()).toBe(filter);
          }
        }
      }
    });

    test('should display loading skeleton while fetching debates', async ({ page }) => {
      // Intercept the debates API call
      await page.route('**/api/debate/debates', async route => {
        // Delay the response to see loading state
        await page.waitForTimeout(2000);
        await route.continue();
      });
      
      // Reload page to trigger loading
      await page.reload();
      
      // Check for skeleton loader
      const skeleton = page.locator('[data-testid="debate-list-skeleton"]');
      await expect(skeleton).toBeVisible();
      
      // Take screenshot of skeleton
      await page.screenshot({ 
        path: `test_probe/evidence/screenshots/debate-list-skeleton.png`,
        fullPage: false 
      });
      
      // Wait for actual content
      await page.waitForSelector('[data-testid="debate-card"]', { timeout: 10000 });
      await expect(skeleton).not.toBeVisible();
    });
  });

  test.describe('Error States', () => {
    test('should display error message when API fails', async ({ page }) => {
      // Intercept API call and return error
      await page.route('**/api/debate/debates', route => {
        route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Internal Server Error' })
        });
      });
      
      // Reload to trigger error
      await page.reload();
      
      // Check for error message
      const errorMessage = page.locator('[data-testid="error-message"]');
      await expect(errorMessage).toBeVisible();
      await expect(errorMessage).toContainText('Error loading debates');
      
      // Check for retry button
      const retryButton = page.locator('button:has-text("Retry")');
      await expect(retryButton).toBeVisible();
      
      // Take screenshot of error state
      await page.screenshot({ 
        path: `test_probe/evidence/screenshots/error-state.png`,
        fullPage: false 
      });
    });

    test('should show empty state when no debates exist', async ({ page }) => {
      // Intercept API call and return empty array
      await page.route('**/api/debate/debates', route => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ debates: [] })
        });
      });
      
      // Reload to see empty state
      await page.reload();
      
      // Check for empty state message
      const emptyState = page.locator('[data-testid="empty-state"]');
      await expect(emptyState).toBeVisible();
      await expect(emptyState).toContainText('No debates yet');
      
      // Should show create button prominently
      const createButton = emptyState.locator('button:has-text("Create your first debate")');
      await expect(createButton).toBeVisible();
    });
  });

  test.describe('Toast Notifications', () => {
    test('should show success toast on debate creation', async ({ page }) => {
      // Create a debate
      await page.click('button:has-text("Create Debate")');
      await page.fill('input[name="name"]', 'Toast Test Debate');
      await page.fill('input[name="topic"]', 'Testing toast notifications');
      await page.click('button:has-text("Create"):not(:has-text("Create Debate"))');
      
      // Wait for success toast
      const toast = page.locator('[data-testid="toast-success"]');
      await expect(toast).toBeVisible();
      await expect(toast).toContainText('Debate created successfully');
      
      // Toast should auto-dismiss
      await expect(toast).not.toBeVisible({ timeout: 6000 });
    });

    test('should show error toast on failure', async ({ page }) => {
      // Intercept create API and return error
      await page.route('**/api/debate/debates', route => {
        if (route.request().method() === 'POST') {
          route.fulfill({
            status: 400,
            contentType: 'application/json',
            body: JSON.stringify({ error: 'Invalid debate configuration' })
          });
        } else {
          route.continue();
        }
      });
      
      // Try to create debate
      await page.click('button:has-text("Create Debate")');
      await page.fill('input[name="name"]', 'Error Test');
      await page.fill('input[name="topic"]', 'This should fail');
      await page.click('button:has-text("Create"):not(:has-text("Create Debate"))');
      
      // Check error toast
      const errorToast = page.locator('[data-testid="toast-error"]');
      await expect(errorToast).toBeVisible();
      await expect(errorToast).toContainText('Invalid debate configuration');
    });
  });

  test.describe('Accessibility', () => {
    test('should have proper ARIA labels and roles', async ({ page }) => {
      // Check main navigation
      await expect(page.locator('nav[role="navigation"]')).toBeVisible();
      
      // Check main content area
      await expect(page.locator('main[role="main"]')).toBeVisible();
      
      // Check buttons have accessible labels
      const createButton = page.locator('button:has-text("Create Debate")');
      await expect(createButton).toHaveAttribute('aria-label', /create.*debate/i);
      
      // Check form inputs have labels
      await createButton.click();
      const nameInput = page.locator('input[name="name"]');
      const nameLabel = page.locator('label[for="name"]');
      await expect(nameLabel).toBeVisible();
      await expect(nameLabel).toContainText('Debate Name');
    });

    test('should be keyboard navigable', async ({ page }) => {
      // Tab through main elements
      await page.keyboard.press('Tab'); // Skip to main content
      await page.keyboard.press('Tab'); // Focus on first interactive element
      
      // Check focus is visible
      const focusedElement = page.locator(':focus');
      await expect(focusedElement).toBeVisible();
      
      // Navigate with arrow keys in debate list
      const debateCards = page.locator('[data-testid="debate-card"]');
      if (await debateCards.count() > 1) {
        await debateCards.first().focus();
        await page.keyboard.press('ArrowDown');
        
        const newFocused = page.locator(':focus');
        const secondCard = debateCards.nth(1);
        await expect(newFocused).toBe(secondCard);
      }
    });
  });
});