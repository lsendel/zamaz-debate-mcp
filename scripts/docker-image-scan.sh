#!/bin/bash

# Docker Image Vulnerability Scanning Script
# This script scans Docker images for vulnerabilities using Trivy

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
REGISTRY="${REGISTRY:-ghcr.io}"
REGISTRY_BASE="${REGISTRY_BASE:-$(git config --get remote.origin.url | sed 's/.*github.com[:/]\([^/]*\).*/\1/')}"
VERSION="${VERSION:-latest}"
SERVICES=()
SEVERITY="CRITICAL,HIGH"
OUTPUT_FORMAT="table"
OUTPUT_DIR="./security-reports"
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
    echo "Docker Image Vulnerability Scanning Script"
    echo ""
    echo "Usage: $0 [options] [service1 service2 ...]"
    echo ""
    echo "Options:"
    echo "  -r, --registry <registry>     Docker registry (default: ghcr.io)"
    echo "  -b, --base <base>            Registry base path (default: github username)"
    echo "  -v, --version <version>      Image version (default: latest)"
    echo "  -s, --severity <severity>    Severity levels to scan for (default: CRITICAL,HIGH)"
    echo "  -f, --format <format>        Output format: table, json, sarif (default: table)"
    echo "  -o, --output <dir>           Output directory (default: ./security-reports)"
    echo "  -x, --fail <severity>        Fail on severity level (default: CRITICAL)"
    echo "  -a, --all                    Scan all services"
    echo "  -h, --help                   Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 mcp-organization mcp-llm"
    echo "  $0 --version 1.0.0 --format json mcp-organization"
    echo "  $0 --all --severity CRITICAL,HIGH,MEDIUM"
}

# Check if Trivy is installed
check_trivy() {
    if ! command -v trivy &> /dev/null; then
        log_warning "Trivy not installed. Attempting to install..."
        
        # Check OS and install Trivy
        if [[ "$OSTYPE" == "linux-gnu"* ]]; then
            # Linux
            if command -v apt-get &> /dev/null; then
                # Debian/Ubuntu
                log_info "Installing Trivy on Debian/Ubuntu..."
                sudo apt-get update
                sudo apt-get install -y wget apt-transport-https gnupg lsb-release
                wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
                echo deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main | sudo tee -a /etc/apt/sources.list.d/trivy.list
                sudo apt-get update
                sudo apt-get install -y trivy
            elif command -v yum &> /dev/null; then
                # RHEL/CentOS
                log_info "Installing Trivy on RHEL/CentOS..."
                sudo rpm -ivh https://github.com/aquasecurity/trivy/releases/download/v0.48.0/trivy_0.48.0_Linux-64bit.rpm
            else
                log_error "Unsupported Linux distribution. Please install Trivy manually."
                exit 1
            fi
        elif [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            if command -v brew &> /dev/null; then
                log_info "Installing Trivy on macOS..."
                brew install aquasecurity/trivy/trivy
            else
                log_error "Homebrew not installed. Please install Trivy manually."
                exit 1
            fi
        else
            log_error "Unsupported OS. Please install Trivy manually."
            exit 1
        fi
    fi
    
    # Verify installation
    if ! command -v trivy &> /dev/null; then
        log_error "Failed to install Trivy. Please install it manually."
        exit 1
    fi
    
    log_success "Trivy is installed."
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -b|--base)
            REGISTRY_BASE="$2"
            shift 2
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -s|--severity)
            SEVERITY="$2"
            shift 2
            ;;
        -f|--format)
            OUTPUT_FORMAT="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        -x|--fail)
            FAIL_ON_SEVERITY="$2"
            shift 2
            ;;
        -a|--all)
            # Find all services with Dockerfiles
            while IFS= read -r dockerfile; do
                service=$(echo "$dockerfile" | sed 's|./\([^/]*\)/.*|\1|')
                SERVICES+=("$service")
            done < <(find . -name "Dockerfile" -path "./mcp-*" | sort)
            shift
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
            SERVICES+=("$1")
            shift
            ;;
    esac
done

