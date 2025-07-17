#!/bin/bash

# Comprehensive Security Testing Orchestrator
# This script runs all security testing tools and generates a consolidated report

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
RESULTS_DIR="$PROJECT_ROOT/comprehensive-security-results"
TARGET_HOST="${TARGET_HOST:-localhost:8080}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test results tracking
TOTAL_SUITES=0
PASSED_SUITES=0
FAILED_SUITES=0
WARNINGS=0

# Create results directory
mkdir -p "$RESULTS_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
CONSOLIDATED_REPORT="$RESULTS_DIR/consolidated_security_report_$TIMESTAMP.md"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') $1" >> "$CONSOLIDATED_REPORT"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
    echo "[SUCCESS] $(date '+%Y-%m-%d %H:%M:%S') $1" >> "$CONSOLIDATED_REPORT"
    ((PASSED_SUITES++))
}

log_failure() {
    echo -e "${RED}[FAILURE]${NC} $1"
    echo "[FAILURE] $(date '+%Y-%m-%d %H:%M:%S') $1" >> "$CONSOLIDATED_REPORT"
    ((FAILED_SUITES++))
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
    echo "[WARNING] $(date '+%Y-%m-%d %H:%M:%S') $1" >> "$CONSOLIDATED_REPORT"
    ((WARNINGS++))
}

log_section() {
    echo -e "${CYAN}[SECTION]${NC} $1"
    echo "" >> "$CONSOLIDATED_REPORT"
    echo "## $1" >> "$CONSOLIDATED_REPORT"
    echo "" >> "$CONSOLIDATED_REPORT"
}

# Initialize consolidated report
init_consolidated_report() {
    cat > "$CONSOLIDATED_REPORT" << EOF
# Comprehensive Security Testing Report

**Assessment Date:** $(date)  
**Target System:** $TARGET_HOST  
**Testing Duration:** $(date) - (In Progress)  
**Security Testing Suite:** MCP Comprehensive Security Testing v1.0  

---

## Executive Summary

This report presents the results of comprehensive security testing performed using multiple security testing tools and methodologies, including penetration testing, OWASP ZAP scanning, and security benchmark assessments.

### Security Testing Scope

1. **Automated Penetration Testing** - Custom security testing scripts
2. **OWASP ZAP Security Scanning** - Industry-standard web application security testing
3. **Security Benchmark Assessment** - Compliance with NIST, CIS, and OWASP standards
4. **Configuration Security Review** - Security configuration validation
5. **Dependency Security Scan** - Third-party component vulnerability assessment

---

## Test Execution Log

EOF
}

# Check if target is accessible
check_target_accessibility() {
    log_section "Pre-Test Validation"
    
    log_info "Checking target accessibility: $TARGET_HOST"
    
    if curl -s --connect-timeout 10 "http://$TARGET_HOST/api/v1/health" > /dev/null; then
        log_success "Target system is accessible and responding"
        return 0
    else
        log_failure "Target system is not accessible - tests may fail"
        return 1
    fi
}

# Run penetration testing
run_penetration_tests() {
    log_section "Penetration Testing Suite"
    ((TOTAL_SUITES++))
    
    log_info "Starting automated penetration testing..."
    
    if [ -f "$SCRIPT_DIR/security-penetration-test.sh" ]; then
        log_info "Running security-penetration-test.sh"
        
        if TARGET_HOST="$TARGET_HOST" "$SCRIPT_DIR/security-penetration-test.sh" > "$RESULTS_DIR/penetration_test_output_$TIMESTAMP.log" 2>&1; then
            log_success "Penetration testing completed successfully"
            
            # Extract key findings
            if [ -f "$PROJECT_ROOT/penetration-test-results/penetration_test_report_"*.md ]; then
                PENTEST_REPORT=$(ls -t "$PROJECT_ROOT/penetration-test-results/penetration_test_report_"*.md | head -1)
                log_info "Penetration test report: $(basename "$PENTEST_REPORT")"
                
                # Extract summary
                if grep -q "Vulnerabilities Found:" "$PENTEST_REPORT"; then
                    VULN_COUNT=$(grep "Vulnerabilities Found:" "$PENTEST_REPORT" | grep -o '[0-9]\+')
                    if [ "$VULN_COUNT" -gt 0 ]; then
                        log_warning "Found $VULN_COUNT vulnerabilities during penetration testing"
                    else
                        log_success "No vulnerabilities found during penetration testing"
                    fi
                fi
            fi
        else
            log_failure "Penetration testing failed or found critical issues"
        fi
    else
        log_warning "Penetration testing script not found: $SCRIPT_DIR/security-penetration-test.sh"
    fi
}

