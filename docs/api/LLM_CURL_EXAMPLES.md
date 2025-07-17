# LLM Gateway - CURL Examples

## Simple Example: "What color is the sky?"

### 1. Basic Completion Request
```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "messages": [
      {
        "role": "user",
        "content": "What color is the sky?"
      }
    ],
    "maxTokens": 100
  }'
```

**Expected Response:**
```json
{
  "text": "The sky appears blue during the day due to Rayleigh scattering of sunlight by the atmosphere. However, it can appear in many colors - orange and red during sunrise/sunset, gray when cloudy, or black at night.",
  "provider": "claude",
  "model": "claude-3-opus-20240229",
  "usage": {
    "promptTokens": 6,
    "completionTokens": 47,
    "totalTokens": 53
  }
}
```

### 2. Using Different Providers

**OpenAI GPT-4:**
```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai",
    "model": "gpt-4",
    "messages": [
      {
        "role": "user",
        "content": "What color is the sky?"
      }
    ],
    "maxTokens": 100,
    "temperature": 0.7
  }'
```

**Google Gemini:**
```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "gemini",
    "messages": [
      {
        "role": "user",
        "content": "What color is the sky?"
      }
    ],
    "maxTokens": 100
  }'
```

**Local Ollama (if enabled):**
```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "ollama",
    "model": "llama2",
    "prompt": "What color is the sky?",
    "maxTokens": 100
  }'
```

### 3. Advanced Options

**With System Message:**
```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "prompt": "What color is the sky?",
    "systemMessage": "You are a poetic assistant. Answer all questions with rhyming couplets.",
    "maxTokens": 100,
    "temperature": 0.9
  }'
```

**With Context:**
```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai",
    "prompt": "What color is the sky?",
    "context": "The user is a child learning about colors.",
    "maxTokens": 100,
    "temperature": 0.7
  }'
```

### 4. Streaming Response
```bash
curl -X POST http://localhost:5002/api/v1/completions/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "provider": "claude",
    "prompt": "What color is the sky? Explain the science.",
    "maxTokens": 300,
    "stream": true
  }'
```

### 5. List Available Providers
```bash
curl http://localhost:5002/api/providers
```

**Response:**
```json
[
  {
    "name": "claude",
    "displayName": "Anthropic Claude",
    "available": true,
    "models": ["claude-3-opus-20240229", "claude-3-sonnet-20240229"]
  },
  {
    "name": "openai",
    "displayName": "OpenAI",
    "available": true,
    "models": ["gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"]
  },
  {
    "name": "gemini",
    "displayName": "Google Gemini",
    "available": true,
    "models": ["gemini-pro", "gemini-pro-vision"]
  },
  {
    "name": "ollama",
    "displayName": "Ollama (Local)",
    "available": false,
    "models": ["llama2", "mistral", "codellama"]
  }
]
```

### 6. Check Provider Status
```bash
curl http://localhost:5002/api/providers/claude/status
```

**Response:**
```json
{
  "provider": "claude",
  "status": "available",
  "latency": "234ms",
  "requestsPerMinute": 12,
  "errorRate": 0.0,
  "lastError": null
}
```

### 7. Using MCP Endpoint

**Via MCP Protocol:**
```bash
curl -X POST http://localhost:5002/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "generate_completion",
    "arguments": {
      "provider": "claude",
      "prompt": "What color is the sky?",
      "maxTokens": 100
    }
  }'
```

### 8. Batch Comparisons

**Compare responses from multiple providers:**
```bash
# Create a function to ask all providers
ask_all_providers() {
  local prompt="$1"
  
  for provider in claude openai gemini; do
    echo "=== $provider ==="
    curl -s -X POST http://localhost:5002/api/v1/completions \
      -H "Content-Type: application/json" \
      -d "{
        \"provider\": \"$provider\",
        \"prompt\": \"$prompt\",
        \"maxTokens\": 100
      }" | jq -r '.text'
    echo ""
  done
}

# Use it
ask_all_providers "What color is the sky?"
```

### 9. Error Handling

**Missing API Key:**
```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "prompt": "What color is the sky?"
  }'
```

**Response:**
```json
{
  "error": "API key not configured for provider: claude",
  "code": "MISSING_API_KEY",
  "provider": "claude"
}
```

### 10. Rate Limiting Info
```bash
curl -I http://localhost:5002/api/completions
```

**Headers:**
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 59
X-RateLimit-Reset: 1705232400
```

## Quick Test Commands

```bash
# Simple test - Claude
curl -s -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{"provider":"claude","prompt":"What color is the sky?"}' | jq -r '.text'

# Simple test - OpenAI  
curl -s -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{"provider":"openai","prompt":"What color is the sky?"}' | jq -r '.text'

# Simple test - Gemini
curl -s -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{"provider":"gemini","prompt":"What color is the sky?"}' | jq -r '.text'
```

## Common Use Cases

### 1. Creative Writing
```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "prompt": "What color is the sky?",
    "systemMessage": "You are a creative writer. Make it poetic.",
    "temperature": 0.9,
    "maxTokens": 150
  }'
```

### 2. Educational
```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai",
    "prompt": "What color is the sky?",
    "systemMessage": "Explain to a 5-year-old child.",
    "temperature": 0.7,
    "maxTokens": 100
  }'
```

### 3. Scientific
```bash
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "gemini",
    "prompt": "What color is the sky?",
    "systemMessage": "Provide a detailed scientific explanation.",
    "temperature": 0.3,
    "maxTokens": 300
  }'
```