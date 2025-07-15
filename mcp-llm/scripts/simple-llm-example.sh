#!/bin/bash

echo "🌤️  Simple LLM Example: What color is the sky?"
echo "=============================================="
echo ""

# Simple curl command
echo "Sending request to LLM Gateway..."
echo ""

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

echo ""
echo ""
echo "Note: This requires API keys to be configured in the .env file"
echo "Set ANTHROPIC_API_KEY, OPENAI_API_KEY, or GOOGLE_API_KEY"