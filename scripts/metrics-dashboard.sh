#!/bin/bash

# Test Metrics Dashboard Generator and Server
# Collects test metrics and generates interactive dashboard

set -e

# Configuration
DASHBOARD_PORT="${DASHBOARD_PORT:-8888}"
DASHBOARD_HOST="${DASHBOARD_HOST:-localhost}"
METRICS_DIR="${METRICS_DIR:-metrics-data}"
DASHBOARD_DIR="${DASHBOARD_DIR:-dashboard}"
AUTO_REFRESH="${AUTO_REFRESH:-30}"
ENVIRONMENT="${ENVIRONMENT:-development}"

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
    echo -e "${BLUE}[METRICS]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[METRICS SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[METRICS WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[METRICS ERROR]${NC} $1"
}

log_metric() {
    echo -e "${CYAN}[METRIC]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking dashboard prerequisites..."
    
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
    
    # Check if Python is available (for simple HTTP server)
    if ! command -v python3 &> /dev/null && ! command -v python &> /dev/null; then
        log_warning "Python not available - dashboard server will be limited"
    fi
    
    log_success "Prerequisites check completed"
}

# Collect metrics from Maven Surefire reports
collect_surefire_metrics() {
    local project_path="$1"
    local module_name="$2"
    
    log_info "Collecting Surefire metrics for $module_name..."
    
    local surefire_dir="$project_path/target/surefire-reports"
    local metrics_file="$METRICS_DIR/${module_name}-surefire.json"
    
    if [ ! -d "$surefire_dir" ]; then
        log_warning "No Surefire reports found for $module_name"
        return 0
    fi
    
    local total_tests=0
    local failed_tests=0
    local error_tests=0
    local skipped_tests=0
    local total_time=0
    
    # Parse XML files
    for xml_file in "$surefire_dir"/TEST-*.xml; do
        if [ -f "$xml_file" ]; then
            local tests=$(grep -o 'tests="[^"]*"' "$xml_file" | cut -d'"' -f2 | head -1)
            local failures=$(grep -o 'failures="[^"]*"' "$xml_file" | cut -d'"' -f2 | head -1)
            local errors=$(grep -o 'errors="[^"]*"' "$xml_file" | cut -d'"' -f2 | head -1)
            local skipped=$(grep -o 'skipped="[^"]*"' "$xml_file" | cut -d'"' -f2 | head -1)
            local time=$(grep -o 'time="[^"]*"' "$xml_file" | cut -d'"' -f2 | head -1)
            
            total_tests=$((total_tests + ${tests:-0}))
            failed_tests=$((failed_tests + ${failures:-0}))
            error_tests=$((error_tests + ${errors:-0}))
            skipped_tests=$((skipped_tests + ${skipped:-0}))
            total_time=$(echo "$total_time + ${time:-0}" | bc -l 2>/dev/null || echo "$total_time")
        fi
    done
    
    local passed_tests=$((total_tests - failed_tests - error_tests))
    local success_rate=0
    if [ $total_tests -gt 0 ]; then
        success_rate=$(echo "scale=2; $passed_tests * 100 / $total_tests" | bc -l)
    fi
    
    # Generate JSON
    {
        echo "{"
        echo "  \"module\": \"$module_name\","
        echo "  \"type\": \"surefire\","
        echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
        echo "  \"metrics\": {"
        echo "    \"total_tests\": $total_tests,"
        echo "    \"passed_tests\": $passed_tests,"
        echo "    \"failed_tests\": $failed_tests,"
        echo "    \"error_tests\": $error_tests,"
        echo "    \"skipped_tests\": $skipped_tests,"
        echo "    \"success_rate\": $success_rate,"
        echo "    \"execution_time_seconds\": $total_time"
        echo "  }"
        echo "}"
    } > "$metrics_file"
    
    log_metric "$module_name: $passed_tests/$total_tests tests passed ($success_rate%)"
}