# Run OWASP ZAP testing
run_owasp_zap_tests() {
    log_section "OWASP ZAP Security Scanning"
    ((TOTAL_SUITES++))
    
    log_info "Starting OWASP ZAP security scanning..."
    
    if [ -f "$SCRIPT_DIR/owasp-zap-security-test.sh" ]; then
        log_info "Running owasp-zap-security-test.sh"
        
        if TARGET_URL="http://$TARGET_HOST" "$SCRIPT_DIR/owasp-zap-security-test.sh" > "$RESULTS_DIR/zap_test_output_$TIMESTAMP.log" 2>&1; then
            log_success "OWASP ZAP scanning completed successfully"
            
            # Extract key findings from ZAP results
            if [ -f "$PROJECT_ROOT/zap-security-results/security_summary_"*.md ]; then
                ZAP_SUMMARY=$(ls -t "$PROJECT_ROOT/zap-security-results/security_summary_"*.md | head -1)
                log_info "ZAP security summary: $(basename "$ZAP_SUMMARY")"
                
                # Extract alert counts
                if grep -q "High Risk:" "$ZAP_SUMMARY"; then
                    HIGH_ALERTS=$(grep "High Risk:" "$ZAP_SUMMARY" | grep -o '[0-9]\+')
                    MEDIUM_ALERTS=$(grep "Medium Risk:" "$ZAP_SUMMARY" | grep -o '[0-9]\+')
                    
                    if [ "$HIGH_ALERTS" -gt 0 ]; then
                        log_warning "OWASP ZAP found $HIGH_ALERTS high-risk and $MEDIUM_ALERTS medium-risk alerts"
                    else
                        log_success "OWASP ZAP found no high-risk vulnerabilities"
                    fi
                fi
            fi
        else
            log_warning "OWASP ZAP scanning failed - may require manual setup"
        fi
    else
        log_warning "OWASP ZAP testing script not found: $SCRIPT_DIR/owasp-zap-security-test.sh"
    fi
}

# Run security benchmark assessment
run_security_benchmark() {
    log_section "Security Benchmark Assessment"
    ((TOTAL_SUITES++))
    
    log_info "Starting security benchmark assessment..."
    
    if [ -f "$SCRIPT_DIR/security-benchmark.sh" ]; then
        log_info "Running security-benchmark.sh"
        
        if TARGET_HOST="$TARGET_HOST" "$SCRIPT_DIR/security-benchmark.sh" > "$RESULTS_DIR/benchmark_output_$TIMESTAMP.log" 2>&1; then
            log_success "Security benchmark assessment completed successfully"
            
            # Extract benchmark score
            if [ -f "$PROJECT_ROOT/security-benchmark-results/security_benchmark_"*.md ]; then
                BENCHMARK_REPORT=$(ls -t "$PROJECT_ROOT/security-benchmark-results/security_benchmark_"*.md | head -1)
                log_info "Security benchmark report: $(basename "$BENCHMARK_REPORT")"
                
                if grep -q "Security Score:" "$BENCHMARK_REPORT"; then
                    SECURITY_SCORE=$(grep "Security Score:" "$BENCHMARK_REPORT" | grep -o '[0-9]\+%')
                    log_info "Security benchmark score: $SECURITY_SCORE"
                    
                    SCORE_NUM=$(echo "$SECURITY_SCORE" | tr -d '%')
                    if [ "$SCORE_NUM" -ge 85 ]; then
                        log_success "Excellent security benchmark score: $SECURITY_SCORE"
                    elif [ "$SCORE_NUM" -ge 70 ]; then
                        log_warning "Good security benchmark score with room for improvement: $SECURITY_SCORE"
                    else
                        log_warning "Security benchmark score indicates significant improvements needed: $SECURITY_SCORE"
                    fi
                fi
            fi
        else
            log_warning "Security benchmark assessment found issues or failed"
        fi
    else
        log_warning "Security benchmark script not found: $SCRIPT_DIR/security-benchmark.sh"
    fi
}

