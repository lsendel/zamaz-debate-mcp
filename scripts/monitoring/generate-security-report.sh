#!/bin/bash

# Comprehensive Security Report Generator
# This script generates a comprehensive security report by aggregating results from all security scans

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
REPORT_DIR="./security-reports"
OUTPUT_FORMAT="markdown"
INCLUDE_DETAILS=true
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
REPORT_TITLE="Comprehensive Security Report"

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
    echo "Comprehensive Security Report Generator"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -d, --dir <dir>              Input directory with security reports (default: ./security-reports)"
    echo "  -o, --output <file>          Output file (default: ./security-reports/comprehensive-security-report-TIMESTAMP.md)"
    echo "  -f, --format <format>        Output format: markdown, html, json (default: markdown)"
    echo "  -s, --summary                Generate summary report only (no details)"
    echo "  -t, --title <title>          Report title (default: 'Comprehensive Security Report')"
    echo "  -h, --help                   Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --dir ./reports --format html"
    echo "  $0 --summary --title 'Security Summary Report'"
}

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -d|--dir)
            REPORT_DIR="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -f|--format)
            OUTPUT_FORMAT="$2"
            shift 2
            ;;
        -s|--summary)
            INCLUDE_DETAILS=false
            shift
            ;;
        -t|--title)
            REPORT_TITLE="$2"
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
            log_error "Unknown argument: $1"
            show_help
            exit 1
            ;;
    esac
done

# Set default output file if not specified
if [[ -z "${OUTPUT_FILE:-}" ]]; then
    OUTPUT_FILE="""$REPORT_DIR""/comprehensive-security-report-""$TIMESTAMP"".""$OUTPUT_FORMAT"""
fi

# Create output directory if it doesn't exist
mkdir -p "$(dirname """$OUTPUT_FILE""")"

# Check if input directory exists
if [[ ! -d """$REPORT_DIR""" ]]; then
    log_error "Input directory does not exist: ""$REPORT_DIR"""
    exit 1
fi

