#!/bin/bash

# Agentic Flows Integration Test Suite
# Tests integration with all system components

set -e

# Configuration
API_BASE_URL=${API_BASE_URL:-http://localhost:5013}
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-debate_db}
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Test results
PASSED=0
FAILED=0
SKIPPED=0

echo -e "${BLUE}Agentic Flows Integration Test Suite${NC}"
echo "======================================="

# Helper functions
log_test() {
    echo -e "\n${YELLOW}TEST: $1${NC}"
}

log_pass() {
    echo -e "${GREEN}✓ PASS: $1${NC}"
    ((PASSED++))
}

log_fail() {
    echo -e "${RED}✗ FAIL: $1${NC}"
    ((FAILED++))
}

log_skip() {
    echo -e "${YELLOW}⚠ SKIP: $1${NC}"
    ((SKIPPED++))
}

# Test database connectivity
test_database_connection() {
    log_test "Database Connection"
    
    if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1" > /dev/null 2>&1; then
        log_pass "Database connection successful"
        
        # Check tables exist
        TABLES=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('agentic_flows', 'flow_executions', 'flow_templates')")
        
        if [ "$TABLES" -eq 3 ]; then
            log_pass "All required tables exist"
        else
            log_fail "Missing required tables"
        fi
    else
        log_fail "Database connection failed"
    fi
}

# Test Redis connectivity
test_redis_connection() {
    log_test "Redis Connection"
    
    if redis-cli -h $REDIS_HOST -p $REDIS_PORT ping > /dev/null 2>&1; then
        log_pass "Redis connection successful"
        
        # Test cache operations
        redis-cli -h $REDIS_HOST -p $REDIS_PORT SET test:key "test_value" EX 10 > /dev/null
        VALUE=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT GET test:key)
        
        if [ "$VALUE" = "test_value" ]; then
            log_pass "Redis cache operations working"
        else
            log_fail "Redis cache operations failed"
        fi
    else
        log_fail "Redis connection failed"
    fi
}

# Test API health
test_api_health() {
    log_test "API Health Check"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" ${API_BASE_URL}/actuator/health)
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "API health endpoint responding"
        
        # Check component health
        if echo "$BODY" | grep -q '"status":"UP"'; then
            log_pass "API components healthy"
        else
            log_fail "API components unhealthy"
            echo "$BODY" | jq '.'
        fi
    else
        log_fail "API health check failed (HTTP $HTTP_CODE)"
    fi
}

# Test flow types endpoint
test_flow_types() {
    log_test "Flow Types Endpoint"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" ${API_BASE_URL}/api/v1/agentic-flows/types)
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "Flow types endpoint responding"
        
        # Check all flow types present
        EXPECTED_TYPES=("INTERNAL_MONOLOGUE" "SELF_CRITIQUE_LOOP" "MULTI_AGENT_RED_TEAM" "TOOL_CALLING_VERIFICATION" "RAG_WITH_RERANKING")
        
        for TYPE in "${EXPECTED_TYPES[@]}"; do
            if echo "$BODY" | grep -q "$TYPE"; then
                log_pass "Flow type $TYPE available"
            else
                log_fail "Flow type $TYPE missing"
            fi
        done
    else
        log_fail "Flow types endpoint failed (HTTP $HTTP_CODE)"
    fi
}

# Test flow creation
test_flow_creation() {
    log_test "Flow Creation"
    
    # Get auth token first
    AUTH_RESPONSE=$(curl -s -X POST ${API_BASE_URL}/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d '{"username":"demo","password":"demo123"}')
    
    TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.token')
    
    if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
        log_skip "Authentication failed - skipping flow creation test"
        return
    fi
    
    # Create test debate
    DEBATE_RESPONSE=$(curl -s -X POST ${API_BASE_URL}/api/v1/debates \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "topic": "Integration Test Debate",
            "format": "OXFORD",
            "rounds": 3
        }')
    
    DEBATE_ID=$(echo "$DEBATE_RESPONSE" | jq -r '.id')
    
    if [ -z "$DEBATE_ID" ] || [ "$DEBATE_ID" = "null" ]; then
        log_fail "Failed to create test debate"
        return
    fi
    
    # Create flow
    FLOW_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -X POST ${API_BASE_URL}/api/v1/debates/${DEBATE_ID}/agentic-flows \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Integration Test Flow",
            "flowType": "INTERNAL_MONOLOGUE",
            "description": "Test flow for integration testing",
            "configuration": {
                "prefix": "Test reasoning:",
                "temperature": 0.7
            }
        }')
    
    HTTP_CODE=$(echo "$FLOW_RESPONSE" | tail -1)
    BODY=$(echo "$FLOW_RESPONSE" | head -n -1)
    
    if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
        log_pass "Flow created successfully"
        
        FLOW_ID=$(echo "$BODY" | jq -r '.id')
        echo "Created flow: $FLOW_ID"
        
        # Test flow execution
        test_flow_execution "$TOKEN" "$FLOW_ID"
        
        # Cleanup
        curl -s -X DELETE ${API_BASE_URL}/api/v1/debates/${DEBATE_ID} \
            -H "Authorization: Bearer $TOKEN" > /dev/null
    else
        log_fail "Flow creation failed (HTTP $HTTP_CODE)"
        echo "$BODY"
    fi
}

