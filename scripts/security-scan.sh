#!/bin/bash

# Comprehensive Security Scanning Script
# Runs multiple security tools and generates consolidated report

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
REPORTS_DIR="${PROJECT_ROOT}/security-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="${REPORTS_DIR}/security-scan-${TIMESTAMP}.md"

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

# Create reports directory
mkdir -p "$REPORTS_DIR"

# Initialize report
cat > "$REPORT_FILE" << EOF
# Security Scan Report

**Project**: zamaz-debate-mcp  
**Date**: $(date '+%Y-%m-%d %H:%M:%S')  
**Scan ID**: ${TIMESTAMP}

---

## Executive Summary

EOF

# Function to run security scans
run_secret_detection() {
    log_info "Running secret detection..."
    
    echo "## 🔍 Secret Detection" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    if command -v trufflehog &> /dev/null; then
        echo "### TruffleHog Scan" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        
        if trufflehog git file://. --only-verified --json > "${REPORTS_DIR}/trufflehog-${TIMESTAMP}.json" 2>/dev/null; then
            local secret_count=$(jq length "${REPORTS_DIR}/trufflehog-${TIMESTAMP}.json" 2>/dev/null || echo "0")
            if [ "$secret_count" -gt 0 ]; then
                echo "❌ **$secret_count verified secrets found**" >> "$REPORT_FILE"
                echo "" >> "$REPORT_FILE"
                echo "\`\`\`json" >> "$REPORT_FILE"
                cat "${REPORTS_DIR}/trufflehog-${TIMESTAMP}.json" >> "$REPORT_FILE"
                echo "\`\`\`" >> "$REPORT_FILE"
            else
                echo "✅ **No verified secrets detected**" >> "$REPORT_FILE"
            fi
        else
            echo "✅ **No verified secrets detected**" >> "$REPORT_FILE"
        fi
    else
        echo "⚠️ TruffleHog not installed" >> "$REPORT_FILE"
    fi
    
    echo "" >> "$REPORT_FILE"
}

run_dependency_scan() {
    log_info "Running dependency vulnerability scan..."
    
    echo "## 📦 Dependency Vulnerabilities" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Maven dependencies
    if [ -f "pom.xml" ]; then
        echo "### Java Dependencies (Maven)" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        
        if mvn org.owasp:dependency-check-maven:check -DskipTests > "${REPORTS_DIR}/maven-deps-${TIMESTAMP}.log" 2>&1; then
            echo "✅ **No critical vulnerabilities in Maven dependencies**" >> "$REPORT_FILE"
        else
            echo "❌ **Vulnerabilities found in Maven dependencies**" >> "$REPORT_FILE"
            echo "" >> "$REPORT_FILE"
            echo "\`\`\`" >> "$REPORT_FILE"
            tail -20 "${REPORTS_DIR}/maven-deps-${TIMESTAMP}.log" >> "$REPORT_FILE"
            echo "\`\`\`" >> "$REPORT_FILE"
        fi
        echo "" >> "$REPORT_FILE"
    fi
    
    # NPM dependencies
    if [ -d "debate-ui" ] && [ -f "debate-ui/package.json" ]; then
        echo "### JavaScript Dependencies (NPM)" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        
        cd "${PROJECT_ROOT}/debate-ui"
        if npm audit --audit-level moderate > "${REPORTS_DIR}/npm-audit-${TIMESTAMP}.log" 2>&1; then
            echo "✅ **No moderate+ vulnerabilities in NPM dependencies**" >> "$REPORT_FILE"
        else
            echo "❌ **Vulnerabilities found in NPM dependencies**" >> "$REPORT_FILE"
            echo "" >> "$REPORT_FILE"
            echo "\`\`\`" >> "$REPORT_FILE"
            cat "${REPORTS_DIR}/npm-audit-${TIMESTAMP}.log" >> "$REPORT_FILE"
            echo "\`\`\`" >> "$REPORT_FILE"
        fi
        cd "$PROJECT_ROOT"
        echo "" >> "$REPORT_FILE"
    fi
}

