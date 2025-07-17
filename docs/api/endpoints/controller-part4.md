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
