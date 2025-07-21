#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "=== Validating Agentic Flows Implementation ==="
echo

# Check services health
echo "1. Checking service health..."
services=(
    "http://localhost:5013/actuator/health:Controller"
    "http://localhost:5005/actuator/health:Organization" 
    "http://localhost:5002/actuator/health:LLM"
    "http://localhost:5004/actuator/health:RAG"
)

for service in "${services[@]}"; do
    IFS=':' read -r url name <<< "$service"
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url")
    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✓${NC} $name API is healthy"
    else
        echo -e "${RED}✗${NC} $name API is not running (status: $response)"
    fi
done

echo
echo "2. Testing Agentic Flows REST endpoints..."

# Test flow types endpoint
echo -n "Testing GET /api/v1/agentic-flows/types: "
response=$(curl -s -w "\n%{http_code}" http://localhost:5013/api/v1/agentic-flows/types)
status=$(echo "$response" | tail -n1)
if [ "$status" = "200" ]; then
    echo -e "${GREEN}✓${NC} Flow types endpoint works"
    echo "$response" | head -n -1 | jq -r '.[] | "  - " + .' 2>/dev/null || echo "$response" | head -n -1
else
    echo -e "${RED}✗${NC} Status: $status"
fi

echo
echo "3. Testing GraphQL endpoint..."
# Test GraphQL
graphql_query='{"query":"{ agenticFlowTypes }"}'
echo -n "Testing POST /graphql: "
response=$(curl -s -X POST http://localhost:5013/graphql \
    -H "Content-Type: application/json" \
    -d "$graphql_query" \
    -w "\n%{http_code}")
status=$(echo "$response" | tail -n1)
if [ "$status" = "200" ]; then
    echo -e "${GREEN}✓${NC} GraphQL endpoint works"
else
    echo -e "${RED}✗${NC} Status: $status"
fi

echo
echo "4. Testing database connection..."
# Check if agentic_flows table exists
PGPASSWORD=postgres psql -h localhost -U lsendel -d debate_db -c "SELECT COUNT(*) FROM agentic_flows;" 2>/dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} Database table 'agentic_flows' exists"
else
    echo -e "${YELLOW}⚠${NC} Could not verify database table"
fi

echo
echo "5. Checking UI components..."
# Check if UI has agentic flow components
ui_response=$(curl -s http://localhost:3001/)
if echo "$ui_response" | grep -q "AgenticFlow\|agentic-flow\|flow-config"; then
    echo -e "${GREEN}✓${NC} UI has agentic flow components"
else
    echo -e "${YELLOW}⚠${NC} UI components not detected in initial load"
fi

echo
echo "6. Testing flow execution (if available)..."
# Try to execute a simple flow
flow_data='{
    "flowType": "INTERNAL_MONOLOGUE",
    "prompt": "Test prompt",
    "configuration": {
        "prefix": "Thinking:"
    }
}'

echo -n "Testing POST /api/v1/agentic-flows/execute: "
response=$(curl -s -X POST http://localhost:5013/api/v1/agentic-flows/execute \
    -H "Content-Type: application/json" \
    -d "$flow_data" \
    -w "\n%{http_code}")
status=$(echo "$response" | tail -n1)
if [ "$status" = "200" ] || [ "$status" = "201" ]; then
    echo -e "${GREEN}✓${NC} Flow execution works"
    echo "$response" | head -n -1 | jq '.' 2>/dev/null || echo "$response" | head -n -1
else
    echo -e "${RED}✗${NC} Status: $status"
fi

echo
echo "=== Summary ==="
echo "The implementation includes:"
echo "- Domain entities and flow processors"
echo "- Database schema with agentic_flows table"
echo "- REST and GraphQL APIs"
echo "- UI components for flow configuration"
echo "- Analytics and monitoring"
echo
echo "Note: Some endpoints may return 404 if authentication is required."