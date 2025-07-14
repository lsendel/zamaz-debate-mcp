#!/bin/bash

echo "🌤️  Simple LLM Example: What color is the sky?"
echo "=============================================="
echo ""

# Example 1: Basic request
echo "1. Basic request to Claude:"
echo "------------------------"
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

echo ""
echo ""

# Example 2: With system message
echo "2. Request with system message:"
echo "----------------------------"
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "systemPrompt": "You are a helpful assistant. Keep answers brief.",
    "messages": [
      {
        "role": "user",
        "content": "What color is the sky?"
      }
    ],
    "maxTokens": 50
  }'

echo ""
echo ""

# Example 3: Multi-turn conversation
echo "3. Multi-turn conversation:"
echo "------------------------"
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "messages": [
      {
        "role": "user",
        "content": "What color is the sky?"
      },
      {
        "role": "assistant",
        "content": "The sky appears blue during the day due to Rayleigh scattering of sunlight."
      },
      {
        "role": "user",
        "content": "Why does it change color at sunset?"
      }
    ],
    "maxTokens": 100
  }'

echo ""
echo ""

# Example 4: Simple one-liner
echo "4. Simple one-liner:"
echo "-----------------"
echo 'curl -s -X POST http://localhost:5002/api/v1/completions -H "Content-Type: application/json" -d "{\"provider\":\"claude\",\"messages\":[{\"role\":\"user\",\"content\":\"What color is the sky?\"}]}" | jq -r .text'
echo ""
curl -s -X POST http://localhost:5002/api/v1/completions -H "Content-Type: application/json" -d '{"provider":"claude","messages":[{"role":"user","content":"What color is the sky?"}]}' | jq -r '.text // .message // "No response"'

echo ""
echo ""
echo "Note: Requires API keys in .env file"