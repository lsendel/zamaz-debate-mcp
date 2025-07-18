#!/bin/bash

# Quick MCP Services Test
# A simpler test to check what's actually working

echo "=== Quick MCP Services Test ==="
echo ""

# Test each service
echo "1. MCP Organization (5005):"
curl -s ${ORGANIZATION_SERVICE_URL}/actuator/health 2>/dev/null && echo "  ✓ Running" || echo "  ✗ Not running"

echo ""
echo "2. MCP Controller (5013):"
curl -s ${CONTROLLER_SERVICE_URL}/actuator/health 2>/dev/null && echo "  ✓ Running" || echo "  ✗ Not running"

echo ""
echo "3. MCP LLM (5002):"
if curl -s ${LLM_SERVICE_URL}/actuator/health 2>/dev/null; then
    echo "  ✓ Running"
    echo "  Available providers:"
    curl -s ${LLM_SERVICE_URL}/api/v1/providers | jq -r '.[] | "    - \(.name)"' 2>/dev/null
else
    echo "  ✗ Not running"
fi

echo ""
echo "4. MCP RAG (5004):"
curl -s ${RAG_SERVICE_URL}/actuator/health 2>/dev/null && echo "  ✓ Running" || echo "  ✗ Not running"

echo ""
echo "5. MCP Template (5006):"
curl -s ${TEMPLATE_SERVICE_URL}/actuator/health 2>/dev/null && echo "  ✓ Running" || echo "  ✗ Not running"


echo ""
echo "=== Testing LLM Service Endpoints ==="
echo ""

# Test LLM providers endpoint
echo "LLM Providers endpoint:"
curl -s ${LLM_SERVICE_URL}/api/v1/providers | jq -r '.[] | "  - \(.name): \(.status)"' 2>/dev/null | head -10

echo ""
echo "=== Testing Simple LLM Completion ==="
# Try a simple completion with a mock provider
curl -s -X POST ${LLM_SERVICE_URL}/api/v1/completions \
    -H "Content-Type: application/json" \
    -d '{
        "provider": "openai",
        "messages": [{"role": "user", "content": "Say hello"}],
        "maxTokens": 10
    }' | jq '.' 2>/dev/null || echo "Completion request failed"

echo ""
echo "=== Summary ==="
RUNNING=0
[ "$(curl" -s -o /dev/null -w "%{http_code}" ${ORGANIZATION_SERVICE_URL}/actuator/health 2>/dev/null) = "200" ] && RUNNING=$((RUNNING + 1))
[ "$(curl" -s -o /dev/null -w "%{http_code}" ${CONTROLLER_SERVICE_URL}/actuator/health 2>/dev/null) = "200" ] && RUNNING=$((RUNNING + 1))
[ "$(curl" -s -o /dev/null -w "%{http_code}" ${LLM_SERVICE_URL}/actuator/health 2>/dev/null) = "200" ] && RUNNING=$((RUNNING + 1))
[ "$(curl" -s -o /dev/null -w "%{http_code}" ${RAG_SERVICE_URL}/actuator/health 2>/dev/null) = "200" ] && RUNNING=$((RUNNING + 1))
[ "$(curl" -s -o /dev/null -w "%{http_code}" ${TEMPLATE_SERVICE_URL}/actuator/health 2>/dev/null) = "200" ] && RUNNING=$((RUNNING + 1))
echo "Services running: """$RUNNING""" / 5"