# Test flow execution
test_flow_execution() {
    local TOKEN=$1
    local FLOW_ID=$2
    
    log_test "Flow Execution"
    
    EXEC_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -X POST ${API_BASE_URL}/api/v1/agentic-flows/${FLOW_ID}/execute \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "prompt": "What is the meaning of life?",
            "context": {
                "test": true
            }
        }')
    
    HTTP_CODE=$(echo "$EXEC_RESPONSE" | tail -1)
    BODY=$(echo "$EXEC_RESPONSE" | head -n -1)
    
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "Flow execution successful"
        
        # Check result structure
        if echo "$BODY" | jq -e '.finalAnswer' > /dev/null && \
           echo "$BODY" | jq -e '.confidence' > /dev/null && \
           echo "$BODY" | jq -e '.status' > /dev/null; then
            log_pass "Flow result structure valid"
            
            CONFIDENCE=$(echo "$BODY" | jq -r '.confidence')
            echo "Confidence: $CONFIDENCE%"
        else
            log_fail "Flow result structure invalid"
        fi
    else
        log_fail "Flow execution failed (HTTP $HTTP_CODE)"
    fi
}

# Test caching
test_caching() {
    log_test "Caching Functionality"
    
    # Create two identical requests and measure time
    TOKEN=$(curl -s -X POST ${API_BASE_URL}/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d '{"username":"demo","password":"demo123"}' | jq -r '.token')
    
    if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
        log_skip "Authentication failed - skipping cache test"
        return
    fi
    
    # First request (cache miss)
    START1=$(date +%s%N)
    RESPONSE1=$(curl -s ${API_BASE_URL}/api/v1/agentic-flows/cache-test \
        -H "Authorization: Bearer $TOKEN")
    END1=$(date +%s%N)
    TIME1=$(( (END1 - START1) / 1000000 ))
    
    # Second request (cache hit)
    START2=$(date +%s%N)
    RESPONSE2=$(curl -s ${API_BASE_URL}/api/v1/agentic-flows/cache-test \
        -H "Authorization: Bearer $TOKEN")
    END2=$(date +%s%N)
    TIME2=$(( (END2 - START2) / 1000000 ))
    
    echo "First request: ${TIME1}ms"
    echo "Second request: ${TIME2}ms"
    
    if [ "$TIME2" -lt "$TIME1" ]; then
        log_pass "Cache appears to be working (${TIME2}ms < ${TIME1}ms)"
    else
        log_fail "Cache not working effectively"
    fi
}

# Test rate limiting
test_rate_limiting() {
    log_test "Rate Limiting"
    
    TOKEN=$(curl -s -X POST ${API_BASE_URL}/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d '{"username":"demo","password":"demo123"}' | jq -r '.token')
    
    if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
        log_skip "Authentication failed - skipping rate limit test"
        return
    fi
    
    # Make rapid requests
    RATE_LIMITED=false
    for i in {1..150}; do
        RESPONSE=$(curl -s -w "\n%{http_code}" ${API_BASE_URL}/api/v1/agentic-flows/types \
            -H "Authorization: Bearer $TOKEN")
        HTTP_CODE=$(echo "$RESPONSE" | tail -1)
        
        if [ "$HTTP_CODE" = "429" ]; then
            RATE_LIMITED=true
            log_pass "Rate limiting triggered after $i requests"
            break
        fi
    done
    
    if [ "$RATE_LIMITED" = false ]; then
        log_fail "Rate limiting not triggered after 150 requests"
    fi
}

# Test metrics endpoint
test_metrics() {
    log_test "Metrics Collection"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" ${API_BASE_URL}/actuator/prometheus)
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "Metrics endpoint responding"
        
        # Check for agentic flow metrics
        if echo "$BODY" | grep -q "agentic_flow_execution"; then
            log_pass "Agentic flow metrics being collected"
        else
            log_fail "Agentic flow metrics not found"
        fi
    else
        log_fail "Metrics endpoint failed (HTTP $HTTP_CODE)"
    fi
}

# Test database migrations
test_database_migrations() {
    log_test "Database Migrations"
    
    # Check migration history
    MIGRATIONS=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM flyway_schema_history WHERE script LIKE '%agentic_flow%'" 2>/dev/null || echo "0")
    
    if [ "$MIGRATIONS" -gt 0 ]; then
        log_pass "Found $MIGRATIONS agentic flow migrations"
        
        # Check latest migration
        LATEST=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT script FROM flyway_schema_history WHERE script LIKE '%agentic_flow%' ORDER BY installed_rank DESC LIMIT 1" 2>/dev/null)
        echo "Latest migration: $LATEST"
    else
        log_fail "No agentic flow migrations found"
    fi
}

# Run all tests
run_all_tests() {
    echo "Starting integration tests..."
    echo "API URL: $API_BASE_URL"
    echo ""
    
    test_database_connection
    test_redis_connection
    test_api_health
    test_flow_types
    test_flow_creation
    test_caching
    test_rate_limiting
    test_metrics
    test_database_migrations
    
    echo -e "\n${BLUE}Test Summary${NC}"
    echo "============"
    echo -e "${GREEN}Passed: $PASSED${NC}"
    echo -e "${RED}Failed: $FAILED${NC}"
    echo -e "${YELLOW}Skipped: $SKIPPED${NC}"
    
    if [ $FAILED -eq 0 ]; then
        echo -e "\n${GREEN}All tests passed!${NC}"
        exit 0
    else
        echo -e "\n${RED}Some tests failed!${NC}"
        exit 1
    fi
}

# Run tests
run_all_tests