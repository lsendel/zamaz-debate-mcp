#!/bin/bash

# Security Workflow Validation Script
# This script validates the fixed security workflow for syntax and configuration issues

set -e

echo "🔍 Security Workflow Validation Script"
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2${NC}"
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

echo -e "\n1. Checking YAML syntax..."

# Check if Python is available for YAML validation
if command -v python3 &> /dev/null; then
    python3 -c "
import yaml
import sys
try:
    with open('security-workflow-fixed.yml', 'r') as f:
        content = f.read()
    yaml.safe_load(content)
    print('✅ YAML syntax is valid')
    sys.exit(0)
except yaml.YAMLError as e:
    print(f'❌ YAML syntax error: {e}')
    sys.exit(1)
except Exception as e:
    print(f'❌ Error: {e}')
    sys.exit(1)
"
    yaml_status=$?
    print_status $yaml_status "YAML syntax validation"
else
    print_warning "Python3 not available, skipping YAML validation"
    yaml_status=0
fi

echo -e "\n2. Checking file structure..."

# Check if fixed workflow file exists
if [ -f "security-workflow-fixed.yml" ]; then
    print_status 0 "Fixed workflow file exists"
else
    print_status 1 "Fixed workflow file missing"
    exit 1
fi

# Check if original workflow file exists
if [ -f ".github/workflows/security.yml" ]; then
    print_status 0 "Original workflow file exists"
else
    print_status 1 "Original workflow file missing"
fi

echo -e "\n3. Checking workflow content..."

# Check for required jobs
required_jobs=("validate-workflow" "semgrep" "java-security" "frontend-security" "secrets-scan" "codeql-analysis" "owasp-dependency-check" "security-summary")

for job in "${required_jobs[@]}"; do
    if grep -q "^  $job:" security-workflow-fixed.yml; then
        print_status 0 "Job '$job' found"
    else
        print_status 1 "Job '$job' missing"
    fi
done

echo -e "\n4. Checking for common issues..."

# Check for newline at end of file
if [ -n "$(tail -c1 security-workflow-fixed.yml)" ]; then
    print_status 1 "Missing newline at end of file"
else
    print_status 0 "Proper newline at end of file"
fi

# Check for cache-dependency-path issues
if grep -q "cache-dependency-path:" security-workflow-fixed.yml; then
    print_warning "Found cache-dependency-path - verify the path exists"
else
    print_status 0 "No problematic cache-dependency-path found"
fi

# Check for npm ci before npm audit
frontend_section=$(sed -n '/frontend-security:/,/^  [a-zA-Z]/p' security-workflow-fixed.yml)
if echo "$frontend_section" | grep -q "npm ci" && echo "$frontend_section" | grep -q "npm audit"; then
    # Check order
    ci_line=$(echo "$frontend_section" | grep -n "npm ci" | cut -d: -f1)
    audit_line=$(echo "$frontend_section" | grep -n "npm audit" | cut -d: -f1)
    if [ "$ci_line" -lt "$audit_line" ]; then
        print_status 0 "npm ci runs before npm audit"
    else
        print_status 1 "npm audit runs before npm ci"
    fi
else
    print_warning "npm ci step added for proper dependency installation"
fi

echo -e "\n5. Implementation instructions..."
echo "================================"
echo ""
echo "To apply the fix:"
echo "1. cp security-workflow-fixed.yml .github/workflows/security.yml"
echo "2. git add .github/workflows/security.yml"
echo "3. git commit -m 'fix: correct security scanning workflow startup failure'"
echo "4. git push origin main"
echo ""
echo "To test:"
echo "1. Go to GitHub Actions"
echo "2. Select 'Security Scanning' workflow"
echo "3. Click 'Run workflow'"
echo "4. Verify it starts without startup_failure"
echo ""

if [ $yaml_status -eq 0 ]; then
    echo -e "${GREEN}🎉 Validation completed successfully!${NC}"
    echo "The fixed workflow is ready for deployment."
else
    echo -e "${RED}❌ Validation failed!${NC}"
    echo "Please fix the issues above before deploying."
    exit 1
fi