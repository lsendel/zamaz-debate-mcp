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
  topic: string;
  description?: string;
  status: "created" | "in_progress" | "completed" | "cancelled";
  participants: Participant[];
  rounds: Round[];
  maxRounds: number;
  currentRound: number;
  turnTimeLimit?: number;
  organizationId: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
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
    super("/api/debate");
  }

  // Debate management
  async createDebate(data: CreateDebateRequest): Promise<Debate> {
    return this.callTool("create_debate", data);
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
    return this.callTool("start_debate", { debate_id: debateId });
  }

  async pauseDebate(debateId: string): Promise<void> {
    return this.callTool("pause_debate", { debate_id: debateId });
  }

  async resumeDebate(debateId: string): Promise<void> {
    return this.callTool("resume_debate", { debate_id: debateId });
  }

  async cancelDebate(debateId: string): Promise<void> {
    return this.callTool("cancel_debate", { debate_id: debateId });
  }

  // Participant management
  async addParticipant(
    debateId: string,
    participant: Omit<Participant, "id">,
  ): Promise<Participant> {
    return this.callTool("add_participant", {
      debate_id: debateId,
      ...participant,
    });
  }

  async removeParticipant(
    debateId: string,
    participantId: string,
  ): Promise<void> {
    return this.callTool("remove_participant", {
      debate_id: debateId,
      participant_id: participantId,
    });
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
    return this.callTool("submit_response", {
      debate_id: debateId,
      participant_id: participantId,
      content,
    });
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
}

export default new DebateClient();
