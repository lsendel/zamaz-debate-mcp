import BaseApiClient from './baseClient';

export interface LLMProvider {
  name: string;
  models: string[];
  features: {
    streaming: boolean;
    functionCalling: boolean;
    visionSupport: boolean;
    maxTokens: number;
  };
  status: 'available' | 'unavailable' | 'rate_limited';
}

export interface CompletionRequest {
  provider: string;
  model: string;
  messages: Array<{
    role: 'system' | 'user' | 'assistant';
    content: string;
  }>;
  temperature?: number;
  maxTokens?: number;
  topP?: number;
  stream?: boolean;
  stopSequences?: string[];
}

export interface CompletionResponse {
  content: string;
  model: string;
  usage?: {
    promptTokens: number;
    completionTokens: number;
    totalTokens: number;
  };
  finishReason?: string;
  cached?: boolean;
}

export interface ModelInfo {
  provider: string;
  model: string;
  description: string;
  contextWindow: number;
  pricing: {
    inputTokens: number;
    outputTokens: number;
  };
  capabilities: string[];
}

class LLMClient extends BaseApiClient {
  constructor() {
    super('/api/llm');
  }

  // Provider management
  async listProviders(): Promise<LLMProvider[]> {
    const resources = await this.listResources();
    return resources.filter((r: any) => r.uri.startsWith('provider://'));
  }

  async getProvider(name: string): Promise<LLMProvider> {
    return this.getResource(`provider://${name}`);
  }

  // Completion
  async complete(request: CompletionRequest): Promise<CompletionResponse> {
    return this.callTool('complete', request);
  }

  async streamComplete(
    request: CompletionRequest,
    onChunk: (chunk: string) => void,
    onComplete?: (response: CompletionResponse) => void
  ): Promise<void> {
    const response = await this.makeStreamRequest(request);
    const reader = this.getResponseReader(response);
    await this.processStream(reader, onChunk, onComplete);
  }

  private async makeStreamRequest(request: CompletionRequest): Promise<Response> {
    const response = await fetch('/api/llm/tools/complete', {
      method: 'POST',
      headers: this.getStreamHeaders(),
      body: JSON.stringify({ ...request, stream: true }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response;
  }

  private getStreamHeaders(): HeadersInit {
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
      'X-Organization-Id': localStorage.getItem('currentOrgId') || '',
    };
  }

  private getResponseReader(response: Response): ReadableStreamDefaultReader<Uint8Array> {
    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error('No response body');
    }
    return reader;
  }

  private async processStream(
    reader: ReadableStreamDefaultReader<Uint8Array>,
    onChunk: (chunk: string) => void,
    onComplete?: (response: CompletionResponse) => void
  ): Promise<void> {
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer = await this.processChunk(
        decoder.decode(value, { stream: true }),
        buffer,
        onChunk,
        onComplete
      );
    }
  }

  private async processChunk(
    decodedValue: string,
    currentBuffer: string,
    onChunk: (chunk: string) => void,
    onComplete?: (response: CompletionResponse) => void
  ): Promise<string> {
    let buffer = currentBuffer + decodedValue;
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      await this.processLine(line, onChunk, onComplete);
    }

    return buffer;
  }

  private async processLine(
    line: string,
    onChunk: (chunk: string) => void,
    onComplete?: (response: CompletionResponse) => void
  ): Promise<void> {
    if (!line.startsWith('data: ')) return;

    const data = line.slice(6);
    if (data === '[DONE]') return;

    try {
      const chunk = JSON.parse(data);
      this.handleChunk(chunk, onChunk, onComplete);
    } catch (e) {
      console.error('Failed to parse SSE chunk:', e);
    }
  }

  private handleChunk(
    chunk: any,
    onChunk: (chunk: string) => void,
    onComplete?: (response: CompletionResponse) => void
  ): void {
    if (chunk.content) {
      onChunk(chunk.content);
    }
    if (chunk.done && onComplete) {
      onComplete(chunk);
    }
  }

  // Model information
  async listModels(provider?: string): Promise<ModelInfo[]> {
    const params = provider ? { provider } : undefined;
    const response = await this.client.get('/models', { params });
    return response.data;
  }

  async getModelInfo(provider: string, model: string): Promise<ModelInfo> {
    const response = await this.client.get(`/models/${provider}/${model}`);
    return response.data;
  }

  // Token counting
  async countTokens(text: string, model: string): Promise<number> {
    return this.callTool('count_tokens', { text, model });
  }

  // Rate limit information
  async getRateLimits(): Promise<any> {
    const response = await this.client.get('/rate-limits');
    return response.data;
  }

  // Health check
  async checkHealth(): Promise<{ status: string; providers: Record<string, string> }> {
    const response = await this.client.get('/health');
    return response.data;
  }
}

export default new LLMClient();