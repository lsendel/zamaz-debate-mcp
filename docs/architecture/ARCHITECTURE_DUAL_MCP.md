# Dual MCP Service Architecture

## Overview

The system consists of two separate MCP services:
1. **mcp-llm**: Handles all LLM provider interactions
2. **mcp-debate**: Manages debates, context, and orchestration

## Architecture Diagram

```
┌─────────────────┐         ┌──────────────────┐
│  Claude Code    │         │  Other Clients   │
└────────┬────────┘         └────────┬─────────┘
         │                           │
         │         MCP Protocol      │
         │                           │
    ┌────▼────────────────────┬──────▼────────┐
    │                         │                │
    │      mcp-debate         │    mcp-llm    │
    │                         │                │
    │  - Debate Management    │  - LLM Calls  │
    │  - Context Tracking     │  - Provider   │
    │  - History Storage      │    Mgmt       │
    │  - Orchestration        │  - Streaming  │
    │                         │                │
    └────────────┬────────────┴───────┬────────┘
                 │                    │
                 │   MCP Protocol     │
                 │   Communication    │
                 └────────────────────┘
```

## MCP-LLM Service

### Purpose
Provides a unified interface for interacting with multiple LLM providers.

### Resources
- `/providers` - List available LLM providers and models
- `/providers/{provider}/models` - Get models for specific provider
- `/conversations/{id}` - Access conversation history

### Tools
- `complete` - Generate completion from any LLM
  ```json
  {
    "provider": "claude|openai|gemini|llama",
    "model": "model-name",
    "messages": [...],
    "max_tokens": 1000,
    "temperature": 0.7,
    "stream": false
  }
  ```
- `stream_complete` - Stream completion responses
- `list_models` - Get available models per provider
- `estimate_tokens` - Calculate token usage

### Provider Implementation
```python
# mcp-llm/src/providers/base_provider.py
class LLMProvider(ABC):
    @abstractmethod
    async def complete(self, messages: List[Message], **kwargs) -> CompletionResponse:
        pass
    
    @abstractmethod
    async def stream_complete(self, messages: List[Message], **kwargs) -> AsyncIterator[str]:
        pass
    
    @abstractmethod
    def estimate_tokens(self, messages: List[Message]) -> int:
        pass
```

## MCP-Debate Service

### Purpose
Manages debate sessions, participants, context, and history.

### Resources
- `/debates` - List all debates
- `/debates/{id}` - Get debate details
- `/debates/{id}/messages` - Debate message history
- `/debates/{id}/participants` - Debate participants
- `/debates/{id}/context` - Current context state

### Tools
- `create_debate` - Initialize new debate
  ```json
  {
    "name": "Climate Change Debate",
    "participants": [
      {
        "name": "Pro Climate Action",
        "llm_config": {
          "provider": "claude",
          "model": "claude-3-opus",
          "system_prompt": "Argue for immediate climate action"
        }
      }
    ],
    "rules": {...},
    "max_rounds": 10
  }
  ```
- `add_message` - Add message to debate
- `get_next_turn` - Orchestrate next participant turn
- `summarize_debate` - Generate debate summary
- `export_debate` - Export in various formats

### Context Management
```python
# mcp-debate/src/managers/context_manager.py
class ContextManager:
    def __init__(self, max_tokens: int = 10000):
        self.max_tokens = max_tokens
    
    def add_message(self, debate_id: str, message: Message):
        # Add to context with windowing
        pass
    
    def get_context(self, debate_id: str) -> Context:
        # Return current context
        pass
    
    def compress_context(self, debate_id: str):
        # Summarize older messages
        pass
```

## Inter-MCP Communication

The debate service calls the LLM service using MCP client:

```python
# mcp-debate/src/llm_client.py
class LLMServiceClient:
    def __init__(self, llm_service_url: str):
        self.client = MCPClient(llm_service_url)
    
    async def get_response(self, participant: Participant, context: Context):
        response = await self.client.call_tool(
            "complete",
            provider=participant.llm_provider,
            model=participant.model,
            messages=context.messages,
            temperature=participant.temperature
        )
        return response
```

## Docker Compose Configuration

```yaml
version: '3.8'

services:
  mcp-llm:
    build: ./mcp-llm
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - GOOGLE_API_KEY=${GOOGLE_API_KEY}
      - LLAMA_ENDPOINT=${LLAMA_ENDPOINT}
    ports:
      - "5001:5001"
    volumes:
      - ./mcp-llm/config:/app/config
      - llm-cache:/app/cache
    networks:
      - mcp-network

  mcp-debate:
    build: ./mcp-debate
    environment:
      - LLM_SERVICE_URL=http://mcp-llm:5001
      - DATABASE_PATH=/app/data/debates.db
    ports:
      - "5002:5002"
    volumes:
      - ./mcp-debate/data:/app/data
      - ./mcp-debate/config:/app/config
    depends_on:
      - mcp-llm
    networks:
      - mcp-network

networks:
  mcp-network:
    driver: bridge

volumes:
  llm-cache:
```

## Error Handling Strategy

### MCP-LLM Errors
1. **Provider Errors**: API failures, rate limits
2. **Authentication Errors**: Invalid API keys
3. **Model Errors**: Unsupported models
4. **Token Limit Errors**: Exceeded context window

### MCP-Debate Errors
1. **Debate State Errors**: Invalid transitions
2. **Participant Errors**: Missing or invalid config
3. **Context Errors**: Token limit exceeded
4. **Storage Errors**: Database issues

### Error Propagation
- LLM errors are caught and wrapped with context
- Debate service implements retry logic
- Graceful fallbacks for non-critical errors
- Comprehensive error logging in both services

## Benefits of Dual Architecture

1. **Separation of Concerns**
   - LLM service focuses on provider integration
   - Debate service handles business logic

2. **Independent Scaling**
   - Scale LLM service based on API calls
   - Scale debate service based on active debates

3. **Reusability**
   - LLM service can be used by other projects
   - Clear API boundaries

4. **Maintainability**
   - Update LLM providers without touching debate logic
   - Add new debate features without LLM changes

5. **Testing**
   - Mock LLM service for debate testing
   - Test LLM providers independently