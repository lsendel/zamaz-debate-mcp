# MCP Debate Service - CLAUDE.md

This file provides guidance to Claude Code when working with the mcp-debate service.

## Service Overview

The `mcp-debate` service is the central orchestration engine for the zamaz-debate-mcp platform. It manages multi-participant debates, coordinates turn-based interactions, integrates with LLM providers for content generation, and provides real-time updates via WebSocket.

## Purpose

- **Debate Orchestration**: Manage complex multi-participant debate flows
- **Turn Management**: Coordinate turn-based interactions with rules enforcement
- **LLM Integration**: Generate participant responses using various LLM providers
- **Real-time Updates**: WebSocket support for live debate progress
- **Format Support**: Multiple debate formats with customizable rules

## Technology Stack

- **Language**: Python 3.11+
- **Framework**: MCP SDK + FastAPI for WebSocket API
- **Database**: SQLite with async SQLAlchemy
- **Real-time**: WebSocket with connection pooling
- **Concurrency**: asyncio with rate limiting and queuing
- **Monitoring**: Built-in performance tracking and alerts

## Debate Formats

### Supported Formats
1. **Round Robin**: Each participant speaks in turn
2. **Oxford Style**: Formal structured debate with propositions
3. **Panel Discussion**: Multiple experts discussing topics
4. **Town Hall**: Open forum with audience participation
5. **Socratic Method**: Question-driven dialogue
6. **Parliamentary**: Formal debate with strict rules

### Turn Types
- **Opening Statement**: Initial position presentation
- **Argument**: Main points and evidence
- **Rebuttal**: Counter-arguments
- **Question**: Inquiry to other participants
- **Answer**: Response to questions
- **Closing Statement**: Final summary

## Architecture Components

### 1. MCP Server (`mcp_server.py`)
Implements MCP protocol with:
- **Tools**: create_debate, update_debate, get_debate, list_debates, execute_turn, list_turns, get_summary, delete_debate
- **Resources**: debate formats, debate list, debate templates
- **Error Handling**: Structured error responses

### 2. Debate Orchestrator (`orchestrators/debate_orchestrator.py`)
Core orchestration logic:
```python
# Key responsibilities
- Validate turn order and rules
- Integrate with Context service for history
- Call LLM service for content generation
- Optional RAG augmentation
- Update debate state
- Broadcast events via WebSocket
```

### 3. Data Models (`models.py`)
```python
# Core entities
Debate:
  - participants: List[Participant]
  - rules: DebateRules
  - turns: List[Turn]
  - status: DebateStatus
  - context_id: Optional[str]

Participant:
  - role: ParticipantRole
  - provider: Optional[str]  # LLM provider
  - model: Optional[str]     # LLM model
  - is_human: bool

Turn:
  - participant_id: str
  - type: TurnType
  - content: str
  - metadata: Dict
```

### 4. Database Store (`db/debate_store.py`)
SQLite persistence with:
- Debates table (JSON serialization)
- Turns table (individual records)
- Summaries table (cached summaries)
- Async operations with proper indexing

### 5. WebSocket Manager (`websocket_manager.py`)
Real-time communication:
- Connection pooling by debate_id and org_id
- Event types: turn_started, turn_completed, debate_updated
- Automatic cleanup on disconnect

### 6. Concurrency Control (`concurrency.py`)
Resource management:
- Rate limiting: 100 requests/minute per organization
- Queue depth: 10 concurrent requests max
- Debate locking: Prevents concurrent modifications
- Connection pooling: Reused database connections

## MCP Tools

### 1. create_debate
```json
{
  "title": "AI Ethics Debate",
  "description": "Discussing AI safety",
  "format": "oxford",
  "participants": [...],
  "rules": {...},
  "organization_id": "org-123"
}
```

### 2. execute_turn
```json
{
  "debate_id": "debate-123",
  "participant_id": "participant-1",
  "type": "argument",
  "content": "Manual content",  // Optional
  "use_llm": true,
  "use_rag": true
}
```

### 3. get_summary
```json
{
  "debate_id": "debate-123",
  "include_analysis": true
}
```

## Configuration

### Environment Variables
```bash
# Service Configuration
MCP_HOST=0.0.0.0
MCP_PORT=5003
API_HOST=0.0.0.0
API_PORT=5013

# Database
DATABASE_PATH=./data/debates.db

# Service URLs
CONTEXT_SERVICE_URL=http://mcp-context:5001
LLM_SERVICE_URL=http://mcp-llm:5002
RAG_SERVICE_URL=http://mcp-rag:5004
ORGANIZATION_SERVICE_URL=http://mcp-organization:5005

# Performance
MAX_CONCURRENT_DEBATES=100
MAX_TURNS_PER_DEBATE=1000
RATE_LIMIT_PER_ORG=100
QUEUE_SIZE=10

# Monitoring
ENABLE_MONITORING=true
ALERT_THRESHOLD_MS=5000
```

