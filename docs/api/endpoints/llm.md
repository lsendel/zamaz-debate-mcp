# MCP-LLM Service API Documentation

This document provides detailed documentation for the MCP-LLM service API endpoints.

## Table of Contents

1. [Generate Completion](#generate-completion)
2. [Stream Completion](#stream-completion)
3. [List Models](#list-models)
4. [Count Tokens](#count-tokens)
5. [MCP Tool: Complete](#mcp-tool-complete)
6. [MCP Tool: Stream Complete](#mcp-tool-stream-complete)
7. [MCP Tool: List Models](#mcp-tool-list-models)
8. [MCP Tool: Estimate Tokens](#mcp-tool-estimate-tokens)

---

## Generate Completion

# Endpoint: POST /api/v1/completions

**Description**: Generates a text completion using the specified LLM model

**Service**: MCP-LLM

**Authentication Required**: Yes

**Required Headers**:
- `Authorization`: Bearer token
- `X-Organization-ID`: Organization ID
- `Content-Type`: application/json

## Request

### Request Body

```json
{
  "model": "claude-3-opus-20240229",
  "prompt": "Explain quantum computing in simple terms",
  "maxTokens": 500,
  "temperature": 0.7,
  "topP": 0.9,
  "stopSequences": ["##", "END"],
  "stream": false,
  "systemPrompt": "You are a helpful assistant that explains complex topics in simple terms."
}
```

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `model` | string | Yes | The LLM model to use (e.g., "claude-3-opus-20240229", "gpt-4") |
| `prompt` | string | Yes | The prompt to generate a completion for |
| `maxTokens` | integer | No | Maximum number of tokens to generate (default varies by model) |
| `temperature` | number | No | Sampling temperature between 0 and 1 (default: 0.7) |
| `topP` | number | No | Nucleus sampling parameter between 0 and 1 (default: 1.0) |
| `stopSequences` | array | No | Sequences that will stop generation when encountered |
| `stream` | boolean | No | Whether to stream the response (default: false) |
| `systemPrompt` | string | No | System prompt to guide the model's behavior |

## Response

### Success Response (200 OK)

```json
{
  "id": "completion-123456",
  "model": "claude-3-opus-20240229",
  "content": "Quantum computing is like traditional computing but instead of using bits that are either 0 or 1, it uses quantum bits or 'qubits' that can exist in multiple states at once thanks to a property called superposition. This allows quantum computers to process certain types of problems much faster than regular computers...",
  "finishReason": "stop",
  "usage": {
    "promptTokens": 15,
    "completionTokens": 486,
    "totalTokens": 501
  }
}
```

| Property | Type | Description |
|----------|------|-------------|
| `id` | string | Unique identifier for the completion |
| `model` | string | The model used for the completion |
| `content` | string | The generated completion text |
| `finishReason` | string | Reason why the completion finished ("stop", "length", "content_filter") |
| `usage` | object | Token usage information |
| `usage.promptTokens` | integer | Number of tokens in the prompt |
| `usage.completionTokens` | integer | Number of tokens in the completion |
| `usage.totalTokens` | integer | Total number of tokens used |

### Error Responses

#### 400 Bad Request

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Invalid request parameters",
    "details": {
      "model": "must not be null"
    },
    "requestId": "request-456"
  }
}
```

#### 401 Unauthorized

```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required",
    "requestId": "request-456"
  }
}
```

#### 403 Forbidden

```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "Insufficient permissions to use model claude-3-opus-20240229",
    "requestId": "request-456"
  }
}
```

#### 404 Not Found

```json
{
  "error": {
    "code": "MODEL_NOT_FOUND",
    "message": "Model not found: claude-3-opus-invalid",
    "requestId": "request-456"
  }
}
```

#### 429 Too Many Requests

```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded",
    "details": {
      "retryAfter": 60
    },
    "requestId": "request-456"
  }
}
```

#### 500 Internal Server Error

```json
{
  "error": {
    "code": "PROVIDER_ERROR",
    "message": "Error from LLM provider",
    "requestId": "request-456"
  }
}
```

## Example

### cURL

```bash
curl -X POST "http://localhost:5002/api/v1/completions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "model": "claude-3-opus-20240229",
    "prompt": "Explain quantum computing in simple terms",
    "maxTokens": 500,
    "temperature": 0.7
  }'
```

### Python

```python
import requests

url = "http://localhost:5002/api/v1/completions"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-123"
}
payload = {
    "model": "claude-3-opus-20240229",
    "prompt": "Explain quantum computing in simple terms",
    "maxTokens": 500,
    "temperature": 0.7
}

