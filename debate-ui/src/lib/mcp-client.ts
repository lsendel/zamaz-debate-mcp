// MCP Client for Browser/Frontend
import { logger } from '@/lib/logger';

export class MCPClient {
  private baseUrl: string;
  private wsUrl: string;
  private ws: WebSocket | null = null;
  private organizationId?: string;

  constructor(service: 'debate' | 'llm' | 'context' | 'rag') {
    const ports: Record<string, number> = {
      debate: 5013,
      llm: 5002,
      context: 5001,
      rag: 5004
    };
    
    // Validate service parameter to prevent object injection
    if (!Object.hasOwn(ports, service)) {
      throw new Error(`Invalid service: ${service}`);
    }
    
    // Use the Next.js API routes that proxy to the services
    this.baseUrl = `/api/${service}`;
    // eslint-disable-next-line security/detect-object-injection
    this.wsUrl = `ws://localhost:${ports[service]}/ws`;
    
    // Get organization from localStorage (only on client-side)
    if (typeof window !== 'undefined' && window.localStorage) {
      const savedOrgId = localStorage.getItem('currentOrganizationId');
      if (savedOrgId) {
        this.organizationId = savedOrgId;
      }
    }
  }

  setOrganizationId(orgId: string) {
    this.organizationId = orgId;
  }

  async listTools(): Promise<any[]> {
    const response = await fetch(`${this.baseUrl}/tools`);
    if (!response.ok) throw new Error('Failed to list tools');
    return response.json();
  }

  async callTool(toolName: string, args: any): Promise<any> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    
    // Add organization header if available
    if (this.organizationId) {
      headers['X-Organization-ID'] = this.organizationId;
    }
    
    const response = await fetch(`${this.baseUrl}/tools/${toolName}`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ arguments: args }),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Tool call failed: ${error}`);
    }

    return response.json();
  }

  async readResource(uri: string): Promise<any> {
    const headers: Record<string, string> = {};
    
    // Add organization header if available
    if (this.organizationId) {
      headers['X-Organization-ID'] = this.organizationId;
    }
    
    logger.debug('Reading resource', { uri, organizationId: this.organizationId });
    
    const response = await fetch(`${this.baseUrl}/resources?uri=${encodeURIComponent(uri)}`, {
      headers
    });
    
    if (!response.ok) {
      logger.error('Failed to read resource', new Error(`HTTP ${response.status}`), { uri });
      throw new Error('Failed to read resource');
    }
    
    const data = await response.json();
    logger.debug('Resource data received', { uri, dataKeys: Object.keys(data) });
    
    return data;
  }

  connectWebSocket(onMessage: (_data: any) => void): void {
    this.ws = new WebSocket(this.wsUrl);
    
    this.ws.onopen = () => {
      logger.info('WebSocket connected');
    };

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        onMessage(data);
      } catch (e) {
        logger.error('Failed to parse WebSocket message', e as Error);
      }
    };

    this.ws.onerror = (_error) => {
      logger.error('WebSocket error', new Error('WebSocket connection failed'));
    };

    this.ws.onclose = () => {
      logger.info('WebSocket disconnected');
      // Attempt to reconnect after 5 seconds
      setTimeout(() => this.connectWebSocket(onMessage), 5000);
    };
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}

// Ollama-specific client for local LLM
export class OllamaClient {
  private baseUrl: string;

  constructor(baseUrl: string = 'http://localhost:11434') {
    this.baseUrl = baseUrl;
  }

  async listModels(): Promise<any[]> {
    const response = await fetch(`${this.baseUrl}/api/tags`);
    if (!response.ok) throw new Error('Failed to list Ollama models');
    const data = await response.json();
    return data.models || [];
  }

  async pullModel(modelName: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/api/pull`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ name: modelName }),
    });

    if (!response.ok) throw new Error('Failed to pull model');
    
    // Stream the response to show progress
    const reader = response.body?.getReader();
    if (!reader) return;

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      
      const text = new TextDecoder().decode(value);
      const lines = text.split('\n').filter(line => line.trim());
      
      for (const line of lines) {
        try {
          const data = JSON.parse(line);
          logger.debug('Pull progress', data);
        } catch (e) {
          // Ignore parse errors
        }
      }
    }
  }

  async generate(prompt: string, model: string = 'llama3'): Promise<string> {
    const response = await fetch(`${this.baseUrl}/api/generate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model,
        prompt,
        stream: false,
      }),
    });

    if (!response.ok) throw new Error('Failed to generate');
    const data = await response.json();
    return data.response;
  }
}