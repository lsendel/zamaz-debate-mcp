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
# MCP-Controller Service API Documentation (Continued)

This document continues the detailed documentation for the MCP-Controller service API endpoints.

---

## Add Message

# Endpoint: POST /api/v1/debates/{id}/messages

**Description**: Adds a message to a debate

**Service**: MCP-Controller

**Authentication Required**: Yes

**Required Headers**:
- `Authorization`: Bearer token
- `X-Organization-ID`: Organization ID
- `Content-Type`: application/json

## Request

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Debate identifier |

### Request Body

```json
{
  "participantId": "participant-111",
  "content": "While I acknowledge the urgency of climate change, we must consider the economic impact of rapid transitions...",
  "metadata": {
    "citations": [
      {"source": "IMF Economic Outlook 2024", "page": 42}
    ],
    "sentiment": "neutral",
    "keywords": ["economic impact", "transition", "climate change"]
  }
}
```

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `participantId` | string | Yes | ID of the participant adding the message |
| `content` | string | Yes | Content of the message |
| `metadata` | object | No | Additional metadata for the message |

## Response

### Success Response (201 Created)

```json
{
  "id": "message-789012",
  "debateId": "debate-123456",
  "participantId": "participant-111",
  "content": "While I acknowledge the urgency of climate change, we must consider the economic impact of rapid transitions...",
  "round": 2,
  "turnIndex": 0,
  "metadata": {
    "citations": [
      {"source": "IMF Economic Outlook 2024", "page": 42}
    ],
    "sentiment": "neutral",
    "keywords": ["economic impact", "transition", "climate change"],
    "tokenCount": 24
  },
  "createdAt": "2025-07-17T01:05:00Z"
}
```

| Property | Type | Description |
|----------|------|-------------|
| `id` | string | Unique identifier for the message |
| `debateId` | string | ID of the debate |
| `participantId` | string | ID of the participant who added the message |
| `content` | string | Content of the message |
| `round` | integer | Round number of the debate |
| `turnIndex` | integer | Turn index within the round |
| `metadata` | object | Additional metadata for the message |
| `createdAt` | string | Creation timestamp (ISO 8601) |

### Error Responses

