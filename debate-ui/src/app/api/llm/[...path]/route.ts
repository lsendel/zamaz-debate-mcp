import { NextRequest, NextResponse } from 'next/server';
import { apiLogger } from '@/lib/api-logger';

const LLM_SERVICE_URL = process.env.LLM_SERVICE_URL || 'http://localhost:5002';

export async function GET(
  req: NextRequest,
  { params }: { params: { path: string[] } }
) {
  try {
    const path = params.path.join('/');
    const searchParams = req.nextUrl.searchParams;
    
    const response = await fetch(
      `${LLM_SERVICE_URL}/${path}?${searchParams}`,
      {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
        },
      }
    );

    const data = await response.json();
    
    if (!response.ok) {
      return NextResponse.json(
        { error: data.detail || 'LLM service error' },
        { status: response.status }
      );
    }

    return NextResponse.json(data);
  } catch (error) {
    apiLogger.error('LLM service error', error as Error, { path: params.path.join('/') });
    return NextResponse.json(
      { error: 'Failed to connect to LLM service' },
      { status: 500 }
    );
  }
}

export async function POST(
  req: NextRequest,
  { params }: { params: { path: string[] } }
) {
  try {
    const path = params.path.join('/');
    const body = await req.json();
    
    const response = await fetch(
      `${LLM_SERVICE_URL}/${path}`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: JSON.stringify(body),
      }
    );

    const data = await response.json();
    
    if (!response.ok) {
      return NextResponse.json(
        { error: data.detail || 'LLM service error' },
        { status: response.status }
      );
    }

    return NextResponse.json(data);
  } catch (error) {
    apiLogger.error('LLM service error', error as Error, { path: params.path.join('/') });
    return NextResponse.json(
      { error: 'Failed to connect to LLM service' },
      { status: 500 }
    );
  }
}

export async function PUT(
  req: NextRequest,
  { params }: { params: { path: string[] } }
) {
  try {
    const path = params.path.join('/');
    const body = await req.json();
    
    const response = await fetch(
      `${LLM_SERVICE_URL}/${path}`,
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: JSON.stringify(body),
      }
    );

    const data = await response.json();
    
    if (!response.ok) {
      return NextResponse.json(
        { error: data.detail || 'LLM service error' },
        { status: response.status }
      );
    }

    return NextResponse.json(data);
  } catch (error) {
    apiLogger.error('LLM service error', error as Error, { path: params.path.join('/') });
    return NextResponse.json(
      { error: 'Failed to connect to LLM service' },
      { status: 500 }
    );
  }
}

export async function DELETE(
  req: NextRequest,
  { params }: { params: { path: string[] } }
) {
  try {
    const path = params.path.join('/');
    
    const response = await fetch(
      `${LLM_SERVICE_URL}/${path}`,
      {
        method: 'DELETE',
        headers: {
          'Accept': 'application/json',
        },
      }
    );

    if (response.status === 204) {
      return new NextResponse(null, { status: 204 });
    }

    const data = await response.json();
    
    if (!response.ok) {
      return NextResponse.json(
        { error: data.detail || 'LLM service error' },
        { status: response.status }
      );
    }

    return NextResponse.json(data);
  } catch (error) {
    apiLogger.error('LLM service error', error as Error, { path: params.path.join('/') });
    return NextResponse.json(
      { error: 'Failed to connect to LLM service' },
      { status: 500 }
    );
  }
}