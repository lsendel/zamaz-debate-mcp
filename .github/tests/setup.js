// Global test setup
const core = require('@actions/core');

// Suppress console output during tests unless debugging
if (!process.env.DEBUG) {
  global.console = {
    ...console,
    log: jest.fn(),
    error: jest.fn(),
    warn: jest.fn(),
    info: jest.fn(),
    debug: jest.fn()
  };
}

// Set up common environment variables
process.env.GITHUB_REPOSITORY = 'test-owner/test-repo';
process.env.GITHUB_TOKEN = 'test-token';
process.env.GITHUB_RUN_ID = '123456';
process.env.GITHUB_RUN_NUMBER = '42';

// Reset mocks before each test
beforeEach(() => {
  jest.clearAllMocks();
});