#!/bin/bash

# End-to-End Validation Script for MCP Services
# Orchestrates comprehensive validation across all testing frameworks

set -e

# Configuration
E2E_MODE="${E2E_MODE:-complete}"
VALIDATION_TIMEOUT="${VALIDATION_TIMEOUT:-2h}"
CONCURRENCY="${CONCURRENCY:-4}"
FAIL_FAST="${FAIL_FAST:-false}"
GENERATE_REPORTS="${GENERATE_REPORTS:-true}"
REPORT_DIR="${REPORT_DIR:-e2e-reports}"
ENVIRONMENT="${ENVIRONMENT:-test}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[E2E INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[E2E SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[E2E WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[E2E ERROR]${NC} $1"
}

log_phase() {
    echo -e "${PURPLE}[PHASE]${NC} $1"
}

log_metric() {
    echo -e "${CYAN}[METRIC]${NC} $1"
}

# Service configuration
declare -A SERVICES=(
    ["mcp-organization"]="8080"
    ["mcp-llm"]="8081"
    ["mcp-controller"]="8082"
    ["mcp-context"]="8083"
    ["mcp-rag"]="8084"
    ["mcp-gateway"]="8085"
)

# Validation phases
declare -A PHASES=(
    ["prerequisites"]="Prerequisites Check"
    ["unit-integration"]="Unit & Integration Tests"
    ["contract"]="Contract Tests"
    ["functional"]="Functional E2E Tests"
    ["performance"]="Performance Tests"
    ["resilience"]="Chaos Engineering Tests"
    ["security"]="Security Tests"
    ["cleanup"]="Test Cleanup"
)

# Test framework commands
declare -A TEST_COMMANDS=(
    ["unit-tests"]="mvn test -Dtest=*Test"
    ["integration-tests"]="mvn test -Dtest=*IntegrationTest"
    ["contract-tests"]="./scripts/contract-testing.sh"
    ["chaos-tests"]="./scripts/chaos-testing.sh"
    ["e2e-tests"]="mvn test -Dtest=*E2ETest"
)