# Collect metrics from JaCoCo coverage reports
collect_coverage_metrics() {
    local project_path="$1"
    local module_name="$2"
    
    log_info "Collecting coverage metrics for $module_name..."
    
    local jacoco_report="$project_path/target/site/jacoco/index.html"
    local metrics_file="$METRICS_DIR/${module_name}-coverage.json"
    
    if [ ! -f "$jacoco_report" ]; then
        log_warning "No JaCoCo report found for $module_name"
        return 0
    fi
    
    # Extract coverage percentages from HTML
    local instruction_coverage=$(grep -o 'Total[^%]*%' "$jacoco_report" | head -1 | grep -o '[0-9]\+%' | tr -d '%' || echo "0")
    local branch_coverage=$(grep -o 'Total[^%]*%' "$jacoco_report" | sed -n '2p' | grep -o '[0-9]\+%' | tr -d '%' || echo "0")
    local line_coverage=$(grep -o 'Total[^%]*%' "$jacoco_report" | sed -n '3p' | grep -o '[0-9]\+%' | tr -d '%' || echo "0")
    
    # Generate JSON
    {
        echo "{"
        echo "  \"module\": \"$module_name\","
        echo "  \"type\": \"coverage\","
        echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
        echo "  \"metrics\": {"
        echo "    \"instruction_coverage\": $instruction_coverage,"
        echo "    \"branch_coverage\": $branch_coverage,"
        echo "    \"line_coverage\": $line_coverage"
        echo "  }"
        echo "}"
    } > "$metrics_file"
    
    log_metric "$module_name: ${line_coverage}% line coverage"
}

# Collect performance metrics
collect_performance_metrics() {
    local project_path="$1"
    local module_name="$2"
    
    log_info "Collecting performance metrics for $module_name..."
    
    local performance_dir="$project_path/target/performance-results"
    local metrics_file="$METRICS_DIR/${module_name}-performance.json"
    
    if [ ! -d "$performance_dir" ]; then
        log_warning "No performance results found for $module_name"
        return 0
    fi
    
    # Look for JMeter results (JTL files)
    local jtl_file=$(find "$performance_dir" -name "*.jtl" | head -1)
    if [ -n "$jtl_file" ] && [ -f "$jtl_file" ]; then
        local total_requests=$(wc -l < "$jtl_file")
        local avg_response_time=$(awk -F',' 'NR>1{sum+=$2; count++} END{if(count>0) print sum/count; else print 0}' "$jtl_file")
        local error_count=$(awk -F',' 'NR>1 && $8=="false"{count++} END{print count+0}' "$jtl_file")
        local error_rate=0
        
        if [ $total_requests -gt 0 ]; then
            error_rate=$(echo "scale=2; $error_count * 100 / $total_requests" | bc -l)
        fi
        
        # Generate JSON
        {
            echo "{"
            echo "  \"module\": \"$module_name\","
            echo "  \"type\": \"performance\","
            echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
            echo "  \"metrics\": {"
            echo "    \"total_requests\": $total_requests,"
            echo "    \"avg_response_time_ms\": $avg_response_time,"
            echo "    \"error_count\": $error_count,"
            echo "    \"error_rate\": $error_rate"
            echo "  }"
            echo "}"
        } > "$metrics_file"
        
        log_metric "$module_name: ${avg_response_time}ms avg response time, ${error_rate}% error rate"
    fi
}

