# MCP-Controller Service API Documentation

This document provides detailed documentation for the MCP-Controller service API endpoints.

## Table of Contents

1. [Create Debate](#create-debate)
2. [Get Debate](#get-debate)
3. [List Debates](#list-debates)
4. [Add Message](#add-message)
5. [Get Next Turn](#get-next-turn)
6. [Summarize Debate](#summarize-debate)
7. [MCP Tool: Create Debate](#mcp-tool-create-debate)
8. [MCP Tool: Add Message](#mcp-tool-add-message)
9. [MCP Tool: Get Next Turn](#mcp-tool-get-next-turn)
10. [MCP Tool: Summarize Debate](#mcp-tool-summarize-debate)

---

## Create Debate

# Endpoint: POST /api/v1/debates

**Description**: Creates a new debate with the specified configuration

**Service**: MCP-Controller

**Authentication Required**: Yes

**Required Headers**:
- `Authorization`: Bearer token
- `X-Organization-ID`: Organization ID
- `Content-Type`: application/json

## Request

### Request Body

```json
{
  "name": "Climate Policy Debate",
  "format": "oxford",
  "description": "Debate on international climate policy approaches",
  "participants": [
    {
      "name": "Team Green",
      "role": "proposition",
      "llmConfig": {
        "provider": "claude",
        "model": "claude-3-opus-20240229",
        "systemPrompt": "You are an expert in environmental policy advocating for aggressive climate action."
      }
    },
    {
      "name": "Team Growth", 
      "role": "opposition",
      "llmConfig": {
        "provider": "openai",
        "model": "gpt-4",
        "systemPrompt": "You are an economist focused on balancing climate action with economic growth."
      }
    }
  ],
  "maxRounds": 6,
  "topic": "Should countries prioritize immediate emissions reductions over economic growth?",
  "rules": ["no_repetition", "stay_on_topic"],
  "metadata": {
    "category": "climate",
    "difficulty": "advanced",
    "tags": ["climate", "economics", "policy"]
  }
}
```

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | Yes | Name of the debate |
| `format` | string | No | Format of the debate (e.g., "oxford", "standard", "freestyle") |
| `description` | string | No | Description of the debate |
| `participants` | array | Yes | List of debate participants |
| `participants[].name` | string | Yes | Name of the participant |
| `participants[].role` | string | No | Role of the participant (e.g., "proposition", "opposition") |
| `participants[].llmConfig` | object | Yes | LLM configuration for the participant |
| `participants[].llmConfig.provider` | string | Yes | LLM provider (e.g., "claude", "openai") |
| `participants[].llmConfig.model` | string | Yes | LLM model to use |
| `participants[].llmConfig.systemPrompt` | string | No | System prompt for the participant |
| `maxRounds` | integer | No | Maximum number of rounds for the debate (default: 5) |
| `topic` | string | No | Topic of the debate |
| `rules` | array | No | List of rules to apply to the debate |
| `metadata` | object | No | Additional metadata for the debate |

## Response

### Success Response (201 Created)

```json
{
  "id": "debate-123456",
  "organizationId": "org-789",
  "name": "Climate Policy Debate",
  "format": "oxford",
  "description": "Debate on international climate policy approaches",
  "participants": [
    {
      "id": "participant-111",
      "name": "Team Green",
      "role": "proposition",
      "llmConfig": {
        "provider": "claude",
        "model": "claude-3-opus-20240229",
        "systemPrompt": "You are an expert in environmental policy advocating for aggressive climate action."
      }
    },
    {
      "id": "participant-222",
      "name": "Team Growth",
      "role": "opposition",
      "llmConfig": {
        "provider": "openai",
        "model": "gpt-4",
        "systemPrompt": "You are an economist focused on balancing climate action with economic growth."
      }
    }
  ],
  "maxRounds": 6,
  "currentRound": 0,
  "currentParticipantIndex": 0,
  "topic": "Should countries prioritize immediate emissions reductions over economic growth?",
  "rules": ["no_repetition", "stay_on_topic"],
  "status": "CREATED",
  "metadata": {
    "category": "climate",
    "difficulty": "advanced",
    "tags": ["climate", "economics", "policy"]
  },
  "createdAt": "2025-07-17T01:00:00Z",
  "updatedAt": "2025-07-17T01:00:00Z"
}
```

| Property | Type | Description |
|----------|------|-------------|
| `id` | string | Unique identifier for the debate |
| `organizationId` | string | Organization ID that owns the debate |
| `name` | string | Name of the debate |
| `format` | string | Format of the debate |
| `description` | string | Description of the debate |
| `participants` | array | List of debate participants |
| `participants[].id` | string | Unique identifier for the participant |
| `participants[].name` | string | Name of the participant |
| `participants[].role` | string | Role of the participant |
| `participants[].llmConfig` | object | LLM configuration for the participant |
| `maxRounds` | integer | Maximum number of rounds for the debate |
| `currentRound` | integer | Current round of the debate |
| `currentParticipantIndex` | integer | Index of the current participant |
| `topic` | string | Topic of the debate |
| `rules` | array | List of rules applied to the debate |
| `status` | string | Status of the debate (e.g., "CREATED", "IN_PROGRESS", "COMPLETED") |
| `metadata` | object | Additional metadata for the debate |
| `createdAt` | string | Creation timestamp (ISO 8601) |
| `updatedAt` | string | Last update timestamp (ISO 8601) |

### Error Responses

#### 400 Bad Request

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Invalid request parameters",
    "details": {
      "participants": "must have at least 2 participants"
    },
    "requestId": "request-456"
  }
}
```

#### 401 Unauthorized

```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required",
    "requestId": "request-456"
  }
}
```

#### 403 Forbidden

```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "Insufficient permissions to create debate",
    "requestId": "request-456"
  }
}
```

## Example

### cURL

```bash
curl -X POST "http://localhost:5013/api/v1/debates" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -H "X-Organization-ID: org-789" \
  -d '{
    "name": "Climate Policy Debate",
    "format": "oxford",
    "description": "Debate on international climate policy approaches",
    "participants": [
      {
        "name": "Team Green",
        "role": "proposition",
        "llmConfig": {
          "provider": "claude",
          "model": "claude-3-opus-20240229",
          "systemPrompt": "You are an expert in environmental policy advocating for aggressive climate action."
        }
      },
      {
        "name": "Team Growth", 
        "role": "opposition",
        "llmConfig": {
          "provider": "openai",
          "model": "gpt-4",
          "systemPrompt": "You are an economist focused on balancing climate action with economic growth."
        }
      }
    ],
    "maxRounds": 6,
    "topic": "Should countries prioritize immediate emissions reductions over economic growth?"
  }'
```

### Python

```python
import requests

url = "http://localhost:5013/api/v1/debates"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-789"
}
payload = {
    "name": "Climate Policy Debate",
    "format": "oxford",
    "description": "Debate on international climate policy approaches",
    "participants": [
      {
        "name": "Team Green",
        "role": "proposition",
        "llmConfig": {
          "provider": "claude",
          "model": "claude-3-opus-20240229",
          "systemPrompt": "You are an expert in environmental policy advocating for aggressive climate action."
        }
      },
      {
        "name": "Team Growth", 
        "role": "opposition",
        "llmConfig": {
          "provider": "openai",
          "model": "gpt-4",
          "systemPrompt": "You are an economist focused on balancing climate action with economic growth."
        }
      }
    ],
    "maxRounds": 6,
    "topic": "Should countries prioritize immediate emissions reductions over economic growth?"
}

response = requests.post(url, headers=headers, json=payload)
print(response.json())
```

## Notes

- Rate limited to 30 debate creations per hour per organization
- Maximum of 10 participants per debate
- Maximum system prompt length is 4,000 characters
- All debates are stored for 30 days by default

---

## Get Debate

# Endpoint: GET /api/v1/debates/{id}

**Description**: Retrieves details of a specific debate

**Service**: MCP-Controller

**Authentication Required**: Yes

**Required Headers**:
- `Authorization`: Bearer token
- `X-Organization-ID`: Organization ID

## Request

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Debate identifier |

## Response

### Success Response (200 OK)

```json
{
  "id": "debate-123456",
  "organizationId": "org-789",
  "name": "Climate Policy Debate",
  "format": "oxford",
  "description": "Debate on international climate policy approaches",
  "participants": [
    {
      "id": "participant-111",
      "name": "Team Green",
      "role": "proposition",
      "llmConfig": {
        "provider": "claude",
        "model": "claude-3-opus-20240229",
        "systemPrompt": "You are an expert in environmental policy advocating for aggressive climate action."
      }
    },
    {
      "id": "participant-222",
      "name": "Team Growth",
      "role": "opposition",
      "llmConfig": {
        "provider": "openai",
        "model": "gpt-4",
        "systemPrompt": "You are an economist focused on balancing climate action with economic growth."
      }
    }
  ],
  "maxRounds": 6,
  "currentRound": 2,
  "currentParticipantIndex": 0,
  "topic": "Should countries prioritize immediate emissions reductions over economic growth?",
  "rules": ["no_repetition", "stay_on_topic"],
  "status": "IN_PROGRESS",
  "metadata": {
    "category": "climate",
    "difficulty": "advanced",
    "tags": ["climate", "economics", "policy"]
  },
  "messageCount": 4,
  "createdAt": "2025-07-16T01:00:00Z",
  "updatedAt": "2025-07-17T01:00:00Z"
}
```

| Property | Type | Description |
|----------|------|-------------|
| `id` | string | Unique identifier for the debate |
| `organizationId` | string | Organization ID that owns the debate |
| `name` | string | Name of the debate |
| `format` | string | Format of the debate |
| `description` | string | Description of the debate |
| `participants` | array | List of debate participants |
| `maxRounds` | integer | Maximum number of rounds for the debate |
| `currentRound` | integer | Current round of the debate |
| `currentParticipantIndex` | integer | Index of the current participant |
| `topic` | string | Topic of the debate |
| `rules` | array | List of rules applied to the debate |
| `status` | string | Status of the debate |
| `metadata` | object | Additional metadata for the debate |
| `messageCount` | integer | Number of messages in the debate |
| `createdAt` | string | Creation timestamp (ISO 8601) |
| `updatedAt` | string | Last update timestamp (ISO 8601) |

### Error Responses

#### 401 Unauthorized

```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required",
    "requestId": "request-456"
  }
}
```

#### 403 Forbidden

```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "Insufficient permissions to access this debate",
    "requestId": "request-456"
  }
}
```

#### 404 Not Found

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Debate not found: debate-123456",
    "requestId": "request-456"
  }
}
```

