#!/bin/bash

echo "🌤️  MCP LLM Example: What color is the sky?"
echo "==========================================="
echo ""

# Using the MCP endpoint
echo "Using MCP endpoint to ask Claude:"
echo "--------------------------------"

curl -X POST http://localhost:5002/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "generate_completion",
    "arguments": {
      "provider": "claude",
      "prompt": "What color is the sky?",
      "maxTokens": 100,
      "temperature": 0.7
    }
  }'

echo ""
echo ""

# List available providers
echo "Available LLM providers:"
echo "-----------------------"
curl -X POST http://localhost:5002/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list_providers",
    "arguments": {}
  }'

echo ""
echo ""

# Alternative: Direct provider listing
echo "Alternative method - list providers:"
echo "-----------------------------------"
curl -X POST http://localhost:5002/mcp/list-tools \
  -H "Content-Type: application/json" \
  -d '{}'

echo ""