response = requests.post(url, headers=headers, json=payload)
print(response.json())
```

### JavaScript

```javascript
fetch("http://localhost:5002/api/v1/completions", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-123"
  },
  body: JSON.stringify({
    model: "claude-3-opus-20240229",
    prompt: "Explain quantum computing in simple terms",
    maxTokens: 500,
    temperature: 0.7
  })
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error("Error:", error));
```

## Notes

- Rate limited to 60 requests per minute per organization
- Maximum prompt size varies by model
- Token limits vary by model:
  - Claude-3-Opus: 100,000 tokens
  - GPT-4: 8,000 tokens
  - Gemini-Pro: 32,000 tokens
- All requests are logged for audit purposes

---

## Stream Completion

# Endpoint: POST /api/v1/completions/stream

**Description**: Streams a text completion using the specified LLM model

**Service**: MCP-LLM

**Authentication Required**: Yes

**Required Headers**:
- `Authorization`: Bearer token
- `X-Organization-ID`: Organization ID
- `Content-Type`: application/json
- `Accept`: text/event-stream

## Request

### Request Body

```json
{
  "model": "claude-3-opus-20240229",
  "prompt": "Write a short story about a robot learning to paint",
  "maxTokens": 1000,
  "temperature": 0.9,
  "topP": 0.95,
  "stopSequences": ["##", "END"],
  "systemPrompt": "You are a creative storyteller."
}
```

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `model` | string | Yes | The LLM model to use (e.g., "claude-3-opus-20240229", "gpt-4") |
| `prompt` | string | Yes | The prompt to generate a completion for |
| `maxTokens` | integer | No | Maximum number of tokens to generate (default varies by model) |
| `temperature` | number | No | Sampling temperature between 0 and 1 (default: 0.7) |
| `topP` | number | No | Nucleus sampling parameter between 0 and 1 (default: 1.0) |
| `stopSequences` | array | No | Sequences that will stop generation when encountered |
| `systemPrompt` | string | No | System prompt to guide the model's behavior |

## Response

### Success Response (200 OK)

The response is a stream of server-sent events (SSE) with the following format:

```
data: {"id":"completion-123456","model":"claude-3-opus-20240229","content":"Unit","finishReason":null}

data: {"id":"completion-123456","model":"claude-3-opus-20240229","content":" 7","finishReason":null}

data: {"id":"completion-123456","model":"claude-3-opus-20240229","content":" was","finishReason":null}

...

data: {"id":"completion-123456","model":"claude-3-opus-20240229","content":".","finishReason":"stop","usage":{"promptTokens":12,"completionTokens":523,"totalTokens":535}}

data: [DONE]
```

Each event contains a chunk of the completion. The final event includes the finish reason and token usage statistics, followed by a `[DONE]` event.

### Error Responses

Error responses are returned as a single SSE event:

```
data: {"error":{"code":"INVALID_REQUEST","message":"Invalid request parameters","details":{"model":"must not be null"},"requestId":"request-456"}}
```

## Example

### cURL

```bash
curl -X POST "http://localhost:5002/api/v1/completions/stream" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -H "Authorization: Bearer your-token" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "model": "claude-3-opus-20240229",
    "prompt": "Write a short story about a robot learning to paint",
    "maxTokens": 1000,
    "temperature": 0.9
  }'
```

### JavaScript

```javascript
const eventSource = new EventSource("http://localhost:5002/api/v1/completions/stream", {
  headers: {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-123"
  },
  method: "POST",
  body: JSON.stringify({
    model: "claude-3-opus-20240229",
    prompt: "Write a short story about a robot learning to paint",
    maxTokens: 1000,
    temperature: 0.9
  })
});

eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data);
  if (data === "[DONE]") {
    eventSource.close();
    return;
  }
  
  if (data.error) {
    console.error("Error:", data.error);
    eventSource.close();
    return;
  }
  
  console.log(data.content);
};

