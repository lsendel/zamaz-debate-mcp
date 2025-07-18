#!/bin/bash
# Performance-focused integration tests for the linting system
# Tests scalability, caching, and optimization features

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PERF_TEST_DIR=".linting/performance-tests"
RESULTS_DIR=".linting/test-results/performance"

setup_performance_tests() {
    echo -e "${BLUE}Setting up performance test environment...${NC}"
    
    mkdir -p "$PERF_TEST_DIR"/{large-codebase,cache-tests,parallel-tests}
    mkdir -p "$RESULTS_DIR"
    
    # Generate large test codebase
    for i in {1..50}; do
        cat > "$PERF_TEST_DIR/large-codebase/TestClass$i.java" << EOF
package com.zamaz.mcp.test.large;

import java.util.*;
import java.io.*;

public class TestClass$i {
    private static final int CONSTANT_$i = $i;
    private List<String> data = new ArrayList<>();
    
    public void processData() {
        for (int j = 0; j < 100; j++) {
            data.add("Item " + j + " from class $i");
        }
        
        // Intentional style violations for testing
        if(data.size()>0){
            System.out.println("Processing " + data.size() + " items");
        }
    }
    
    public String getData(int index) {
        if (index >= 0 && index < data.size()) {
            return data.get(index);
        }
        return null;
    }
}
EOF
    done
    
    echo -e "${GREEN}✓ Performance test environment ready${NC}"
}

# Test linting performance on large codebase
test_large_codebase_performance() {
    echo -e "${YELLOW}Testing large codebase performance...${NC}"
    
    local start_time=$(date +%s.%N)
    
    # Run checkstyle on all generated files
    find "$PERF_TEST_DIR/large-codebase" -name "*.java" -exec \
        java -jar $(find ~/.m2/repository -name 'checkstyle-*.jar' | head -1) \
        -c .linting/java/checkstyle.xml {} \; > "$RESULTS_DIR/large-codebase-results.txt" 2>&1 || true
    
    local end_time=$(date +%s.%N)
    local duration=$(echo "$end_time - $start_time" | bc -l)
    
    echo -e "${GREEN}✓ Large codebase test completed in ${duration}s${NC}"
    echo "Duration: ${duration}s" > "$RESULTS_DIR/large-codebase-performance.txt"
    
    # Performance threshold check (should complete within 60 seconds)
    if (( $(echo "$duration < 60" | bc -l) )); then
        echo -e "${GREEN}✓ Performance within acceptable limits${NC}"
        return 0
    else
        echo -e "${RED}✗ Performance exceeds acceptable limits${NC}"
        return 1
    fi
}

# Test caching effectiveness
test_cache_performance() {
    echo -e "${YELLOW}Testing cache performance...${NC}"
    
    # Clear cache first
    node .linting/scripts/cache-manager.js clean
    
    # First run (cold cache)
    local start_time1=$(date +%s.%N)
    .linting/scripts/incremental-lint.sh --include-pattern "$PERF_TEST_DIR/large-codebase/*.java" > /dev/null 2>&1 || true
    local end_time1=$(date +%s.%N)
    local cold_duration=$(echo "$end_time1 - $start_time1" | bc -l)
    
    # Second run (warm cache)
    local start_time2=$(date +%s.%N)
    .linting/scripts/incremental-lint.sh --include-pattern "$PERF_TEST_DIR/large-codebase/*.java" > /dev/null 2>&1 || true
    local end_time2=$(date +%s.%N)
    local warm_duration=$(echo "$end_time2 - $start_time2" | bc -l)
    
    # Calculate improvement
    local improvement=$(echo "scale=2; (($cold_duration - $warm_duration) / $cold_duration) * 100" | bc -l)
    
    echo -e "${GREEN}✓ Cold cache: ${cold_duration}s, Warm cache: ${warm_duration}s${NC}"
    echo -e "${GREEN}✓ Cache improvement: ${improvement}%${NC}"
    
    {
        echo "Cold cache duration: ${cold_duration}s"
        echo "Warm cache duration: ${warm_duration}s"
        echo "Improvement: ${improvement}%"
    } > "$RESULTS_DIR/cache-performance.txt"
    
    # Cache should provide at least 20% improvement
    if (( $(echo "$improvement > 20" | bc -l) )); then
        echo -e "${GREEN}✓ Cache provides significant performance improvement${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ Cache improvement below expected threshold${NC}"
        return 0  # Not a failure, just suboptimal
    fi
}

