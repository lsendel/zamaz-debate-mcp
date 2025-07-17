#!/bin/bash

# SonarCloud Report Generator using API
# This script generates markdown reports directly from SonarCloud API

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
REPORTS_DIR="${PROJECT_ROOT}/sonar-reports"

# SonarCloud Configuration
SONAR_URL="${SONAR_URL:-https://sonarcloud.io}"
SONAR_TOKEN="${SONAR_TOKEN:-}"
SONAR_PROJECT_KEY="${SONAR_PROJECT_KEY:-lsendel_zamaz-debate-mcp}"
SONAR_ORGANIZATION="${SONAR_ORGANIZATION:-lsendel}"
SONAR_BRANCH="${SONAR_BRANCH:-main}"

# Report Configuration
REPORT_DATE=$(date +%Y%m%d_%H%M%S)
REPORT_FILENAME="sonarcloud-report-${SONAR_PROJECT_KEY//[:]/-}-${REPORT_DATE}.md"
REPORT_PATH="${REPORTS_DIR}/${REPORT_FILENAME}"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
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

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    if ! command -v curl &> /dev/null; then
        log_error "curl is required but not installed."
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        log_error "jq is required but not installed. Install with: brew install jq"
        exit 1
    fi
    
    if [ -z "$SONAR_TOKEN" ]; then
        log_error "SONAR_TOKEN environment variable is required"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# API call helper
api_call() {
    local endpoint=$1
    curl -s -H "Authorization: Bearer $SONAR_TOKEN" \
         "${SONAR_URL}/api/${endpoint}"
}

# Get project information
get_project_info() {
    log_info "Fetching project information..."
    api_call "components/show?component=${SONAR_PROJECT_KEY}"
}

# Get project measures
get_measures() {
    log_info "Fetching project metrics..."
    local metrics="alert_status,bugs,reliability_rating,vulnerabilities,security_rating,security_hotspots,security_hotspots_reviewed,security_review_rating,code_smells,sqale_rating,sqale_index,duplicated_lines_density,coverage,lines,ncloc"
    api_call "measures/component?component=${SONAR_PROJECT_KEY}&metricKeys=${metrics}"
}

# Get issues summary
get_issues() {
    log_info "Fetching issues..."
    api_call "issues/search?componentKeys=${SONAR_PROJECT_KEY}&ps=1&facets=severities,types,resolutions"
}

# Get quality gate status
get_quality_gate() {
    log_info "Fetching quality gate status..."
    api_call "qualitygates/project_status?projectKey=${SONAR_PROJECT_KEY}"
}

# Format metric value
format_metric() {
    local key=$1
    local value=$2
    
    case $key in
        coverage|duplicated_lines_density)
            echo "${value}%"
            ;;
        sqale_index)
            # Convert minutes to days/hours
            local minutes=${value%.*}
            if [ $minutes -lt 60 ]; then
                echo "${minutes}min"
            elif [ $minutes -lt 1440 ]; then
                echo "$((minutes / 60))h $((minutes % 60))min"
            else
                echo "$((minutes / 1440))d $((minutes % 1440 / 60))h"
            fi
            ;;
        *)
            echo "$value"
            ;;
    esac
}