# Validate arguments
if [[ ${#SERVICES[@]} -eq 0 ]]; then
    log_error "No services specified"
    show_help
    exit 1
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Check if Trivy is installed
check_trivy

# Scan images
log_info "Scanning Docker images for vulnerabilities: ${SERVICES[*]}"
log_info "Severity levels: $SEVERITY"
log_info "Output format: $OUTPUT_FORMAT"
log_info "Output directory: $OUTPUT_DIR"

FAILED=0

for service in "${SERVICES[@]}"; do
    IMAGE="$REGISTRY/$REGISTRY_BASE/$service:$VERSION"
    OUTPUT_FILE="$OUTPUT_DIR/$service-$VERSION-$(date +%Y%m%d-%H%M%S)"
    
    log_info "Scanning $IMAGE"
    
    case $OUTPUT_FORMAT in
        json)
            OUTPUT_FILE="$OUTPUT_FILE.json"
            trivy image --format json --output "$OUTPUT_FILE" --severity "$SEVERITY" "$IMAGE"
            ;;
        sarif)
            OUTPUT_FILE="$OUTPUT_FILE.sarif"
            trivy image --format sarif --output "$OUTPUT_FILE" --severity "$SEVERITY" "$IMAGE"
            ;;
        *)
            OUTPUT_FILE="$OUTPUT_FILE.txt"
            trivy image --format table --output "$OUTPUT_FILE" --severity "$SEVERITY" "$IMAGE"
            ;;
    esac
    
    # Check for vulnerabilities at the specified fail level
    if trivy image --quiet --severity "$FAIL_ON_SEVERITY" "$IMAGE" 2>/dev/null; then
        log_success "No $FAIL_ON_SEVERITY vulnerabilities found in $service:$VERSION"
    else
        log_error "$FAIL_ON_SEVERITY vulnerabilities found in $service:$VERSION"
        FAILED=1
    fi
    
    log_info "Scan report saved to $OUTPUT_FILE"
done

# Generate summary report
SUMMARY_FILE="$OUTPUT_DIR/vulnerability-summary-$(date +%Y%m%d-%H%M%S).md"
log_info "Generating summary report: $SUMMARY_FILE"

cat > "$SUMMARY_FILE" << EOF
# Docker Image Vulnerability Scan Summary

- **Date:** $(date +"%Y-%m-%d %H:%M:%S")
- **Registry:** $REGISTRY/$REGISTRY_BASE
- **Version:** $VERSION
- **Severity Levels:** $SEVERITY

## Scanned Images

| Service | Status | Report |
|---------|--------|--------|
EOF

for service in "${SERVICES[@]}"; do
    LATEST_REPORT=$(ls -t "$OUTPUT_DIR/$service-$VERSION-"* 2>/dev/null | head -n1)
    REPORT_NAME=$(basename "$LATEST_REPORT")
    
    if trivy image --quiet --severity "$FAIL_ON_SEVERITY" "$REGISTRY/$REGISTRY_BASE/$service:$VERSION" 2>/dev/null; then
        STATUS="✅ Pass"
    else
        STATUS="❌ Fail"
    fi
    
    echo "| $service | $STATUS | [$REPORT_NAME]($REPORT_NAME) |" >> "$SUMMARY_FILE"
done

cat >> "$SUMMARY_FILE" << EOF

## Summary

- **Total Images Scanned:** ${#SERVICES[@]}
- **Failed:** $FAILED
- **Passed:** $((${#SERVICES[@]} - FAILED))

## Remediation

For any failed images, please review the detailed reports and address the vulnerabilities by:

1. Updating base images to newer versions
2. Applying security patches
3. Removing unnecessary packages
4. Updating dependencies with known vulnerabilities

## Next Steps

- Review detailed reports for each image
- Prioritize fixing critical vulnerabilities
- Update images and rescan to verify fixes
EOF

log_info "Summary report generated: $SUMMARY_FILE"

if [[ $FAILED -eq 0 ]]; then
    log_success "All images passed vulnerability scanning"
    exit 0
else
    log_error "Some images failed vulnerability scanning"
    exit 1
fi