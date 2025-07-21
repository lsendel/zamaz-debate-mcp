const { test, expect } = require('@playwright/test');

// Configuration
const BASE_URL = process.env.BASE_URL || 'http://localhost:3001';
const API_URL = process.env.API_URL || 'http://localhost:5013';

// Test data
const testDebate = {
  topic: 'E2E Test: Impact of AI on Education',
  format: 'OXFORD',
  rounds: 3,
  participants: [
    {
      name: 'AI Advocate',
      type: 'AI',
      provider: 'OpenAI',
      model: 'gpt-4'
    },
    {
      name: 'AI Skeptic',
      type: 'AI',
      provider: 'Claude',
      model: 'claude-3'
    }
  ]
};

const testFlow = {
  name: 'E2E Test Flow',
  flowType: 'TREE_OF_THOUGHTS',
  description: 'Test flow for E2E testing',
  configuration: {
    maxDepth: 2,
    branchingFactor: 2,
    evaluationMetric: 'coherence'
  }
};

test.describe('Agentic Flows E2E Tests', () => {
  let authToken;
  let debateId;
  let flowId;

  test.beforeAll(async ({ request }) => {
    // Login and get auth token
    const loginResponse = await request.post(`${BASE_URL}/api/v1/auth/login`, {
      data: {
        username: 'demo',
        password: 'demo123'
      }
    });
    
    expect(loginResponse.ok()).toBeTruthy();
    const loginData = await loginResponse.json();
    authToken = loginData.token;
  });

  test('should create a debate with agentic flows', async ({ page, request }) => {
    // Navigate to debates page
    await page.goto(`${BASE_URL}/debates`);
    
    // Set auth token in localStorage
    await page.evaluate((token) => {
      localStorage.setItem('accessToken', token);
    }, authToken);
    
    // Click create debate button
    await page.click('button:has-text("Create Debate")');
    
    // Fill debate form
    await page.fill('input[name="topic"]', testDebate.topic);
    await page.selectOption('select[name="format"]', testDebate.format);
    await page.fill('input[name="rounds"]', testDebate.rounds.toString());
    
    // Add participants
    for (const participant of testDebate.participants) {
      await page.click('button:has-text("Add Participant")');
      await page.fill('input[name="participantName"]', participant.name);
      await page.selectOption('select[name="participantType"]', participant.type);
      await page.selectOption('select[name="provider"]', participant.provider);
      await page.selectOption('select[name="model"]', participant.model);
    }
    
    // Create debate
    await page.click('button:has-text("Create")');
    
    // Wait for navigation to debate detail page
    await page.waitForURL(/\/debates\/[a-f0-9-]+/);
    
    // Extract debate ID from URL
    const url = page.url();
    debateId = url.match(/debates\/([a-f0-9-]+)/)[1];
    
    // Verify debate was created
    await expect(page.locator('h1')).toContainText(testDebate.topic);
  });

  test('should configure agentic flow for debate', async ({ page }) => {
    // Navigate to debate
    await page.goto(`${BASE_URL}/debates/${debateId}`);
    
    // Open flow configuration
    await page.click('button:has-text("Configure Flows")');
    
    // Wait for modal
    await page.waitForSelector('.ant-modal');
    
    // Click create flow
    await page.click('button:has-text("Create Flow")');
    
    // Fill flow form
    await page.fill('input[name="name"]', testFlow.name);
    await page.fill('textarea[name="description"]', testFlow.description);
    
    // Select flow type
    await page.click('.ant-select-selector');
    await page.click(`div.ant-select-item:has-text("${testFlow.flowType}")`);
    
    // Configure flow parameters
    await page.fill('input[name="maxDepth"]', testFlow.configuration.maxDepth.toString());
    await page.fill('input[name="branchingFactor"]', testFlow.configuration.branchingFactor.toString());
    
    // Assign to first participant
    await page.check('input[type="checkbox"]:first-of-type');
    
    // Save flow
    await page.click('button:has-text("Save")');
    
    // Verify flow was created
    await expect(page.locator('.flow-list')).toContainText(testFlow.name);
    
    // Close modal
    await page.click('.ant-modal-close');
  });

  test('should start debate with agentic flow', async ({ page }) => {
    // Navigate to debate
    await page.goto(`${BASE_URL}/debates/${debateId}`);
    
    // Start debate
    await page.click('button:has-text("Start Debate")');
    
    // Wait for first response with flow execution
    await page.waitForSelector('.participant-response', { timeout: 60000 });
    
    // Verify flow indicator is present
    await expect(page.locator('.flow-indicator')).toBeVisible();
    
    // Verify confidence score is displayed
    await expect(page.locator('.confidence-score')).toContainText('%');
    
    // Click to view flow details
    await page.click('button:has-text("View Flow Details")');
    
    // Verify flow visualization is displayed
    await expect(page.locator('.flow-visualization')).toBeVisible();
    await expect(page.locator('.thought-tree')).toBeVisible();
  });

  test('should display flow analytics', async ({ page }) => {
    // Navigate to debate
    await page.goto(`${BASE_URL}/debates/${debateId}`);
    
    // Open analytics
    await page.click('button:has-text("Flow Analytics")');
    
    // Wait for analytics to load
    await page.waitForSelector('.analytics-dashboard');
    
    // Verify analytics components
    await expect(page.locator('.average-confidence')).toBeVisible();
    await expect(page.locator('.execution-time-chart')).toBeVisible();
    await expect(page.locator('.flow-type-distribution')).toBeVisible();
    
    // Verify data is present
    const avgConfidence = await page.locator('.average-confidence .value').textContent();
    expect(parseFloat(avgConfidence)).toBeGreaterThan(0);
  });

  test('should handle flow execution errors gracefully', async ({ page, request }) => {
    // Create a flow with invalid configuration
    const invalidFlow = {
      name: 'Invalid Flow',
      flowType: 'TREE_OF_THOUGHTS',
      configuration: {
        maxDepth: -1, // Invalid value
        branchingFactor: 0 // Invalid value
      }
    };
    
    // Attempt to create flow via API
    const response = await request.post(
      `${API_URL}/api/v1/debates/${debateId}/agentic-flows`,
      {
        data: invalidFlow,
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      }
    );
    
    // Should return error
    expect(response.status()).toBe(400);
    const error = await response.json();
    expect(error.message).toContain('Invalid configuration');
  });

  test('should export flow results', async ({ page }) => {
    // Navigate to debate
    await page.goto(`${BASE_URL}/debates/${debateId}`);
    
    // Wait for responses
    await page.waitForSelector('.participant-response');
    
    // Open flow details
    await page.click('button:has-text("View Flow Details"):first-of-type');
    
    // Click export button
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.click('button:has-text("Export")')
    ]);
    
    // Verify download
    expect(download.suggestedFilename()).toContain('flow-result');
    expect(download.suggestedFilename()).toContain('.json');
  });

  test('should handle real-time flow updates', async ({ page }) => {
    // Navigate to debate
    await page.goto(`${BASE_URL}/debates/${debateId}`);
    
    // Continue debate
    await page.click('button:has-text("Continue")');
    
    // Wait for processing indicator
    await page.waitForSelector('.flow-processing');
    
    // Verify real-time updates
    await expect(page.locator('.flow-status')).toContainText('Processing');
    
    // Wait for completion
    await page.waitForSelector('.flow-status:has-text("Completed")', { timeout: 60000 });
    
    // Verify result is displayed
    await expect(page.locator('.confidence-score')).toBeVisible();
  });

  test('should show flow recommendations', async ({ page }) => {
    // Navigate to debate
    await page.goto(`${BASE_URL}/debates/${debateId}`);
    
    // Open flow configuration
    await page.click('button:has-text("Configure Flows")');
    
    // View recommendations
    await page.click('button:has-text("View Recommendations")');
    
    // Wait for recommendations
    await page.waitForSelector('.flow-recommendations');
    
    // Verify recommendations are displayed
    const recommendations = await page.locator('.recommendation-item').count();
    expect(recommendations).toBeGreaterThan(0);
    
    // Apply a recommendation
    await page.click('button:has-text("Apply"):first-of-type');
    
    // Verify flow was created from recommendation
    await expect(page.locator('.ant-message')).toContainText('Flow created');
  });

  test('should compare flow performance', async ({ page }) => {
    // Navigate to analytics
    await page.goto(`${BASE_URL}/debates/${debateId}`);
    await page.click('button:has-text("Flow Analytics")');
    
    // Switch to comparison view
    await page.click('button:has-text("Compare Flows")');
    
    // Select flows to compare
    await page.check('input[type="checkbox"]:nth-of-type(1)');
    await page.check('input[type="checkbox"]:nth-of-type(2)');
    
    // View comparison
    await page.click('button:has-text("Compare Selected")');
    
    // Verify comparison chart
    await expect(page.locator('.flow-comparison-chart')).toBeVisible();
    await expect(page.locator('.comparison-metrics')).toBeVisible();
  });

  test('should handle concurrent flow executions', async ({ request }) => {
    // Create multiple flow execution requests
    const promises = [];
    
    for (let i = 0; i < 5; i++) {
      const promise = request.post(
        `${API_URL}/api/v1/agentic-flows/${flowId}/execute`,
        {
          data: {
            prompt: `Test prompt ${i}`,
            context: { debateId, round: 1 }
          },
          headers: {
            'Authorization': `Bearer ${authToken}`
          }
        }
      );
      promises.push(promise);
    }
    
    // Execute concurrently
    const responses = await Promise.all(promises);
    
    // All should succeed
    responses.forEach(response => {
      expect(response.ok()).toBeTruthy();
    });
    
    // Verify results
    const results = await Promise.all(
      responses.map(r => r.json())
    );
    
    results.forEach(result => {
      expect(result.status).toBe('SUCCESS');
      expect(result.confidence).toBeGreaterThan(0);
    });
  });

  test.afterAll(async ({ request }) => {
    // Cleanup - delete test debate
    if (debateId) {
      await request.delete(`${API_URL}/api/v1/debates/${debateId}`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      });
    }
  });
});

