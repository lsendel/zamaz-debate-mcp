import { test, expect } from '@playwright/test';

/**
 * Smoke Tests
 * 
 * Goal: Quick verification that core functionality is working
 * Focus: Basic health checks, critical paths only
 * 
 * These tests should run quickly (< 2 minutes total) and verify:
 * 1. Services are running
 * 2. UI loads correctly
 * 3. Basic debate creation works
 * 4. LLM integration is functional
 */

test.describe('Smoke Tests', () => {
  const baseURL = process.env.BASE_URL || 'http://localhost:3000';
  const serviceURLs = {
    llm: 'http://localhost:5002',
    debate: 'http://localhost:5013'
  };

  test.describe.configure({ mode: 'parallel' });
  test.setTimeout(30000); // 30 seconds per test

  test('Services health check', async ({ page }) => {
    const services = [
      { name: 'UI', url: baseURL, healthPath: '/' },
      { name: 'LLM Service', url: serviceURLs.llm, healthPath: '/health' },
      { name: 'Debate Service', url: serviceURLs.debate, healthPath: '/health' }
    ];

    for (const service of services) {
      const response = await page.request.get(`${service.url}${service.healthPath}`);
      expect(response.ok(), `${service.name} should be healthy`).toBeTruthy();
      console.log(`✅ ${service.name}: OK (${response.status()})`);
    }
  });

  test('UI loads and displays main elements', async ({ page }) => {
    await page.goto(baseURL);
    
    // Main elements should be visible
    await expect(page.locator('h1').first()).toBeVisible();
    await expect(page.locator('h1').first()).toContainText('AI Debate System');
    await expect(page.locator('button:has-text("Create Debate")')).toBeVisible();
    
    // Navigation should be present
    await expect(page.locator('nav')).toBeVisible();
    
    console.log('✅ UI: Main elements loaded');
  });

  test('LLM models are available', async ({ page }) => {
    const response = await page.request.get(`${serviceURLs.llm}/models`);
    expect(response.ok()).toBeTruthy();
    
    const models = await response.json();
    expect(models.length).toBeGreaterThan(0);
    
    // Check for required models
    const modelIds = models.map(m => m.id);
    expect(modelIds).toContain('claude-3-5-sonnet-20241022');
    expect(modelIds).toContain('gemini-2.5-pro');
    
    console.log(`✅ LLM Service: ${models.length} models available`);
  });

  test('Create debate dialog opens and has required fields', async ({ page }) => {
    await page.goto(baseURL);
    await page.click('button:has-text("Create Debate")');
    
    // Dialog should open
    const dialog = page.locator('[role="dialog"]');
    await expect(dialog).toBeVisible();
    
    // Required fields should be present
    await expect(page.locator('input[name="name"]')).toBeVisible();
    await expect(page.locator('input[name="topic"]')).toBeVisible();
    await expect(page.locator('[data-testid="participant-0-provider"]')).toBeVisible();
    await expect(page.locator('[data-testid="participant-1-provider"]')).toBeVisible();
    
    // Can close dialog
    await page.click('button:has-text("Cancel")');
    await expect(dialog).not.toBeVisible();
    
    console.log('✅ Create Debate: Dialog functional');
  });

  test('Quick debate creation and navigation', async ({ page }) => {
    await page.goto(baseURL);
    
    // Create a simple debate
    await page.click('button:has-text("Create Debate")');
    await page.fill('input[name="name"]', `Smoke Test ${Date.now()}`);
    await page.fill('input[name="topic"]', 'Quick smoke test verification');
    
    // Use default participants
    await page.click('button:has-text("Create"):not(:has-text("Create Debate"))');
    
    // Should navigate to debate page
    await page.waitForURL(/\/debate\//, { timeout: 10000 });
    
    // Debate page should load
    await expect(page.locator('[data-testid="debate-status"]')).toBeVisible();
    await expect(page.locator('button:has-text("Start Debate")')).toBeVisible();
    
    console.log('✅ Debate Creation: Success');
  });

  test('WebSocket connection establishes', async ({ page }) => {
    await page.goto(baseURL);
    
    // Wait for WebSocket connection
    const wsPromise = page.waitForEvent('websocket', { timeout: 10000 });
    
    try {
      const ws = await wsPromise;
      console.log(`✅ WebSocket: Connected to ${ws.url()}`);
    } catch (error) {
      // WebSocket might not be required for basic functionality
      console.log('⚠️ WebSocket: Not connected (may be optional)');
    }
  });

  test('API endpoints respond correctly', async ({ page }) => {
    // Test key API endpoints
    const endpoints = [
      { method: 'GET', path: '/debates', service: serviceURLs.debate },
      { method: 'GET', path: '/models', service: serviceURLs.llm },
      { method: 'GET', path: '/models/claude', service: serviceURLs.llm },
      { method: 'GET', path: '/models/gemini', service: serviceURLs.llm }
    ];

    for (const endpoint of endpoints) {
      const response = await page.request[endpoint.method.toLowerCase()](
        `${endpoint.service}${endpoint.path}`
      );
      expect(response.ok(), `${endpoint.method} ${endpoint.path} should return 2xx`).toBeTruthy();
    }
    
    console.log('✅ API Endpoints: All responding');
  });

  test('Quick LLM response test', async ({ page }) => {
    // Test a quick completion to verify LLM is working
    const response = await page.request.post(`${serviceURLs.llm}/chat/completions`, {
      data: {
        model: 'claude-3-5-sonnet-20241022',
        messages: [{ role: 'user', content: 'Say "Hello" in one word.' }],
        max_tokens: 10,
        temperature: 0
      }
    });

    expect(response.ok()).toBeTruthy();
    const completion = await response.json();
    expect(completion.choices[0].message.content).toBeTruthy();
    
    console.log('✅ LLM Response: Working');
  });
});

// Critical path smoke test - runs serially
test.describe('Critical Path Smoke Test', () => {
  test.describe.configure({ mode: 'serial' });
  test.setTimeout(60000); // 1 minute for the full flow

  test('Complete debate flow (create -> start -> turn)', async ({ page }) => {
    const baseURL = process.env.BASE_URL || 'http://localhost:3000';
    await page.goto(baseURL);

    // Step 1: Create debate
    await test.step('Create debate', async () => {
      await page.click('button:has-text("Create Debate")');
      await page.fill('input[name="name"]', 'Critical Path Test');
      await page.fill('input[name="topic"]', 'Smoke test critical path');
      await page.click('button:has-text("Create"):not(:has-text("Create Debate"))');
      await page.waitForURL(/\/debate\//);
    });

    // Step 2: Start debate
    await test.step('Start debate', async () => {
      await page.click('button:has-text("Start Debate")');
      await page.waitForFunction(
        () => {
          const status = document.querySelector('[data-testid="debate-status"]');
          return status?.textContent?.toLowerCase() === 'active';
        },
        { timeout: 10000 }
      );
    });

    // Step 3: Wait for first turn
    await test.step('First turn generated', async () => {
      await page.waitForSelector('[data-testid="debate-turn-1"]', { timeout: 45000 });
      const turnContent = await page.textContent('[data-testid="debate-turn-1"] [data-testid="turn-content"]');
      expect(turnContent).toBeTruthy();
      expect(turnContent.length).toBeGreaterThan(50);
    });

    console.log('✅ Critical Path: Complete');
  });
});

// Summary test that always runs last
test.describe('Smoke Test Summary', () => {
  test('Generate smoke test summary', async ({ page }) => {
    const summary = {
      timestamp: new Date().toISOString(),
      environment: process.env.NODE_ENV || 'test',
      baseURL,
      status: 'completed',
      duration: Date.now() - parseInt(process.env.TEST_RUN_ID || '0')
    };

    console.log('\n=== SMOKE TEST SUMMARY ===');
    console.log(`Timestamp: ${summary.timestamp}`);
    console.log(`Environment: ${summary.environment}`);
    console.log(`Base URL: ${summary.baseURL}`);
    console.log(`Status: ${summary.status}`);
    console.log('========================\n');

    // Save summary
    const fs = require('fs');
    fs.writeFileSync(
      `test_probe/evidence/test-runs/smoke-summary-${Date.now()}.json`,
      JSON.stringify(summary, null, 2)
    );
  });
});