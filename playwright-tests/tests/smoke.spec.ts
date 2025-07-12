import { test, expect } from '@playwright/test';

test.describe('Smoke Tests - Basic Functionality', () => {
  test('should load the home page', async ({ page }) => {
    await page.goto('/');
    
    // Check title
    await expect(page).toHaveTitle(/AI Debate System/);
    
    // Check main heading
    const heading = page.getByRole('heading', { name: 'AI Debate System' });
    await expect(heading).toBeVisible();
    
    // Check key UI elements
    await expect(page.getByText('New Debate')).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Debates' })).toBeVisible();
  });

  test('should navigate between tabs', async ({ page }) => {
    await page.goto('/');
    
    // Test tab navigation
    const tabs = [
      { name: 'Debates', content: /debates|No debates yet/ },
      { name: 'Gallery', content: /Debate Templates/ },
      { name: 'Library', content: /Template Library/ },
      { name: 'Setup', content: /Ollama Setup/ }
    ];
    
    for (const tab of tabs) {
      await page.getByRole('tab', { name: tab.name }).click();
      await expect(page.getByText(tab.content)).toBeVisible({ timeout: 10000 });
    }
  });

  test('should open create debate dialog', async ({ page }) => {
    await page.goto('/');
    
    // Click New Debate button
    await page.getByRole('button', { name: 'New Debate' }).click();
    
    // Check dialog opened
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByRole('heading', { name: /Create.*Debate/i })).toBeVisible();
    
    // Check form fields
    await expect(page.getByLabel('Debate Name')).toBeVisible();
    await expect(page.getByLabel('Topic')).toBeVisible();
    
    // Close dialog
    await page.getByRole('button', { name: 'Cancel' }).click();
    await expect(page.getByRole('dialog')).not.toBeVisible();
  });

  test('should display organization switcher', async ({ page }) => {
    await page.goto('/');
    
    // Find organization dropdown
    const orgSwitcher = page.getByRole('button').filter({ hasText: /Organization/ });
    await expect(orgSwitcher).toBeVisible();
    
    // Click to open dropdown
    await orgSwitcher.click();
    
    // Check dropdown content
    await expect(page.getByText('Create Organization')).toBeVisible();
    await expect(page.getByText('View History')).toBeVisible();
    
    // Close dropdown
    await page.keyboard.press('Escape');
  });

  test('should show keyboard shortcuts', async ({ page }) => {
    await page.goto('/');
    
    // Press keyboard shortcut
    await page.keyboard.press('Control+/');
    
    // Check shortcuts dialog
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByText('Keyboard Shortcuts')).toBeVisible();
    
    // Close dialog
    await page.keyboard.press('Escape');
  });

  test('should display quick actions', async ({ page }) => {
    await page.goto('/');
    
    // Check quick action cards
    const quickActions = [
      'Start New Debate',
      'Continue Active',
      'View History',
      'Manage Teams'
    ];
    
    for (const action of quickActions) {
      await expect(page.getByText(action)).toBeVisible();
    }
  });

  test('should handle responsive design', async ({ page, viewport }) => {
    // Test mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');
    
    // Check mobile menu or responsive elements
    await expect(page.getByRole('heading', { name: 'AI Debate System' })).toBeVisible();
    
    // Test tablet viewport
    await page.setViewportSize({ width: 768, height: 1024 });
    await expect(page.getByRole('heading', { name: 'AI Debate System' })).toBeVisible();
    
    // Test desktop viewport
    await page.setViewportSize({ width: 1920, height: 1080 });
    await expect(page.getByRole('heading', { name: 'AI Debate System' })).toBeVisible();
  });

  test('should check service health endpoints', async ({ request }) => {
    const services = [
      { name: 'Context', port: 5001 },
      { name: 'LLM', port: 5002 },
      { name: 'Debate', port: 5003 },
      { name: 'RAG', port: 5004 },
      { name: 'Organization', port: 5005 },
      { name: 'Template', port: 5006 }
    ];
    
    for (const service of services) {
      const response = await request.get(`http://localhost:${service.port}/health`);
      expect(response.ok(), `${service.name} service should be healthy`).toBeTruthy();
    }
  });
});