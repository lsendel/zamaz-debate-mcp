# MCP Context Service - Claude Development Guide

## Quick Reference
- **Port**: 5001
- **Database**: PostgreSQL (`context_db`)
- **Primary Purpose**: Manage conversation contexts and message history
- **Dependencies**: PostgreSQL, Redis (for caching)

## Key Files to Check
```
mcp-context/
├── src/
│   ├── mcp_server.py                    # MCP interface - START HERE
│   ├── models.py                        # Context data models
│   ├── db/
│   │   └── connection.py                # Database setup
│   └── managers/
│       ├── context_manager.py           # Core context logic
│       ├── window_strategies.py         # Context windowing
│       ├── token_counter.py             # Token management
│       └── auth_manager.py              # Authorization
```

## Current Implementation Status
✅ **Implemented**:
- Context CRUD operations
- Message management
- Token counting
- Context windowing strategies
- Context search
- Metadata handling
- Basic auth framework

❌ **Not Implemented**:
- Redis caching layer
- Context compression
- Cross-context linking
- Context templates
- Real-time synchronization
- Context analytics

## Context Window Strategies

### 1. Fixed Window
```python
class FixedWindowStrategy:
    # Keep last N messages
    window_size = 10
    # Simple and predictable
```

### 2. Token Window
```python
class TokenWindowStrategy:
    # Keep messages within token limit
    max_tokens = 4000
    # Optimal for LLM constraints
```

### 3. Time Window
```python
class TimeWindowStrategy:
    # Keep messages from last N minutes
    time_window_minutes = 30
    # Good for real-time conversations
```

### 4. Adaptive Window
```python
class AdaptiveWindowStrategy:
    # Dynamically adjust based on:
    # - Message importance
    # - Token usage
    # - Time decay
```

## Common Development Tasks

### 1. Creating a Context
```python
# Full context creation:
{
    "name": "create_context",
    "arguments": {
        "name": "Debate-123",
        "description": "AI Ethics debate context",
        "metadata": {
            "debate_id": "123",
            "organization_id": "org-456",
            "participants": ["AI-1", "AI-2"]
        },
        "window_strategy": "token",
        "window_config": {
            "max_tokens": 4000
        }
    }
}
```

### 2. Adding Messages
```python
# Message structure:
{
    "name": "add_message",
    "arguments": {
        "context_id": "ctx-123",
        "message": {
            "role": "assistant",
            "content": "I believe that...",
            "metadata": {
                "participant_id": "AI-1",
                "turn_number": 5,
                "timestamp": "2024-01-01T00:00:00Z"
            }
        }
    }
}
```

### 3. Retrieving Context
```python
# Get with windowing applied:
{
    "name": "get_context",
    "arguments": {
        "context_id": "ctx-123",
        "include_metadata": true,
        "apply_window": true
    }
}
# Returns messages within window limits
```

## Token Management

### Token Counting
```python
# Accurate counting per model:
def count_tokens(text: str, model: str = "gpt-4"):
    if model.startswith("gpt"):
        return count_openai_tokens(text, model)
    elif model.startswith("claude"):
        return count_anthropic_tokens(text)
    else:
        return estimate_tokens(text)  # Fallback
```

### Token Optimization
```python
# Strategies to reduce tokens:
1. Message summarization
2. Remove redundant content
3. Compress system messages
4. Use references instead of repetition
```

## Database Schema Insights

### Context Table
```python
# Key fields:
- id: UUID
- name: User-friendly name
- metadata: JSONB for flexibility
- window_strategy: Enum
- window_config: JSONB
- created_at, updated_at
- organization_id (future)
```

### Messages Table
```python
# Key fields:
- id: UUID
- context_id: Foreign key
- role: system/user/assistant
- content: Text
- metadata: JSONB
- token_count: Cached count
- created_at
```

## Search Functionality

