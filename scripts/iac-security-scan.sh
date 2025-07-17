#!/bin/bash

# Infrastructure as Code Security Scanning Script
# This script scans IaC files (Dockerfiles, Kubernetes, Terraform, etc.) for security issues

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
SCAN_TYPE="all"
REPORT_DIR="./security-reports/iac"
SEVERITY="HIGH,CRITICAL"
FAIL_ON_SEVERITY="CRITICAL"

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
    echo "Infrastructure as Code Security Scanning Script"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -t, --type <type>            Scan type: all, docker, kubernetes, terraform, github (default: all)"
    echo "  -o, --output <dir>           Output directory (default: ./security-reports/iac)"
    echo "  -s, --severity <severity>    Severity levels to scan for (default: HIGH,CRITICAL)"
    echo "  -f, --fail <severity>        Fail on severity level (default: CRITICAL)"
    echo "  -h, --help                   Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --type docker"
    echo "  $0 --type kubernetes --severity MEDIUM,HIGH,CRITICAL"
    echo "  $0 --type all --fail HIGH"
}

# Check if required tools are installed
check_tools() {
    local missing_tools=()
    
    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    fi
    
    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        log_error "The following required tools are missing: ${missing_tools[*]}"
        log_error "Please install them before running this script."
        exit 1
    fi
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            SCAN_TYPE="$2"
            shift 2
            ;;
        -o|--output)
            REPORT_DIR="$2"
            shift 2
            ;;
        -s|--severity)
            SEVERITY="$2"
            shift 2
            ;;
        -f|--fail)
            FAIL_ON_SEVERITY="$2"
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

# Create output directory
mkdir -p "$REPORT_DIR"

# Check if required tools are installed
check_tools

# Generate timestamp for reports
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Run Checkov scan
run_checkov_scan() {
    local scan_type=$1
    local output_file="$REPORT_DIR/checkov-$scan_type-$TIMESTAMP"
    
    log_info "Running Checkov scan for $scan_type"
    
    # Determine framework based on scan type
    local framework=""
    case $scan_type in
        docker)
            framework="dockerfile"
            ;;
        kubernetes)
            framework="kubernetes"
            ;;
        terraform)
            framework="terraform"
            ;;
        github)
            framework="github_actions"
            ;;
        all)
            framework="dockerfile,kubernetes,terraform,github_actions,secrets"
            ;;
        *)
            log_error "Unknown scan type for Checkov: $scan_type"
            return 1
            ;;
    esac
    
    # Run Checkov using Docker
    docker run --rm -v "$(pwd):/data" bridgecrew/checkov \
        --directory /data \
        --framework "$framework" \
        --output json --output-file-path /data/"$output_file".json \
        --quiet
    
    # Also generate CLI output for readability
    docker run --rm -v "$(pwd):/data" bridgecrew/checkov \
        --directory /data \
        --framework "$framework" \
        --output cli --output-file-path /data/"$output_file".txt \
        --quiet
    
    # Generate SARIF output for GitHub integration
    docker run --rm -v "$(pwd):/data" bridgecrew/checkov \
        --directory /data \
        --framework "$framework" \
        --output sarif --output-file-path /data/"$output_file".sarif \
        --quiet
    
    log_success "Checkov scan for $scan_type completed"
    
    # Parse results
    if [[ -f "$output_file.json" ]]; then
        if command -v jq &> /dev/null; then
            local failed_checks=$(jq '.results.failed_checks | length' "$output_file.json")
            local passed_checks=$(jq '.results.passed_checks | length' "$output_file.json")
            
            log_info "Scan results for $scan_type:"
            log_info "- Failed checks: $failed_checks"
            log_info "- Passed checks: $passed_checks"
            
            # Check for high severity issues
            local high_severity=$(jq '.results.failed_checks[] | select(.severity == "HIGH" or .severity == "CRITICAL") | .id' "$output_file.json" | wc -l)
            
            if [[ "$high_severity" -gt 0 ]]; then
                log_warning "Found $high_severity HIGH/CRITICAL severity issues"
                
                # Fail if severity threshold is met
                if [[ "$FAIL_ON_SEVERITY" == "HIGH" || "$FAIL_ON_SEVERITY" == "CRITICAL" ]]; then
                    return 1
                fi
            fi
        else
            log_warning "jq not installed. Cannot parse JSON results."
        fi
    else
        log_warning "No JSON report found for $scan_type. Cannot analyze results."
    fi
    
    return 0
}

