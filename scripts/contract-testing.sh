#!/bin/bash

# Contract Testing Script for MCP Services
# Automates contract testing across all services using Pact

set -e

# Configuration
PACT_BROKER_URL="${PACT_BROKER_URL:-http://localhost:9292}"
PACT_BROKER_USERNAME="${PACT_BROKER_USERNAME:-admin}"
PACT_BROKER_PASSWORD="${PACT_BROKER_PASSWORD:-admin}"
ENVIRONMENT="${ENVIRONMENT:-test}"
VERSION="${VERSION:-$(git rev-parse --short HEAD)}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
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

declare -A CONSUMER_TESTS=(
    ["mcp-llm"]="LlmProviderContractTest"
    ["mcp-controller"]="DebateServiceContractTest"
)

declare -A PROVIDER_TESTS=(
    ["mcp-organization"]="OrganizationProviderTest"
    ["mcp-llm"]="LlmProviderTest"
    ["mcp-controller"]="DebateProviderTest"
)

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Java is installed
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    # Check if Maven is installed
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed or not in PATH"
        exit 1
    fi
    
    # Check if Docker is running (for Pact Broker)
    if ! docker ps &> /dev/null; then
        log_warning "Docker is not running. Pact Broker may not be available."
    fi
    
    log_success "Prerequisites check completed"
}

# Start Pact Broker
start_pact_broker() {
    log_info "Starting Pact Broker..."
    
    if docker ps --format 'table {{.Names}}' | grep -q "pact-broker"; then
        log_info "Pact Broker is already running"
        return 0
    fi
    
    docker run -d --name pact-broker \
        -p 9292:9292 \
        -e PACT_BROKER_DATABASE_URL=sqlite:///tmp/pact_broker.sqlite \
        -e PACT_BROKER_BASIC_AUTH_USERNAME=$PACT_BROKER_USERNAME \
        -e PACT_BROKER_BASIC_AUTH_PASSWORD=$PACT_BROKER_PASSWORD \
        pactfoundation/pact-broker:latest
    
    # Wait for Pact Broker to be ready
    log_info "Waiting for Pact Broker to be ready..."
    for i in {1..30}; do
        if curl -s -u $PACT_BROKER_USERNAME:$PACT_BROKER_PASSWORD $PACT_BROKER_URL/health &> /dev/null; then
            log_success "Pact Broker is ready"
            return 0
        fi
        sleep 2
    done
    
    log_error "Pact Broker failed to start"
    return 1
}

# Build all services
build_services() {
    log_info "Building all services..."
    
    # Build parent project
    mvn clean install -DskipTests
    
    # Build each service
    for service in "${!SERVICES[@]}"; do
        log_info "Building $service..."
        cd $service
        mvn clean compile test-compile
        cd ..
    done
    
    log_success "All services built successfully"
}

