# MCP LLM Service - CLAUDE.md

This file provides guidance to Claude Code when working with the mcp-llm service.

## Service Overview

The `mcp-llm` service is a unified LLM provider gateway for the zamaz-debate-mcp platform. It abstracts multiple LLM providers (Claude, OpenAI, Gemini, Llama, Grok, Qwen) behind a consistent interface, enabling seamless provider switching and feature comparison.

## Purpose

- **Provider Abstraction**: Unified interface for multiple LLM providers
- **Model Management**: List and select appropriate models per provider
- **Token Estimation**: Provider-specific token counting and limits
- **Error Handling**: Consistent error responses across providers
- **Performance**: Built-in retry logic and connection pooling

## Technology Stack

- **Language**: Python 3.11+
- **Framework**: MCP SDK + FastAPI for REST API
- **HTTP Client**: httpx for async requests
- **Providers**: Anthropic, OpenAI, Google Generative AI, Ollama
- **Token Counting**: tiktoken (OpenAI), custom implementations
- **Container**: Docker with health checks

## Supported Providers

### 1. Claude (Anthropic)
- **Models**: claude-3-opus, claude-3-sonnet, claude-3-haiku
- **Context**: Up to 200K tokens
- **Features**: Vision support, system prompts, XML tags
- **Special**: Best for complex reasoning and coding tasks

### 2. OpenAI
- **Models**: gpt-4-turbo, gpt-4, gpt-3.5-turbo
- **Context**: 4K-128K tokens depending on model
- **Features**: Function calling, JSON mode, vision (GPT-4V)
- **Token Counting**: Uses tiktoken for accurate counts

### 3. Gemini (Google)
- **Models**: gemini-pro, gemini-pro-vision
- **Context**: Up to 1M tokens (gemini-1.5-pro)
- **Features**: Multimodal, safety settings, streaming
- **Special**: Largest context window, good for long documents

### 4. Llama (via Ollama)
- **Models**: llama2, codellama, mistral, mixtral
- **Context**: Varies by model (4K-32K typical)
- **Features**: Local inference, no API costs
- **Special**: Privacy-focused, runs locally

### 5. Grok (X.AI)
- **Models**: grok-1, grok-1.5
- **Context**: 8K-32K tokens
- **Features**: Real-time information access
- **Status**: Placeholder implementation

### 6. Qwen (Alibaba)
- **Models**: qwen-turbo, qwen-plus, qwen-max
- **Context**: 8K-32K tokens
- **Features**: Strong multilingual support
- **Special**: Optimized for Chinese and Asian languages

## Architecture Patterns

### Provider Factory Pattern
```python
# Lazy initialization with singleton pattern
class LLMProviderFactory:
    _providers: Dict[str, BaseLLMProvider] = {}
    
    @classmethod
    def get_provider(cls, provider_type: str) -> BaseLLMProvider:
        if provider_type not in cls._providers:
            cls._providers[provider_type] = cls._create_provider(provider_type)
        return cls._providers[provider_type]
```

### Base Provider Pattern
All providers inherit from `BaseLLMProvider`:
- Consistent interface for all operations
- Built-in retry logic with exponential backoff
- Standardized error handling
- Abstract methods for provider-specific logic

### MCP Implementation
- **Tools**: complete, stream_complete, list_models, estimate_tokens
- **Resources**: providers list, model details, conversation placeholder
- **Error Handling**: Structured ErrorResponse with details

## Configuration

### Environment Variables
```bash
# API Keys (required for each provider)
ANTHROPIC_API_KEY=sk-ant-...
OPENAI_API_KEY=sk-...
GOOGLE_API_KEY=...
XAI_API_KEY=...
QWEN_API_KEY=...

# Ollama Configuration
OLLAMA_BASE_URL=http://localhost:11434

# Server Configuration
MCP_HOST=0.0.0.0
MCP_PORT=5002
API_HOST=0.0.0.0
API_PORT=8001

# Provider Defaults
DEFAULT_PROVIDER=openai
DEFAULT_MODEL=gpt-3.5-turbo
DEFAULT_MAX_TOKENS=1000
DEFAULT_TEMPERATURE=0.7
```

### Running the Service
```bash
# MCP Server (production)
python -m src.mcp_server

# REST API Server (development/testing)
python src/api_server.py

# Docker
docker build -t mcp-llm .
docker run -p 5002:5002 -p 8001:8001 mcp-llm

# Both servers simultaneously
python -m src.mcp_server & python src/api_server.py
```

## MCP Tools

### 1. complete
Non-streaming completion with full response
```python
{
    "provider": "openai",
    "model": "gpt-4",
    "messages": [...],
    "temperature": 0.7,
    "max_tokens": 1000
}
```