# Function to count issues by severity from various report formats
count_issues_by_severity() {
    local report_file="$1"
    local report_type="$2"
    local critical=0
    local high=0
    local medium=0
    local low=0
    
    case ""$report_type"" in
        dependency-check)
            if [[ -f """$report_file""" && """$report_file""" == *json ]]; then
                if command -v jq &> /dev/null; then
                    critical=$(jq '.dependencies[] | select(.vulnerabilities != null) | .vulnerabilities[] | select(.cvssv3 != null and .cvssv3.baseScore >= 9.0) | .cvssv3.baseScore' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    high=$(jq '.dependencies[] | select(.vulnerabilities != null) | .vulnerabilities[] | select(.cvssv3 != null and .cvssv3.baseScore >= 7.0 and .cvssv3.baseScore < 9.0) | .cvssv3.baseScore' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    medium=$(jq '.dependencies[] | select(.vulnerabilities != null) | .vulnerabilities[] | select(.cvssv3 != null and .cvssv3.baseScore >= 4.0 and .cvssv3.baseScore < 7.0) | .cvssv3.baseScore' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    low=$(jq '.dependencies[] | select(.vulnerabilities != null) | .vulnerabilities[] | select(.cvssv3 != null and .cvssv3.baseScore < 4.0) | .cvssv3.baseScore' """$report_file""" 2>/dev/null | wc -l || echo 0)
                fi
            fi
            ;;
        
        trivy)
            if [[ -f """$report_file""" && """$report_file""" == *json ]]; then
                if command -v jq &> /dev/null; then
                    critical=$(jq '.Results[] | select(.Vulnerabilities != null) | .Vulnerabilities[] | select(.Severity == "CRITICAL") | .VulnerabilityID' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    high=$(jq '.Results[] | select(.Vulnerabilities != null) | .Vulnerabilities[] | select(.Severity == "HIGH") | .VulnerabilityID' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    medium=$(jq '.Results[] | select(.Vulnerabilities != null) | .Vulnerabilities[] | select(.Severity == "MEDIUM") | .VulnerabilityID' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    low=$(jq '.Results[] | select(.Vulnerabilities != null) | .Vulnerabilities[] | select(.Severity == "LOW") | .VulnerabilityID' """$report_file""" 2>/dev/null | wc -l || echo 0)
                fi
            fi
            ;;
        
        semgrep)
            if [[ -f """$report_file""" && """$report_file""" == *json ]]; then
                if command -v jq &> /dev/null; then
                    critical=$(jq '.results[] | select(.extra.severity == "ERROR") | .check_id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    high=$(jq '.results[] | select(.extra.severity == "WARNING") | .check_id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    medium=$(jq '.results[] | select(.extra.severity == "INFO") | .check_id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    low=0
                fi
            fi
            ;;
        
        checkov)
            if [[ -f """$report_file""" && """$report_file""" == *json ]]; then
                if command -v jq &> /dev/null; then
                    critical=$(jq '.results.failed_checks[] | select(.severity == "CRITICAL") | .check_id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    high=$(jq '.results.failed_checks[] | select(.severity == "HIGH") | .check_id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    medium=$(jq '.results.failed_checks[] | select(.severity == "MEDIUM") | .check_id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    low=$(jq '.results.failed_checks[] | select(.severity == "LOW") | .check_id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                fi
            fi
            ;;
        
        zap)
            if [[ -f """$report_file""" && """$report_file""" == *json ]]; then
                if command -v jq &> /dev/null; then
                    critical=$(jq '.site[0].alerts[] | select(.riskcode >= 3) | .instances | length' """$report_file""" 2>/dev/null | awk '{sum+=$1} END {print sum}' || echo 0)
                    high=$(jq '.site[0].alerts[] | select(.riskcode == 2) | .instances | length' """$report_file""" 2>/dev/null | awk '{sum+=$1} END {print sum}' || echo 0)
                    medium=$(jq '.site[0].alerts[] | select(.riskcode == 1) | .instances | length' """$report_file""" 2>/dev/null | awk '{sum+=$1} END {print sum}' || echo 0)
                    low=$(jq '.site[0].alerts[] | select(.riskcode == 0) | .instances | length' """$report_file""" 2>/dev/null | awk '{sum+=$1} END {print sum}' || echo 0)
                fi
            fi
            ;;
        
        gitleaks)
            if [[ -f """$report_file""" && """$report_file""" == *json ]]; then
                if command -v jq &> /dev/null; then
                    critical=$(jq '.matches[] | select(.rule.severity == "CRITICAL") | .rule.id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    high=$(jq '.matches[] | select(.rule.severity == "HIGH") | .rule.id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    medium=$(jq '.matches[] | select(.rule.severity == "MEDIUM") | .rule.id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    low=$(jq '.matches[] | select(.rule.severity == "LOW") | .rule.id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                fi
            fi
            ;;
        
        *)
            # Generic JSON parsing for other report types
            if [[ -f """$report_file""" && """$report_file""" == *json ]]; then
                if command -v jq &> /dev/null; then
                    # Try common patterns for severity in JSON reports
                    critical=$(jq '.. | objects | select(.severity == "CRITICAL" or .severity == "critical") | .id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    high=$(jq '.. | objects | select(.severity == "HIGH" or .severity == "high") | .id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    medium=$(jq '.. | objects | select(.severity == "MEDIUM" or .severity == "medium") | .id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                    low=$(jq '.. | objects | select(.severity == "LOW" or .severity == "low") | .id' """$report_file""" 2>/dev/null | wc -l || echo 0)
                fi
            fi
            ;;
    esac
    
    echo """$critical"",""$high"",""$medium"",""$low"""
}

# Function to generate markdown report
generate_markdown_report() {
    log_info "Generating markdown report..."
    
    # Initialize report
    cat > """$OUTPUT_FILE""" << EOF
# $REPORT_TITLE

- **Date:** $(date +"%Y-%m-%d %H:%M:%S")
- **Repository:** $(git config --get remote.origin.url 2>/dev/null || echo "Unknown")
- **Branch:** $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "Unknown")
- **Commit:** $(git rev-parse --short HEAD 2>/dev/null || echo "Unknown")

## Executive Summary

This report provides a comprehensive overview of security scans performed on the codebase, including:

- Dependency vulnerability scanning
- Static Application Security Testing (SAST)
- Container security scanning
- Infrastructure as Code (IaC) security scanning
- Secrets detection
- Dynamic Application Security Testing (DAST)

## Security Scan Results

| Scan Type | Critical | High | Medium | Low | Total |
|-----------|----------|------|--------|-----|-------|
EOF
    
    # Initialize counters
    total_critical=0
    total_high=0
    total_medium=0
    total_low=0
    
    # Process dependency check reports
    if [[ -d """$REPORT_DIR""" ]]; then
        dependency_check_files=$(find """$REPORT_DIR""" -name "dependency-check-report.json" 2>/dev/null || echo "")
        if [[ -n """$dependency_check_files""" ]]; then
            dependency_critical=0
            dependency_high=0
            dependency_medium=0
            dependency_low=0
            
            for file in ""$dependency_check_files""; do
                counts=$(count_issues_by_severity """$file""" "dependency-check")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                dependency_critical=$((dependency_critical + critical))
                dependency_high=$((dependency_high + high))
                dependency_medium=$((dependency_medium + medium))
                dependency_low=$((dependency_low + low))
            done
            
            dependency_total=$((dependency_critical + dependency_high + dependency_medium + dependency_low))
            echo "| Dependency Vulnerabilities | ""$dependency_critical"" | ""$dependency_high"" | ""$dependency_medium"" | ""$dependency_low"" | ""$dependency_total"" |" >> """$OUTPUT_FILE"""
            
            total_critical=$((total_critical + dependency_critical))
            total_high=$((total_high + dependency_high))
            total_medium=$((total_medium + dependency_medium))
            total_low=$((total_low + dependency_low))
        fi
        
        # Process container scan reports
        container_scan_files=$(find """$REPORT_DIR""" -path "*/container-scan/*-scan.json" 2>/dev/null || echo "")
        if [[ -n """$container_scan_files""" ]]; then
            container_critical=0
            container_high=0
            container_medium=0
            container_low=0
            
            for file in ""$container_scan_files""; do
                counts=$(count_issues_by_severity """$file""" "trivy")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                container_critical=$((container_critical + critical))
                container_high=$((container_high + high))
                container_medium=$((container_medium + medium))
                container_low=$((container_low + low))
            done
            
            container_total=$((container_critical + container_high + container_medium + container_low))
            echo "| Container Vulnerabilities | ""$container_critical"" | ""$container_high"" | ""$container_medium"" | ""$container_low"" | ""$container_total"" |" >> """$OUTPUT_FILE"""
            
            total_critical=$((total_critical + container_critical))
            total_high=$((total_high + container_high))
            total_medium=$((total_medium + container_medium))
            total_low=$((total_low + container_low))
        fi
        
        # Process SAST reports
        sast_files=$(find """$REPORT_DIR""" -name "semgrep-results.json" 2>/dev/null || echo "")
        if [[ -n """$sast_files""" ]]; then
            sast_critical=0
            sast_high=0
            sast_medium=0
            sast_low=0
            
            for file in ""$sast_files""; do
                counts=$(count_issues_by_severity """$file""" "semgrep")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                sast_critical=$((sast_critical + critical))
                sast_high=$((sast_high + high))
                sast_medium=$((sast_medium + medium))
                sast_low=$((sast_low + low))
            done
            
            sast_total=$((sast_critical + sast_high + sast_medium + sast_low))
            echo "| SAST Issues | ""$sast_critical"" | ""$sast_high"" | ""$sast_medium"" | ""$sast_low"" | ""$sast_total"" |" >> """$OUTPUT_FILE"""
            
            total_critical=$((total_critical + sast_critical))
            total_high=$((total_high + sast_high))
            total_medium=$((total_medium + sast_medium))
            total_low=$((total_low + sast_low))
        fi
        
        # Process IaC reports
        iac_files=$(find """$REPORT_DIR""" -path "*/iac/checkov-*-results.json" 2>/dev/null || echo "")
        if [[ -n """$iac_files""" ]]; then
            iac_critical=0
            iac_high=0
            iac_medium=0
            iac_low=0
            
            for file in ""$iac_files""; do
                counts=$(count_issues_by_severity """$file""" "checkov")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                iac_critical=$((iac_critical + critical))
                iac_high=$((iac_high + high))
                iac_medium=$((iac_medium + medium))
                iac_low=$((iac_low + low))
            done
            
            iac_total=$((iac_critical + iac_high + iac_medium + iac_low))
            echo "| IaC Issues | ""$iac_critical"" | ""$iac_high"" | ""$iac_medium"" | ""$iac_low"" | ""$iac_total"" |" >> """$OUTPUT_FILE"""
            
            total_critical=$((total_critical + iac_critical))
            total_high=$((total_high + iac_high))
            total_medium=$((total_medium + iac_medium))
            total_low=$((total_low + iac_low))
        fi
        
        # Process secrets detection reports
        secrets_files=$(find """$REPORT_DIR""" -name "gitleaks-results.json" 2>/dev/null || echo "")
        if [[ -n """$secrets_files""" ]]; then
            secrets_critical=0
            secrets_high=0
            secrets_medium=0
            secrets_low=0
            
            for file in ""$secrets_files""; do
                counts=$(count_issues_by_severity """$file""" "gitleaks")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                secrets_critical=$((secrets_critical + critical))
                secrets_high=$((secrets_high + high))
                secrets_medium=$((secrets_medium + medium))
                secrets_low=$((secrets_low + low))
            done
            
            secrets_total=$((secrets_critical + secrets_high + secrets_medium + secrets_low))
            echo "| Secrets Detected | ""$secrets_critical"" | ""$secrets_high"" | ""$secrets_medium"" | ""$secrets_low"" | ""$secrets_total"" |" >> """$OUTPUT_FILE"""
            
            total_critical=$((total_critical + secrets_critical))
            total_high=$((total_high + secrets_high))
            total_medium=$((total_medium + secrets_medium))
            total_low=$((total_low + secrets_low))
        fi
        
        # Process DAST reports
        dast_files=$(find """$REPORT_DIR""" -name "zap-*-scan-report.json" 2>/dev/null || echo "")
        if [[ -n """$dast_files""" ]]; then
            dast_critical=0
            dast_high=0
            dast_medium=0
            dast_low=0
            
            for file in ""$dast_files""; do
                counts=$(count_issues_by_severity """$file""" "zap")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                dast_critical=$((dast_critical + critical))
                dast_high=$((dast_high + high))
                dast_medium=$((dast_medium + medium))
                dast_low=$((dast_low + low))
            done
            
            dast_total=$((dast_critical + dast_high + dast_medium + dast_low))
            echo "| DAST Issues | ""$dast_critical"" | ""$dast_high"" | ""$dast_medium"" | ""$dast_low"" | ""$dast_total"" |" >> """$OUTPUT_FILE"""
            
            total_critical=$((total_critical + dast_critical))
            total_high=$((total_high + dast_high))
            total_medium=$((total_medium + dast_medium))
            total_low=$((total_low + dast_low))
        fi
    fi
    
    # Add total row
    total_all=$((total_critical + total_high + total_medium + total_low))
    echo "| **TOTAL** | **""$total_critical""** | **""$total_high""** | **""$total_medium""** | **""$total_low""** | **""$total_all""** |" >> """$OUTPUT_FILE"""
    
    # Add risk assessment
    cat >> """$OUTPUT_FILE""" << EOF

## Risk Assessment

Based on the security scan results, the overall security risk is assessed as:

EOF
    
    if [[ ""$total_critical"" -gt 0 ]]; then
        echo "**CRITICAL** - Critical vulnerabilities require immediate attention and remediation." >> """$OUTPUT_FILE"""
    elif [[ ""$total_high"" -gt 0 ]]; then
        echo "**HIGH** - High severity issues should be addressed promptly." >> """$OUTPUT_FILE"""
    elif [[ ""$total_medium"" -gt 0 ]]; then
        echo "**MEDIUM** - Medium severity issues should be addressed in upcoming sprints." >> """$OUTPUT_FILE"""
    elif [[ ""$total_low"" -gt 0 ]]; then
        echo "**LOW** - Low severity issues should be reviewed and addressed as time permits." >> """$OUTPUT_FILE"""
    else

        echo "**MINIMAL** - No significant security issues detected." >> """$OUTPUT_FILE"""
    fi
    
    # Add OWASP Top 10 coverage
    cat >> """$OUTPUT_FILE""" << EOF

## OWASP Top 10 Coverage

| OWASP Category | Status | Details |
|----------------|--------|---------|
| A01:2021 Broken Access Control | $([ ""$total_critical"" -eq 0 ] && echo "✅ No issues" || echo "⚠️ Issues detected") | Access control vulnerabilities |
| A02:2021 Cryptographic Failures | $([ "$(find" """$REPORT_DIR""" -type f -exec grep -l "crypto\|cipher\|tls\|ssl" {} \; 2>/dev/null | wc -l) -eq 0 ] && echo "✅ No issues" || echo "⚠️ Issues detected") | Cryptographic implementation issues |
| A03:2021 Injection | $([ "$(find" """$REPORT_DIR""" -type f -exec grep -l "injection\|sql\|xss\|command" {} \; 2>/dev/null | wc -l) -eq 0 ] && echo "✅ No issues" || echo "⚠️ Issues detected") | SQL, NoSQL, command injection |
| A04:2021 Insecure Design | ⚠️ Manual review required | Requires architecture review |
| A05:2021 Security Misconfiguration | $([ ""$iac_critical"" -eq 0 ] && [ ""$iac_high"" -eq 0 ] && echo "✅ No issues" || echo "⚠️ Issues detected") | Configuration vulnerabilities |
| A06:2021 Vulnerable Components | $([ ""$dependency_critical"" -eq 0 ] && [ ""$dependency_high"" -eq 0 ] && echo "✅ No issues" || echo "⚠️ Issues detected") | Dependency vulnerabilities |
| A07:2021 Auth Failures | $([ "$(find" """$REPORT_DIR""" -type f -exec grep -l "auth\|login\|password" {} \; 2>/dev/null | wc -l) -eq 0 ] && echo "✅ No issues" || echo "⚠️ Issues detected") | Authentication vulnerabilities |
| A08:2021 Software and Data Integrity | $([ ""$secrets_critical"" -eq 0 ] && echo "✅ No issues" || echo "⚠️ Issues detected") | Supply chain, secrets management |
| A09:2021 Security Logging | ⚠️ Manual review required | Requires logging review |
| A10:2021 SSRF | $([ "$(find" """$REPORT_DIR""" -type f -exec grep -l "ssrf\|request forgery" {} \; 2>/dev/null | wc -l) -eq 0 ] && echo "✅ No issues" || echo "⚠️ Issues detected") | Server-side request forgery |
EOF
    
    # Add detailed findings if requested
    if [[ """$INCLUDE_DETAILS""" == true ]]; then
        cat >> """$OUTPUT_FILE""" << EOF

## Detailed Findings

### Critical Vulnerabilities

EOF
        
        # Find critical vulnerabilities
        critical_findings=$(find """$REPORT_DIR""" -type f -name "*.json" -exec grep -l "CRITICAL\|critical\|ERROR\|error" {} \; 2>/dev/null || echo "")
        if [[ -n """$critical_findings""" ]]; then
            for file in ""$critical_findings""; do
                if command -v jq &> /dev/null; then
                    # Extract critical findings based on file type
                    if [[ """$file""" == *dependency-check* ]]; then
                        jq '.dependencies[] | select(.vulnerabilities != null) | .vulnerabilities[] | select(.cvssv3 != null and .cvssv3.baseScore >= 9.0) | "- **" + .name + "**: " + .description + " (CVSS: " + (.cvssv3.baseScore | tostring) + ")"' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    elif [[ """$file""" == *trivy* ]]; then
                        jq '.Results[] | select(.Vulnerabilities != null) | .Vulnerabilities[] | select(.Severity == "CRITICAL") | "- **" + .VulnerabilityID + "**: " + .Title + " in " + .PkgName + " " + .InstalledVersion + " (Fixed in: " + (.FixedVersion // "N/A") + ")"' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    elif [[ """$file""" == *semgrep* ]]; then
                        jq '.results[] | select(.extra.severity == "ERROR") | "- **" + .check_id + "**: " + .extra.message + " in " + .path + ":" + (.start.line | tostring)' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    elif [[ """$file""" == *checkov* ]]; then
                        jq '.results.failed_checks[] | select(.severity == "CRITICAL") | "- **" + .check_id + "**: " + .check_name + " in " + .file_path + ":" + (.file_line_range[0] | tostring)' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    elif [[ """$file""" == *gitleaks* ]]; then
                        jq '.matches[] | select(.rule.severity == "CRITICAL") | "- **" + .rule.id + "**: " + .rule.description + " in " + .file + ":" + (.startLine | tostring)' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    elif [[ """$file""" == *zap* ]]; then
                        jq '.site[0].alerts[] | select(.riskcode >= 3) | "- **" + .name + "**: " + .desc + " (" + (.instances | length | tostring) + " instances)"' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    fi
                fi
            done
        else
            echo "No critical vulnerabilities found." >> """$OUTPUT_FILE"""
        fi
        
        cat >> """$OUTPUT_FILE""" << EOF

### High Severity Issues

EOF
        
        # Find high severity issues
        high_findings=$(find """$REPORT_DIR""" -type f -name "*.json" -exec grep -l "HIGH\|high\|WARNING\|warning" {} \; 2>/dev/null || echo "")
        if [[ -n """$high_findings""" ]]; then
            for file in ""$high_findings""; do
                if command -v jq &> /dev/null; then
                    # Extract high severity findings based on file type
                    if [[ """$file""" == *dependency-check* ]]; then
                        jq '.dependencies[] | select(.vulnerabilities != null) | .vulnerabilities[] | select(.cvssv3 != null and .cvssv3.baseScore >= 7.0 and .cvssv3.baseScore < 9.0) | "- **" + .name + "**: " + .description + " (CVSS: " + (.cvssv3.baseScore | tostring) + ")"' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    elif [[ """$file""" == *trivy* ]]; then
                        jq '.Results[] | select(.Vulnerabilities != null) | .Vulnerabilities[] | select(.Severity == "HIGH") | "- **" + .VulnerabilityID + "**: " + .Title + " in " + .PkgName + " " + .InstalledVersion + " (Fixed in: " + (.FixedVersion // "N/A") + ")"' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    elif [[ """$file""" == *semgrep* ]]; then
                        jq '.results[] | select(.extra.severity == "WARNING") | "- **" + .check_id + "**: " + .extra.message + " in " + .path + ":" + (.start.line | tostring)' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    elif [[ """$file""" == *checkov* ]]; then
                        jq '.results.failed_checks[] | select(.severity == "HIGH") | "- **" + .check_id + "**: " + .check_name + " in " + .file_path + ":" + (.file_line_range[0] | tostring)' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    elif [[ """$file""" == *gitleaks* ]]; then
                        jq '.matches[] | select(.rule.severity == "HIGH") | "- **" + .rule.id + "**: " + .rule.description + " in " + .file + ":" + (.startLine | tostring)' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    elif [[ """$file""" == *zap* ]]; then
                        jq '.site[0].alerts[] | select(.riskcode == 2) | "- **" + .name + "**: " + .desc + " (" + (.instances | length | tostring) + " instances)"' """$file""" 2>/dev/null | sed 's/"//g' >> """$OUTPUT_FILE""" || true
                    fi
                fi
            done
        else
            echo "No high severity issues found." >> """$OUTPUT_FILE"""
        fi
    fi
    
    # Add recommendations
    cat >> """$OUTPUT_FILE""" << EOF

## Recommendations

Based on the security scan results, the following recommendations are provided:

1. **Address Critical Vulnerabilities**: $([ ""$total_critical"" -gt 0 ] && echo "Fix the ""$total_critical"" critical vulnerabilities immediately." || echo "No critical vulnerabilities to address.")
2. **Remediate High Severity Issues**: $([ ""$total_high"" -gt 0 ] && echo "Address the ""$total_high"" high severity issues in the next sprint." || echo "No high severity issues to address.")
3. **Update Dependencies**: $([ ""$dependency_critical"" -gt 0 ] || [ ""$dependency_high"" -gt 0 ] && echo "Update vulnerable dependencies to secure versions." || echo "Keep dependencies up to date.")
4. **Secure Container Images**: $([ ""$container_critical"" -gt 0 ] || [ ""$container_high"" -gt 0 ] && echo "Fix container vulnerabilities by updating base images or removing vulnerable packages." || echo "Continue using secure container images.")
5. **Improve Code Security**: $([ ""$sast_critical"" -gt 0 ] || [ ""$sast_high"" -gt 0 ] && echo "Fix identified code security issues." || echo "Maintain secure coding practices.")
6. **Enhance Infrastructure Security**: $([ ""$iac_critical"" -gt 0 ] || [ ""$iac_high"" -gt 0 ] && echo "Address infrastructure as code security issues." || echo "Continue following infrastructure security best practices.")
7. **Secure Secret Management**: $([ ""$secrets_critical"" -gt 0 ] || [ ""$secrets_high"" -gt 0 ] && echo "Remove exposed secrets and implement proper secret management." || echo "Continue using secure secret management practices.")
8. **API Security**: $([ ""$dast_critical"" -gt 0 ] || [ ""$dast_high"" -gt 0 ] && echo "Fix identified API security vulnerabilities." || echo "Maintain secure API implementation.")

## Next Steps

1. Review this report with the security team
2. Prioritize vulnerabilities based on severity and impact
3. Create tickets for remediation tasks
4. Implement fixes according to priority
5. Verify fixes with follow-up security scans
6. Schedule regular security scans as part of the CI/CD pipeline

## Report Details

- **Generated By**: $(whoami)
- **Generated On**: $(date +"%Y-%m-%d %H:%M:%S")
- **Report Version**: 1.0
EOF
    
    log_success "Markdown report generated: ""$OUTPUT_FILE"""
}

# Function to generate HTML report
generate_html_report() {
    log_info "Generating HTML report..."
    
    # Generate markdown first
    local markdown_file="${OUTPUT_FILE%.html}.md"
    OUTPUT_FILE="""$markdown_file""" generate_markdown_report
    
    # Convert markdown to HTML
    if command -v pandoc &> /dev/null; then
        pandoc -f markdown -t html """$markdown_file""" -o """$OUTPUT_FILE""" --standalone --metadata title="""$REPORT_TITLE""" \
            --css "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" \
            --template <(echo '
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>""$title""$</title>
  ""$for""(css)$
  <link rel="stylesheet" href="""$css""$">
  ""$endfor""$
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      line-height: 1.6;
      padding: 2rem;
      max-width: 1200px;
      margin: 0 auto;
    }
    h1 { color: #2c3e50; border-bottom: 2px solid #eee; padding-bottom: 0.5rem; }
    h2 { color: #3498db; margin-top: 2rem; }
    h3 { color: #2c3e50; }
    table { width: 100%; border-collapse: collapse; margin: 1rem 0; }
    th, td { padding: 0.75rem; border: 1px solid #dee2e6; }
    th { background-color: #f8f9fa; }
    tr:nth-child(even) { background-color: #f8f9fa; }
    .critical { color: #dc3545; font-weight: bold; }
    .high { color: #fd7e14; font-weight: bold; }
    .medium { color: #ffc107; }
    .low { color: #6c757d; }
    .success { color: #28a745; }
  </style>
</head>
<body>
  <div class="container">
    ""$body""$
  </div>
</body>
</html>
')
        
        # Add some JavaScript to make the HTML report interactive
        echo '<script>
document.addEventListener("DOMContentLoaded", function() {
  // Add classes to severity levels
  document.querySelectorAll("table tr").forEach(function(row) {
    const cells = row.querySelectorAll("td");
    if (cells.length >= 5) {
      const criticalCell = cells[1];
      const highCell = cells[2];
      
      if (criticalCell && criticalCell.textContent.trim() !== "0" && !isNaN(criticalCell.textContent.trim())) {
        criticalCell.classList.add("critical");
      }
      
      if (highCell && highCell.textContent.trim() !== "0" && !isNaN(highCell.textContent.trim())) {
        highCell.classList.add("high");
      }
    }
  });
});
</script>' >> """$OUTPUT_FILE"""
        
        log_success "HTML report generated: ""$OUTPUT_FILE"""
    else
        log_error "pandoc not installed. Cannot generate HTML report."
        log_info "Using markdown report instead: ""$markdown_file"""
        OUTPUT_FILE="""$markdown_file"""
    fi
}

# Function to generate JSON report
generate_json_report() {
    log_info "Generating JSON report..."
    
    # Initialize JSON structure
    cat > """$OUTPUT_FILE""" << EOF
{
  "report": {
    "title": """$REPORT_TITLE""",
    "date": "$(date +"%Y-%m-%d %H:%M:%S")",
    "repository": "$(git config --get remote.origin.url 2>/dev/null || echo "Unknown")",
    "branch": "$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "Unknown")",
    "commit": "$(git rev-parse --short HEAD 2>/dev/null || echo "Unknown")",
    "summary": {
      "scans": [
EOF
    
    # Initialize counters
    total_critical=0
    total_high=0
    total_medium=0
    total_low=0
    
    # Process dependency check reports
    if [[ -d """$REPORT_DIR""" ]]; then
        dependency_check_files=$(find """$REPORT_DIR""" -name "dependency-check-report.json" 2>/dev/null || echo "")
        if [[ -n """$dependency_check_files""" ]]; then
            dependency_critical=0
            dependency_high=0
            dependency_medium=0
            dependency_low=0
            
            for file in ""$dependency_check_files""; do
                counts=$(count_issues_by_severity """$file""" "dependency-check")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                dependency_critical=$((dependency_critical + critical))
                dependency_high=$((dependency_high + high))
                dependency_medium=$((dependency_medium + medium))
                dependency_low=$((dependency_low + low))
            done
            
            dependency_total=$((dependency_critical + dependency_high + dependency_medium + dependency_low))
            cat >> """$OUTPUT_FILE""" << EOF
        {
          "type": "Dependency Vulnerabilities",
          "critical": ""$dependency_critical"",
          "high": ""$dependency_high"",
          "medium": ""$dependency_medium"",
          "low": ""$dependency_low"",
          "total": $dependency_total
        },
EOF
            
            total_critical=$((total_critical + dependency_critical))
            total_high=$((total_high + dependency_high))
            total_medium=$((total_medium + dependency_medium))
            total_low=$((total_low + dependency_low))
        fi
        
        # Process container scan reports
        container_scan_files=$(find """$REPORT_DIR""" -path "*/container-scan/*-scan.json" 2>/dev/null || echo "")
        if [[ -n """$container_scan_files""" ]]; then
            container_critical=0
            container_high=0
            container_medium=0
            container_low=0
            
            for file in ""$container_scan_files""; do
                counts=$(count_issues_by_severity """$file""" "trivy")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                container_critical=$((container_critical + critical))
                container_high=$((container_high + high))
                container_medium=$((container_medium + medium))
                container_low=$((container_low + low))
            done
            
            container_total=$((container_critical + container_high + container_medium + container_low))
            cat >> """$OUTPUT_FILE""" << EOF
        {
          "type": "Container Vulnerabilities",
          "critical": ""$container_critical"",
          "high": ""$container_high"",
          "medium": ""$container_medium"",
          "low": ""$container_low"",
          "total": $container_total
        },
EOF
            
            total_critical=$((total_critical + container_critical))
            total_high=$((total_high + container_high))
            total_medium=$((total_medium + container_medium))
            total_low=$((total_low + container_low))
        fi
        
        # Process SAST reports
        sast_files=$(find """$REPORT_DIR""" -name "semgrep-results.json" 2>/dev/null || echo "")
        if [[ -n """$sast_files""" ]]; then
            sast_critical=0
            sast_high=0
            sast_medium=0
            sast_low=0
            
            for file in ""$sast_files""; do
                counts=$(count_issues_by_severity """$file""" "semgrep")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                sast_critical=$((sast_critical + critical))
                sast_high=$((sast_high + high))
                sast_medium=$((sast_medium + medium))
                sast_low=$((sast_low + low))
            done
            
            sast_total=$((sast_critical + sast_high + sast_medium + sast_low))
            cat >> """$OUTPUT_FILE""" << EOF
        {
          "type": "SAST Issues",
          "critical": ""$sast_critical"",
          "high": ""$sast_high"",
          "medium": ""$sast_medium"",
          "low": ""$sast_low"",
          "total": $sast_total
        },
EOF
            
            total_critical=$((total_critical + sast_critical))
            total_high=$((total_high + sast_high))
            total_medium=$((total_medium + sast_medium))
            total_low=$((total_low + sast_low))
        fi
        
        # Process IaC reports
        iac_files=$(find """$REPORT_DIR""" -path "*/iac/checkov-*-results.json" 2>/dev/null || echo "")
        if [[ -n """$iac_files""" ]]; then
            iac_critical=0
            iac_high=0
            iac_medium=0
            iac_low=0
            
            for file in ""$iac_files""; do
                counts=$(count_issues_by_severity """$file""" "checkov")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                iac_critical=$((iac_critical + critical))
                iac_high=$((iac_high + high))
                iac_medium=$((iac_medium + medium))
                iac_low=$((iac_low + low))
            done
            
            iac_total=$((iac_critical + iac_high + iac_medium + iac_low))
            cat >> """$OUTPUT_FILE""" << EOF
        {
          "type": "IaC Issues",
          "critical": ""$iac_critical"",
          "high": ""$iac_high"",
          "medium": ""$iac_medium"",
          "low": ""$iac_low"",
          "total": $iac_total
        },
EOF
            
            total_critical=$((total_critical + iac_critical))
            total_high=$((total_high + iac_high))
            total_medium=$((total_medium + iac_medium))
            total_low=$((total_low + iac_low))
        fi
        
        # Process secrets detection reports
        secrets_files=$(find """$REPORT_DIR""" -name "gitleaks-results.json" 2>/dev/null || echo "")
        if [[ -n """$secrets_files""" ]]; then
            secrets_critical=0
            secrets_high=0
            secrets_medium=0
            secrets_low=0
            
            for file in ""$secrets_files""; do
                counts=$(count_issues_by_severity """$file""" "gitleaks")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                secrets_critical=$((secrets_critical + critical))
                secrets_high=$((secrets_high + high))
                secrets_medium=$((secrets_medium + medium))
                secrets_low=$((secrets_low + low))
            done
            
            secrets_total=$((secrets_critical + secrets_high + secrets_medium + secrets_low))
            cat >> """$OUTPUT_FILE""" << EOF
        {
          "type": "Secrets Detected",
          "critical": ""$secrets_critical"",
          "high": ""$secrets_high"",
          "medium": ""$secrets_medium"",
          "low": ""$secrets_low"",
          "total": $secrets_total
        },
EOF
            
            total_critical=$((total_critical + secrets_critical))
            total_high=$((total_high + secrets_high))
            total_medium=$((total_medium + secrets_medium))
            total_low=$((total_low + secrets_low))
        fi
        
        # Process DAST reports
        dast_files=$(find """$REPORT_DIR""" -name "zap-*-scan-report.json" 2>/dev/null || echo "")
        if [[ -n """$dast_files""" ]]; then
            dast_critical=0
            dast_high=0
            dast_medium=0
            dast_low=0
            
            for file in ""$dast_files""; do
                counts=$(count_issues_by_severity """$file""" "zap")
                IFS=',' read -r critical high medium low <<< """$counts"""
                
                dast_critical=$((dast_critical + critical))
                dast_high=$((dast_high + high))
                dast_medium=$((dast_medium + medium))
                dast_low=$((dast_low + low))
            done
            
            dast_total=$((dast_critical + dast_high + dast_medium + dast_low))
            cat >> """$OUTPUT_FILE""" << EOF
        {
          "type": "DAST Issues",
          "critical": ""$dast_critical"",
          "high": ""$dast_high"",
          "medium": ""$dast_medium"",
          "low": ""$dast_low"",
          "total": $dast_total
        },
EOF
            
            total_critical=$((total_critical + dast_critical))
            total_high=$((total_high + dast_high))
            total_medium=$((total_medium + dast_medium))
            total_low=$((total_low + dast_low))
        fi
    fi
    
    # Add total row and close JSON structure
    total_all=$((total_critical + total_high + total_medium + total_low))
    
    # Remove trailing comma from last scan entry
    sed -i '$ s/,$//' """$OUTPUT_FILE"""
    
    cat >> """$OUTPUT_FILE""" << EOF
      ],
      "totals": {
        "critical": ""$total_critical"",
        "high": ""$total_high"",
        "medium": ""$total_medium"",
        "low": ""$total_low"",
        "total": $total_all
      },
      "risk_assessment": "$(
        if [[ ""$total_critical"" -gt 0 ]]; then
          echo "CRITICAL"
        elif [[ ""$total_high"" -gt 0 ]]; then
          echo "HIGH"
        elif [[ ""$total_medium"" -gt 0 ]]; then
          echo "MEDIUM"
        elif [[ ""$total_low"" -gt 0 ]]; then
          echo "LOW"
        else
          echo "MINIMAL"
        fi
      )"
    }
  }
}
EOF
    
    # Pretty-print JSON if jq is available
    if command -v jq &> /dev/null; then
        jq . """$OUTPUT_FILE""" > "${OUTPUT_FILE}.tmp" && mv "${OUTPUT_FILE}.tmp" """$OUTPUT_FILE"""
    fi
    
    log_success "JSON report generated: ""$OUTPUT_FILE"""
}

# Generate report based on format
case ""$OUTPUT_FORMAT"" in
    markdown)
        generate_markdown_report
        ;;
    html)
        generate_html_report
        ;;
    json)
        generate_json_report
        ;;
    *)
        log_error "Unknown output format: ""$OUTPUT_FORMAT"""
        show_help
        exit 1
        ;;
esac

log_success "Security report generation completed"