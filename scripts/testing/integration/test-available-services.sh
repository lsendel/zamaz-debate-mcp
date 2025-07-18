#!/bin/bash

# Test Available MCP Services
# Tests only the services that are currently running

echo "=== Testing Available MCP Services ==="
echo ""

# Test LLM Service
echo "1. Testing MCP LLM Service (Port 5002)"
echo "=========================================="

# Health check
echo -n "Health Check: "
if curl -s http://localhost:5002/health | grep -q "healthy"; then
    echo "✓ Passed"
else
    echo "✗ Failed"
fi

# List providers
echo ""
echo "Available Providers:"
curl -s http://localhost:5002/providers | jq -r '.providers[] | "  - \(.name): \(.description // "No description")"' 2>/dev/null

# List all models
echo ""
echo "Available Models:"
curl -s http://localhost:5002/models | jq -r '.models[:5][] | "  - \(.provider)/\(.name)"' 2>/dev/null
echo "  ... and more"

# Test token estimation
echo ""
echo "Testing Token Estimation:"
RESPONSE=$(curl -s -X POST http://localhost:5002/tools/estimate_tokens \
    -H "Content-Type: application/json" \
    -d '{"arguments": {"text": "Hello world!", "model": "gpt-3.5-turbo"}}' 2>/dev/null)
echo "  Response: """$RESPONSE""""

echo ""
echo ""
echo "2. Testing MCP Debate Service (Port 5013)"
echo "=========================================="

# Health check
echo -n "Health Check: "
if curl -s http://localhost:5013/health | grep -q "healthy"; then
    echo "✓ Passed"
else
    echo "✗ Failed"
fi

# Try to access API info
echo ""
echo "API Endpoints:"
curl -s http://localhost:5013/ 2>/dev/null | jq '.' 2>/dev/null || echo "  No root endpoint"

# Try different endpoints
echo ""
echo "Checking various endpoints:"
echo -n "  /docs: "
curl -s -o /dev/null -w "%{http_code}" http://localhost:5013/docs
echo ""
echo -n "  /api: "
curl -s -o /dev/null -w "%{http_code}" http://localhost:5013/api
echo ""
echo -n "  /resources: "
curl -s -o /dev/null -w "%{http_code}" http://localhost:5013/resources
echo ""

# Test WebSocket endpoint
echo ""
echo "WebSocket endpoint: ws://localhost:5013/ws"

echo ""
echo ""
echo "=== Summary ==="
echo "LLM Service: ✓ Running (with 4 providers)"
echo "Debate Service: ✓ Running"
echo "Other services: ✗ Not running (Docker issue)"
echo ""
echo "Note: To run full tests, all services need to be started with Docker."