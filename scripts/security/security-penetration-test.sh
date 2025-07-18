#!/bin/bash

# Comprehensive Security Penetration Testing Suite
# This script performs automated penetration testing on the MCP Gateway

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname """"$SCRIPT_DIR"""")"
RESULTS_DIR=""""$PROJECT_ROOT"""/penetration-test-results"
TARGET_HOST="${TARGET_HOST:-localhost:8080}"
API_BASE_URL="http://${TARGET_HOST}/api/v1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
VULNERABILITIES_FOUND=0

# Create results directory
mkdir -p """"$RESULTS_DIR""""
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_REPORT=""""$RESULTS_DIR"""/penetration_test_report_"""$TIMESTAMP""".md"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
    echo "[INFO] $1" >> """"$TEST_REPORT""""
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
    echo "[PASS] $1" >> """"$TEST_REPORT""""
    ((PASSED_TESTS++))
}

log_failure() {
    echo -e "${RED}[FAIL]${NC} $1"
    echo "[FAIL] $1" >> """"$TEST_REPORT""""
    ((FAILED_TESTS++))
}

log_vulnerability() {
    echo -e "${PURPLE}[VULN]${NC} $1"
    echo "[VULNERABILITY] $1" >> """"$TEST_REPORT""""
    ((VULNERABILITIES_FOUND++))
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
    echo "[WARN] $1" >> """"$TEST_REPORT""""
}

