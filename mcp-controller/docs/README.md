# MCP-Controller Service Documentation

The MCP-Controller service orchestrates debates and manages debate flow in the Zamaz Debate MCP system. It replaces the previous mcp-debate service with enhanced functionality and multi-tenant support.

## Overview

The MCP-Controller service manages the lifecycle of debates, including participant turns, rule enforcement, and debate progression. It integrates with the MCP-LLM service for AI responses and supports various debate formats and configurations.

## Features

- **Debate Orchestration**: Manage debate flow and participant turns
- **Multiple Debate Formats**: Support for various debate structures and rules
- **Multi-tenant Support**: Complete isolation between organizations
- **Participant Management**: Configure and manage debate participants
- **Rule Enforcement**: Enforce debate rules and constraints
- **Summarization**: Generate debate summaries and key points
- **Integration**: Seamless integration with LLM and RAG services

## Architecture

The Controller service follows a clean architecture pattern:

- **Controllers**: Handle HTTP requests and responses
- **Services**: Implement business logic for debate management
- **Repositories**: Manage data persistence
- **Models**: Define debate-related data structures
- **Integration**: Connect with other MCP services

## API Endpoints

### Debates

- `POST /api/v1/debates`: Create a new debate
- `GET /api/v1/debates`: List debates
- `GET /api/v1/debates/{id}`: Get debate details
- `PUT /api/v1/debates/{id}`: Update debate
- `DELETE /api/v1/debates/{id}`: Delete debate

### Messages

- `POST /api/v1/debates/{id}/messages`: Add message to debate
- `GET /api/v1/debates/{id}/messages`: Get debate messages
- `GET /api/v1/debates/{id}/messages/{messageId}`: Get specific message

### Turns

- `POST /api/v1/debates/{id}/next-turn`: Get next turn
- `PUT /api/v1/debates/{id}/turn`: Update current turn
- `GET /api/v1/debates/{id}/current-turn`: Get current turn details

### Participants

- `POST /api/v1/debates/{id}/participants`: Add participant
- `GET /api/v1/debates/{id}/participants`: List participants
- `PUT /api/v1/debates/{id}/participants/{participantId}`: Update participant
- `DELETE /api/v1/debates/{id}/participants/{participantId}`: Remove participant

### Summaries

- `POST /api/v1/debates/{id}/summarize`: Generate debate summary
- `GET /api/v1/debates/{id}/summaries`: List debate summaries
- `GET /api/v1/debates/{id}/summaries/{summaryId}`: Get specific summary

### MCP Tools

The service exposes the following MCP tools:

- `create_debate`: Initialize a new debate
- `add_message`: Add message to debate
- `get_next_turn`: Get next turn in debate
- `summarize_debate`: Generate debate summary
- `list_debates`: List debates for organization
- `get_debate`: Get debate details

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | postgres |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | PostgreSQL database name | debate_db |
| `DB_USER` | PostgreSQL username | postgres |
| `DB_PASSWORD` | PostgreSQL password | postgres |
| `REDIS_HOST` | Redis host | redis |
| `REDIS_PORT` | Redis port | 6379 |
| `ORGANIZATION_SERVICE_URL` | Organization service URL | http://mcp-organization:5005 |
| `LLM_SERVICE_URL` | LLM service URL | http://mcp-llm:5002 |
| `SERVER_PORT` | Server port | 5013 |
| `LOG_LEVEL` | Logging level | INFO |

### Debate Configuration

Debate-specific settings can be configured in `config/debate.yml`:

```yaml
debate:
  formats:
    standard:
      max_rounds: 10
      turn_timeout_seconds: 300
      rules:
        - "participants_alternate"
        - "no_consecutive_turns"
    oxford:
      max_rounds: 6
      turn_timeout_seconds: 600
      rules:
        - "proposition_first"
        - "opposition_second"
        - "alternating_rebuttals"
    freestyle:
      max_rounds: 20
      turn_timeout_seconds: 180
      rules:
        - "free_participation"
  
  default_format: "standard"
  
  summarization:
    default_strategy: "key_points"
    strategies:
      - "key_points"
      - "pro_con"
      - "chronological"
      - "thematic"
```

## Usage Examples

### Create a Debate

```bash
curl -X POST http://localhost:5013/api/v1/debates \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
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

### Add Message to Debate

```bash
curl -X POST http://localhost:5013/api/v1/debates/debate-123/messages \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "participantId": "participant-456",
    "content": "While I acknowledge the urgency of climate change, we must consider the economic impact of rapid transitions...",
    "metadata": {
      "citations": [
        {"source": "IMF Economic Outlook 2024", "page": 42}
      ]
    }
  }'
```

### Get Next Turn

```bash
curl -X POST http://localhost:5013/api/v1/debates/debate-123/next-turn \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "generateResponse": true,
    "contextStrategy": "full_history",
    "maxTokens": 1000
  }'
