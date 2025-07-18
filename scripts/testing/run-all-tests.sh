#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results directory
TEST_RESULTS_DIR="/test_probe"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_DIR="${TEST_RESULTS_DIR}/run_${TIMESTAMP}"

# Create results directories
mkdir -p "${RESULTS_DIR}/screenshots"
mkdir -p "${RESULTS_DIR}/videos"
mkdir -p "${RESULTS_DIR}/logs"
mkdir -p "${RESULTS_DIR}/reports"

# Log file
LOG_FILE="${RESULTS_DIR}/logs/test_run.log"

# Function to log messages
log() {
    echo -e "${1}" | tee -a "${LOG_FILE}"
}

# Function to check service health
check_service() {
    local service_name=$1
    local service_url=$2
    local max_retries=30
    local retry_count=0
    
    log "${BLUE}Checking ${service_name} at ${service_url}...${NC}"
    
    while [ ""$retry_count"" -lt ""$max_retries"" ]; do
        if curl -s -f "${service_url}/health" > /dev/null 2>&1; then
            log "${GREEN}✓ ${service_name} is healthy${NC}"
            return 0
        fi
        
        retry_count=$((retry_count + 1))
        log "${YELLOW}Waiting for ${service_name}... (${retry_count}/${max_retries})${NC}"
        sleep 2
    done
    
    log "${RED}✗ ${service_name} failed to become healthy${NC}"
    return 1
}

# Function to run tests with retry logic
run_test_suite() {
    local test_name=$1
    local test_command=$2
    local max_retries=3
    local retry_count=0
    
    log "\n${BLUE}=== Running ${test_name} ===${NC}"
    
    while [ ""$retry_count"" -lt ""$max_retries"" ]; do
        log "Attempt $((retry_count + 1)) of ${max_retries}"
        
        # Run the test command and capture output
        if eval "${test_command}" >> "${RESULTS_DIR}/logs/${test_name}.log" 2>&1; then
            log "${GREEN}✓ ${test_name} passed${NC}"
            return 0
        else
            retry_count=$((retry_count + 1))
            if [ ""$retry_count"" -lt ""$max_retries"" ]; then
                log "${YELLOW}⚠ ${test_name} failed, retrying...${NC}"
                sleep 5
            fi
        fi
    done
    
    log "${RED}✗ ${test_name} failed after ${max_retries} attempts${NC}"
    return 1
}

# Start logging
log "${BLUE}=== AI Debate System Test Runner ===${NC}"
log "Timestamp: ${TIMESTAMP}"
log "Results directory: ${RESULTS_DIR}"

# Check all services are healthy
log "\n${BLUE}=== Checking Service Health ===${NC}"

SERVICES_HEALTHY=true

check_service "Debate UI" "http://host.docker.internal:3000" || SERVICES_HEALTHY=false
check_service "LLM Service" "http://host.docker.internal:5002" || SERVICES_HEALTHY=false
check_service "Debate Service" "http://host.docker.internal:5013" || SERVICES_HEALTHY=false

if [ """$SERVICES_HEALTHY""" = false ]; then
    log "${RED}Some services are not healthy. Exiting...${NC}"
    exit 1
fi

# Configure test environments
export SCREENSHOT_DIR="${RESULTS_DIR}/screenshots"
export VIDEO_DIR="${RESULTS_DIR}/videos"
export REPORT_DIR="${RESULTS_DIR}/reports"

# Configure timeouts for stability
export PUPPETEER_TIMEOUT=60000
export PLAYWRIGHT_TIMEOUT=60000
export JEST_TIMEOUT=120000

# Track test results
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
TEST_RESULTS=()

# Run Puppeteer E2E Tests
log "\n${BLUE}=== Running Puppeteer E2E Tests ===${NC}"
cd /tests/e2e-tests

# Configure Puppeteer for Docker
export PUPPETEER_ARGS='--no-sandbox --disable-setuid-sandbox --disable-dev-shm-usage'

