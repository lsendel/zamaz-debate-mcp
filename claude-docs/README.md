# Claude Development Documentation

This directory contains comprehensive documentation specifically designed for Claude (or other AI assistants) to effectively understand and work with the AI Debate System codebase.

## Directory Structure

```
claude-docs/
├── mcp-services/          # Detailed guides for each MCP service
│   ├── README.md         # Service overview and common patterns
│   ├── mcp-context.md    # Context management service
│   ├── mcp-llm.md        # LLM provider service
│   ├── mcp-debate.md     # Debate orchestration service
│   ├── mcp-rag.md        # RAG/document service
│   ├── mcp-organization.md # Organization service
│   └── mcp-template.md   # Template service
└── README.md             # This file
```

## Quick Navigation

### By Task

#### 🔧 "I need to fix a bug in..."
- **UI/Frontend**: Start with `debate-ui/` and check component files
- **Service Logic**: Go to `claude-docs/mcp-services/<service-name>.md`
- **API Routes**: Check `debate-ui/src/app/api/`
- **Database Issues**: See service doc's "Database Schema" section

#### 🚀 "I need to add a feature for..."
- **New UI Component**: Check existing patterns in `debate-ui/src/components/`
- **New Service Endpoint**: See "Common Development Tasks" in service docs
- **Service Integration**: Check "Integration Points" sections
- **Database Changes**: Review schema in service's `db/connection.py`

#### 🔍 "I need to understand how..."
- **Services Communicate**: See `MCP_ORGANIZATION_INTEGRATION.md`
- **Authentication Works**: Check `mcp-organization.md` (future)
- **Debates Flow**: Read `mcp-debate.md` "Debate Flow" section
- **Templates Render**: See `mcp-template.md` "Jinja2 Examples"

### By Service

| Service | Purpose | Key Features | UI Integration |
|---------|---------|--------------|----------------|
| **Context** | Message history | Windowing, token management | ❌ Not integrated |
| **LLM** | AI model access | Multi-provider, streaming | ❌ Not integrated |
| **Debate** | Orchestration | Turn management, rules | ✅ Fully integrated |
| **RAG** | Document search | Embeddings, semantic search | ❌ Not integrated |
| **Organization** | Multi-tenancy | Users, permissions, quotas | ❌ Not integrated |
| **Template** | Jinja2 templates | Debate prompts, rendering | ❌ Not integrated |

## Development Workflow

### 1. Before Starting
```bash
# Check service status
docker-compose ps

# View logs if issues
docker logs <service-name> -f

# Verify UI is running
cd debate-ui && npm run dev
```

### 2. Making Changes
1. Read relevant service documentation
2. Check "Current Implementation Status"
3. Follow patterns in "Common Development Tasks"
4. Test using "Quick Commands" provided

### 3. Testing
```bash
# Run E2E tests
cd e2e-tests && npm test

# Test specific service
curl http://localhost:<port>/health

# Test via UI
# Open http://localhost:3000
```

## Key Architectural Decisions

### Why MCP (Model Context Protocol)?
- Standardized service communication
- Language-agnostic services
- Clear separation of concerns
- Easy to extend and maintain

### Why Microservices?
- Independent scaling
- Technology flexibility
- Fault isolation
- Parallel development

### Why These Specific Services?
- **Context**: Centralized conversation management
- **LLM**: Unified interface for multiple AI providers
- **Debate**: Core business logic isolation
- **RAG**: Specialized document handling
- **Organization**: Multi-tenancy from the start
- **Template**: Flexible prompt management

## Common Gotchas

### 1. Service Discovery
- Services use hardcoded URLs (environment variables)
- No service mesh or discovery (yet)
- Health checks are your friend

### 2. Database Connections
- Each service has its own database/schema
- PostgreSQL for persistent data
- Redis for caching
- Qdrant for vectors

### 3. Async Everything
- All Python services use asyncio
- Database operations are async
- Don't block the event loop

### 4. Error Handling
- Services should fail gracefully
- Always return proper MCP errors
- Log errors with context

## Integration Priorities

Based on current UI limitations:

### High Priority
1. **Organization Service**: Enable proper multi-tenancy
2. **Template Service**: Dynamic debate templates
3. **Context Service**: Persistent conversation history

### Medium Priority
4. **LLM Service**: Direct model management
5. **RAG Service**: Evidence-based debates

## Getting Started with a Task

1. **Identify the service(s) involved**
   - Use the quick navigation above
   - Check service dependencies

2. **Read the service documentation**
   - Understand current state
   - Check integration points
   - Review common issues

3. **Follow established patterns**
   - Use existing code as reference
   - Maintain consistency
   - Document changes

4. **Test thoroughly**
   - Unit tests for logic
   - Integration tests for services
   - E2E tests for user flows

## Questions to Ask

Before implementing:
- Which service owns this functionality?
- Does this require organization context?
- What are the performance implications?
- How does this affect other services?
- Is there an existing pattern to follow?

## Remember

- These docs are living documents - update them!
- When in doubt, check existing implementations
- Services should be loosely coupled
- The UI is the integration point
- Organization context is king (future)