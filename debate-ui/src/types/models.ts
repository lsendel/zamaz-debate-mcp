export interface ModelInfo {
  provider: string;
  name: string;
  displayName: string;
  description: string;
  contextWindow: number;
  isLocal?: boolean;
  size?: string;
}

export const AI_MODELS: ModelInfo[] = [
  // Anthropic Claude Models
  {
    provider: 'claude',
    name: 'claude-3-opus-20240229',
    displayName: 'Claude 3 Opus',
    description: 'Most capable Claude model for complex tasks',
    contextWindow: 200000
  },
  {
    provider: 'claude',
    name: 'claude-3-sonnet-20240229',
    displayName: 'Claude 3 Sonnet',
    description: 'Balanced performance and speed',
    contextWindow: 200000
  },
  {
    provider: 'claude',
    name: 'claude-3-haiku-20240307',
    displayName: 'Claude 3 Haiku',
    description: 'Fast and efficient for simple tasks',
    contextWindow: 200000
  },
  {
    provider: 'claude',
    name: 'claude-3-5-sonnet-20241022',
    displayName: 'Claude 3.5 Sonnet',
    description: 'Latest Sonnet with improved capabilities',
    contextWindow: 200000
  },

  // OpenAI Models
  {
    provider: 'openai',
    name: 'gpt-4-turbo-preview',
    displayName: 'GPT-4 Turbo',
    description: 'Latest GPT-4 with 128k context',
    contextWindow: 128000
  },
  {
    provider: 'openai',
    name: 'gpt-4',
    displayName: 'GPT-4',
    description: 'Most capable GPT model',
    contextWindow: 8192
  },
  {
    provider: 'openai',
    name: 'gpt-4o',
    displayName: 'GPT-4o',
    description: 'Optimized GPT-4 for speed',
    contextWindow: 128000
  },
  {
    provider: 'openai',
    name: 'gpt-4o-mini',
    displayName: 'GPT-4o Mini',
    description: 'Small, fast, and affordable',
    contextWindow: 128000
  },
  {
    provider: 'openai',
    name: 'o1-preview',
    displayName: 'O1 Preview',
    description: 'Reasoning model for complex problems',
    contextWindow: 128000
  },
  {
    provider: 'openai',
    name: 'o1-mini',
    displayName: 'O1 Mini',
    description: 'Smaller reasoning model',
    contextWindow: 128000
  },
  {
    provider: 'openai',
    name: 'gpt-3.5-turbo',
    displayName: 'GPT-3.5 Turbo',
    description: 'Fast and cost-effective',
    contextWindow: 16385
  },

  // Google Gemini Models
  {
    provider: 'gemini',
    name: 'gemini-1.5-pro',
    displayName: 'Gemini 1.5 Pro',
    description: 'Most capable with 1M token context',
    contextWindow: 1048576
  },
  {
    provider: 'gemini',
    name: 'gemini-1.5-flash',
    displayName: 'Gemini 1.5 Flash',
    description: 'Fast multimodal model',
    contextWindow: 1048576
  },
  {
    provider: 'gemini',
    name: 'gemini-2.0-flash-exp',
    displayName: 'Gemini 2.0 Flash (Experimental)',
    description: 'Next-gen experimental model',
    contextWindow: 1048576
  },
  {
    provider: 'gemini',
    name: 'gemini-pro',
    displayName: 'Gemini Pro',
    description: 'Balanced performance model',
    contextWindow: 32768
  },
  {
    provider: 'gemini',
    name: 'gemini-pro-vision',
    displayName: 'Gemini Pro Vision',
    description: 'Multimodal understanding',
    contextWindow: 16384
  },

  // Ollama Local Models
  {
    provider: 'llama',
    name: 'llama3.2',
    displayName: 'Llama 3.2',
    description: 'Latest Llama with vision capabilities',
    contextWindow: 128000,
    isLocal: true,
    size: '3.8GB'
  },
  {
    provider: 'llama',
    name: 'llama3.1',
    displayName: 'Llama 3.1',
    description: 'Updated Llama 3 with improvements',
    contextWindow: 128000,
    isLocal: true,
    size: '4.7GB'
  },
  {
    provider: 'llama',
    name: 'llama3.1:70b',
    displayName: 'Llama 3.1 70B',
    description: 'Large Llama model',
    contextWindow: 128000,
    isLocal: true,
    size: '40GB'
  },
  {
    provider: 'llama',
    name: 'llama3',
    displayName: 'Llama 3',
    description: 'Meta\'s latest open model',
    contextWindow: 8192,
    isLocal: true,
    size: '4.7GB'
  },
  {
    provider: 'llama',
    name: 'llama3:70b',
    displayName: 'Llama 3 70B',
    description: 'Largest Llama 3 model',
    contextWindow: 8192,
    isLocal: true,
    size: '40GB'
  },
  {
    provider: 'llama',
    name: 'mistral',
    displayName: 'Mistral 7B',
    description: 'Efficient French AI model',
    contextWindow: 32768,
    isLocal: true,
    size: '4.1GB'
  },
  {
    provider: 'llama',
    name: 'mixtral:8x7b',
    displayName: 'Mixtral 8x7B',
    description: 'MoE model with 45B parameters',
    contextWindow: 32768,
    isLocal: true,
    size: '26GB'
  },
  {
    provider: 'llama',
    name: 'mixtral:8x22b',
    displayName: 'Mixtral 8x22B',
    description: 'Large MoE model',
    contextWindow: 65536,
    isLocal: true,
    size: '87GB'
  },
  {
    provider: 'llama',
    name: 'qwen2.5',
    displayName: 'Qwen 2.5',
    description: 'Alibaba\'s multilingual model',
    contextWindow: 32768,
    isLocal: true,
    size: '4.4GB'
  },
  {
    provider: 'llama',
    name: 'deepseek-coder-v2',
    displayName: 'DeepSeek Coder V2',
    description: 'Specialized for coding',
    contextWindow: 16384,
    isLocal: true,
    size: '16GB'
  },
  {
    provider: 'llama',
    name: 'phi3',
    displayName: 'Phi-3',
    description: 'Microsoft\'s small language model',
    contextWindow: 128000,
    isLocal: true,
    size: '2.3GB'
  },
  {
    provider: 'llama',
    name: 'gemma2',
    displayName: 'Gemma 2',
    description: 'Google\'s open model',
    contextWindow: 8192,
    isLocal: true,
    size: '5.4GB'
  },
  {
    provider: 'llama',
    name: 'solar',
    displayName: 'Solar 10.7B',
    description: 'Upstage AI\'s model',
    contextWindow: 4096,
    isLocal: true,
    size: '6.1GB'
  }
];

// Helper function to get models by provider
export function getModelsByProvider(provider: string): ModelInfo[] {
  return AI_MODELS.filter(model => model.provider === provider);
}

// Helper function to get model info
export function getModelInfo(provider: string, modelName: string): ModelInfo | undefined {
  return AI_MODELS.find(model => model.provider === provider && model.name === modelName);
}