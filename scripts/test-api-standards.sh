#!/bin/bash

# Test API Standards Script
# This script validates that all services follow the new API standards

set -e

echo "🔍 Testing API Standards Implementation..."
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Function to log test results
log_test() {
    local test_name="$1"
    local status="$2"
    local message="$3"
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓ PASS${NC} $test_name: $message"
        ((PASSED++))
    elif [ "$status" = "FAIL" ]; then
        echo -e "${RED}✗ FAIL${NC} $test_name: $message"
        ((FAILED++))
    else
        echo -e "${YELLOW}⚠ WARN${NC} $test_name: $message"
        ((WARNINGS++))
    fi
}

# Function to test API endpoint
test_api_endpoint() {
    local service="$1"
    local port="$2"
    local endpoint="$3"
    local expected_status="$4"
    
    local url="http://localhost:$port$endpoint"
    
    if curl -s -f -o /dev/null -w "%{http_code}" "$url" >/dev/null 2>&1; then
        local status_code=$(curl -s -o /dev/null -w "%{http_code}" "$url")
        if [ "$status_code" = "$expected_status" ]; then
            log_test "API Endpoint" "PASS" "$service endpoint $endpoint returns $status_code"
            return 0
        else
            log_test "API Endpoint" "FAIL" "$service endpoint $endpoint returns $status_code (expected $expected_status)"
            return 1
        fi
    else
        log_test "API Endpoint" "FAIL" "$service endpoint $endpoint is not accessible"
        return 1
    fi
}

# Function to test API response format
test_api_response_format() {
    local service="$1"
    local port="$2"
    local endpoint="$3"
    
    local url="http://localhost:$port$endpoint"
    local response=$(curl -s "$url" 2>/dev/null || echo '{}')
    
    # Check if response has standard fields
    if echo "$response" | jq -e '.timestamp' >/dev/null 2>&1; then
        log_test "API Response Format" "PASS" "$service response has timestamp field"
    else
        log_test "API Response Format" "FAIL" "$service response missing timestamp field"
    fi
    
    if echo "$response" | jq -e '.message' >/dev/null 2>&1; then
        log_test "API Response Format" "PASS" "$service response has message field"
    else
        log_test "API Response Format" "WARN" "$service response missing message field"
    fi
}

# Function to test OpenAPI documentation
test_openapi_docs() {
    local service="$1"
    local port="$2"
    
    # Test OpenAPI JSON endpoint
    local api_docs_url="http://localhost:$port/api-docs"
    if curl -s -f "$api_docs_url" >/dev/null 2>&1; then
        log_test "OpenAPI Docs" "PASS" "$service OpenAPI docs accessible at $api_docs_url"
        
        # Check OpenAPI structure
        local openapi_content=$(curl -s "$api_docs_url" 2>/dev/null || echo '{}')
        if echo "$openapi_content" | jq -e '.openapi' >/dev/null 2>&1; then
            log_test "OpenAPI Structure" "PASS" "$service OpenAPI has version field"
        else
            log_test "OpenAPI Structure" "FAIL" "$service OpenAPI missing version field"
        fi
        
        if echo "$openapi_content" | jq -e '.info' >/dev/null 2>&1; then
            log_test "OpenAPI Structure" "PASS" "$service OpenAPI has info section"
        else
            log_test "OpenAPI Structure" "FAIL" "$service OpenAPI missing info section"
        fi
    else
        log_test "OpenAPI Docs" "FAIL" "$service OpenAPI docs not accessible"
    fi
    
    # Test Swagger UI
    local swagger_url="http://localhost:$port/swagger-ui.html"
    if curl -s -f "$swagger_url" >/dev/null 2>&1; then
        log_test "Swagger UI" "PASS" "$service Swagger UI accessible at $swagger_url"
    else
        log_test "Swagger UI" "FAIL" "$service Swagger UI not accessible"
    fi
}

