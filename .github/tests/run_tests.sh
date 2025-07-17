#!/bin/bash

# Test runner script for Kiro GitHub integration
# Provides different test execution modes

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
TEST_TYPE="unit"
COVERAGE=false
VERBOSE=false
MARKERS=""

# Help function
show_help() {
    echo "Kiro GitHub Integration Test Runner"
    echo
    echo "Usage: $0 [OPTIONS]"
    echo
    echo "Options:"
    echo "  -t, --type TYPE      Test type: unit, integration, e2e, performance, all (default: unit)"
    echo "  -m, --marker MARKER  Run tests with specific marker"
    echo "  -c, --coverage       Generate coverage report"
    echo "  -v, --verbose        Verbose output"
    echo "  -h, --help           Show this help message"
    echo
    echo "Examples:"
    echo "  $0 -t unit           # Run unit tests"
    echo "  $0 -t integration -c # Run integration tests with coverage"
    echo "  $0 -t e2e -v         # Run end-to-end tests with verbose output"
    echo "  $0 -m security       # Run only security tests"
    echo "  $0 -t all -c         # Run all tests with coverage"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            TEST_TYPE="$2"
            shift 2
            ;;
        -m|--marker)
            MARKERS="$2"
            shift 2
            ;;
        -c|--coverage)
            COVERAGE=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# Setup Python path
export PYTHONPATH="${PROJECT_ROOT}/.github/scripts:${PYTHONPATH}"

# Change to test directory
cd "$SCRIPT_DIR"

# Install test dependencies if needed
echo -e "${BLUE}Checking test dependencies...${NC}"
pip install -q pytest pytest-asyncio pytest-cov pytest-timeout pytest-mock

# Build pytest command
PYTEST_CMD="python -m pytest"

# Add coverage if requested
if [ "$COVERAGE" = true ]; then
    PYTEST_CMD="$PYTEST_CMD --cov=../scripts --cov-report=html --cov-report=term"
fi

# Add verbose if requested
if [ "$VERBOSE" = true ]; then
    PYTEST_CMD="$PYTEST_CMD -vv"
fi

# Add test selection based on type
case $TEST_TYPE in
    unit)
        echo -e "${BLUE}Running unit tests...${NC}"
        PYTEST_CMD="$PYTEST_CMD -m unit"
        ;;
    integration)
        echo -e "${BLUE}Running integration tests...${NC}"
        PYTEST_CMD="$PYTEST_CMD -m integration"
        ;;
    e2e)
        echo -e "${BLUE}Running end-to-end tests...${NC}"
        PYTEST_CMD="$PYTEST_CMD -m e2e"
        ;;
    performance)
        echo -e "${BLUE}Running performance tests...${NC}"
        PYTEST_CMD="$PYTEST_CMD -m 'performance or load or stress'"
        ;;
    all)
        echo -e "${BLUE}Running all tests...${NC}"
        # No marker filter
        ;;
    *)
        echo -e "${RED}Invalid test type: $TEST_TYPE${NC}"
        show_help
        exit 1
        ;;
esac

# Add custom markers if specified
if [ -n "$MARKERS" ]; then
    PYTEST_CMD="$PYTEST_CMD -m '$MARKERS'"
fi

# Show test configuration
echo -e "${YELLOW}Test Configuration:${NC}"
echo "  Test Type: $TEST_TYPE"
echo "  Coverage: $COVERAGE"
echo "  Verbose: $VERBOSE"
[ -n "$MARKERS" ] && echo "  Markers: $MARKERS"
echo

# Run tests
echo -e "${BLUE}Executing: $PYTEST_CMD${NC}"
echo

if $PYTEST_CMD; then
    echo
    echo -e "${GREEN}✅ All tests passed!${NC}"
    
    if [ "$COVERAGE" = true ]; then
        echo
        echo -e "${BLUE}Coverage report generated in htmlcov/index.html${NC}"
    fi
else
    echo
    echo -e "${RED}❌ Some tests failed!${NC}"
    exit 1
fi

# Run specific test suites
run_security_tests() {
    echo -e "${BLUE}Running security-focused tests...${NC}"
    python -m pytest -m security -v
}

run_quick_tests() {
    echo -e "${BLUE}Running quick smoke tests...${NC}"
    python -m pytest -m "not slow" -x --maxfail=5
}

# Additional test commands
case ${2:-} in
    security)
        run_security_tests
        ;;
    quick)
        run_quick_tests
        ;;
esac