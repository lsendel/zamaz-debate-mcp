import { test as base, expect } from '@playwright/test';
import axios from 'axios';
import WebSocket from 'ws';
import { v4 as uuidv4 } from 'crypto';

// Define custom fixtures
type TestFixtures = {
  apiClient: typeof axios;
  wsClient: WebSocket | null;
  testData: {
    organizationId: string;
    userId: string;
    debateId: string | null;
    authToken: string | null;
  };
  screenshots: {
    capture: (name: string) => Promise<void>;
  };
};

export const test = base.extend<TestFixtures>({
  apiClient: async ({}, use) => {
    const client = axios.create({
      baseURL: process.env.API_BASE_URL || 'http://localhost:8080/api/v1',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    await use(client);
  },

  wsClient: async ({}, use) => {
    let ws: WebSocket | null = null;
    await use(ws);
    
    // Cleanup
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.close();
    }
  },

  testData: async ({}, use) => {
    const data = {
      organizationId: process.env.TEST_ORG_ID || 'test-org-' + uuidv4(),
      userId: 'test-user-' + uuidv4(),
      debateId: null,
      authToken: null,
    };
    
    await use(data);
  },

  screenshots: async ({ page }, use) => {
    const screenshotDir = 'test-results/screenshots';
    let screenshotCounter = 0;
    
    const capture = async (name: string) => {
      screenshotCounter++;
      const filename = `${screenshotCounter.toString().padStart(3, '0')}-${name}.png`;
      await page.screenshot({ 
        path: `${screenshotDir}/${filename}`,
        fullPage: true 
      });
      console.log(`Screenshot captured: ${filename}`);
    };
    
    await use({ capture });
  },
});

export { expect };

// Utility functions for common operations
export class DebateTestUtils {
  static async createOrganization(apiClient: typeof axios, name: string) {
    try {
      const response = await apiClient.post('/organizations', {
        name,
        description: 'Test organization for E2E tests',
        type: 'EDUCATIONAL',
      });
      return response.data;
    } catch (error) {
      console.error('Failed to create organization:', error);
      throw error;
    }
  }

  static async createDebate(apiClient: typeof axios, organizationId: string, topic: string) {
    try {
      const response = await apiClient.post('/debates', {
        topic,
        description: 'E2E test debate',
        format: 'OXFORD',
        organizationId,
        maxRounds: 3,
        roundDurationMinutes: 5,
        minParticipants: 2,
        maxParticipants: 4,
      });
      return response.data;
    } catch (error) {
      console.error('Failed to create debate:', error);
      throw error;
    }
  }

  static async connectWebSocket(debateId: string, token: string): Promise<WebSocket> {
    return new Promise((resolve, reject) => {
      const wsUrl = `ws://localhost:8087/api/v1/debates/${debateId}/ws`;
      const ws = new WebSocket(wsUrl, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      ws.on('open', () => {
        console.log('WebSocket connected');
        resolve(ws);
      });

      ws.on('error', (error) => {
        console.error('WebSocket error:', error);
        reject(error);
      });

      ws.on('message', (data) => {
        console.log('WebSocket message:', data.toString());
      });
    });
  }

  static async waitForWebSocketMessage(ws: WebSocket, messageType: string, timeout = 10000): Promise<any> {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        reject(new Error(`Timeout waiting for message type: ${messageType}`));
      }, timeout);

      const messageHandler = (data: WebSocket.Data) => {
        try {
          const message = JSON.parse(data.toString());
          if (message.type === messageType) {
            clearTimeout(timer);
            ws.removeListener('message', messageHandler);
            resolve(message);
          }
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
        }
      };

      ws.on('message', messageHandler);
    });
  }

  static async generateAuthToken(apiClient: typeof axios, userId: string): Promise<string> {
    // In a real scenario, this would authenticate with the auth service
    // For testing, we'll use a mock token or request from auth endpoint
    try {
      const response = await apiClient.post('/auth/token', {
        userId,
        scope: 'debate:full',
      });
      return response.data.token;
    } catch (error) {
        console.error("Error:", error);
      console.log('Using mock token for testing');
      return 'mock-jwt-token-' + userId;
      console.error("Error:", error);
    }
  }
}