#### 400 Bad Request

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Invalid request parameters",
    "details": {
      "content": "must not be empty"
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
    "message": "Insufficient permissions to add message to this debate",
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

#### 409 Conflict

```json
{
  "error": {
    "code": "INVALID_TURN",
    "message": "Not this participant's turn",
    "details": {
      "currentParticipantId": "participant-222"
    },
    "requestId": "request-456"
  }
}
```

## Example

### cURL

```bash
curl -X POST "http://localhost:5013/api/v1/debates/debate-123456/messages" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -H "X-Organization-ID: org-789" \
  -d '{
    "participantId": "participant-111",
    "content": "While I acknowledge the urgency of climate change, we must consider the economic impact of rapid transitions..."
  }'
```

### Python

```python
import requests

url = "http://localhost:5013/api/v1/debates/debate-123456/messages"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-789"
}
payload = {
    "participantId": "participant-111",
    "content": "While I acknowledge the urgency of climate change, we must consider the economic impact of rapid transitions..."
}

response = requests.post(url, headers=headers, json=payload)
print(response.json())
```

## Notes

- Messages can only be added by the participant whose turn it is, unless the debate format allows free participation
- Maximum message length is 100,000 characters
- Messages are automatically assigned to the current round and turn
- Token count is automatically calculated and added to metadata

---

## Get Next Turn

# Endpoint: POST /api/v1/debates/{id}/next-turn

**Description**: Gets the next turn in a debate, optionally generating an AI response

**Service**: MCP-Controller

**Authentication Required**: Yes

**Required Headers**:
- `Authorization`: Bearer token
- `X-Organization-ID`: Organization ID
- `Content-Type`: application/json

## Request

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Debate identifier |

### Request Body

```json
{
  "generateResponse": true,
  "contextStrategy": "full_history",
  "maxTokens": 1000,
  "temperature": 0.7,
  "includeMetadata": true
}
```

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `generateResponse` | boolean | No | Whether to generate an AI response (default: false) |
| `contextStrategy` | string | No | Strategy for context generation (e.g., "full_history", "last_round", "summary_with_last_round") |
| `maxTokens` | integer | No | Maximum tokens for generated response |
| `temperature` | number | No | Temperature for response generation |
| `includeMetadata` | boolean | No | Whether to include metadata in the response |

## Response

### Success Response (200 OK)

```json
{
  "debateId": "debate-123456",
  "currentRound": 2,
  "currentParticipant": {
    "id": "participant-222",
    "name": "Team Growth",
    "role": "opposition"
  },
  "previousMessage": {
    "id": "message-789012",
    "participantId": "participant-111",
    "content": "While I acknowledge the urgency of climate change, we must consider the economic impact of rapid transitions...",
    "round": 2,
    "turnIndex": 0,
    "createdAt": "2025-07-17T01:05:00Z"
  },
  "generatedResponse": {
    "content": "Thank you for acknowledging the urgency of climate change. I agree this is a critical issue, but I'd like to expand on the economic considerations...",
    "metadata": {
      "tokenCount": 487,
      "citations": [
        {"source": "World Economic Forum 2024 Report", "page": 23}
      ]
    }
  },
  "context": {
    "messageCount": 5,
    "tokenCount": 1250,
    "strategy": "full_history"
  },
  "nextActions": [
    {
      "action": "add_message",
      "participantId": "participant-222",
      "endpoint": "/api/v1/debates/debate-123456/messages"
    },
    {
      "action": "skip_turn",
      "endpoint": "/api/v1/debates/debate-123456/skip-turn"
    }
  ]
}
```

| Property | Type | Description |
|----------|------|-------------|
| `debateId` | string | ID of the debate |
| `currentRound` | integer | Current round of the debate |
| `currentParticipant` | object | Information about the current participant |
| `previousMessage` | object | Information about the previous message |
| `generatedResponse` | object | Generated AI response (if requested) |
| `context` | object | Context information used for generation |
| `nextActions` | array | Available actions for the next step |

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

#### 409 Conflict

```json
{
  "error": {
    "code": "DEBATE_COMPLETED",
    "message": "Debate has already been completed",
    "requestId": "request-456"
  }
}
```

## Example

### cURL

```bash
curl -X POST "http://localhost:5013/api/v1/debates/debate-123456/next-turn" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -H "X-Organization-ID: org-789" \
  -d '{
    "generateResponse": true,
    "contextStrategy": "full_history",
    "maxTokens": 1000
  }'
```

### Python

```python
import requests

url = "http://localhost:5013/api/v1/debates/debate-123456/next-turn"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-789"
}
payload = {
    "generateResponse": True,
    "contextStrategy": "full_history",
    "maxTokens": 1000
}

response = requests.post(url, headers=headers, json=payload)
print(response.json())
```

## Notes

- If `generateResponse` is true, the service will generate an AI response using the current participant's LLM configuration
- The generated response is not automatically added to the debate; it must be explicitly added using the add message endpoint
- Context strategies determine how much of the debate history is included in the context for response generation
- If the debate has reached the maximum number of rounds, the next turn will indicate that the debate is complete
# MCP-Controller Service API Documentation (Continued)

This document continues the detailed documentation for the MCP-Controller service API endpoints.

---

## Summarize Debate

# Endpoint: POST /api/v1/debates/{id}/summarize

**Description**: Generates a summary of the debate

**Service**: MCP-Controller

**Authentication Required**: Yes

**Required Headers**:
- `Authorization`: Bearer token
- `X-Organization-ID`: Organization ID
- `Content-Type`: application/json

## Request

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Debate identifier |

### Request Body

```json
{
  "strategy": "pro_con",
  "maxLength": 500,
  "includeKeyPoints": true,
  "focusAreas": ["economic impact", "climate science"],
  "temperature": 0.3
}
```

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `strategy` | string | No | Summary strategy (e.g., "pro_con", "chronological", "key_points", "thematic") |
| `maxLength` | integer | No | Maximum length of the summary in tokens |
| `includeKeyPoints` | boolean | No | Whether to include key points in the summary |
| `focusAreas` | array | No | Specific areas to focus on in the summary |
| `temperature` | number | No | Temperature for summary generation |

## Response

### Success Response (200 OK)

```json
{
  "id": "summary-345678",
  "debateId": "debate-123456",
  "content": "This debate focused on whether countries should prioritize immediate emissions reductions over economic growth. Team Green argued that urgent climate action is necessary to prevent catastrophic outcomes, citing recent IPCC reports showing accelerating climate change impacts. Team Growth countered that economic stability is essential for sustainable climate action, arguing that rapid transitions could disproportionately harm developing economies...",
  "keyPoints": [
    {
      "side": "Team Green",
      "points": [
        "Climate tipping points require immediate action regardless of economic cost",
        "Technological solutions exist but need rapid deployment",
        "Economic costs of inaction exceed costs of immediate action"
      ]
    },
    {
      "side": "Team Growth",
      "points": [
        "Economic stability is prerequisite for sustainable climate action",
        "Developing nations need different transition timelines",
        "Market-based solutions can drive efficient emissions reductions"
      ]
    }
  ],
  "metadata": {
    "strategy": "pro_con",
    "tokenCount": 487,
    "messagesCovered": 6,
    "roundsCovered": 3
  },
  "createdAt": "2025-07-17T01:10:00Z"
}
```

| Property | Type | Description |
|----------|------|-------------|
| `id` | string | Unique identifier for the summary |
| `debateId` | string | ID of the debate |
| `content` | string | Summary content |
| `keyPoints` | array | Key points from the debate (if requested) |
| `metadata` | object | Additional metadata for the summary |
| `createdAt` | string | Creation timestamp (ISO 8601) |

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
    "message": "Insufficient permissions to summarize this debate",
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

#### 422 Unprocessable Entity

```json
{
  "error": {
    "code": "INSUFFICIENT_CONTENT",
    "message": "Debate has insufficient content for summarization",
    "details": {
      "minimumMessages": 4,
      "currentMessages": 2
    },
    "requestId": "request-456"
  }
}
```

## Example

### cURL

```bash
curl -X POST "http://localhost:5013/api/v1/debates/debate-123456/summarize" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -H "X-Organization-ID: org-789" \
  -d '{
    "strategy": "pro_con",
    "maxLength": 500,
    "includeKeyPoints": true
  }'
```

### Python

```python
import requests

url = "http://localhost:5013/api/v1/debates/debate-123456/summarize"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-789"
}
payload = {
    "strategy": "pro_con",
    "maxLength": 500,
    "includeKeyPoints": True
}

response = requests.post(url, headers=headers, json=payload)
print(response.json())
```

## Notes

- Summarization requires at least 4 messages in the debate
- Summary strategies determine the structure and focus of the generated summary
- Summaries are stored and can be retrieved later
- Multiple summaries can be generated for the same debate with different strategies

---

## MCP Tool: Create Debate

# MCP Tool: create_debate

**Description**: Creates a new debate with the specified configuration

**Service**: MCP-Controller

**Authentication Required**: Yes

## Parameters

```json
{
  "name": "Climate Policy Debate",
  "format": "oxford",
  "description": "Debate on international climate policy approaches",
  "participants": [
    {
      "name": "Team Green",
      "role": "proposition",
      "llm_config": {
        "provider": "claude",
        "model": "claude-3-opus-20240229",
        "system_prompt": "You are an expert in environmental policy advocating for aggressive climate action."
      }
    },
    {
      "name": "Team Growth", 
      "role": "opposition",
      "llm_config": {
        "provider": "openai",
        "model": "gpt-4",
        "system_prompt": "You are an economist focused on balancing climate action with economic growth."
      }
    }
  ],
  "max_rounds": 6,
  "topic": "Should countries prioritize immediate emissions reductions over economic growth?",
  "rules": ["no_repetition", "stay_on_topic"],
  "metadata": {
    "category": "climate",
    "difficulty": "advanced",
    "tags": ["climate", "economics", "policy"]
  }
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | Name of the debate |
| `format` | string | No | Format of the debate |
| `description` | string | No | Description of the debate |
| `participants` | array | Yes | List of debate participants |
| `participants[].name` | string | Yes | Name of the participant |
| `participants[].role` | string | No | Role of the participant |
| `participants[].llm_config` | object | Yes | LLM configuration for the participant |
| `max_rounds` | integer | No | Maximum number of rounds for the debate |
| `topic` | string | No | Topic of the debate |
| `rules` | array | No | List of rules to apply to the debate |
| `metadata` | object | No | Additional metadata for the debate |

## Result

```json
{
  "id": "debate-123456",
  "organization_id": "org-789",
  "name": "Climate Policy Debate",
  "format": "oxford",
  "description": "Debate on international climate policy approaches",
  "participants": [
    {
      "id": "participant-111",
      "name": "Team Green",
      "role": "proposition",
      "llm_config": {
        "provider": "claude",
        "model": "claude-3-opus-20240229",
        "system_prompt": "You are an expert in environmental policy advocating for aggressive climate action."
      }
    },
    {
      "id": "participant-222",
      "name": "Team Growth",
      "role": "opposition",
      "llm_config": {
        "provider": "openai",
        "model": "gpt-4",
        "system_prompt": "You are an economist focused on balancing climate action with economic growth."
      }
    }
  ],
  "max_rounds": 6,
  "current_round": 0,
  "current_participant_index": 0,
  "topic": "Should countries prioritize immediate emissions reductions over economic growth?",
  "rules": ["no_repetition", "stay_on_topic"],
  "status": "CREATED",
  "metadata": {
    "category": "climate",
    "difficulty": "advanced",
    "tags": ["climate", "economics", "policy"]
  },
  "created_at": "2025-07-17T01:00:00Z",
  "updated_at": "2025-07-17T01:00:00Z"
}
```

## Example

### Python

```python
from mcp_client import MCPClient

client = MCPClient("http://localhost:5013")

result = await client.call_tool("create_debate", {
    "name": "Climate Policy Debate",
    "format": "oxford",
    "description": "Debate on international climate policy approaches",
    "participants": [
      {
        "name": "Team Green",
        "role": "proposition",
        "llm_config": {
          "provider": "claude",
          "model": "claude-3-opus-20240229",
          "system_prompt": "You are an expert in environmental policy advocating for aggressive climate action."
        }
      },
      {
        "name": "Team Growth", 
        "role": "opposition",
        "llm_config": {
          "provider": "openai",
          "model": "gpt-4",
          "system_prompt": "You are an economist focused on balancing climate action with economic growth."
        }
      }
    ],
    "max_rounds": 6,
    "topic": "Should countries prioritize immediate emissions reductions over economic growth?"
})

print(f"Created debate: {result['id']}")
```

## Notes

- Rate limited to 30 debate creations per hour per organization
- Maximum of 10 participants per debate
- Maximum system prompt length is 4,000 characters
- All debates are stored for 30 days by default

---

## MCP Tool: Add Message

# MCP Tool: add_message

**Description**: Adds a message to a debate

**Service**: MCP-Controller

**Authentication Required**: Yes

## Parameters

```json
{
  "debate_id": "debate-123456",
  "participant_id": "participant-111",
  "content": "While I acknowledge the urgency of climate change, we must consider the economic impact of rapid transitions...",
  "metadata": {
    "citations": [
      {"source": "IMF Economic Outlook 2024", "page": 42}
    ],
    "sentiment": "neutral",
    "keywords": ["economic impact", "transition", "climate change"]
  }
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `debate_id` | string | Yes | ID of the debate |
| `participant_id` | string | Yes | ID of the participant adding the message |
| `content` | string | Yes | Content of the message |
| `metadata` | object | No | Additional metadata for the message |

## Result

```json
{
  "id": "message-789012",
  "debate_id": "debate-123456",
  "participant_id": "participant-111",
  "content": "While I acknowledge the urgency of climate change, we must consider the economic impact of rapid transitions...",
  "round": 2,
  "turn_index": 0,
  "metadata": {
    "citations": [
      {"source": "IMF Economic Outlook 2024", "page": 42}
    ],
    "sentiment": "neutral",
    "keywords": ["economic impact", "transition", "climate change"],
    "token_count": 24
  },
  "created_at": "2025-07-17T01:05:00Z"
}
```

## Example

### Python

```python
from mcp_client import MCPClient

client = MCPClient("http://localhost:5013")

result = await client.call_tool("add_message", {
    "debate_id": "debate-123456",
    "participant_id": "participant-111",
    "content": "While I acknowledge the urgency of climate change, we must consider the economic impact of rapid transitions..."
})

print(f"Added message: {result['id']}")
```

## Notes

- Messages can only be added by the participant whose turn it is, unless the debate format allows free participation
- Maximum message length is 100,000 characters
- Messages are automatically assigned to the current round and turn
- Token count is automatically calculated and added to metadata
# MCP-Controller Service API Documentation (Continued)

This document continues the detailed documentation for the MCP-Controller service API endpoints.

---

## MCP Tool: Get Next Turn

# MCP Tool: get_next_turn

**Description**: Gets the next turn in a debate, optionally generating an AI response

**Service**: MCP-Controller

**Authentication Required**: Yes

## Parameters

```json
{
  "debate_id": "debate-123456",
  "generate_response": true,
  "context_strategy": "full_history",
  "max_tokens": 1000,
  "temperature": 0.7,
  "include_metadata": true
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `debate_id` | string | Yes | ID of the debate |
| `generate_response` | boolean | No | Whether to generate an AI response (default: false) |
| `context_strategy` | string | No | Strategy for context generation |
| `max_tokens` | integer | No | Maximum tokens for generated response |
| `temperature` | number | No | Temperature for response generation |
| `include_metadata` | boolean | No | Whether to include metadata in the response |

## Result

```json
{
  "debate_id": "debate-123456",
  "current_round": 2,
  "current_participant": {
    "id": "participant-222",
    "name": "Team Growth",
    "role": "opposition"
  },
  "previous_message": {
    "id": "message-789012",
    "participant_id": "participant-111",
    "content": "While I acknowledge the urgency of climate change, we must consider the economic impact of rapid transitions...",
    "round": 2,
    "turn_index": 0,
    "created_at": "2025-07-17T01:05:00Z"
  },
  "generated_response": {
    "content": "Thank you for acknowledging the urgency of climate change. I agree this is a critical issue, but I'd like to expand on the economic considerations...",
    "metadata": {
      "token_count": 487,
      "citations": [
        {"source": "World Economic Forum 2024 Report", "page": 23}
      ]
    }
  },
  "context": {
    "message_count": 5,
    "token_count": 1250,
    "strategy": "full_history"
  },
  "next_actions": [
    {
      "action": "add_message",
      "participant_id": "participant-222"
    },
    {
      "action": "skip_turn"
    }
  ]
}
```

## Example

### Python

```python
from mcp_client import MCPClient

client = MCPClient("http://localhost:5013")

result = await client.call_tool("get_next_turn", {
    "debate_id": "debate-123456",
    "generate_response": True,
    "context_strategy": "full_history",
    "max_tokens": 1000
})

print(f"Current participant: {result['current_participant']['name']}")
if "generated_response" in result:
    print(f"Generated response: {result['generated_response']['content'][:100]}...")
```

## Notes

- If `generate_response` is true, the service will generate an AI response using the current participant's LLM configuration
- The generated response is not automatically added to the debate; it must be explicitly added using the add message endpoint
- Context strategies determine how much of the debate history is included in the context for response generation
- If the debate has reached the maximum number of rounds, the next turn will indicate that the debate is complete

---

## MCP Tool: Summarize Debate

# MCP Tool: summarize_debate

**Description**: Generates a summary of the debate

**Service**: MCP-Controller

**Authentication Required**: Yes

## Parameters

```json
{
  "debate_id": "debate-123456",
  "strategy": "pro_con",
  "max_length": 500,
  "include_key_points": true,
  "focus_areas": ["economic impact", "climate science"],
  "temperature": 0.3
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `debate_id` | string | Yes | ID of the debate |
| `strategy` | string | No | Summary strategy |
| `max_length` | integer | No | Maximum length of the summary in tokens |
| `include_key_points` | boolean | No | Whether to include key points in the summary |
| `focus_areas` | array | No | Specific areas to focus on in the summary |
| `temperature` | number | No | Temperature for summary generation |

## Result

```json
{
  "id": "summary-345678",
  "debate_id": "debate-123456",
  "content": "This debate focused on whether countries should prioritize immediate emissions reductions over economic growth. Team Green argued that urgent climate action is necessary to prevent catastrophic outcomes, citing recent IPCC reports showing accelerating climate change impacts. Team Growth countered that economic stability is essential for sustainable climate action, arguing that rapid transitions could disproportionately harm developing economies...",
  "key_points": [
    {
      "side": "Team Green",
      "points": [
        "Climate tipping points require immediate action regardless of economic cost",
        "Technological solutions exist but need rapid deployment",
        "Economic costs of inaction exceed costs of immediate action"
      ]
    },
    {
      "side": "Team Growth",
      "points": [
        "Economic stability is prerequisite for sustainable climate action",
        "Developing nations need different transition timelines",
        "Market-based solutions can drive efficient emissions reductions"
      ]
    }
  ],
  "metadata": {
    "strategy": "pro_con",
    "token_count": 487,
    "messages_covered": 6,
    "rounds_covered": 3
  },
  "created_at": "2025-07-17T01:10:00Z"
}
```

## Example

### Python

```python
from mcp_client import MCPClient

client = MCPClient("http://localhost:5013")

result = await client.call_tool("summarize_debate", {
    "debate_id": "debate-123456",
    "strategy": "pro_con",
    "max_length": 500,
    "include_key_points": True
})

print(f"Summary: {result['content'][:100]}...")
if "key_points" in result:
    for side in result["key_points"]:
        print(f"\n{side['side']} key points:")
        for point in side["points"]:
            print(f"- {point}")
```

## Notes

- Summarization requires at least 4 messages in the debate
- Summary strategies determine the structure and focus of the generated summary
- Summaries are stored and can be retrieved later
- Multiple summaries can be generated for the same debate with different strategies

---

## MCP Tool: List Debates

# MCP Tool: list_debates

**Description**: Lists debates for the organization with optional filtering

**Service**: MCP-Controller

**Authentication Required**: Yes

## Parameters

```json
{
  "status": "IN_PROGRESS",
  "format": "oxford",
  "topic": "climate",
  "participant": "Team Green",
  "page": 0,
  "size": 10,
  "sort": "createdAt,desc"
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | string | No | Filter by status |
| `format` | string | No | Filter by format |
| `topic` | string | No | Filter by topic (partial match) |
| `participant` | string | No | Filter by participant name (partial match) |
| `page` | integer | No | Page number for pagination (default: 0) |
| `size` | integer | No | Page size for pagination (default: 20) |
| `sort` | string | No | Sort field and direction (default: "createdAt,desc") |

## Result

```json
{
  "content": [
    {
      "id": "debate-123456",
      "name": "Climate Policy Debate",
      "format": "oxford",
      "topic": "Should countries prioritize immediate emissions reductions over economic growth?",
      "status": "IN_PROGRESS",
      "participant_count": 2,
      "current_round": 2,
      "max_rounds": 6,
      "created_at": "2025-07-16T01:00:00Z",
      "updated_at": "2025-07-17T01:00:00Z"
    },
    {
      "id": "debate-123457",
      "name": "AI Ethics Debate",
      "format": "standard",
      "topic": "Should AI development be regulated?",
      "status": "COMPLETED",
      "participant_count": 2,
      "current_round": 5,
      "max_rounds": 5,
      "created_at": "2025-07-15T01:00:00Z",
      "updated_at": "2025-07-16T01:00:00Z"
    }
  ],
  "pageable": {
    "page": 0,
    "size": 10,
    "sort": "createdAt,desc"
  },
  "total_elements": 2,
  "total_pages": 1
}
```

## Example

### Python

```python
from mcp_client import MCPClient

client = MCPClient("http://localhost:5013")

result = await client.call_tool("list_debates", {
    "status": "IN_PROGRESS",
    "format": "oxford",
    "page": 0,
    "size": 10
})

print(f"Found {result['total_elements']} debates:")
for debate in result["content"]:
    print(f"- {debate['name']} ({debate['id']}): {debate['status']}, {debate['current_round']}/{debate['max_rounds']} rounds")
```

## Notes

- Results are paginated with a default page size of 20
- Maximum page size is 100
- List endpoint returns summarized debate information, not full details
- For full debate details, use the get_debate tool

---

## MCP Tool: Get Debate

# MCP Tool: get_debate

**Description**: Retrieves details of a specific debate

**Service**: MCP-Controller

**Authentication Required**: Yes

## Parameters

```json
{
  "debate_id": "debate-123456",
  "include_messages": false
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `debate_id` | string | Yes | ID of the debate |
| `include_messages` | boolean | No | Whether to include messages in the response (default: false) |

## Result

```json
{
  "id": "debate-123456",
  "organization_id": "org-789",
  "name": "Climate Policy Debate",
  "format": "oxford",
  "description": "Debate on international climate policy approaches",
  "participants": [
    {
      "id": "participant-111",
      "name": "Team Green",
      "role": "proposition",
      "llm_config": {
        "provider": "claude",
        "model": "claude-3-opus-20240229",
        "system_prompt": "You are an expert in environmental policy advocating for aggressive climate action."
      }
    },
    {
      "id": "participant-222",
      "name": "Team Growth",
      "role": "opposition",
      "llm_config": {
        "provider": "openai",
        "model": "gpt-4",
        "system_prompt": "You are an economist focused on balancing climate action with economic growth."
      }
    }
  ],
  "max_rounds": 6,
  "current_round": 2,
  "current_participant_index": 0,
  "topic": "Should countries prioritize immediate emissions reductions over economic growth?",
  "rules": ["no_repetition", "stay_on_topic"],
  "status": "IN_PROGRESS",
  "metadata": {
    "category": "climate",
    "difficulty": "advanced",
    "tags": ["climate", "economics", "policy"]
  },
  "message_count": 4,
  "created_at": "2025-07-16T01:00:00Z",
  "updated_at": "2025-07-17T01:00:00Z",
  "messages": []
}
```

## Example

### Python

```python
from mcp_client import MCPClient

client = MCPClient("http://localhost:5013")

result = await client.call_tool("get_debate", {
    "debate_id": "debate-123456"
})

print(f"Debate: {result['name']}")
print(f"Topic: {result['topic']}")
print(f"Status: {result['status']}")
print(f"Current round: {result['current_round']}/{result['max_rounds']}")
print(f"Participants:")
for participant in result["participants"]:
    print(f"- {participant['name']} ({participant['role']})")
```

## Notes

- If `include_messages` is true, the response will include the debate messages
- For debates with many messages, it's recommended to use the get_debate_messages tool instead
- System prompts may be redacted for shared debates

---

## MCP Tool: Get Debate Messages

# MCP Tool: get_debate_messages

**Description**: Retrieves messages from a debate

**Service**: MCP-Controller

**Authentication Required**: Yes

## Parameters

```json
{
  "debate_id": "debate-123456",
  "page": 0,
  "size": 20,
  "sort": "createdAt,asc",
  "participant_id": "participant-111",
  "round": 2
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `debate_id` | string | Yes | ID of the debate |
| `page` | integer | No | Page number for pagination (default: 0) |
| `size` | integer | No | Page size for pagination (default: 20) |
| `sort` | string | No | Sort field and direction (default: "createdAt,asc") |
| `participant_id` | string | No | Filter by participant ID |
| `round` | integer | No | Filter by round number |

## Result

```json
{
  "content": [
    {
      "id": "message-789010",
      "debate_id": "debate-123456",
      "participant_id": "participant-111",
      "content": "Climate change represents an existential threat that requires immediate action...",
      "round": 1,
      "turn_index": 0,
      "metadata": {
        "token_count": 42,
        "citations": [
          {"source": "IPCC Sixth Assessment Report", "page": 15}
        ]
      },
      "created_at": "2025-07-16T01:05:00Z"
    },
    {
      "id": "message-789011",
      "debate_id": "debate-123456",
      "participant_id": "participant-222",
      "content": "While climate change is certainly a serious concern, we must balance our approach...",
      "round": 1,
      "turn_index": 1,
      "metadata": {
        "token_count": 38,
        "citations": [
          {"source": "World Economic Forum 2024 Report", "page": 23}
        ]
      },
      "created_at": "2025-07-16T01:10:00Z"
    }
  ],
  "pageable": {
    "page": 0,
    "size": 20,
    "sort": "createdAt,asc"
  },
  "total_elements": 4,
  "total_pages": 1
}
```

## Example

### Python

```python
from mcp_client import MCPClient

client = MCPClient("http://localhost:5013")

result = await client.call_tool("get_debate_messages", {
    "debate_id": "debate-123456",
    "sort": "createdAt,asc"
})

print(f"Debate messages ({result['total_elements']}):")
for message in result["content"]:
    print(f"Round {message['round']}, {message['created_at']}")
    print(f"Participant: {message['participant_id']}")
    print(f"Message: {message['content'][:100]}...")
    print()
```

## Notes

- Results are paginated with a default page size of 20
- Maximum page size is 100
- Messages can be filtered by participant ID and round number
- Messages are typically sorted chronologically (createdAt,asc) by default
