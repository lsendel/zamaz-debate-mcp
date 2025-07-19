#!/bin/bash
# Master Integration Test Runner for Project Linter
# Orchestrates all integration tests to ensure 80%+ functionality coverage

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test configuration
MASTER_RESULTS_DIR=".linting/test-results/master"
COVERAGE_TARGET=80
START_TIME=$(date +%s)

# Test suite tracking (using simple arrays instead of associative arrays)
TEST_SUITE_NAMES="core_integration performance workflow e2e"
TEST_SUITE_SCRIPTS=".linting/scripts/integration-test-suite.sh .linting/scripts/performance-integration-tests.sh .linting/scripts/workflow-integration-tests.sh .linting/scripts/e2e-test.sh"

# Coverage areas
COVERAGE_AREAS="java_linting frontend_linting config_linting doc_linting security_linting incremental_linting cache_management ide_integration ci_cd_integration reporting error_handling performance service_overrides pre_commit_hooks quality_gates workflow_integration end_to_end"

# Initialize master test environment
initialize_master_tests() {
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║                PROJECT LINTER INTEGRATION TESTS             ║${NC}"
    echo -e "${CYAN}║                    Master Test Runner                       ║${NC}"
    echo -e "${CYAN}║                  Target Coverage: ${COVERAGE_TARGET}%                        ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    mkdir -p "$MASTER_RESULTS_DIR"
    
    # Initialize master coverage report
    cat > "$MASTER_RESULTS_DIR/master-coverage-report.json" << EOF
{
    "test_execution": {
        "start_time": "$(date -Iseconds)",
        "target_coverage": $COVERAGE_TARGET,
        "test_suites": {}
    },
    "coverage_areas": {
        "java_linting": {"tested": false, "suites": []},
        "frontend_linting": {"tested": false, "suites": []},
        "config_linting": {"tested": false, "suites": []},
        "doc_linting": {"tested": false, "suites": []},
        "security_linting": {"tested": false, "suites": []},
        "incremental_linting": {"tested": false, "suites": []},
        "cache_management": {"tested": false, "suites": []},
        "ide_integration": {"tested": false, "suites": []},
        "ci_cd_integration": {"tested": false, "suites": []},
        "reporting": {"tested": false, "suites": []},
        "error_handling": {"tested": false, "suites": []},
        "performance": {"tested": false, "suites": []},
        "service_overrides": {"tested": false, "suites": []},
        "pre_commit_hooks": {"tested": false, "suites": []},
        "quality_gates": {"tested": false, "suites": []},
        "workflow_integration": {"tested": false, "suites": []},
        "end_to_end": {"tested": false, "suites": []}
    }
}
EOF
    
    echo -e "${GREEN}✓ Master test environment initialized${NC}"
    echo ""
}

# Run individual test suite
run_test_suite() {
    local suite_name="$1"
    local suite_script="$2"
    
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║ Running Test Suite: $(printf "%-42s" "$suite_name") ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    
    local suite_start_time=$(date +%s)
    local suite_log="$MASTER_RESULTS_DIR/${suite_name}-execution.log"
    
    if [ -f "$suite_script" ]; then
        # Make script executable
        chmod +x "$suite_script"
        
        # Run the test suite
        if bash "$suite_script" > "$suite_log" 2>&1; then
            local suite_end_time=$(date +%s)
            local suite_duration=$((suite_end_time - suite_start_time))
            
            echo -e "${GREEN}✅ Test Suite '$suite_name' PASSED (${suite_duration}s)${NC}"
            SUITE_STATUS["$suite_name"]="PASSED"
            
            # Extract coverage information from suite results
            extract_suite_coverage "$suite_name" "$suite_log"
        else
            local suite_end_time=$(date +%s)
            local suite_duration=$((suite_end_time - suite_start_time))
            
            echo -e "${RED}❌ Test Suite '$suite_name' FAILED (${suite_duration}s)${NC}"
            echo -e "${YELLOW}   Check log: $suite_log${NC}"
            SUITE_STATUS["$suite_name"]="FAILED"
            
            # Show last few lines of error log
            echo -e "${RED}   Last 5 lines of error log:${NC}"
            tail -n 5 "$suite_log" | sed 's/^/   /'
        fi
    else
        echo -e "${RED}❌ Test Suite script not found: $suite_script${NC}"
        SUITE_STATUS["$suite_name"]="NOT_FOUND"
    fi
    
    echo ""
}

# Extract coverage information from suite results
extract_suite_coverage() {
    local suite_name="$1"
    local suite_log="$2"
    
    # Look for coverage indicators in the log
    local coverage_areas=(
        "java_linting" "frontend_linting" "config_linting" "doc_linting"
        "security_linting" "incremental_linting" "cache_management"
        "ide_integration" "ci_cd_integration" "reporting" "error_handling"
        "performance" "service_overrides" "pre_commit_hooks" "quality_gates"
        "workflow_integration" "end_to_end"
    )
    
    for area in "${coverage_areas[@]}"; do
        if grep -q "$area" "$suite_log" 2>/dev/null; then
            SUITE_COVERAGE["${suite_name}_${area}"]=1
        fi
    done
}

