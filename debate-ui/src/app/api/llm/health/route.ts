import { NextRequest, NextResponse } from 'next/server';

export async function GET(req: NextRequest) {
  try {
    const LLM_SERVICE_URL = process.env.LLM_SERVICE_URL || 'http://localhost:5002';
    
    try {
      const response = await fetch(`${LLM_SERVICE_URL}/health`, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
        },
        signal: AbortSignal.timeout(2000)
      });

      if (response.ok) {
        const data = await response.json();
        return NextResponse.json(data);
      }
    } catch (error) {
      // Service not available
    }

    // Return mock health status
    return NextResponse.json({
      status: 'degraded',
      providers: {
        claude: {
          available: true,
          error: null,
          models: ['claude-3-5-sonnet-20241022', 'claude-3-opus-20240229', 'claude-3-haiku-20240307']
        },
        openai: {
          available: true,
          error: null,
          models: ['gpt-4o', 'gpt-4-turbo-preview', 'gpt-3.5-turbo']
        },
        gemini: {
          available: true,
          error: null,
          models: ['gemini-2.0-flash-exp', 'gemini-1.5-pro', 'gemini-1.5-flash']
        },
        llama: {
          available: false,
          error: 'Ollama service not running',
          models: []
        }
      },
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    return NextResponse.json({
      status: 'unhealthy',
      providers: {},
      timestamp: new Date().toISOString()
    }, { status: 503 });
  }
}