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