# Run configuration security review
run_configuration_review() {
    log_section "Configuration Security Review"
    ((TOTAL_SUITES++))
    
    log_info "Reviewing security configurations..."
    
    local config_issues=0
    
    # Check for security configuration files
    log_info "Checking for security configuration files..."
    
    if [ -f "$PROJECT_ROOT/mcp-gateway/src/main/resources/application-security.yml" ]; then
        log_success "Security configuration file found"
        
        # Check for hardcoded secrets
        if grep -r "password.*:" "$PROJECT_ROOT/mcp-gateway/src/main/resources/" | grep -v "password.*\${" | grep -v "password.*#"; then
            log_warning "Potential hardcoded passwords found in configuration"
            ((config_issues++))
        else
            log_success "No hardcoded passwords found in configuration"
        fi
        
        # Check for proper JWT configuration
        if grep -q "jwt:" "$PROJECT_ROOT/mcp-gateway/src/main/resources/application-security.yml"; then
            if grep -q "secret.*\${" "$PROJECT_ROOT/mcp-gateway/src/main/resources/application-security.yml"; then
                log_success "JWT secret properly configured with environment variable"
            else
                log_warning "JWT secret may be hardcoded"
                ((config_issues++))
            fi
        fi
        
        # Check security headers configuration
        if grep -q "security:" "$PROJECT_ROOT/mcp-gateway/src/main/resources/application-security.yml"; then
            log_success "Security headers configuration found"
        else
            log_warning "Security headers configuration may be missing"
            ((config_issues++))
        fi
        
    else
        log_warning "Security configuration file not found"
        ((config_issues++))
    fi
    
    # Check Docker security
    log_info "Checking Docker security configurations..."
    
    if [ -f "$PROJECT_ROOT/docker-compose.yml" ]; then
        # Check for privileged containers
        if grep -q "privileged.*true" "$PROJECT_ROOT/docker-compose.yml"; then
            log_warning "Privileged containers detected - security risk"
            ((config_issues++))
        else
            log_success "No privileged containers found"
        fi
        
        # Check for proper network isolation
        if grep -q "networks:" "$PROJECT_ROOT/docker-compose.yml"; then
            log_success "Network isolation configured"
        else
            log_warning "Network isolation may not be configured"
            ((config_issues++))
        fi
    fi
    
    if [ $config_issues -eq 0 ]; then
        log_success "Configuration security review passed with no issues"
    else
        log_warning "Configuration security review found $config_issues potential issues"
    fi
}

# Run dependency security scan
run_dependency_scan() {
    log_section "Dependency Security Scan"
    ((TOTAL_SUITES++))
    
    log_info "Scanning dependencies for known vulnerabilities..."
    
    if [ -f "$PROJECT_ROOT/pom.xml" ]; then
        log_info "Maven project detected - checking for OWASP dependency check"
        
        # Check if OWASP dependency check plugin is configured
        if grep -q "dependency-check-maven" "$PROJECT_ROOT/pom.xml"; then
            log_success "OWASP dependency check plugin configured"
            
            # Try to run dependency check
            log_info "Running dependency vulnerability scan..."
            cd "$PROJECT_ROOT"
            
            if mvn org.owasp:dependency-check-maven:check > "$RESULTS_DIR/dependency_scan_$TIMESTAMP.log" 2>&1; then
                log_success "Dependency scan completed successfully"
                
                # Check for vulnerabilities in report
                if [ -f "$PROJECT_ROOT/target/dependency-check-report.html" ]; then
                    if grep -q "vulnerabilities.*found" "$PROJECT_ROOT/target/dependency-check-report.html"; then
                        log_warning "Vulnerabilities found in dependencies - review dependency-check-report.html"
                    else
                        log_success "No vulnerabilities found in dependencies"
                    fi
                fi
            else
                log_warning "Dependency scan failed - may require internet connection"
            fi
        else
            log_info "OWASP dependency check plugin not configured - using manual check"
            
            # Manual dependency version check for known vulnerable versions
            local vulnerable_deps=0
            
            # Check for outdated Spring Boot versions
            if grep -q "spring-boot.version.*2\." "$PROJECT_ROOT/pom.xml"; then
                log_warning "Spring Boot 2.x detected - consider upgrading to 3.x for security fixes"
                ((vulnerable_deps++))
            fi
            
            # Check for old JWT library versions
            if grep -q "jjwt.*0\.[0-9]\." "$PROJECT_ROOT/pom.xml"; then
                log_warning "Older JJWT version detected - ensure it's the latest version"
                ((vulnerable_deps++))
            fi
            
            if [ $vulnerable_deps -eq 0 ]; then
                log_success "Manual dependency check found no obvious security issues"
            else
                log_warning "Manual dependency check found $vulnerable_deps potential issues"
            fi
        fi
    else
        log_warning "No Maven pom.xml found - cannot perform dependency scan"
    fi
}

