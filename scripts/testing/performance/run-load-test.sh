#!/bin/bash

# Load testing script for MCP system

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Default values
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
USERS="${USERS:-100}"
RAMP_DURATION="${RAMP_DURATION:-60}"
TEST_DURATION="${TEST_DURATION:-300}"
SIMULATION="${SIMULATION:-DebateSystemLoadTest}"

echo -e "${GREEN}MCP Load Testing Suite${NC}"
echo "========================="

# Function to print usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  -u, --users        Number of concurrent users (default: 100)"
    echo "  -r, --ramp         Ramp-up duration in seconds (default: 60)"
    echo "  -d, --duration     Test duration in seconds (default: 300)"
    echo "  -g, --gateway      Gateway URL (default: http://localhost:8080)"
    echo "  -s, --simulation   Simulation class name (default: DebateSystemLoadTest)"
    echo "  -h, --help         Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --users 500 --duration 600"
    echo "  $0 -u 1000 -r 120 -d 900 -g https://api.mcp.example.com"
}

# Parse command line arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -u|--users)
            USERS="$2"
            shift 2
            ;;
        -r|--ramp)
            RAMP_DURATION="$2"
            shift 2
            ;;
        -d|--duration)
            TEST_DURATION="$2"
            shift 2
            ;;
        -g|--gateway)
            GATEWAY_URL="$2"
            shift 2
            ;;
        -s|--simulation)
            SIMULATION="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            exit 1
            ;;
    esac
done

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Maven is not installed. Please install Maven to run load tests.${NC}"
    exit 1
fi

# Display test configuration
echo -e "${YELLOW}Load Test Configuration:${NC}"
echo "  Gateway URL:    ""$GATEWAY_URL"""
echo "  Users:          ""$USERS"""
echo "  Ramp Duration:  ""$RAMP_DURATION"" seconds"
echo "  Test Duration:  ""$TEST_DURATION"" seconds"
echo "  Simulation:     ""$SIMULATION"""
echo ""

# Check if gateway is reachable
echo -e "${YELLOW}Checking gateway connectivity...${NC}"
if curl -s -o /dev/null -w "%{http_code}" """$GATEWAY_URL""/actuator/health" | grep -q "200"; then
    echo -e "${GREEN}✓ Gateway is reachable${NC}"
else
    echo -e "${RED}✗ Gateway is not reachable at ""$GATEWAY_URL""${NC}"
    echo "Please ensure the gateway is running and accessible."
    exit 1
fi

# Create test users if needed
echo -e "${YELLOW}Preparing test data...${NC}"
for i in {1..10}; do
    ORG_ID="org-""$i"""
    curl -s -X POST """$GATEWAY_URL""/api/v1/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"username\": \"loadtest-""$ORG_ID""\", \"password\": \"test123\", \"organizationId\": \"""$ORG_ID""\"}" \
        > /dev/null 2>&1 || true
done
echo -e "${GREEN}✓ Test data prepared${NC}"

# Run the load test
echo ""
echo -e "${YELLOW}Starting load test...${NC}"
echo "This will take approximately ""$TEST_DURATION"" seconds"
echo ""

# Export environment variables
export GATEWAY_URL
export USERS
export RAMP_DURATION
export TEST_DURATION

# Run Gatling
mvn gatling:test -Dgatling.simulationClass=com.zamaz.mcp.loadtest.$SIMULATION

# Check exit code
if [ "$?" -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ Load test completed successfully${NC}"
    echo "Results are available in: target/gatling/results/"
    
    # Find the latest report
    LATEST_REPORT=$(ls -t target/gatling/results/ | head -1)
    if [ -n """$LATEST_REPORT""" ]; then
        echo "Latest report: target/gatling/results/""$LATEST_REPORT""/index.html"
        
        # Try to open report in browser
        if command -v open &> /dev/null; then
            open "target/gatling/results/""$LATEST_REPORT""/index.html"
        elif command -v xdg-open &> /dev/null; then
            xdg-open "target/gatling/results/""$LATEST_REPORT""/index.html"
        fi
    fi
else
    echo ""
    echo -e "${RED}✗ Load test failed${NC}"
    echo "Check the logs above for error details"
    exit 1
fi