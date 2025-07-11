# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a multi-service MCP (Model Context Protocol) system for managing debates with multi-tenant support. The system consists of four separate services:
1. **mcp-context**: Multi-tenant context management service
2. **mcp-llm**: LLM provider gateway
3. **mcp-debate**: Debate orchestration service  
4. **mcp-rag**: Retrieval Augmented Generation service

## Technology Stack

- **Language**: Python 3.11+
- **MCP Framework**: Python MCP SDK
- **Database**: PostgreSQL (context), SQLite (debate), Qdrant/Pinecone (RAG)
- **Cache**: Redis
- **Container**: Docker & Docker Compose
- **Async**: asyncio with aiohttp

## Project Structure

```
zamaz-debate-mcp/
├── mcp-context/          # Context management service
│   ├── src/
│   ├── Dockerfile
│   └── requirements.txt
├── mcp-llm/              # LLM gateway service
│   ├── src/
│   │   └── providers/    # Claude, OpenAI, Gemini, Llama
│   ├── Dockerfile
│   └── requirements.txt
├── mcp-debate/           # Debate orchestration
│   ├── src/
│   ├── Dockerfile
│   └── requirements.txt
├── mcp-rag/              # RAG service
│   ├── src/
│   ├── Dockerfile
│   └── requirements.txt
├── docker-compose.yml    # Multi-service orchestration
└── docs/                 # Architecture documentation
```

## Development Commands

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f [service-name]

# Run individual service
cd mcp-llm && python -m src.mcp_server

# Run tests
pytest tests/

# Format code
black src/
ruff check src/
```

## Key Architecture Decisions

1. **Multi-Tenant by Design**: Context service supports multiple organizations with complete isolation
2. **Stateless LLM Service**: Receives full context with each request
3. **Service Communication**: Services communicate via MCP protocol
4. **Context Strategy**: Debate service manages context optimization before sending to LLM service
5. **Security**: API key authentication, JWT for inter-service communication

## MCP Implementation Guidelines

When implementing MCP handlers:
- All resources should include organization scoping
- Tools should validate permissions before operations
- Use structured logging with context (org_id, user_id, request_id)
- Implement proper error handling with specific error codes
- Include rate limiting metadata in responses

## Context Management Best Practices

- Always include org_id in context operations
- Implement context windowing to respect token limits
- Use Redis for active context caching
- Store full history in PostgreSQL with versioning
- Support context sharing with explicit permissions

## Error Handling

Standard error response format:
```python
ErrorResponse(
    error="Descriptive error message",
    error_type="InvalidRequest|AuthError|RateLimit|ProviderError",
    details={"field": "value"},
    request_id="uuid"
)
```

## Testing Strategy

- Unit tests for each service component
- Integration tests for MCP protocol compliance
- End-to-end tests for complete debate flows
- Load tests for multi-tenant scenarios