import { test, expect } from '@playwright/test';

/**
 * Global Setup Test
 * This test runs before all other tests to ensure the environment is ready
 */

test('Verify test environment is ready', async ({ page }) => {
  console.log('\n🚀 Verifying test environment...');
  
  const services = [
    { name: 'UI', url: 'http://localhost:3000', required: true },
    { name: 'LLM Service', url: 'http://localhost:5002/health', required: true },
    { name: 'Debate Service', url: 'http://localhost:5013/health', required: true },
    { name: 'Context Service', url: 'http://localhost:5001/health', required: false },
    { name: 'RAG Service', url: 'http://localhost:5004/health', required: false }
  ];
  
  const serviceStatus = [];
  
  for (const service of services) {
    try {
      const response = await page.request.get(service.url, { timeout: 5000 });
      const isHealthy = response.ok();
      serviceStatus.push({
        ...service,
        status: isHealthy ? 'healthy' : 'unhealthy',
        statusCode: response.status()
      });
      
      if (service.required && !isHealthy) {
        throw new Error(`Required service ${service.name} is not healthy`);
      }
      
      console.log(`${isHealthy ? '✅' : '⚠️'} ${service.name}: ${response.status()}`);
    } catch (error) {
      serviceStatus.push({
        ...service,
        status: 'unavailable',
        error: error.message
      });
      
      if (service.required) {
        throw new Error(`Required service ${service.name} is not available: ${error.message}`);
      }
      
      console.log(`${service.required ? '❌' : '⚠️'} ${service.name}: Unavailable`);
    }
  }
  
  // Save service status
  const fs = require('fs');
  fs.writeFileSync(
    `test_probe/evidence/test-runs/service-status-${Date.now()}.json`,
    JSON.stringify(serviceStatus, null, 2)
  );
  
  console.log('\n✅ Test environment is ready!\n');
});