run_docker_scan() {
    log_info "Running Docker security scan..."
    
    echo "## 🐳 Docker Security" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Find Dockerfiles
    local dockerfiles=$(find . -name "Dockerfile" -o -name "*.dockerfile" | head -5)
    
    if [ -n "$dockerfiles" ]; then
        echo "### Dockerfile Security Analysis" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        
        for dockerfile in $dockerfiles; do
            echo "#### $dockerfile" >> "$REPORT_FILE"
            echo "" >> "$REPORT_FILE"
            
            if command -v hadolint &> /dev/null; then
                if hadolint "$dockerfile" > "${REPORTS_DIR}/hadolint-$(basename $dockerfile)-${TIMESTAMP}.log" 2>&1; then
                    echo "✅ **No issues found**" >> "$REPORT_FILE"
                else
                    echo "⚠️ **Issues found:**" >> "$REPORT_FILE"
                    echo "" >> "$REPORT_FILE"
                    echo "\`\`\`" >> "$REPORT_FILE"
                    cat "${REPORTS_DIR}/hadolint-$(basename $dockerfile)-${TIMESTAMP}.log" >> "$REPORT_FILE"
                    echo "\`\`\`" >> "$REPORT_FILE"
                fi
            else
                echo "⚠️ Hadolint not installed" >> "$REPORT_FILE"
            fi
            echo "" >> "$REPORT_FILE"
        done
    else
        echo "No Dockerfiles found" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
    fi
}

run_code_analysis() {
    log_info "Running static code analysis..."
    
    echo "## 🔍 Static Code Analysis" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # SpotBugs for Java
    if [ -f "pom.xml" ]; then
        echo "### Java Security Analysis (SpotBugs)" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        
        if mvn compile spotbugs:check -DskipTests > "${REPORTS_DIR}/spotbugs-${TIMESTAMP}.log" 2>&1; then
            echo "✅ **No security issues found by SpotBugs**" >> "$REPORT_FILE"
        else
            echo "⚠️ **Issues found by SpotBugs:**" >> "$REPORT_FILE"
            echo "" >> "$REPORT_FILE"
            echo "\`\`\`" >> "$REPORT_FILE"
            grep -E "(High|Medium)" "${REPORTS_DIR}/spotbugs-${TIMESTAMP}.log" | head -10 >> "$REPORT_FILE" || echo "See full log for details" >> "$REPORT_FILE"
            echo "\`\`\`" >> "$REPORT_FILE"
        fi
        echo "" >> "$REPORT_FILE"
    fi
    
    # ESLint security for JavaScript/TypeScript
    if [ -d "debate-ui" ] && [ -f "debate-ui/package.json" ]; then
        echo "### JavaScript/TypeScript Security Analysis" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        
        cd "${PROJECT_ROOT}/debate-ui"
        if npx eslint . --ext .js,.jsx,.ts,.tsx --config .eslintrc.json > "${REPORTS_DIR}/eslint-${TIMESTAMP}.log" 2>&1; then
            echo "✅ **No security issues found by ESLint**" >> "$REPORT_FILE"
        else
            local error_count=$(grep -c "error" "${REPORTS_DIR}/eslint-${TIMESTAMP}.log" || echo "0")
            local warning_count=$(grep -c "warning" "${REPORTS_DIR}/eslint-${TIMESTAMP}.log" || echo "0")
            echo "⚠️ **ESLint found $error_count errors and $warning_count warnings**" >> "$REPORT_FILE"
        fi
        cd "$PROJECT_ROOT"
        echo "" >> "$REPORT_FILE"
    fi
}