# Run Hadolint for Dockerfiles
run_hadolint_scan() {
    local output_file="$REPORT_DIR/hadolint-$TIMESTAMP"
    
    log_info "Running Hadolint scan for Dockerfiles"
    
    # Find all Dockerfiles
    local dockerfiles=$(find . -name "Dockerfile" -o -name "*.Dockerfile")
    
    if [[ -z "$dockerfiles" ]]; then
        log_warning "No Dockerfiles found"
        return 0
    fi
    
    # Run Hadolint using Docker
    echo "$dockerfiles" | xargs docker run --rm -i -v "$(pwd):/data" hadolint/hadolint hadolint --format json > "$output_file.json"
    
    # Also generate text output
    echo "$dockerfiles" | xargs docker run --rm -i -v "$(pwd):/data" hadolint/hadolint hadolint > "$output_file.txt"
    
    log_success "Hadolint scan completed"
    
    # Parse results
    if [[ -f "$output_file.json" && -s "$output_file.json" ]]; then
        if command -v jq &> /dev/null; then
            local issues=$(jq '. | length' "$output_file.json")
            local error_issues=$(jq '.[] | select(.level == "error") | .level' "$output_file.json" | wc -l)
            
            log_info "Hadolint results:"
            log_info "- Total issues: $issues"
            log_info "- Error level issues: $error_issues"
            
            if [[ "$error_issues" -gt 0 && "$FAIL_ON_SEVERITY" == "HIGH" ]]; then
                return 1
            fi
        else
            log_warning "jq not installed. Cannot parse JSON results."
        fi
    else
        log_info "No Hadolint issues found"
    fi
    
    return 0
}

# Run TFSec for Terraform files
run_tfsec_scan() {
    local output_file="$REPORT_DIR/tfsec-$TIMESTAMP"
    
    log_info "Running TFSec scan for Terraform files"
    
    # Check if there are any Terraform files
    if ! find . -name "*.tf" | grep -q .; then
        log_warning "No Terraform files found"
        return 0
    fi
    
    # Run TFSec using Docker
    docker run --rm -v "$(pwd):/data" aquasec/tfsec:latest /data \
        --format json --out /data/"$output_file".json
    
    # Also generate text output
    docker run --rm -v "$(pwd):/data" aquasec/tfsec:latest /data \
        --format text --out /data/"$output_file".txt
    
    # Generate SARIF output
    docker run --rm -v "$(pwd):/data" aquasec/tfsec:latest /data \
        --format sarif --out /data/"$output_file".sarif
    
    log_success "TFSec scan completed"
    
    # Parse results
    if [[ -f "$output_file.json" ]]; then
        if command -v jq &> /dev/null; then
            local issues=$(jq '.results | length' "$output_file.json")
            local high_severity=$(jq '.results[] | select(.severity == "HIGH" or .severity == "CRITICAL") | .severity' "$output_file.json" | wc -l)
            
            log_info "TFSec results:"
            log_info "- Total issues: $issues"
            log_info "- High/Critical severity issues: $high_severity"
            
            if [[ "$high_severity" -gt 0 && "$FAIL_ON_SEVERITY" == "HIGH" ]]; then
                return 1
            fi
        else
            log_warning "jq not installed. Cannot parse JSON results."
        fi
    else
        log_info "No TFSec issues found"
    fi
    
    return 0
}