eventSource.onerror = (error) => {
  console.error("EventSource error:", error);
  eventSource.close();
};
```

## Notes

- Rate limited to 30 streaming requests per minute per organization
- Streaming connections time out after 5 minutes of inactivity
- Maximum prompt size varies by model
- Token limits vary by model
- All requests are logged for audit purposes

---

## List Models

# Endpoint: GET /api/v1/models

**Description**: Lists all available LLM models

**Service**: MCP-LLM

**Authentication Required**: Yes

**Required Headers**:
- `Authorization`: Bearer token
- `X-Organization-ID`: Organization ID

## Request

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `provider` | string | No | null | Filter models by provider (e.g., "claude", "openai", "gemini") |

## Response

### Success Response (200 OK)

```json
{
  "models": [
    {
      "id": "claude-3-opus-20240229",
      "provider": "claude",
      "name": "Claude 3 Opus",
      "maxTokens": 100000,
      "features": ["completion", "streaming", "system_prompt"],
      "pricing": {
        "inputTokens": 0.00001500,
        "outputTokens": 0.00007500,
        "currency": "USD"
      }
    },
    {
      "id": "claude-3-sonnet-20240229",
      "provider": "claude",
      "name": "Claude 3 Sonnet",
      "maxTokens": 100000,
      "features": ["completion", "streaming", "system_prompt"],
      "pricing": {
        "inputTokens": 0.00000300,
        "outputTokens": 0.00001500,
        "currency": "USD"
      }
    },
    {
      "id": "gpt-4",
      "provider": "openai",
      "name": "GPT-4",
      "maxTokens": 8192,
      "features": ["completion", "streaming", "system_prompt"],
      "pricing": {
        "inputTokens": 0.00003000,
        "outputTokens": 0.00006000,
        "currency": "USD"
      }
    }
  ]
}
```

| Property | Type | Description |
|----------|------|-------------|
| `models` | array | List of available models |
| `models[].id` | string | Unique identifier for the model |
| `models[].provider` | string | Provider of the model (e.g., "claude", "openai", "gemini") |
| `models[].name` | string | Display name of the model |
| `models[].maxTokens` | integer | Maximum number of tokens the model can process |
| `models[].features` | array | List of features supported by the model |
| `models[].pricing` | object | Pricing information for the model |
| `models[].pricing.inputTokens` | number | Cost per input token in the specified currency |
| `models[].pricing.outputTokens` | number | Cost per output token in the specified currency |
| `models[].pricing.currency` | string | Currency for pricing (e.g., "USD") |

### Error Responses

#### 401 Unauthorized

```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required",
    "requestId": "request-456"
  }
}
```

#### 403 Forbidden

```json
{
  "error": {
    "code": "FORBIDDEN",
    "message": "Insufficient permissions to list models",
    "requestId": "request-456"
  }
}
```

## Example

### cURL

```bash
curl -X GET "http://localhost:5002/api/v1/models?provider=claude" \
  -H "Authorization: Bearer your-token" \
  -H "X-Organization-ID: org-123"
```

### Python

```python
import requests

url = "http://localhost:5002/api/v1/models"
headers = {
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-123"
}
params = {
    "provider": "claude"
}

