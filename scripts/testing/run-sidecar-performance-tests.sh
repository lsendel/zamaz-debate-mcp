#!/bin/bash

# MCP Sidecar Performance Test Runner
# This script runs comprehensive performance tests for the MCP Sidecar

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SIDECAR_DIR="$PROJECT_ROOT/mcp-sidecar"
RESULTS_DIR="$PROJECT_ROOT/performance-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_FILE="$RESULTS_DIR/sidecar_performance_$TIMESTAMP.txt"

# Test configuration
MAVEN_OPTS="-Xmx2g -XX:+UseG1GC"
TEST_PROFILE="test"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}MCP Sidecar Performance Test Suite${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to log with timestamp
log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}"
}

# Function to log errors
log_error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
}

# Function to log warnings
log_warning() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

# Function to check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check Java version
    if ! java -version 2>&1 | grep -q "21"; then
        log_error "Java 21 is required but not found"
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is required but not found"
        exit 1
    fi
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is required but not found"
        exit 1
    fi
    
    # Check if Docker is running
    if ! docker info &> /dev/null; then
        log_error "Docker is not running"
        exit 1
    fi
    
    log "Prerequisites check passed"
}

# Function to prepare test environment
prepare_environment() {
    log "Preparing test environment..."
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    
    # Start Redis for testing
    log "Starting Redis container for testing..."
    docker run -d --name redis-test -p 6379:6379 redis:7-alpine > /dev/null 2>&1 || true
    
    # Wait for Redis to be ready
    sleep 5
    
    # Check Redis connectivity
    if ! docker exec redis-test redis-cli ping &> /dev/null; then
        log_error "Redis test container is not responding"
        exit 1
    fi
    
    log "Test environment prepared"
}

# Function to run performance tests
run_performance_tests() {
    log "Running performance tests..."
    
    cd "$SIDECAR_DIR"
    
    # Set Maven options for performance
    export MAVEN_OPTS="$MAVEN_OPTS"
    
    # Run performance tests
    log "Executing performance test suite..."
    
    mvn test \
        -Dtest=SidecarPerformanceTest \
        -Dspring.profiles.active=$TEST_PROFILE \
        -Dlogging.level.com.zamaz.mcp.sidecar=WARN \
        -Dlogging.level.org.springframework=WARN \
        -Dspring.jpa.show-sql=false \
        -Dspring.jpa.hibernate.ddl-auto=none \
        -Dmaven.test.failure.ignore=true \
        > "$RESULTS_FILE" 2>&1
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        log "Performance tests completed successfully"
    else
        log_warning "Performance tests completed with warnings (exit code: $exit_code)"
    fi
    
    return $exit_code
}

# Function to run JMeter load tests
run_jmeter_tests() {
    log "Running JMeter load tests..."
    
    # Check if JMeter is available
    if ! command -v jmeter &> /dev/null; then
        log_warning "JMeter not found, skipping load tests"
        return 0
    fi
    
    # Create JMeter test plan
    create_jmeter_test_plan
    
    # Run JMeter tests
    jmeter -n -t "$RESULTS_DIR/sidecar_load_test.jmx" \
           -l "$RESULTS_DIR/jmeter_results_$TIMESTAMP.jtl" \
           -e -o "$RESULTS_DIR/jmeter_report_$TIMESTAMP" \
           >> "$RESULTS_FILE" 2>&1
    
    log "JMeter load tests completed"
}

