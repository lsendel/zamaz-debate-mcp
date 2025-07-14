#!/bin/bash

# Quick MCP Services Test
# A simpler test to check what's actually working

echo "=== Quick MCP Services Test ==="
echo ""

# Test each service
echo "1. MCP Organization (5005):"
curl -s http://localhost:5005/health 2>/dev/null && echo "  ✓ Running" || echo "  ✗ Not running"

echo ""
echo "2. MCP Context (5001):"
curl -s http://localhost:5001/health 2>/dev/null && echo "  ✓ Running" || echo "  ✗ Not running"

echo ""
echo "3. MCP LLM (5002):"
if curl -s http://localhost:5002/health | grep -q "healthy"; then
    echo "  ✓ Running"
    echo "  Available providers:"
    curl -s http://localhost:5002/providers | jq -r '.providers[] | "    - \(.name)"' 2>/dev/null
fi

echo ""
echo "4. MCP Debate (5013):"
if curl -s http://localhost:5013/health | grep -q "healthy"; then
    echo "  ✓ Running"
    # Try to list debates
    echo "  Checking debates endpoint:"
    curl -s http://localhost:5013/resources/debates 2>/dev/null | jq '.' 2>/dev/null || echo "    No debates resource"
fi

echo ""
echo "5. MCP RAG (5004):"
curl -s http://localhost:5004/health 2>/dev/null && echo "  ✓ Running" || echo "  ✗ Not running"

echo ""
echo "6. MCP Template (5006):"
curl -s http://localhost:5006/health 2>/dev/null && echo "  ✓ Running" || echo "  ✗ Not running"

echo ""
echo "=== Testing LLM Service Endpoints ==="
echo ""

# Test LLM models endpoint
echo "LLM Models endpoint:"
curl -s http://localhost:5002/models | jq -r '.models[] | "  - \(.provider): \(.name)"' 2>/dev/null | head -10

echo ""
echo "=== Testing Simple LLM Completion ==="
# Try a simple completion with a mock provider
curl -s -X POST http://localhost:5002/completions \
    -H "Content-Type: application/json" \
    -d '{
        "provider": "openai",
        "model": "gpt-3.5-turbo",
        "messages": [{"role": "user", "content": "Say hello"}],
        "max_tokens": 10
    }' | jq '.' 2>/dev/null || echo "Completion request failed"

echo ""
echo "=== Summary ==="
RUNNING=$(curl -s http://localhost:5002/health 2>/dev/null && echo 1 || echo 0)
RUNNING=$((RUNNING + $(curl -s http://localhost:5013/health 2>/dev/null && echo 1 || echo 0)))
echo "Services running: $RUNNING / 6"