import { chromium, FullConfig } from '@playwright/test';
import fs from 'fs';
import path from 'path';

/**
 * Global setup for all Playwright tests
 * Ensures services are ready and test environment is prepared
 */
async function globalSetup(config: FullConfig) {
  console.log('🚀 Starting global test setup...');
  
  // Ensure evidence directories exist
  const evidenceBase = path.join(__dirname, '../test_probe/evidence');
  const directories = [
    evidenceBase,
    path.join(evidenceBase, 'screenshots'),
    path.join(evidenceBase, 'videos'),
    path.join(evidenceBase, 'traces'),
    path.join(evidenceBase, 'logs'),
    path.join(evidenceBase, 'debate-transcripts'),
    path.join(evidenceBase, 'performance-metrics'),
    path.join(evidenceBase, 'database-snapshots'),
    path.join(evidenceBase, 'test-runs'),
    path.join(evidenceBase, 'html-report'),
    path.join(evidenceBase, 'test-artifacts')
  ];
  
  directories.forEach(dir => {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
      console.log(`✅ Created directory: ${dir}`);
    }
  });
  
  // Create test run metadata
  const testRunInfo = {
    id: Date.now().toString(),
    startTime: new Date().toISOString(),
    environment: {
      nodeVersion: process.version,
      platform: process.platform,
      baseUrl: process.env.BASE_URL || 'http://localhost:3000',
      llmProviders: {
        claude: !!process.env.ANTHROPIC_API_KEY,
        openai: !!process.env.OPENAI_API_KEY,
        gemini: !!process.env.GOOGLE_API_KEY
      }
    }
  };
  
  fs.writeFileSync(
    path.join(evidenceBase, 'test-runs', `setup-${testRunInfo.id}.json`),
    JSON.stringify(testRunInfo, null, 2)
  );
  
  // Check service health
  console.log('🏥 Checking service health...');
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();
  
  const services = [
    { name: 'UI', url: process.env.BASE_URL || 'http://localhost:3000' },
    { name: 'LLM Service', url: 'http://localhost:5002/health' },
    { name: 'Debate Service', url: 'http://localhost:5013/health' }
  ];
  
  const healthResults = [];
  
  for (const service of services) {
    let isReady = false;
    for (let i = 0; i < 30; i++) {
      try {
        const response = await page.request.get(service.url, { timeout: 10000 });
        const isHealthy = response.ok();
        const data = isHealthy ? await response.json().catch(() => ({})) : {};
        
        if (isHealthy) {
          healthResults.push({
            service: service.name,
            url: service.url,
            healthy: isHealthy,
            status: response.status(),
            data
          });
          
          console.log(`✅ ${service.name}: ${response.status()}`);
          isReady = true;
          break;
        }
      } catch (error) {
        // Service not ready yet
      }
      
      if (i < 29) {
        await new Promise(resolve => setTimeout(resolve, 2000));
      }
    }
    
    if (!isReady) {
      healthResults.push({
        service: service.name,
        url: service.url,
        healthy: false,
        error: 'Service failed to become ready'
      });
      console.log(`❌ ${service.name}: Failed to connect`);
    }
  }
  
  // Save health check results
  fs.writeFileSync(
    path.join(evidenceBase, 'test-runs', `health-check-${testRunInfo.id}.json`),
    JSON.stringify(healthResults, null, 2)
  );
  
  // Verify at least critical services are running
  const criticalServicesHealthy = healthResults
    .filter(r => ['LLM Service', 'Debate Service'].includes(r.service))
    .every(r => r.healthy);
  
  if (!criticalServicesHealthy) {
    console.error('❌ Critical services are not healthy!');
    console.log('Please ensure all services are running with: docker-compose up -d');
    throw new Error('Critical services not available');
  }
  
  await browser.close();
  
  console.log('✅ Global setup completed successfully');
  
  // Store test run ID for use in tests
  process.env.TEST_RUN_ID = testRunInfo.id;
  
  // Set up test data if needed
  await setupTestData();
  
  return () => {
    console.log('Global teardown will be executed after all tests');
  };
}

async function setupTestData() {
  // Create default test organization if needed
  try {
    console.log('📊 Setting up test data...');
    
    // You can add API calls here to set up test data
    // For example:
    // - Create test organizations
    // - Create test templates
    // - Create test users
    
    console.log('✅ Test data setup complete');
  } catch (error) {
    console.error('❌ Failed to setup test data:', error);
  }
}

export default globalSetup;