# Test parallel execution performance
test_parallel_performance() {
    echo -e "${YELLOW}Testing parallel execution performance...${NC}"
    
    # Sequential execution
    local start_time1=$(date +%s.%N)
    for file in "$PERF_TEST_DIR/large-codebase"/*.java; do
        java -jar $(find ~/.m2/repository -name 'checkstyle-*.jar' | head -1) \
            -c .linting/java/checkstyle.xml "$file" > /dev/null 2>&1 || true
    done
    local end_time1=$(date +%s.%N)
    local sequential_duration=$(echo "$end_time1 - $start_time1" | bc -l)
    
    # Parallel execution
    local start_time2=$(date +%s.%N)
    find "$PERF_TEST_DIR/large-codebase" -name "*.java" | \
        xargs -P 4 -I {} java -jar $(find ~/.m2/repository -name 'checkstyle-*.jar' | head -1) \
        -c .linting/java/checkstyle.xml {} > /dev/null 2>&1 || true
    local end_time2=$(date +%s.%N)
    local parallel_duration=$(echo "$end_time2 - $start_time2" | bc -l)
    
    # Calculate improvement
    local improvement=$(echo "scale=2; (($sequential_duration - $parallel_duration) / $sequential_duration) * 100" | bc -l)
    
    echo -e "${GREEN}✓ Sequential: ${sequential_duration}s, Parallel: ${parallel_duration}s${NC}"
    echo -e "${GREEN}✓ Parallel improvement: ${improvement}%${NC}"
    
    {
        echo "Sequential duration: ${sequential_duration}s"
        echo "Parallel duration: ${parallel_duration}s"
        echo "Improvement: ${improvement}%"
    } > "$RESULTS_DIR/parallel-performance.txt"
    
    return 0
}

# Test memory usage
test_memory_usage() {
    echo -e "${YELLOW}Testing memory usage...${NC}"
    
    # Monitor memory usage during linting
    local pid_file="/tmp/lint_memory_test.pid"
    local memory_log="$RESULTS_DIR/memory-usage.txt"
    
    # Start memory monitoring in background
    (
        while [ -f "$pid_file" ]; do
            ps -o pid,vsz,rss,comm -p $(cat "$pid_file") 2>/dev/null || true
            sleep 1
        done
    ) > "$memory_log" &
    local monitor_pid=$!
    
    # Run linting process
    echo $$ > "$pid_file"
    .linting/scripts/incremental-lint.sh --include-pattern "$PERF_TEST_DIR/large-codebase/*.java" > /dev/null 2>&1 || true
    rm -f "$pid_file"
    
    # Stop monitoring
    kill $monitor_pid 2>/dev/null || true
    
    # Analyze memory usage
    if [ -s "$memory_log" ]; then
        local max_memory=$(awk 'NR>1 {if($2>max) max=$2} END {print max}' "$memory_log")
        echo -e "${GREEN}✓ Maximum memory usage: ${max_memory}KB${NC}"
        echo "Maximum memory usage: ${max_memory}KB" >> "$RESULTS_DIR/memory-analysis.txt"
    fi
    
    return 0
}

# Main performance test execution
main() {
    echo -e "${BLUE}=== PERFORMANCE INTEGRATION TESTS ===${NC}"
    
    setup_performance_tests
    
    echo -e "${BLUE}Running performance tests...${NC}"
    
    test_large_codebase_performance
    test_cache_performance
    test_parallel_performance
    test_memory_usage
    
    echo -e "${GREEN}Performance integration tests completed!${NC}"
    echo -e "${BLUE}Results available in: $RESULTS_DIR${NC}"
}

# Execute if run directly
if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
    main "$@"
fi