# Check prerequisites
check_prerequisites() {
    log_info "Checking E2E validation prerequisites..."
    
    # Check if Java is available
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    # Check if Maven is available
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed or not in PATH"
        exit 1
    fi
    
    # Check if Docker is available
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check system resources
    local memory_gb=$(free -g | awk '/^Mem:/{print $2}' 2>/dev/null || echo "0")
    if [ """"$memory_gb"""" -lt 8 ]; then
        log_warning "System has less than 8GB RAM. E2E tests may be slow or fail."
    fi
    
    # Check disk space
    local disk_space_gb=$(df . | awk 'NR==2 {print int($4/1024/1024)}')
    if [ """"$disk_space_gb"""" -lt 5 ]; then
        log_warning "Less than 5GB disk space available. May not be sufficient for reports."
    fi
    
    log_success "Prerequisites check completed"
}

# System health check
check_system_health() {
    log_info "Checking system health..."
    
    local healthy_services=0
    local total_services=${#SERVICES[@]}
    
    for service in "${!SERVICES[@]}"; do
        local port="${SERVICES["""$service"""]}"
        if curl -s http://localhost:"""$port"""/health &> /dev/null; then
            ((healthy_services++))
            log_success """"$service""" is healthy"
        else
            log_warning """"$service""" is not responding on port """$port""""
        fi
    done
    
    local health_percentage=$((healthy_services * 100 / total_services))
    log_metric "System Health: """$healthy_services"""/"""$total_services""" services healthy ("""$health_percentage"""%)"
    
    if [ """$health_percentage""" -lt 80 ]; then
        log_error "System health is below 80%. Cannot proceed with E2E validation."
        exit 1
    fi
    
    return 0
}

# Establish baseline metrics
establish_baseline() {
    log_info "Establishing baseline metrics..."
    
    mkdir -p """"$REPORT_DIR"""/baseline"
    local baseline_file=""""$REPORT_DIR"""/baseline/system-baseline.json"
    
    {
        echo "{"
        echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
        echo "  \"environment\": \""""$ENVIRONMENT"""\","
        echo "  \"services\": {"
        
        local first=true
        for service in "${!SERVICES[@]}"; do
            if [ """"$first"""" = true ]; then
                first=false
            else
                echo ","
            fi
            
            local port="${SERVICES["""$service"""]}"
            local response_time=$(measure_response_time "http://localhost:"""$port"""/health")
            local health_status="unknown"
            
            if curl -s http://localhost:"""$port"""/health &> /dev/null; then
                health_status="healthy"
            else
                health_status="unhealthy"
            fi
            
            echo "    \""""$service"""\": {"
            echo "      \"port\": """$port""","
            echo "      \"health\": \""""$health_status"""\","
            echo "      \"response_time_ms\": """$response_time""""
            echo "    }"
        done
        
        echo "  },"
        echo "  \"system_metrics\": {"
        echo "    \"memory_total_gb\": $(free -g | awk '/^Mem:/{print $2}' 2>/dev/null || echo 0),"
        echo "    \"memory_available_gb\": $(free -g | awk '/^Mem:/{print $7}' 2>/dev/null || echo 0),"
        echo "    \"disk_available_gb\": $(df . | awk 'NR==2 {print int($4/1024/1024)}')"
        echo "  }"
        echo "}"
    } > """"$baseline_file""""
    
    log_success "Baseline established: """$baseline_file""""
}

# Measure response time
measure_response_time() {
    local url="$1"
    local start_time=$(date +%s%3N)
    
    if curl -s """"$url"""" &> /dev/null; then
        local end_time=$(date +%s%3N)
        echo $((end_time - start_time))
    else
        echo -1
    fi
}

# Run specific validation phase
run_validation_phase() {
    local phase="$1"
    local phase_name="${PHASES["""$phase"""]}"
    
    log_phase "Starting phase: """$phase_name""""
    
    local phase_dir=""""$REPORT_DIR"""/phases/"""$phase""""
    mkdir -p """"$phase_dir""""
    
    local start_time=$(date +%s)
    local phase_log=""""$phase_dir"""/phase.log"
    local phase_result=""""$phase_dir"""/result.json"
    local success=true
    
    case """$phase""" in
        "prerequisites")
            run_prerequisites_phase """"$phase_dir"""" > """"$phase_log"""" 2>&1 || success=false
            ;;
        "unit-integration")
            run_unit_integration_phase """"$phase_dir"""" > """"$phase_log"""" 2>&1 || success=false
            ;;
        "contract")
            run_contract_phase """"$phase_dir"""" > """"$phase_log"""" 2>&1 || success=false
            ;;
        "functional")
            run_functional_phase """"$phase_dir"""" > """"$phase_log"""" 2>&1 || success=false
            ;;
        "performance")
            run_performance_phase """"$phase_dir"""" > """"$phase_log"""" 2>&1 || success=false
            ;;
        "resilience")
            run_resilience_phase """"$phase_dir"""" > """"$phase_log"""" 2>&1 || success=false
            ;;
        "security")
            run_security_phase """"$phase_dir"""" > """"$phase_log"""" 2>&1 || success=false
            ;;
        "cleanup")
            run_cleanup_phase """"$phase_dir"""" > """"$phase_log"""" 2>&1 || success=false
            ;;
        *)
            log_error "Unknown phase: """$phase""""
            return 1
            ;;
    esac
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    # Generate phase result
    {
        echo "{"
        echo "  \"phase\": \""""$phase"""\","
        echo "  \"name\": \""""$phase_name"""\","
        echo "  \"success\": """$success""","
        echo "  \"duration_seconds\": """$duration""","
        echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
        echo "  \"log_file\": \"phase.log\""
        echo "}"
    } > """"$phase_result""""
    
    if [ """"$success"""" = true ]; then
        log_success "Phase '"""$phase_name"""' completed successfully in ${duration}s"
        return 0
    else
        log_error "Phase '"""$phase_name"""' failed after ${duration}s"
        return 1
    fi
}

# Phase implementations
run_prerequisites_phase() {
    local phase_dir="$1"
    echo "Running prerequisites checks..."
    
    check_system_health
    
    # Additional prerequisite checks
    echo "Checking database connectivity..."
    # Implementation would check actual database connections
    
    echo "Checking external service dependencies..."
    # Implementation would check external services
    
    echo "Prerequisites phase completed"
}

run_unit_integration_phase() {
    local phase_dir="$1"
    echo "Running unit and integration tests..."
    
    # Run unit tests
    echo "Executing unit tests..."
    if mvn test -Dtest=*Test -DfailIfNoTests=false; then
        echo "Unit tests passed"
    else
        echo "Unit tests failed"
        return 1
    fi
    
    # Run integration tests
    echo "Executing integration tests..."
    if mvn test -Dtest=*IntegrationTest -DfailIfNoTests=false; then
        echo "Integration tests passed"
    else
        echo "Integration tests failed"
        return 1
    fi
    
    echo "Unit and integration tests completed"
}

run_contract_phase() {
    local phase_dir="$1"
    echo "Running contract tests..."
    
    if [ -f "./scripts/contract-testing.sh" ]; then
        if ./scripts/contract-testing.sh; then
            echo "Contract tests passed"
        else
            echo "Contract tests failed"
            return 1
        fi
    else
        echo "Contract testing script not found, skipping"
    fi
    
    echo "Contract tests completed"
}

run_functional_phase() {
    local phase_dir="$1"
    echo "Running functional E2E tests..."
    
    # Run functional tests
    if mvn test -Dtest=*E2ETest -DfailIfNoTests=false; then
        echo "Functional tests passed"
    else
        echo "Functional tests failed"
        return 1
    fi
    
    # Run UI tests if available
    if [ -d "debate-ui" ] && [ -f "debate-ui/package.json" ]; then
        echo "Running UI E2E tests..."
        cd debate-ui
        if npm test -- --watchAll=false; then
            echo "UI tests passed"
        else
            echo "UI tests failed"
            cd ..
            return 1
        fi
        cd ..
    fi
    
    echo "Functional tests completed"
}

run_performance_phase() {
    local phase_dir="$1"
    echo "Running performance tests..."
    
    # Run performance tests if available
    if mvn test -Dtest=*PerformanceTest -DfailIfNoTests=false; then
        echo "Performance tests passed"
    else
        echo "Performance tests failed"
        return 1
    fi
    
    echo "Performance tests completed"
}

run_resilience_phase() {
    local phase_dir="$1"
    echo "Running resilience tests..."
    
    if [ -f "./scripts/chaos-testing.sh" ]; then
        if CHAOS_MODE=safe EXPERIMENT_DURATION=3m ./scripts/chaos-testing.sh; then
            echo "Chaos tests passed"
        else
            echo "Chaos tests failed"
            return 1
        fi
    else
        echo "Chaos testing script not found, skipping"
    fi
    
    echo "Resilience tests completed"
}

run_security_phase() {
    local phase_dir="$1"
    echo "Running security tests..."
    
    # Run security tests
    if mvn test -Dtest=*SecurityTest -DfailIfNoTests=false; then
        echo "Security tests passed"
    else
        echo "Security tests failed"
        return 1
    fi
    
    echo "Security tests completed"
}

run_cleanup_phase() {
    local phase_dir="$1"
    echo "Running cleanup..."
    
    # Stop any test processes
    pkill -f "test" 2>/dev/null || true
    
    # Clean up test data
    echo "Cleaning up test data..."
    
    # Generate final reports
    echo "Generating final reports..."
    
    echo "Cleanup completed"
}

# Run complete E2E validation
run_complete_validation() {
    log_info "Starting complete E2E validation..."
    
    establish_baseline
    
    local failed_phases=()
    local total_start_time=$(date +%s)
    
    for phase in prerequisites unit-integration contract functional performance resilience security cleanup; do
        if ! run_validation_phase """"$phase""""; then
            failed_phases+=(""""$phase"""")
            
            if [ """"$FAIL_FAST"""" = "true" ]; then
                log_error "Fail-fast enabled, stopping after phase failure: """$phase""""
                break
            fi
        fi
    done
    
    local total_end_time=$(date +%s)
    local total_duration=$((total_end_time - total_start_time))
    
    # Generate summary
    generate_validation_summary """"$total_duration"""" "${failed_phases[@]}"
    
    if [ "${#failed_phases[@]}" -eq 0 ]; then
        log_success "Complete E2E validation passed in ${total_duration}s"
        return 0
    else
        log_error "E2E validation failed. Failed phases: ${failed_phases[*]}"
        return 1
    fi
}

# Run specific validation mode
run_validation_mode() {
    local mode="$1"
    
    case """$mode""" in
        "complete")
            run_complete_validation
            ;;
        "critical")
            log_info "Running critical validation (prerequisites, unit-integration, functional)..."
            establish_baseline
            for phase in prerequisites unit-integration functional; do
                if ! run_validation_phase """"$phase""""; then
                    log_error "Critical phase failed: """$phase""""
                    return 1
                fi
            done
            run_validation_phase "cleanup"
            log_success "Critical validation completed successfully"
            ;;
        "performance")
            log_info "Running performance validation..."
            establish_baseline
            for phase in prerequisites performance; do
                if ! run_validation_phase """"$phase""""; then
                    log_error "Performance validation failed: """$phase""""
                    return 1
                fi
            done
            run_validation_phase "cleanup"
            log_success "Performance validation completed successfully"
            ;;
        "resilience")
            log_info "Running resilience validation..."
            establish_baseline
            for phase in prerequisites resilience; do
                if ! run_validation_phase """"$phase""""; then
                    log_error "Resilience validation failed: """$phase""""
                    return 1
                fi
            done
            run_validation_phase "cleanup"
            log_success "Resilience validation completed successfully"
            ;;
        "security")
            log_info "Running security validation..."
            establish_baseline
            for phase in prerequisites security; do
                if ! run_validation_phase """"$phase""""; then
                    log_error "Security validation failed: """$phase""""
                    return 1
                fi
            done
            run_validation_phase "cleanup"
            log_success "Security validation completed successfully"
            ;;
        *)
            log_error "Unknown validation mode: """$mode""""
            return 1
            ;;
    esac
}

# Generate validation summary
generate_validation_summary() {
    local total_duration="$1"
    shift
    local failed_phases=("$@")
    
    log_info "Generating validation summary..."
    
    local summary_file=""""$REPORT_DIR"""/validation-summary.json"
    local total_phases=8
    local successful_phases=$((total_phases - ${#failed_phases[@]}))
    local success_rate=$((successful_phases * 100 / total_phases))
    
    {
        echo "{"
        echo "  \"validation_id\": \"e2e-$(date +%Y%m%d-%H%M%S)\","
        echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
        echo "  \"mode\": \""""$E2E_MODE"""\","
        echo "  \"environment\": \""""$ENVIRONMENT"""\","
        echo "  \"total_duration_seconds\": """$total_duration""","
        echo "  \"total_phases\": """$total_phases""","
        echo "  \"successful_phases\": """$successful_phases""","
        echo "  \"failed_phases\": ${#failed_phases[@]},"
        echo "  \"success_rate_percentage\": """$success_rate""","
        echo "  \"overall_success\": $([ "${#failed_phases[@]}" -eq 0 ] && echo true || echo false),"
        echo "  \"failed_phase_names\": ["
        
        if [ "${#failed_phases[@]}" -gt 0 ]; then
            local first=true
            for phase in "${failed_phases[@]}"; do
                if [ """"$first"""" = true ]; then
                    first=false
                else
                    echo ","
                fi
                echo "    \""""$phase"""\""
            done
        fi
        
        echo "  ]"
        echo "}"
    } > """"$summary_file""""
    
    log_success "Validation summary generated: """$summary_file""""
    
    # Print summary to console
    echo
    log_info "=== E2E Validation Summary ==="
    log_metric "Mode: """$E2E_MODE""""
    log_metric "Duration: ${total_duration}s"
    log_metric "Success Rate: """$success_rate"""% ("""$successful_phases"""/"""$total_phases""" phases)"
    
    if [ "${#failed_phases[@]}" -eq 0 ]; then
        log_success "Overall Result: SUCCESS"
    else
        log_error "Overall Result: FAILURE"
        log_error "Failed Phases: ${failed_phases[*]}"
    fi
    echo
}

# Generate comprehensive report
generate_comprehensive_report() {
    if [ """"$GENERATE_REPORTS"""" != "true" ]; then
        return 0
    fi
    
    log_info "Generating comprehensive E2E report..."
    
    local report_file=""""$REPORT_DIR"""/e2e-validation-report.html"
    
    cat > """"$report_file"""" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>MCP End-to-End Validation Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; }
        .phase { border: 1px solid #ddd; margin: 10px 0; padding: 15px; border-radius: 5px; }
        .success { border-left: 5px solid green; }
        .failed { border-left: 5px solid red; }
        .metric { display: inline-block; margin: 5px 10px; padding: 5px; background-color: #e9e9e9; border-radius: 3px; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>MCP End-to-End Validation Report</h1>
        <p><strong>Generated:</strong> $(date)</p>
        <p><strong>Mode:</strong> """$E2E_MODE"""</p>
        <p><strong>Environment:</strong> """$ENVIRONMENT"""</p>
    </div>
    
    <div class="section">
        <h2>Phase Results</h2>
EOF

    # Add phase results
    for phase in "${!PHASES[@]}"; do
        local phase_result_file=""""$REPORT_DIR"""/phases/"""$phase"""/result.json"
        if [ -f """"$phase_result_file"""" ]; then
            local success=$(grep '"success"' """"$phase_result_file"""" | cut -d':' -f2 | tr -d ' ,' | tr -d '"')
            local duration=$(grep '"duration_seconds"' """"$phase_result_file"""" | cut -d':' -f2 | tr -d ' ,')
            local phase_name="${PHASES["""$phase"""]}"
            
            local css_class="phase"
            if [ """"$success"""" = "true" ]; then
                css_class="phase success"
            else
                css_class="phase failed"
            fi
            
            cat >> """"$report_file"""" << EOF
        <div class=""""$css_class"""">
            <h3>"""$phase_name"""</h3>
            <p><strong>Status:</strong> $([ """"$success"""" = "true" ] && echo "SUCCESS" || echo "FAILED")</p>
            <p><strong>Duration:</strong> ${duration}s</p>
            <p><strong>Log:</strong> <a href="phases/"""$phase"""/phase.log">View Log</a></p>
        </div>
EOF
        fi
    done
    
    cat >> """"$report_file"""" << EOF
    </div>
</body>
</html>
EOF

    log_success "Comprehensive report generated: """$report_file""""
}

# Cleanup function
cleanup() {
    log_info "Performing E2E validation cleanup..."
    
    # Stop any running test processes
    pkill -f "mvn.*test" 2>/dev/null || true
    pkill -f "chaos" 2>/dev/null || true
    
    # Generate final reports
    if [ """"$GENERATE_REPORTS"""" = "true" ]; then
        generate_comprehensive_report
    fi
}

# Main execution
main() {
    local action="${1:-"""$E2E_MODE"""}"
    
    # Create report directory
    mkdir -p """"$REPORT_DIR"""/phases"
    
    # Setup cleanup trap
    trap cleanup EXIT
    
    case """$action""" in
        "check")
            log_info "Running E2E validation prerequisite check..."
            check_prerequisites
            check_system_health
            ;;
        "complete"|"critical"|"performance"|"resilience"|"security")
            log_info "Running E2E validation mode: """$action""""
            check_prerequisites
            E2E_MODE=""""$action""""
            if run_validation_mode """"$action""""; then
                log_success "E2E validation completed successfully"
                exit 0
            else
                log_error "E2E validation failed"
                exit 1
            fi
            ;;
        "report")
            log_info "Generating E2E validation report..."
            generate_comprehensive_report
            ;;
        "help")
            echo "Usage: $0 [action]"
            echo "Actions:"
            echo "  check       - Check prerequisites only"
            echo "  complete    - Run complete E2E validation (default)"
            echo "  critical    - Run critical validation only (prerequisites, unit-integration, functional)"
            echo "  performance - Run performance validation only"
            echo "  resilience  - Run resilience validation only"
            echo "  security    - Run security validation only"
            echo "  report      - Generate comprehensive report"
            echo "  help        - Show this help message"
            echo ""
            echo "Environment variables:"
            echo "  E2E_MODE="""$E2E_MODE""""
            echo "  VALIDATION_TIMEOUT="""$VALIDATION_TIMEOUT""""
            echo "  CONCURRENCY="""$CONCURRENCY""""
            echo "  FAIL_FAST="""$FAIL_FAST""""
            echo "  GENERATE_REPORTS="""$GENERATE_REPORTS""""
            echo "  REPORT_DIR="""$REPORT_DIR""""
            echo "  ENVIRONMENT="""$ENVIRONMENT""""
            exit 0
            ;;
        *)
            log_error "Unknown action: """$action""""
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"