# Calculate overall coverage
calculate_coverage() {
    echo -e "${BLUE}Calculating coverage across all test suites...${NC}"
    
    local total_areas=17  # Total number of coverage areas
    local covered_areas=0
    
    # Coverage areas to check
    local areas=(
        "java_linting" "frontend_linting" "config_linting" "doc_linting"
        "security_linting" "incremental_linting" "cache_management"
        "ide_integration" "ci_cd_integration" "reporting" "error_handling"
        "performance" "service_overrides" "pre_commit_hooks" "quality_gates"
        "workflow_integration" "end_to_end"
    )
    
    echo -e "${PURPLE}Coverage Analysis:${NC}"
    echo -e "${PURPLE}==================${NC}"
    
    for area in "${areas[@]}"; do
        local area_covered=false
        
        # Check if any suite covered this area
        for suite in "${!TEST_SUITES[@]}"; do
            if [ "${SUITE_COVERAGE["${suite}_${area}"]:-0}" -eq 1 ]; then
                area_covered=true
                break
            fi
        done
        
        if [ "$area_covered" = true ]; then
            echo -e "${GREEN}✅ $area${NC}"
            covered_areas=$((covered_areas + 1))
        else
            echo -e "${RED}❌ $area${NC}"
        fi
    done
    
    local coverage_percentage=$(( (covered_areas * 100) / total_areas ))
    
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║                    COVERAGE SUMMARY                         ║${NC}"
    echo -e "${CYAN}╠══════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${CYAN}║ Covered Areas: $(printf "%2d" $covered_areas)/$(printf "%-2d" $total_areas)                                    ║${NC}"
    echo -e "${CYAN}║ Coverage Percentage: $(printf "%3d" $coverage_percentage)%%                               ║${NC}"
    echo -e "${CYAN}║ Target Coverage: $(printf "%3d" $COVERAGE_TARGET)%%                                  ║${NC}"
    
    if [ $coverage_percentage -ge $COVERAGE_TARGET ]; then
        echo -e "${CYAN}║ Status: ${GREEN}✅ TARGET ACHIEVED${CYAN}                              ║${NC}"
    else
        echo -e "${CYAN}║ Status: ${RED}❌ TARGET NOT MET${CYAN}                               ║${NC}"
    fi
    
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    
    return $coverage_percentage
}

# Generate comprehensive test report
generate_master_report() {
    local end_time=$(date +%s)
    local total_duration=$((end_time - START_TIME))
    local coverage_percentage=$1
    
    echo -e "${BLUE}Generating comprehensive test report...${NC}"
    
    local report_file="$MASTER_RESULTS_DIR/comprehensive-test-report.md"
    
    cat > "$report_file" << EOF
# Project Linter Integration Test Report

**Test Execution Date:** $(date)  
**Total Duration:** ${total_duration} seconds  
**Target Coverage:** ${COVERAGE_TARGET}%  
**Achieved Coverage:** ${coverage_percentage}%  
**Status:** $([ $coverage_percentage -ge $COVERAGE_TARGET ] && echo "✅ PASSED" || echo "❌ FAILED")

## Executive Summary

This comprehensive integration test suite validates the Project Linter system across all major functionality areas. The test suite includes:

- Core linting functionality for Java, TypeScript, YAML, and Markdown
- Performance and scalability testing
- Workflow integration testing (IDE, CI/CD, pre-commit hooks)
- End-to-end testing of complete linting workflows
- Error handling and recovery testing
- Security linting and vulnerability detection

## Test Suite Results

| Test Suite | Status | Duration | Coverage Areas |
|------------|--------|----------|----------------|
EOF
    
    # Add test suite results
    for suite in "${!TEST_SUITES[@]}"; do
        local status="${SUITE_STATUS[$suite]:-NOT_RUN}"
        local status_icon
        case $status in
            "PASSED") status_icon="✅" ;;
            "FAILED") status_icon="❌" ;;
            *) status_icon="⚠️" ;;
        esac
        
        echo "| $suite | $status_icon $status | - | Multiple |" >> "$report_file"
    done
    
    cat >> "$report_file" << EOF

## Coverage Analysis

### Functional Areas Tested

