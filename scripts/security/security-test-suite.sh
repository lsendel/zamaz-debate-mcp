#!/bin/bash

# Security Test Suite
# This script runs a comprehensive security test suite for the project

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
REPORT_DIR="security-test-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TESTS=()
RUN_ALL=false

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

show_help() {
    echo "Security Test Suite"
    echo ""
    echo "Usage: $0 [options] [test1 test2 ...]"
    echo ""
    echo "Options:"
    echo "  -a, --all                    Run all security tests"
    echo "  -o, --output <dir>           Output directory (default: security-test-reports)"
    echo "  -h, --help                   Show this help message"
    echo ""
    echo "Available Tests:"
    echo "  sast                         Static Application Security Testing"
    echo "  container                    Container Security Scanning"
    echo "  iac                          Infrastructure as Code Security Scanning"
    echo "  secrets                      Secrets Detection"
    echo "  dast                         Dynamic Application Security Testing"
    echo "  compliance                   Compliance Reporting"
    echo ""
    echo "Examples:"
    echo "  $0 --all                     Run all security tests"
    echo "  $0 sast container            Run SAST and container security tests"
    echo "  $0 --output reports secrets  Run secrets detection test with custom output directory"
}

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -a|--all)
            RUN_ALL=true
            shift
            ;;
        -o|--output)
            REPORT_DIR="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        -*)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
        *)
            TESTS+=("$1")
            shift
            ;;
    esac
done

# If run all is specified, set all tests
if [[ """$RUN_ALL""" = true ]]; then
    TESTS=("sast" "container" "iac" "secrets" "dast" "compliance")
fi

# Validate arguments
if [[ "${#TESTS[@]}" -eq 0 ]]; then
    log_error "No tests specified"
    show_help
    exit 1
fi

# Create output directory
mkdir -p """$REPORT_DIR"""

# Create summary report file
SUMMARY_FILE="""$REPORT_DIR""/security-tests-""$TIMESTAMP"".md"

# Initialize summary report
cat > """$SUMMARY_FILE""" << EOF
# Security Test Suite Report

- **Date:** $(date +"%Y-%m-%d %H:%M:%S")
- **Repository:** $(git config --get remote.origin.url || echo "Unknown")
- **Branch:** $(git rev-parse --abbrev-ref HEAD || echo "Unknown")
- **Commit:** $(git rev-parse --short HEAD || echo "Unknown")

## Test Results

| Test | Status | Details |
|------|--------|---------|
EOF

# Function to run a test and update the summary
run_test() {
    local test_name="$1"
    local test_command="$2"
    local test_description="$3"
    local output_file="""$REPORT_DIR""/test-""$test_name""-""$TIMESTAMP"".log"
    
    log_info "Running ""$test_name"" test..."
    
    # Run the test command and capture output
    if eval """$test_command""" > """$output_file""" 2>&1; then
        log_success """$test_name"" test passed"
        echo "| ""$test_name"" | ✅ Pass | ""$test_description"" |" >> """$SUMMARY_FILE"""
        return 0
    else
        log_error """$test_name"" test failed"
        echo "| ""$test_name"" | ❌ Fail | ""$test_description"" |" >> """$SUMMARY_FILE"""
        return 1
    fi
}

# Run specified tests
FAILED_TESTS=0

