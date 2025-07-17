#!/bin/bash

# API Security Scanning Script
# This script runs DAST (Dynamic Application Security Testing) on API endpoints using OWASP ZAP

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
TARGET_URL="http://localhost:8080"
API_SPEC_FILE=""
REPORT_DIR="./security-reports/dast"
SCAN_TYPE="baseline"
ZAP_OPTIONS=""
FAIL_ON_SEVERITY="HIGH"

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
    echo "API Security Scanning Script"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -t, --target <url>           Target URL to scan (default: http://localhost:8080)"
    echo "  -s, --spec <file>            OpenAPI/Swagger specification file (optional)"
    echo "  -o, --output <dir>           Output directory (default: ./security-reports/dast)"
    echo "  -m, --mode <mode>            Scan mode: baseline, full, api (default: baseline)"
    echo "  -f, --fail <severity>        Fail on severity level: LOW, MEDIUM, HIGH, CRITICAL (default: HIGH)"
    echo "  -z, --zap-options <options>  Additional ZAP options"
    echo "  -h, --help                   Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --target http://localhost:8080"
    echo "  $0 --target http://localhost:8080 --mode full"
    echo "  $0 --target http://localhost:8080 --spec api-spec.json --mode api"
}

# Check if ZAP is installed or available via Docker
check_zap() {
    if command -v zap-baseline.py &> /dev/null; then
        log_info "Using local ZAP installation"
        ZAP_CMD="zap"
        return 0
    elif command -v docker &> /dev/null; then
        log_info "Using ZAP via Docker"
        ZAP_CMD="docker"
        return 0
    else
        log_error "Neither local ZAP nor Docker found. Please install one of them."
        exit 1
    fi
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--target)
            TARGET_URL="$2"
            shift 2
            ;;
        -s|--spec)
            API_SPEC_FILE="$2"
            shift 2
            ;;
        -o|--output)
            REPORT_DIR="$2"
            shift 2
            ;;
        -m|--mode)
            SCAN_TYPE="$2"
            shift 2
            ;;
        -f|--fail)
            FAIL_ON_SEVERITY="$2"
            shift 2
            ;;
        -z|--zap-options)
            ZAP_OPTIONS="$2"
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

# Check if ZAP is available
check_zap

# Generate timestamp for reports
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Run ZAP scan based on scan type
case $SCAN_TYPE in
    baseline)
        log_info "Running ZAP baseline scan against $TARGET_URL"
        
        if [[ "$ZAP_CMD" == "zap" ]]; then
            zap-baseline.py -t "$TARGET_URL" -r "$REPORT_DIR/zap-baseline-$TIMESTAMP.html" -J "$REPORT_DIR/zap-baseline-$TIMESTAMP.json" $ZAP_OPTIONS
        else
            docker run --rm -v "$(pwd)/$REPORT_DIR:/zap/wrk/:rw" owasp/zap2docker-stable zap-baseline.py -t "$TARGET_URL" -r /zap/wrk/zap-baseline-$TIMESTAMP.html -J /zap/wrk/zap-baseline-$TIMESTAMP.json $ZAP_OPTIONS
        fi
        ;;
    
    full)
        log_info "Running ZAP full scan against $TARGET_URL"
        
        if [[ "$ZAP_CMD" == "zap" ]]; then
            zap-full-scan.py -t "$TARGET_URL" -r "$REPORT_DIR/zap-full-$TIMESTAMP.html" -J "$REPORT_DIR/zap-full-$TIMESTAMP.json" $ZAP_OPTIONS
        else
            docker run --rm -v "$(pwd)/$REPORT_DIR:/zap/wrk/:rw" owasp/zap2docker-stable zap-full-scan.py -t "$TARGET_URL" -r /zap/wrk/zap-full-$TIMESTAMP.html -J /zap/wrk/zap-full-$TIMESTAMP.json $ZAP_OPTIONS
        fi
        ;;
    
    api)
        if [[ -z "$API_SPEC_FILE" ]]; then
            log_error "API scan requires an OpenAPI/Swagger specification file"
            exit 1
        fi
        
        log_info "Running ZAP API scan against $TARGET_URL using spec $API_SPEC_FILE"
        
        # Create directory for API spec file
        mkdir -p "$REPORT_DIR/specs"
        cp "$API_SPEC_FILE" "$REPORT_DIR/specs/"
        SPEC_FILENAME=$(basename "$API_SPEC_FILE")
        
        if [[ "$ZAP_CMD" == "zap" ]]; then
            zap-api-scan.py -t "$TARGET_URL" -f "$API_SPEC_FILE" -r "$REPORT_DIR/zap-api-$TIMESTAMP.html" -J "$REPORT_DIR/zap-api-$TIMESTAMP.json" $ZAP_OPTIONS
        else
            docker run --rm -v "$(pwd)/$REPORT_DIR:/zap/wrk/:rw" owasp/zap2docker-stable zap-api-scan.py -t "$TARGET_URL" -f "/zap/wrk/specs/$SPEC_FILENAME" -r /zap/wrk/zap-api-$TIMESTAMP.html -J /zap/wrk/zap-api-$TIMESTAMP.json $ZAP_OPTIONS
        fi
        ;;
    
    *)
        log_error "Unknown scan type: $SCAN_TYPE"
        show_help
        exit 1
        ;;
