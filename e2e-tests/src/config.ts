export const config = {
  // Base URLs for services
  UI_URL: process.env.UI_URL || 'http://localhost:3000',
  MCP_CONTEXT_URL: process.env.MCP_CONTEXT_URL || 'http://localhost:8001',
  MCP_LLM_URL: process.env.MCP_LLM_URL || 'http://localhost:8002',
  MCP_DEBATE_URL: process.env.MCP_DEBATE_URL || 'http://localhost:8003',
  MCP_RAG_URL: process.env.MCP_RAG_URL || 'http://localhost:8004',
  
  // Test configuration
  HEADLESS: process.env.HEADLESS !== 'false',
  SLOWMO: parseInt(process.env.SLOWMO || '0'),
  TIMEOUT: 30000,
  
  // Test data
  TEST_ORG_ID: 'test-org-123',
  TEST_USER_ID: 'test-user-456',
  
  // Puppeteer options
  PUPPETEER_OPTIONS: {
    headless: process.env.HEADLESS !== 'false',
    slowMo: parseInt(process.env.SLOWMO || '0'),
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-accelerated-2d-canvas',
      '--disable-gpu'
    ],
    defaultViewport: {
      width: 1280,
      height: 800
    }
  }
};