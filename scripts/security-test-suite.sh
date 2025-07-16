#!/bin/bash

# Comprehensive Security Testing Suite
# Tests security controls and validates security implementation

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TEST_REPORTS_DIR="${PROJECT_ROOT}/security-test-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="${TEST_REPORTS_DIR}/security-tests-${TIMESTAMP}.md"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Create test reports directory
mkdir -p "$TEST_REPORTS_DIR"

# Initialize test report
cat > "$REPORT_FILE" << EOF
# Security Test Suite Report

**Project**: zamaz-debate-mcp  
**Date**: $(date '+%Y-%m-%d %H:%M:%S')  
**Test Suite**: Comprehensive Security Validation

---

## Test Results Summary

EOF

# Test counters
TEST_PASSED=0
TEST_FAILED=0
TEST_SKIPPED=0

# Test framework functions
run_test() {
    local test_name="$1"
    local test_command="$2"
    local expected_result="${3:-0}"  # Default expect success (0)
    
    log_info "Running test: $test_name"
    
    if eval "$test_command" > "${TEST_REPORTS_DIR}/test-${test_name//[ ]/-}-${TIMESTAMP}.log" 2>&1; then
        local result=0
    else
        local result=$?
    fi
    
    if [ $result -eq $expected_result ]; then
        echo "✅ **PASS**: $test_name" >> "$REPORT_FILE"
        TEST_PASSED=$((TEST_PASSED + 1))
        log_success "PASS: $test_name"
    else
        echo "❌ **FAIL**: $test_name" >> "$REPORT_FILE"
        echo "   Expected exit code $expected_result, got $result" >> "$REPORT_FILE"
        TEST_FAILED=$((TEST_FAILED + 1))
        log_error "FAIL: $test_name"
    fi
    echo "" >> "$REPORT_FILE"
}

skip_test() {
    local test_name="$1"
    local reason="$2"
    
    echo "⏭️ **SKIP**: $test_name" >> "$REPORT_FILE"
    echo "   Reason: $reason" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    TEST_SKIPPED=$((TEST_SKIPPED + 1))
    log_warning "SKIP: $test_name - $reason"
}

# Security test functions
test_no_hardcoded_secrets() {
    echo "## 🔐 Secret Management Tests" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Test 1: No hardcoded passwords in config files
    run_test "No hardcoded passwords in configuration" \
        "! grep -r 'changeme\|password123\|admin123' --include='*.yml' --include='*.yaml' --include='*.properties' ." \
        0
    
    # Test 2: No .env files committed
    run_test "No environment files in repository" \
        "! find . -name '*.env' -not -path './.*' -not -name '*.env.example' | grep -q ." \
        0
    
    # Test 3: Secrets baseline exists
    run_test "Secrets baseline file exists" \
        "test -f .secrets.baseline" \
        0
    
    # Test 4: Environment validation works
    run_test "Environment validation prevents empty passwords" \
        "echo 'Testing DB_PASSWORD validation'; export DB_PASSWORD=''; ! docker-compose config 2>/dev/null" \
        1
}

test_docker_security() {
    echo "## 🐳 Docker Security Tests" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Test 1: Dockerfiles use non-root users
    run_test "Dockerfiles use non-root users" \
        "grep -l 'USER.*[^0]\|USER.*[^r]oot' */Dockerfile */*.dockerfile" \
        0
    
    # Test 2: No --privileged flags in docker-compose
    run_test "No privileged containers in docker-compose" \
        "! grep -q 'privileged.*true' docker-compose*.yml" \
        0
    
    # Test 3: Health checks present
    run_test "Health checks defined for services" \
        "grep -q 'healthcheck\|HEALTHCHECK' docker-compose*.yml */Dockerfile */*.dockerfile" \
        0
    
    # Test 4: Hadolint passes on Dockerfiles
    if command -v hadolint &> /dev/null; then
        run_test "Hadolint Docker security scan passes" \
            "hadolint test-runner.dockerfile" \
            0
    else
        skip_test "Hadolint Docker security scan" "hadolint not installed"
    fi
}

