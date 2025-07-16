# MCP Context Service

Multi-tenant context management service for the MCP debate system.

## Overview

The MCP Context Service provides comprehensive conversation context management with multi-tenant support. It stores conversation histories in PostgreSQL, uses Redis for caching active contexts, and provides intelligent context windowing for token management.

## Features

- **Multi-Tenant Context Management**: Complete isolation between organizations
- **Context Versioning**: Track and restore context history
- **Token Management**: Intelligent windowing based on token limits
- **Context Sharing**: Share contexts between organizations with permission control
- **High Performance**: Redis caching for active contexts
- **MCP Protocol**: Full MCP protocol support for tools and resources

## API Endpoints

### REST API

- `POST /api/contexts` - Create a new context
- `GET /api/contexts/{id}` - Get a context by ID
- `GET /api/contexts` - List contexts for an organization
- `GET /api/contexts/search` - Search contexts
- `POST /api/contexts/{id}/messages` - Append a message
- `GET /api/contexts/{id}/messages` - Get messages
- `POST /api/contexts/{id}/window` - Get context window with token management
- `DELETE /api/contexts/{id}` - Delete a context
- `POST /api/contexts/{id}/archive` - Archive a context

### MCP Endpoints

- `GET /mcp` - Server information
- `POST /mcp/list-tools` - List available tools
- `POST /mcp/call-tool` - Call a tool

## MCP Tools

- `create_context` - Create a new conversation context
- `append_message` - Append a message to a context
- `get_context_window` - Get a context window with token management
- `search_contexts` - Search contexts by name or description
- `share_context` - Share a context with another organization or user

## Configuration

```yaml
# Database
DB_HOST: localhost
DB_PORT: 5432
DB_NAME: context_db
DB_USER: postgres
DB_PASSWORD: postgres

# Redis
REDIS_HOST: localhost
REDIS_PORT: 6379

# Server
SERVER_PORT: 5007

# Context Settings
CONTEXT_CACHE_TTL: 3600  # 1 hour
CONTEXT_WINDOW_SIZE: 4096  # Default window size in tokens
CONTEXT_MAX_WINDOW_SIZE: 32768  # Maximum window size
CONTEXT_VERSION_RETENTION: 30  # Days to retain versions
TOKEN_COUNT_MODEL: gpt-4  # Model for token counting
```

## Running the Service

### With Docker Compose

```bash
docker-compose -f docker-compose.yml -f docker-compose-java.yml up mcp-context
```

### Standalone

```bash
cd mcp-context
mvn spring-boot:run
```

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Test MCP endpoints
curl http://localhost:5007/mcp

# Test health endpoint
curl http://localhost:5007/actuator/health
```

## Database Schema

The service uses the following main tables:

- `contexts` - Main context storage
- `messages` - Individual messages within contexts
- `context_versions` - Version history
- `shared_contexts` - Context sharing relationships

## Token Management

The service uses the tiktoken library for accurate token counting and supports:

- Automatic token counting for all messages
- Context windowing to fit within token limits
- Message truncation strategies
- Support for different LLM token models

## Security

- JWT authentication for all API endpoints
- Organization-based access control
- Context sharing with granular permissions
- API key validation for inter-service communication