// Mock LLM providers and models for development
export const MOCK_LLM_PROVIDERS = [
  {
    name: 'claude',
    models: [
      'claude-3-opus-20240229',
      'claude-3-sonnet-20240229',
      'claude-3-haiku-20240307',
      'claude-2.1',
      'claude-2.0'
    ],
    features: {
      streaming: true,
      functionCalling: true,
      visionSupport: true,
      maxTokens: 200000
    },
    status: 'available' as const
  },
  {
    name: 'openai',
    models: [
      'gpt-4-turbo-preview',
      'gpt-4',
      'gpt-4-32k',
      'gpt-3.5-turbo',
      'gpt-3.5-turbo-16k'
    ],
    features: {
      streaming: true,
      functionCalling: true,
      visionSupport: true,
      maxTokens: 128000
    },
    status: 'available' as const
  },
  {
    name: 'gemini',
    models: [
      'gemini-pro',
      'gemini-pro-vision',
      'gemini-ultra'
    ],
    features: {
      streaming: true,
      functionCalling: true,
      visionSupport: true,
      maxTokens: 32000
    },
    status: 'available' as const
  },
  {
    name: 'ollama',
    models: [
      'llama2',
      'mistral',
      'mixtral',
      'codellama',
      'neural-chat'
    ],
    features: {
      streaming: true,
      functionCalling: false,
      visionSupport: false,
      maxTokens: 4096
    },
    status: 'available' as const
  }
];

export const MOCK_MODEL_INFO = {
  'claude-3-opus-20240229': {
    provider: 'claude',
    model: 'claude-3-opus-20240229',
    description: 'Most capable Claude model, best for complex tasks',
    contextWindow: 200000,
    pricing: { inputTokens: 0.015, outputTokens: 0.075 },
    capabilities: ['reasoning', 'coding', 'analysis', 'creative_writing']
  },
  'gpt-4-turbo-preview': {
    provider: 'openai',
    model: 'gpt-4-turbo-preview',
    description: 'Latest GPT-4 with 128k context',
    contextWindow: 128000,
    pricing: { inputTokens: 0.01, outputTokens: 0.03 },
    capabilities: ['reasoning', 'coding', 'analysis', 'function_calling']
  },
  'gemini-pro': {
    provider: 'gemini',
    model: 'gemini-pro',
    description: 'Google\'s advanced language model',
    contextWindow: 32000,
    pricing: { inputTokens: 0.001, outputTokens: 0.002 },
    capabilities: ['reasoning', 'coding', 'multimodal']
  }
};

// Mock debates for the debates list
export const MOCK_DEBATES = [
  {
    id: 'debate-1',
    topic: 'The Impact of AI on Society',
    description: 'A comprehensive debate about the positive and negative impacts of artificial intelligence on modern society.',
    status: 'completed',
    participants: [
      {
        id: 'p1',
        name: 'AI Optimist',
        llmProvider: 'claude',
        model: 'claude-3-opus-20240229'
      },
      {
        id: 'p2',
        name: 'AI Skeptic',
        llmProvider: 'openai',
        model: 'gpt-4'
      }
    ],
    rounds: [],
    maxRounds: 5,
    currentRound: 5,
    organizationId: 'org-123',
    createdAt: '2024-01-15T10:00:00Z',
    updatedAt: '2024-01-15T11:30:00Z',
    completedAt: '2024-01-15T11:30:00Z'
  },
  {
    id: 'debate-2',
    topic: 'Remote Work vs Office Work',
    description: 'Discussing the pros and cons of remote work compared to traditional office environments.',
    status: 'in_progress',
    participants: [
      {
        id: 'p3',
        name: 'Remote Advocate',
        llmProvider: 'gemini',
        model: 'gemini-pro'
      },
      {
        id: 'p4',
        name: 'Office Defender',
        llmProvider: 'claude',
        model: 'claude-3-sonnet-20240229'
      }
    ],
    rounds: [],
    maxRounds: 4,
    currentRound: 2,
    organizationId: 'org-123',
    createdAt: '2024-01-18T14:00:00Z',
    updatedAt: '2024-01-18T14:45:00Z'
  }
];

// Mock organization data
export const MOCK_ORGANIZATIONS = [
  {
    id: 'org-123',
    name: 'Demo Organization',
    description: 'Development testing organization',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  },
  {
    id: 'org-456',
    name: 'Test Corp',
    description: 'Another test organization',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  }
];