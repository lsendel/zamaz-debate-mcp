#!/bin/bash
# SonarQube Analysis Runner Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
CONFIG_FILE="sonarqube_config.yaml"
FIX_ISSUES=false
DETAILED_REPORT=false
QUIET=false

# Help function
show_help() {
    cat << EOF
SonarQube Analysis Runner

Usage: $0 [OPTIONS]

Options:
    -c, --config FILE       Configuration file (default: sonarqube_config.yaml)
    -f, --fix-issues        Automatically fix issues when possible
    -d, --detailed-report   Generate detailed report only
    -q, --quiet             Suppress console output
    -h, --help              Show this help message

Examples:
    $0                      # Run analysis with default settings
    $0 --fix-issues         # Run analysis and fix issues
    $0 --detailed-report    # Generate detailed report only
    $0 -c custom.yaml -f    # Use custom config and fix issues

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        -f|--fix-issues)
            FIX_ISSUES=true
            shift
            ;;
        -d|--detailed-report)
            DETAILED_REPORT=true
            shift
            ;;
        -q|--quiet)
            QUIET=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo -e "${RED}Error: Unknown option $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check if configuration file exists
if [[ ! -f "$CONFIG_FILE" ]]; then
    echo -e "${RED}Error: Configuration file '$CONFIG_FILE' not found${NC}"
    exit 1
fi

# Load environment variables if .env exists
if [[ -f "../../.env" ]]; then
    echo -e "${BLUE}Loading environment variables from .env file...${NC}"
    export $(cat ../../.env | grep -v '^#' | grep -v '^$' | xargs)
fi

# Check if SONAR_TOKEN is set
if [[ -z "$SONAR_TOKEN" ]]; then
    echo -e "${RED}Error: SONAR_TOKEN environment variable is not set${NC}"
    echo -e "${YELLOW}Please set your SonarCloud token:${NC}"
    echo "export SONAR_TOKEN='your-token-here'"
    exit 1
fi

# Check if Python is available
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}Error: Python3 is required but not installed${NC}"
    exit 1
fi

# Install required Python packages if needed
echo -e "${BLUE}Checking Python dependencies...${NC}"
pip3 install -q requests pyyaml schedule 2>/dev/null || true

# Create output directory
mkdir -p sonar-reports

# Build command
CMD="python3 run-sonar-analysis.py --config '$CONFIG_FILE'"

if [[ "$FIX_ISSUES" == true ]]; then
    CMD="$CMD --fix-issues"
fi

if [[ "$DETAILED_REPORT" == true ]]; then
    CMD="$CMD --detailed-report"
fi

if [[ "$QUIET" == true ]]; then
    CMD="$CMD --quiet"
fi

# Run the analysis
echo -e "${GREEN}Starting SonarQube analysis...${NC}"
echo -e "${BLUE}Command: $CMD${NC}"

eval $CMD

exit_code=$?

if [[ $exit_code -eq 0 ]]; then
    echo -e "${GREEN}✅ Analysis completed successfully!${NC}"
    
    # Show generated files
    echo -e "${BLUE}Generated files:${NC}"
    ls -la sonar-reports/ | grep -E '\.(md|html|json)$' | tail -5
    
    if [[ "$FIX_ISSUES" == true ]]; then
        echo -e "${YELLOW}⚠️  Issues were automatically fixed. Please review the changes before committing.${NC}"
    fi
else
    echo -e "${RED}❌ Analysis failed with exit code $exit_code${NC}"
    exit $exit_code
fi