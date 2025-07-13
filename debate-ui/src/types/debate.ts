export interface LLMConfig {
  provider: 'claude' | 'openai' | 'gemini' | 'llama';
  model: string;
  temperature?: number;
  maxTokens?: number;
  systemPrompt?: string;
}

export interface Participant {
  id?: string;
  name: string;
  role?: 'debater' | 'moderator' | 'judge' | 'observer';
  position?: string;
  llm_config: LLMConfig;
}

export interface DebateRules {
  format?: 'round_robin' | 'free_form' | 'oxford' | 'panel' | 'socratic' | 'adversarial';
  maxRounds?: number;
  maxTurnsPerParticipant?: number;
  turnTimeLimitSeconds?: number;
  minTurnLength?: number;
  maxTurnLength?: number;
}

export interface Debate {
  id: string;
  name: string;
  topic: string;
  subject?: string;
  externalContext?: string;
  description?: string;
  participants: Participant[];
  rules: DebateRules;
  status: 'draft' | 'active' | 'paused' | 'completed' | 'archived';
  currentRound?: number;
  currentTurn?: number;
  nextParticipantId?: string;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  resolution?: string;
  conclusions?: Record<string, string>; // participantId -> conclusion
}

export interface Turn {
  id: string;
  debateId: string;
  participantId: string;
  turnNumber: number;
  roundNumber: number;
  turnType: 'opening' | 'argument' | 'rebuttal' | 'question' | 'answer' | 'closing';
  content: string;
  createdAt: string;
}

export interface DebateSummary {
  debateId: string;
  summary: string;
  keyPoints: string[];
  participantPositions: Record<string, string>;
  consensusPoints: string[];
  disagreementPoints: string[];
  createdAt: string;
}

// Ollama-specific models
export const OLLAMA_MODELS = [
  { name: 'llama3', size: '4.7GB', description: 'Latest Llama 3 8B model' },
  { name: 'llama3:70b', size: '40GB', description: 'Llama 3 70B model' },
  { name: 'mistral', size: '4.1GB', description: 'Mistral 7B v0.2' },
  { name: 'mixtral', size: '26GB', description: 'Mixtral 8x7B MoE' },
  { name: 'neural-chat', size: '4.1GB', description: 'Intel Neural Chat 7B' },
  { name: 'starling-lm', size: '4.1GB', description: 'Starling 7B Alpha' },
  { name: 'codellama', size: '3.8GB', description: 'Code Llama 7B' },
  { name: 'llama2-uncensored', size: '3.8GB', description: 'Llama 2 Uncensored' },
  { name: 'vicuna', size: '3.8GB', description: 'Vicuna 7B v1.5' },
  { name: 'orca-mini', size: '1.9GB', description: 'Orca Mini 3B' },
  { name: 'phi', size: '1.6GB', description: 'Microsoft Phi-2' },
  { name: 'tinyllama', size: '637MB', description: 'TinyLlama 1.1B' }
];