#!/bin/bash

# Interactive Test Runner for MCP Project
# Provides an easy way to run different types of tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Test results tracking
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0
START_TIME=$(date +%s)

print_header() {
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_menu() {
    echo -e "${CYAN}Available Test Options:${NC}"
    echo
    echo "1) 🚀 Quick Unit Tests (< 30 seconds)"
    echo "2) 🔧 All Unit Tests"
    echo "3) 🌐 Integration Tests"
    echo "4) 🎭 Contract Tests"
    echo "5) 🔐 Security Tests"
    echo "6) ⚡ Performance Tests"
    echo "7) 🎨 UI Tests"
    echo "8) 🔄 End-to-End Tests"
    echo "9) 📊 Coverage Report"
    echo "10) 🧹 Clean & Test All"
    echo
    echo "Module-specific tests:"
    echo "11) Common Module Tests"
    echo "12) Security Module Tests"
    echo "13) Organization Module Tests"
    echo "14) LLM Module Tests"
    echo "15) Controller Module Tests"
    echo "16) RAG Module Tests"
    echo "17) Gateway Module Tests"
    echo
    echo "Special options:"
    echo "18) 🔧 Fix failing tests (auto-retry with debugging)"
    echo "19) 📈 Test metrics and timing"
    echo "20) 🏷️  Tag-based test selection"
    echo
    echo "0) Exit"
    echo
}

run_maven_test() {
    local test_profile="$1"
    local modules="$2"
    local additional_args="$3"
    local description="$4"
    
    print_info "Running $description..."
    
    cd "$PROJECT_ROOT"
    
    local mvn_cmd="mvn test"
    
    if [ ! -z "$test_profile" ]; then
        mvn_cmd="$mvn_cmd -P$test_profile"
    fi
    
    if [ ! -z "$modules" ]; then
        mvn_cmd="$mvn_cmd -pl $modules"
    fi
    
    if [ ! -z "$additional_args" ]; then
        mvn_cmd="$mvn_cmd $additional_args"
    fi
    
    echo "Executing: $mvn_cmd"
    
    if eval $mvn_cmd; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        print_success "$description completed successfully"
        return 0
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        print_error "$description failed"
        return 1
    fi
}

run_quick_tests() {
    print_header "Quick Unit Tests"
    run_maven_test "" "mcp-common,mcp-security" "-Dtest=*Test -Dgroups=fast" "Quick unit tests"
}

run_unit_tests() {
    print_header "All Unit Tests"
    run_maven_test "" "" "-Dtest=*Test -DexcludedGroups=integration,e2e,performance" "All unit tests"
}

run_integration_tests() {
    print_header "Integration Tests"
    
    # Check if test containers are available
    print_info "Checking test environment..."
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Integration tests require Docker."
        return 1
    fi
    
    run_maven_test "" "" "-Dtest=*IT,*IntegrationTest -Dgroups=integration" "Integration tests"
}

run_contract_tests() {
    print_header "Contract Tests"
    run_maven_test "" "" "-Dtest=*ContractTest -Dgroups=contract" "Contract tests"
}

run_security_tests() {
    print_header "Security Tests"
    run_maven_test "security" "mcp-security" "-Dtest=*SecurityTest" "Security tests"
}

run_performance_tests() {
    print_header "Performance Tests"
    print_warning "Performance tests may take several minutes..."
    run_maven_test "" "" "-Dtest=*PerformanceTest -Dgroups=performance" "Performance tests"
}

run_ui_tests() {
    print_header "UI Tests"
    
    if [ ! -d "$PROJECT_ROOT/debate-ui" ]; then
        print_error "UI directory not found"
        return 1
    fi
    
    cd "$PROJECT_ROOT/debate-ui"
    
    if npm test -- --watchAll=false; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        print_success "UI tests completed successfully"
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        print_error "UI tests failed"
    fi
    
    cd "$PROJECT_ROOT"
}

run_e2e_tests() {
    print_header "End-to-End Tests"
    
    print_info "Starting services for E2E tests..."
    if ! make start >/dev/null 2>&1; then
        print_error "Failed to start services"
        return 1
    fi
    
    sleep 10  # Wait for services to be ready
    
    run_maven_test "" "" "-Dtest=*E2ETest -Dgroups=e2e" "End-to-end tests"
    
    print_info "Cleaning up test services..."
    make stop >/dev/null 2>&1 || true
}

run_coverage_report() {
    print_header "Coverage Report"
    
    cd "$PROJECT_ROOT"
    
    print_info "Running tests with coverage..."
    if mvn clean test jacoco:report; then
        print_success "Coverage report generated"
        
        # Try to find and display coverage summary
        if [ -f "target/site/jacoco/index.html" ]; then
            print_info "Coverage report available at: file://$PROJECT_ROOT/target/site/jacoco/index.html"
        fi
        
        # Look for coverage percentage
        local coverage_files=$(find . -name "jacoco.xml" 2>/dev/null)
        if [ ! -z "$coverage_files" ]; then
            print_info "Jacoco XML reports generated"
        fi
        
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        print_error "Coverage report generation failed"
    fi
}

run_clean_test_all() {
    print_header "Clean & Test All"
    
    cd "$PROJECT_ROOT"
    
    print_info "Cleaning project..."
    if mvn clean; then
        print_success "Project cleaned"
    else
        print_error "Clean failed"
        return 1
    fi
    
    print_info "Running all tests..."
    run_maven_test "" "" "" "Full test suite"
}

run_module_tests() {
    local module="$1"
    local module_name="$2"
    
    print_header "$module_name Module Tests"
    run_maven_test "" "$module" "" "$module_name tests"
}

fix_failing_tests() {
    print_header "Test Debugging Mode"
    
    print_info "Running tests in debug mode with detailed output..."
    
    cd "$PROJECT_ROOT"
    
    # Run with maximum verbosity and debugging
    mvn test -X -e -Dsurefire.printSummary=true -Dmaven.test.failure.ignore=true 2>&1 | tee test-debug.log
    
    print_info "Test debug log saved to test-debug.log"
    print_info "Analyzing common failure patterns..."
    
    # Check for common issues
    if grep -q "java.lang.OutOfMemoryError" test-debug.log; then
        print_warning "Memory issues detected. Try: export MAVEN_OPTS='-Xmx2048m'"
    fi
    
    if grep -q "Connection refused" test-debug.log; then
        print_warning "Connection issues detected. Check if test services are running."
    fi
    
    if grep -q "Port already in use" test-debug.log; then
        print_warning "Port conflicts detected. Try stopping other services."
    fi
}

show_test_metrics() {
    print_header "Test Metrics & Timing"
    
    cd "$PROJECT_ROOT"
    
    print_info "Analyzing test execution times..."
    
    # Run tests with timing
    mvn test -Dsurefire.reportFormat=xml 2>/dev/null || true
    
    # Parse test reports
    local test_reports=$(find . -name "TEST-*.xml" 2>/dev/null | head -10)
    
    if [ ! -z "$test_reports" ]; then
        echo -e "${CYAN}Test Timing Summary:${NC}"
        
        for report in $test_reports; do
            if [ -f "$report" ]; then
                local class_name=$(basename "$report" .xml | sed 's/TEST-//')
                local time=$(grep -o 'time="[^"]*"' "$report" | head -1 | sed 's/time="//;s/"//')
                echo "  $class_name: ${time}s"
            fi
        done
    else
        print_warning "No test reports found"
    fi
}

tag_based_tests() {
    print_header "Tag-based Test Selection"
    
    echo "Available tags:"
    echo "  fast, slow, integration, unit, security, performance"
    echo
    read -p "Enter tags (comma-separated): " tags
    
    if [ ! -z "$tags" ]; then
        run_maven_test "" "" "-Dgroups=$tags" "Tagged tests ($tags)"
    fi
}

show_summary() {
    local end_time=$(date +%s)
    local duration=$((end_time - START_TIME))
    
    print_header "Test Session Summary"
    
    echo -e "${CYAN}Tests Run: $TESTS_RUN${NC}"
    echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
    echo -e "${RED}Failed: $TESTS_FAILED${NC}"
    echo -e "${BLUE}Duration: ${duration}s${NC}"
    
    if [ $TESTS_FAILED -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed!${NC}"
    else
        echo -e "${RED}❌ Some tests failed${NC}"
    fi
}

interactive_mode() {
    while true; do
        print_header "MCP Test Runner"
        print_menu
        
        read -p "Select an option (0-20): " choice
        
        TESTS_RUN=$((TESTS_RUN + 1))
        
        case $choice in
            1) run_quick_tests ;;
            2) run_unit_tests ;;
            3) run_integration_tests ;;
            4) run_contract_tests ;;
            5) run_security_tests ;;
            6) run_performance_tests ;;
            7) run_ui_tests ;;
            8) run_e2e_tests ;;
            9) run_coverage_report ;;
            10) run_clean_test_all ;;
            11) run_module_tests "mcp-common" "Common" ;;
            12) run_module_tests "mcp-security" "Security" ;;
            13) run_module_tests "mcp-organization" "Organization" ;;
            14) run_module_tests "mcp-llm" "LLM" ;;
            15) run_module_tests "mcp-controller" "Controller" ;;
            16) run_module_tests "mcp-rag" "RAG" ;;
            17) run_module_tests "mcp-gateway" "Gateway" ;;
            18) fix_failing_tests ;;
            19) show_test_metrics ;;
            20) tag_based_tests ;;
            0) 
                show_summary
                print_info "Goodbye!"
                exit 0
                ;;
            *) 
                print_error "Invalid option"
                ;;
        esac
        
        echo
        read -p "Press Enter to continue..."
        echo
    done
}

# Main execution
if [ "$1" == "--interactive" ] || [ "$1" == "-i" ] || [ $# -eq 0 ]; then
    interactive_mode
else
    # Direct command execution
    case $1 in
        "quick") run_quick_tests ;;
        "unit") run_unit_tests ;;
        "integration") run_integration_tests ;;
        "security") run_security_tests ;;
        "ui") run_ui_tests ;;
        "e2e") run_e2e_tests ;;
        "coverage") run_coverage_report ;;
        "all") run_clean_test_all ;;
        *) 
            echo "Usage: $0 [quick|unit|integration|security|ui|e2e|coverage|all|--interactive]"
            exit 1
            ;;
    esac
    
    show_summary
fi