# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a multi-service MCP (Model Context Protocol) system for managing debates with multi-tenant support. The system consists of five separate services:
1. **mcp-organization**: Organization management and multi-tenant setup
2. **mcp-context**: Multi-tenant context management service
3. **mcp-llm**: LLM provider gateway
4. **mcp-debate**: Debate orchestration service  
5. **mcp-rag**: Retrieval Augmented Generation service

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
├── mcp-organization/     # Organization management service
│   ├── src/
│   ├── Dockerfile
│   └── requirements.txt
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

## Makefile Best Practices and Learnings

### Key Improvements Made (2025-07-12)

1. **Port Conflict Handling**
   ```makefile
   # Check ports before starting services
   check-ports: ## Check if required ports are available
   	@for port in $(UI_PORT) $(DEBATE_API_PORT) $(LLM_API_PORT) 5432 6379 6333; do \
   		if lsof -Pi :$$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then \
   			echo "Port $$port is already in use"; \
   			exit 1; \
   		fi \
   	done
   ```

2. **Service Health Checks**
   ```makefile
   wait-for-services: ## Wait for all services to be healthy
   	@timeout 30 bash -c 'until docker-compose exec -T postgres pg_isready > /dev/null 2>&1; do sleep 1; done'
   	@timeout 30 bash -c 'until curl -s http://localhost:$(LLM_API_PORT)/health > /dev/null 2>&1; do sleep 1; done'
   ```

3. **Dynamic UI Port Selection**
   ```makefile
   ui: ## Start UI development server
   	@if lsof -Pi :$(UI_PORT) -sTCP:LISTEN -t >/dev/null 2>&1; then \
   		echo "Port $(UI_PORT) is in use. Trying port 3001..."; \
   		cd debate-ui && PORT=3001 npm run dev; \
   	else \
   		cd debate-ui && PORT=$(UI_PORT) npm run dev; \
   	fi
   ```

4. **Color-Coded Output**
   - Use ANSI escape codes for better visibility
   - Green (✅) for success, Red (❌) for errors, Yellow (⚠️) for warnings
   - Blue for informational messages

5. **Improved Error Handling**
   - Check command prerequisites before execution
   - Provide clear error messages with recovery suggestions
   - Add confirmation prompts for destructive operations

### Common Issues and Solutions

1. **UI Port Already in Use**
   - **Issue**: Port 3000 often occupied by other services
   - **Solution**: Automatic fallback to port 3001
   - **User Action**: Check `http://localhost:3001` if 3000 doesn't work

2. **Service Dependencies Not Ready**
   - **Issue**: UI starts before backend services are healthy
   - **Solution**: Added `wait-for-services` target
   - **Usage**: `make start` now waits for all services

3. **Test Runner Timeout**
   - **Issue**: Puppeteer/Playwright tests timeout in Docker
   - **Solution**: Use headless mode and proper wait strategies
   - **Alternative**: Run `make test-ui-only` for quick UI tests

### Makefile Command Reference

| Command | Purpose | When to Use |
|---------|---------|-------------|
| `make setup` | First-time setup | Once after cloning |
| `make start` | Start backend services | Daily development |
| `make ui` | Start UI dev server | In separate terminal |
| `make check-health` | Verify services | Troubleshooting |
| `make logs service=X` | View specific logs | Debug issues |
| `make fix-ui` | Fix common UI issues | UI problems |

### UI Testing Evidence

The UI has been thoroughly tested and is working correctly:

1. **APIs Functional**
   - Debate API: Returns 1 test debate
   - LLM API: Returns 4 providers
   - Health API: All services healthy

2. **UI Features Working**
   - Organization switcher displays correctly
   - Debates show without loading issues
   - Create Debate dialog functional
   - Tab navigation works

3. **Common UI Fixes Applied**
   - Default organization creation on first load
   - Proper state management in React hooks
   - Enhanced error logging for debugging

## IMPORTANT LEARNINGS (2025-07-13)

### UI Development Best Practices
1. **Always test UI changes with real user interactions** - Use Puppeteer or manual testing
2. **Import all required components** - Missing imports cause runtime errors (e.g., Sparkles icon)
3. **Add proper error handling and logging** - Use logger.info/error for debugging
4. **Test API integration end-to-end** - Verify the UI actually calls the backend APIs

### Improved Makefile Commands
- `make start-all` - Start all services including UI (replaces separate start + ui)
- `make stop-all` - Stop everything including UI processes
- `make start-ui` - Start only the UI development server
- `make quick-test` - Run quick UI tests without Docker
- `make full-test` - Run comprehensive E2E tests in Docker
- `make start-with-ollama` - Start everything including Ollama support

### Debate Creation Flow
1. UI calls `debateClient.callTool('create_debate', debate)`
2. This proxies to `/api/debate/tools/create_debate`
3. Which calls the debate service at `http://localhost:5013/tools/create_debate`
4. Response includes `{ debateId: string, status: string }`
5. UI reloads debates list and shows success message

### Quick Debate Feature
- Added "Quick Debate" button with one-click debate creation
- Pre-configured with sensible defaults
- Uses Claude vs OpenAI for interesting debates
- 3 rounds, 300 char limit for quick results

### Testing With Puppeteer
```javascript
// Key findings from Puppeteer testing:
// 1. WebSocket connections fail repeatedly but don't block functionality
// 2. API calls work correctly through the proxy
// 3. Import errors crash the whole page - always check imports
// 4. Use waitForTimeout instead of page.waitForTimeout in newer versions
```

### Common Pitfalls to Avoid
1. **Don't assume services are running** - Always check with `make status`
2. **Don't forget to import Lucide icons** - Each icon must be imported
3. **Don't skip E2E testing** - UI might look fine but not actually work
4. **Don't use generic names in Makefile** - Be specific (start-all vs start)