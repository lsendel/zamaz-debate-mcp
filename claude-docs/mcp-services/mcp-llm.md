# MCP LLM Service - Claude Development Guide

## Quick Reference
- **Port**: 5002
- **Database**: Redis (for caching)
- **Primary Purpose**: Unified interface for multiple LLM providers
- **Dependencies**: Redis, various LLM APIs

## Key Files to Check
```
mcp-llm/
├── src/
│   ├── mcp_server.py              # MCP interface - START HERE
│   ├── models.py                  # Request/response models
│   └── providers/
│       ├── base_provider.py       # Abstract base class
│       ├── openai_provider.py     # OpenAI/GPT models
│       ├── claude_provider.py     # Anthropic Claude
│       ├── gemini_provider.py     # Google Gemini
│       ├── llama_provider.py      # Ollama/local models
│       ├── grok_provider.py       # X.AI Grok
│       ├── qwen_provider.py       # Alibaba Qwen
│       └── provider_factory.py    # Provider selection
```

## Current Implementation Status
✅ **Implemented**:
- Multi-provider support (7 providers)
- Streaming responses
- Token counting
- Redis caching
- Retry logic
- Error handling
- Model listing

❌ **Not Implemented**:
- Organization-based quotas
- Cost tracking
- Request queuing
- Model health monitoring
- Fallback providers
- Response quality metrics

## Supported Providers & Models

### 1. OpenAI
```python
MODELS = [
    "gpt-4-turbo-preview", "gpt-4", "gpt-4-32k",
    "gpt-3.5-turbo", "gpt-3.5-turbo-16k"
]
# Requires: OPENAI_API_KEY
```

### 2. Anthropic Claude
```python
MODELS = [
    "claude-3-opus-20240229",
    "claude-3-sonnet-20240229",
    "claude-3-haiku-20240307",
    "claude-2.1", "claude-2.0"
]
# Requires: ANTHROPIC_API_KEY
```

### 3. Google Gemini
```python
MODELS = [
    "gemini-pro", "gemini-pro-vision"
]
# Requires: GOOGLE_API_KEY
```

### 4. Ollama (Local)
```python
# Dynamic model list from Ollama
# Common: llama2, mistral, codellama, mixtral
# Requires: OLLAMA_ENDPOINT
```

### 5. X.AI Grok
```python
MODELS = ["grok-beta"]
# Requires: XAI_API_KEY
```

### 6. Alibaba Qwen
```python
MODELS = [
    "qwen-turbo", "qwen-plus", 
    "qwen-max", "qwen-max-longcontext"
]
# Requires: DASHSCOPE_API_KEY
```

## Common Development Tasks

### 1. Adding a New Provider
```python
# 1. Create provider file: providers/new_provider.py
from .base_provider import BaseLLMProvider

class NewProvider(BaseLLMProvider):
    def __init__(self):
        self.api_key = os.getenv("NEW_PROVIDER_API_KEY")
    
    async def generate(self, request):
        # Implement API call
        pass
    
    async def stream_generate(self, request):
        # Implement streaming
        pass
    
    def count_tokens(self, text):
        # Implement token counting
        pass

# 2. Register in provider_factory.py
PROVIDERS = {
    "new_provider": NewProvider,
    # ... existing providers
}

# 3. Add models to models.py if needed
```

### 2. Implementing Streaming
```python
async def stream_generate(self, request):
    # Most providers support streaming:
    async for chunk in api_stream_call():
        yield GenerateResponseChunk(
            content=chunk.text,
            finish_reason=None,
            usage=None
        )
    
    # Final chunk with usage:
    yield GenerateResponseChunk(
        content="",
        finish_reason="stop",
        usage=TokenUsage(...)
    )
```

### 3. Token Counting Patterns
```python
# OpenAI style (tiktoken):
import tiktoken
encoding = tiktoken.encoding_for_model(model)
tokens = len(encoding.encode(text))

# Anthropic style:
tokens = anthropic.count_tokens(text)

# Rough estimation (fallback):
tokens = len(text.split()) * 1.3
```

## Caching Strategy

### Redis Cache Structure
```python
# Cache key format:
cache_key = f"llm:{provider}:{model}:{hash(messages)}:{temperature}"

# Cache TTL:
CACHE_TTL = 3600  # 1 hour default

# Skip cache for:
- Streaming requests
- High temperature (>0.7)
- Specific providers (if configured)
```