# Run consumer contract tests
run_consumer_tests() {
    log_info "Running consumer contract tests..."
    
    local failed_tests=()
    
    for service in "${!CONSUMER_TESTS[@]}"; do
        local test_class="${CONSUMER_TESTS[$service]}"
        log_info "Running consumer tests for $service ($test_class)..."
        
        cd $service
        if mvn test -Dtest=$test_class -Dpact.provider.version=$VERSION; then
            log_success "Consumer tests passed for $service"
        else
            log_error "Consumer tests failed for $service"
            failed_tests+=("$service")
        fi
        cd ..
    done
    
    if [ ${#failed_tests[@]} -gt 0 ]; then
        log_error "Consumer tests failed for: ${failed_tests[*]}"
        return 1
    fi
    
    log_success "All consumer tests passed"
}

# Publish contracts to Pact Broker
publish_contracts() {
    log_info "Publishing contracts to Pact Broker..."
    
    for service in "${!CONSUMER_TESTS[@]}"; do
        log_info "Publishing contracts for $service..."
        
        cd $service
        mvn pact:publish \
            -Dpact.broker.url=$PACT_BROKER_URL \
            -Dpact.broker.username=$PACT_BROKER_USERNAME \
            -Dpact.broker.password=$PACT_BROKER_PASSWORD \
            -Dpact.consumer.version=$VERSION \
            -Dpact.consumer.tags=$ENVIRONMENT
        cd ..
    done
    
    log_success "Contracts published successfully"
}

# Start services for provider verification
start_services() {
    log_info "Starting services for provider verification..."
    
    for service in "${!SERVICES[@]}"; do
        local port="${SERVICES[$service]}"
        log_info "Starting $service on port $port..."
        
        cd $service
        # Start service in background
        nohup mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=$port" > ../logs/$service.log 2>&1 &
        echo $! > ../logs/$service.pid
        cd ..
        
        # Wait for service to be ready
        log_info "Waiting for $service to be ready..."
        for i in {1..60}; do
            if curl -s http://localhost:$port/health &> /dev/null; then
                log_success "$service is ready on port $port"
                break
            fi
            sleep 2
        done
    done
}

# Run provider verification tests
run_provider_tests() {
    log_info "Running provider verification tests..."
    
    local failed_tests=()
    
    for service in "${!PROVIDER_TESTS[@]}"; do
        local test_class="${PROVIDER_TESTS[$service]}"
        local port="${SERVICES[$service]}"
        
        log_info "Running provider verification for $service ($test_class)..."
        
        cd $service
        if mvn test -Dtest=$test_class \
            -Dpact.verifier.publishResults=true \
            -Dpact.provider.version=$VERSION \
            -Dpact.provider.tag=$ENVIRONMENT \
            -Dpact.broker.url=$PACT_BROKER_URL \
            -Dpact.broker.username=$PACT_BROKER_USERNAME \
            -Dpact.broker.password=$PACT_BROKER_PASSWORD \
            -Dtest.server.port=$port; then
            log_success "Provider verification passed for $service"
        else
            log_error "Provider verification failed for $service"
            failed_tests+=("$service")
        fi
        cd ..
    done
    
    if [ ${#failed_tests[@]} -gt 0 ]; then
        log_error "Provider verification failed for: ${failed_tests[*]}"
        return 1
    fi
    
    log_success "All provider verifications passed"
}

# Stop services
stop_services() {
    log_info "Stopping services..."
    
    for service in "${!SERVICES[@]}"; do
        if [ -f "logs/$service.pid" ]; then
            local pid=$(cat "logs/$service.pid")
            if kill -0 $pid 2>/dev/null; then
                log_info "Stopping $service (PID: $pid)..."
                kill $pid
                rm "logs/$service.pid"
            fi
        fi
    done
    
    log_success "All services stopped"
}

# Stop Pact Broker
stop_pact_broker() {
    log_info "Stopping Pact Broker..."
    
    if docker ps --format 'table {{.Names}}' | grep -q "pact-broker"; then
        docker stop pact-broker
        docker rm pact-broker
        log_success "Pact Broker stopped"
    fi
}

# Generate contract test report
generate_report() {
    log_info "Generating contract test report..."
    
    local report_file="contract-test-report.html"
    
    cat > $report_file << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Contract Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; }
        .success { color: green; }
        .error { color: red; }
        .warning { color: orange; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>MCP Contract Test Report</h1>
        <p>Generated: $(date)</p>
        <p>Version: $VERSION</p>
        <p>Environment: $ENVIRONMENT</p>
    </div>
    
    <div class="section">
        <h2>Test Summary</h2>
        <table>
            <tr><th>Service</th><th>Consumer Tests</th><th>Provider Tests</th><th>Status</th></tr>
EOF

    for service in "${!SERVICES[@]}"; do
        local consumer_status="N/A"
        local provider_status="N/A"
        local overall_status="✅ Pass"
        
        if [[ -n "${CONSUMER_TESTS[$service]}" ]]; then
            consumer_status="✅ Pass"
        fi
        
        if [[ -n "${PROVIDER_TESTS[$service]}" ]]; then
            provider_status="✅ Pass"
        fi
        
        cat >> $report_file << EOF
            <tr>
                <td>$service</td>
                <td>$consumer_status</td>
                <td>$provider_status</td>
                <td>$overall_status</td>
            </tr>
EOF
    done
    
    cat >> $report_file << EOF
        </table>
    </div>
    
    <div class="section">
        <h2>Contract Matrix</h2>
        <p>View detailed contract interactions at: <a href="$PACT_BROKER_URL">$PACT_BROKER_URL</a></p>
    </div>
    
    <div class="section">
        <h2>Service Logs</h2>
        <p>Service logs are available in the logs/ directory</p>
    </div>
</body>
</html>
EOF

    log_success "Contract test report generated: $report_file"
}

# Cleanup function
cleanup() {
    log_info "Performing cleanup..."
    stop_services
    stop_pact_broker
}

# Main execution
main() {
    local action="${1:-full}"
    
    # Create logs directory
    mkdir -p logs
    
    # Setup cleanup trap
    trap cleanup EXIT
    
    case $action in
        "consumer")
            log_info "Running consumer contract tests only..."
            check_prerequisites
            build_services
            run_consumer_tests
            ;;
        "provider")
            log_info "Running provider verification tests only..."
            check_prerequisites
            start_pact_broker
            start_services
            run_provider_tests
            ;;
        "publish")
            log_info "Publishing contracts only..."
            check_prerequisites
            build_services
            run_consumer_tests
            publish_contracts
            ;;
        "full")
            log_info "Running full contract testing pipeline..."
            check_prerequisites
            start_pact_broker
            build_services
            run_consumer_tests
            publish_contracts
            start_services
            run_provider_tests
            generate_report
            ;;
        "help")
            echo "Usage: $0 [action]"
            echo "Actions:"
            echo "  consumer  - Run consumer contract tests only"
            echo "  provider  - Run provider verification tests only"
            echo "  publish   - Publish contracts to broker"
            echo "  full      - Run complete contract testing pipeline (default)"
            echo "  help      - Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown action: $action"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
    
    log_success "Contract testing completed successfully!"
}

# Run main function with all arguments
main "$@"