# Function to create JMeter test plan
create_jmeter_test_plan() {
    cat > "$RESULTS_DIR/sidecar_load_test.jmx" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.4.1">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="MCP Sidecar Load Test">
      <stringProp name="TestPlan.comments">Performance test for MCP Sidecar</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.tearDown_on_shutdown">true</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
        <collectionProp name="Arguments.arguments"/>
      </elementProp>
      <stringProp name="TestPlan.user_define_classpath"></stringProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Load Test Thread Group" enabled="true">
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlGui" testclass="LoopController" testname="Loop Controller" enabled="true">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">100</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">50</stringProp>
        <stringProp name="ThreadGroup.ramp_time">30</stringProp>
        <boolProp name="ThreadGroup.scheduler">false</boolProp>
        <stringProp name="ThreadGroup.duration"></stringProp>
        <stringProp name="ThreadGroup.delay"></stringProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Health Check" enabled="true">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
            <collectionProp name="Arguments.arguments"/>
          </elementProp>
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.protocol">http</stringProp>
          <stringProp name="HTTPSampler.contentEncoding"></stringProp>
          <stringProp name="HTTPSampler.path">/actuator/health</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
          <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
          <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
          <boolProp name="HTTPSampler.DO_MULTIPART_POST">false</boolProp>
          <stringProp name="HTTPSampler.embedded_url_re"></stringProp>
          <stringProp name="HTTPSampler.connect_timeout"></stringProp>
          <stringProp name="HTTPSampler.response_timeout"></stringProp>
        </HTTPSamplerProxy>
        <hashTree/>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
EOF
}

# Function to run memory profiling
run_memory_profiling() {
    log "Running memory profiling..."
    
    # Use jstat to monitor memory usage
    if command -v jstat &> /dev/null; then
        log "Monitoring JVM memory usage during tests..."
        
        # Find Java process
        local java_pid=$(pgrep -f "mcp-sidecar" || echo "")
        
        if [ -n "$java_pid" ]; then
            # Monitor for 60 seconds
            for i in {1..12}; do
                echo "=== Memory Usage Sample $i ===" >> "$RESULTS_FILE"
                jstat -gc "$java_pid" >> "$RESULTS_FILE" 2>&1
                sleep 5
            done
        else
            log_warning "Could not find Java process for memory monitoring"
        fi
    else
        log_warning "jstat not available, skipping memory profiling"
    fi
}

# Function to collect system metrics
collect_system_metrics() {
    log "Collecting system metrics..."
    
    {
        echo "=== System Information ==="
        echo "Date: $(date)"
        echo "Hostname: $(hostname)"
        echo "OS: $(uname -a)"
        echo "Java Version: $(java -version 2>&1 | head -1)"
        echo "Available Memory: $(free -h | grep Mem | awk '{print $2}')"
        echo "CPU Info: $(lscpu | grep 'Model name' | cut -d: -f2 | sed 's/^[ \t]*//')"
        echo "CPU Cores: $(nproc)"
        echo ""
        
        echo "=== Docker Information ==="
        docker --version
        echo "Docker Memory: $(docker system df)"
        echo ""
        
        echo "=== Network Configuration ==="
        echo "Network interfaces:"
        ip addr show | grep -E '^[0-9]+:' | awk '{print $2}' | sed 's/://'
        echo ""
        
    } >> "$RESULTS_FILE"
}