# Function to test service health
test_service_health() {
    local service="$1"
    local port="$2"
    
    local health_url="http://localhost:$port/actuator/health"
    if curl -s -f "$health_url" >/dev/null 2>&1; then
        local health_response=$(curl -s "$health_url" 2>/dev/null || echo '{}')
        local status=$(echo "$health_response" | jq -r '.status' 2>/dev/null || echo 'UNKNOWN')
        
        if [ "$status" = "UP" ]; then
            log_test "Service Health" "PASS" "$service is healthy (UP)"
        else
            log_test "Service Health" "FAIL" "$service health status: $status"
        fi
    else
        log_test "Service Health" "FAIL" "$service health endpoint not accessible"
    fi
}

# Load environment variables
if [ -f ".env" ]; then
    export $(cat .env | grep -v '^#' | grep -v '^$' | grep '=' | xargs)
fi

# Services to test
declare -A services=(
    ["mcp-organization"]="5005"
    ["mcp-llm"]="5002"
    ["mcp-controller"]="5013"
    ["mcp-rag"]="5004"
    ["mcp-gateway"]="8080"
)

echo ""
echo "🏥 1. Service Health Tests"
echo "-------------------------"

for service in "${!services[@]}"; do
    port="${services[$service]}"
    echo "Testing $service on port $port..."
    test_service_health "$service" "$port"
done

echo ""
echo "📚 2. OpenAPI Documentation Tests"
echo "--------------------------------"

for service in "${!services[@]}"; do
    port="${services[$service]}"
    if [ "$service" != "mcp-gateway" ]; then  # Gateway might not have OpenAPI docs
        echo "Testing $service OpenAPI documentation..."
        test_openapi_docs "$service" "$port"
    fi
done

echo ""
echo "🎯 3. API Endpoint Tests"
echo "-----------------------"

# Test common endpoints
test_api_endpoint "mcp-organization" "5005" "/actuator/health" "200"
test_api_endpoint "mcp-llm" "5002" "/actuator/health" "200"
test_api_endpoint "mcp-controller" "5013" "/actuator/health" "200"
test_api_endpoint "mcp-rag" "5004" "/actuator/health" "200"
test_api_endpoint "mcp-gateway" "8080" "/actuator/health" "200"

# Test API versioning endpoints (if services are running)
if curl -s -f "http://localhost:5005/actuator/health" >/dev/null 2>&1; then
    log_test "API Versioning" "PASS" "Organization service is running for API tests"
    
    # Test versioned API endpoints
    test_api_endpoint "mcp-organization" "5005" "/api/v1/organizations" "200"
    test_api_endpoint "mcp-llm" "5002" "/api/v1/providers" "200"
    test_api_endpoint "mcp-controller" "5013" "/api/v1/debates" "200"
else
    log_test "API Versioning" "WARN" "Services not running - skipping API endpoint tests"
fi

echo ""
echo "📊 4. Response Format Tests"
echo "-------------------------"

# Test response formats (if services are running)
if curl -s -f "http://localhost:5005/actuator/health" >/dev/null 2>&1; then
    for service in "${!services[@]}"; do
        port="${services[$service]}"
        if [ "$service" != "mcp-gateway" ]; then
            echo "Testing $service response format..."
            test_api_response_format "$service" "$port" "/actuator/health"
        fi
    done
else
    log_test "Response Format" "WARN" "Services not running - skipping response format tests"
fi

echo ""
echo "🔧 5. Configuration Tests"
echo "------------------------"

# Test environment variables
if [ -z "$ORGANIZATION_SERVICE_URL" ]; then
    log_test "Environment Config" "FAIL" "ORGANIZATION_SERVICE_URL not set"
else
    log_test "Environment Config" "PASS" "ORGANIZATION_SERVICE_URL is set"
fi

if [ -z "$LLM_SERVICE_URL" ]; then
    log_test "Environment Config" "FAIL" "LLM_SERVICE_URL not set"
else
    log_test "Environment Config" "PASS" "LLM_SERVICE_URL is set"
fi

if [ -z "$CONTROLLER_SERVICE_URL" ]; then
    log_test "Environment Config" "FAIL" "CONTROLLER_SERVICE_URL not set"
else
    log_test "Environment Config" "PASS" "CONTROLLER_SERVICE_URL is set"
fi

echo ""
echo "🧪 6. Frontend API Client Tests"
echo "------------------------------"

