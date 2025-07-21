import BaseApiClient from "./baseClient";

export interface Participant {
  id: string;
  name: string;
  llmProvider: string;
  model: string;
  systemPrompt?: string;
  temperature?: number;
  maxTokens?: number;
}

export interface Response {
  id: string;
  participantId: string;
  roundNumber: number;
  content: string;
  timestamp: string;
  tokenCount?: number;
}

export interface Round {
  roundNumber: number;
  responses: Response[];
  status: "pending" | "in_progress" | "completed";
}

export interface Debate {
  id: string;
  title?: string;
  topic: string;
  description?: string;
  status: "CREATED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
  format?: string;
  participants: string[] | Participant[]; // Can be either strings or objects
  rounds?: Round[];
  maxRounds?: number;
  currentRound?: number;
  turnTimeLimit?: number;
  organizationId?: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
  createdBy?: string;
  winner?: string;
  summary?: string;
}

export interface CreateDebateRequest {
  topic: string;
  description?: string;
  participants: Array<{
    name: string;
    llmProvider: string;
    model: string;
    systemPrompt?: string;
    temperature?: number;
    maxTokens?: number;
  }>;
  maxRounds: number;
  turnTimeLimit?: number;
}

export interface DebateEvent {
  type:
    | "debate_started"
    | "round_started"
    | "response_received"
    | "round_completed"
    | "debate_completed";
  debateId: string;
  data: any;
  timestamp: string;
}

class DebateClient extends BaseApiClient {
  private ws: WebSocket | null = null;
  private eventHandlers: Map<string, Set<(event: DebateEvent) => void>> =
    new Map();

  constructor() {
    const baseURL = import.meta.env.VITE_DEBATE_API_URL || "http://localhost:5013";
    super(`${baseURL}/api/v1`);
  }

  // Debate management
  async createDebate(data: CreateDebateRequest): Promise<Debate> {
    const response = await this.client.post("/debates", {
      title: data.topic, // Use topic as title for now
      topic: data.topic,
      description: data.description,
      format: "OXFORD", // Default format
      participants: data.participants.map(p => p.name), // Simplified for now
      maxRounds: data.maxRounds,
      turnTimeLimit: data.turnTimeLimit
    });
    return response.data;
  }

  async getDebate(id: string): Promise<Debate> {
    const response = await this.client.get(`/debates/${id}`);
    return response.data;
  }

  async listDebates(params?: {
    status?: string;
    limit?: number;
    offset?: number;
  }): Promise<Debate[]> {
    const response = await this.client.get("/debates", { params });
    return response.data;
  }

  async startDebate(debateId: string): Promise<void> {
    const response = await this.client.post(`/debates/${debateId}/start`);
    return response.data;
  }

  async pauseDebate(debateId: string): Promise<void> {
    // For now, just update the status to paused
    const response = await this.client.put(`/debates/${debateId}`, { status: 'PAUSED' });
    return response.data;
  }

  async resumeDebate(debateId: string): Promise<void> {
    // For now, just update the status to in_progress
    const response = await this.client.put(`/debates/${debateId}`, { status: 'IN_PROGRESS' });
    return response.data;
  }

  async cancelDebate(debateId: string): Promise<void> {
    // For now, just update the status to cancelled
    const response = await this.client.put(`/debates/${debateId}`, { status: 'CANCELLED' });
    return response.data;
  }

  // Participant management
  async addParticipant(
    debateId: string,
    participant: Omit<Participant, "id">,
  ): Promise<Participant> {
    const response = await this.client.post(`/debates/${debateId}/participants`, participant);
    return response.data;
  }

  async removeParticipant(
    debateId: string,
    participantId: string,
  ): Promise<void> {
    const response = await this.client.delete(`/debates/${debateId}/participants/${participantId}`);
    return response.data;
  }

  async updateParticipant(
    debateId: string,
    participantId: string,
    updates: Partial<Participant>,
  ): Promise<Participant> {
    const response = await this.client.put(
      `/debates/${debateId}/participants/${participantId}`,
      updates,
    );
    return response.data;
  }

  // Response management
  async getResponses(
    debateId: string,
    roundNumber?: number,
  ): Promise<Response[]> {
    const params =
      roundNumber !== undefined ? { round: roundNumber } : undefined;
    const response = await this.client.get(`/debates/${debateId}/responses`, {
      params,
    });
    return response.data;
  }

