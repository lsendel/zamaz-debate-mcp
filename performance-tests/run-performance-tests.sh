#!/bin/bash

# Performance Test Runner Script
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BASE_URL=${BASE_URL:-"http://localhost:8080"}
JWT_TOKEN=${JWT_TOKEN:-"test-token"}
TEST_TYPE=${1:-"load"} # load, stress, spike, soak
TOOL=${2:-"k6"} # k6, gatling, locust, artillery

echo -e "${GREEN}MCP Debate API Performance Test Runner${NC}"
echo "========================================"
echo "Base URL: """$BASE_URL""""
echo "Test Type: """$TEST_TYPE""""
echo "Tool: """$TOOL""""
echo ""

# Function to check if services are healthy
check_services() {
    echo -e "${YELLOW}Checking service health...${NC}"
    
    # Check if API is responding
    if curl -f -s """"$BASE_URL"""/health" > /dev/null; then
        echo -e "${GREEN}✓ API is healthy${NC}"
    else
        echo -e "${RED}✗ API is not responding${NC}"
        exit 1
    fi
    
    # Check if required services are running
    if docker ps | grep -q "postgres"; then
        echo -e "${GREEN}✓ PostgreSQL is running${NC}"
    else
        echo -e "${YELLOW}! PostgreSQL not detected${NC}"
    fi
    
    if docker ps | grep -q "redis"; then
        echo -e "${GREEN}✓ Redis is running${NC}"
    else
        echo -e "${YELLOW}! Redis not detected${NC}"
    fi
}

# Function to start monitoring stack
start_monitoring() {
    echo -e "${YELLOW}Starting performance monitoring stack...${NC}"
    docker-compose -f docker-compose.perf.yml up -d influxdb grafana-perf
    sleep 10
    echo -e "${GREEN}✓ Monitoring stack started${NC}"
    echo "  Grafana: http://localhost:3001 (admin/admin123)"
}

# Function to run k6 tests
run_k6_test() {
    local test_script=""
    case """$TEST_TYPE""" in
        "load")
            test_script="debate-api-load-test.js"
            ;;
        "stress")
            test_script="debate-api-stress-test.js"
            ;;
        "spike")
            test_script="debate-api-spike-test.js"
            ;;
        "soak")
            test_script="debate-api-soak-test.js"
            ;;
        *)
            echo -e "${RED}Unknown test type: """$TEST_TYPE"""${NC}"
            exit 1
            ;;
    esac
    
    echo -e "${YELLOW}Running k6 """$TEST_TYPE""" test...${NC}"
    
    # Run k6 with Docker
    docker run --rm \
        -v $(pwd)/k6:/scripts \
        -v $(pwd)/results:/results \
        -e BASE_URL=""""$BASE_URL"""" \
        -e JWT_TOKEN=""""$JWT_TOKEN"""" \
        -e K6_OUT="json=/results/k6-${TEST_TYPE}-$(date +%Y%m%d-%H%M%S).json" \
        --network host \
        grafana/k6:latest \
        run /scripts/$test_script
        
    echo -e "${GREEN}✓ k6 test completed${NC}"
}

# Function to run Gatling tests
run_gatling_test() {
    echo -e "${YELLOW}Running Gatling """$TEST_TYPE""" test...${NC}"
    
    # Build Gatling image
    docker build -t mcp-gatling -f gatling/Dockerfile gatling/
    
    # Run Gatling test
    docker run --rm \
        -v $(pwd)/gatling/results:/opt/gatling/results \
        -e BASE_URL=""""$BASE_URL"""" \
        -e JWT_TOKEN=""""$JWT_TOKEN"""" \
        --network host \
        mcp-gatling \
        -s com.zamaz.mcp.performance.DebateAPILoadTest
        
    echo -e "${GREEN}✓ Gatling test completed${NC}"
}

# Function to run Locust tests
run_locust_test() {
    echo -e "${YELLOW}Starting Locust for """$TEST_TYPE""" test...${NC}"
    
    # Start Locust
    docker-compose -f docker-compose.perf.yml up -d locust-master locust-worker
    
    echo -e "${GREEN}✓ Locust started${NC}"
    echo "  Web UI: http://localhost:8089"
    echo "  Configure test parameters in the web UI"
}

# Function to generate report
generate_report() {
    echo -e "${YELLOW}Generating performance test report...${NC}"
    
    # Create report directory
    mkdir -p results/reports
    
    # Generate HTML report
    cat > results/reports/performance-report-$(date +%Y%m%d-%H%M%S).html <<EOF
<!DOCTYPE html>
<html>
<head>
    <title>MCP Performance Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        .info { background: #f0f0f0; padding: 10px; margin: 10px 0; }
        .success { color: green; }
        .warning { color: orange; }
        .error { color: red; }
    </style>
</head>
<body>
    <h1>Performance Test Report</h1>
    <div class="info">
        <h2>Test Configuration</h2>
        <p><strong>Test Type:</strong> """$TEST_TYPE"""</p>
        <p><strong>Tool:</strong> """$TOOL"""</p>
        <p><strong>Base URL:</strong> """$BASE_URL"""</p>
        <p><strong>Date:</strong> $(date)</p>
    </div>
    <div class="info">
        <h2>Results</h2>
        <p>Check the results directory for detailed metrics.</p>
        <p>Grafana dashboards available at: <a href="http://localhost:3001">http://localhost:3001</a></p>
    </div>
</body>
</html>
EOF
    
    echo -e "${GREEN}✓ Report generated${NC}"
}

# Main execution
main() {
    # Check prerequisites
    check_services
    
    # Start monitoring if requested
    if [[ "$3" == "--with-monitoring" ]]; then
        start_monitoring
    fi
    
    # Run the appropriate test
    case """$TOOL""" in
        "k6")
            run_k6_test
            ;;
        "gatling")
            run_gatling_test
            ;;
        "locust")
            run_locust_test
            ;;
        *)
            echo -e "${RED}Unknown tool: """$TOOL"""${NC}"
            echo "Supported tools: k6, gatling, locust"
            exit 1
            ;;
    esac
    
    # Generate report
    generate_report
    
    echo ""
    echo -e "${GREEN}Performance test completed successfully!${NC}"
    echo "Results saved in: ./results/"
}

# Show help if needed
if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "Usage: $0 [TEST_TYPE] [TOOL] [OPTIONS]"
    echo ""
    echo "TEST_TYPE: load (default), stress, spike, soak"
    echo "TOOL: k6 (default), gatling, locust"
    echo "OPTIONS:"
    echo "  --with-monitoring    Start Grafana and InfluxDB for real-time monitoring"
    echo ""
    echo "Examples:"
    echo "  $0                           # Run load test with k6"
    echo "  $0 stress k6                 # Run stress test with k6"
    echo "  $0 spike gatling             # Run spike test with Gatling"
    echo "  $0 load k6 --with-monitoring # Run load test with monitoring"
    exit 0
fi

# Run main function
main