### Context Search
```python
# Search contexts by:
- Name (fuzzy matching)
- Metadata fields
- Date range
- Message content (if enabled)

# Example:
{
    "name": "search_contexts",
    "arguments": {
        "query": "ethics",
        "metadata_filters": {
            "debate_id": {"$exists": true}
        },
        "limit": 10
    }
}
```

## Integration Patterns

### With Debate Service
```python
# Debate creates context:
context = await context_service.create_context({
    "name": f"Debate: {debate.topic}",
    "metadata": {
        "debate_id": debate.id,
        "participants": [p.name for p in debate.participants]
    }
})

# Add turns as messages:
await context_service.add_message({
    "context_id": context.id,
    "message": {
        "role": "assistant",
        "content": turn.content,
        "metadata": {"turn_id": turn.id}
    }
})
```

### With LLM Service
```python
# Get context for LLM:
context = await context_service.get_context(
    context_id=debate_context_id,
    apply_window=True
)

# Send to LLM:
llm_response = await llm_service.generate({
    "messages": context.messages,
    "model": "gpt-4"
})
```

## Performance Considerations

### Current Bottlenecks
1. **No caching**: Every request hits PostgreSQL
2. **Token counting**: Can be expensive for large contexts
3. **Window calculation**: Done on every retrieval

### Optimization Opportunities
```python
# Redis caching (not implemented):
@cache(ttl=300)
async def get_context(context_id: str):
    # Cache full contexts
    # Invalidate on message add

# Batch operations:
async def add_messages_batch(messages: List[Message]):
    # Single transaction
    # Bulk token counting
```

## Testing Scenarios

### Load Testing
```python
# Test concurrent message additions:
async def stress_test():
    tasks = []
    for i in range(100):
        task = add_message(context_id, f"Message {i}")
        tasks.append(task)
    await asyncio.gather(*tasks)
```

### Window Strategy Testing
```python
# Verify window limits:
1. Add messages exceeding limit
2. Retrieve context
3. Verify correct messages returned
4. Check token count accuracy
```

## Common Issues & Solutions

### Issue: "Context not found"
```python
# Check: Context ID format (UUID)
# Check: Context not deleted
# Solution: Implement soft deletes
```

### Issue: "Token limit exceeded"
```python
# Window strategy should prevent this
# Check: Token counting accuracy
# Solution: More aggressive windowing
```

### Issue: "Slow context retrieval"
```python
# Large contexts are slow
# Check: Number of messages
# Solution: Implement pagination
# Solution: Add Redis cache
```

## Security Considerations

### Authorization (Partial)
```python
# Current: Basic auth manager exists
# TODO: Implement full auth flow
# TODO: Context-level permissions
# TODO: Organization-based access
```

### Data Privacy
```python
# Considerations:
- Encrypt sensitive messages
- Audit log access
- Data retention policies
- GDPR compliance
```

## Environment Variables
```bash
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=context_db
POSTGRES_USER=context_user
POSTGRES_PASSWORD=context_pass
REDIS_URL=redis://redis:6379/0
MCP_PORT=5001
LOG_LEVEL=INFO
```

## Quick Commands
```bash
# Health check
curl http://localhost:5001/health

# List all contexts
echo '{"method": "resources/list"}' | nc localhost 5001

# Get specific context
curl http://localhost:5001/resources/context://contexts/ctx-123
```

## Advanced Features (Future)

### Context Templates
```python
# Predefined context structures:
templates = {
    "debate": {
        "window_strategy": "token",
        "window_config": {"max_tokens": 4000},
        "metadata": {"type": "debate"}
    },
    "interview": {
        "window_strategy": "time",
        "window_config": {"minutes": 60}
    }
}
```

### Context Linking
```python
# Reference other contexts:
{
    "parent_context_id": "ctx-parent",
    "linked_contexts": ["ctx-related-1", "ctx-related-2"],
    "inherit_messages": false
}
```

## Next Development Priorities
1. Implement Redis caching layer
2. Add context compression
3. Create context templates
4. Implement full authorization
5. Add real-time sync via WebSocket
6. Create context analytics
7. Add message search
8. Implement context versioning