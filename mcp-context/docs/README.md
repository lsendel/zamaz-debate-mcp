# MCP-Context Service Documentation

The MCP-Context service manages conversation contexts with multi-tenant isolation in the Zamaz Debate MCP system. It provides efficient storage, retrieval, and manipulation of conversation contexts for LLM interactions.

## Overview

The MCP-Context service handles the creation, storage, and optimization of conversation contexts. It supports various context management strategies, including windowing, summarization, and compression, to optimize token usage while maintaining conversation coherence.

## Features

- **Context Management**: Create, update, and retrieve conversation contexts
- **Multi-tenant Isolation**: Complete isolation between organizations
- **Context Optimization**: Various strategies to optimize token usage
- **Context Sharing**: Controlled sharing between organizations
- **Context Versioning**: Track context history and changes
- **Context Summarization**: Generate summaries of conversation contexts
- **Token Management**: Track and optimize token usage

## Architecture

The Context service follows a clean architecture pattern:

- **Controllers**: Handle HTTP requests and responses
- **Services**: Implement business logic for context management
- **Repositories**: Manage data persistence
- **Models**: Define context-related data structures
- **Strategies**: Implement context optimization strategies

## API Endpoints

### Contexts

- `POST /api/v1/contexts`: Create context
- `GET /api/v1/contexts`: List contexts
- `GET /api/v1/contexts/{id}`: Get context details
- `DELETE /api/v1/contexts/{id}`: Delete context

### Messages

- `POST /api/v1/contexts/{id}/messages`: Add messages to context
- `GET /api/v1/contexts/{id}/messages`: Get context messages
- `DELETE /api/v1/contexts/{id}/messages/{messageId}`: Remove message from context

### Context Windows

- `GET /api/v1/contexts/{id}/window`: Get optimized context window
- `POST /api/v1/contexts/{id}/window`: Generate custom context window

### Context Summaries

- `POST /api/v1/contexts/{id}/summarize`: Generate context summary
- `GET /api/v1/contexts/{id}/summaries`: List context summaries

### Context Sharing

- `POST /api/v1/contexts/{id}/share`: Share context
- `GET /api/v1/contexts/shared`: List shared contexts
- `DELETE /api/v1/contexts/{id}/share/{organizationId}`: Revoke sharing

### MCP Tools

The service exposes the following MCP tools:

- `create_context`: Create new context
- `append_to_context`: Add messages to context
- `get_context_window`: Get optimized context window
- `summarize_context`: Generate context summary
- `share_context`: Share context with organization
- `compress_context`: Reduce token usage

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | postgres |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | PostgreSQL database name | context_db |
| `DB_USER` | PostgreSQL username | postgres |
| `DB_PASSWORD` | PostgreSQL password | postgres |
| `REDIS_HOST` | Redis host | redis |
| `REDIS_PORT` | Redis port | 6379 |
| `SERVER_PORT` | Server port | 5001 |
| `LOG_LEVEL` | Logging level | INFO |

### Context Configuration

Context-specific settings can be configured in `config/context.yml`:

```yaml
context:
  optimization:
    default_strategy: "sliding_window"
    strategies:
      sliding_window:
        default_window_size: 10
        include_system_messages: true
      summarization:
        summary_prompt: "Summarize the conversation so far:"
        max_summary_tokens: 500
      hybrid:
        summary_position: "prefix"
        max_summary_tokens: 300
        window_size: 5
      compression:
        compression_level: "medium"
        preserve_key_information: true
  
  token_limits:
    default_max_tokens: 8000
    claude_max_tokens: 100000
    gpt4_max_tokens: 8000
    gemini_max_tokens: 32000
    
  sharing:
    default_sharing_permission: "read"
    allowed_sharing_permissions:
      - "read"
      - "append"
      - "full"
```

## Usage Examples

### Create a Context

```bash
curl -X POST http://localhost:5001/api/v1/contexts \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "name": "Customer Support Conversation",
    "namespace": "support",
    "initialMessages": [
      {
        "role": "system",
        "content": "You are a helpful customer support agent for Acme Corporation."
      },
      {
        "role": "user",
        "content": "I need help with my recent order #12345."
      }
    ],
    "metadata": {
      "customer_id": "cust-789",
      "product_id": "prod-456"
    }
  }'
```

### Add Messages to Context

```bash
curl -X POST http://localhost:5001/api/v1/contexts/ctx-123/messages \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "messages": [
      {
        "role": "assistant",
        "content": "I'd be happy to help with your order #12345. What specific issue are you experiencing?"
      },
      {
        "role": "user",
        "content": "I ordered 3 items but only received 2."
      }
    ]
  }'
```

### Get Optimized Context Window

```bash
curl -X GET "http://localhost:5001/api/v1/contexts/ctx-123/window?max_tokens=4000&strategy=sliding_window" \
  -H "X-Organization-ID: org-123"
```

### Generate Context Summary

```bash
curl -X POST http://localhost:5001/api/v1/contexts/ctx-123/summarize \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "maxTokens": 300,
    "focusOn": ["order issues", "missing items"]
  }'
```

### Share Context

```bash
curl -X POST http://localhost:5001/api/v1/contexts/ctx-123/share \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "organizationId": "org-456",
    "permission": "read",
    "expiresAt": "2025-08-16T00:00:00Z"
  }'
```

## Data Models

### Context

