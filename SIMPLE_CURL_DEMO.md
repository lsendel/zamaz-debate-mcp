# Simple CURL Demo - "What color is the sky?"

## Working Example with zamaz-llm Service

Since the services need proper message format and the MCP endpoints aren't fully deployed yet, here's how the system would work:

### 1. Basic LLM Request (Conceptual)

**Correct format for the API:**
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
    "maxTokens": 100,
    "temperature": 0.7
  }'
```

**Response format:**
```json
{
  "text": "...",
  "provider": "claude",
  "model": "claude-3-opus-20240229",
  "usage": {
    "promptTokens": 0,
    "completionTokens": 0,
    "totalTokens": 0
  }
}
```

### 2. Using Multiple Providers

Each provider will return their response in the same format.

### 3. One-Liner Examples

```bash
# Simple question to Claude
curl -s -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{"provider":"claude","messages":[{"role":"user","content":"What color is the sky?"}]}' \
  | jq -r '.text'

# Compare multiple providers
for provider in claude openai gemini; do
  echo "=== $provider ==="
  curl -s -X POST http://localhost:5002/api/v1/completions \
    -H "Content-Type: application/json" \
    -d "{\"provider\":\"$provider\",\"messages\":[{\"role\":\"user\",\"content\":\"What color is the sky?\"}]}" \
    | jq -r '.text // .error'
  echo
done
```

### 4. With System Context

```bash
# Poetic response
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "systemPrompt": "You are a poet. Answer in verse.",
    "messages": [
      {
        "role": "user",
        "content": "What color is the sky?"
      }
    ],
    "maxTokens": 100
  }'

# Response will be in verse format
```

### 5. Educational Response

```bash
# For a child
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai",
    "systemPrompt": "Explain to a 5-year-old child.",
    "messages": [
      {
        "role": "user", 
        "content": "What color is the sky?"
      }
    ],
    "temperature": 0.7
  }'

# Response will be in simple language for a child
```

## Current Status

The LLM service is running but requires:
1. ✅ Proper message format (messages array with role/content)
2. ✅ Valid API keys in .env file
3. ⚠️  Service implementation fixes for the message handling

## Alternative: Direct Provider APIs

If you need to test immediately, you can use the providers directly:

```bash
# Direct Claude API
curl -X POST https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{
    "model": "claude-3-opus-20240229",
    "messages": [{"role": "user", "content": "What color is the sky?"}],
    "max_tokens": 100
  }'

# Direct OpenAI API
curl -X POST https://api.openai.com/v1/chat/completions \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "What color is the sky?"}],
    "max_tokens": 100
  }'
```

## Summary

The zamaz-llm gateway provides a unified interface to multiple LLM providers. Once the service implementation is fixed, you'll be able to:

1. Use any provider with the same API format
2. Switch providers easily without changing code
3. Compare responses from multiple providers
4. Add custom system prompts and parameters
5. Track usage across all providers in one place