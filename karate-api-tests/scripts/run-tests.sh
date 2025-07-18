#!/bin/bash

# Karate API Tests Runner
# This script provides various options for running Karate tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
PROFILE="dev"
TAGS=""
PARALLEL_THREADS=1
OUTPUT_DIR="target/karate-reports"
MAVEN_OPTS="-Xmx2g -XX:+UseG1GC"

print_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -p, --profile PROFILE     Maven profile to use (dev, ci, performance, security) [default: dev]"
    echo "  -t, --tags TAGS          Test tags to run (e.g., @smoke, @regression, @security)"
    echo "  -j, --parallel THREADS   Number of parallel threads [default: 1]"
    echo "  -o, --output DIR         Output directory for reports [default: target/karate-reports]"
    echo "  -s, --services           Start required services before running tests"
    echo "  -c, --clean              Clean previous test results"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --profile ci --tags @smoke --parallel 4"
    echo "  $0 --profile performance --tags @performance --parallel 8"
    echo "  $0 --profile security --tags @security"
    echo "  $0 --tags @regression --services --clean"
}

start_services() {
    echo -e "${BLUE}Starting required services...${NC}"
    
    # Check if docker-compose is available
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}Error: docker-compose not found. Please install docker-compose.${NC}"
        exit 1
    fi
    
    # Start services using docker-compose
    cd "$(dirname "$0")/../.."
    docker-compose -f infrastructure/docker-compose/docker-compose.yml up -d
    
    echo -e "${GREEN}Services started successfully.${NC}"
    echo -e "${YELLOW}Waiting for services to be ready...${NC}"
    
    # Wait for services to be ready
    sleep 30
    
    # Check service health
    check_service_health
}

