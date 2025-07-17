# API Documentation Overview

This document provides an overview of the APIs available in the Zamaz Debate MCP system.

## API Design Principles

The Zamaz Debate MCP APIs follow these design principles:

1. **RESTful Design**: Resources are represented as URLs, with standard HTTP methods
2. **JSON Format**: All requests and responses use JSON format
3. **Authentication**: APIs require authentication via API keys or JWT tokens
4. **Multi-tenant**: All APIs respect organization boundaries
5. **Versioning**: APIs are versioned to ensure backward compatibility
6. **Consistent Error Handling**: Standard error response format across all services

## Common Headers

All API requests should include these headers:

```
X-Organization-ID: your-org-id
Content-Type: application/json
Authorization: Bearer your-jwt-token
```

## Error Response Format

All APIs return errors in this standard format:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": {
      "field": "Additional error context"
    },
    "request_id": "unique-request-identifier"
  }
}
```

## API Services

### MCP Organization Service (Port 5005)

Manages organizations and multi-tenant functionality.

**Key Endpoints:**
- `POST /api/v1/organizations` - Create organization
- `GET /api/v1/organizations/{id}` - Get organization details
- `PUT /api/v1/organizations/{id}` - Update organization
- `GET /api/v1/organizations/{id}/api-keys` - List API keys
- `POST /api/v1/organizations/{id}/api-keys` - Create API key

**MCP Tools:**
- `create_organization` - Create new organization
- `get_organization` - Get organization details
- `update_organization` - Update organization settings
- `list_api_keys` - List organization API keys
- `create_api_key` - Create new API key

### MCP LLM Service (Port 5002)

Provides unified access to multiple LLM providers.

**Key Endpoints:**
- `POST /api/v1/completions` - Generate completion
- `POST /api/v1/completions/stream` - Stream completion
- `GET /api/v1/models` - List available models
- `POST /api/v1/tokens/count` - Count tokens

**MCP Tools:**
- `complete` - Generate completion
- `stream_complete` - Stream responses
- `list_models` - Available models
- `estimate_tokens` - Token counting

### MCP Controller Service (Port 5013)

Orchestrates debates and manages debate flow.

**Key Endpoints:**
- `POST /api/v1/debates` - Create debate
- `GET /api/v1/debates/{id}` - Get debate details
- `POST /api/v1/debates/{id}/messages` - Add message to debate
- `GET /api/v1/debates/{id}/messages` - Get debate messages
- `POST /api/v1/debates/{id}/next-turn` - Get next turn

**MCP Tools:**
- `create_debate` - Initialize debate
- `add_message` - Add to debate
- `get_next_turn` - Orchestrate turns
- `summarize_debate` - Generate summary

### MCP RAG Service (Port 5004)

Enhances responses with retrieval augmented generation.

**Key Endpoints:**
- `POST /api/v1/knowledge-bases` - Create knowledge base
- `POST /api/v1/knowledge-bases/{id}/documents` - Add documents
- `POST /api/v1/knowledge-bases/{id}/search` - Search knowledge base
- `POST /api/v1/augment` - Augment context with retrieved info

**MCP Tools:**
- `create_knowledge_base` - New KB
- `ingest_documents` - Add documents
- `search` - Semantic search
- `augment_context` - Enhance with retrieved info

### MCP Template Service (Port 5006)

Manages templates for debates and other content.

**Key Endpoints:**
- `POST /api/v1/templates` - Create template
- `GET /api/v1/templates/{id}` - Get template
- `PUT /api/v1/templates/{id}` - Update template
- `POST /api/v1/templates/{id}/instantiate` - Create instance from template

**MCP Tools:**
- `create_template` - Create new template
- `get_template` - Get template details
- `update_template` - Update template
- `instantiate_template` - Create instance from template

## Authentication and Authorization

### API Key Authentication

For service-to-service communication:

```
Authorization: Bearer api-key-value
```

### JWT Authentication

For user authentication:

```
Authorization: Bearer jwt-token-value
```

JWT tokens contain claims about the user and their organization:

```json
{
  "sub": "user-123",
  "org_id": "org-456",
  "roles": ["admin"],
  "exp": 1687267200
}
```

## Rate Limiting

APIs implement rate limiting based on organization tier:

| Tier | Requests/Minute | Requests/Day | Concurrent Requests |
|------|----------------|--------------|---------------------|
| Basic | 30 | 5,000 | 5 |
| Pro | 60 | 10,000 | 10 |
| Enterprise | 120 | 50,000 | 20 |

Rate limit headers are included in responses:

```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 59
X-RateLimit-Reset: 1626192000
```

## Pagination

List endpoints support pagination with these query parameters:

- `page`: Page number (starting from 0)
- `size`: Page size
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

Response includes pagination metadata:

```json
{
  "content": [...],
  "pageable": {
    "page": 0,
    "size": 20,
    "sort": "createdAt,desc"
  },
  "totalElements": 100,
  "totalPages": 5
}
```

## API Versioning

APIs are versioned in the URL path:

```
/api/v1/resource
/api/v2/resource
```

Breaking changes are only introduced in new API versions.

## API Examples

### Create a Debate

**Request:**
```bash
curl -X POST http://localhost:5013/api/v1/debates \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -H "Authorization: Bearer your-token" \
  -d '{
    "name": "AI Ethics Debate",
    "participants": [
      {
        "name": "Pro-Regulation",
        "llmConfig": {
          "provider": "claude",
          "model": "claude-3-opus-20240229",
          "systemPrompt": "Argue for AI regulation"
        }
      },
      {
        "name": "Pro-Innovation", 
        "llmConfig": {
          "provider": "openai",
          "model": "gpt-4",
          "systemPrompt": "Argue against excessive regulation"
        }
      }
    ],
    "maxRounds": 5
  }'
```

**Response:**
```json
{
  "id": "debate-123",
  "name": "AI Ethics Debate",
  "status": "CREATED",
  "participants": [...],
  "currentParticipantIndex": 0,
  "rounds": 0,
  "maxRounds": 5,
  "createdAt": "2025-07-16T12:00:00Z",
  "organizationId": "org-123"
}
```

### Generate LLM Completion

**Request:**
```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -H "Authorization: Bearer your-token" \
  -d '{
    "model": "claude-3-opus-20240229",
    "prompt": "Explain the concept of retrieval augmented generation",
    "maxTokens": 500,
    "temperature": 0.7
  }'
```

**Response:**
```json
{
  "id": "completion-456",
  "model": "claude-3-opus-20240229",
  "content": "Retrieval Augmented Generation (RAG) is...",
  "finishReason": "stop",
  "usage": {
    "promptTokens": 8,
    "completionTokens": 486,
    "totalTokens": 494
  }
}
```

## API Documentation by Service

For detailed API documentation for each service, see:

- [Organization Service API](./endpoints/organization.md)
- [LLM Service API](./endpoints/llm.md)
- [Controller Service API](./endpoints/controller.md)
- [RAG Service API](./endpoints/rag.md)
- [Template Service API](./endpoints/template.md)
