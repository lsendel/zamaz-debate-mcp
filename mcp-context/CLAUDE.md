# MCP Context Service - CLAUDE.md

This file provides guidance to Claude Code when working with the mcp-context service.

## Service Overview

The `mcp-context` service is a sophisticated multi-tenant context management system for the zamaz-debate-mcp platform. It handles conversation histories, implements intelligent context windowing strategies, and provides token-optimized contexts for LLM consumption.

## Purpose

- **Context Management**: Store and retrieve conversation histories with multi-tenant isolation
- **Token Optimization**: Implement windowing strategies to fit contexts within LLM token limits
- **Context Sharing**: Enable cross-organization context sharing with permissions
- **Version Control**: Track context versions and support forking
- **Audit Compliance**: Maintain comprehensive audit logs for all operations

## Technology Stack

- **Language**: Python 3.11+
- **Framework**: MCP SDK (Model Context Protocol)
- **Database**: PostgreSQL with SQLAlchemy ORM (async)
- **Cache**: Redis for performance optimization (planned)
- **Authentication**: JWT-based with organization context
- **Token Counting**: tiktoken library (OpenAI tokenizer)
- **Async**: asyncio with asyncpg for database operations

## Key Features

### Context Management
- Create, update, and retrieve conversation contexts
- Append messages to existing contexts
- Fork contexts for branching conversations
- Support for multiple namespaces per organization
- Metadata storage for extensibility

### Window Strategies
Four intelligent strategies for context optimization:
1. **Full Strategy**: Returns complete context without modification
2. **Sliding Window**: Returns most recent N messages within token limit
3. **Sliding Window with Summary**: Summarizes older messages + recent messages
4. **Semantic Selection**: Selects semantically relevant messages (simplified implementation)

### Multi-Tenant Architecture
- Complete organization isolation
- Namespace support for logical grouping
- Cross-organization sharing with granular permissions
- Organization-scoped authentication and authorization

### Token Management
- Accurate token counting using tiktoken
- Per-model token limits and overhead calculation
- Fallback to word-based approximation
- Strategy-based optimization to fit within limits

## Database Models

### Core Entities
- **Organizations**: Tenant isolation
- **Namespaces**: Logical grouping within organizations
- **Contexts**: Conversation containers with metadata
- **Messages**: Individual conversation turns with roles
- **Context Windows**: Optimized context snapshots
- **Context Shares**: Cross-organization sharing records
- **Context Summaries**: Summarized context portions
- **Audit Logs**: Comprehensive operation tracking

### Schema Design
```python
# Key relationships
Organization -> Namespaces -> Contexts -> Messages
Context -> Context Windows (cached/optimized versions)
Context -> Context Shares (cross-org permissions)
Context -> Audit Logs (compliance tracking)
```

## MCP Tools Available

1. **create_context** - Create new conversation context
2. **get_context** - Retrieve context by ID
3. **update_context** - Update context metadata
4. **append_message** - Add message to context
5. **get_context_window** - Get optimized context for LLM
6. **fork_context** - Create a copy of existing context
7. **share_context** - Share context with another organization

## Configuration

### Environment Variables
```bash
# Server Configuration
MCP_HOST=0.0.0.0
MCP_PORT=5001

# Database
DATABASE_URL=postgresql+asyncpg://user:pass@localhost/context_db

# Redis Cache (when implemented)
REDIS_URL=redis://localhost:6379

# Authentication
JWT_SECRET=your-secret-key
JWT_ALGORITHM=HS256

# Token Limits
DEFAULT_MAX_TOKENS=8192
TOKEN_OVERHEAD=50
```

### Running the Service
```bash
# Development
python -m src.mcp_server

# Docker
docker build -t mcp-context .
docker run -p 5001:5001 mcp-context

# With environment
docker run -p 5001:5001 -e DATABASE_URL=... mcp-context
```

## Window Strategy Implementation

### Strategy Selection Logic
```python
# Based on context size and requirements
if total_tokens <= max_tokens:
    use FullStrategy
elif require_summary:
    use SlidingWindowWithSummaryStrategy
elif semantic_search:
    use SemanticSelectionStrategy
else:
    use SlidingWindowStrategy
```

### Token Calculation
- Uses cl100k_base encoding (GPT-3.5/4)
- Accounts for message structure overhead
- Includes role tokens and separators
- Fallback: 1 token ≈ 0.75 words

## Security Considerations

- JWT-based authentication with organization context
- All operations scoped to authenticated organization
- Cross-organization sharing requires explicit permissions
- Audit logging for compliance and security tracking
- No direct database access from external services

## Performance Optimizations

1. **Caching**: In-memory cache for frequently accessed contexts
2. **Async Operations**: All database operations are async
3. **Batch Operations**: Support for bulk message appending
4. **Lazy Loading**: Messages loaded only when needed
5. **Index Strategy**: Optimized indexes for common queries

## Integration with Other Services

### Dependencies
- **mcp-organization**: For organization validation
- **mcp-llm**: Consumes optimized contexts for generation

### API Patterns
```python
# Get optimized context for LLM
context_window = await context_client.get_context_window(
    context_id=context_id,
    strategy="sliding_window",
    max_tokens=4096
)

# Share context across organizations
share = await context_client.share_context(
    context_id=context_id,
    target_org_id=other_org_id,
    permissions=["read"]
)
```

## Development Guidelines

### Adding New Window Strategies
1. Inherit from `WindowStrategy` base class
2. Implement `apply()` method
3. Register in `window_strategies.py`
4. Add strategy-specific configuration

### Database Operations
- Always use async context managers
- Include organization_id in all queries
- Use transactions for multi-step operations
- Implement proper error handling and rollback

### Testing Strategies
- Unit tests for each window strategy
- Integration tests for MCP protocol
- Multi-tenant isolation tests
- Performance tests for large contexts
- Token counting accuracy tests

## Known Limitations & TODOs

1. **Database Implementation**: Currently using mock operations, needs real PostgreSQL integration
2. **LLM Summary Integration**: Summary strategy needs actual LLM integration
3. **Redis Cache**: Cache layer not yet implemented
4. **Semantic Search**: Simplified implementation, needs vector embeddings
5. **Batch Operations**: Bulk message operations need optimization

## Debugging Tips

1. **Token Count Issues**: Enable debug logging in TokenCounter
2. **Strategy Selection**: Log strategy choices and reasons
3. **Performance**: Monitor context size vs. retrieval time
4. **Multi-tenant Issues**: Verify organization_id in all operations
5. **Authentication**: Check JWT token validity and claims

## Future Enhancements

- Vector embeddings for semantic search
- Streaming context updates via WebSocket
- Context compression algorithms
- Multi-language support in token counting
- Advanced sharing permissions (time-based, usage-based)
- Context analytics and insights