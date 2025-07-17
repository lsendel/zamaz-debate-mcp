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
