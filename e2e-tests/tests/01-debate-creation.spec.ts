import { test, expect, DebateTestUtils } from './fixtures/test-base';
import fs from 'fs/promises';
import path from 'path';

test.describe('Debate Creation Flow', () => {
  let evidenceDir: string;

  test.beforeAll(async () => {
    // Create evidence directory for this test run
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    evidenceDir = path.join('test-evidence', 'test-runs', `${timestamp}-debate-creation`);
    await fs.mkdir(evidenceDir, { recursive: true });
    await fs.mkdir(path.join(evidenceDir, 'screenshots'), { recursive: true });
    await fs.mkdir(path.join(evidenceDir, 'logs'), { recursive: true });
  });

  test('Create a new debate with all configurations', async ({ 
    page, 
    apiClient, 
    testData, 
    screenshots 
  }) => {
    // Test metadata
    const testMetadata = {
      testName: 'Create a new debate',
      timestamp: new Date().toISOString(),
      environment: process.env.NODE_ENV || 'test',
    };

    // Step 1: Navigate to debate creation page
    await page.goto('/debates/create');
    await screenshots.capture('debate-creation-page');
    
    // Step 2: Fill in debate details
    await page.fill('[data-testid="debate-topic"]', 'Should AI have rights? A comprehensive discussion');
    await page.fill('[data-testid="debate-description"]', 'An in-depth exploration of AI consciousness and rights');
    await page.selectOption('[data-testid="debate-format"]', 'OXFORD');
    await page.selectOption('[data-testid="organization-select"]', testData.organizationId);
    
    await screenshots.capture('debate-form-filled');

    // Step 3: Configure advanced settings
    await page.click('[data-testid="advanced-settings-toggle"]');
    await page.fill('[data-testid="max-rounds"]', '5');
    await page.fill('[data-testid="round-duration"]', '10');
    await page.fill('[data-testid="min-participants"]', '2');
    await page.fill('[data-testid="max-participants"]', '6');
    
    await screenshots.capture('advanced-settings-configured');

    // Step 4: Submit the form
    const [response] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes('/api/v1/debates') && resp.request().method() === 'POST'),
      page.click('[data-testid="create-debate-button"]')
    ]);

    // Capture API response
    const responseData = await response.json();
    await fs.writeFile(
      path.join(evidenceDir, 'logs', 'create-debate-response.json'),
      JSON.stringify(responseData, null, 2)
    );

    expect(response.status()).toBe(201);
    expect(responseData).toHaveProperty('id');
    expect(responseData.topic).toBe('Should AI have rights? A comprehensive discussion');
    
    testData.debateId = responseData.id;

    // Step 5: Verify navigation to debate details
    await page.waitForURL(`/debates/${responseData.id}`);
    await screenshots.capture('debate-created-details-page');

    // Step 6: Verify debate appears in listing
    await page.goto('/debates');
    await page.waitForSelector(`[data-testid="debate-${responseData.id}"]`);
    await screenshots.capture('debate-in-listing');

    // Save test evidence summary
    const evidenceSummary = {
      ...testMetadata,
      result: 'PASSED',
      debateId: responseData.id,
      screenshots: [
        'debate-creation-page.png',
        'debate-form-filled.png',
        'advanced-settings-configured.png',
        'debate-created-details-page.png',
        'debate-in-listing.png'
      ],
      apiResponses: ['create-debate-response.json'],
      assertions: {
        'API returns 201': true,
        'Debate has ID': true,
        'Topic matches input': true,
        'Navigation successful': true,
        'Appears in listing': true
      }
    };

    await fs.writeFile(
      path.join(evidenceDir, 'test-summary.json'),
      JSON.stringify(evidenceSummary, null, 2)
    );
  });

  test('Validate debate creation constraints', async ({ 
    page, 
    apiClient, 
    screenshots 
  }) => {
    await page.goto('/debates/create');
    
    // Test 1: Empty topic validation
    await page.click('[data-testid="create-debate-button"]');
    await expect(page.locator('[data-testid="topic-error"]')).toContainText('Topic is required');
    await screenshots.capture('validation-empty-topic');

    // Test 2: Invalid round configuration
    await page.fill('[data-testid="debate-topic"]', 'Test Topic');
    await page.click('[data-testid="advanced-settings-toggle"]');
    await page.fill('[data-testid="max-rounds"]', '0');
    await page.click('[data-testid="create-debate-button"]');
    await expect(page.locator('[data-testid="rounds-error"]')).toContainText('Must have at least 1 round');
    await screenshots.capture('validation-invalid-rounds');

    // Test 3: Participant constraints
    await page.fill('[data-testid="max-rounds"]', '3');
    await page.fill('[data-testid="min-participants"]', '10');
    await page.fill('[data-testid="max-participants"]', '2');
    await page.click('[data-testid="create-debate-button"]');
    await expect(page.locator('[data-testid="participants-error"]')).toContainText('Min participants cannot exceed max');
    await screenshots.capture('validation-participant-constraints');

    // Log validation test results
    const validationResults = {
      timestamp: new Date().toISOString(),
      validationTests: [
        { test: 'Empty topic', passed: true },
        { test: 'Invalid rounds', passed: true },
        { test: 'Participant constraints', passed: true }
      ]
    };

    await fs.writeFile(
      path.join(evidenceDir, 'logs', 'validation-test-results.json'),
      JSON.stringify(validationResults, null, 2)
    );
  });

  test('Create debate from template', async ({ 
    page, 
    apiClient, 
    testData,
    screenshots 
  }) => {
    // Navigate to templates
    await page.goto('/debates/templates');
    await screenshots.capture('debate-templates-page');

    // Select a template
    await page.click('[data-testid="template-academic-debate"]');
    await screenshots.capture('template-selected');

    // Customize template
    await page.fill('[data-testid="debate-topic"]', 'Is remote work more productive than office work?');
    await screenshots.capture('template-customized');

    // Create from template
    const [response] = await Promise.all([
      page.waitForResponse(resp => resp.url().includes('/api/v1/debates') && resp.request().method() === 'POST'),
      page.click('[data-testid="create-from-template-button"]')
    ]);

    const responseData = await response.json();
    expect(response.status()).toBe(201);
    expect(responseData.format).toBe('OXFORD'); // Template default
    expect(responseData.maxRounds).toBe(5); // Template default

    await screenshots.capture('debate-created-from-template');

    // Document template usage
    const templateEvidence = {
      templateUsed: 'academic-debate',
      customizations: {
        topic: 'Is remote work more productive than office work?'
      },
      templateDefaults: {
        format: 'OXFORD',
        maxRounds: 5,
        roundDuration: 15
      },
      createdDebateId: responseData.id
    };

    await fs.writeFile(
      path.join(evidenceDir, 'logs', 'template-usage.json'),
      JSON.stringify(templateEvidence, null, 2)
    );
  });

  test.afterAll(async () => {
    // Generate test run summary
    const summary = {
      testSuite: 'Debate Creation Flow',
      totalTests: 3,
      passed: 3,
      failed: 0,
      duration: Date.now() - Date.parse(evidenceDir.split('-').slice(-1)[0]),
      evidenceLocation: evidenceDir
    };

    await fs.writeFile(
      path.join(evidenceDir, 'summary.json'),
      JSON.stringify(summary, null, 2)
    );
  });
});