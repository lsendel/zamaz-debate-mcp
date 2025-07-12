import { defineConfig, devices } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

// Load environment variables
dotenv.config({ path: '../.env' });

/**
 * Comprehensive test configuration for AI Debate System
 * Focused on thorough testing with evidence collection
 */
export default defineConfig({
  testDir: './tests',
  /* Run tests sequentially for LLM stability */
  fullyParallel: false,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry failed tests to handle transient issues */
  retries: process.env.CI ? 2 : 1,
  /* Limited workers for resource management */
  workers: process.env.CI ? 1 : 2,
  /* Comprehensive reporter configuration */
  reporter: [
    ['html', { 
      outputFolder: path.join(__dirname, '../test_probe/evidence/html-report'),
      open: 'never'
    }],
    ['json', {
      outputFile: path.join(__dirname, '../test_probe/evidence/test-results.json')
    }],
    ['junit', { 
      outputFile: path.join(__dirname, '../test_probe/evidence/junit-results.xml') 
    }],
    ['list'],
    ['line'],
    // Custom reporter for detailed evidence collection
    ['./reporters/evidence-reporter.ts']
  ],
  /* Shared settings for all the projects below */
  use: {
    /* Base URL to use in actions */
    baseURL: process.env.BASE_URL || 'http://localhost:3000',

    /* Comprehensive trace collection */
    trace: 'retain-on-failure',

    /* Screenshot configuration for evidence */
    screenshot: {
      mode: 'only-on-failure',
      fullPage: true
    },

    /* Video recording for all tests */
    video: {
      mode: 'on',
      size: { width: 1280, height: 720 }
    },

    /* Extended timeouts for LLM operations */
    actionTimeout: 30000,
    navigationTimeout: 60000,

    /* Custom test attributes */
    testIdAttribute: 'data-testid',

    /* Browser context options */
    contextOptions: {
      recordVideo: {
        dir: path.join(__dirname, '../test_probe/evidence/videos')
      }
    },

    /* Viewport size */
    viewport: { width: 1280, height: 720 },
  },

  /* Configure specialized test projects */
  projects: [
    // Setup project to ensure services are ready
    {
      name: 'setup',
      testMatch: /global-setup\.spec\.ts/,
    },
    
    // Main browser for comprehensive tests
    {
      name: 'chromium',
      use: { 
        ...devices['Desktop Chrome'],
        // Save artifacts to project-specific directory
        screenshot: {
          mode: 'on',
          fullPage: true
        },
      },
    },

    // Comprehensive debate testing project
    {
      name: 'comprehensive-debate-tests',
      testDir: './tests/comprehensive',
      testMatch: /.*comprehensive-debate.*\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        // Extended timeouts for LLM operations
        actionTimeout: 60000,
        navigationTimeout: 120000,
      },
      timeout: 300000, // 5 minutes per test
      retries: 2,
    },

    // LLM integration testing project
    {
      name: 'llm-integration',
      testDir: './tests/llm',
      testMatch: /.*llm-.*\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        // Specific settings for LLM tests
        launchOptions: {
          slowMo: 100, // Slow down for LLM response times
        },
      },
      timeout: 240000, // 4 minutes per test
    },

    // Database verification project
    {
      name: 'database-verification',
      testDir: './tests/database',
      testMatch: /.*database.*\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
      },
      timeout: 120000, // 2 minutes per test
    },

    // UI component testing project
    {
      name: 'ui-components',
      testDir: './tests/ui',
      testMatch: /.*ui-.*\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        // Fast timeouts for UI tests
        actionTimeout: 10000,
        navigationTimeout: 20000,
      },
      timeout: 60000, // 1 minute per test
    },

    // Performance testing project
    {
      name: 'performance',
      testDir: './tests/performance',
      testMatch: /.*performance.*\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        // Performance specific settings
        launchOptions: {
          args: ['--enable-precise-memory-info'],
        },
      },
    },
  ],

  /* Run your local dev server before starting the tests */
  webServer: process.env.CI ? undefined : {
    command: 'cd ../debate-ui && npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 180 * 1000, // 3 minutes to start
  },

  /* Global setup */
  globalSetup: require.resolve('./global-setup'),
  
  /* Global teardown */
  globalTeardown: require.resolve('./global-teardown'),

  /* Default test timeout */
  timeout: 120 * 1000, // 2 minutes default

  /* Expect timeout */
  expect: {
    timeout: 30000, // 30 seconds for assertions
  },

  /* Output directory for test artifacts */
  outputDir: path.join(__dirname, '../test_probe/evidence/test-artifacts'),

  /* Preserve output between test runs */
  preserveOutput: 'failures-only',
});