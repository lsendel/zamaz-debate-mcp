import { NextRequest, NextResponse } from 'next/server';

// Mock providers for development when backend is not available
const mockProviders = {
  providers: [
    {
      id: 'claude',
      name: 'Claude (Anthropic)',
      type: 'api' as const,
      models: [
        {
          id: 'claude-3-5-sonnet-20241022',
          name: 'Claude 3.5 Sonnet',
          provider: 'claude',
          capabilities: ['chat', 'streaming'],
          contextWindow: 200000,
          costPer1kTokens: { input: 0.003, output: 0.015 }
        },
        {
          id: 'claude-3-opus-20240229',
          name: 'Claude 3 Opus',
          provider: 'claude',
          capabilities: ['chat', 'streaming'],
          contextWindow: 200000,
          costPer1kTokens: { input: 0.015, output: 0.075 }
        },
        {
          id: 'claude-3-haiku-20240307',
          name: 'Claude 3 Haiku',
          provider: 'claude',
          capabilities: ['chat', 'streaming'],
          contextWindow: 200000,
          costPer1kTokens: { input: 0.00025, output: 0.00125 }
        }
      ],
      isAvailable: true,
      requiredConfig: ['ANTHROPIC_API_KEY']
    },
    {
      id: 'openai',
      name: 'OpenAI',
      type: 'api' as const,
      models: [
        {
          id: 'gpt-4o',
          name: 'GPT-4o',
          provider: 'openai',
          capabilities: ['chat', 'streaming', 'vision'],
          contextWindow: 128000,
          costPer1kTokens: { input: 0.005, output: 0.015 }
        },
        {
          id: 'gpt-4-turbo-preview',
          name: 'GPT-4 Turbo',
          provider: 'openai',
          capabilities: ['chat', 'streaming'],
          contextWindow: 128000,
          costPer1kTokens: { input: 0.01, output: 0.03 }
        },
        {
          id: 'gpt-3.5-turbo',
          name: 'GPT-3.5 Turbo',
          provider: 'openai',
          capabilities: ['chat', 'streaming'],
          contextWindow: 16385,
          costPer1kTokens: { input: 0.0005, output: 0.0015 }
        }
      ],
      isAvailable: true,
      requiredConfig: ['OPENAI_API_KEY']
    },
    {
      id: 'gemini',
      name: 'Google Gemini',
      type: 'api' as const,
      models: [
        {
          id: 'gemini-2.0-flash-exp',
          name: 'Gemini 2.0 Flash',
          provider: 'gemini',
          capabilities: ['chat', 'streaming', 'vision'],
          contextWindow: 1048576,
          costPer1kTokens: { input: 0.00025, output: 0.001 }
        },
        {
          id: 'gemini-1.5-pro',
          name: 'Gemini 1.5 Pro',
          provider: 'gemini',
          capabilities: ['chat', 'streaming', 'vision'],
          contextWindow: 1048576,
          costPer1kTokens: { input: 0.00125, output: 0.005 }
        },
        {
          id: 'gemini-1.5-flash',
          name: 'Gemini 1.5 Flash',
          provider: 'gemini',
          capabilities: ['chat', 'streaming', 'vision'],
          contextWindow: 1048576,
          costPer1kTokens: { input: 0.000075, output: 0.0003 }
        }
      ],
      isAvailable: true,
      requiredConfig: ['GOOGLE_API_KEY']
    },
    {
      id: 'llama',
      name: 'Llama (Ollama)',
      type: 'local' as const,
      models: [
        {
          id: 'llama3.2',
          name: 'Llama 3.2',
          provider: 'llama',
          capabilities: ['chat', 'streaming'],
          contextWindow: 128000
        },
        {
          id: 'llama3.1',
          name: 'Llama 3.1',
          provider: 'llama',
          capabilities: ['chat', 'streaming'],
          contextWindow: 128000
        },
        {
          id: 'llama3',
          name: 'Llama 3',
          provider: 'llama',
          capabilities: ['chat', 'streaming'],
          contextWindow: 8192
        },
        {
          id: 'mistral',
          name: 'Mistral',
          provider: 'llama',
          capabilities: ['chat', 'streaming'],
          contextWindow: 32768
        },
        {
          id: 'mixtral',
          name: 'Mixtral',
          provider: 'llama',
          capabilities: ['chat', 'streaming'],
          contextWindow: 32768
        }
      ],
      isAvailable: false, // Requires Ollama to be running
      requiredConfig: []
    }
  ]
};

export async function GET(req: NextRequest) {
  try {
    // Try to connect to the actual LLM service first
    const LLM_SERVICE_URL = process.env.LLM_SERVICE_URL || 'http://localhost:5002';
    
    try {
      const response = await fetch(`${LLM_SERVICE_URL}/providers`, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
        },
        // Add a timeout
        signal: AbortSignal.timeout(2000)
      });

      if (response.ok) {
        const data = await response.json();
        return NextResponse.json(data);
      }
    } catch (error) {
      // If backend is not available, return mock data
      console.log('LLM service not available, returning mock data');
    }

    // Return mock data for development
    return NextResponse.json(mockProviders);
  } catch (error) {
    console.error('Error in LLM providers route:', error);
    return NextResponse.json(mockProviders);
  }
}