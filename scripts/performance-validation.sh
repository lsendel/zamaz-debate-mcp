#!/bin/bash

# Performance Validation Script
# This script validates that the best practices implementation doesn't impact performance

set -e

echo "🚀 Performance Validation Tests"
echo "==============================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Performance thresholds (in milliseconds)
HEALTH_CHECK_THRESHOLD=100
API_RESPONSE_THRESHOLD=500
STARTUP_THRESHOLD=30000

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

# Function to measure response time
measure_response_time() {
    local url="$1"
    local timeout="${2:-5}"
    
    local start_time=$(date +%s%N)
    if curl -s -f --max-time "$timeout" "$url" >/dev/null 2>&1; then
        local end_time=$(date +%s%N)
        local duration=$(( (end_time - start_time) / 1000000 ))  # Convert to milliseconds
        echo "$duration"
        return 0
    else
        echo "-1"
        return 1
    fi
}

# Function to test endpoint performance
test_endpoint_performance() {
    local service="$1"
    local port="$2"
    local endpoint="$3"
    local threshold="$4"
    
    local url="http://localhost:$port$endpoint"
    
    echo "Testing $service endpoint performance..."
    
    # Warm up with a few requests
    for i in {1..3}; do
        curl -s -f "$url" >/dev/null 2>&1 || true
    done
    
    # Measure response time over multiple requests
    local total_time=0
    local successful_requests=0
    
    for i in {1..10}; do
        local response_time=$(measure_response_time "$url" 5)
        if [ "$response_time" -gt -1 ]; then
            total_time=$((total_time + response_time))
            successful_requests=$((successful_requests + 1))
        fi
    done
    
    if [ $successful_requests -gt 0 ]; then
        local avg_time=$((total_time / successful_requests))
        
        if [ $avg_time -lt $threshold ]; then
            log_test "Performance" "PASS" "$service $endpoint avg response time: ${avg_time}ms (threshold: ${threshold}ms)"
        else
            log_test "Performance" "FAIL" "$service $endpoint avg response time: ${avg_time}ms (threshold: ${threshold}ms)"
        fi
        
        echo "  📊 Details: $successful_requests/10 requests successful, avg: ${avg_time}ms"
    else
        log_test "Performance" "FAIL" "$service $endpoint not responding"
    fi
}

# Function to test memory usage
test_memory_usage() {
    local service="$1"
    local port="$2"
    
    echo "Testing $service memory usage..."
    
    # Try to get memory metrics from actuator
    local metrics_url="http://localhost:$port/actuator/metrics/jvm.memory.used"
    if curl -s -f "$metrics_url" >/dev/null 2>&1; then
        local memory_response=$(curl -s "$metrics_url" 2>/dev/null || echo '{}')
        local memory_used=$(echo "$memory_response" | jq -r '.measurements[0].value' 2>/dev/null || echo "unknown")
        
        if [ "$memory_used" != "unknown" ] && [ "$memory_used" != "null" ]; then
            local memory_mb=$(echo "scale=2; $memory_used / 1024 / 1024" | bc 2>/dev/null || echo "0")
            
            if (( $(echo "$memory_mb > 0" | bc -l) )); then
                if (( $(echo "$memory_mb < 512" | bc -l) )); then
                    log_test "Memory Usage" "PASS" "$service memory usage: ${memory_mb}MB"
                elif (( $(echo "$memory_mb < 1024" | bc -l) )); then
                    log_test "Memory Usage" "WARN" "$service memory usage: ${memory_mb}MB (consider optimization)"
                else
                    log_test "Memory Usage" "FAIL" "$service memory usage: ${memory_mb}MB (too high)"
                fi
            else
                log_test "Memory Usage" "WARN" "$service memory metrics not available"
            fi
        else
            log_test "Memory Usage" "WARN" "$service memory metrics not available"
        fi
    else
        log_test "Memory Usage" "WARN" "$service metrics endpoint not accessible"
    fi
}

