# API Documentation

## Overview

The MCP Debate System provides a comprehensive REST API for managing debates, organizations, and AI interactions. All endpoints support JSON and require authentication unless specified otherwise.

## Table of Contents

1. [Authentication](#authentication)
2. [Organization API](endpoints/organization.md)
3. [Debate API](endpoints/debate.md)
4. [LLM API](endpoints/llm.md)
5. [WebSocket API](#websocket-api)
6. [Error Handling](#error-handling)
7. [Rate Limiting](#rate-limiting)

## Base URLs

- **Production**: `https://api.mcp-debate.com`
- **Staging**: `https://staging-api.mcp-debate.com`
- **Local**: `http://localhost:8080`

## Authentication

All API requests require authentication using JWT tokens.

### Obtaining a Token

```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secure-password"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600,
  "user": {
    "id": "user-123",
    "email": "user@example.com",
    "organizationId": "org-456"
  }
}
```

### Using the Token

Include the token in the Authorization header:

```bash
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

## Common Endpoints

### Health Check
```bash
GET /health

Response:
{
  "status": "UP",
  "timestamp": "2024-01-17T10:30:00Z",
  "services": {
    "database": "UP",
    "redis": "UP",
    "llm-gateway": "UP"
  }
}
```

### API Version
```bash
GET /api/version

Response:
{
  "version": "1.0.0",
  "apiVersion": "v1",
  "buildTime": "2024-01-17T10:00:00Z"
}
```

## Response Format

All API responses follow a consistent format:

### Success Response
```json
{
  "success": true,
  "data": {
    // Response data
  },
  "timestamp": "2024-01-17T10:30:00Z"
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input parameters",
    "details": {
      "field": "email",
      "reason": "Invalid email format"
    }
  },
  "timestamp": "2024-01-17T10:30:00Z"
}
```

## Pagination

List endpoints support pagination:

```bash
GET /api/debates?page=0&size=20&sort=createdAt,desc

Response:
{
  "content": [...],
  "pageable": {
    "sort": {
      "sorted": true,
      "ascending": false
    },
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

## WebSocket API

Real-time updates via WebSocket connections.

### Connection
```javascript
const ws = new WebSocket('wss://api.mcp-debate.com/ws/debate');
ws.onopen = () => {
  ws.send(JSON.stringify({
    action: 'subscribe',
    debateId: 'debate-123',
    token: 'jwt-token'
  }));
};
```

### Message Types

#### Debate Update
```json
{
  "type": "debate.update",
  "debateId": "debate-123",
  "data": {
    "status": "IN_PROGRESS",
    "currentRound": 2
  }
}
```

#### New Message
```json
{
  "type": "debate.message",
  "debateId": "debate-123",
  "data": {
    "id": "msg-456",
    "participantId": "claude",
    "content": "I argue that...",
    "timestamp": "2024-01-17T10:30:00Z"
  }
}
```

## Error Handling

### Error Codes

| Code | Description | HTTP Status |
|------|-------------|-------------|
| AUTHENTICATION_REQUIRED | Missing or invalid token | 401 |
| FORBIDDEN | Insufficient permissions | 403 |
| NOT_FOUND | Resource not found | 404 |
| VALIDATION_ERROR | Invalid input | 400 |
| RATE_LIMIT_EXCEEDED | Too many requests | 429 |
| INTERNAL_ERROR | Server error | 500 |

### Error Response Example
```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded",
    "details": {
      "limit": 100,
      "remaining": 0,
      "resetAt": "2024-01-17T11:00:00Z"
    }
  }
}
```

## Rate Limiting

API requests are rate limited per organization:

- **Standard**: 100 requests/minute
- **Professional**: 500 requests/minute
- **Enterprise**: 2000 requests/minute

Rate limit headers:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1705492800
```

## API Examples

### Quick Start Examples

#### Create a Debate
```bash
curl -X POST https://api.mcp-debate.com/api/debate/create \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "AI Ethics Debate",
    "topic": "Should AI have rights?",
    "participants": [
      {
        "name": "Claude",
        "position": "PRO",
        "aiProvider": "CLAUDE"
      },
      {
        "name": "GPT-4",
        "position": "CON",
        "aiProvider": "OPENAI"
      }
    ]
  }'
```

#### List Debates
```bash
curl https://api.mcp-debate.com/api/debate/list \
  -H "Authorization: Bearer $TOKEN"
```

#### Get Debate Status
```bash
curl https://api.mcp-debate.com/api/debate/debate-123 \
  -H "Authorization: Bearer $TOKEN"
```

## SDK Support

Official SDKs available:

- **JavaScript/TypeScript**: `npm install @mcp-debate/sdk`
- **Python**: `pip install mcp-debate-sdk`
- **Java**: Maven dependency available
- **Go**: `go get github.com/zamaz/mcp-debate-sdk-go`

## API Versioning

The API uses URL versioning. Current version: `v1`

Future versions will be available at:
- `/api/v2/...`
- `/api/v3/...`

Deprecation policy:
- 6 months notice before deprecation
- 12 months support after new version release

## Further Reading

- [Organization API Documentation](endpoints/organization.md)
- [Debate API Documentation](endpoints/debate.md)
- [LLM API Documentation](endpoints/llm.md)
- [cURL Examples](DEBATE_CURL_EXAMPLES.md)
- [API Testing Guide](API_TEST_PLAN.md)