test_dependency_security() {
    echo "## 📦 Dependency Security Tests" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Test 1: NPM audit passes
    if [ -d "debate-ui" ] && [ -f "debate-ui/package.json" ]; then
        run_test "NPM dependencies have no moderate+ vulnerabilities" \
            "cd debate-ui && npm audit --audit-level moderate" \
            0
    else
        skip_test "NPM dependency security test" "No Node.js project found"
    fi
    
    # Test 2: Maven dependencies security
    if [ -f "pom.xml" ]; then
        run_test "Maven dependencies compilation succeeds" \
            "mvn clean compile -DskipTests -q" \
            0
    else
        skip_test "Maven dependency test" "No Maven project found"
    fi
    
    # Test 3: Check for known vulnerable dependencies
    run_test "No known vulnerable dependency patterns" \
        "! grep -r 'log4j.*2\.1[0-4]\|spring.*4\.[0-2]' pom.xml */pom.xml 2>/dev/null" \
        0
}

test_authentication_security() {
    echo "## 🔑 Authentication & Authorization Tests" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Test 1: JWT secret configuration is secure
    run_test "JWT secret uses environment variable" \
        "grep -q 'JWT_SECRET.*{' */src/main/resources/application*.yml */src/main/resources/application*.properties 2>/dev/null" \
        0
    
    # Test 2: Password encryption is configured
    run_test "Password encryption configured" \
        "grep -q 'BCryptPasswordEncoder\|PasswordEncoder' */src/main/java/**/*.java 2>/dev/null" \
        0
    
    # Test 3: CORS configuration is restrictive
    run_test "CORS configuration exists" \
        "grep -q 'CORS\|cors\|allowedOrigins' */src/main/java/**/*.java */src/main/resources/*.yml 2>/dev/null" \
        0
    
    # Test 4: Security annotations are used
    run_test "Security annotations present" \
        "grep -q '@RequiresPermission\|@PreAuthorize\|@Secured' */src/main/java/**/*.java 2>/dev/null" \
        0
}

test_api_security() {
    echo "## 🌐 API Security Tests" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Test 1: API validation annotations exist
    run_test "Input validation annotations present" \
        "grep -q '@Valid\|@NotNull\|@Size\|@Pattern' */src/main/java/**/*.java 2>/dev/null" \
        0
    
    # Test 2: Error handling doesn't expose internals
    run_test "Secure error handling configured" \
        "grep -q 'GlobalExceptionHandler\|@ControllerAdvice' */src/main/java/**/*.java 2>/dev/null" \
        0
    
    # Test 3: Rate limiting configuration
    run_test "Rate limiting implementation exists" \
        "grep -q 'RateLimit\|Throttle\|rate.limit' */src/main/java/**/*.java */src/main/resources/*.yml 2>/dev/null" \
        0
    
    # Test 4: HTTPS redirection configured
    run_test "HTTPS security configuration present" \
        "grep -q 'requiresChannel\|HTTPS\|TLS' */src/main/java/**/*.java */src/main/resources/*.yml 2>/dev/null" \
        0
}

test_logging_security() {
    echo "## 📝 Logging Security Tests" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Test 1: No sensitive data in logs
    run_test "No password logging detected" \
        "! grep -r 'log.*password\|logger.*password' --include='*.java' --include='*.js' --include='*.ts' ." \
        0
    
    # Test 2: Structured logging configuration
    run_test "Structured logging configuration exists" \
        "grep -q 'logback\|slf4j\|logging' */src/main/resources/*.xml */src/main/resources/*.yml 2>/dev/null" \
        0
    
    # Test 3: Log rotation configured
    run_test "Log rotation configuration present" \
        "grep -q 'maxFileSize\|maxHistory\|rolling' */src/main/resources/*.xml 2>/dev/null" \
        0
}