EOF
    
    # Add coverage details
    local areas=(
        "java_linting:Java Code Quality (Checkstyle, SpotBugs, PMD)"
        "frontend_linting:Frontend Code Quality (ESLint, Prettier, TypeScript)"
        "config_linting:Configuration Validation (YAML, JSON, Docker)"
        "doc_linting:Documentation Quality (Markdown, Links, Spelling)"
        "security_linting:Security Analysis (OWASP, Secrets Detection)"
        "incremental_linting:Incremental Processing and Caching"
        "cache_management:Performance Optimization"
        "ide_integration:Development Environment Integration"
        "ci_cd_integration:Continuous Integration Pipeline"
        "reporting:Quality Metrics and Reporting"
        "error_handling:Error Recovery and Resilience"
        "performance:Scalability and Performance"
        "service_overrides:Service-Specific Configurations"
        "pre_commit_hooks:Git Hook Integration"
        "quality_gates:Quality Gate Enforcement"
        "workflow_integration:End-to-End Developer Workflows"
        "end_to_end:Complete System Testing"
    )
    
    for area_desc in "${areas[@]}"; do
        local area="${area_desc%%:*}"
        local description="${area_desc##*:}"
        
        local area_covered=false
        for suite in "${!TEST_SUITES[@]}"; do
            if [ "${SUITE_COVERAGE["${suite}_${area}"]:-0}" -eq 1 ]; then
                area_covered=true
                break
            fi
        done
        
        if [ "$area_covered" = true ]; then
            echo "- ✅ **$description**" >> "$report_file"
        else
            echo "- ❌ **$description**" >> "$report_file"
        fi
    done
    
    cat >> "$report_file" << EOF

## Performance Metrics

$([ -f ".linting/test-results/performance/large-codebase-performance.txt" ] && echo "### Large Codebase Performance" && cat ".linting/test-results/performance/large-codebase-performance.txt" || echo "Performance metrics not available")

## Quality Gate Results

$([ -f ".linting/test-results/workflow/quality-gate-report.txt" ] && echo "### Quality Gate Analysis" && cat ".linting/test-results/workflow/quality-gate-report.txt" || echo "Quality gate results not available")

## Recommendations

EOF
    
    if [ $coverage_percentage -ge $COVERAGE_TARGET ]; then
        cat >> "$report_file" << EOF
✅ **Coverage Target Achieved**: The linting system has achieved ${coverage_percentage}% coverage, exceeding the target of ${COVERAGE_TARGET}%.

### Next Steps:
- Deploy the linting system to production
- Monitor performance metrics in real-world usage
- Gather developer feedback for continuous improvement
- Consider expanding coverage to additional file types or quality checks
EOF
    else
        local missing_coverage=$((COVERAGE_TARGET - coverage_percentage))
        cat >> "$report_file" << EOF
❌ **Coverage Target Not Met**: The linting system achieved ${coverage_percentage}% coverage, falling short of the ${COVERAGE_TARGET}% target by ${missing_coverage}%.

### Required Actions:
- Review failed test suites and address issues
- Implement missing functionality areas
- Enhance existing test coverage
- Re-run integration tests after improvements
EOF
    fi
    
    cat >> "$report_file" << EOF

## Test Artifacts

- **Master Results Directory:** \`$MASTER_RESULTS_DIR\`
- **Individual Suite Logs:** Available in respective test result directories
- **Coverage Report:** \`$MASTER_RESULTS_DIR/master-coverage-report.json\`

---

*Report generated by Project Linter Master Integration Test Suite*
EOF
    
    echo -e "${GREEN}✅ Comprehensive test report generated: $report_file${NC}"
}

# Main execution function
main() {
    initialize_master_tests
    
    echo -e "${YELLOW}Starting comprehensive integration test execution...${NC}"
    echo ""
    
    # Run all test suites
    for suite_name in "${!TEST_SUITES[@]}"; do
        run_test_suite "$suite_name" "${TEST_SUITES[$suite_name]}"
    done
    
    # Calculate and display coverage
    calculate_coverage
    local coverage_result=$?
    
    # Generate comprehensive report
    generate_master_report $coverage_result
    
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║                    FINAL RESULTS                            ║${NC}"
    echo -e "${CYAN}╠══════════════════════════════════════════════════════════════╣${NC}"
    
    local passed_suites=0
    local total_suites=${#TEST_SUITES[@]}
    
    for suite in "${!SUITE_STATUS[@]}"; do
        if [ "${SUITE_STATUS[$suite]}" = "PASSED" ]; then
            passed_suites=$((passed_suites + 1))
        fi
    done
    
    echo -e "${CYAN}║ Test Suites Passed: $(printf "%2d" $passed_suites)/$(printf "%-2d" $total_suites)                              ║${NC}"
    echo -e "${CYAN}║ Coverage Achieved: $(printf "%3d" $coverage_result)%%                                ║${NC}"
    echo -e "${CYAN}║ Total Duration: $(printf "%4d" $(($(date +%s) - START_TIME)))s                                   ║${NC}"
    
    if [ $coverage_result -ge $COVERAGE_TARGET ] && [ $passed_suites -eq $total_suites ]; then
        echo -e "${CYAN}║ Overall Status: ${GREEN}✅ SUCCESS${CYAN}                               ║${NC}"
        local exit_code=0
    else
        echo -e "${CYAN}║ Overall Status: ${RED}❌ FAILURE${CYAN}                               ║${NC}"
        local exit_code=1
    fi
    
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    
    echo ""
    echo -e "${BLUE}📊 Detailed report available at: $MASTER_RESULTS_DIR/comprehensive-test-report.md${NC}"
    
    exit $exit_code
}

# Execute main function
main "$@"