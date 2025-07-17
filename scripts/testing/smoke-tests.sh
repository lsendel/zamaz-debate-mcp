#!/bin/bash

# Smoke tests for deployment verification
# Usage: ./smoke-tests.sh [staging|production]

set -e

ENVIRONMENT=${1:-staging}

# Configuration based on environment
case $ENVIRONMENT in
    "staging")
        BASE_URL="https://staging.zamaz-debate.com"
        ;;
    "production")
        BASE_URL="https://zamaz-debate.com"
        ;;
    *)
        echo "Usage: $0 [staging|production]"
        exit 1
        ;;
esac

echo "Running smoke tests for $ENVIRONMENT environment..."
echo "Base URL: $BASE_URL"

# Test results
PASSED=0
FAILED=0
TESTS=()

# Function to run a test
run_test() {
    local test_name="$1"
    local test_command="$2"
    local expected_status="${3:-200}"
    
    echo -n "Testing $test_name... "
    
    if eval "$test_command"; then
        echo "✅ PASSED"
        PASSED=$((PASSED + 1))
        TESTS+=("$test_name: PASSED")
    else
        echo "❌ FAILED"
        FAILED=$((FAILED + 1))
        TESTS+=("$test_name: FAILED")
    fi
}

# Function to check HTTP status
check_http_status() {
    local url="$1"
    local expected_status="${2:-200}"
    local timeout="${3:-10}"
    
    local actual_status=$(curl -s -o /dev/null -w "%{http_code}" --max-time $timeout "$url" || echo "000")
    
    if [[ "$actual_status" == "$expected_status" ]]; then
        return 0
    else
        echo " (Expected: $expected_status, Got: $actual_status)"
        return 1
    fi
}

# Function to check JSON response
check_json_response() {
    local url="$1"
    local expected_field="$2"
    local timeout="${3:-10}"
    
    local response=$(curl -s --max-time $timeout "$url" || echo "{}")
    
    if echo "$response" | jq -e "$expected_field" > /dev/null 2>&1; then
        return 0
    else
        echo " (Field '$expected_field' not found in response)"
        return 1
    fi
}

echo "Starting smoke tests..."
echo "==========================================="

# Test 1: Gateway Health Check
run_test "Gateway Health Check" \
    "check_http_status '$BASE_URL/actuator/health' 200"

# Test 2: Organization Service Health
run_test "Organization Service Health" \
    "check_json_response '$BASE_URL/api/v1/organizations/health' '.status == \"UP\"'"

# Test 3: Controller Service Health
run_test "Controller Service Health" \
    "check_json_response '$BASE_URL/api/v1/debates/health' '.status == \"UP\"'"

# Test 4: Frontend Loading
run_test "Frontend Loading" \
    "check_http_status '$BASE_URL' 200"

# Test 5: API Gateway CORS
run_test "API Gateway CORS" \
    "curl -s -H 'Origin: https://app.zamaz-debate.com' -H 'Access-Control-Request-Method: GET' -X OPTIONS '$BASE_URL/api/v1/organizations' | grep -q 'Access-Control-Allow-Origin'"

# Test 6: Authentication Endpoint
run_test "Authentication Endpoint" \
    "check_http_status '$BASE_URL/api/v1/auth/login' 405"  # POST required

# Test 7: Database Connectivity (via API)
run_test "Database Connectivity" \
    "check_http_status '$BASE_URL/api/v1/organizations' 401"  # Unauthorized but connected

# Test 8: Redis Connectivity (via health endpoint)
run_test "Redis Connectivity" \
    "check_json_response '$BASE_URL/actuator/health' '.components.redis.status == \"UP\"'"

# Test 9: Metrics Endpoint
run_test "Metrics Endpoint" \
    "check_http_status '$BASE_URL/actuator/prometheus' 200"

# Test 10: WebSocket Endpoint (basic connectivity)
run_test "WebSocket Endpoint" \
    "check_http_status '$BASE_URL/api/v1/debates/test/ws' 400"  # Bad request but endpoint exists

if [[ $ENVIRONMENT == "production" ]]; then
    # Additional production-specific tests
    
    # Test 11: SSL Certificate
    run_test "SSL Certificate" \
        "curl -s --max-time 10 '$BASE_URL' | grep -q 'Zamaz'"
    
    # Test 12: Security Headers
    run_test "Security Headers" \
        "curl -s -I '$BASE_URL' | grep -q 'X-Frame-Options'"
    
    # Test 13: Rate Limiting
    run_test "Rate Limiting Headers" \
        "curl -s -I '$BASE_URL/api/v1/organizations' | grep -q 'X-RateLimit'"
fi

echo "==========================================="
echo "Smoke test results:"
echo "✅ Passed: $PASSED"
echo "❌ Failed: $FAILED"
echo ""

# Print detailed results
for test_result in "${TESTS[@]}"; do
    echo "  $test_result"
done

echo ""

if [[ $FAILED -eq 0 ]]; then
    echo "🎉 All smoke tests passed! Deployment looks good."
    exit 0
else
    echo "⚠️  Some smoke tests failed. Please investigate."
    exit 1
fi