  async submitResponse(
    debateId: string,
    participantId: string,
    content: string,
  ): Promise<Response> {
    const response = await this.client.post(`/debates/${debateId}/responses`, {
      participantId,
      content
    });
    return response.data;
  }

  // WebSocket connection for live updates
  connectWebSocket(debateId: string): void {
    if (this.ws) {
      this.ws.close();
    }

    // Use environment variable or fall back to default
    const wsBase = import.meta.env.VITE_WS_URL || "ws://localhost:5013";
    const wsUrl = `${wsBase}/ws/debates/${debateId}`;
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log("WebSocket connected for debate:", debateId);
    };

    this.ws.onmessage = (event) => {
      try {
        const debateEvent: DebateEvent = JSON.parse(event.data);
        this.emitEvent(debateEvent.type, debateEvent);
      } catch (error) {
        console.error("Failed to parse WebSocket message:", error);
      }
    };

    this.ws.onerror = (error) => {
      console.error("WebSocket error:", error);
    };

    this.ws.onclose = () => {
      console.log("WebSocket disconnected");
      this.ws = null;
    };
  }

  disconnectWebSocket(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  // Event handling
  on(eventType: string, handler: (event: DebateEvent) => void): void {
    if (!this.eventHandlers.has(eventType)) {
      this.eventHandlers.set(eventType, new Set());
    }
    this.eventHandlers.get(eventType)!.add(handler);
  }

  off(eventType: string, handler: (event: DebateEvent) => void): void {
    const handlers = this.eventHandlers.get(eventType);
    if (handlers) {
      handlers.delete(handler);
    }
  }

  private emitEvent(eventType: string, event: DebateEvent): void {
    const handlers = this.eventHandlers.get(eventType);
    if (handlers) {
      handlers.forEach((handler) => handler(event));
    }
  }

  // Analysis and export
  async getDebateAnalysis(debateId: string): Promise<any> {
    const response = await this.client.get(`/debates/${debateId}/analysis`);
    return response.data;
  }

  async exportDebate(
    debateId: string,
    format: "json" | "pdf" | "markdown" = "json",
  ): Promise<Blob> {
    const response = await this.client.get(`/debates/${debateId}/export`, {
      params: { format },
      responseType: "blob",
    });
    return response.data;
  }

  // Agentic Flow Configuration
  async configureDebateAgenticFlow(debateId: string, configuration: any): Promise<any> {
    const response = await this.client.post(`/debates/${debateId}/agentic-flow`, configuration);
    return response.data;
  }

  async configureParticipantAgenticFlow(
    debateId: string,
    participantId: string,
    configuration: any
  ): Promise<any> {
    const response = await this.client.post(
      `/debates/${debateId}/participants/${participantId}/agentic-flow`,
      configuration
    );
    return response.data;
  }

  async getDebateAgenticFlow(debateId: string): Promise<any> {
    const response = await this.client.get(`/debates/${debateId}/agentic-flow`);
    return response.data;
  }

  async getParticipantAgenticFlow(debateId: string, participantId: string): Promise<any> {
    const response = await this.client.get(
      `/debates/${debateId}/participants/${participantId}/agentic-flow`
    );
    return response.data;
  }

  // Agentic Flow Analytics
  async getDebateAnalytics(debateId: string): Promise<any> {
    const response = await this.client.get(`/analytics/debates/${debateId}/agentic-flows`);
    return response.data;
  }

  async getFlowTypeStatistics(
    organizationId: string,
    startDate: string,
    endDate: string
  ): Promise<any[]> {
    const response = await this.client.get('/analytics/agentic-flows/statistics', {
      params: { organizationId, startDate, endDate }
    });
    return response.data;
  }

  async getFlowExecutionTimeSeries(
    organizationId: string,
    startDate: string,
    endDate: string
  ): Promise<any[]> {
    const response = await this.client.get('/analytics/agentic-flows/time-series', {
      params: { organizationId, startDate, endDate }
    });
    return response.data;
  }

  async getTrendingFlowTypes(organizationId: string, limit: number = 10): Promise<any[]> {
    const response = await this.client.get('/analytics/agentic-flows/trending', {
      params: { organizationId, limit }
    });
    return response.data;
  }

  async compareFlowTypes(
    organizationId: string,
    flowTypes: string[],
    startDate: string,
    endDate: string
  ): Promise<any> {
    const response = await this.client.post('/analytics/agentic-flows/compare', {
      organizationId,
      flowTypes,
      startDate,
      endDate
    });
    return response.data;
  }
}

export default new DebateClient();
