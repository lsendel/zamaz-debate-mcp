#!/bin/bash

# SonarQube Report Generation Script
# This script generates markdown reports from SonarQube analysis
# Uses the CNES Report tool: https://github.com/cnescatlab/sonar-cnes-report

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname """$SCRIPT_DIR""")"
REPORTS_DIR="${PROJECT_ROOT}/sonar-reports"
TOOLS_DIR="${PROJECT_ROOT}/tools"
CNES_REPORT_JAR="${TOOLS_DIR}/sonar-cnes-report.jar"
CNES_REPORT_VERSION="4.2.0"

# SonarQube Configuration
SONAR_URL="${SONAR_URL:-http://localhost:9000}"
SONAR_TOKEN="${SONAR_TOKEN:-}"
SONAR_PROJECT_KEY="${SONAR_PROJECT_KEY:-com.zamaz.mcp:mcp-parent}"
SONAR_ORGANIZATION="${SONAR_ORGANIZATION:-}"
SONAR_BRANCH="${SONAR_BRANCH:-main}"

# Report Configuration
REPORT_AUTHOR="${REPORT_AUTHOR:-MCP Team}"
REPORT_LANGUAGE="${REPORT_LANGUAGE:-en_US}"
REPORT_DATE=$(date +%Y%m%d_%H%M%S)
REPORT_FILENAME="sonar-report-${SONAR_PROJECT_KEY//[:]/-}-${REPORT_DATE}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
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
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed. Please install Java 8 or higher."
        exit 1
    fi
    
    # Check Java version
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1-2)
    JAVA_MAJOR=$(echo ""$JAVA_VERSION"" | cut -d'.' -f1)
    if [[ ""$JAVA_MAJOR"" -lt 8 ]]; then
        log_error "Java 8 or higher is required. Current version: ""$JAVA_VERSION"""
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Download CNES Report tool if not present
download_cnes_report() {
    if [ ! -f """$CNES_REPORT_JAR""" ]; then
        log_info "Downloading CNES Report tool v${CNES_REPORT_VERSION}..."
        
        mkdir -p """$TOOLS_DIR"""
        
        DOWNLOAD_URL="https://github.com/cnescatlab/sonar-cnes-report/releases/download/${CNES_REPORT_VERSION}/sonar-cnes-report-${CNES_REPORT_VERSION}.jar"
        
        if command -v wget &> /dev/null; then
            wget -q """$DOWNLOAD_URL""" -O """$CNES_REPORT_JAR"""
        elif command -v curl &> /dev/null; then
            curl -sL """$DOWNLOAD_URL""" -o """$CNES_REPORT_JAR"""
        else
            log_error "Neither wget nor curl is available. Please install one of them."
            exit 1
        fi
        
        if [ -f """$CNES_REPORT_JAR""" ]; then
            log_success "CNES Report tool downloaded successfully"
        else
            log_error "Failed to download CNES Report tool"
            exit 1
        fi
    else
        log_info "CNES Report tool already present"
    fi
}

# Validate SonarQube connection
validate_sonarqube_connection() {
    log_info "Validating SonarQube connection..."
    
    if [ -z """$SONAR_TOKEN""" ]; then
        log_warning "SONAR_TOKEN not set. Trying anonymous access..."
        AUTH_HEADER=""
    else
        AUTH_HEADER="Authorization: Bearer ""$SONAR_TOKEN"""
    fi
    
    # Test connection
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H """$AUTH_HEADER""" "${SONAR_URL}/api/system/status")
    
    if [ """$HTTP_CODE""" != "200" ]; then
        log_error "Cannot connect to SonarQube at ${SONAR_URL}. HTTP Code: ${HTTP_CODE}"
        log_error "Please check your SONAR_URL and SONAR_TOKEN environment variables"
        exit 1
    fi
    
    log_success "SonarQube connection validated"
}