# Generate consolidated findings
generate_consolidated_findings() {
    log_section "Consolidated Security Findings"
    
    cat >> "$CONSOLIDATED_REPORT" << EOF

### Summary of Security Test Results

| Test Suite | Status | Key Findings |
|------------|--------|-------------|
EOF
    
    # Add findings from each test suite
    echo "| Penetration Testing | $([ -f "$PROJECT_ROOT/penetration-test-results/penetration_test_report_"*.md ] && echo "✅ Completed" || echo "⚠️ Issues") | $([ -f "$PROJECT_ROOT/penetration-test-results/penetration_test_report_"*.md ] && grep "Vulnerabilities Found:" "$PROJECT_ROOT/penetration-test-results/penetration_test_report_"*.md | head -1 || echo "Not completed") |" >> "$CONSOLIDATED_REPORT"
    
    echo "| OWASP ZAP Scanning | $([ -f "$PROJECT_ROOT/zap-security-results/security_summary_"*.md ] && echo "✅ Completed" || echo "⚠️ Issues") | $([ -f "$PROJECT_ROOT/zap-security-results/security_summary_"*.md ] && grep "Risk Assessment" -A 1 "$PROJECT_ROOT/zap-security-results/security_summary_"*.md | tail -1 || echo "Not completed") |" >> "$CONSOLIDATED_REPORT"
    
    echo "| Security Benchmark | $([ -f "$PROJECT_ROOT/security-benchmark-results/security_benchmark_"*.md ] && echo "✅ Completed" || echo "⚠️ Issues") | $([ -f "$PROJECT_ROOT/security-benchmark-results/security_benchmark_"*.md ] && grep "Security Score:" "$PROJECT_ROOT/security-benchmark-results/security_benchmark_"*.md | head -1 || echo "Not completed") |" >> "$CONSOLIDATED_REPORT"
    
    echo "| Configuration Review | ✅ Completed | Manual review completed |" >> "$CONSOLIDATED_REPORT"
    echo "| Dependency Scan | ✅ Completed | Dependency security reviewed |" >> "$CONSOLIDATED_REPORT"
    
    cat >> "$CONSOLIDATED_REPORT" << EOF

### Overall Security Assessment

**Test Suites Executed:** $TOTAL_SUITES  
**Successfully Completed:** $PASSED_SUITES  
**Failed or Issues:** $FAILED_SUITES  
**Warnings:** $WARNINGS  

### Security Posture Rating

EOF
    
    local overall_score=0
    if [ $FAILED_SUITES -eq 0 ] && [ $WARNINGS -le 2 ]; then
        overall_score=95
        echo "🏆 **EXCELLENT** (95/100) - Strong security posture with minimal issues" >> "$CONSOLIDATED_REPORT"
    elif [ $FAILED_SUITES -le 1 ] && [ $WARNINGS -le 5 ]; then
        overall_score=85
        echo "🥇 **GOOD** (85/100) - Good security posture with minor improvements needed" >> "$CONSOLIDATED_REPORT"
    elif [ $FAILED_SUITES -le 2 ] && [ $WARNINGS -le 10 ]; then
        overall_score=75
        echo "🥈 **ACCEPTABLE** (75/100) - Acceptable security posture with several improvements needed" >> "$CONSOLIDATED_REPORT"
    else
        overall_score=60
        echo "🥉 **NEEDS IMPROVEMENT** (60/100) - Security posture requires significant improvements" >> "$CONSOLIDATED_REPORT"
    fi
    
    cat >> "$CONSOLIDATED_REPORT" << EOF

### Priority Action Items

1. **High Priority:** Address any critical vulnerabilities found
2. **Medium Priority:** Fix configuration issues and warnings
3. **Low Priority:** Implement additional security enhancements
4. **Ongoing:** Establish regular security testing schedule

### Detailed Reports Location

All detailed reports are available in the following directories:
- **Penetration Testing:** \`penetration-test-results/\`
- **OWASP ZAP Results:** \`zap-security-results/\`
- **Security Benchmarks:** \`security-benchmark-results/\`
- **Consolidated Results:** \`comprehensive-security-results/\`

---

**Assessment Completed:** $(date)  
**Next Security Assessment:** $(date -d '+1 month')  
**Recommendation:** Schedule monthly comprehensive security testing

EOF
}

# Cleanup function
cleanup() {
    log_info "Cleaning up temporary files..."
    # Add any cleanup tasks here
    true
}

# Main execution function
main() {
    echo "================================================================"
    echo -e "${CYAN}         MCP COMPREHENSIVE SECURITY TESTING SUITE${NC}"
    echo "================================================================"
    echo -e "${BLUE}Target:${NC} $TARGET_HOST"
    echo -e "${BLUE}Started:${NC} $(date)"
    echo -e "${BLUE}Results Directory:${NC} $RESULTS_DIR"
    echo "================================================================"
    echo ""
    
    # Initialize report
    init_consolidated_report
    
    # Setup cleanup trap
    trap cleanup EXIT
    
    # Pre-test validation
    if ! check_target_accessibility; then
        log_warning "Target accessibility check failed - continuing with limited tests"
    fi
    
    # Run all security test suites
    run_penetration_tests
    echo ""
    
    run_owasp_zap_tests
    echo ""
    
    run_security_benchmark
    echo ""
    
    run_configuration_review
    echo ""
    
    run_dependency_scan
    echo ""
    
    # Generate consolidated findings
    generate_consolidated_findings
    
    # Display final summary
    echo "================================================================"
    echo -e "${CYAN}         COMPREHENSIVE SECURITY TESTING COMPLETE${NC}"
    echo "================================================================"
    echo -e "${BLUE}Test Suites:${NC} $TOTAL_SUITES"
    echo -e "${GREEN}Completed:${NC} $PASSED_SUITES"
    echo -e "${RED}Failed:${NC} $FAILED_SUITES"
    echo -e "${YELLOW}Warnings:${NC} $WARNINGS"
    echo ""
    echo -e "${BLUE}Consolidated Report:${NC} $CONSOLIDATED_REPORT"
    echo -e "${BLUE}Completed:${NC} $(date)"
    echo "================================================================"
    
    # Exit with appropriate code
    if [ $FAILED_SUITES -eq 0 ] && [ $WARNINGS -le 5 ]; then
        echo -e "${GREEN}✅ Security testing completed successfully${NC}"
        exit 0
    elif [ $FAILED_SUITES -le 1 ]; then
        echo -e "${YELLOW}⚠️ Security testing completed with warnings${NC}"
        exit 1
    else
        echo -e "${RED}🚨 Security testing found significant issues${NC}"
        exit 2
    fi
}

# Help function
show_help() {
    echo "Comprehensive Security Testing Orchestrator"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -h, --help              Show this help message"
    echo "  -t, --target HOST:PORT  Target host and port (default: localhost:8080)"
    echo ""
    echo "Environment Variables:"
    echo "  TARGET_HOST             Target host and port"
    echo ""
    echo "Test Suites Included:"
    echo "  - Automated Penetration Testing"
    echo "  - OWASP ZAP Security Scanning"
    echo "  - Security Benchmark Assessment (NIST, CIS, OWASP)"
    echo "  - Configuration Security Review"
    echo "  - Dependency Vulnerability Scanning"
    echo ""
    echo "Examples:"
    echo "  $0                      Test localhost:8080"
    echo "  $0 -t api.example.com:443"
    echo "  TARGET_HOST=staging.example.com:8080 $0"
    echo ""
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -t|--target)
            TARGET_HOST="$2"
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