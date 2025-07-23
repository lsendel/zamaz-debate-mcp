#!/bin/bash
# Script to validate all GitHub Actions workflow files

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
WORKFLOWS_DIR="$REPO_ROOT/.github/workflows"

echo "🔍 Validating GitHub Actions workflows..."
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Track validation results
TOTAL_FILES=0
VALID_FILES=0
INVALID_FILES=0
WARNINGS=0

# Function to validate YAML syntax
validate_yaml() {
    local file=$1
    local filename=$(basename "$file")
    
    echo -n "Checking $filename... "
    
    # Check YAML syntax
    if python3 -c "import yaml; yaml.safe_load(open('$file', 'r'))" 2>/dev/null; then
        echo -e "${GREEN}✓ Valid YAML${NC}"
        ((VALID_FILES++))
        
        # Additional checks for workflow files
        if grep -q "workflow_call" "$file"; then
            # Check for GITHUB_TOKEN in secrets of workflow_call
            if grep -A 10 "workflow_call:" "$file" | grep -A 10 "secrets:" | grep -q "GITHUB_TOKEN:"; then
                echo -e "  ${YELLOW}⚠ Warning: GITHUB_TOKEN in workflow_call secrets (reserved name)${NC}"
                ((WARNINGS++))
            fi
        fi
        
        # Check for multi-line expressions within ${{ }}
        if grep -Pzo '\$\{\{[^}]*\n[^}]*\}\}' "$file" >/dev/null 2>&1; then
            echo -e "  ${YELLOW}⚠ Warning: Multi-line expression detected in \${{ }}${NC}"
            ((WARNINGS++))
        fi
        
        # Check for failure handler integration
        if grep -q "uses: ./.github/workflows/workflow-failure-handler.yml" "$file"; then
            echo -e "  ${GREEN}✓ Has failure handler${NC}"
            
            # Check if GITHUB_TOKEN is passed to failure handler
            if grep -B 5 -A 20 "uses: ./.github/workflows/workflow-failure-handler.yml" "$file" | grep -q "GITHUB_TOKEN:"; then
                echo -e "  ${YELLOW}⚠ Warning: GITHUB_TOKEN passed to failure handler${NC}"
                ((WARNINGS++))
            fi
        fi
        
        return 0
    else
        echo -e "${RED}✗ Invalid YAML${NC}"
        ((INVALID_FILES++))
        
        # Show detailed error
        echo -e "  ${RED}Error details:${NC}"
        python3 -c "import yaml; yaml.safe_load(open('$file', 'r'))" 2>&1 | sed 's/^/    /'
        return 1
    fi
}

# Function to check workflow best practices
check_best_practices() {
    local file=$1
    local filename=$(basename "$file")
    
    # Check for hardcoded values that should be secrets
    if grep -E "(password|token|key|secret)" "$file" | grep -v -E "(\$\{\{|secrets\.|github\.token)" >/dev/null 2>&1; then
        echo -e "  ${YELLOW}⚠ Warning: Possible hardcoded secrets detected${NC}"
        ((WARNINGS++))
    fi
    
    # Check for proper job dependencies
    if grep -q "needs:" "$file" && grep -q "if: failure()" "$file"; then
        echo -e "  ${GREEN}✓ Has proper failure handling${NC}"
    fi
}

# Main validation loop
for workflow_file in "$WORKFLOWS_DIR"/*.yml "$WORKFLOWS_DIR"/*.yaml; do
    if [ -f "$workflow_file" ]; then
        ((TOTAL_FILES++))
        validate_yaml "$workflow_file"
        check_best_practices "$workflow_file"
        echo
    fi
done

# Summary
echo "=================================="
echo "Validation Summary:"
echo "  Total files: $TOTAL_FILES"
echo -e "  ${GREEN}Valid files: $VALID_FILES${NC}"
echo -e "  ${RED}Invalid files: $INVALID_FILES${NC}"
echo -e "  ${YELLOW}Warnings: $WARNINGS${NC}"

# Exit with appropriate code
if [ $INVALID_FILES -gt 0 ]; then
    echo -e "\n${RED}❌ Validation failed! Fix the invalid files before committing.${NC}"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    echo -e "\n${YELLOW}⚠️  Validation passed with warnings. Review the warnings above.${NC}"
    exit 0
else
    echo -e "\n${GREEN}✅ All workflows are valid!${NC}"
    exit 0
fi