#!/bin/bash

# View Evidence Reports Script
# This script generates evidence reports and starts an HTTP server to view them

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
EVIDENCE_DIR="$PROJECT_ROOT/test-evidence-reports"
HTTP_PORT=8080

print_banner() {
    echo -e "${BLUE}"
    echo "==========================================="
    echo "  Evidence Report Viewer"
    echo "==========================================="
    echo -e "${NC}"
}

check_reports() {
    if [ ! -d "$EVIDENCE_DIR" ] || [ ! -f "$EVIDENCE_DIR/index.html" ]; then
        echo -e "${YELLOW}No evidence reports found. Would you like to generate them now? (y/n)${NC}"
        read -r response
        if [[ "$response" =~ ^[Yy]$ ]]; then
            echo -e "${BLUE}Generating evidence reports...${NC}"
            "$SCRIPT_DIR/generate-evidence-reports.sh"
        else
            echo -e "${RED}No reports to view. Run './scripts/generate-evidence-reports.sh' first.${NC}"
            exit 1
        fi
    fi
}

start_server() {
    echo -e "${GREEN}Starting HTTP server...${NC}"
    echo -e "${BLUE}Evidence reports available at:${NC}"
    echo ""
    echo -e "${GREEN}  🌐 http://localhost:${HTTP_PORT}${NC}"
    echo ""
    echo -e "${BLUE}Report Structure:${NC}"
    echo "  📋 Main Dashboard: http://localhost:${HTTP_PORT}/index.html"
    echo "  🔍 Consolidated Report: http://localhost:${HTTP_PORT}/consolidated-evidence-*.html"
    echo "  🚀 Service Reports: http://localhost:${HTTP_PORT}/html/"
    echo "  📄 Raw Evidence: http://localhost:${HTTP_PORT}/json/"
    echo "  📝 Summaries: http://localhost:${HTTP_PORT}/summary/"
    echo ""
    echo -e "${YELLOW}Press Ctrl+C to stop the server${NC}"
    echo ""
    
    # Change to evidence directory and start server
    cd "$EVIDENCE_DIR"
    
    # Check if Python 3 is available
    if command -v python3 &> /dev/null; then
        python3 -m http.server $HTTP_PORT
    elif command -v python &> /dev/null; then
        # Check if it's Python 3
        if python --version 2>&1 | grep -q "Python 3"; then
            python -m http.server $HTTP_PORT
        else
            # Python 2 fallback
            python -m SimpleHTTPServer $HTTP_PORT
        fi
    else
        echo -e "${RED}Python is not installed. Please install Python to use the HTTP server.${NC}"
        echo -e "${YELLOW}Alternatively, you can open the reports directly:${NC}"
        echo "  open $EVIDENCE_DIR/index.html"
        exit 1
    fi
}

# Main execution
main() {
    print_banner
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --port)
                HTTP_PORT="$2"
                shift 2
                ;;
            --generate)
                echo -e "${BLUE}Generating fresh evidence reports...${NC}"
                "$SCRIPT_DIR/generate-evidence-reports.sh"
                shift
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "  --port PORT     HTTP server port (default: 8080)"
                echo "  --generate      Generate fresh reports before viewing"
                echo "  -h, --help      Show this help message"
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    check_reports
    start_server
}

# Run main function
main "$@"