# Generate markdown report
generate_report() {
    log_info "Generating markdown report..."
    
    mkdir -p "$REPORTS_DIR"
    
    # Fetch all data
    local project_info=$(get_project_info)
    local measures=$(get_measures)
    local issues=$(get_issues)
    local quality_gate=$(get_quality_gate)
    
    # Start writing report
    cat > "$REPORT_PATH" << EOF
# SonarCloud Analysis Report

**Project**: ${SONAR_PROJECT_KEY}  
**Organization**: ${SONAR_ORGANIZATION}  
**Generated**: $(date '+%Y-%m-%d %H:%M:%S')  
**Branch**: ${SONAR_BRANCH}

---

## Quality Gate Status

EOF

    # Add quality gate status
    local qg_status=$(echo "$quality_gate" | jq -r '.projectStatus.status')
    if [ "$qg_status" = "OK" ]; then
        echo "✅ **PASSED**" >> "$REPORT_PATH"
    else
        echo "❌ **FAILED**" >> "$REPORT_PATH"
    fi
    
    echo "" >> "$REPORT_PATH"
    echo "## Key Metrics" >> "$REPORT_PATH"
    echo "" >> "$REPORT_PATH"
    
    # Create metrics table
    cat >> "$REPORT_PATH" << EOF
| Metric | Value | Rating |
|--------|-------|--------|
EOF

    # Extract and format metrics
    local bugs=$(echo "$measures" | jq -r '.component.measures[] | select(.metric=="bugs") | .value // "0"')
    local vulnerabilities=$(echo "$measures" | jq -r '.component.measures[] | select(.metric=="vulnerabilities") | .value // "0"')
    local code_smells=$(echo "$measures" | jq -r '.component.measures[] | select(.metric=="code_smells") | .value // "0"')
    local coverage=$(echo "$measures" | jq -r '.component.measures[] | select(.metric=="coverage") | .value // "N/A"')
    local duplications=$(echo "$measures" | jq -r '.component.measures[] | select(.metric=="duplicated_lines_density") | .value // "0"')
    local debt=$(echo "$measures" | jq -r '.component.measures[] | select(.metric=="sqale_index") | .value // "0"')
    local security_hotspots=$(echo "$measures" | jq -r '.component.measures[] | select(.metric=="security_hotspots") | .value // "0"')
    local ncloc=$(echo "$measures" | jq -r '.component.measures[] | select(.metric=="ncloc") | .value // "0"')
    
    # Ratings
    local reliability_rating=$(echo "$measures" | jq -r '.component.measures[] | select(.metric=="reliability_rating") | .value // "1"')
    local security_rating=$(echo "$measures" | jq -r '.component.measures[] | select(.metric=="security_rating") | .value // "1"')
    local maintainability_rating=$(echo "$measures" | jq -r '.component.measures[] | select(.metric=="sqale_rating") | .value // "1"')
    
    # Convert ratings to letters
    rating_to_letter() {
        case $1 in
            1) echo "A" ;;
            2) echo "B" ;;
            3) echo "C" ;;
            4) echo "D" ;;
            5) echo "E" ;;
            *) echo "-" ;;
        esac
    }
    
    # Write metrics
    echo "| **Bugs** | $bugs | $(rating_to_letter $reliability_rating) |" >> "$REPORT_PATH"
    echo "| **Vulnerabilities** | $vulnerabilities | $(rating_to_letter $security_rating) |" >> "$REPORT_PATH"
    echo "| **Security Hotspots** | $security_hotspots | - |" >> "$REPORT_PATH"
    echo "| **Code Smells** | $code_smells | $(rating_to_letter $maintainability_rating) |" >> "$REPORT_PATH"
    echo "| **Coverage** | $(format_metric coverage $coverage) | - |" >> "$REPORT_PATH"
    echo "| **Duplications** | $(format_metric duplicated_lines_density $duplications) | - |" >> "$REPORT_PATH"
    echo "| **Technical Debt** | $(format_metric sqale_index $debt) | - |" >> "$REPORT_PATH"
    echo "| **Lines of Code** | $ncloc | - |" >> "$REPORT_PATH"
    
    # Add issues breakdown
    echo "" >> "$REPORT_PATH"
    echo "## Issues Breakdown" >> "$REPORT_PATH"
    echo "" >> "$REPORT_PATH"
    
    # Extract facets
    local severities=$(echo "$issues" | jq -r '.facets[] | select(.property=="severities") | .values')
    
    echo "### By Severity" >> "$REPORT_PATH"
    echo "" >> "$REPORT_PATH"
    echo "| Severity | Count |" >> "$REPORT_PATH"
    echo "|----------|-------|" >> "$REPORT_PATH"
    
    for severity in BLOCKER CRITICAL MAJOR MINOR INFO; do
        local count=$(echo "$severities" | jq -r ".[] | select(.val==\"$severity\") | .count // 0")
        if [ "$count" != "0" ] && [ -n "$count" ]; then
            echo "| $severity | $count |" >> "$REPORT_PATH"
        fi
    done
    
    # Add project link
    echo "" >> "$REPORT_PATH"
    echo "---" >> "$REPORT_PATH"
    echo "" >> "$REPORT_PATH"
    echo "📊 [View Full Analysis on SonarCloud](https://sonarcloud.io/project/overview?id=${SONAR_PROJECT_KEY})" >> "$REPORT_PATH"
    
    # Create symlink to latest
    ln -sf "$REPORT_FILENAME" "${REPORTS_DIR}/latest-sonarcloud-report.md"
    
    log_success "Report generated: $REPORT_PATH"
}

# Main execution
main() {
    echo "================================================"
    echo "SonarCloud Report Generator"
    echo "================================================"
    echo
    
    check_prerequisites
    generate_report
    
    echo
    echo "================================================"
    log_success "Report generation completed!"
    log_info "View report: ${REPORTS_DIR}/latest-sonarcloud-report.md"
    echo "================================================"
}

# Run main function
main "$@"