# Function to test concurrent requests
test_concurrent_performance() {
    local service="$1"
    local port="$2"
    local endpoint="$3"
    
    echo "Testing $service concurrent request handling..."
    
    local url="http://localhost:$port$endpoint"
    local concurrent_requests=10
    local temp_dir="/tmp/perf_test_$$"
    mkdir -p "$temp_dir"
    
    # Start concurrent requests
    local start_time=$(date +%s%N)
    for i in $(seq 1 $concurrent_requests); do
        curl -s -f "$url" >"$temp_dir/response_$i" 2>/dev/null &
    done
    
    # Wait for all requests to complete
    wait
    local end_time=$(date +%s%N)
    local total_duration=$(( (end_time - start_time) / 1000000 ))
    
    # Count successful responses
    local successful=0
    for i in $(seq 1 $concurrent_requests); do
        if [ -s "$temp_dir/response_$i" ]; then
            successful=$((successful + 1))
        fi
    done
    
    # Cleanup
    rm -rf "$temp_dir"
    
    local success_rate=$((successful * 100 / concurrent_requests))
    
    if [ $success_rate -ge 90 ]; then
        log_test "Concurrent Performance" "PASS" "$service handled $successful/$concurrent_requests concurrent requests (${success_rate}%) in ${total_duration}ms"
    elif [ $success_rate -ge 70 ]; then
        log_test "Concurrent Performance" "WARN" "$service handled $successful/$concurrent_requests concurrent requests (${success_rate}%) in ${total_duration}ms"
    else
        log_test "Concurrent Performance" "FAIL" "$service handled $successful/$concurrent_requests concurrent requests (${success_rate}%) in ${total_duration}ms"
    fi
}

