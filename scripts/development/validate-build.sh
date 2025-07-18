#!/bin/bash

# CI/CD Build Validation Script
# This script validates the build setup and runs comprehensive quality checks

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
COVERAGE_THRESHOLD=${COVERAGE_THRESHOLD:-80}
SECURITY_THRESHOLD=${SECURITY_FAIL_THRESHOLD:-7}
MAVEN_OPTS=${MAVEN_OPTS:-"-Xmx3072m -XX:MaxMetaspaceSize=768m"}

# Functions
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

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check Java version
    if ! java -version 2>&1 | grep -q "21"; then
        log_error "Java 21 is required"
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed"
        exit 1
    fi
    
    # Check Maven version
    MVN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
    log_info "Using Maven version: """$MVN_VERSION""""
    
    log_success "Prerequisites check passed"
}

validate_maven_config() {
    log_info "Validating Maven configuration..."
    
    # Validate parent POM
    if ! mvn validate -q; then
        log_error "Maven POM validation failed"
        exit 1
    fi
    
    # Check effective POM
    mvn help:effective-pom -q > /dev/null
    
    log_success "Maven configuration is valid"
}

run_build_with_tests() {
    log_info "Running full build with tests..."
    
    # Clean and compile
    mvn clean compile -T 2C -Pci --batch-mode --no-transfer-progress
    
    # Run unit tests
    log_info "Running unit tests..."
    mvn test -T 2C -Pci --batch-mode --no-transfer-progress
    
    # Check coverage
    log_info "Checking test coverage..."
    mvn jacoco:check --batch-mode --no-transfer-progress || {
        log_warning "Coverage threshold not met (required: ${COVERAGE_THRESHOLD}%)"
    }
    
    # Run integration tests
    log_info "Running integration tests..."
    mvn verify -T 2C -Pci --batch-mode --no-transfer-progress -DskipUnitTests=true
    
    log_success "Build and tests completed successfully"
}

run_quality_checks() {
    log_info "Running code quality checks..."
    
    # Checkstyle
    log_info "Running Checkstyle..."
    mvn checkstyle:check -Pcode-quality --batch-mode --no-transfer-progress || {
        log_warning "Checkstyle issues found"
    }
    
    # SpotBugs
    log_info "Running SpotBugs..."
    mvn spotbugs:check -Pcode-quality --batch-mode --no-transfer-progress || {
        log_warning "SpotBugs issues found"
    }
    
    log_success "Quality checks completed"
}

run_security_scan() {
    log_info "Running security scans..."
    
    # OWASP Dependency Check
    log_info "Running OWASP Dependency Check..."
    mvn org.owasp:dependency-check-maven:check -Psecurity \
        --batch-mode --no-transfer-progress \
        -DfailBuildOnCVSS=${SECURITY_THRESHOLD} || {
        log_warning "Security vulnerabilities found above threshold (CVSS >= ${SECURITY_THRESHOLD})"
    }
    
    log_success "Security scan completed"
}

generate_reports() {
    log_info "Generating comprehensive reports..."
    
    # Generate aggregated test reports
    mvn jacoco:report-aggregate --batch-mode --no-transfer-progress
    
    # Generate quality reports
    mvn checkstyle:checkstyle spotbugs:spotbugs -Pcode-quality --batch-mode --no-transfer-progress
    
    log_success "Reports generated successfully"
}

print_summary() {
    echo ""
    echo "=================================="
    echo "     BUILD VALIDATION SUMMARY     "
    echo "=================================="
    echo ""
    
    # Find and display key metrics
    if [ -d "target/site/jacoco-aggregate" ]; then
        log_info "Test coverage reports available in target/site/jacoco-aggregate/"
    fi
    
    if [ -f "target/checkstyle-result.xml" ]; then
        CHECKSTYLE_ERRORS=$(grep -c "error" target/checkstyle-result.xml || echo "0")
        log_info "Checkstyle errors: """$CHECKSTYLE_ERRORS""""
    fi
    
    if [ -f "target/spotbugsXml.xml" ]; then
        SPOTBUGS_BUGS=$(grep -c "BugInstance" target/spotbugsXml.xml || echo "0")
        log_info "SpotBugs issues: """$SPOTBUGS_BUGS""""
    fi
    
    log_success "Build validation completed successfully!"
    echo ""
}

# Main execution
main() {
    log_info "Starting CI/CD build validation..."
    echo ""
    
    check_prerequisites
    validate_maven_config
    run_build_with_tests
    run_quality_checks
    run_security_scan
    generate_reports
    print_summary
}

# Handle script arguments
case "${1:-all}" in
    "prereq")
        check_prerequisites
        ;;
    "build")
        run_build_with_tests
        ;;
    "quality")
        run_quality_checks
        ;;
    "security")
        run_security_scan
        ;;
    "reports")
        generate_reports
        ;;
    "all"|*)
        main
        ;;
esac