# Generate report
generate_report() {
    log_info "Generating SonarQube report..."
    log_info "Project: ""$SONAR_PROJECT_KEY"""
    log_info "Branch: ""$SONAR_BRANCH"""
    log_info "Output format: Markdown"
    
    mkdir -p """$REPORTS_DIR"""
    
    # Build command
    CMD="java -jar ""$CNES_REPORT_JAR"""
    CMD="""$CMD"" -s ""$SONAR_URL"""
    CMD="""$CMD"" -p ""$SONAR_PROJECT_KEY"""
    CMD="""$CMD"" -o ""$REPORTS_DIR"""
    CMD="""$CMD"" -l ""$REPORT_LANGUAGE"""
    CMD="""$CMD"" -a \"""$REPORT_AUTHOR""\""
    CMD="""$CMD"" -b ""$SONAR_BRANCH"""
    CMD="""$CMD"" -r ""$REPORT_FILENAME"""
    
    if [ -n """$SONAR_TOKEN""" ]; then
        CMD="""$CMD"" -t ""$SONAR_TOKEN"""
    fi
    
    # Execute command
    log_info "Executing: ""$CMD"""
    
    if ""$CMD""; then
        log_success "Report generated successfully"
        
        # Find the generated markdown file
        MARKDOWN_FILE=$(find """$REPORTS_DIR""" -name "${REPORT_FILENAME}*.md" -type f -print -quit)
        
        if [ -n """$MARKDOWN_FILE""" ]; then
            log_success "Markdown report: ""$MARKDOWN_FILE"""
            
            # Create a latest symlink
            LATEST_LINK="${REPORTS_DIR}/latest-sonar-report.md"
            ln -sf "$(basename """$MARKDOWN_FILE""")" """$LATEST_LINK"""
            log_info "Latest report link: ""$LATEST_LINK"""
        else
            log_warning "Markdown file not found. Check other formats in ""$REPORTS_DIR"""
        fi
    else
        log_error "Failed to generate report"
        exit 1
    fi
}

# Create summary
create_summary() {
    log_info "Creating report summary..."
    
    SUMMARY_FILE="${REPORTS_DIR}/report-summary.json"
    
    cat > """$SUMMARY_FILE""" << EOF
{
  "generated_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "project_key": """$SONAR_PROJECT_KEY""",
  "branch": """$SONAR_BRANCH""",
  "sonar_url": """$SONAR_URL""",
  "report_file": "$(basename """$MARKDOWN_FILE""")",
  "author": """$REPORT_AUTHOR"""
}
EOF
    
    log_success "Summary created: ""$SUMMARY_FILE"""
}

# Main execution
main() {
    echo "================================================"
    echo "SonarQube Report Generator"
    echo "================================================"
    echo
    
    check_prerequisites
    download_cnes_report
    validate_sonarqube_connection
    generate_report
    create_summary
    
    echo
    echo "================================================"
    log_success "Report generation completed!"
    echo "================================================"
}

# Show usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Generate SonarQube reports in markdown format.

OPTIONS:
    -h, --help              Show this help message
    -u, --url URL           SonarQube server URL (default: ""$SONAR_URL"")
    -t, --token TOKEN       SonarQube authentication token
    -p, --project KEY       Project key (default: ""$SONAR_PROJECT_KEY"")
    -b, --branch BRANCH     Branch name (default: ""$SONAR_BRANCH"")
    -a, --author AUTHOR     Report author (default: ""$REPORT_AUTHOR"")

ENVIRONMENT VARIABLES:
    SONAR_URL               SonarQube server URL
    SONAR_TOKEN             Authentication token
    SONAR_PROJECT_KEY       Project key
    SONAR_BRANCH            Branch name
    REPORT_AUTHOR           Report author name

EXAMPLES:
    # Basic usage with environment variables
    export SONAR_TOKEN=your-token-here
    $0

    # With command line options
    $0 -u http://sonar.example.com -t your-token -p my-project

    # Generate report for specific branch
    $0 -b feature/new-feature
EOF
}

# Parse command line arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            exit 0
            ;;
        -u|--url)
            SONAR_URL="$2"
            shift 2
            ;;
        -t|--token)
            SONAR_TOKEN="$2"
            shift 2
            ;;
        -p|--project)
            SONAR_PROJECT_KEY="$2"
            shift 2
            ;;
        -b|--branch)
            SONAR_BRANCH="$2"
            shift 2
            ;;
        -a|--author)
            REPORT_AUTHOR="$2"
            shift 2
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Run main function
main