## Example

### cURL

```bash
curl -X GET "http://localhost:5013/api/v1/debates/debate-123456" \
  -H "Authorization: Bearer your-token" \
  -H "X-Organization-ID: org-789"
```

### Python

```python
import requests

url = "http://localhost:5013/api/v1/debates/debate-123456"
headers = {
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-789"
}

response = requests.get(url, headers=headers)
print(response.json())
```

## Notes

- Debate details include basic information but not the full message history
- To retrieve messages, use the `/api/v1/debates/{id}/messages` endpoint
- System prompts may be redacted for shared debates

---

## List Debates

# Endpoint: GET /api/v1/debates

**Description**: Lists debates for the organization with optional filtering

**Service**: MCP-Controller

**Authentication Required**: Yes

**Required Headers**:
- `Authorization`: Bearer token
- `X-Organization-ID`: Organization ID

## Request

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | integer | No | 0 | Page number for pagination |
| `size` | integer | No | 20 | Page size for pagination |
| `sort` | string | No | "createdAt,desc" | Sort field and direction |
| `status` | string | No | null | Filter by status (e.g., "CREATED", "IN_PROGRESS", "COMPLETED") |
| `format` | string | No | null | Filter by format (e.g., "oxford", "standard") |
| `topic` | string | No | null | Filter by topic (partial match) |
| `participant` | string | No | null | Filter by participant name (partial match) |