# Run each test file individually for better error tracking
for test_file in src/tests/*.test.ts; do
    if [ -f """$test_file""" ]; then
        test_name=$(basename """$test_file""" .test.ts)
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
        
        if run_test_suite "puppeteer_${test_name}" "npm test -- ${test_file} --forceExit"; then
            PASSED_TESTS=$((PASSED_TESTS + 1))
            TEST_RESULTS+=("${GREEN}✓ Puppeteer: ${test_name}${NC}")
        else
            FAILED_TESTS=$((FAILED_TESTS + 1))
            TEST_RESULTS+=("${RED}✗ Puppeteer: ${test_name}${NC}")
            
            # Copy any screenshots on failure
            if [ -d "screenshots" ]; then
                cp -r screenshots/* "${SCREENSHOT_DIR}/" 2>/dev/null || true
            fi
        fi
    fi
done

# Run Playwright Tests
log "\n${BLUE}=== Running Playwright Tests ===${NC}"
cd /tests/playwright-tests

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    npm install
fi

# Install browsers if needed
npx playwright install chromium

# Configure test environment
export BASE_URL="http://host.docker.internal:3000"
export TEST_PROBE_DIR="${RESULTS_DIR}"

# Run smoke tests first for quick validation
log "\n${BLUE}Running smoke tests...${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
if run_test_suite "playwright_smoke" "npx playwright test tests/smoke --project=chromium"; then
    PASSED_TESTS=$((PASSED_TESTS + 1))
    TEST_RESULTS+=("${GREEN}✓ Playwright: Smoke Tests${NC}")
else
    FAILED_TESTS=$((FAILED_TESTS + 1))
    TEST_RESULTS+=("${RED}✗ Playwright: Smoke Tests${NC}")
    log "${YELLOW}Smoke tests failed, but continuing with other tests...${NC}"
fi

# Run comprehensive debate tests
log "\n${BLUE}Running comprehensive debate tests...${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
if run_test_suite "playwright_comprehensive" "npx playwright test --project=comprehensive-debate-tests"; then
    PASSED_TESTS=$((PASSED_TESTS + 1))
    TEST_RESULTS+=("${GREEN}✓ Playwright: Comprehensive Debate Tests${NC}")
else
    FAILED_TESTS=$((FAILED_TESTS + 1))
    TEST_RESULTS+=("${RED}✗ Playwright: Comprehensive Debate Tests${NC}")
fi

# Run UI component tests
log "\n${BLUE}Running UI component tests...${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
if run_test_suite "playwright_ui" "npx playwright test --project=ui-components"; then
    PASSED_TESTS=$((PASSED_TESTS + 1))
    TEST_RESULTS+=("${GREEN}✓ Playwright: UI Component Tests${NC}")
else
    FAILED_TESTS=$((FAILED_TESTS + 1))
    TEST_RESULTS+=("${RED}✗ Playwright: UI Component Tests${NC}")
fi

# Run LLM integration tests
log "\n${BLUE}Running LLM integration tests...${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
if run_test_suite "playwright_llm" "npx playwright test --project=llm-integration"; then
    PASSED_TESTS=$((PASSED_TESTS + 1))
    TEST_RESULTS+=("${GREEN}✓ Playwright: LLM Integration Tests${NC}")
else
    FAILED_TESTS=$((FAILED_TESTS + 1))
    TEST_RESULTS+=("${RED}✗ Playwright: LLM Integration Tests${NC}")
fi

# Run database verification tests
log "\n${BLUE}Running database verification tests...${NC}"
TOTAL_TESTS=$((TOTAL_TESTS + 1))
if run_test_suite "playwright_db" "npx playwright test --project=database-verification"; then
    PASSED_TESTS=$((PASSED_TESTS + 1))
    TEST_RESULTS+=("${GREEN}✓ Playwright: Database Verification Tests${NC}")
else
    FAILED_TESTS=$((FAILED_TESTS + 1))
    TEST_RESULTS+=("${RED}✗ Playwright: Database Verification Tests${NC}")
fi

# Copy test evidence to results directory
if [ -d "../test_probe/evidence" ]; then
    cp -r ../test_probe/evidence/* "${RESULTS_DIR}/" 2>/dev/null || true
fi

# Generate summary report
log "\n${BLUE}=== Test Summary ===${NC}"
log "Total tests: ${TOTAL_TESTS}"
log "${GREEN}Passed: ${PASSED_TESTS}${NC}"
log "${RED}Failed: ${FAILED_TESTS}${NC}"

log "\n${BLUE}=== Detailed Results ===${NC}"
for result in "${TEST_RESULTS[@]}"; do
    log """$result"""
done

# Create summary JSON
cat > "${RESULTS_DIR}/summary.json" << EOF
{
  "timestamp": "${TIMESTAMP}",
  "total_tests": ${TOTAL_TESTS},
  "passed": ${PASSED_TESTS},
  "failed": ${FAILED_TESTS},
  "success_rate": $(awk "BEGIN {printf \"%.2f\", ${PASSED_TESTS}/${TOTAL_TESTS}*100}")
}
EOF

# Clean up test artifacts and move to results directory
log "\n${BLUE}=== Collecting Test Artifacts ===${NC}"

# Collect Playwright artifacts
if [ -d "/tests/playwright-tests/test-results" ]; then
    cp -r /tests/playwright-tests/test-results/* "${RESULTS_DIR}/playwright-test-results/" 2>/dev/null || true
fi

# Collect screenshots and videos
find /tests -name "*.png" -o -name "*.jpg" -o -name "*.jpeg" 2>/dev/null | while read -r file; do
    cp """$file""" "${SCREENSHOT_DIR}/" 2>/dev/null || true
done

find /tests -name "*.webm" -o -name "*.mp4" 2>/dev/null | while read -r file; do
    cp """$file""" "${VIDEO_DIR}/" 2>/dev/null || true
done

# Final status
if [ ""$FAILED_TESTS"" -eq 0 ]; then
    log "\n${GREEN}=== ALL TESTS PASSED! ===${NC}"
    log "Results saved to: ${RESULTS_DIR}"
    exit 0
else
    log "\n${RED}=== TESTS FAILED ===${NC}"
    log "Failed tests: ${FAILED_TESTS}"
    log "Results saved to: ${RESULTS_DIR}"
    exit 1
fi