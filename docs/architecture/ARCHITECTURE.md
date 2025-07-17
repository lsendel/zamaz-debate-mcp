# Debate MCP Service Architecture

## Overview

The Debate MCP Service is a Model Context Protocol server that enables structured debates with multiple LLM providers. It manages debate sessions, maintains context across conversations, and provides a unified interface for interacting with various LLMs.

## Core Components

### 1. MCP Server (mcp_server.py)
- Implements MCP protocol using Python SDK
- Exposes resources and tools for debate management
- Handles client requests and responses
- Manages session lifecycle

### 2. Debate Manager (debate_manager.py)
- Creates and manages debate sessions
- Tracks debate history and metadata
- Handles debate persistence using SQLite
- Manages participant turns and rules

### 3. LLM Integration Layer

#### Abstract Interface (llm_interface.py)
- Defines common interface for all LLM providers
- Handles authentication and configuration
- Manages rate limiting and retries

#### Provider Implementations
- **Claude Provider** (providers/claude_provider.py): Anthropic API
- **OpenAI Provider** (providers/openai_provider.py): GPT models
- **Gemini Provider** (providers/gemini_provider.py): Google AI
- **Llama Provider** (providers/llama_provider.py): Local or API-based

### 4. Context Management (context_manager.py)
- Maintains conversation context across LLM calls
- Implements context windowing for token limits
- Provides context summarization capabilities
- Tracks token usage per provider

### 5. Data Models (models.py)
- **Debate**: ID, name, participants, created_at, status
- **Message**: ID, debate_id, role, content, llm_provider, model, timestamp
- **Context**: debate_id, messages, summary, token_count
- **Participant**: ID, name, llm_provider, model, system_prompt

## MCP Resources

### /debates
- List all debates with metadata
- Filterable by status, date, participant

### /debates/{debate_id}
- Get specific debate details
- Includes participants, status, metadata

### /debates/{debate_id}/messages
- Retrieve debate message history
- Supports pagination and filtering

### /debates/{debate_id}/context
- Get current context state
- Includes token usage and summary

## MCP Tools

### create_debate
- Initialize new debate session
- Parameters: name, participants, rules, context_window

### send_message
- Send message in active debate
- Parameters: debate_id, content, llm_provider, model

### list_debates
- Retrieve all debates with filtering
- Parameters: status, limit, offset

### get_debate_context
- Get current context for debate
- Parameters: debate_id, include_summary

### switch_participant
- Change active participant in debate
- Parameters: debate_id, participant_id

### summarize_debate
- Generate debate summary
- Parameters: debate_id, summary_type

## Data Flow

1. **Client Request** → MCP Server
2. **MCP Server** → Debate Manager (for debate operations)
3. **Debate Manager** → Context Manager (for context updates)
4. **Context Manager** → LLM Provider (for AI responses)
5. **LLM Provider** → External API (OpenAI, Anthropic, etc.)
6. **Response** → Context Manager → Debate Manager → MCP Server → Client

## Error Handling Strategy

### Error Types
1. **Configuration Errors**: Missing API keys, invalid settings
2. **LLM Provider Errors**: API failures, rate limits, timeouts
3. **Context Errors**: Token limit exceeded, invalid context
4. **Debate Errors**: Invalid debate ID, state conflicts
5. **MCP Protocol Errors**: Invalid requests, serialization issues

### Error Responses
- Structured error messages with codes
- Fallback mechanisms for LLM failures
- Graceful degradation for non-critical errors
- Comprehensive logging for debugging

## Security Considerations

1. **API Key Management**: Environment variables, never in code
2. **Input Validation**: Sanitize all user inputs
3. **Rate Limiting**: Per-provider and global limits
4. **Access Control**: Debate-level permissions
5. **Data Privacy**: Secure storage of debate content

## Scalability Design

1. **Stateless MCP Server**: Can run multiple instances
2. **Database Connection Pooling**: Efficient SQLite usage
3. **Async Operations**: Non-blocking LLM calls
4. **Caching Layer**: Redis for frequent queries
5. **Context Compression**: Efficient storage of long debates

## Docker Architecture

```
zamaz-debate-mcp/
├── Dockerfile
├── docker-compose.yml
├── src/
│   ├── mcp_server.py
│   ├── debate_manager.py
│   ├── context_manager.py
│   ├── models.py
│   ├── llm_interface.py
│   └── providers/
├── data/              # Persistent volume
│   └── debates.db
├── logs/              # Log files
└── config/            # Configuration files
```

## Configuration Management

### Environment Variables
- `OPENAI_API_KEY`: OpenAI API access
- `ANTHROPIC_API_KEY`: Claude API access
- `GOOGLE_API_KEY`: Gemini API access
- `LLAMA_ENDPOINT`: Llama model endpoint
- `DATABASE_PATH`: SQLite database location
- `LOG_LEVEL`: Logging verbosity
- `MAX_CONTEXT_TOKENS`: Global token limit

### Configuration File (config.yaml)
```yaml
llm_providers:
  claude:
    models: ["claude-3-opus", "claude-3-sonnet"]
    max_tokens: 100000
  openai:
    models: ["gpt-4", "gpt-3.5-turbo"]
    max_tokens: 8000
  gemini:
    models: ["gemini-pro", "gemini-ultra"]
    max_tokens: 32000
  llama:
    models: ["llama-2-70b", "codellama"]
    max_tokens: 4000

debate_settings:
  max_participants: 10
  default_context_window: 10000
  message_retention_days: 30
```