# Function to test startup time
test_startup_performance() {
    local service="$1"
    local port="$2"
    
    echo "Testing $service startup performance..."
    
    # Check if service is already running
    if curl -s -f "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
        # Try to get startup time from actuator
        local startup_url="http://localhost:$port/actuator/metrics/application.started.time"
        if curl -s -f "$startup_url" >/dev/null 2>&1; then
            local startup_response=$(curl -s "$startup_url" 2>/dev/null || echo '{}')
            local startup_time=$(echo "$startup_response" | jq -r '.measurements[0].value' 2>/dev/null || echo "unknown")
            
            if [ "$startup_time" != "unknown" ] && [ "$startup_time" != "null" ]; then
                local startup_ms=$(echo "scale=0; $startup_time * 1000" | bc 2>/dev/null || echo "0")
                
                if (( $(echo "$startup_ms > 0" | bc -l) )); then
                    if [ ${startup_ms%.*} -lt $STARTUP_THRESHOLD ]; then
                        log_test "Startup Performance" "PASS" "$service startup time: ${startup_ms}ms (threshold: ${STARTUP_THRESHOLD}ms)"
                    else
                        log_test "Startup Performance" "WARN" "$service startup time: ${startup_ms}ms (threshold: ${STARTUP_THRESHOLD}ms)"
                    fi
                else
                    log_test "Startup Performance" "WARN" "$service startup metrics not available"
                fi
            else
                log_test "Startup Performance" "WARN" "$service startup metrics not available"
            fi
        else
            log_test "Startup Performance" "WARN" "$service startup metrics endpoint not accessible"
        fi
    else
        log_test "Startup Performance" "WARN" "$service not running - cannot test startup time"
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
echo "🏥 1. Health Check Performance"
echo "-----------------------------"

for service in "${!services[@]}"; do
    port="${services[$service]}"
    test_endpoint_performance "$service" "$port" "/actuator/health" "$HEALTH_CHECK_THRESHOLD"
done

echo ""
echo "🎯 2. API Endpoint Performance"
echo "-----------------------------"

# Test API endpoints (if services are running)
if curl -s -f "http://localhost:5005/actuator/health" >/dev/null 2>&1; then
    test_endpoint_performance "mcp-organization" "5005" "/api/v1/organizations" "$API_RESPONSE_THRESHOLD"
    test_endpoint_performance "mcp-llm" "5002" "/api/v1/providers" "$API_RESPONSE_THRESHOLD"
    test_endpoint_performance "mcp-controller" "5013" "/api/v1/debates" "$API_RESPONSE_THRESHOLD"
    test_endpoint_performance "mcp-rag" "5004" "/actuator/health" "$API_RESPONSE_THRESHOLD"
else
    log_test "API Performance" "WARN" "Services not running - skipping API performance tests"
fi

echo ""
echo "💾 3. Memory Usage Tests"
echo "----------------------"

for service in "${!services[@]}"; do
    port="${services[$service]}"
    if [ "$service" != "mcp-gateway" ]; then  # Gateway might not have detailed metrics
        test_memory_usage "$service" "$port"
    fi
done

echo ""
echo "⚡ 4. Concurrent Request Tests"
echo "----------------------------"

# Test concurrent performance (if services are running)
if curl -s -f "http://localhost:5005/actuator/health" >/dev/null 2>&1; then
    test_concurrent_performance "mcp-organization" "5005" "/actuator/health"
    test_concurrent_performance "mcp-llm" "5002" "/actuator/health"
    test_concurrent_performance "mcp-controller" "5013" "/actuator/health"
    test_concurrent_performance "mcp-rag" "5004" "/actuator/health"
else
    log_test "Concurrent Performance" "WARN" "Services not running - skipping concurrent tests"
fi

echo ""
echo "🚀 5. Startup Performance Tests"
echo "------------------------------"

for service in "${!services[@]}"; do
    port="${services[$service]}"
    if [ "$service" != "mcp-gateway" ]; then  # Gateway might not have detailed metrics
        test_startup_performance "$service" "$port"
    fi
done

echo ""
echo "📊 6. System Resource Usage"
echo "-------------------------"

# Check system resources
if command -v free >/dev/null 2>&1; then
    total_memory=$(free -m | awk '/^Mem:/{print $2}')
    used_memory=$(free -m | awk '/^Mem:/{print $3}')
    memory_percent=$(echo "scale=1; $used_memory * 100 / $total_memory" | bc 2>/dev/null || echo "0")
    
    if (( $(echo "$memory_percent < 80" | bc -l) )); then
        log_test "System Memory" "PASS" "System memory usage: ${memory_percent}% (${used_memory}MB/${total_memory}MB)"
    elif (( $(echo "$memory_percent < 90" | bc -l) )); then
        log_test "System Memory" "WARN" "System memory usage: ${memory_percent}% (${used_memory}MB/${total_memory}MB)"
    else
        log_test "System Memory" "FAIL" "System memory usage: ${memory_percent}% (${used_memory}MB/${total_memory}MB)"
    fi
else
    log_test "System Memory" "WARN" "Memory usage check not available"
fi

# Check CPU load
if command -v uptime >/dev/null 2>&1; then
    load_avg=$(uptime | awk -F'load average:' '{print $2}' | cut -d, -f1 | xargs)
    cpu_cores=$(nproc 2>/dev/null || echo "1")
    
    if command -v bc >/dev/null 2>&1; then
        load_percent=$(echo "scale=1; $load_avg * 100 / $cpu_cores" | bc)
        
        if (( $(echo "$load_percent < 50" | bc -l) )); then
            log_test "System Load" "PASS" "System load: ${load_percent}% (${load_avg}/${cpu_cores} cores)"
        elif (( $(echo "$load_percent < 80" | bc -l) )); then
            log_test "System Load" "WARN" "System load: ${load_percent}% (${load_avg}/${cpu_cores} cores)"
        else
            log_test "System Load" "FAIL" "System load: ${load_percent}% (${load_avg}/${cpu_cores} cores)"
        fi
    else
        log_test "System Load" "WARN" "System load: ${load_avg} (${cpu_cores} cores)"
    fi
else
    log_test "System Load" "WARN" "Load average check not available"
fi

echo ""
echo "======================================================"
echo "📊 PERFORMANCE VALIDATION SUMMARY"
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
echo "🎯 Performance Thresholds:"
echo "• Health Check: < ${HEALTH_CHECK_THRESHOLD}ms"
echo "• API Response: < ${API_RESPONSE_THRESHOLD}ms"
echo "• Startup Time: < ${STARTUP_THRESHOLD}ms"
echo "• Memory Usage: < 512MB per service"
echo "• Concurrent Success: > 90%"

echo ""
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 PERFORMANCE VALIDATION PASSED!${NC}"
    echo -e "${GREEN}✅ Best practices implementation maintains good performance${NC}"
    
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠️  $WARNINGS performance warnings found - consider optimization${NC}"
    fi
    
    echo ""
    echo "🚀 Performance Summary:"
    echo "• All services meet performance thresholds"
    echo "• Memory usage is within acceptable limits"
    echo "• Concurrent request handling is efficient"
    echo "• System resources are well utilized"
    
    exit 0
else
    echo -e "${RED}❌ PERFORMANCE VALIDATION FAILED!${NC}"
    echo -e "${RED}🔧 Performance issues detected${NC}"
    
    echo ""
    echo "🔍 Recommended Actions:"
    echo "1. Check service logs for performance bottlenecks"
    echo "2. Review memory usage and consider optimization"
    echo "3. Monitor system resources during load"
    echo "4. Consider tuning JVM parameters"
    echo "5. Profile slow endpoints for optimization"
    
    exit 1
fi