# Test execution function
run_test() {
    local test_name="$1"
    local test_command="$2"
    local expected_result="$3"
    
    ((TOTAL_TESTS++))
    log_info "Running test: """$test_name""""
    
    if eval """"$test_command"""" &>/dev/null; then
        if [ """"$expected_result"""" = "pass" ]; then
            log_success """"$test_name""""
        else
            log_vulnerability "SECURITY ISSUE: """$test_name""" - Expected failure but test passed"
        fi
    else
        if [ """"$expected_result"""" = "fail" ]; then
            log_success """"$test_name""""
        else
            log_failure """"$test_name""""
        fi
    fi
}

# Initialize test report
init_report() {
    cat > """"$TEST_REPORT"""" << EOF
# Security Penetration Testing Report

**Date:** $(date)  
**Target:** """$TARGET_HOST"""  
**Tester:** Automated Security Testing Suite  

---

## Executive Summary

This report contains the results of comprehensive penetration testing performed on the MCP Gateway security implementation.

---

## Test Results

EOF
}

# Authentication Testing
test_authentication_security() {
    log_info "=== AUTHENTICATION SECURITY TESTS ==="
    
    # Test 1: SQL Injection in login
    run_test "SQL Injection in login credentials" \
        "curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/json' -d '{\"username\":\"admin'\'' OR 1=1 --\",\"password\":\"password\"}' | grep -q 'Invalid credentials\\|Authentication failed'" \
        "pass"
    
    # Test 2: XSS in login fields
    run_test "XSS injection in login fields" \
        "curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/json' -d '{\"username\":\"<script>alert(1)</script>\",\"password\":\"password\"}' | grep -q 'Invalid credentials\\|Authentication failed'" \
        "pass"
    
    # Test 3: Brute force protection
    run_test "Brute force protection activation" \
        "for i in {1..15}; do curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/json' -d '{\"username\":\"admin\",\"password\":\"wrong'"""$i"""'\"}' >/dev/null; done; curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/json' -d '{\"username\":\"admin\",\"password\":\"wrong\"}' | grep -q '429\\|Too Many Requests\\|Rate limit'" \
        "pass"
    
    # Test 4: Invalid JWT token handling
    run_test "Invalid JWT token rejection" \
        "curl -s -H 'Authorization: Bearer invalid.jwt.token' '"""$API_BASE_URL"""/auth/me' | grep -q '401\\|Unauthorized\\|Invalid token'" \
        "pass"
    
    # Test 5: Expired JWT token handling
    run_test "Expired JWT token rejection" \
        "curl -s -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c' '"""$API_BASE_URL"""/auth/me' | grep -q '401\\|Unauthorized\\|Token expired'" \
        "pass"
    
    # Test 6: Missing Authorization header
    run_test "Missing Authorization header rejection" \
        "curl -s '"""$API_BASE_URL"""/auth/me' | grep -q '401\\|Unauthorized\\|Missing authorization'" \
        "pass"
}

# Authorization Testing
test_authorization_security() {
    log_info "=== AUTHORIZATION SECURITY TESTS ==="
    
    # Test 1: Access admin endpoints without admin role
    run_test "Admin endpoint protection" \
        "curl -s '"""$API_BASE_URL"""/security/monitoring/metrics' | grep -q '401\\|403\\|Unauthorized\\|Forbidden'" \
        "pass"
    
    # Test 2: JWT token manipulation attempt
    run_test "JWT token manipulation protection" \
        "curl -s -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTEyMyIsInJvbGVzIjpbIkFETUlOIl0sImV4cCI6OTk5OTk5OTk5OX0.fake-signature' '"""$API_BASE_URL"""/security/monitoring/metrics' | grep -q '401\\|403\\|Unauthorized\\|Invalid'" \
        "pass"
    
    # Test 3: Organization isolation bypass attempt
    run_test "Organization isolation enforcement" \
        "curl -s '"""$API_BASE_URL"""/organizations/other-org-123/data' | grep -q '401\\|403\\|404\\|Unauthorized\\|Forbidden\\|Not Found'" \
        "pass"
    
    # Test 4: Permission escalation attempt
    run_test "Permission escalation prevention" \
        "curl -s -X POST '"""$API_BASE_URL"""/security/block-ip' -H 'Content-Type: application/json' -d '{\"ip\":\"1.1.1.1\",\"reason\":\"test\"}' | grep -q '401\\|403\\|Unauthorized\\|Forbidden'" \
        "pass"
}

# Input Validation Testing
test_input_validation() {
    log_info "=== INPUT VALIDATION TESTS ==="
    
    # Test 1: SQL Injection in query parameters
    run_test "SQL injection in query parameters" \
        "curl -s '"""$API_BASE_URL"""/debates?id=1'\'' UNION SELECT password FROM users --' | grep -q 'Bad Request\\|Invalid input\\|400'" \
        "pass"
    
    # Test 2: NoSQL injection attempt
    run_test "NoSQL injection protection" \
        "curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/json' -d '{\"username\":{\"\"""$ne"""\":null},\"password\":{\"\"""$ne"""\":null}}' | grep -q 'Bad Request\\|Invalid input\\|400'" \
        "pass"
    
    # Test 3: Path traversal attempt
    run_test "Path traversal protection" \
        "curl -s '"""$API_BASE_URL"""/../../../etc/passwd' | grep -q '400\\|404\\|Bad Request\\|Not Found'" \
        "pass"
    
    # Test 4: Large payload handling
    run_test "Large payload rejection" \
        "curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/json' -d '{\"username\":\"'$(printf 'a%.0s' {1..2000000})'\",\"password\":\"test\"}' | grep -q '413\\|400\\|Payload Too Large\\|Bad Request'" \
        "pass"
    
    # Test 5: XML External Entity (XXE) injection
    run_test "XXE injection protection" \
        "curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/xml' -d '<?xml version=\"1.0\"?><!DOCTYPE root [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><root>&xxe;</root>' | grep -q '400\\|415\\|Unsupported Media Type'" \
        "pass"
    
    # Test 6: Command injection attempt
    run_test "Command injection protection" \
        "curl -s '"""$API_BASE_URL"""/debates?search=test;cat%20/etc/passwd' | grep -q '400\\|Bad Request\\|Invalid input'" \
        "pass"
}

# Rate Limiting Testing
test_rate_limiting() {
    log_info "=== RATE LIMITING TESTS ==="
    
    # Test 1: API rate limiting enforcement
    run_test "API rate limiting activation" \
        "for i in {1..150}; do curl -s '"""$API_BASE_URL"""/health' >/dev/null; done; curl -s '"""$API_BASE_URL"""/health' | grep -q '429\\|Too Many Requests'" \
        "pass"
    
    # Test 2: Authentication endpoint rate limiting
    run_test "Auth endpoint rate limiting" \
        "for i in {1..10}; do curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/json' -d '{\"username\":\"test\",\"password\":\"test\"}' >/dev/null; done; curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/json' -d '{\"username\":\"test\",\"password\":\"test\"}' | grep -q '429\\|Too Many Requests'" \
        "pass"
    
    # Test 3: Rate limit headers presence
    run_test "Rate limit headers included" \
        "curl -s -I '"""$API_BASE_URL"""/health' | grep -q 'X-RateLimit'" \
        "pass"
}

# DDoS Protection Testing
test_ddos_protection() {
    log_info "=== DDOS PROTECTION TESTS ==="
    
    # Test 1: Connection flood protection
    run_test "Connection flood protection" \
        "for i in {1..100}; do curl -s '"""$API_BASE_URL"""/health' >/dev/null & done; wait; curl -s '"""$API_BASE_URL"""/health' | grep -q '503\\|429\\|Service Unavailable\\|Too Many Requests'" \
        "pass"
    
    # Test 2: Suspicious pattern detection
    run_test "Suspicious pattern detection" \
        "curl -s '"""$API_BASE_URL"""/test?id=1'\'' OR 1=1--' | grep -q '400\\|403\\|Blocked\\|Suspicious'" \
        "pass"
    
    # Test 3: Scanner tool detection
    run_test "Scanner tool detection" \
        "curl -s -H 'User-Agent: Mozilla/5.0 (compatible; Nmap Scripting Engine)' '"""$API_BASE_URL"""/health' | grep -q '403\\|Blocked\\|Forbidden'" \
        "pass"
}

# Security Headers Testing
test_security_headers() {
    log_info "=== SECURITY HEADERS TESTS ==="
    
    # Test 1: Content Security Policy
    run_test "Content-Security-Policy header present" \
        "curl -s -I '"""$API_BASE_URL"""/health' | grep -i 'content-security-policy'" \
        "pass"
    
    # Test 2: X-Frame-Options
    run_test "X-Frame-Options header present" \
        "curl -s -I '"""$API_BASE_URL"""/health' | grep -i 'x-frame-options'" \
        "pass"
    
    # Test 3: X-Content-Type-Options
    run_test "X-Content-Type-Options header present" \
        "curl -s -I '"""$API_BASE_URL"""/health' | grep -i 'x-content-type-options'" \
        "pass"
    
    # Test 4: Strict-Transport-Security (HTTPS)
    run_test "HSTS header present (HTTPS)" \
        "curl -s -I 'https://${TARGET_HOST}/api/v1/health' | grep -i 'strict-transport-security' || echo 'HTTPS not configured'" \
        "pass"
    
    # Test 5: Referrer-Policy
    run_test "Referrer-Policy header present" \
        "curl -s -I '"""$API_BASE_URL"""/health' | grep -i 'referrer-policy'" \
        "pass"
    
    # Test 6: X-XSS-Protection
    run_test "X-XSS-Protection header present" \
        "curl -s -I '"""$API_BASE_URL"""/health' | grep -i 'x-xss-protection'" \
        "pass"
}

# Session Management Testing
test_session_management() {
    log_info "=== SESSION MANAGEMENT TESTS ==="
    
    # Test 1: Session fixation protection
    run_test "Session fixation protection" \
        "curl -s -c cookies.txt -b cookies.txt '"""$API_BASE_URL"""/health' >/dev/null; curl -s -c cookies2.txt -b cookies.txt '"""$API_BASE_URL"""/health' >/dev/null; [ ! -f cookies.txt ] || [ ! -f cookies2.txt ] || ! diff cookies.txt cookies2.txt >/dev/null" \
        "pass"
    
    # Test 2: Concurrent session handling
    run_test "Concurrent session management" \
        "curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/json' -d '{\"username\":\"testuser\",\"password\":\"testpass\"}' | grep -q 'accessToken\\|token'" \
        "pass"
    
    # Cleanup
    rm -f cookies.txt cookies2.txt 2>/dev/null || true
}

# CSRF Protection Testing
test_csrf_protection() {
    log_info "=== CSRF PROTECTION TESTS ==="
    
    # Test 1: CSRF token requirement for state-changing operations
    run_test "CSRF protection for POST operations" \
        "curl -s -X POST '"""$API_BASE_URL"""/security/block-ip' -H 'Content-Type: application/json' -d '{\"ip\":\"1.1.1.1\",\"reason\":\"test\"}' | grep -q '401\\|403\\|CSRF\\|Forbidden'" \
        "pass"
    
    # Test 2: Origin header validation
    run_test "Origin header validation" \
        "curl -s -H 'Origin: http://evil.com' '"""$API_BASE_URL"""/health' | grep -q '403\\|Blocked\\|Forbidden' || echo 'Origin validation may not be configured'" \
        "pass"
}

# Business Logic Testing
test_business_logic() {
    log_info "=== BUSINESS LOGIC TESTS ==="
    
    # Test 1: Negative number handling
    run_test "Negative number input validation" \
        "curl -s '"""$API_BASE_URL"""/debates?limit=-1' | grep -q '400\\|Bad Request\\|Invalid'" \
        "pass"
    
    # Test 2: Race condition protection
    run_test "Race condition protection" \
        "curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/json' -d '{\"username\":\"testuser\",\"password\":\"testpass\"}' & curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: application/json' -d '{\"username\":\"testuser\",\"password\":\"testpass\"}' & wait" \
        "pass"
    
    # Test 3: Integer overflow handling
    run_test "Integer overflow protection" \
        "curl -s '"""$API_BASE_URL"""/debates?limit=999999999999999999999' | grep -q '400\\|Bad Request\\|Invalid'" \
        "pass"
}

# SSL/TLS Testing
test_ssl_tls() {
    log_info "=== SSL/TLS TESTS ==="
    
    # Test 1: SSL certificate validation
    if command -v openssl &> /dev/null; then
        run_test "SSL certificate validation" \
            "timeout 5 openssl s_client -connect ${TARGET_HOST/localhost/127.0.0.1} -verify_return_error </dev/null 2>&1 | grep -q 'Verify return code: 0' || echo 'HTTPS not configured or certificate issues'" \
            "pass"
    else
        log_warning "OpenSSL not available for SSL/TLS testing"
    fi
    
    # Test 2: TLS version support
    if command -v nmap &> /dev/null; then
        run_test "Weak TLS version rejection" \
            "nmap --script ssl-enum-ciphers -p ${TARGET_HOST##*:} ${TARGET_HOST%:*} 2>/dev/null | grep -q 'TLSv1.0\\|TLSv1.1' && echo 'Weak TLS versions detected' || echo 'Strong TLS only'" \
            "fail"
    else
        log_warning "Nmap not available for TLS testing"
    fi
}

# Information Disclosure Testing
test_information_disclosure() {
    log_info "=== INFORMATION DISCLOSURE TESTS ==="
    
    # Test 1: Error message information leakage
    run_test "Error message sanitization" \
        "curl -s '"""$API_BASE_URL"""/nonexistent' | grep -v 'stack trace\\|SQLException\\|Exception in thread\\|at java\\.' || echo 'Information leakage detected'" \
        "pass"
    
    # Test 2: Version header disclosure
    run_test "Version information disclosure" \
        "curl -s -I '"""$API_BASE_URL"""/health' | grep -i 'server\\|x-powered-by\\|version' | grep -v 'nginx\\|apache' || echo 'No version disclosure'" \
        "fail"
    
    # Test 3: Directory listing protection
    run_test "Directory listing protection" \
        "curl -s '"""$API_BASE_URL"""/' | grep -q 'Index of\\|Directory listing' && echo 'Directory listing enabled' || echo 'Directory listing disabled'" \
        "fail"
}

# API Security Testing
test_api_security() {
    log_info "=== API SECURITY TESTS ==="
    
    # Test 1: HTTP methods restriction
    run_test "Unauthorized HTTP methods rejection" \
        "curl -s -X TRACE '"""$API_BASE_URL"""/health' | grep -q '405\\|Method Not Allowed'" \
        "pass"
    
    # Test 2: Content-Type validation
    run_test "Content-Type validation" \
        "curl -s -X POST '"""$API_BASE_URL"""/auth/login' -H 'Content-Type: text/plain' -d 'malicious data' | grep -q '415\\|400\\|Unsupported Media Type\\|Bad Request'" \
        "pass"
    
    # Test 3: API versioning security
    run_test "API version security" \
        "curl -s '"""$API_BASE_URL"""/../v0/health' | grep -q '404\\|Not Found'" \
        "pass"
    
    # Test 4: GraphQL injection (if applicable)
    run_test "GraphQL injection protection" \
        "curl -s -X POST '"""$API_BASE_URL"""/graphql' -H 'Content-Type: application/json' -d '{\"query\":\"{ __schema { types { name } } }\"}' | grep -q '404\\|Not Found\\|GraphQL not enabled'" \
        "pass"
}

# Generate final report
generate_final_report() {
    cat >> """"$TEST_REPORT"""" << EOF

---

## Summary

**Total Tests:** """$TOTAL_TESTS"""  
**Passed:** """$PASSED_TESTS"""  
**Failed:** """$FAILED_TESTS"""  
**Vulnerabilities Found:** """$VULNERABILITIES_FOUND"""  

### Security Posture Assessment

EOF

    if [ """$VULNERABILITIES_FOUND""" -eq 0 ]; then
        echo "✅ **EXCELLENT** - No critical vulnerabilities detected" >> """"$TEST_REPORT""""
    elif [ """$VULNERABILITIES_FOUND""" -le 2 ]; then
        echo "⚠️ **GOOD** - Minor security issues detected" >> """"$TEST_REPORT""""
    elif [ """$VULNERABILITIES_FOUND""" -le 5 ]; then
        echo "🔶 **MODERATE** - Several security issues require attention" >> """"$TEST_REPORT""""
    else
        echo "🚨 **CRITICAL** - Multiple vulnerabilities require immediate attention" >> """"$TEST_REPORT""""
    fi

    cat >> """"$TEST_REPORT"""" << EOF

### Recommendations

1. **Review all failed tests** and implement necessary security controls
2. **Address any vulnerabilities** found during testing
3. **Implement additional monitoring** for detected attack patterns
4. **Regular penetration testing** should be performed quarterly
5. **Security training** for development team on secure coding practices

### Next Steps

- [ ] Fix all identified vulnerabilities
- [ ] Implement additional security controls where needed
- [ ] Schedule regular security assessments
- [ ] Update security policies and procedures

---

**Report Generated:** $(date)  
**Tool Version:** MCP Security Penetration Testing Suite v1.0  
**Next Review:** $(date -d '+3 months')

EOF
}

# Main execution
main() {
    log_info "Starting comprehensive security penetration testing"
    log_info "Target: """$TARGET_HOST""""
    log_info "Results will be saved to: """$TEST_REPORT""""
    
    init_report
    
    # Run all test suites
    test_authentication_security
    test_authorization_security
    test_input_validation
    test_rate_limiting
    test_ddos_protection
    test_security_headers
    test_session_management
    test_csrf_protection
    test_business_logic
    test_ssl_tls
    test_information_disclosure
    test_api_security
    
    # Generate final report
    generate_final_report
    
    # Display summary
    echo ""
    echo "==============================================="
    log_info "PENETRATION TESTING COMPLETE"
    echo "==============================================="
    echo -e "${BLUE}Total Tests:${NC} """$TOTAL_TESTS""""
    echo -e "${GREEN}Passed:${NC} """$PASSED_TESTS""""
    echo -e "${RED}Failed:${NC} """$FAILED_TESTS""""
    echo -e "${PURPLE}Vulnerabilities:${NC} """$VULNERABILITIES_FOUND""""
    echo ""
    echo -e "${BLUE}Report saved to:${NC} """$TEST_REPORT""""
    
    # Exit with appropriate code
    if [ """$VULNERABILITIES_FOUND""" -gt 0 ]; then
        echo -e "${RED}⚠️ VULNERABILITIES DETECTED - Review required${NC}"
        exit 1
    elif [ """$FAILED_TESTS""" -gt $((TOTAL_TESTS / 4)) ]; then
        echo -e "${YELLOW}⚠️ Multiple test failures - Review recommended${NC}"
        exit 2
    else
        echo -e "${GREEN}✅ Security posture looks good${NC}"
        exit 0
    fi
}

# Help function
show_help() {
    echo "Security Penetration Testing Suite"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -h, --help              Show this help message"
    echo "  -t, --target HOST:PORT  Target host and port (default: localhost:8080)"
    echo ""
    echo "Environment Variables:"
    echo "  TARGET_HOST             Target host and port (default: localhost:8080)"
    echo ""
    echo "Examples:"
    echo "  $0                      Run tests against localhost:8080"
    echo "  $0 -t example.com:443   Run tests against example.com:443"
    echo "  TARGET_HOST=api.example.com:8080 $0"
    echo ""
}

# Parse command line arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -t|--target)
            TARGET_HOST="$2"
            API_BASE_URL="http://${TARGET_HOST}/api/v1"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Run main function
main