```json
{
  "id": "ctx-123",
  "organizationId": "org-456",
  "name": "Customer Support Conversation",
  "namespace": "support",
  "messageCount": 5,
  "tokenCount": 1250,
  "metadata": {
    "customer_id": "cust-789",
    "product_id": "prod-456"
  },
  "createdAt": "2025-07-15T10:30:00Z",
  "updatedAt": "2025-07-16T14:22:15Z",
  "lastMessageAt": "2025-07-16T14:22:15Z"
}
```

### Message

```json
{
  "id": "msg-456",
  "contextId": "ctx-123",
  "role": "user",
  "content": "I ordered 3 items but only received 2.",
  "tokenCount": 12,
  "metadata": {
    "sentiment": "neutral",
    "entities": ["order", "items"]
  },
  "createdAt": "2025-07-16T14:22:15Z",
  "sequence": 3
}
```

### Context Window

```json
{
  "contextId": "ctx-123",
  "messages": [
    {
      "role": "system",
      "content": "You are a helpful customer support agent for Acme Corporation."
    },
    {
      "role": "user",
      "content": "I need help with my recent order #12345."
    },
    {
      "role": "assistant",
      "content": "I'd be happy to help with your order #12345. What specific issue are you experiencing?"
    },
    {
      "role": "user",
      "content": "I ordered 3 items but only received 2."
    }
  ],
  "tokenCount": 62,
  "strategy": "sliding_window",
  "truncated": false
}
```

### Context Summary

```json
{
  "id": "sum-789",
  "contextId": "ctx-123",
  "content": "Customer is reporting a missing item from order #12345. They ordered 3 items but only received 2.",
  "tokenCount": 20,
  "createdAt": "2025-07-16T14:30:00Z",
  "strategy": "focused",
  "focusAreas": ["order issues", "missing items"]
}
```

### Context Sharing

```json
{
  "id": "share-789",
  "contextId": "ctx-123",
  "ownerOrganizationId": "org-123",
  "sharedWithOrganizationId": "org-456",
  "permission": "read",
  "createdAt": "2025-07-15T09:45:00Z",
  "expiresAt": "2025-08-16T00:00:00Z",
  "createdBy": "user-789"
}
```

## Context Optimization Strategies

The service supports multiple context optimization strategies:

### Sliding Window

Keeps the most recent N messages in the context window:

```json
{
  "strategy": "sliding_window",
  "windowSize": 10,
  "includeSystemMessages": true
}
```

### Summarization

Generates a summary of older messages and includes recent messages:

```json
{
  "strategy": "summarization",
  "summaryPrompt": "Summarize the conversation so far:",
  "maxSummaryTokens": 500,
  "recentMessageCount": 5
}
```

### Hybrid

Combines summarization with sliding window:

```json
{
  "strategy": "hybrid",
  "summaryPosition": "prefix",
  "maxSummaryTokens": 300,
  "windowSize": 5
}
```

### Compression

Compresses messages to reduce token usage:

```json
{
  "strategy": "compression",
  "compressionLevel": "medium",
  "preserveKeyInformation": true
}
```

### Semantic Chunking

Groups messages by semantic similarity:

```json
{
  "strategy": "semantic_chunking",
  "chunkCount": 3,
  "includeAllSystemMessages": true
}
```

## Token Management

The service tracks token usage for different LLM providers:

- **Token Counting**: Accurate token counting for different models
- **Token Optimization**: Strategies to reduce token usage
- **Token Limits**: Enforce token limits based on model
- **Token Usage Tracking**: Track token usage by organization

## Monitoring and Metrics

The service exposes the following metrics:

- Context count by organization
- Message count by context
- Token usage by context
- Context window generation time
- Summarization performance

Access metrics at: `http://localhost:5001/actuator/metrics`

## Troubleshooting

### Common Issues

1. **Token Limit Exceeded**
   - Use a different optimization strategy
   - Increase max tokens if model supports it
   - Consider context summarization or compression

2. **Context Performance Issues**
   - Check database performance
   - Monitor context size
   - Consider archiving old contexts

3. **Summarization Quality Issues**
   - Adjust summary prompt
   - Increase max summary tokens
   - Try different summarization strategies

### Logs

Service logs can be accessed via:

```bash
docker-compose logs mcp-context
```

## Development

### Building the Service

```bash
cd mcp-context
mvn clean install
```

### Running Tests

```bash
cd mcp-context
mvn test
```

### Local Development

```bash
cd mcp-context
mvn spring-boot:run
```

## Advanced Features

### Context Namespaces

Organize contexts by namespace:

```bash
curl -X POST http://localhost:5001/api/v1/contexts \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "name": "Product Development Discussion",
    "namespace": "product-dev",
    "initialMessages": [
      {
        "role": "system",
        "content": "This is a discussion about product roadmap for Q3 2025."
      }
    ]
  }'
```

### Context Archiving

Archive old contexts to save storage:

```bash
curl -X POST http://localhost:5001/api/v1/contexts/ctx-123/archive \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "compressionLevel": "high",
    "retainMetadata": true
  }'
```

### Context Merging

Merge multiple contexts:

```bash
curl -X POST http://localhost:5001/api/v1/contexts/merge \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "contextIds": ["ctx-123", "ctx-456"],
    "name": "Combined Support Conversation",
    "strategy": "chronological",
    "deduplicateMessages": true
  }'
```

### Context Analytics

Get analytics for contexts:

```bash
curl -X GET http://localhost:5001/api/v1/contexts/ctx-123/analytics \
  -H "X-Organization-ID: org-123" \
  -d '{
    "includeMessageDistribution": true,
    "includeTokenUsage": true,
    "includeTopics": true
  }'
```