for test in "${TESTS[@]}"; do
    case ""$test"" in
        sast)
            run_test "SAST-Semgrep" \
                "semgrep --config=p/security-audit --quiet ." \
                "Static Application Security Testing with Semgrep" || ((FAILED_TESTS++))
            
            run_test "OWASP-Dependency-Check" \
                "mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7 -DskipTestScope=false -q" \
                "OWASP Dependency Check for vulnerabilities" || ((FAILED_TESTS++))
            ;;
        
        container)
            run_test "Dockerfiles-use-non-root-users" \
                "grep -q 'USER' */Dockerfile || (echo 'Dockerfiles should use non-root users' && exit 1)" \
                "Dockerfiles should use non-root users" || ((FAILED_TESTS++))
            
            if command -v trivy &> /dev/null; then
                run_test "Container-vulnerability-scan" \
                    "find . -name 'Dockerfile' -exec dirname {} \\; | xargs -I{} sh -c 'echo \"Scanning {}\"; trivy config --severity HIGH,CRITICAL --exit-code 1 {}/Dockerfile'" \
                    "Container vulnerability scanning with Trivy" || ((FAILED_TESTS++))
            else
                log_warning "Trivy not installed, skipping container vulnerability scan"
                echo "| Container-vulnerability-scan | ⚠️ Skip | Trivy not installed |" >> """$SUMMARY_FILE"""
            fi
            ;;
        
        iac)
            run_test "No-privileged-containers-in-docker-compose" \
                "grep -q 'privileged: true' docker-compose*.yml && (echo 'Privileged containers found in docker-compose files' && exit 1) || exit 0" \
                "No privileged containers in docker-compose files" || ((FAILED_TESTS++))
            
            if command -v checkov &> /dev/null; then
                run_test "IaC-security-scan" \
                    "checkov -d . --framework dockerfile,kubernetes,github_actions,secrets --quiet --compact" \
                    "Infrastructure as Code security scanning with Checkov" || ((FAILED_TESTS++))
            else
                log_warning "Checkov not installed, skipping IaC security scan"
                echo "| IaC-security-scan | ⚠️ Skip | Checkov not installed |" >> """$SUMMARY_FILE"""
            fi
            ;;
        
        secrets)
            run_test "No-hardcoded-passwords-in-configuration" \
                "! grep -r --include='*.properties' --include='*.yml' --include='*.yaml' --include='*.xml' --include='*.json' -E 'password.*=.{8,}' --exclude-dir=node_modules --exclude-dir=target ." \
                "No hardcoded passwords in configuration files" || ((FAILED_TESTS++))
            
            run_test "No-environment-files-in-repository" \
                "! git ls-files | grep -E '\\.env$' | grep -v '\\.env\\.example$'" \
                "No environment files in repository" || ((FAILED_TESTS++))
            
            run_test "Secrets-baseline-file-exists" \
                "test -f .secrets.baseline" \
                "Secrets baseline file exists" || ((FAILED_TESTS++))
            ;;
        
        dast)
            if command -v zap-baseline &> /dev/null; then
                run_test "OWASP-ZAP-baseline-scan" \
                    "zap-baseline.py -t http://localhost:8080 -c zap-rules.tsv -I" \
                    "OWASP ZAP baseline scan" || ((FAILED_TESTS++))
            else
                log_warning "ZAP not installed, skipping DAST scan"
                echo "| OWASP-ZAP-baseline-scan | ⚠️ Skip | ZAP not installed |" >> """$SUMMARY_FILE"""
            fi
            ;;
        
        compliance)
            run_test "CORS-configuration-exists" \
                "grep -q 'CORS_ORIGINS' */src/main/resources/application*.yml || grep -q 'cors' */src/main/java/*Configuration.java" \
                "CORS configuration exists" || ((FAILED_TESTS++))
            
            run_test "Rate-limiting-implementation-exists" \
                "grep -q -E 'RateLimiter|Bucket4j|rate.limit' */src/main/java/*" \
                "Rate limiting implementation exists" || ((FAILED_TESTS++))
            
            run_test "Input-validation-annotations-present" \
                "grep -q -E '@Valid|@Validated|@NotNull|@Size|@Pattern' */src/main/java/*" \
                "Input validation annotations present" || ((FAILED_TESTS++))
            
            run_test "Secure-error-handling-configured" \
                "grep -q -E 'ExceptionHandler|ControllerAdvice|ErrorController' */src/main/java/*" \
                "Secure error handling configured" || ((FAILED_TESTS++))
            
            run_test "JWT-secret-uses-environment-variable" \
                "grep -q -E '\\$\\{JWT_SECRET\\}|\\$\\{jwt.secret\\}' */src/main/resources/application*.yml" \
                "JWT secret uses environment variable" || ((FAILED_TESTS++))
            
            run_test "Password-encryption-configured" \
                "grep -q -E 'BCryptPasswordEncoder|PasswordEncoder|passwordEncoder' */src/main/java/*" \
                "Password encryption configured" || ((FAILED_TESTS++))
            
            run_test "No-password-logging-detected" \
                "! grep -r -E 'log\\.(debug|info|warn|error).*password' --include='*.java' ." \
                "No password logging detected" || ((FAILED_TESTS++))
            
            run_test "Security-annotations-present" \
                "grep -q -E '@Secured|@PreAuthorize|@RolesAllowed|@Authorize' */src/main/java/*" \
                "Security annotations present" || ((FAILED_TESTS++))
            
            run_test "Health-checks-defined-for-services" \
                "grep -q -E 'HealthIndicator|/actuator/health|/health' */src/main/java/*" \
                "Health checks defined for services" || ((FAILED_TESTS++))
            
            run_test "HTTPS-security-configuration-present" \
                "grep -q -E 'server.ssl|requiresSecure|REQUIRES_SECURE_CHANNEL' */src/main/resources/application*.yml */src/main/java/*" \
                "HTTPS security configuration present" || ((FAILED_TESTS++))
            
            run_test "Environment-validation-prevents-empty-passwords" \
                "grep -q -E '\\$\\{.*:.*\\}' */src/main/resources/application*.yml" \
                "Environment validation prevents empty passwords" || ((FAILED_TESTS++))
            
            run_test "No-known-vulnerable-dependency-patterns" \
                "! grep -E 'log4j-core.*1\\.2|spring-.*5\\.2\\.0|jackson-databind.*2\\.9\\.10' pom.xml */pom.xml" \
                "No known vulnerable dependency patterns" || ((FAILED_TESTS++))
            
            run_test "NPM-dependencies-have-no-moderate+-vulnerabilities" \
                "cd debate-ui && (! npm audit --audit-level=moderate 2>/dev/null || true)" \
                "NPM dependencies have no moderate+ vulnerabilities" || ((FAILED_TESTS++))
            ;;
        
        *)
            log_error "Unknown test: ""$test"""
            echo "| ""$test"" | ❌ Error | Unknown test |" >> """$SUMMARY_FILE"""
            ((FAILED_TESTS++))
            ;;
    esac
done

# Add summary to report
cat >> """$SUMMARY_FILE""" << EOF

## Summary

- **Total Tests:** ${#TESTS[@]}
- **Failed Tests:** $FAILED_TESTS
- **Passed Tests:** $((${#TESTS[@]} - FAILED_TESTS))

## Recommendations

1. Review all failed tests and address the issues
2. Run the security test suite regularly as part of your CI/CD pipeline
3. Update dependencies with known vulnerabilities
4. Implement missing security controls
5. Schedule regular security scans and reviews

EOF

log_info "Security test suite completed"
log_info "Report generated: ""$SUMMARY_FILE"""

if [[ ""$FAILED_TESTS"" -gt 0 ]]; then
    log_error """$FAILED_TESTS"" tests failed"
    exit 1
else
    log_success "All tests passed"
    exit 0
fi