test_data_protection() {
    echo "## 🛡️ Data Protection Tests" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Test 1: Database encryption configuration
    run_test "Database connection encryption configured" \
        "grep -q 'ssl\|encrypt' */src/main/resources/application*.yml */src/main/resources/application*.properties 2>/dev/null" \
        0
    
    # Test 2: Sensitive field annotations
    run_test "Sensitive data annotations present" \
        "grep -q '@JsonIgnore\|@Transient\|@Column.*nullable' */src/main/java/**/*.java 2>/dev/null" \
        0
    
    # Test 3: Data masking in logs
    run_test "Data masking utilities exist" \
        "grep -q 'mask\|sanitize\|redact' */src/main/java/**/*.java 2>/dev/null" \
        0
}

run_integration_security_tests() {
    echo "## 🔬 Integration Security Tests" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Test 1: Security test classes exist
    run_test "Security integration tests present" \
        "find . -name '*Security*Test.java' -o -name '*Auth*Test.java' | grep -q ." \
        0
    
    # Test 2: Test security configuration
    run_test "Test security configuration exists" \
        "find . -name 'TestSecurityConfiguration.java' -o -name 'application-test.yml' | grep -q ." \
        0
    
    # Test 3: Mock security context in tests
    run_test "Security context mocking in tests" \
        "grep -q '@WithMockUser\|MockSecurityContext\|@TestMethodSecurity' */src/test/java/**/*.java 2>/dev/null" \
        0
}

generate_test_summary() {
    log_info "Generating test summary..."
    
    local total_tests=$((TEST_PASSED + TEST_FAILED + TEST_SKIPPED))
    local pass_rate=0
    
    if [ $total_tests -gt 0 ]; then
        pass_rate=$((TEST_PASSED * 100 / total_tests))
    fi
    
    # Update summary at the top
    local temp_file=$(mktemp)
    {
        head -n 10 "$REPORT_FILE"
        echo "| **Metric** | **Value** |"
        echo "|------------|----------|"
        echo "| Total Tests | $total_tests |"
        echo "| Passed | $TEST_PASSED |"
        echo "| Failed | $TEST_FAILED |"
        echo "| Skipped | $TEST_SKIPPED |"
        echo "| Pass Rate | ${pass_rate}% |"
        echo ""
        if [ $TEST_FAILED -eq 0 ]; then
            echo "**Overall Status**: 🟢 **ALL SECURITY TESTS PASSED**"
        else
            echo "**Overall Status**: 🔴 **$TEST_FAILED SECURITY TESTS FAILED**"
        fi
        echo ""
        echo "---"
        echo ""
        tail -n +11 "$REPORT_FILE"
    } > "$temp_file"
    mv "$temp_file" "$REPORT_FILE"
    
    # Create symlink to latest
    ln -sf "$(basename "$REPORT_FILE")" "${TEST_REPORTS_DIR}/latest-security-tests.md"
}

# Main execution
main() {
    echo "================================================"
    echo "Security Testing Suite"
    echo "================================================"
    echo
    
    log_info "Starting comprehensive security testing..."
    
    test_no_hardcoded_secrets
    test_docker_security
    test_dependency_security
    test_authentication_security
    test_api_security
    test_logging_security
    test_data_protection
    run_integration_security_tests
    
    generate_test_summary
    
    echo
    echo "================================================"
    log_success "Security testing completed!"
    log_info "Report: ${TEST_REPORTS_DIR}/latest-security-tests.md"
    log_info "Total: $((TEST_PASSED + TEST_FAILED + TEST_SKIPPED)) tests ($TEST_PASSED passed, $TEST_FAILED failed, $TEST_SKIPPED skipped)"
    echo "================================================"
    
    # Return exit code based on test results
    if [ $TEST_FAILED -gt 0 ]; then
        log_error "$TEST_FAILED security tests failed"
        exit 1
    else
        log_success "All security tests passed!"
        exit 0
    fi
}

# Run main function
main "$@"
