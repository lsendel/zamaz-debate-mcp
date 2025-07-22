import BaseApiClient from "./baseClient";

export interface LLMProvider {
  id: string;
  name: string;
  apiEndpoint: string;
  authType: string;
  status: string;
  models: LLMModel[];
}

export interface LLMModel {
  id: string;
  name: string;
  provider: string;
  type: string;
  maxTokens: number;
  costPerToken: number;
  capabilities: string[];
}

export interface CompletionRequest {
  provider: string;
  model: string;
  messages: Array<{
    role: "system" | "user" | "assistant";
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


class LLMClient extends BaseApiClient {
  constructor() {
    super("/api/v1");
  }

  // Provider management
  async listProviders(): Promise<LLMProvider[]> {
    const response = await this.client.get("/providers");
    return response.data;
  }

  async getProvider(name: string): Promise<LLMProvider> {
    const response = await this.client.get(`/providers/${name}`);
    return response.data;
  }

  // Completion
  async complete(request: CompletionRequest): Promise<CompletionResponse> {
    const response = await this.client.post("/chat/completions", {
      model: request.model,
      messages: request.messages,
      max_tokens: request.maxTokens,
      temperature: request.temperature,
      top_p: request.topP,
      stop: request.stopSequences
    });
    
    return {
      content: response.data.choices[0].message.content,
      model: response.data.model,
      usage: {
        promptTokens: response.data.usage.prompt_tokens,
        completionTokens: response.data.usage.completion_tokens,
        totalTokens: response.data.usage.total_tokens
      },
      finishReason: response.data.choices[0].finish_reason
    };
  }

  async streamComplete(
    request: CompletionRequest,
    onChunk: (chunk: string) => void,
    onComplete?: (response: CompletionResponse) => void,
  ): Promise<void> {
    const response = await this.makeStreamRequest(request);
    const reader = this.getResponseReader(response);
    await this.processStream(reader, onChunk, onComplete);
  }

  private async makeStreamRequest(
    request: CompletionRequest,
  ): Promise<Response> {
    const response = await fetch("/api/v1/chat/completions", {
      method: "POST",
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
      "Content-Type": "application/json",
      Authorization: `Bearer ${localStorage.getItem("authToken")}`,
      "X-Organization-Id": localStorage.getItem("currentOrgId") || "",
    };
  }

  private getResponseReader(
    response: Response,
  ): ReadableStreamDefaultReader<Uint8Array> {
    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error("No response body");
    }
    return reader;
  }

  private async processStream(
    reader: ReadableStreamDefaultReader<Uint8Array>,
    onChunk: (chunk: string) => void,
    onComplete?: (response: CompletionResponse) => void,
  ): Promise<void> {
    const decoder = new TextDecoder();
    let buffer = "";

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer = this.updateBufferWithNewData(
          decoder.decode(value, { stream: true }),
          buffer,
          onChunk,
          onComplete,
        );
      }
    } finally {
      reader.releaseLock();
    }
  }

  private updateBufferWithNewData(
    decodedValue: string,
    currentBuffer: string,
    onChunk: (chunk: string) => void,
    onComplete?: (response: CompletionResponse) => void,
  ): string {
    const buffer = currentBuffer + decodedValue;
    const lines = buffer.split("\n");
    const remainingBuffer = lines.pop() || "";

    this.processCompletedLines(lines, onChunk, onComplete);
    return remainingBuffer;
  }

  private processCompletedLines(
    lines: string[],
    onChunk: (chunk: string) => void,
    onComplete?: (response: CompletionResponse) => void,
  ): void {
    lines.forEach((line) => this.processSingleLine(line, onChunk, onComplete));
  }

  private processSingleLine(
    line: string,
    onChunk: (chunk: string) => void,
    onComplete?: (response: CompletionResponse) => void,
  ): void {
    if (!this.isValidSSELine(line)) return;

    const data = this.extractSSEData(line);
    if (this.isEndMarker(data)) return;

    const chunk = this.parseChunkData(data);
    if (chunk) {
      this.handleChunk(chunk, onChunk, onComplete);
    }
  }

  private isValidSSELine(line: string): boolean {
    return line.startsWith("data: ");
  }

  private extractSSEData(line: string): string {
    return line.slice(6);
  }

  private isEndMarker(data: string): boolean {
    return data === "[DONE]";
  }

  private parseChunkData(data: string): any | null {
    try {
      return JSON.parse(data);
    } catch (e) {
      console.error("Failed to parse SSE chunk:", e);
      return null;
    }
  }

  private handleChunk(
    chunk: any,
    onChunk: (chunk: string) => void,
    onComplete?: (response: CompletionResponse) => void,
  ): void {
    if (chunk.content) {
      onChunk(chunk.content);
    }
    if (chunk.done && onComplete) {
      onComplete(chunk);
    }
  }

  // Model information
  async listModels(provider?: string): Promise<LLMModel[]> {
    if (provider) {
      const response = await this.client.get(`/providers/${provider}/models`);
      return response.data;
    } else {
      const response = await this.client.get("/models");
      return response.data;
    }
  }

  async getModelInfo(modelId: string): Promise<LLMModel> {
    const response = await this.client.get(`/models/${modelId}`);
    return response.data;
  }

  // Token counting (mock implementation for now)
  async countTokens(text: string, model: string): Promise<number> {
    // Simple approximation: 1 token per 4 characters
    return Math.ceil(text.length / 4);
  }

  // Rate limit information
  async getRateLimits(): Promise<any> {
    const response = await this.client.get("/rate-limits");
    return response.data;
  }

  // Health check
  async checkHealth(): Promise<{
    status: string;
    providers: Record<string, string>;
  }> {
    const providers = await this.listProviders();
    const providerStatus: Record<string, string> = {};
    
    for (const provider of providers) {
      try {
        const response = await this.client.get(`/providers/${provider.id}/health`);
        providerStatus[provider.id] = response.data.available ? 'available' : 'unavailable';
      } catch (error) {
          console.error("Error:", error);
        providerStatus[provider.id] = 'unavailable';
        console.error("Error:", error);
      }
    }
    
    return {
      status: 'UP',
      providers: providerStatus
    };
  }
}

export default new LLMClient();