## Response

### Success Response (200 OK)

```json
{
  "content": [
    {
      "id": "debate-123456",
      "name": "Climate Policy Debate",
      "format": "oxford",
      "topic": "Should countries prioritize immediate emissions reductions over economic growth?",
      "status": "IN_PROGRESS",
      "participantCount": 2,
      "currentRound": 2,
      "maxRounds": 6,
      "createdAt": "2025-07-16T01:00:00Z",
      "updatedAt": "2025-07-17T01:00:00Z"
    },
    {
      "id": "debate-123457",
      "name": "AI Ethics Debate",
      "format": "standard",
      "topic": "Should AI development be regulated?",
      "status": "COMPLETED",
      "participantCount": 2,
      "currentRound": 5,
      "maxRounds": 5,
      "createdAt": "2025-07-15T01:00:00Z",
      "updatedAt": "2025-07-16T01:00:00Z"
    }
  ],
  "pageable": {
    "page": 0,
    "size": 20,
    "sort": "createdAt,desc"
  },
  "totalElements": 2,
  "totalPages": 1
}
```

| Property | Type | Description |
|----------|------|-------------|
| `content` | array | List of debates |
| `content[].id` | string | Unique identifier for the debate |
| `content[].name` | string | Name of the debate |
| `content[].format` | string | Format of the debate |
| `content[].topic` | string | Topic of the debate |
| `content[].status` | string | Status of the debate |
| `content[].participantCount` | integer | Number of participants in the debate |
| `content[].currentRound` | integer | Current round of the debate |
| `content[].maxRounds` | integer | Maximum number of rounds for the debate |
| `content[].createdAt` | string | Creation timestamp (ISO 8601) |
| `content[].updatedAt` | string | Last update timestamp (ISO 8601) |
| `pageable` | object | Pagination information |
| `pageable.page` | integer | Current page number |
| `pageable.size` | integer | Page size |
| `pageable.sort` | string | Sort field and direction |
| `totalElements` | integer | Total number of debates matching the criteria |
| `totalPages` | integer | Total number of pages |

### Error Responses

#### 401 Unauthorized

```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required",
    "requestId": "request-456"
  }
}
```

#### 403 Forbidden

```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "Insufficient permissions to list debates",
    "requestId": "request-456"
  }
}
```

## Example

### cURL

```bash
curl -X GET "http://localhost:5013/api/v1/debates?status=IN_PROGRESS&format=oxford&page=0&size=10" \
  -H "Authorization: Bearer your-token" \
  -H "X-Organization-ID: org-789"
```

### Python

```python
import requests

url = "http://localhost:5013/api/v1/debates"
headers = {
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-789"
}
params = {
    "status": "IN_PROGRESS",
    "format": "oxford",
    "page": 0,
    "size": 10
}

response = requests.get(url, headers=headers, params=params)
print(response.json())
```

## Notes

- Results are paginated with a default page size of 20
- Maximum page size is 100
- List endpoint returns summarized debate information, not full details
- For full debate details, use the GET /api/v1/debates/{id} endpoint