response = requests.get(url, headers=headers, params=params)
print(response.json())
```

### JavaScript

```javascript
fetch("http://localhost:5002/api/v1/models?provider=claude", {
  method: "GET",
  headers: {
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-123"
  }
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error("Error:", error));
```

## Notes

- The list of available models may vary based on organization tier
- Model availability is subject to change
- Pricing information is for reference only and may not reflect actual billing

---

## Count Tokens

# Endpoint: POST /api/v1/tokens/count

**Description**: Counts the number of tokens in the provided text for a specific model

**Service**: MCP-LLM

**Authentication Required**: Yes

**Required Headers**:
- `Authorization`: Bearer token
- `X-Organization-ID`: Organization ID
- `Content-Type`: application/json

## Request

### Request Body

```json
{
  "model": "claude-3-opus-20240229",
  "text": "This is a sample text to count tokens for."
}
```

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `model` | string | Yes | The LLM model to use for token counting |
| `text` | string | Yes | The text to count tokens for |

## Response

### Success Response (200 OK)

```json
{
  "tokenCount": 9,
  "model": "claude-3-opus-20240229"
}
```

| Property | Type | Description |
|----------|------|-------------|
| `tokenCount` | integer | Number of tokens in the provided text |
| `model` | string | The model used for token counting |

### Error Responses

#### 400 Bad Request

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Invalid request parameters",
    "details": {
      "text": "must not be null"
    },
    "requestId": "request-456"
  }
}
```

#### 401 Unauthorized

```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required",
    "requestId": "request-456"
  }
}
```

#### 404 Not Found

```json
{
  "error": {
    "code": "MODEL_NOT_FOUND",
    "message": "Model not found: claude-3-opus-invalid",
    "requestId": "request-456"
  }
}
```

## Example

### cURL

```bash
curl -X POST "http://localhost:5002/api/v1/tokens/count" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "model": "claude-3-opus-20240229",
    "text": "This is a sample text to count tokens for."
  }'
```

### Python

```python
import requests

url = "http://localhost:5002/api/v1/tokens/count"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-123"
}
payload = {
    "model": "claude-3-opus-20240229",
    "text": "This is a sample text to count tokens for."
}

response = requests.post(url, headers=headers, json=payload)
print(response.json())
```

### JavaScript

```javascript
fetch("http://localhost:5002/api/v1/tokens/count", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "Authorization": "Bearer your-token",
    "X-Organization-ID": "org-123"
  },
  body: JSON.stringify({
    model: "claude-3-opus-20240229",
    text: "This is a sample text to count tokens for."
  })
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error("Error:", error));
```

## Notes

- Token counting is an estimate and may vary slightly from the actual token count used by the provider
- Different models use different tokenization algorithms
- Maximum text size for token counting is 1MB
- This endpoint is not rate limited

---

## MCP Tool: Complete

# MCP Tool: complete

**Description**: Generates a text completion using the specified LLM model

**Service**: MCP-LLM

**Authentication Required**: Yes

## Parameters

```json
{
  "model": "claude-3-opus-20240229",
  "prompt": "Explain quantum computing in simple terms",
  "max_tokens": 500,
  "temperature": 0.7,
  "top_p": 0.9,
  "stop_sequences": ["##", "END"],
  "system_prompt": "You are a helpful assistant that explains complex topics in simple terms."
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `model` | string | Yes | The LLM model to use (e.g., "claude-3-opus-20240229", "gpt-4") |
| `prompt` | string | Yes | The prompt to generate a completion for |
| `max_tokens` | integer | No | Maximum number of tokens to generate (default varies by model) |
| `temperature` | number | No | Sampling temperature between 0 and 1 (default: 0.7) |
| `top_p` | number | No | Nucleus sampling parameter between 0 and 1 (default: 1.0) |
| `stop_sequences` | array | No | Sequences that will stop generation when encountered |
| `system_prompt` | string | No | System prompt to guide the model's behavior |

## Result

```json
{
  "content": "Quantum computing is like traditional computing but instead of using bits that are either 0 or 1, it uses quantum bits or 'qubits' that can exist in multiple states at once thanks to a property called superposition. This allows quantum computers to process certain types of problems much faster than regular computers...",
  "finish_reason": "stop",
  "usage": {
    "prompt_tokens": 15,
    "completion_tokens": 486,
    "total_tokens": 501
  }
}
```

| Property | Type | Description |
|----------|------|-------------|
| `content` | string | The generated completion text |
| `finish_reason` | string | Reason why the completion finished ("stop", "length", "content_filter") |
| `usage` | object | Token usage information |
| `usage.prompt_tokens` | integer | Number of tokens in the prompt |
| `usage.completion_tokens` | integer | Number of tokens in the completion |
| `usage.total_tokens` | integer | Total number of tokens used |

## Example

### Python

```python
from mcp_client import MCPClient

client = MCPClient("http://localhost:5002")

result = await client.call_tool("complete", {
    "model": "claude-3-opus-20240229",
    "prompt": "Explain quantum computing in simple terms",
    "max_tokens": 500,
    "temperature": 0.7
})

print(result["content"])
```

## Notes

- Rate limited to 60 requests per minute per organization
- Maximum prompt size varies by model
- Token limits vary by model
- All requests are logged for audit purposes

---

## MCP Tool: Stream Complete

# MCP Tool: stream_complete

**Description**: Streams a text completion using the specified LLM model

**Service**: MCP-LLM

**Authentication Required**: Yes

## Parameters

```json
{
  "model": "claude-3-opus-20240229",
  "prompt": "Write a short story about a robot learning to paint",
  "max_tokens": 1000,
  "temperature": 0.9,
  "top_p": 0.95,
  "stop_sequences": ["##", "END"],
  "system_prompt": "You are a creative storyteller."
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `model` | string | Yes | The LLM model to use (e.g., "claude-3-opus-20240229", "gpt-4") |
| `prompt` | string | Yes | The prompt to generate a completion for |
| `max_tokens` | integer | No | Maximum number of tokens to generate (default varies by model) |
| `temperature` | number | No | Sampling temperature between 0 and 1 (default: 0.7) |
| `top_p` | number | No | Nucleus sampling parameter between 0 and 1 (default: 1.0) |
| `stop_sequences` | array | No | Sequences that will stop generation when encountered |
| `system_prompt` | string | No | System prompt to guide the model's behavior |

## Result

The result is a generator that yields chunks of the completion:

```python
# Example of what the generator yields
{
  "content": "Unit",
  "finish_reason": null
}

{
  "content": " 7",
  "finish_reason": null
}

{
  "content": " was",
  "finish_reason": null
}

# ... more chunks ...

{
  "content": ".",
  "finish_reason": "stop",
  "usage": {
    "prompt_tokens": 12,
    "completion_tokens": 523,
    "total_tokens": 535
  }
}
```

Each chunk contains a portion of the completion. The final chunk includes the finish reason and token usage statistics.

## Example

### Python

```python
from mcp_client import MCPClient

client = MCPClient("http://localhost:5002")

async for chunk in await client.call_tool("stream_complete", {
    "model": "claude-3-opus-20240229",
    "prompt": "Write a short story about a robot learning to paint",
    "max_tokens": 1000,
    "temperature": 0.9
}):
    print(chunk["content"], end="", flush=True)
    
    if chunk.get("finish_reason"):
        print("\n\nUsage:", chunk["usage"])
```

## Notes

- Rate limited to 30 streaming requests per minute per organization
- Streaming connections time out after 5 minutes of inactivity
- Maximum prompt size varies by model
- Token limits vary by model
- All requests are logged for audit purposes

---

## MCP Tool: List Models

# MCP Tool: list_models

**Description**: Lists all available LLM models

**Service**: MCP-LLM

**Authentication Required**: Yes

## Parameters

```json
{
  "provider": "claude"
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `provider` | string | No | Filter models by provider (e.g., "claude", "openai", "gemini") |

## Result

```json
{
  "models": [
    {
      "id": "claude-3-opus-20240229",
      "provider": "claude",
      "name": "Claude 3 Opus",
      "max_tokens": 100000,
      "features": ["completion", "streaming", "system_prompt"],
      "pricing": {
        "input_tokens": 0.00001500,
        "output_tokens": 0.00007500,
        "currency": "USD"
      }
    },
    {
      "id": "claude-3-sonnet-20240229",
      "provider": "claude",
      "name": "Claude 3 Sonnet",
      "max_tokens": 100000,
      "features": ["completion", "streaming", "system_prompt"],
      "pricing": {
        "input_tokens": 0.00000300,
        "output_tokens": 0.00001500,
        "currency": "USD"
      }
    }
  ]
}
```

| Property | Type | Description |
|----------|------|-------------|
| `models` | array | List of available models |
| `models[].id` | string | Unique identifier for the model |
| `models[].provider` | string | Provider of the model (e.g., "claude", "openai", "gemini") |
| `models[].name` | string | Display name of the model |
| `models[].max_tokens` | integer | Maximum number of tokens the model can process |
| `models[].features` | array | List of features supported by the model |
| `models[].pricing` | object | Pricing information for the model |
| `models[].pricing.input_tokens` | number | Cost per input token in the specified currency |
| `models[].pricing.output_tokens` | number | Cost per output token in the specified currency |
| `models[].pricing.currency` | string | Currency for pricing (e.g., "USD") |

## Example

### Python

```python
from mcp_client import MCPClient

client = MCPClient("http://localhost:5002")

result = await client.call_tool("list_models", {
    "provider": "claude"
})

for model in result["models"]:
    print(f"{model['name']} ({model['id']}): Max tokens: {model['max_tokens']}")
```

## Notes

- The list of available models may vary based on organization tier
- Model availability is subject to change
- Pricing information is for reference only and may not reflect actual billing

---

## MCP Tool: Estimate Tokens

# MCP Tool: estimate_tokens

**Description**: Counts the number of tokens in the provided text for a specific model

**Service**: MCP-LLM

**Authentication Required**: Yes

## Parameters

```json
{
  "model": "claude-3-opus-20240229",
  "text": "This is a sample text to count tokens for."
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `model` | string | Yes | The LLM model to use for token counting |
| `text` | string | Yes | The text to count tokens for |

## Result

```json
{
  "token_count": 9,
  "model": "claude-3-opus-20240229"
}
```

| Property | Type | Description |
|----------|------|-------------|
| `token_count` | integer | Number of tokens in the provided text |
| `model` | string | The model used for token counting |

## Example

### Python

```python
from mcp_client import MCPClient

client = MCPClient("http://localhost:5002")

result = await client.call_tool("estimate_tokens", {
    "model": "claude-3-opus-20240229",
    "text": "This is a sample text to count tokens for."
})

print(f"Token count: {result['token_count']}")
```

## Notes

- Token counting is an estimate and may vary slightly from the actual token count used by the provider
- Different models use different tokenization algorithms
- Maximum text size for token counting is 1MB
- This endpoint is not rate limited
