import { config } from '../config';

export class ApiHelpers {
  static async waitForService(url: string, maxRetries = 30): Promise<void> {
    for (let i = 0; i < maxRetries; i++) {
      try {
        const response = await fetch(`${url}/health`);
        if (response.ok) {
          console.log(`Service at ${url} is ready`);
          return;
        }
      } catch (error) {
        // Service not ready yet
      }
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
    throw new Error(`Service at ${url} failed to start after ${maxRetries} seconds`);
  }

  static async waitForAllServices(): Promise<void> {
    console.log('Waiting for all services to be ready...');
    await Promise.all([
      this.waitForService(config.UI_URL),
      this.waitForService(config.MCP_CONTEXT_URL),
      this.waitForService(config.MCP_LLM_URL),
      this.waitForService(config.MCP_DEBATE_URL),
      this.waitForService(config.MCP_RAG_URL)
    ]);
    console.log('All services are ready!');
  }

  static async createTestOrganization(): Promise<void> {
    const response = await fetch(`${config.MCP_CONTEXT_URL}/organizations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Organization-ID': config.TEST_ORG_ID
      },
      body: JSON.stringify({
        id: config.TEST_ORG_ID,
        name: 'Test Organization'
      })
    });
    
    if (!response.ok && response.status !== 409) {
      throw new Error('Failed to create test organization');
    }
  }

  static async cleanupTestData(): Promise<void> {
    // Clean up test debates
    try {
      const response = await fetch(`${config.MCP_DEBATE_URL}/debates?organization_id=${config.TEST_ORG_ID}`);
      if (response.ok) {
        const debates = await response.json();
        for (const debate of debates) {
          await fetch(`${config.MCP_DEBATE_URL}/debates/${debate.id}`, {
            method: 'DELETE',
            headers: {
              'X-Organization-ID': config.TEST_ORG_ID
            }
          });
        }
      }
    } catch (error) {
      console.warn('Failed to cleanup debates:', error);
    }
  }

  static async verifyMCPConnection(serviceName: string, serviceUrl: string): Promise<boolean> {
    try {
      const response = await fetch(`${serviceUrl}/mcp/list-resources`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Organization-ID': config.TEST_ORG_ID
        },
        body: JSON.stringify({})
      });
      
      if (response.ok) {
        const data = await response.json();
        console.log(`MCP connection verified for ${serviceName}:`, data.resources?.length || 0, 'resources');
        return true;
      }
    } catch (error) {
      console.error(`Failed to verify MCP connection for ${serviceName}:`, error);
    }
    return false;
  }
}