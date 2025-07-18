#!/bin/bash
# API Testing Script with curl
# Tests all endpoints and collects evidence

set -euo pipefail

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
BASE_URL="http://localhost:8080"
EVIDENCE_DIR="test-evidence/api"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Create evidence directory
mkdir -p """$EVIDENCE_DIR"""

# Test results file
RESULTS_FILE="""$EVIDENCE_DIR""/api_test_results_""$TIMESTAMP"".txt"

# Helper function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local expected_status=$4
    local test_name=$5
    
    echo -e "\n${YELLOW}Testing: ""$test_name""${NC}" | tee -a """$RESULTS_FILE"""
    echo "Method: ""$method""" | tee -a """$RESULTS_FILE"""
    echo "Endpoint: ""$endpoint""" | tee -a """$RESULTS_FILE"""
    
    # Prepare curl command
    if [ """$method""" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET \
            -H "Content-Type: application/json" \
            """$BASE_URL"""$endpoint"" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X """$method""" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer test-token" \
            -d """$data""" \
            """$BASE_URL"""$endpoint"" 2>&1)
    fi
    
    # Extract status code and body
    http_code=$(echo """$response""" | tail -n1)
    body=$(echo """$response""" | sed '""$d""')
    
    # Save response
    echo "Response Status: ""$http_code""" | tee -a """$RESULTS_FILE"""
    echo "Response Body: ""$body""" | tee -a """$RESULTS_FILE"""
    
    # Check result
    if [ """$http_code""" = """$expected_status""" ]; then
        echo -e "${GREEN}✅ PASS${NC}" | tee -a """$RESULTS_FILE"""
        return 0
    else
        echo -e "${RED}❌ FAIL (expected ""$expected_status"", got ""$http_code"")${NC}" | tee -a """$RESULTS_FILE"""
        return 1
    fi
}

echo "Starting API Tests at ""$TIMESTAMP""" | tee """$RESULTS_FILE"""
echo "Base URL: ""$BASE_URL""" | tee -a """$RESULTS_FILE"""

# Test 1: Health Check
test_endpoint "GET" "/health" "" "200" "Health Check"

# Test 2: Metrics Endpoint
test_endpoint "GET" "/metrics" "" "200" "Metrics Endpoint"

# Test 3: GitHub Webhook - Valid
WEBHOOK_DATA='{
  "action": "opened",
  "pull_request": {
    "number": 123,
    "title": "Test PR",
    "user": {"login": "test-user"},
    "requested_reviewers": [{"login": "kiro-ai"}]
  },
  "repository": {
    "name": "test-repo",
    "owner": {"login": "test-org"}
  }
}'
test_endpoint "POST" "/webhooks/github" """$WEBHOOK_DATA""" "200" "Valid GitHub Webhook"

# Test 4: GitHub Webhook - Invalid Signature
test_endpoint "POST" "/webhooks/github" """$WEBHOOK_DATA""" "401" "Invalid Webhook Signature"

# Test 5: Trigger Review API
REVIEW_DATA='{
  "repo_owner": "test-org",
  "repo_name": "test-repo",
  "pr_number": 123
}'
test_endpoint "POST" "/api/reviews/trigger" """$REVIEW_DATA""" "200" "Trigger Review"

# Test 6: Get Review Status
test_endpoint "GET" "/api/reviews/status/123" "" "200" "Get Review Status"

# Test 7: Process Queue Status
test_endpoint "GET" "/api/queue/status" "" "200" "Queue Status"

# Test 8: GitHub Rate Limit
test_endpoint "GET" "/api/github/rate-limit" "" "200" "GitHub Rate Limit"

# Test 9: Create Spec from Issue
SPEC_DATA='{
  "repo_owner": "test-org",
  "repo_name": "test-repo",
  "issue_number": 456,
  "spec_type": "feature"
}'
test_endpoint "POST" "/api/specs/create" """$SPEC_DATA""" "200" "Create Spec from Issue"

# Test 10: Webhook Event Types
test_endpoint "GET" "/api/webhooks/events" "" "200" "List Webhook Events"

# Summary
echo -e "\n${YELLOW}=== TEST SUMMARY ===${NC}" | tee -a """$RESULTS_FILE"""
echo "Test results saved to: ""$RESULTS_FILE"""

# Additional curl examples for manual testing
cat > """$EVIDENCE_DIR""/curl_examples_""$TIMESTAMP"".sh" << 'EOF'
#!/bin/bash
# Manual curl testing examples

# 1. Test webhook with custom headers
curl -X POST http://localhost:8080/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: pull_request" \
  -H "X-GitHub-Delivery: test-delivery-id" \
  -H "X-Hub-Signature-256: sha256=test-signature" \
  -d @webhook_payload.json

# 2. Test with verbose output
curl -v -X GET http://localhost:8080/health

# 3. Test with response time
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8080/health

# 4. Test concurrent requests
for i in {1..10}; do
  curl -s http://localhost:8080/health &
done
wait

# 5. Test with authentication
curl -X POST http://localhost:8080/api/reviews/trigger \
  -H "Authorization: Bearer ""$GITHUB_TOKEN""" \
  -H "Content-Type: application/json" \
  -d '{"repo_owner":"org","repo_name":"repo","pr_number":123}'

# 6. Test file upload (for future features)
curl -X POST http://localhost:8080/api/analyze/file \
  -F "file=@example.py" \
  -F "language=python"

# 7. Test streaming endpoint
curl -N http://localhost:8080/api/events/stream

# 8. Test with timeout
curl --max-time 5 http://localhost:8080/api/reviews/status/123

# 9. Test error handling
curl -X POST http://localhost:8080/api/reviews/trigger \
  -H "Content-Type: application/json" \
  -d '{"invalid":"data"}'

# 10. Test rate limiting
for i in {1..100}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/github/rate-limit
done | sort | uniq -c
EOF

chmod +x """$EVIDENCE_DIR""/curl_examples_""$TIMESTAMP"".sh"

# Create curl format file for timing
cat > curl-format.txt << 'EOF'
    time_namelookup:  %{time_namelookup}s\n
       time_connect:  %{time_connect}s\n
    time_appconnect:  %{time_appconnect}s\n
   time_pretransfer:  %{time_pretransfer}s\n
      time_redirect:  %{time_redirect}s\n
 time_starttransfer:  %{time_starttransfer}s\n
                    ----------\n
         time_total:  %{time_total}s\n
EOF

echo -e "\n${GREEN}API tests completed!${NC}"
echo "Evidence saved in: ""$EVIDENCE_DIR"""