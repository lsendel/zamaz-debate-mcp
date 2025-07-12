import { NextRequest, NextResponse } from 'next/server';
import { apiLogger } from '@/lib/api-logger';

const DEBATE_SERVICE_URL = process.env.MCP_DEBATE_URL || 'http://localhost:5013';

// Common proxy handler
async function proxyRequest(
  request: NextRequest,
  method: string,
  path: string
): Promise<NextResponse> {
  const url = new URL(request.url);
  const queryString = url.searchParams.toString();
  const fullUrl = `${DEBATE_SERVICE_URL}/${path}${queryString ? `?${queryString}` : ''}`;

  try {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    // Forward organization header if present
    const orgId = request.headers.get('X-Organization-ID');
    if (orgId) {
      headers['X-Organization-ID'] = orgId;
    }

    let body: string | undefined;
    if (method !== 'GET' && method !== 'DELETE') {
      const requestBody = await request.json();
      body = JSON.stringify(requestBody);
    }

    const response = await fetch(fullUrl, {
      method,
      headers,
      body,
    });

    if (response.status === 204) {
      return new NextResponse(null, { status: 204 });
    }

    const data = await response.json();
    return NextResponse.json(data, { status: response.status });
  } catch (error) {
    apiLogger.error('Debate API error', error as Error, { method, path });
    return NextResponse.json(
      { error: 'Failed to communicate with debate service' },
      { status: 502 }
    );
  }
}

export async function GET(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  const path = params.path.join('/');
  return proxyRequest(request, 'GET', path);
}

export async function POST(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  const path = params.path.join('/');
  return proxyRequest(request, 'POST', path);
}

export async function PUT(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  const path = params.path.join('/');
  return proxyRequest(request, 'PUT', path);
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  const path = params.path.join('/');
  return proxyRequest(request, 'DELETE', path);
}