# Collect chaos engineering metrics
collect_chaos_metrics() {
    local chaos_reports_dir="$1"
    
    log_info "Collecting chaos engineering metrics..."
    
    local metrics_file="$METRICS_DIR/chaos-metrics.json"
    
    if [ ! -d "$chaos_reports_dir" ]; then
        log_warning "No chaos reports found"
        return 0
    fi
    
    local total_experiments=0
    local successful_experiments=0
    local avg_resilience_score=0
    
    # Count experiments and calculate metrics
    for experiment_dir in "$chaos_reports_dir"/experiments/*/; do
        if [ -d "$experiment_dir" ]; then
            total_experiments=$((total_experiments + 1))
            
            local report_file="$experiment_dir/report.json"
            if [ -f "$report_file" ]; then
                local status=$(grep -o '"status": "[^"]*"' "$report_file" | cut -d'"' -f4)
                if [ "$status" = "SUCCESS" ]; then
                    successful_experiments=$((successful_experiments + 1))
                fi
            fi
        fi
    done
    
    local success_rate=0
    if [ $total_experiments -gt 0 ]; then
        success_rate=$(echo "scale=2; $successful_experiments * 100 / $total_experiments" | bc -l)
        avg_resilience_score=$(echo "scale=2; $success_rate * 0.9" | bc -l) # Approximate resilience score
    fi
    
    # Generate JSON
    {
        echo "{"
        echo "  \"module\": \"chaos-engineering\","
        echo "  \"type\": \"chaos\","
        echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
        echo "  \"metrics\": {"
        echo "    \"total_experiments\": $total_experiments,"
        echo "    \"successful_experiments\": $successful_experiments,"
        echo "    \"success_rate\": $success_rate,"
        echo "    \"resilience_score\": $avg_resilience_score"
        echo "  }"
        echo "}"
    } > "$metrics_file"
    
    log_metric "Chaos: $successful_experiments/$total_experiments experiments passed ($success_rate%)"
}

# Collect all metrics
collect_all_metrics() {
    log_info "Collecting metrics from all modules..."
    
    mkdir -p "$METRICS_DIR"
    
    # MCP modules
    local modules=("mcp-organization" "mcp-llm" "mcp-controller" "mcp-context" "mcp-rag" "mcp-gateway" "mcp-common")
    
    for module in "${modules[@]}"; do
        if [ -d "$module" ]; then
            collect_surefire_metrics "$module" "$module"
            collect_coverage_metrics "$module" "$module"
            collect_performance_metrics "$module" "$module"
        fi
    done
    
    # Chaos engineering metrics
    if [ -d "chaos-reports" ]; then
        collect_chaos_metrics "chaos-reports"
    fi
    
    # E2E metrics
    if [ -d "e2e-reports" ]; then
        collect_surefire_metrics "e2e-reports" "e2e-tests"
    fi
    
    log_success "Metrics collection completed"
}

# Generate aggregated metrics
generate_aggregated_metrics() {
    log_info "Generating aggregated metrics..."
    
    local aggregated_file="$METRICS_DIR/aggregated-metrics.json"
    local total_tests=0
    local total_passed=0
    local total_failed=0
    local avg_coverage=0
    local coverage_count=0
    
    # Process all Surefire metrics
    for metrics_file in "$METRICS_DIR"/*-surefire.json; do
        if [ -f "$metrics_file" ]; then
            local tests=$(jq -r '.metrics.total_tests' "$metrics_file" 2>/dev/null || echo "0")
            local passed=$(jq -r '.metrics.passed_tests' "$metrics_file" 2>/dev/null || echo "0")
            local failed=$(jq -r '.metrics.failed_tests' "$metrics_file" 2>/dev/null || echo "0")
            
            total_tests=$((total_tests + tests))
            total_passed=$((total_passed + passed))
            total_failed=$((total_failed + failed))
        fi
    done
    
    # Process all coverage metrics
    for metrics_file in "$METRICS_DIR"/*-coverage.json; do
        if [ -f "$metrics_file" ]; then
            local coverage=$(jq -r '.metrics.line_coverage' "$metrics_file" 2>/dev/null || echo "0")
            if [ "$coverage" != "null" ] && [ "$coverage" != "0" ]; then
                avg_coverage=$(echo "$avg_coverage + $coverage" | bc -l)
                coverage_count=$((coverage_count + 1))
            fi
        fi
    done
    
    if [ $coverage_count -gt 0 ]; then
        avg_coverage=$(echo "scale=2; $avg_coverage / $coverage_count" | bc -l)
    fi
    
    local overall_success_rate=0
    if [ $total_tests -gt 0 ]; then
        overall_success_rate=$(echo "scale=2; $total_passed * 100 / $total_tests" | bc -l)
    fi
    
    # Generate aggregated metrics JSON
    {
        echo "{"
        echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
        echo "  \"environment\": \"$ENVIRONMENT\","
        echo "  \"aggregated_metrics\": {"
        echo "    \"total_tests\": $total_tests,"
        echo "    \"total_passed\": $total_passed,"
        echo "    \"total_failed\": $total_failed,"
        echo "    \"overall_success_rate\": $overall_success_rate,"
        echo "    \"average_coverage\": $avg_coverage"
        echo "  }"
        echo "}"
    } > "$aggregated_file"
    
    log_success "Aggregated metrics generated"
}

# Generate HTML dashboard
generate_html_dashboard() {
    log_info "Generating HTML dashboard..."
    
    mkdir -p "$DASHBOARD_DIR"
    local dashboard_file="$DASHBOARD_DIR/index.html"
    
    # Read aggregated metrics
    local aggregated_file="$METRICS_DIR/aggregated-metrics.json"
    local total_tests=0
    local success_rate=0
    local avg_coverage=0
    
    if [ -f "$aggregated_file" ]; then
        if command -v jq &> /dev/null; then
            total_tests=$(jq -r '.aggregated_metrics.total_tests' "$aggregated_file" 2>/dev/null || echo "0")
            success_rate=$(jq -r '.aggregated_metrics.overall_success_rate' "$aggregated_file" 2>/dev/null || echo "0")
            avg_coverage=$(jq -r '.aggregated_metrics.average_coverage' "$aggregated_file" 2>/dev/null || echo "0")
        fi
    fi
    
    # Generate HTML dashboard
    cat > "$dashboard_file" << EOF
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MCP Test Metrics Dashboard</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f6fa; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 2rem; text-align: center; }
        .header h1 { font-size: 2.5rem; margin-bottom: 0.5rem; }
        .header p { opacity: 0.9; font-size: 1.1rem; }
        .container { max-width: 1200px; margin: 0 auto; padding: 2rem; }
        .metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 2rem; margin-bottom: 3rem; }
        .metric-card { background: white; border-radius: 12px; padding: 2rem; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); transition: transform 0.2s; }
        .metric-card:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15); }
        .metric-value { font-size: 3rem; font-weight: bold; color: #2c3e50; margin-bottom: 0.5rem; }
        .metric-label { color: #7f8c8d; font-size: 1.1rem; text-transform: uppercase; letter-spacing: 1px; }
        .metric-success { color: #27ae60; }
        .metric-warning { color: #f39c12; }
        .metric-danger { color: #e74c3c; }
        .modules-section { background: white; border-radius: 12px; padding: 2rem; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); }
        .modules-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1.5rem; margin-top: 1.5rem; }
        .module-card { border: 1px solid #ecf0f1; border-radius: 8px; padding: 1.5rem; transition: border-color 0.2s; }
        .module-card:hover { border-color: #3498db; }
        .module-name { font-weight: bold; font-size: 1.1rem; margin-bottom: 1rem; color: #2c3e50; }
        .module-metrics { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
        .module-metric { text-align: center; }
        .module-metric-value { font-size: 1.5rem; font-weight: bold; }
        .module-metric-label { font-size: 0.8rem; color: #7f8c8d; margin-top: 0.25rem; }
        .refresh-info { text-align: center; margin-top: 2rem; color: #7f8c8d; }
        .refresh-info .auto-refresh { color: #27ae60; font-weight: bold; }
        .last-updated { text-align: right; color: #7f8c8d; font-size: 0.9rem; margin-bottom: 1rem; }
        
        @media (max-width: 768px) {
            .header h1 { font-size: 2rem; }
            .metric-value { font-size: 2rem; }
            .container { padding: 1rem; }
            .metrics-grid { grid-template-columns: 1fr; }
        }
    </style>
    <script>
        function refreshPage() {
            location.reload();
        }
        
        // Auto-refresh every ${AUTO_REFRESH} seconds
        setInterval(refreshPage, ${AUTO_REFRESH} * 1000);
        
        // Update countdown timer
        let countdown = ${AUTO_REFRESH};
        setInterval(function() {
            countdown--;
            if (countdown <= 0) countdown = ${AUTO_REFRESH};
            document.getElementById('countdown').textContent = countdown;
        }, 1000);
    </script>
</head>
<body>
    <div class="header">
        <h1>🚀 MCP Test Metrics Dashboard</h1>
        <p>Real-time insights into test performance and quality metrics</p>
    </div>
    
    <div class="container">
        <div class="last-updated">
            Last Updated: $(date)
        </div>
        
        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-value metric-$([ $(echo "$total_tests > 0" | bc -l) -eq 1 ] && echo "success" || echo "warning")">$total_tests</div>
                <div class="metric-label">Total Tests</div>
            </div>
            
            <div class="metric-card">
                <div class="metric-value metric-$([ $(echo "$success_rate >= 95" | bc -l) -eq 1 ] && echo "success" || [ $(echo "$success_rate >= 80" | bc -l) -eq 1 ] && echo "warning" || echo "danger")">${success_rate}%</div>
                <div class="metric-label">Success Rate</div>
            </div>
            
            <div class="metric-card">
                <div class="metric-value metric-$([ $(echo "$avg_coverage >= 80" | bc -l) -eq 1 ] && echo "success" || [ $(echo "$avg_coverage >= 60" | bc -l) -eq 1 ] && echo "warning" || echo "danger")">${avg_coverage}%</div>
                <div class="metric-label">Average Coverage</div>
            </div>
            
            <div class="metric-card">
                <div class="metric-value metric-success">$ENVIRONMENT</div>
                <div class="metric-label">Environment</div>
            </div>
        </div>
        
        <div class="modules-section">
            <h2>📊 Module Breakdown</h2>
            <div class="modules-grid">
EOF

    # Add module cards
    for metrics_file in "$METRICS_DIR"/*-surefire.json; do
        if [ -f "$metrics_file" ]; then
            local module_name=$(basename "$metrics_file" -surefire.json)
            local tests=0
            local success_rate=0
            local coverage=0
            
            if command -v jq &> /dev/null; then
                tests=$(jq -r '.metrics.total_tests' "$metrics_file" 2>/dev/null || echo "0")
                success_rate=$(jq -r '.metrics.success_rate' "$metrics_file" 2>/dev/null || echo "0")
            fi
            
            # Try to get coverage for this module
            local coverage_file="$METRICS_DIR/${module_name}-coverage.json"
            if [ -f "$coverage_file" ] && command -v jq &> /dev/null; then
                coverage=$(jq -r '.metrics.line_coverage' "$coverage_file" 2>/dev/null || echo "0")
            fi
            
            cat >> "$dashboard_file" << EOF
                <div class="module-card">
                    <div class="module-name">$module_name</div>
                    <div class="module-metrics">
                        <div class="module-metric">
                            <div class="module-metric-value">$tests</div>
                            <div class="module-metric-label">Tests</div>
                        </div>
                        <div class="module-metric">
                            <div class="module-metric-value">${success_rate}%</div>
                            <div class="module-metric-label">Success</div>
                        </div>
                        <div class="module-metric">
                            <div class="module-metric-value">${coverage}%</div>
                            <div class="module-metric-label">Coverage</div>
                        </div>
                        <div class="module-metric">
                            <div class="module-metric-value">✅</div>
                            <div class="module-metric-label">Status</div>
                        </div>
                    </div>
                </div>
EOF
        fi
    done
    
    cat >> "$dashboard_file" << EOF
            </div>
        </div>
        
        <div class="refresh-info">
            <p>⚡ Dashboard auto-refreshes every <span class="auto-refresh">${AUTO_REFRESH} seconds</span></p>
            <p>Next refresh in <span id="countdown">${AUTO_REFRESH}</span> seconds</p>
        </div>
    </div>
</body>
</html>
EOF

    log_success "HTML dashboard generated: $dashboard_file"
}

# Start dashboard server
start_dashboard_server() {
    log_info "Starting dashboard server on http://$DASHBOARD_HOST:$DASHBOARD_PORT"
    
    cd "$DASHBOARD_DIR"
    
    # Try Python 3 first, then Python 2
    if command -v python3 &> /dev/null; then
        python3 -m http.server $DASHBOARD_PORT --bind $DASHBOARD_HOST &
        local server_pid=$!
    elif command -v python &> /dev/null; then
        python -m SimpleHTTPServer $DASHBOARD_PORT &
        local server_pid=$!
    else
        log_error "Python not available - cannot start HTTP server"
        log_info "Dashboard available at: file://$(pwd)/index.html"
        return 1
    fi
    
    echo $server_pid > dashboard-server.pid
    
    # Wait a moment for server to start
    sleep 2
    
    if kill -0 $server_pid 2>/dev/null; then
        log_success "Dashboard server started (PID: $server_pid)"
        log_success "Access dashboard at: http://$DASHBOARD_HOST:$DASHBOARD_PORT"
        
        # Try to open in browser (if available)
        if command -v open &> /dev/null; then
            open "http://$DASHBOARD_HOST:$DASHBOARD_PORT"
        elif command -v xdg-open &> /dev/null; then
            xdg-open "http://$DASHBOARD_HOST:$DASHBOARD_PORT"
        fi
    else
        log_error "Failed to start dashboard server"
        return 1
    fi
}

# Stop dashboard server
stop_dashboard_server() {
    local pid_file="$DASHBOARD_DIR/dashboard-server.pid"
    
    if [ -f "$pid_file" ]; then
        local server_pid=$(cat "$pid_file")
        if kill -0 $server_pid 2>/dev/null; then
            log_info "Stopping dashboard server (PID: $server_pid)..."
            kill $server_pid
            rm "$pid_file"
            log_success "Dashboard server stopped"
        else
            log_warning "Dashboard server not running"
            rm "$pid_file"
        fi
    else
        log_warning "No dashboard server PID file found"
    fi
}

# Watch for changes and auto-update
watch_and_update() {
    log_info "Starting watch mode - dashboard will update automatically"
    
    while true; do
        collect_all_metrics
        generate_aggregated_metrics
        generate_html_dashboard
        
        log_info "Dashboard updated - sleeping for $AUTO_REFRESH seconds..."
        sleep $AUTO_REFRESH
    done
}

# Main execution
main() {
    local action="${1:-serve}"
    
    case $action in
        "collect")
            log_info "Collecting metrics..."
            check_prerequisites
            collect_all_metrics
            generate_aggregated_metrics
            ;;
        "generate")
            log_info "Generating dashboard..."
            collect_all_metrics
            generate_aggregated_metrics
            generate_html_dashboard
            ;;
        "serve")
            log_info "Starting dashboard server..."
            check_prerequisites
            collect_all_metrics
            generate_aggregated_metrics
            generate_html_dashboard
            start_dashboard_server
            ;;
        "watch")
            log_info "Starting watch mode..."
            check_prerequisites
            start_dashboard_server
            watch_and_update
            ;;
        "stop")
            log_info "Stopping dashboard server..."
            stop_dashboard_server
            ;;
        "help")
            echo "Usage: $0 [action]"
            echo "Actions:"
            echo "  collect   - Collect metrics from test results"
            echo "  generate  - Generate HTML dashboard"
            echo "  serve     - Start dashboard server (default)"
            echo "  watch     - Start server and auto-update"
            echo "  stop      - Stop dashboard server"
            echo "  help      - Show this help message"
            echo ""
            echo "Environment variables:"
            echo "  DASHBOARD_PORT=$DASHBOARD_PORT"
            echo "  DASHBOARD_HOST=$DASHBOARD_HOST"
            echo "  METRICS_DIR=$METRICS_DIR"
            echo "  DASHBOARD_DIR=$DASHBOARD_DIR"
            echo "  AUTO_REFRESH=$AUTO_REFRESH"
            echo "  ENVIRONMENT=$ENVIRONMENT"
            exit 0
            ;;
        *)
            log_error "Unknown action: $action"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Cleanup function
cleanup() {
    log_info "Cleaning up..."
    stop_dashboard_server
}

# Setup cleanup trap
trap cleanup EXIT

# Run main function with all arguments
main "$@"