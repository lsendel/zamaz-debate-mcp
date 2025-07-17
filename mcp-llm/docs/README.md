# MCP-LLM Service Documentation

The MCP-LLM service provides a unified gateway to multiple Large Language Model (LLM) providers, including Claude (Anthropic), GPT (OpenAI), Gemini (Google), and Llama (local via Ollama).

## Overview

The MCP-LLM service abstracts away the differences between various LLM providers, offering a consistent interface for generating text completions. It handles authentication, rate limiting, caching, and provider-specific optimizations.

## Features

- **Multi-provider support**: Unified interface for Claude, GPT, Gemini, and Llama models
- **Streaming responses**: Support for streaming completions
- **Token management**: Token counting and optimization
- **Caching**: Response caching for improved performance and reduced costs
- **Fallback mechanisms**: Automatic fallback to alternative providers
- **Usage tracking**: Per-organization usage tracking and reporting

## Architecture

The service follows a clean architecture pattern:

- **Controllers**: Handle HTTP requests and responses
- **Services**: Implement business logic
- **Providers**: Interface with specific LLM APIs
- **Models**: Define data structures
- **Configuration**: Manage service settings

## API Endpoints

### Completions

- `POST /api/v1/completions`: Generate a completion
- `POST /api/v1/completions/stream`: Stream a completion

### Models

- `GET /api/v1/models`: List available models
- `GET /api/v1/models/{provider}`: List models for a specific provider

### Tokens

- `POST /api/v1/tokens/count`: Count tokens in a text
- `POST /api/v1/tokens/optimize`: Optimize text to fit token limits

### MCP Tools

The service exposes the following MCP tools:

- `complete`: Generate a completion
- `stream_complete`: Stream a completion
- `list_models`: List available models
- `estimate_tokens`: Count tokens in a text

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `CLAUDE_API_KEY` | Anthropic API key | - |
| `OPENAI_API_KEY` | OpenAI API key | - |
| `GEMINI_API_KEY` | Google API key | - |
| `OLLAMA_BASE_URL` | URL for Ollama service | http://ollama:11434 |
| `REDIS_HOST` | Redis host for caching | redis |
| `REDIS_PORT` | Redis port | 6379 |
| `LOG_LEVEL` | Logging level | INFO |

### Provider Configuration

Provider-specific settings can be configured in `config/providers.yml`:

```yaml
providers:
  claude:
    enabled: true
    models:
      - name: claude-3-opus-20240229
        max_tokens: 100000
      - name: claude-3-sonnet-20240229
        max_tokens: 100000
  openai:
    enabled: true
    models:
      - name: gpt-4
        max_tokens: 8192
      - name: gpt-3.5-turbo
        max_tokens: 4096
  gemini:
    enabled: true
    models:
      - name: gemini-pro
        max_tokens: 32768
  llama:
    enabled: true
    models:
      - name: llama2:70b
        max_tokens: 4096
```

## Usage Examples

### Generate a Completion

```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "model": "claude-3-opus-20240229",
    "prompt": "Explain quantum computing in simple terms",
    "maxTokens": 500,
    "temperature": 0.7
  }'
```

### Stream a Completion

```bash
curl -X POST http://localhost:5002/api/v1/completions/stream \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "model": "gpt-4",
    "prompt": "Write a short story about a robot learning to paint",
    "maxTokens": 1000,
    "temperature": 0.9
  }'
```

### List Available Models

```bash
curl -X GET http://localhost:5002/api/v1/models \
  -H "X-Organization-ID: org-123"
```

## Provider Implementation

### Adding a New Provider

To add a new LLM provider:

1. Create a new provider class in `src/main/java/com/zamaz/mcp/llm/provider/`
2. Implement the `LlmProvider` interface
3. Register the provider in `ProviderFactory`
4. Add configuration in `config/providers.yml`

Example provider implementation:

```java
package com.zamaz.mcp.llm.provider;

import com.zamaz.mcp.llm.model.CompletionRequest;
import com.zamaz.mcp.llm.model.CompletionResponse;

public class NewProvider implements LlmProvider {
    
    @Override
    public CompletionResponse complete(CompletionRequest request) {
        // Implementation
    }
    
    @Override
    public List<ModelInfo> listModels() {
        // Implementation
    }
    
    // Other methods
}
```

## Monitoring and Metrics

The service exposes the following metrics:

- Request count by provider
- Token usage by provider and organization
- Response time by provider
- Error rate by provider
- Cache hit/miss ratio

Access metrics at: `http://localhost:5002/actuator/metrics`

## Troubleshooting

### Common Issues

1. **API Key Issues**
   - Check that API keys are correctly set in environment variables
   - Verify API key permissions and quotas

2. **Provider Errors**
   - Check provider status pages for outages
   - Review logs for specific error messages

3. **Performance Issues**
   - Check Redis connection and performance
   - Monitor token usage and rate limits

### Logs

Service logs can be accessed via:

```bash
docker-compose logs mcp-llm
```

## Development

### Building the Service

```bash
cd mcp-llm
mvn clean install
```

### Running Tests

```bash
cd mcp-llm
mvn test
```

### Local Development

```bash
cd mcp-llm
mvn spring-boot:run
```