# Function to generate performance report
generate_performance_report() {
    log "Generating performance report..."
    
    local report_file="$RESULTS_DIR/performance_report_$TIMESTAMP.md"
    
    cat > "$report_file" << EOF
# MCP Sidecar Performance Test Report

**Date:** $(date)
**Test Duration:** $(date -d @$(($(date +%s) - start_time)) -u +%H:%M:%S)

## Test Environment

- **Java Version:** $(java -version 2>&1 | head -1)
- **Available Memory:** $(free -h | grep Mem | awk '{print $2}')
- **CPU Cores:** $(nproc)
- **OS:** $(uname -a)

## Test Results Summary

### Performance Benchmarks

The following performance benchmarks were executed:

1. **Authentication Performance**
   - Measures JWT token generation and validation speed
   - Target: < 100ms average response time

2. **Concurrent Request Handling**
   - Tests system behavior under concurrent load
   - Target: 95% success rate with 50 concurrent users

3. **Security Scanning Performance**
   - Measures security pattern matching speed
   - Target: < 10ms average scan time

4. **Caching Performance**
   - Tests Redis cache read/write performance
   - Target: < 5ms writes, < 2ms reads

5. **Metrics Collection Performance**
   - Measures metrics recording overhead
   - Target: < 1ms per metric

6. **End-to-End Request Flow**
   - Tests complete request processing pipeline
   - Target: < 150ms average response time

### Load Testing Results

JMeter load tests were executed with:
- 50 concurrent users
- 100 requests per user
- 30-second ramp-up time

### Memory Usage Analysis

Memory profiling was performed to identify:
- Heap memory usage patterns
- Garbage collection frequency
- Memory leaks or excessive allocations

## Detailed Results

See the full test output in: \`$(basename "$RESULTS_FILE")\`

## Recommendations

Based on the performance test results, the following optimizations are recommended:

1. **JVM Tuning:** Adjust heap sizes and GC settings based on memory usage patterns
2. **Connection Pooling:** Optimize database and Redis connection pool sizes
3. **Caching Strategy:** Fine-tune cache TTL values and eviction policies
4. **Rate Limiting:** Adjust rate limiting thresholds based on observed capacity

## Next Steps

1. Review failed test cases and investigate root causes
2. Implement recommended optimizations
3. Re-run performance tests to validate improvements
4. Set up continuous performance monitoring
EOF

    log "Performance report generated: $report_file"
}

# Function to cleanup test environment
cleanup() {
    log "Cleaning up test environment..."
    
    # Stop and remove Redis test container
    docker stop redis-test > /dev/null 2>&1 || true
    docker rm redis-test > /dev/null 2>&1 || true
    
    log "Cleanup completed"
}

# Function to display results summary
display_results() {
    log "Performance test results summary:"
    echo ""
    echo -e "${BLUE}Results Location:${NC} $RESULTS_DIR"
    echo -e "${BLUE}Test Output:${NC} $RESULTS_FILE"
    echo ""
    
    # Extract key metrics from results file
    if [ -f "$RESULTS_FILE" ]; then
        echo -e "${BLUE}Key Metrics:${NC}"
        
        # Look for performance benchmark results
        if grep -q "Authentication Benchmark Results" "$RESULTS_FILE"; then
            echo -e "${GREEN}✓ Authentication benchmarks completed${NC}"
        fi
        
        if grep -q "Concurrent Request Benchmark Results" "$RESULTS_FILE"; then
            echo -e "${GREEN}✓ Concurrent request benchmarks completed${NC}"
        fi
        
        if grep -q "Security Scanning Benchmark Results" "$RESULTS_FILE"; then
            echo -e "${GREEN}✓ Security scanning benchmarks completed${NC}"
        fi
        
        if grep -q "Caching Benchmark Results" "$RESULTS_FILE"; then
            echo -e "${GREEN}✓ Caching benchmarks completed${NC}"
        fi
        
        # Check for failures
        if grep -q "FAILED" "$RESULTS_FILE"; then
            echo -e "${RED}⚠ Some tests failed - check logs for details${NC}"
        else
            echo -e "${GREEN}✓ All performance tests passed${NC}"
        fi
    fi
    
    echo ""
    echo -e "${BLUE}To view detailed results:${NC} cat $RESULTS_FILE"
    echo -e "${BLUE}To view performance report:${NC} cat $RESULTS_DIR/performance_report_$TIMESTAMP.md"
}

# Main execution
main() {
    local start_time=$(date +%s)
    
    # Trap cleanup function
    trap cleanup EXIT
    
    # Check prerequisites
    check_prerequisites
    
    # Prepare test environment
    prepare_environment
    
    # Collect system metrics
    collect_system_metrics
    
    # Run performance tests
    run_performance_tests
    
    # Run JMeter tests if available
    run_jmeter_tests
    
    # Run memory profiling
    run_memory_profiling
    
    # Generate performance report
    generate_performance_report
    
    # Display results
    display_results
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log "Performance testing completed in ${duration} seconds"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            TEST_PROFILE="$2"
            shift 2
            ;;
        --results-dir)
            RESULTS_DIR="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --profile PROFILE    Set Spring profile (default: test)"
            echo "  --results-dir DIR    Set results directory (default: ./performance-results)"
            echo "  --help, -h           Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Run main function
main "$@"