# Run Kubesec for Kubernetes files
run_kubesec_scan() {
    local output_file="$REPORT_DIR/kubesec-$TIMESTAMP"
    
    log_info "Running Kubesec scan for Kubernetes files"
    
    # Find all Kubernetes YAML files
    local k8s_files=$(find . -name "*.yaml" -o -name "*.yml" | xargs grep -l "kind:" | grep -v "node_modules")
    
    if [[ -z "$k8s_files" ]]; then
        log_warning "No Kubernetes files found"
        return 0
    fi
    
    # Create directory for individual scan results
    mkdir -p "$REPORT_DIR/kubesec"
    
    # Scan each file individually
    local failed=0
    for file in $k8s_files; do
        local base_name=$(basename "$file")
        local result_file="$REPORT_DIR/kubesec/$base_name.json"
        
        log_info "Scanning $file"
        
        # Run Kubesec using Docker
        if ! docker run --rm -v "$(pwd):/data" kubesec/kubesec:v2 scan /data/"$file" > "$result_file"; then
            log_warning "Failed to scan $file"
            failed=1
        fi
    done
    
    # Combine results
    if command -v jq &> /dev/null; then
        jq -s '.' "$REPORT_DIR/kubesec"/*.json > "$output_file.json"
    else
        cat "$REPORT_DIR/kubesec"/*.json > "$output_file.json"
    fi
    
    log_success "Kubesec scan completed"
    
    # Parse results
    if [[ -f "$output_file.json" ]]; then
        if command -v jq &> /dev/null; then
            local files_scanned=$(jq '. | length' "$output_file.json")
            local critical_issues=$(jq '.[].scoring.critical | select(. != null) | length' "$output_file.json" | awk '{sum+=$1} END {print sum}')
            
            log_info "Kubesec results:"
            log_info "- Files scanned: $files_scanned"
            log_info "- Critical issues: ${critical_issues:-0}"
            
            if [[ "${critical_issues:-0}" -gt 0 && "$FAIL_ON_SEVERITY" == "CRITICAL" ]]; then
                return 1
            fi
        else
            log_warning "jq not installed. Cannot parse JSON results."
        fi
    else
        log_info "No Kubesec issues found"
    fi
    
    return 0
}

# Generate summary report
generate_summary_report() {
    local output_file="$REPORT_DIR/iac-security-summary-$TIMESTAMP.md"
    
    log_info "Generating summary report"
    
    # Create summary report
    cat > "$output_file" << EOF
# Infrastructure as Code Security Scan Summary

- **Date:** $(date +"%Y-%m-%d %H:%M:%S")
- **Scan Type:** $SCAN_TYPE
- **Severity Levels:** $SEVERITY

## Scan Results

| Tool | Status | Details |
|------|--------|---------|
EOF
    
    # Add results for each tool
    if [[ "$SCAN_TYPE" == "all" || "$SCAN_TYPE" == "docker" ]]; then
        if [[ -f "$REPORT_DIR/checkov-docker-$TIMESTAMP.json" ]]; then
            local failed_checks=$(jq '.results.failed_checks | length' "$REPORT_DIR/checkov-docker-$TIMESTAMP.json" 2>/dev/null || echo "N/A")
            echo "| Checkov (Docker) | $([ "$failed_checks" == "0" ] && echo "✅ Pass" || echo "⚠️ Issues Found") | $failed_checks issues found |" >> "$output_file"
        fi
        
        if [[ -f "$REPORT_DIR/hadolint-$TIMESTAMP.json" ]]; then
            local issues=$(jq '. | length' "$REPORT_DIR/hadolint-$TIMESTAMP.json" 2>/dev/null || echo "N/A")
            echo "| Hadolint | $([ "$issues" == "0" ] && echo "✅ Pass" || echo "⚠️ Issues Found") | $issues issues found |" >> "$output_file"
        fi
    fi
    
    if [[ "$SCAN_TYPE" == "all" || "$SCAN_TYPE" == "kubernetes" ]]; then
        if [[ -f "$REPORT_DIR/checkov-kubernetes-$TIMESTAMP.json" ]]; then
            local failed_checks=$(jq '.results.failed_checks | length' "$REPORT_DIR/checkov-kubernetes-$TIMESTAMP.json" 2>/dev/null || echo "N/A")
            echo "| Checkov (Kubernetes) | $([ "$failed_checks" == "0" ] && echo "✅ Pass" || echo "⚠️ Issues Found") | $failed_checks issues found |" >> "$output_file"
        fi
        
        if [[ -f "$REPORT_DIR/kubesec-$TIMESTAMP.json" ]]; then
            local files_scanned=$(jq '. | length' "$REPORT_DIR/kubesec-$TIMESTAMP.json" 2>/dev/null || echo "N/A")
            echo "| Kubesec | ℹ️ Info | $files_scanned files scanned |" >> "$output_file"
        fi
    fi
    
    if [[ "$SCAN_TYPE" == "all" || "$SCAN_TYPE" == "terraform" ]]; then
        if [[ -f "$REPORT_DIR/checkov-terraform-$TIMESTAMP.json" ]]; then
            local failed_checks=$(jq '.results.failed_checks | length' "$REPORT_DIR/checkov-terraform-$TIMESTAMP.json" 2>/dev/null || echo "N/A")
            echo "| Checkov (Terraform) | $([ "$failed_checks" == "0" ] && echo "✅ Pass" || echo "⚠️ Issues Found") | $failed_checks issues found |" >> "$output_file"
        fi
        
        if [[ -f "$REPORT_DIR/tfsec-$TIMESTAMP.json" ]]; then
            local issues=$(jq '.results | length' "$REPORT_DIR/tfsec-$TIMESTAMP.json" 2>/dev/null || echo "N/A")
            echo "| TFSec | $([ "$issues" == "0" ] && echo "✅ Pass" || echo "⚠️ Issues Found") | $issues issues found |" >> "$output_file"
        fi
    fi
    
    if [[ "$SCAN_TYPE" == "all" || "$SCAN_TYPE" == "github" ]]; then
        if [[ -f "$REPORT_DIR/checkov-github-$TIMESTAMP.json" ]]; then
            local failed_checks=$(jq '.results.failed_checks | length' "$REPORT_DIR/checkov-github-$TIMESTAMP.json" 2>/dev/null || echo "N/A")
            echo "| Checkov (GitHub Actions) | $([ "$failed_checks" == "0" ] && echo "✅ Pass" || echo "⚠️ Issues Found") | $failed_checks issues found |" >> "$output_file"
        fi
    fi
    
    cat >> "$output_file" << EOF

## Recommendations

1. Review all high and critical severity issues
2. Address security misconfigurations in IaC files
3. Follow security best practices for containerization
4. Implement least privilege principle in all infrastructure code
5. Run regular security scans as part of CI/CD pipeline

## Detailed Reports

EOF
    
    # Add links to detailed reports
    find "$REPORT_DIR" -name "*$TIMESTAMP*" -not -name "iac-security-summary-*" | while read -r file; do
        local file_name=$(basename "$file")
        echo "- [${file_name%.*} (${file##*.})](./${file_name})" >> "$output_file"
    done
    
    log_success "Summary report generated: $output_file"
}

# Main execution
log_info "Starting Infrastructure as Code security scanning"
log_info "Scan type: $SCAN_TYPE"
log_info "Output directory: $REPORT_DIR"
log_info "Severity levels: $SEVERITY"

# Track failures
FAILED=0

# Run scans based on scan type
case $SCAN_TYPE in
    all)
        run_checkov_scan "all" || FAILED=1
        run_hadolint_scan || FAILED=1
        run_tfsec_scan || FAILED=1
        run_kubesec_scan || FAILED=1
        ;;
    docker)
        run_checkov_scan "docker" || FAILED=1
        run_hadolint_scan || FAILED=1
        ;;
    kubernetes)
        run_checkov_scan "kubernetes" || FAILED=1
        run_kubesec_scan || FAILED=1
        ;;
    terraform)
        run_checkov_scan "terraform" || FAILED=1
        run_tfsec_scan || FAILED=1
        ;;
    github)
        run_checkov_scan "github" || FAILED=1
        ;;
    *)
        log_error "Unknown scan type: $SCAN_TYPE"
        show_help
        exit 1
        ;;
esac

# Generate summary report
generate_summary_report

if [[ $FAILED -eq 0 ]]; then
    log_success "Infrastructure as Code security scanning completed successfully"
    exit 0
else
    log_error "Infrastructure as Code security scanning found issues"
    exit 1
fi