### 2. stream_complete
Streaming completion (currently returns full response)
```python
# Same parameters as complete
# TODO: Implement true streaming
```

### 3. list_models
Get available models for a provider
```python
{
    "provider": "claude"  # optional, returns all if not specified
}
```

### 4. estimate_tokens
Count tokens in messages
```python
{
    "provider": "openai",
    "model": "gpt-4",
    "messages": [...]
}
```

## REST API Endpoints

- `GET /health` - Service health check
- `GET /providers` - List all providers
- `POST /complete` - Non-streaming completion
- `POST /stream` - Streaming completion
- `GET /models` - List available models
- `POST /count-tokens` - Estimate token count

## Error Handling

### Standard Error Response
```python
{
    "error": "Rate limit exceeded",
    "error_type": "RateLimitError",
    "details": {
        "retry_after": 60,
        "limit": 10000,
        "remaining": 0
    }
}
```

### Provider-Specific Errors
- **Claude**: 429 (rate limit), 401 (auth), 400 (invalid request)
- **OpenAI**: Detailed error messages with types
- **Gemini**: Safety blocking, quota exceeded
- **Llama**: Connection errors, model not found

## Token Management

### Provider-Specific Counting
1. **OpenAI**: tiktoken with model-specific encodings
2. **Claude**: Character-based estimation (1 token ≈ 4 chars)
3. **Gemini**: Built-in count_tokens method
4. **Others**: Word-based fallback (1 token ≈ 0.75 words)

### Token Limits by Model
```python
MODEL_LIMITS = {
    "gpt-4": 8192,
    "gpt-4-32k": 32768,
    "claude-3-opus": 200000,
    "gemini-pro": 30720,
    "llama2": 4096
}
```

## Development Guidelines

### Adding a New Provider
1. Create `src/providers/newprovider_provider.py`
2. Inherit from `BaseLLMProvider`
3. Implement required methods:
   - `_make_request()`
   - `_extract_content()`
   - `_handle_error()`
   - `list_models()`
   - `estimate_tokens()`
4. Add to `ProviderType` enum
5. Update factory in `provider_factory.py`
6. Add environment variable for API key

### Testing Providers
```bash
# Test individual provider
curl -X POST http://localhost:8001/complete \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "model": "claude-3-sonnet",
    "messages": [{"role": "user", "content": "Hello"}]
  }'

# Test MCP interface
mcp-client call complete --provider openai --model gpt-4
```

### Performance Optimization
1. **Connection Pooling**: httpx clients are reused
2. **Provider Caching**: Single instance per provider
3. **Retry Logic**: Exponential backoff for transient errors
4. **Timeout Configuration**: Adjustable per provider

## Integration with Other Services

### Dependencies
- **mcp-context**: Receives optimized contexts for generation
- **mcp-debate**: Primary consumer for debate turns

### Usage Patterns
```python
# From debate service
response = await llm_client.complete(
    provider="claude",
    model="claude-3-opus",
    messages=context_window,
    temperature=0.8,
    max_tokens=500
)

# With streaming
async for chunk in llm_client.stream_complete(...):
    process_chunk(chunk)
```

## Monitoring and Health

### Health Check Response
```json
{
    "status": "healthy",
    "providers": {
        "openai": "configured",
        "claude": "configured",
        "gemini": "not_configured",
        "llama": "available"
    },
    "timestamp": "2024-01-15T10:30:00Z"
}
```

### Metrics to Track
- Request count per provider
- Token usage per provider
- Error rates and types
- Response latencies
- Rate limit status

## Known Issues and TODOs

1. **Streaming**: Currently returns full response, needs true streaming
2. **Caching**: No response caching implemented
3. **Rate Limiting**: Basic retry, needs smarter backoff
4. **Metrics**: No comprehensive metrics collection
5. **Load Balancing**: No support for multiple API keys
6. **Context Truncation**: Manual handling needed

## Debugging Tips

1. **API Key Issues**: Check environment variables
2. **Provider Errors**: Enable debug logging for full responses
3. **Token Counting**: Compare estimates with actual usage
4. **Rate Limits**: Monitor retry attempts and delays
5. **Model Availability**: Use list_models to verify

## Security Considerations

- API keys stored in environment variables only
- No key logging or exposure in responses
- Provider errors sanitized before returning
- Request validation for all inputs
- No direct model prompt injection

## Future Enhancements

- Redis caching for common requests
- True streaming implementation
- WebSocket support for real-time streaming
- Load balancing across multiple API keys
- Comprehensive metrics and monitoring
- Automatic fallback between providers
- Cost tracking and optimization
- Fine-tuning support for applicable models