esac

# Check scan results
if [[ -f "$REPORT_DIR/zap-$SCAN_TYPE-$TIMESTAMP.json" ]]; then
    # Parse JSON to check for high/critical alerts
    if command -v jq &> /dev/null; then
        HIGH_ALERTS=$(jq '.site[0].alerts[] | select(.riskcode >= 3) | .instances | length' "$REPORT_DIR/zap-$SCAN_TYPE-$TIMESTAMP.json" 2>/dev/null | awk '{sum+=$1} END {print sum}')
        MEDIUM_ALERTS=$(jq '.site[0].alerts[] | select(.riskcode == 2) | .instances | length' "$REPORT_DIR/zap-$SCAN_TYPE-$TIMESTAMP.json" 2>/dev/null | awk '{sum+=$1} END {print sum}')
        LOW_ALERTS=$(jq '.site[0].alerts[] | select(.riskcode == 1) | .instances | length' "$REPORT_DIR/zap-$SCAN_TYPE-$TIMESTAMP.json" 2>/dev/null | awk '{sum+=$1} END {print sum}')
        
        log_info "Scan results:"
        log_info "- High/Critical alerts: ${HIGH_ALERTS:-0}"
        log_info "- Medium alerts: ${MEDIUM_ALERTS:-0}"
        log_info "- Low alerts: ${LOW_ALERTS:-0}"
        
        # Generate markdown report
        cat > "$REPORT_DIR/zap-summary-$TIMESTAMP.md" << EOF
# ZAP Security Scan Summary

- **Date:** $(date +"%Y-%m-%d %H:%M:%S")
- **Target:** $TARGET_URL
- **Scan Type:** $SCAN_TYPE
- **Report:** [HTML Report](./zap-$SCAN_TYPE-$TIMESTAMP.html) | [JSON Report](./zap-$SCAN_TYPE-$TIMESTAMP.json)

## Alerts Summary

| Severity | Count |
|----------|-------|
| High/Critical | ${HIGH_ALERTS:-0} |
| Medium | ${MEDIUM_ALERTS:-0} |
| Low | ${LOW_ALERTS:-0} |

## Recommendations

1. Review all high and critical alerts immediately
2. Address medium alerts in upcoming sprints
3. Document low alerts for future consideration
4. Run regular security scans to track progress

EOF
        
        # Check if we should fail based on severity
        case $FAIL_ON_SEVERITY in
            CRITICAL)
                # ZAP doesn't distinguish between high and critical, so we use high (3+)
                if [[ "${HIGH_ALERTS:-0}" -gt 0 ]]; then
                    log_error "Critical alerts found. Failing build."
                    exit 1
                fi
                ;;
            HIGH)
                if [[ "${HIGH_ALERTS:-0}" -gt 0 ]]; then
                    log_error "High or critical alerts found. Failing build."
                    exit 1
                fi
                ;;
            MEDIUM)
                if [[ "${HIGH_ALERTS:-0}" -gt 0 || "${MEDIUM_ALERTS:-0}" -gt 0 ]]; then
                    log_error "Medium or higher alerts found. Failing build."
                    exit 1
                fi
                ;;
            LOW)
                if [[ "${HIGH_ALERTS:-0}" -gt 0 || "${MEDIUM_ALERTS:-0}" -gt 0 || "${LOW_ALERTS:-0}" -gt 0 ]]; then
                    log_error "Alerts found. Failing build."
                    exit 1
                fi
                ;;
            *)
                log_warning "Unknown severity level: $FAIL_ON_SEVERITY. Not failing build."
                ;;
        esac
    else
        log_warning "jq not installed. Cannot parse JSON results."
    fi
else
    log_warning "No JSON report found. Cannot analyze results."
fi

log_success "API security scan completed. Reports saved to $REPORT_DIR"