### Running the Service
```bash
# MCP Server
python -m src.mcp_server

# API Server (for WebSocket)
python src/api_server.py

# Docker
docker build -t mcp-debate .
docker run -p 5003:5003 -p 5013:5013 mcp-debate

# Development (both servers)
make start-debate
```

## Debate Flow

### 1. Debate Creation
```python
1. Validate participants and rules
2. Create context in Context service
3. Initialize debate state
4. Store in database
5. Return debate ID
```

### 2. Turn Execution
```python
1. Validate turn order and participant
2. Get context window from Context service
3. If use_llm:
   - Prepare prompt with context
   - Call LLM service
   - Apply participant personality
4. If use_rag:
   - Query relevant knowledge
   - Augment prompt
5. Store turn in database
6. Update context
7. Broadcast via WebSocket
8. Check completion conditions
```

### 3. Summary Generation
```python
1. Retrieve all turns
2. Group by participant
3. Analyze key points
4. Generate structured summary
5. Cache for future requests
```

## Integration Patterns

### With Context Service
```python
# Create debate context
context = await context_client.create_context(
    organization_id=debate.organization_id,
    namespace="debates",
    metadata={"debate_id": debate.id}
)

# Get optimized window
window = await context_client.get_context_window(
    context_id=debate.context_id,
    strategy="sliding_window",
    max_tokens=4000
)
```

### With LLM Service
```python
# Generate turn content
response = await llm_client.complete(
    provider=participant.provider,
    model=participant.model,
    messages=build_prompt(context, turn_type),
    temperature=0.8,
    max_tokens=500
)
```

### With RAG Service
```python
# Augment with knowledge
knowledge = await rag_client.search(
    query=current_topic,
    organization_id=debate.organization_id,
    limit=5
)
```

## WebSocket Events

### Event Types
1. **turn_started**: Participant begins speaking
2. **turn_completed**: Turn content available
3. **debate_updated**: Status or metadata changed
4. **participant_joined**: New participant added
5. **debate_completed**: All turns finished

### Event Format
```json
{
  "event": "turn_completed",
  "debate_id": "debate-123",
  "data": {
    "turn_id": "turn-456",
    "participant_id": "participant-1",
    "content": "...",
    "timestamp": "2024-01-20T10:30:00Z"
  }
}
```

## Monitoring and Performance

### Metrics Tracked
- Debates per organization
- Average turn generation time
- LLM service latency
- Context retrieval time
- WebSocket connection count
- Error rates by type

### Performance Optimization
1. **Caching**: Summary caching, context windows
2. **Batching**: Group LLM requests when possible
3. **Async Operations**: Non-blocking database access
4. **Connection Pooling**: Reuse service connections
5. **Rate Limiting**: Prevent resource exhaustion

## Error Handling

### Common Errors
1. **Invalid Turn Order**: Participant speaking out of turn
2. **Context Overflow**: Debate exceeds token limits
3. **LLM Failure**: Provider unavailable or error
4. **Rate Limit**: Organization exceeds quota
5. **WebSocket Disconnect**: Client connection lost

### Error Recovery
- Automatic retry for transient failures
- Graceful degradation without optional services
- State consistency via database transactions
- Event replay for missed WebSocket messages

## Development Guidelines

### Adding New Debate Formats
1. Define format in `models.py`
2. Implement turn order logic
3. Add validation rules
4. Create format-specific prompts
5. Update documentation

### Testing Debates
```bash
# Unit tests
pytest tests/test_orchestrator.py

# Integration test
python tests/integration/test_full_debate.py

# Load test
locust -f tests/load/debate_load_test.py
```

### Debugging Tips
1. **Turn Generation**: Enable debug logging for prompts
2. **WebSocket Issues**: Check connection manager logs
3. **Performance**: Monitor metrics dashboard
4. **State Issues**: Verify database consistency
5. **Service Integration**: Test with mock services

## Security Considerations

- Organization-scoped access control
- Input validation for all debate parameters
- Sanitization of LLM-generated content
- Rate limiting per organization
- WebSocket authentication via tokens
- No direct database access from clients

## Known Limitations

1. **Scalability**: Single SQLite database
2. **Turn Editing**: No modification after creation
3. **Branching**: No support for debate forks
4. **Audio/Video**: Text-only debates
5. **Voting**: No built-in voting mechanism

## Future Enhancements

- PostgreSQL for better scalability
- Turn editing and branching support
- Audio transcription integration
- Real-time translation
- Audience participation features
- Advanced analytics and insights
- Multi-modal debates (text + voice)
- Blockchain-based verification