check_service_health() {
    echo -e "${BLUE}Checking service health...${NC}"
    
    local services=(
        "http://localhost:8080/health:Gateway"
        "http://localhost:5005/health:Organization"
        "http://localhost:5002/health:LLM"
        "http://localhost:5013/health:Controller"
        "http://localhost:5004/health:RAG"
        "http://localhost:5006/health:Template"
    )
    
    local failed_services=()
    
    for service in "${services[@]}"; do
        IFS=':' read -r url name <<< "$service"
        
        if curl -s -f "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ $name service is healthy${NC}"
        else
            echo -e "${RED}✗ $name service is not responding${NC}"
            failed_services+=("$name")
        fi
    done
    
    if [ ${#failed_services[@]} -gt 0 ]; then
        echo -e "${RED}Warning: Some services are not healthy: ${failed_services[*]}${NC}"
        echo -e "${YELLOW}Tests may fail due to unhealthy services.${NC}"
        read -p "Do you want to continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        echo -e "${GREEN}All services are healthy!${NC}"
    fi
}

clean_results() {
    echo -e "${BLUE}Cleaning previous test results...${NC}"
    
    if [ -d "$OUTPUT_DIR" ]; then
        rm -rf "$OUTPUT_DIR"
        echo -e "${GREEN}Cleaned $OUTPUT_DIR${NC}"
    fi
    
    # Clean Maven target directory
    if [ -d "target" ]; then
        rm -rf target/surefire-reports
        rm -rf target/failsafe-reports
        rm -rf target/test-classes
        echo -e "${GREEN}Cleaned Maven target directories${NC}"
    fi
}

run_tests() {
    echo -e "${BLUE}Running Karate API tests...${NC}"
    echo -e "${YELLOW}Profile: $PROFILE${NC}"
    echo -e "${YELLOW}Tags: ${TAGS:-"all"}${NC}"
    echo -e "${YELLOW}Parallel threads: $PARALLEL_THREADS${NC}"
    echo -e "${YELLOW}Output directory: $OUTPUT_DIR${NC}"
    
    # Build Maven command
    local mvn_cmd="mvn clean test -P$PROFILE"
    
    # Add tags if specified
    if [ -n "$TAGS" ]; then
        mvn_cmd="$mvn_cmd -Dkarate.options=\"--tags $TAGS\""
    fi
    
    # Add parallel threads
    mvn_cmd="$mvn_cmd -Dparallel.threads=$PARALLEL_THREADS"
    
    # Add output directory
    mvn_cmd="$mvn_cmd -Dkarate.outputDir=$OUTPUT_DIR"
    
    # Set Maven options
    export MAVEN_OPTS="$MAVEN_OPTS"
    
    # Run tests
    echo -e "${BLUE}Executing: $mvn_cmd${NC}"
    eval "$mvn_cmd"
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}Tests completed successfully!${NC}"
        generate_report
    else
        echo -e "${RED}Tests failed with exit code: $exit_code${NC}"
        generate_report
        exit $exit_code
    fi
}

generate_report() {
    echo -e "${BLUE}Generating test report...${NC}"
    
    # Generate HTML report if it doesn't exist
    if [ -f "$OUTPUT_DIR/karate-summary.html" ]; then
        echo -e "${GREEN}HTML report available at: $OUTPUT_DIR/karate-summary.html${NC}"
    fi
    
    # Generate JSON summary
    if [ -f "$OUTPUT_DIR/karate-summary.json" ]; then
        echo -e "${GREEN}JSON report available at: $OUTPUT_DIR/karate-summary.json${NC}"
        
        # Extract test statistics
        local total_tests=$(jq '.scenarioCount' "$OUTPUT_DIR/karate-summary.json")
        local passed_tests=$(jq '.scenariosPassed' "$OUTPUT_DIR/karate-summary.json")
        local failed_tests=$(jq '.scenariosFailed' "$OUTPUT_DIR/karate-summary.json")
        local execution_time=$(jq '.elapsedTime' "$OUTPUT_DIR/karate-summary.json")
        
        echo -e "${BLUE}Test Summary:${NC}"
        echo -e "  Total scenarios: $total_tests"
        echo -e "  Passed: ${GREEN}$passed_tests${NC}"
        echo -e "  Failed: ${RED}$failed_tests${NC}"
        echo -e "  Execution time: ${execution_time}ms"
    fi
}

run_specific_suite() {
    local suite=$1
    echo -e "${BLUE}Running $suite test suite...${NC}"
    
    case $suite in
        "auth")
            mvn test -Dtest=authentication.AuthTestRunner -P$PROFILE
            ;;
        "organization")
            mvn test -Dtest=organization.OrganizationTestRunner -P$PROFILE
            ;;
        "debate")
            mvn test -Dtest=debate.DebateTestRunner -P$PROFILE
            ;;
        "llm")
            mvn test -Dtest=llm.LlmTestRunner -P$PROFILE
            ;;
        "rag")
            mvn test -Dtest=rag.RagTestRunner -P$PROFILE
            ;;
        "integration")
            mvn test -Dtest=integration.IntegrationTestRunner -P$PROFILE
            ;;
        "performance")
            mvn test -Dtest=performance.PerformanceTestRunner -P$PROFILE
            ;;
        "security")
            mvn test -Dtest=security.SecurityTestRunner -P$PROFILE
            ;;
        *)
            echo -e "${RED}Unknown test suite: $suite${NC}"
            echo -e "${YELLOW}Available suites: auth, organization, debate, llm, rag, integration, performance, security${NC}"
            exit 1
            ;;
    esac
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--profile)
            PROFILE="$2"
            shift 2
            ;;
        -t|--tags)
            TAGS="$2"
            shift 2
            ;;
        -j|--parallel)
            PARALLEL_THREADS="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        -s|--services)
            START_SERVICES=true
            shift
            ;;
        -c|--clean)
            CLEAN_RESULTS=true
            shift
            ;;
        --suite)
            TEST_SUITE="$2"
            shift 2
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            print_usage
            exit 1
            ;;
    esac
done

# Change to project directory
cd "$(dirname "$0")/.."

# Validate profile
if [[ ! "$PROFILE" =~ ^(dev|ci|performance|security)$ ]]; then
    echo -e "${RED}Error: Invalid profile '$PROFILE'. Must be one of: dev, ci, performance, security${NC}"
    exit 1
fi

# Start services if requested
if [ "$START_SERVICES" = true ]; then
    start_services
fi

# Clean results if requested
if [ "$CLEAN_RESULTS" = true ]; then
    clean_results
fi

# Run specific test suite if specified
if [ -n "$TEST_SUITE" ]; then
    run_specific_suite "$TEST_SUITE"
else
    # Run all tests
    run_tests
fi

echo -e "${GREEN}Test execution completed!${NC}"