# Test frontend API client configuration
if [ -f "debate-ui/src/api/debateClient.ts" ]; then
    if grep -q "/api/v1" "debate-ui/src/api/debateClient.ts"; then
        log_test "Frontend API Client" "PASS" "Debate client uses API v1 versioning"
    else
        log_test "Frontend API Client" "FAIL" "Debate client missing API v1 versioning"
    fi
    
    if grep -q "BaseApiClient" "debate-ui/src/api/debateClient.ts"; then
        log_test "Frontend API Client" "PASS" "Debate client extends BaseApiClient"
    else
        log_test "Frontend API Client" "FAIL" "Debate client not extending BaseApiClient"
    fi
else
    log_test "Frontend API Client" "FAIL" "Debate client file not found"
fi

# Test frontend environment configuration
if [ -f "debate-ui/.env" ]; then
    log_test "Frontend Config" "PASS" "Frontend environment file exists"
    
    if grep -q "VITE_ORGANIZATION_API_URL" "debate-ui/.env"; then
        log_test "Frontend Config" "PASS" "Frontend has organization API URL"
    else
        log_test "Frontend Config" "FAIL" "Frontend missing organization API URL"
    fi
else
    log_test "Frontend Config" "FAIL" "Frontend environment file missing"
fi

echo ""
echo "🔗 7. Integration Tests"
echo "----------------------"

# Test if services can communicate (if running)
if curl -s -f "http://localhost:5005/actuator/health" >/dev/null 2>&1 && \
   curl -s -f "http://localhost:5002/actuator/health" >/dev/null 2>&1; then
    log_test "Service Integration" "PASS" "Multiple services are running"
    
    # Test gateway routing (if gateway is running)
    if curl -s -f "http://localhost:8080/actuator/health" >/dev/null 2>&1; then
        log_test "Gateway Integration" "PASS" "Gateway service is running"
    else
        log_test "Gateway Integration" "WARN" "Gateway service not running"
    fi
else
    log_test "Service Integration" "WARN" "Services not running - skipping integration tests"
fi

echo ""
echo "======================================================"
echo "📊 API STANDARDS TEST SUMMARY"
echo "======================================================"
echo -e "${GREEN}✓ Passed: $PASSED${NC}"
echo -e "${RED}✗ Failed: $FAILED${NC}"
echo -e "${YELLOW}⚠ Warnings: $WARNINGS${NC}"

total=$((PASSED + FAILED + WARNINGS))
if [ $total -gt 0 ]; then
    pass_rate=$((PASSED * 100 / total))
    echo -e "📈 Pass Rate: ${GREEN}$pass_rate%${NC}"
else
    echo -e "📈 Pass Rate: ${GREEN}100%${NC}"
fi

echo ""
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL CRITICAL API TESTS PASSED!${NC}"
    echo -e "${GREEN}✅ API standards implementation is working${NC}"
    
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠️  $WARNINGS warnings found - some services may not be running${NC}"
    fi
    
    echo ""
    echo "🚀 API Endpoints Available:"
    echo "• Organization API: http://localhost:5005/api/v1/organizations"
    echo "• LLM API: http://localhost:5002/api/v1/providers"
    echo "• Debate API: http://localhost:5013/api/v1/debates"
    echo "• RAG API: http://localhost:5004/api/v1/documents"
    echo "• Gateway API: http://localhost:8080"
    echo ""
    echo "📖 Documentation Available:"
    echo "• Organization: http://localhost:5005/swagger-ui.html"
    echo "• LLM: http://localhost:5002/swagger-ui.html"
    echo "• Debate: http://localhost:5013/swagger-ui.html"
    echo "• RAG: http://localhost:5004/swagger-ui.html"
    
    exit 0
else
    echo -e "${RED}❌ $FAILED API TESTS FAILED!${NC}"
    echo -e "${RED}🔧 API standards implementation needs attention${NC}"
    
    echo ""
    echo "🔍 Recommended Actions:"
    echo "1. Start services: make dev"
    echo "2. Check service logs: make logs"
    echo "3. Verify configuration: ./scripts/validate-configuration.sh"
    echo "4. Re-run API tests: ./scripts/test-api-standards.sh"
    
    exit 1
fi