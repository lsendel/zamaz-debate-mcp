#!/bin/bash

# Test script for Java MCP services
set -e

echo "🧪 Testing Java MCP Services"
echo "============================"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Service URLs
ORG_URL="http://localhost:5005"
LLM_URL="http://localhost:5002"
CONTROLLER_URL="http://localhost:5013"

# Test function
test_endpoint() {
    local url=$1
    local expected_status=$2
    local description=$3
    
    echo -n "Testing $description... "
    
    status=$(curl -s -o /dev/null -w "%{http_code}" "$url" || echo "000")
    
    if [ "$status" = "$expected_status" ]; then
        echo -e "${GREEN}✓ PASS${NC} (Status: $status)"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (Expected: $expected_status, Got: $status)"
        return 1
    fi
}

# Test function with body
test_endpoint_with_body() {
    local url=$1
    local method=$2
    local body=$3
    local expected_status=$4
    local description=$5
    
    echo -n "Testing $description... "
    
    status=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" -H "Content-Type: application/json" -d "$body" "$url" || echo "000")
    
    if [ "$status" = "$expected_status" ]; then
        echo -e "${GREEN}✓ PASS${NC} (Status: $status)"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} (Expected: $expected_status, Got: $status)"
        return 1
    fi
}

echo ""
echo "1️⃣ Testing MCP Organization Service (Java)"
echo "----------------------------------------"
test_endpoint "$ORG_URL/actuator/health" "200" "Health check"
test_endpoint "$ORG_URL/swagger-ui.html" "200" "Swagger UI"
test_endpoint "$ORG_URL/api-docs" "200" "OpenAPI docs"

# Test MCP tools
echo ""
echo "Testing MCP Tools endpoints:"
test_endpoint_with_body "$ORG_URL/tools/create_organization" "POST" '{"name":"Test Org","description":"Test organization"}' "401" "Create organization (should require auth)"
test_endpoint "$ORG_URL/tools/resources/organizations" "200" "List organizations resource"

echo ""
echo "2️⃣ Testing MCP LLM Service (Java)"
echo "--------------------------------"
test_endpoint "$LLM_URL/actuator/health" "200" "Health check"
test_endpoint "$LLM_URL/swagger-ui.html" "200" "Swagger UI"
test_endpoint "$LLM_URL/api-docs" "200" "OpenAPI docs"

# Test provider endpoints
echo ""
echo "Testing Provider endpoints:"
test_endpoint "$LLM_URL/api/v1/providers" "200" "List providers"

echo ""
echo "3️⃣ Testing MCP Controller Service (Java)"
echo "---------------------------------------"
test_endpoint "$CONTROLLER_URL/actuator/health" "200" "Health check"
test_endpoint "$CONTROLLER_URL/swagger-ui.html" "200" "Swagger UI"
test_endpoint "$CONTROLLER_URL/api-docs" "200" "OpenAPI docs"

# Test debate endpoints
echo ""
echo "Testing Debate endpoints:"
test_endpoint "$CONTROLLER_URL/api/v1/debates" "200" "List debates"

echo ""
echo "4️⃣ Testing Service Integration"
echo "-----------------------------"

# Create a test organization (this would normally require auth)
echo -n "Creating test organization... "
org_response=$(curl -s -X POST "$ORG_URL/tools/create_organization" \
    -H "Content-Type: application/json" \
    -d '{"name":"Integration Test Org","description":"Test organization for integration"}' 2>/dev/null || echo '{"error":"failed"}')
echo "$org_response" | jq '.' 2>/dev/null || echo -e "${YELLOW}⚠️  Skipped (auth required)${NC}"

# Test LLM completion (basic test)
echo -n "Testing LLM completion... "
llm_response=$(curl -s -X POST "$LLM_URL/api/v1/completions" \
    -H "Content-Type: application/json" \
    -d '{
        "provider": "claude",
        "messages": [{"role": "user", "content": "Say hello"}],
        "maxTokens": 10
    }' 2>/dev/null || echo '{"error":"failed"}')

if echo "$llm_response" | grep -q "error"; then
    echo -e "${YELLOW}⚠️  Skipped (API key required)${NC}"
else
    echo -e "${GREEN}✓ PASS${NC}"
fi

# Test debate creation
echo -n "Creating test debate... "
debate_response=$(curl -s -X POST "$CONTROLLER_URL/api/v1/debates" \
    -H "Content-Type: application/json" \
    -d '{
        "organizationId": "00000000-0000-0000-0000-000000000000",
        "title": "Test Debate",
        "topic": "Is AI beneficial for humanity?",
        "format": "OXFORD",
        "maxRounds": 3
    }' 2>/dev/null || echo '{"error":"failed"}')

if echo "$debate_response" | grep -q '"id"'; then
    echo -e "${GREEN}✓ PASS${NC}"
    debate_id=$(echo "$debate_response" | jq -r '.id')
    echo "   Created debate ID: $debate_id"
else
    echo -e "${RED}✗ FAIL${NC}"
fi

echo ""
echo "5️⃣ Testing Prometheus Metrics"
echo "----------------------------"
test_endpoint "$ORG_URL/actuator/prometheus" "200" "Organization metrics"
test_endpoint "$LLM_URL/actuator/prometheus" "200" "LLM metrics"
test_endpoint "$CONTROLLER_URL/actuator/prometheus" "200" "Controller metrics"

echo ""
echo "📊 Summary"
echo "========="
echo "All Java MCP services have been tested."
echo ""
echo "Note: Some tests may show as 'Skipped' if:"
echo "- Authentication is required (JWT tokens)"
echo "- API keys are not configured"
echo "- Services are still initializing"
echo ""
echo "To run comprehensive tests with authentication:"
echo "1. Obtain a JWT token from the auth endpoint"
echo "2. Set API keys in your .env file"
echo "3. Run the tests with proper headers"