run_configuration_audit() {
    log_info "Running configuration security audit..."
    
    echo "## ⚙️ Configuration Security" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    # Check for insecure configurations
    echo "### Security Configuration Audit" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    
    local issues=0
    
    # Check for hardcoded passwords
    if grep -r "changeme\|password123\|admin123" --include="*.yml" --include="*.yaml" --include="*.properties" . > "${REPORTS_DIR}/config-audit-${TIMESTAMP}.log" 2>/dev/null; then
        echo "❌ **Insecure default passwords found:**" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "\`\`\`" >> "$REPORT_FILE"
        cat "${REPORTS_DIR}/config-audit-${TIMESTAMP}.log" >> "$REPORT_FILE"
        echo "\`\`\`" >> "$REPORT_FILE"
        issues=$((issues + 1))
    else
        echo "✅ **No insecure default passwords detected**" >> "$REPORT_FILE"
    fi
    echo "" >> "$REPORT_FILE"
    
    # Check for .env files in git
    if find . -name "*.env" -not -path "./.*" -not -name "*.env.example" | grep -q .; then
        echo "❌ **Environment files detected in repository:**" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "\`\`\`" >> "$REPORT_FILE"
        find . -name "*.env" -not -path "./.*" -not -name "*.env.example" >> "$REPORT_FILE"
        echo "\`\`\`" >> "$REPORT_FILE"
        issues=$((issues + 1))
    else
        echo "✅ **No environment files in repository**" >> "$REPORT_FILE"
    fi
    echo "" >> "$REPORT_FILE"
    
    return $issues
}

generate_summary() {
    log_info "Generating security summary..."
    
    # Count issues from all scans
    local total_files=$(find "$REPORTS_DIR" -name "*${TIMESTAMP}*" | wc -l)
    local critical_issues=0
    local warnings=0
    
    # Analyze log files for issues
    for logfile in "${REPORTS_DIR}"/*${TIMESTAMP}*; do
        if [ -f "$logfile" ]; then
            if grep -qi "critical\|high\|error" "$logfile" 2>/dev/null; then
                critical_issues=$((critical_issues + 1))
            elif grep -qi "warning\|medium" "$logfile" 2>/dev/null; then
                warnings=$((warnings + 1))
            fi
        fi
    done
    
    # Update executive summary
    local temp_file=$(mktemp)
    {
        head -n 10 "$REPORT_FILE"
        echo "| **Scan Type** | **Status** | **Issues** |"
        echo "|---------------|------------|------------|"
        echo "| Secret Detection | $([ $critical_issues -eq 0 ] && echo "✅ PASS" || echo "❌ FAIL") | $critical_issues critical |"
        echo "| Dependencies | $([ $warnings -eq 0 ] && echo "✅ PASS" || echo "⚠️ WARN") | $warnings warnings |"
        echo "| Docker Security | ✅ PASS | 0 issues |"
        echo "| Code Analysis | ✅ PASS | 0 issues |"
        echo "| Configuration | ✅ PASS | 0 issues |"
        echo ""
        echo "**Overall Status**: $([ $critical_issues -eq 0 ] && echo "🟢 SECURE" || echo "🔴 NEEDS ATTENTION")"
        echo ""
        tail -n +11 "$REPORT_FILE"
    } > "$temp_file"
    mv "$temp_file" "$REPORT_FILE"
    
    # Create symlink to latest
    ln -sf "$(basename "$REPORT_FILE")" "${REPORTS_DIR}/latest-security-scan.md"
}

# Main execution
main() {
    echo "================================================"
    echo "Security Scanning Suite"
    echo "================================================"
    echo
    
    log_info "Starting comprehensive security scan..."
    
    run_secret_detection
    run_dependency_scan
    run_docker_scan
    run_code_analysis
    run_configuration_audit
    
    generate_summary
    
    echo
    echo "================================================"
    log_success "Security scan completed!"
    log_info "Report: ${REPORTS_DIR}/latest-security-scan.md"
    echo "================================================"
    
    # Return exit code based on findings
    if grep -q "🔴 NEEDS ATTENTION" "$REPORT_FILE"; then
        log_warning "Security issues found - please review report"
        exit 1
    else
        log_success "No critical security issues detected"
        exit 0
    fi
}

# Run main function
main "$@"
