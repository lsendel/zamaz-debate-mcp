#!/bin/bash

# Simple LLM Demo: "What color is the sky?"

echo "🤖 ZAMAZ LLM GATEWAY - DEMO"
echo "==========================="
echo "Question: What color is the sky?"
echo ""

# Base URL
LLM_URL="http://localhost:5002"

# Colors for output
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# First, let's list available providers
echo -e "${BLUE}Step 1: Listing available LLM providers...${NC}"
echo ""

curl -s -X GET """"$LLM_URL"""/api/v1/providers" | jq '.'
echo ""

# Now let's ask each provider the same question
echo -e "${BLUE}Step 2: Asking each provider 'What color is the sky?'${NC}"
echo ""

# Ask Claude
echo -e "${PURPLE}=== Asking Claude ===${NC}"
curl -X POST """"$LLM_URL"""/api/v1/completions" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "messages": [{"role": "user", "content": "What color is the sky?"}],
    "maxTokens": 100,
    "temperature": 0.7
  }' | jq '.'

echo ""

# Ask OpenAI
echo -e "${GREEN}=== Asking OpenAI (GPT-4) ===${NC}"
curl -X POST """"$LLM_URL"""/api/v1/completions" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai",
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "What color is the sky?"}],
    "maxTokens": 100,
    "temperature": 0.7
  }' | jq '.'

echo ""

# Ask Gemini
echo -e "${YELLOW}=== Asking Google Gemini ===${NC}"
curl -X POST """"$LLM_URL"""/api/v1/completions" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "gemini",
    "messages": [{"role": "user", "content": "What color is the sky?"}],
    "maxTokens": 100,
    "temperature": 0.7
  }' | jq '.'

echo ""

# Ask with streaming (if supported)
echo -e "${BLUE}Step 3: Asking with streaming enabled...${NC}"
echo ""

echo -e "${PURPLE}=== Streaming response from Claude ===${NC}"
curl -X POST """"$LLM_URL"""/api/completions/stream" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "provider": "claude",
    "messages": [{"role": "user", "content": "What color is the sky? Explain in detail."}],
    "maxTokens": 200,
    "temperature": 0.7,
    "stream": true
  }'

echo ""
echo ""

# Get provider status
echo -e "${BLUE}Step 4: Checking provider health status...${NC}"
echo ""

for provider in claude openai gemini ollama; do
  echo -e "Checking ${provider}..."
  curl -s -X GET """"$LLM_URL"""/api/v1/providers/"""$provider"""/status" | jq '.'
  echo ""
done

echo "✅ Demo complete!"