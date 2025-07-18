#!/bin/bash

# GitHub Integration E2E Test Runner
# This script runs comprehensive end-to-end tests for the GitHub Integration service

set -e

echo "🚀 Starting GitHub Integration E2E Tests"
echo "========================================="

# Configuration
MAVEN_OPTS="-Xmx2g -XX:+UseG1GC"
TEST_PROFILE="e2e-test"
LOG_LEVEL="INFO"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running"
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed or not in PATH"
        exit 1
    fi
    
    # Check Java version
    if ! java -version 2>&1 | grep -q "17\|18\|19\|20\|21"; then
        print_warning "Java 17+ is recommended for optimal performance"
    fi
    
    print_success "Prerequisites check passed"
}

# Clean up previous test runs
cleanup() {
    print_status "Cleaning up previous test runs..."
    
    # Stop any running containers
    docker container prune -f &> /dev/null || true
    
    # Clean Maven target
    mvn clean -q
    
    print_success "Cleanup completed"
}

# Run individual test suites
run_basic_tests() {
    print_status "Running basic E2E tests..."
    
    mvn test -Dtest=GitHubIntegrationE2ETest \
        -Dspring.profiles.active="""$TEST_PROFILE""" \
        -Dmaven.test.failure.ignore=false \
        -Dlogging.level.root="""$LOG_LEVEL""" \
        $MAVEN_OPTS
    
    if [ "$?" -eq 0 ]; then
        print_success "Basic E2E tests passed"
    else
        print_error "Basic E2E tests failed"
        exit 1
    fi
}

run_pr_flow_tests() {
    print_status "Running PR flow E2E tests..."
    
    mvn test -Dtest=PullRequestReviewFlowE2ETest \
        -Dspring.profiles.active="""$TEST_PROFILE""" \
        -Dmaven.test.failure.ignore=false \
        -Dlogging.level.root="""$LOG_LEVEL""" \
        $MAVEN_OPTS
    
    if [ "$?" -eq 0 ]; then
        print_success "PR flow E2E tests passed"
    else
        print_error "PR flow E2E tests failed"
        exit 1
    fi
}

run_performance_tests() {
    print_status "Running performance E2E tests..."
    
    mvn test -Dtest=PerformanceE2ETest \
        -Dspring.profiles.active="""$TEST_PROFILE""" \
        -Dmaven.test.failure.ignore=false \
        -Dlogging.level.root="""$LOG_LEVEL""" \
        $MAVEN_OPTS
    
    if [ "$?" -eq 0 ]; then
        print_success "Performance E2E tests passed"
    else
        print_error "Performance E2E tests failed"
        exit 1
    fi
}

# Run complete test suite
run_complete_suite() {
    print_status "Running complete E2E test suite..."
    
    mvn test -Dtest=GitHubIntegrationE2ETestSuite \
        -Dspring.profiles.active="""$TEST_PROFILE""" \
        -Dmaven.test.failure.ignore=false \
        -Dlogging.level.root="""$LOG_LEVEL""" \
        $MAVEN_OPTS
    
    if [ "$?" -eq 0 ]; then
        print_success "Complete E2E test suite passed"
    else
        print_error "Complete E2E test suite failed"
        exit 1
    fi
}

# Generate test report
generate_report() {
    print_status "Generating test report..."
    
    mvn surefire-report:report site:site -DgenerateReports=false -q
    
    if [ -f target/site/surefire-report.html ]; then
        print_success "Test report generated: target/site/surefire-report.html"
    else
        print_warning "Test report generation failed"
    fi
}

# Main execution
main() {
    local TEST_TYPE=${1:-"all"}
    
    print_status "Test execution type: """$TEST_TYPE""""
    
    check_prerequisites
    cleanup
    
    case """$TEST_TYPE""" in
        "basic")
            run_basic_tests
            ;;
        "pr-flow")
            run_pr_flow_tests
            ;;
        "performance")
            run_performance_tests
            ;;
        "all")
            run_complete_suite
            ;;
        *)
            print_error "Invalid test type: """$TEST_TYPE""""
            print_status "Usage: $0 [basic|pr-flow|performance|all]"
            exit 1
            ;;
    esac
    
    generate_report
    
    print_success "E2E test execution completed successfully!"
}

# Handle script interruption
trap 'print_warning "Test execution interrupted"; exit 1' INT TERM

# Execute main function
main "$@"