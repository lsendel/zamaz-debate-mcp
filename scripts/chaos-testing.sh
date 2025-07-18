#!/bin/bash

# Chaos Engineering Test Runner for MCP Services
# Orchestrates chaos experiments across the entire system

set -e

# Configuration
CHAOS_MODE="${CHAOS_MODE:-safe}"
EXPERIMENT_DURATION="${EXPERIMENT_DURATION:-5m}"
CONCURRENCY="${CONCURRENCY:-3}"
SAFETY_MODE="${SAFETY_MODE:-true}"
REPORT_DIR="${REPORT_DIR:-chaos-reports}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[CHAOS INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[CHAOS SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[CHAOS WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[CHAOS ERROR]${NC} $1"
}

log_experiment() {
    echo -e "${PURPLE}[EXPERIMENT]${NC} $1"
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

# Chaos experiment configurations
declare -A EXPERIMENTS=(
    ["database-failure"]="DatabaseFailureResilience"
    ["llm-provider-failure"]="LlmProviderFailure"
    ["network-partition"]="NetworkPartition"
    ["memory-pressure"]="MemoryPressure"
    ["cascading-failure"]="CascadingFailure"
    ["multi-tenant-stress"]="MultiTenantIsolationStress"
    ["realtime-failure"]="RealTimeCommunicationFailure"
    ["security-pressure"]="SecurityUnderPressure"
)

# Safety checks
check_safety_mode() {
    if [ "$SAFETY_MODE" = "false" ] && [ "$CHAOS_MODE" != "aggressive" ]; then
        log_warning "Safety mode is disabled but chaos mode is not aggressive"
        log_warning "This may cause unexpected system behavior"
        read -p "Continue? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "Chaos testing cancelled by user"
            exit 0
        fi
    fi
}

# Prerequisites check
check_prerequisites() {
    log_info "Checking chaos testing prerequisites..."
    
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
    
    # Check if services are running
    local services_down=0
    for service in "${!SERVICES[@]}"; do
        local port="${SERVICES[$service]}"
        if ! curl -s http://localhost:$port/health &> /dev/null; then
            log_warning "Service $service is not responding on port $port"
            ((services_down++))
        fi
    done
    
    if [ $services_down -gt 0 ]; then
        log_warning "$services_down services are not responding"
        if [ "$CHAOS_MODE" = "safe" ]; then
            log_error "In safe mode, all services must be running before chaos testing"
            exit 1
        fi
    fi
    
    log_success "Prerequisites check completed"
}

# System health baseline
establish_baseline() {
    log_info "Establishing system health baseline..."
    
    mkdir -p "$REPORT_DIR/baseline"
    
    # Collect initial metrics
    for service in "${!SERVICES[@]}"; do
        local port="${SERVICES[$service]}"
        local baseline_file="$REPORT_DIR/baseline/${service}-baseline.json"
        
        log_info "Collecting baseline for $service..."
        
        # Health check
        if curl -s http://localhost:$port/health > "$baseline_file.health" 2>/dev/null; then
            log_success "Baseline established for $service"
        else
            log_warning "Could not establish baseline for $service"
        fi
        
        # Performance metrics
        {
            echo "{"
            echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
            echo "  \"service\": \"$service\","
            echo "  \"port\": $port,"
            echo "  \"health_check_time\": $(measure_response_time http://localhost:$port/health),"
            echo "  \"memory_usage\": $(get_memory_usage $service),"
            echo "  \"cpu_usage\": $(get_cpu_usage $service)"
            echo "}"
        } > "$baseline_file"
    done
    
    log_success "System baseline established"
}

# Measure response time
measure_response_time() {
    local url="$1"
    local start_time=$(date +%s%3N)
    
    if curl -s "$url" &> /dev/null; then
        local end_time=$(date +%s%3N)
        echo $((end_time - start_time))
    else
        echo -1
    fi
}

# Get memory usage (simplified)
get_memory_usage() {
    local service="$1"
    # In a real implementation, this would get actual memory usage
    echo $((RANDOM % 1000 + 100))
}

# Get CPU usage (simplified)
get_cpu_usage() {
    local service="$1"
    # In a real implementation, this would get actual CPU usage
    echo $((RANDOM % 100))
}

# Run specific chaos experiment
run_experiment() {
    local experiment_name="$1"
    local experiment_class="${EXPERIMENTS[$experiment_name]}"
    
    if [ -z "$experiment_class" ]; then
        log_error "Unknown experiment: $experiment_name"
        return 1
    fi
    
    log_experiment "Starting experiment: $experiment_name"
    
    local experiment_dir="$REPORT_DIR/experiments/$experiment_name"
    mkdir -p "$experiment_dir"
    
    local start_time=$(date +%s)
    local log_file="$experiment_dir/experiment.log"
    
    # Run the chaos experiment
    if cd mcp-controller && mvn test -Dtest="*Chaos*" \
        -Dchaos.experiment="$experiment_name" \
        -Dchaos.duration="$EXPERIMENT_DURATION" \
        -Dchaos.concurrency="$CONCURRENCY" \
        -Dchaos.safety-mode="$SAFETY_MODE" \
        > "../$log_file" 2>&1; then
        
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        log_success "Experiment $experiment_name completed successfully in ${duration}s"
        
        # Generate experiment report
        generate_experiment_report "$experiment_name" "$duration" "SUCCESS"
        return 0
    else
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        log_error "Experiment $experiment_name failed after ${duration}s"
        
        # Generate experiment report
        generate_experiment_report "$experiment_name" "$duration" "FAILED"
        return 1
    fi
    
    cd ..
}

# Generate experiment report
generate_experiment_report() {
    local experiment_name="$1"
    local duration="$2"
    local status="$3"
    
    local experiment_dir="$REPORT_DIR/experiments/$experiment_name"
    local report_file="$experiment_dir/report.json"
    
    {
        echo "{"
        echo "  \"experiment\": \"$experiment_name\","
        echo "  \"status\": \"$status\","
        echo "  \"duration_seconds\": $duration,"
        echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
        echo "  \"chaos_mode\": \"$CHAOS_MODE\","
        echo "  \"safety_mode\": $SAFETY_MODE,"
        echo "  \"system_impact\": {"
        
        # Collect post-experiment system state
        local first=true
        for service in "${!SERVICES[@]}"; do
            if [ "$first" = true ]; then
                first=false
            else
                echo ","
            fi
            
            local port="${SERVICES[$service]}"
            local health_status="unknown"
            local response_time=-1
            
            if curl -s http://localhost:$port/health &> /dev/null; then
                health_status="healthy"
                response_time=$(measure_response_time http://localhost:$port/health)
            else
                health_status="unhealthy"
            fi
            
            echo "    \"$service\": {"
            echo "      \"health\": \"$health_status\","
            echo "      \"response_time_ms\": $response_time,"
            echo "      \"port\": $port"
            echo "    }"
        done
        
        echo "  }"
        echo "}"
    } > "$report_file"
}

# System recovery verification
verify_system_recovery() {
    log_info "Verifying system recovery..."
    
    local recovery_attempts=0
    local max_attempts=30
    local services_healthy=0
    
    while [ $recovery_attempts -lt $max_attempts ]; do
        services_healthy=0
        
        for service in "${!SERVICES[@]}"; do
            local port="${SERVICES[$service]}"
            if curl -s http://localhost:$port/health &> /dev/null; then
                ((services_healthy++))
            fi
        done
        
        if [ $services_healthy -eq ${#SERVICES[@]} ]; then
            log_success "All services have recovered"
            return 0
        fi
        
        log_info "Recovery progress: $services_healthy/${#SERVICES[@]} services healthy"
        sleep 10
        ((recovery_attempts++))
    done
    
    log_warning "System recovery incomplete: $services_healthy/${#SERVICES[@]} services healthy"
    return 1
}

# Generate comprehensive report
generate_comprehensive_report() {
    log_info "Generating comprehensive chaos testing report..."
    
    local report_file="$REPORT_DIR/chaos-test-report.html"
    
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>MCP Chaos Engineering Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; }
        .experiment { border: 1px solid #ddd; margin: 10px 0; padding: 15px; border-radius: 5px; }
        .success { border-left: 5px solid green; }
        .failed { border-left: 5px solid red; }
        .warning { border-left: 5px solid orange; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .metric { display: inline-block; margin: 5px 10px; padding: 5px; background-color: #e9e9e9; border-radius: 3px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>MCP Chaos Engineering Report</h1>
        <p><strong>Generated:</strong> $(date)</p>
        <p><strong>Chaos Mode:</strong> $CHAOS_MODE</p>
        <p><strong>Safety Mode:</strong> $SAFETY_MODE</p>
        <p><strong>Experiment Duration:</strong> $EXPERIMENT_DURATION</p>
    </div>
    
    <div class="section">
        <h2>Executive Summary</h2>
EOF

    # Calculate summary statistics
    local total_experiments=0
    local successful_experiments=0
    local failed_experiments=0
    
    for experiment in "${!EXPERIMENTS[@]}"; do
        if [ -f "$REPORT_DIR/experiments/$experiment/report.json" ]; then
            ((total_experiments++))
            if grep -q "\"status\": \"SUCCESS\"" "$REPORT_DIR/experiments/$experiment/report.json"; then
                ((successful_experiments++))
            else
                ((failed_experiments++))
            fi
        fi
    done
    
    local success_rate=0
    if [ $total_experiments -gt 0 ]; then
        success_rate=$((successful_experiments * 100 / total_experiments))
    fi
    
    cat >> "$report_file" << EOF
        <div class="metric">Total Experiments: $total_experiments</div>
        <div class="metric">Successful: $successful_experiments</div>
        <div class="metric">Failed: $failed_experiments</div>
        <div class="metric">Success Rate: $success_rate%</div>
    </div>
    
    <div class="section">
        <h2>Experiment Results</h2>
EOF

    # Add experiment details
    for experiment in "${!EXPERIMENTS[@]}"; do
        local experiment_dir="$REPORT_DIR/experiments/$experiment"
        if [ -f "$experiment_dir/report.json" ]; then
            local status=$(grep '"status"' "$experiment_dir/report.json" | cut -d'"' -f4)
            local duration=$(grep '"duration_seconds"' "$experiment_dir/report.json" | cut -d':' -f2 | cut -d',' -f1 | tr -d ' ')
            
            local css_class="experiment"
            if [ "$status" = "SUCCESS" ]; then
                css_class="experiment success"
            else
                css_class="experiment failed"
            fi
            
            cat >> "$report_file" << EOF
        <div class="$css_class">
            <h3>$experiment</h3>
            <p><strong>Status:</strong> $status</p>
            <p><strong>Duration:</strong> ${duration}s</p>
            <p><strong>Log:</strong> <a href="experiments/$experiment/experiment.log">View Log</a></p>
        </div>
EOF
        fi
    done
    
    cat >> "$report_file" << EOF
    </div>
    
    <div class="section">
        <h2>System Health</h2>
        <table>
            <tr><th>Service</th><th>Status</th><th>Response Time (ms)</th><th>Port</th></tr>
EOF

    # Add current system health
    for service in "${!SERVICES[@]}"; do
        local port="${SERVICES[$service]}"
        local status="Unknown"
        local response_time="N/A"
        
        if curl -s http://localhost:$port/health &> /dev/null; then
            status="Healthy"
            response_time=$(measure_response_time http://localhost:$port/health)
        else
            status="Unhealthy"
        fi
        
        cat >> "$report_file" << EOF
            <tr>
                <td>$service</td>
                <td>$status</td>
                <td>$response_time</td>
                <td>$port</td>
            </tr>
EOF
    done
    
    cat >> "$report_file" << EOF
        </table>
    </div>
    
    <div class="section">
        <h2>Recommendations</h2>
        <ul>
EOF

    # Add recommendations based on results
    if [ $success_rate -lt 80 ]; then
        echo "            <li>System resilience is below 80%. Consider improving error handling and recovery mechanisms.</li>" >> "$report_file"
    fi
    
    if [ $failed_experiments -gt 0 ]; then
        echo "            <li>Some experiments failed. Review failed experiment logs for specific improvements.</li>" >> "$report_file"
    fi
    
    echo "            <li>Continue regular chaos engineering practices to maintain system resilience.</li>" >> "$report_file"
    echo "            <li>Monitor system metrics during normal operations to detect degradation early.</li>" >> "$report_file"
    
    cat >> "$report_file" << EOF
        </ul>
    </div>
</body>
</html>
EOF

    log_success "Comprehensive report generated: $report_file"
}

# Cleanup function
cleanup() {
    log_info "Performing cleanup..."
    
    # Stop any running chaos processes
    pkill -f "chaos" 2>/dev/null || true
    
    # Verify system recovery
    verify_system_recovery
}

# Main execution
main() {
    local action="${1:-run-all}"
    
    # Create report directory
    mkdir -p "$REPORT_DIR/experiments"
    
    # Setup cleanup trap
    trap cleanup EXIT
    
    case $action in
        "check")
            log_info "Running chaos testing prerequisite check..."
            check_prerequisites
            ;;
        "baseline")
            log_info "Establishing system baseline..."
            check_prerequisites
            establish_baseline
            ;;
        "experiment")
            local experiment_name="$2"
            if [ -z "$experiment_name" ]; then
                log_error "Experiment name required"
                echo "Available experiments: ${!EXPERIMENTS[@]}"
                exit 1
            fi
            check_safety_mode
            check_prerequisites
            establish_baseline
            run_experiment "$experiment_name"
            verify_system_recovery
            ;;
        "run-all")
            log_info "Running all chaos experiments..."
            check_safety_mode
            check_prerequisites
            establish_baseline
            
            local failed_count=0
            for experiment in "${!EXPERIMENTS[@]}"; do
                if ! run_experiment "$experiment"; then
                    ((failed_count++))
                fi
                
                # Brief pause between experiments
                sleep 30
            done
            
            verify_system_recovery
            generate_comprehensive_report
            
            if [ $failed_count -gt 0 ]; then
                log_warning "$failed_count experiments failed"
                exit 1
            else
                log_success "All chaos experiments completed successfully"
            fi
            ;;
        "report")
            log_info "Generating chaos testing report..."
            generate_comprehensive_report
            ;;
        "help")
            echo "Usage: $0 [action] [options]"
            echo "Actions:"
            echo "  check       - Check prerequisites only"
            echo "  baseline    - Establish system baseline only"
            echo "  experiment  - Run specific experiment (requires experiment name)"
            echo "  run-all     - Run all chaos experiments (default)"
            echo "  report      - Generate comprehensive report"
            echo "  help        - Show this help message"
            echo ""
            echo "Available experiments: ${!EXPERIMENTS[@]}"
            echo ""
            echo "Environment variables:"
            echo "  CHAOS_MODE=$CHAOS_MODE (safe|aggressive)"
            echo "  EXPERIMENT_DURATION=$EXPERIMENT_DURATION"
            echo "  CONCURRENCY=$CONCURRENCY"
            echo "  SAFETY_MODE=$SAFETY_MODE"
            echo "  REPORT_DIR=$REPORT_DIR"
            exit 0
            ;;
        *)
            log_error "Unknown action: $action"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"