```

### Generate Debate Summary

```bash
curl -X POST http://localhost:5013/api/v1/debates/debate-123/summarize \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "strategy": "pro_con",
    "maxLength": 500,
    "includeKeyPoints": true
  }'
```

## Debate Models

### Debate

```json
{
  "id": "debate-123",
  "organizationId": "org-456",
  "name": "Climate Policy Debate",
  "description": "Debate on international climate policy approaches",
  "format": "oxford",
  "topic": "Should countries prioritize immediate emissions reductions over economic growth?",
  "status": "IN_PROGRESS",
  "participants": [
    {
      "id": "participant-789",
      "name": "Team Green",
      "role": "proposition",
      "llmConfig": {
        "provider": "claude",
        "model": "claude-3-opus-20240229",
        "systemPrompt": "You are an expert in environmental policy advocating for aggressive climate action."
      }
    },
    {
      "id": "participant-790",
      "name": "Team Growth",
      "role": "opposition",
      "llmConfig": {
        "provider": "openai",
        "model": "gpt-4",
        "systemPrompt": "You are an economist focused on balancing climate action with economic growth."
      }
    }
  ],
  "currentRound": 2,
  "maxRounds": 6,
  "currentParticipantIndex": 0,
  "createdAt": "2025-07-16T10:30:00Z",
  "updatedAt": "2025-07-16T11:45:00Z"
}
```

### Message

```json
{
  "id": "message-456",
  "debateId": "debate-123",
  "participantId": "participant-789",
  "content": "The scientific consensus is clear: we must reduce global emissions by 45% by 2030 to avoid catastrophic climate change...",
  "round": 1,
  "turnIndex": 0,
  "metadata": {
    "citations": [
      {"source": "IPCC Sixth Assessment Report", "page": 15}
    ],
    "tokenCount": 487
  },
  "createdAt": "2025-07-16T10:35:00Z"
}
```

## Debate Formats

The service supports multiple debate formats:

### Standard Format

- Participants take turns in sequence
- Equal time/tokens for each participant
- Flexible number of rounds

### Oxford Format

- Formal structure with proposition and opposition
- Opening statements, rebuttals, and closing arguments
- Strict turn order and timing

### Freestyle Format

- Dynamic turn taking
- Flexible participation
- Minimal constraints

## Integration with Other Services

### LLM Service Integration

The Controller service integrates with the MCP-LLM service to:
- Generate AI participant responses
- Estimate token usage
- Stream responses for real-time debates

### Organization Service Integration

The Controller service integrates with the MCP-Organization service to:
- Validate organization access
- Enforce organization-specific limits
- Track usage for billing

### RAG Service Integration (Optional)

The Controller service can integrate with the MCP-RAG service to:
- Enhance AI responses with relevant information
- Provide factual grounding for debate points
- Include citations from knowledge bases

## Monitoring and Metrics

The service exposes the following metrics:

- Debate count by organization
- Message count by debate
- Response generation time
- Token usage by debate
- Error rate by debate format

Access metrics at: `http://localhost:5013/actuator/metrics`

## Troubleshooting

### Common Issues

1. **Turn Management Issues**
   - Check debate status and current participant
   - Verify participant configuration
   - Review debate format rules

2. **Response Generation Failures**
   - Check LLM service connectivity
   - Verify API key validity
   - Check for token limit issues

3. **Database Issues**
   - Verify database connection
   - Check for schema migration issues
   - Monitor database performance

### Logs

Service logs can be accessed via:

```bash
docker-compose logs mcp-controller
```

## Development

### Building the Service

```bash
cd mcp-controller
mvn clean install
```

### Running Tests

```bash
cd mcp-controller
mvn test
```

### Local Development

```bash
cd mcp-controller
mvn spring-boot:run
```

## Advanced Features

### Custom Debate Formats

Create custom debate formats:

```bash
curl -X POST http://localhost:5013/api/v1/debate-formats \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "name": "panel_discussion",
    "description": "Multi-participant panel with moderator",
    "rules": [
      "moderator_first",
      "panel_response_order",
      "audience_questions"
    ],
    "maxRounds": 15,
    "turnTimeoutSeconds": 240
  }'
```

### Debate Templates

Use templates for common debate scenarios:

```bash
curl -X POST http://localhost:5013/api/v1/debates \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "templateId": "template-123",
    "name": "AI Ethics Debate - July 2025",
    "participants": [
      {"name": "Team Regulation", "role": "proposition"},
      {"name": "Team Innovation", "role": "opposition"}
    ]
  }'
```

### Real-time Debate Features

Enable real-time features for live debates:

```bash
curl -X POST http://localhost:5013/api/v1/debates/debate-123/settings \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "realTimeFeatures": {
      "enabled": true,
      "streamResponses": true,
      "audienceParticipation": true,
      "moderationEnabled": true
    }
  }'
```
