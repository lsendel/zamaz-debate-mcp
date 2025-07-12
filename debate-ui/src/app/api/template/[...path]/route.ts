import { NextRequest, NextResponse } from 'next/server';
import { apiLogger } from '@/lib/api-logger';

const TEMPLATE_SERVICE_URL = process.env.TEMPLATE_SERVICE_URL || 'http://localhost:5006';

export async function GET(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  try {
    const path = params.path.join('/');
    const url = `${TEMPLATE_SERVICE_URL}/${path}`;
    
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
      },
    });

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    apiLogger.error('Template service GET error', error as Error, { path: params.path.join('/') });
    return NextResponse.json(
      { error: 'Failed to fetch from template service' },
      { status: 500 }
    );
  }
}

export async function POST(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  try {
    const path = params.path.join('/');
    const body = await request.json();
    
    // Handle different template operations
    let mcpRequest;
    
    if (path.includes('search')) {
      mcpRequest = {
        jsonrpc: '2.0',
        method: 'tools/call',
        params: {
          name: 'search_templates',
          arguments: body
        },
        id: Date.now()
      };
    } else if (path.includes('create')) {
      mcpRequest = {
        jsonrpc: '2.0',
        method: 'tools/call',
        params: {
          name: 'create_template',
          arguments: body
        },
        id: Date.now()
      };
    } else if (path.includes('render')) {
      mcpRequest = {
        jsonrpc: '2.0',
        method: 'tools/call',
        params: {
          name: 'render_template',
          arguments: body
        },
        id: Date.now()
      };
    } else if (path.includes('debate-templates')) {
      mcpRequest = {
        jsonrpc: '2.0',
        method: 'tools/call',
        params: {
          name: 'create_debate_templates',
          arguments: body
        },
        id: Date.now()
      };
    } else {
      return NextResponse.json(
        { error: 'Unknown template operation' },
        { status: 400 }
      );
    }

    const response = await fetch(TEMPLATE_SERVICE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(mcpRequest),
    });

    const data = await response.json();
    
    // Extract the actual result from MCP response
    if (data.result && data.result.content) {
      const content = data.result.content[0];
      if (content.type === 'text') {
        try {
          const parsedContent = JSON.parse(content.text);
          return NextResponse.json(parsedContent);
        } catch {
          return NextResponse.json({ content: content.text });
        }
      }
    }
    
    return NextResponse.json(data);
  } catch (error) {
    apiLogger.error('Template service POST error', error as Error, { operation: params.path.join('/') });
    return NextResponse.json(
      { error: 'Failed to communicate with template service' },
      { status: 500 }
    );
  }
}

export async function PUT(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  try {
    const path = params.path.join('/');
    const body = await request.json();
    
    if (!path.includes('update')) {
      return NextResponse.json(
        { error: 'PUT method only supports update operations' },
        { status: 400 }
      );
    }

    const mcpRequest = {
      jsonrpc: '2.0',
      method: 'tools/call',
      params: {
        name: 'update_template',
        arguments: body
      },
      id: Date.now()
    };

    const response = await fetch(TEMPLATE_SERVICE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(mcpRequest),
    });

    const data = await response.json();
    
    if (data.result && data.result.content) {
      const content = data.result.content[0];
      if (content.type === 'text') {
        try {
          const parsedContent = JSON.parse(content.text);
          return NextResponse.json(parsedContent);
        } catch {
          return NextResponse.json({ content: content.text });
        }
      }
    }
    
    return NextResponse.json(data);
  } catch (error) {
    apiLogger.error('Template service PUT error', error as Error, { operation: params.path.join('/') });
    return NextResponse.json(
      { error: 'Failed to update template' },
      { status: 500 }
    );
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  try {
    const path = params.path.join('/');
    const { template_id, organization_id } = await request.json();
    
    if (!path.includes('delete')) {
      return NextResponse.json(
        { error: 'DELETE method only supports delete operations' },
        { status: 400 }
      );
    }

    const mcpRequest = {
      jsonrpc: '2.0',
      method: 'tools/call',
      params: {
        name: 'delete_template',
        arguments: { template_id, organization_id }
      },
      id: Date.now()
    };

    const response = await fetch(TEMPLATE_SERVICE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(mcpRequest),
    });

    const data = await response.json();
    
    if (data.result && data.result.content) {
      const content = data.result.content[0];
      if (content.type === 'text') {
        try {
          const parsedContent = JSON.parse(content.text);
          return NextResponse.json(parsedContent);
        } catch {
          return NextResponse.json({ success: true });
        }
      }
    }
    
    return NextResponse.json(data);
  } catch (error) {
    apiLogger.error('Template service DELETE error', error as Error, { operation: params.path.join('/') });
    return NextResponse.json(
      { error: 'Failed to delete template' },
      { status: 500 }
    );
  }
}