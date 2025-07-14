#!/bin/bash

# Comprehensive MCP Services Test Runner
# Executes detailed tests for all MCP services with reporting

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPORT_DIR="${SCRIPT_DIR}/test-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="${REPORT_DIR}/mcp-test-report-${TIMESTAMP}.txt"
SUMMARY_FILE="${REPORT_DIR}/mcp-test-summary-${TIMESTAMP}.json"

# Create report directory
mkdir -p "$REPORT_DIR"

# Test configuration
declare -A SERVICE_PORTS=(
    ["organization"]="5005"
    ["llm"]="5002"
    ["controller"]="5013"
    ["rag"]="5004"
    ["template"]="5006"
)

declare -A SERVICE_NAMES=(
    ["organization"]="MCP Organization (Java)"
    ["llm"]="MCP LLM (Java)"
    ["controller"]="MCP Controller (Java)"
    ["rag"]="MCP RAG (Java)"
    ["template"]="MCP Template (Java)"
)

# Test results tracking
declare -A TEST_RESULTS
declare -A TEST_TIMES
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Functions
log() {
    echo -e "$1" | tee -a "$REPORT_FILE"
}

log_header() {
    log ""
    log "${PURPLE}════════════════════════════════════════════════════════════════${NC}"
    log "${PURPLE}$1${NC}"
    log "${PURPLE}════════════════════════════════════════════════════════════════${NC}"
}

check_service_health() {
    local service=$1
    local port=${SERVICE_PORTS[$service]}
    
    if curl -s "http://localhost:$port/actuator/health" 2>/dev/null | grep -q "UP" 2>/dev/null; then
        return 0
    else
        return 1
    fi
}

run_service_test() {
    local service=$1
    local script="${SCRIPT_DIR}/test-mcp-${service}.sh"
    local start_time=$(date +%s)
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    log ""
    log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    log "${CYAN}Testing ${SERVICE_NAMES[$service]} Service${NC}"
    log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    # Check if test script exists
    if [ ! -f "$script" ]; then
        log "${YELLOW}⚠ Test script not found: $script${NC}"
        TEST_RESULTS[$service]="SKIPPED"
        SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
        return
    fi
    
    # Check if service is healthy
    if ! check_service_health "$service"; then
        log "${YELLOW}⚠ Service not healthy, skipping test${NC}"
        TEST_RESULTS[$service]="SKIPPED"
        SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
        return
    fi
    
    # Run the test
    if bash "$script" >> "$REPORT_FILE" 2>&1; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        TEST_RESULTS[$service]="PASSED"
        TEST_TIMES[$service]=$duration
        PASSED_TESTS=$((PASSED_TESTS + 1))
        
        log "${GREEN}✅ ${SERVICE_NAMES[$service]} tests PASSED (${duration}s)${NC}"
    else
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        TEST_RESULTS[$service]="FAILED"
        TEST_TIMES[$service]=$duration
        FAILED_TESTS=$((FAILED_TESTS + 1))
        
        log "${RED}❌ ${SERVICE_NAMES[$service]} tests FAILED (${duration}s)${NC}"
    fi
}

generate_summary() {
    # Generate JSON summary
    cat > "$SUMMARY_FILE" <<EOF
{
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "total_tests": $TOTAL_TESTS,
    "passed": $PASSED_TESTS,
    "failed": $FAILED_TESTS,
    "skipped": $SKIPPED_TESTS,
    "services": {
EOF
    
    local first=true
    for service in "${!SERVICE_NAMES[@]}"; do
        if [ "$first" = true ]; then
            first=false
        else
            echo "," >> "$SUMMARY_FILE"
        fi
        
        cat >> "$SUMMARY_FILE" <<EOF
        "$service": {
            "name": "${SERVICE_NAMES[$service]}",
            "status": "${TEST_RESULTS[$service]:-NOT_RUN}",
            "duration": ${TEST_TIMES[$service]:-0},
            "port": ${SERVICE_PORTS[$service]}
        }
EOF
    done
    
    echo -e "\n    }\n}" >> "$SUMMARY_FILE"
}

# Main execution
log_header "MCP Services Comprehensive Test Suite"
log "Started at: $(date)"
log "Report file: $REPORT_FILE"
log "Summary file: $SUMMARY_FILE"

# Check if any services are running
log ""
log "${BLUE}Checking service availability...${NC}"
AVAILABLE_SERVICES=0
for service in "${!SERVICE_PORTS[@]}"; do
    if check_service_health "$service"; then
        log "${GREEN}✓ ${SERVICE_NAMES[$service]} is available${NC}"
        AVAILABLE_SERVICES=$((AVAILABLE_SERVICES + 1))
    else
        log "${YELLOW}⚠ ${SERVICE_NAMES[$service]} is not available${NC}"
    fi
done

if [ $AVAILABLE_SERVICES -eq 0 ]; then
    log ""
    log "${RED}❌ No MCP services are running!${NC}"
    log "Please start the services with: make start-all"
    exit 1
fi

# Run tests for each service
log_header "Running Service Tests"

# Run tests in a specific order (dependencies first)
for service in organization llm controller rag template; do
    run_service_test "$service"
done

# Generate summary
generate_summary

# Display final results
log_header "Test Results Summary"

log ""
log "${BLUE}Service Test Results:${NC}"
for service in organization llm controller rag template; do
    if [ -n "${TEST_RESULTS[$service]}" ]; then
        local status_color=$GREEN
        local status_icon="✅"
        
        if [ "${TEST_RESULTS[$service]}" = "FAILED" ]; then
            status_color=$RED
            status_icon="❌"
        elif [ "${TEST_RESULTS[$service]}" = "SKIPPED" ]; then
            status_color=$YELLOW
            status_icon="⚠️"
        fi
        
        local duration="${TEST_TIMES[$service]:-0}"
        log "  ${status_icon} ${SERVICE_NAMES[$service]}: ${status_color}${TEST_RESULTS[$service]}${NC} (${duration}s)"
    fi
done

log ""
log "${BLUE}Overall Statistics:${NC}"
log "  Total Tests Run: $TOTAL_TESTS"
log "  ${GREEN}Passed: $PASSED_TESTS${NC}"
log "  ${RED}Failed: $FAILED_TESTS${NC}"
log "  ${YELLOW}Skipped: $SKIPPED_TESTS${NC}"

# Calculate success rate
if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    log "  Success Rate: ${SUCCESS_RATE}%"
fi

log ""
log "Completed at: $(date)"
log ""

# Exit code based on results
if [ $FAILED_TESTS -gt 0 ]; then
    log "${RED}⚠️  Some tests failed. Check the report for details.${NC}"
    exit 1
else
    log "${GREEN}✅ All tests completed successfully!${NC}"
    exit 0
fi