### Cache Invalidation
```python
# Manual invalidation:
await redis_client.delete(f"llm:{provider}:*")

# Automatic expiry:
await redis_client.setex(key, CACHE_TTL, value)
```

## Error Handling Patterns

### Provider Errors
```python
class LLMProviderError(Exception):
    """Base provider error"""
    provider: str
    model: str
    original_error: Exception

class RateLimitError(LLMProviderError):
    """Rate limit exceeded"""
    retry_after: int

class ModelNotFoundError(LLMProviderError):
    """Model doesn't exist"""

class AuthenticationError(LLMProviderError):
    """Invalid API key"""
```

### Retry Logic
```python
@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=4, max=10),
    retry=retry_if_exception_type(RateLimitError)
)
async def generate_with_retry():
    return await provider.generate(request)
```

## Integration Examples

### From Debate Service
```python
# Generate debate response:
response = await llm_client.generate({
    "provider": "openai",
    "model": "gpt-4",
    "messages": [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_message}
    ],
    "temperature": 0.7,
    "max_tokens": 500
})
```

### Streaming Response
```python
# For real-time UI updates:
async for chunk in llm_client.stream_generate(request):
    await websocket.send_json({
        "type": "llm_chunk",
        "content": chunk.content
    })
```

## Testing Different Providers

### Quick Test Commands
```bash
# Test OpenAI
curl -X POST http://localhost:5002/tools/generate \
  -d '{"provider": "openai", "model": "gpt-3.5-turbo", "messages": [{"role": "user", "content": "Hello"}]}'

# Test Ollama (local)
curl -X POST http://localhost:5002/tools/generate \
  -d '{"provider": "llama", "model": "llama2", "messages": [{"role": "user", "content": "Hello"}]}'

# List available models
curl http://localhost:5002/tools/list_models
```

## Performance Optimization

### Current Optimizations
1. **Connection Pooling**: Reuse HTTP connections
2. **Redis Caching**: Cache repeated requests
3. **Streaming**: Reduce time-to-first-token
4. **Concurrent Requests**: Via asyncio

### Optimization Opportunities
```python
# Request batching (not implemented):
async def batch_generate(requests: List[GenerateRequest]):
    # Group by provider and model
    # Send batched requests
    # Distribute responses

# Fallback providers (not implemented):
async def generate_with_fallback(request):
    for provider in [primary, secondary, tertiary]:
        try:
            return await provider.generate(request)
        except LLMProviderError:
            continue
```

## Monitoring & Metrics

### Key Metrics to Track
```python
# Per provider/model:
- Request count
- Error rate
- Average latency
- Token usage
- Cache hit rate
- Cost per request

# Alerts for:
- High error rates
- Slow response times
- Quota approaching
- Provider downtime
```

## Environment Variables
```bash
# Provider API Keys
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
GOOGLE_API_KEY=...
XAI_API_KEY=...
DASHSCOPE_API_KEY=...

# Ollama Configuration
OLLAMA_ENDPOINT=http://ollama:11434
USE_LOCAL_LLM=false

# Redis Cache
REDIS_URL=redis://redis:6379/1
CACHE_TTL=3600

# Service Config
MCP_PORT=5002
LOG_LEVEL=INFO
```

## Common Issues & Solutions

### Issue: "Model not available"
```python
# Check: Model name spelling
# Check: Provider has model access
# Check: Ollama has model pulled
# Solution: Use list_models first
```

### Issue: "Rate limit exceeded"
```python
# Check: API quota/limits
# Solution: Implement backoff
# Solution: Use multiple API keys
# Solution: Cache more aggressively
```

### Issue: "Timeout error"
```python
# Long requests might timeout
# Check: max_tokens setting
# Solution: Increase timeout
# Solution: Use streaming
```

## Cost Optimization

### Token Usage Tracking
```python
# Track usage per organization:
usage = {
    "organization_id": "org-123",
    "provider": "openai",
    "model": "gpt-4",
    "input_tokens": 150,
    "output_tokens": 200,
    "estimated_cost": 0.012
}
```

### Cost Reduction Strategies
1. Use smaller models when possible
2. Implement aggressive caching
3. Compress/summarize context
4. Use local models (Ollama) for development
5. Set appropriate max_tokens limits

## Next Development Priorities
1. Implement organization-based quotas
2. Add cost tracking and billing integration
3. Create provider health monitoring
4. Implement request queuing
5. Add response quality metrics
6. Create provider fallback chains
7. Add A/B testing framework
8. Implement prompt optimization