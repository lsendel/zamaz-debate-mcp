import { NextRequest, NextResponse } from 'next/server';

const CONTEXT_SERVICE_URL = process.env.MCP_CONTEXT_URL || 'http://localhost:8001';

export async function GET(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  const path = params.path.join('/');
  const url = new URL(request.url);
  const queryString = url.searchParams.toString();
  const fullUrl = `${CONTEXT_SERVICE_URL}/${path}${queryString ? `?${queryString}` : ''}`;

  try {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    // Forward organization header if present
    const orgId = request.headers.get('X-Organization-ID');
    if (orgId) {
      headers['X-Organization-ID'] = orgId;
    }

    const response = await fetch(fullUrl, {
      method: 'GET',
      headers,
    });

    const data = await response.json();
    return NextResponse.json(data, { status: response.status });
  } catch (error) {
    console.error('Context API error:', error);
    return NextResponse.json(
      { error: 'Failed to communicate with context service' },
      { status: 502 }
    );
  }
}

export async function POST(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  const path = params.path.join('/');
  const fullUrl = `${CONTEXT_SERVICE_URL}/${path}`;

  try {
    const body = await request.json();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    // Forward organization header if present
    const orgId = request.headers.get('X-Organization-ID');
    if (orgId) {
      headers['X-Organization-ID'] = orgId;
    }

    const response = await fetch(fullUrl, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
    });

    const data = await response.json();
    return NextResponse.json(data, { status: response.status });
  } catch (error) {
    console.error('Context API error:', error);
    return NextResponse.json(
      { error: 'Failed to communicate with context service' },
      { status: 502 }
    );
  }
}

export async function PUT(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  const path = params.path.join('/');
  const fullUrl = `${CONTEXT_SERVICE_URL}/${path}`;

  try {
    const body = await request.json();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    // Forward organization header if present
    const orgId = request.headers.get('X-Organization-ID');
    if (orgId) {
      headers['X-Organization-ID'] = orgId;
    }

    const response = await fetch(fullUrl, {
      method: 'PUT',
      headers,
      body: JSON.stringify(body),
    });

    const data = await response.json();
    return NextResponse.json(data, { status: response.status });
  } catch (error) {
    console.error('Context API error:', error);
    return NextResponse.json(
      { error: 'Failed to communicate with context service' },
      { status: 502 }
    );
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  const path = params.path.join('/');
  const fullUrl = `${CONTEXT_SERVICE_URL}/${path}`;

  try {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    // Forward organization header if present
    const orgId = request.headers.get('X-Organization-ID');
    if (orgId) {
      headers['X-Organization-ID'] = orgId;
    }

    const response = await fetch(fullUrl, {
      method: 'DELETE',
      headers,
    });

    if (response.status === 204) {
      return new NextResponse(null, { status: 204 });
    }

    const data = await response.json();
    return NextResponse.json(data, { status: response.status });
  } catch (error) {
    console.error('Context API error:', error);
    return NextResponse.json(
      { error: 'Failed to communicate with context service' },
      { status: 502 }
    );
  }
}