// Performance tests
test.describe('Agentic Flows Performance Tests', () => {
  test('should handle high-load flow executions', async ({ request }) => {
    // Login first
    const loginResponse = await request.post(`${BASE_URL}/api/v1/auth/login`, {
      data: { username: 'demo', password: 'demo123' }
    });
    const { token } = await loginResponse.json();
    
    // Measure execution times
    const executionTimes = [];
    const concurrentRequests = 20;
    
    const startTime = Date.now();
    
    // Create requests
    const promises = Array(concurrentRequests).fill(null).map((_, i) => 
      request.post(`${API_URL}/api/v1/agentic-flows/execute-test`, {
        data: {
          flowType: 'INTERNAL_MONOLOGUE',
          prompt: `Performance test prompt ${i}`,
          configuration: { prefix: 'Test:' }
        },
        headers: { 'Authorization': `Bearer ${token}` }
      })
    );
    
    // Execute and measure
    const responses = await Promise.all(promises);
    const endTime = Date.now();
    
    // Calculate metrics
    const totalTime = endTime - startTime;
    const avgTime = totalTime / concurrentRequests;
    
    console.log(`Performance Test Results:
      Total requests: ${concurrentRequests}
      Total time: ${totalTime}ms
      Average time per request: ${avgTime}ms
      Requests per second: ${(concurrentRequests / (totalTime / 1000)).toFixed(2)}
    `);
    
    // Assertions
    expect(avgTime).toBeLessThan(5000); // Average should be under 5 seconds
    
    // All requests should succeed
    responses.forEach(response => {
      expect(response.ok()).toBeTruthy();
    });
  });
});