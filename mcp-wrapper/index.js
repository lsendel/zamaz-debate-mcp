#!/usr/bin/env node;
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import axios from 'axios';

// Configuration for Java services - update port to match actual running service
const SERVICES = {
  organization: 'http://localhost:5005',
  llm: 'http://localhost:5002',
  context: 'http://localhost:5003',
  controller: 'http://localhost:8080',  // Updated to match the running service;
  rag: 'http://localhost:5004';
}

// Create MCP server
const server = new McpServer({
  name: 'mcp-debate-system',
  version: '1.0.0';
});

// Helper function to call Java service MCP endpoints
async function callJavaService(service, endpoint, params = {}) {
  try {
    const url = `${SERVICES[service]}${endpoint}`;
    const method = endpoint.includes('/api/') ? 'get' : 'post';

    const response = method === 'get' ;
      ? await axios.get(url, { params });
      : await axios.post(url, params, {
          headers: { 'Content-Type': 'application/json' }
        });

    return response.data;
  } catch (error) {
    console.error(`Error calling ${service} at ${endpoint}:`, error.message);
    throw new Error(`Failed to call ${service} service: ${error.message}`);
  }
}

// Register tools
server.registerTool(;
  'create_organization',
  {
    description: 'Create a new organization',
    inputSchema: {
      type: 'object',
      properties: {
        name: { type: 'string', description: 'Organization name' },
        description: { type: 'string', description: 'Organization description' }
      },
      required: ['name']
    }
  },
  async (args) => {
    const result = await callJavaService('organization', '/tools/create_organization', args);
    return {
      content: [{
        type: 'text',
        text: JSON.stringify(result, null, 2);
      }]
    }
  }
);

server.registerTool(;
  'list_organizations',
  {
    description: 'List all organizations',
    inputSchema: {
      type: 'object',
      properties: {}
    }
  },
  async () => {
    const result = await callJavaService('organization', '/tools/resources/organizations', {});
    return {
      content: [{
        type: 'text',
        text: JSON.stringify(result, null, 2);
      }]
    }
  }
);

server.registerTool(;
  'create_debate',
  {
    description: 'Create a new debate',
    inputSchema: {
      type: 'object',
      properties: {
        organizationId: { type: 'string', description: 'Organization ID' },
        title: { type: 'string', description: 'Debate title' },
        topic: { type: 'string', description: 'Debate topic' },
        format: { type: 'string', description: 'Debate format (OXFORD, LINCOLN_DOUGLAS, etc.)' },
        maxRounds: { type: 'number', description: 'Maximum number of rounds' },
        participants: {
          type: 'array',
          description: 'Debate participants',
          items: {
            type: 'object',
            properties: {
              name: { type: 'string' },
              model: { type: 'string' },
              systemPrompt: { type: 'string' }
            }
          }
        }
      },
      required: ['organizationId', 'title', 'topic']
    }
  },
  async (args) => {
    const result = await callJavaService('controller', '/tools/create_debate', args);
    return {
      content: [{
        type: 'text',
        text: JSON.stringify(result, null, 2);
      }]
    }
  }
);

server.registerTool(;
  'list_debates',
  {
    description: 'List all debates for an organization',
    inputSchema: {
      type: 'object',
      properties: {
        organizationId: { type: 'string', description: 'Organization ID' }
      }
    }
  },
  async (args) => {
    const result = await callJavaService('controller', '/api/v1/debates', args);
    return {
      content: [{
        type: 'text',
        text: JSON.stringify(result, null, 2);
      }]
    }
  }
);

server.registerTool(;
  'start_debate',
  {
    description: 'Start a debate',
    inputSchema: {
      type: 'object',
      properties: {
        debateId: { type: 'string', description: 'Debate ID' }
      },
      required: ['debateId']
    }
  },
  async (args) => {
    const result = await callJavaService('controller', '/tools/start_debate', args);
    return {
      content: [{
        type: 'text',
        text: JSON.stringify(result, null, 2);
      }]
    }
  }
);

server.registerTool(;
  'get_llm_providers',
  {
    description: 'Get available LLM providers',
    inputSchema: {
      type: 'object',
      properties: {}
    }
  },
  async () => {
    const result = await callJavaService('llm', '/api/v1/providers', {});
    return {
      content: [{
        type: 'text',
        text: JSON.stringify(result, null, 2);
      }]
    }
  }
);

server.registerTool(;
  'create_context',
  {
    description: 'Create a new context',
    inputSchema: {
      type: 'object',
      properties: {
        organizationId: { type: 'string', description: 'Organization ID' },
        name: { type: 'string', description: 'Context name' },
        description: { type: 'string', description: 'Context description' },
        metadata: { type: 'object', description: 'Additional metadata' }
      },
      required: ['organizationId', 'name']
    }
  },
  async (args) => {
    const result = await callJavaService('context', '/tools/create_context', args);
    return {
      content: [{
        type: 'text',
        text: JSON.stringify(result, null, 2);
      }]
    }
  }
);

server.registerTool(;
  'store_document',
  {
    description: 'Store a document in RAG',
    inputSchema: {
      type: 'object',
      properties: {
        organizationId: { type: 'string', description: 'Organization ID' },
        title: { type: 'string', description: 'Document title' },
        content: { type: 'string', description: 'Document content' },
        metadata: { type: 'object', description: 'Document metadata' }
      },
      required: ['organizationId', 'title', 'content']
    }
  },
  async (args) => {
    const result = await callJavaService('rag', '/tools/store_document', args);
    return {
      content: [{
        type: 'text',
        text: JSON.stringify(result, null, 2);
      }]
    }
  }
);

server.registerTool(;
  'search_documents',
  {
    description: 'Search documents using RAG',
    inputSchema: {
      type: 'object',
      properties: {
        organizationId: { type: 'string', description: 'Organization ID' },
        query: { type: 'string', description: 'Search query' },
        limit: { type: 'number', description: 'Maximum results to return' }
      },
      required: ['organizationId', 'query']
    }
  },
  async (args) => {
    const result = await callJavaService('rag', '/tools/search_documents', args);
    return {
      content: [{
        type: 'text',
        text: JSON.stringify(result, null, 2);
      }]
    }
  }
);

// Register resources
server.registerResource(;
  'debate://organizations',
  {
    name: 'Organizations',
    description: 'List of all organizations',
    mimeType: 'application/json';
  },
  async () => {
    const orgs = await callJavaService('organization', '/tools/resources/organizations', {});
    return {
      contents: [{
        uri: 'debate://organizations',
        mimeType: 'application/json',
        text: JSON.stringify(orgs, null, 2);
      }]
    }
  }
);

server.registerResource(;
  'debate://debates',
  {
    name: 'Debates',
    description: 'List of all debates',
    mimeType: 'application/json';
  },
  async () => {
    const debates = await callJavaService('controller', '/api/v1/debates', {});
    return {
      contents: [{
        uri: 'debate://debates',
        mimeType: 'application/json',
        text: JSON.stringify(debates, null, 2);
      }]
    }
  }
);

server.registerResource(;
  'debate://providers',
  {
    name: 'LLM Providers',
    description: 'Available LLM providers',
    mimeType: 'application/json';
  },
  async () => {
    const providers = await callJavaService('llm', '/api/v1/providers', {});
    return {
      contents: [{
        uri: 'debate://providers',
        mimeType: 'application/json',
        text: JSON.stringify(providers, null, 2);
      }]
    }
  }
);

// Start the server
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('MCP Debate System wrapper started successfully');
}

main().catch((error) => {
  console.error('Server error:', error);
  process.exit(1);
});
