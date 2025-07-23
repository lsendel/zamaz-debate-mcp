#!/bin/bash
# Test script to verify security workflow syntax

echo "Testing security.yml workflow..."

# Test if the workflow file is valid YAML
if python3 -c "import yaml; yaml.safe_load(open('.github/workflows/security.yml', 'r'))" 2>/dev/null; then
    echo "✅ YAML syntax is valid"
else
    echo "❌ YAML syntax error"
    exit 1
fi

# Check for common issues that prevent jobs from running
echo ""
echo "Checking for common issues..."

# Check if any job has invalid 'if' conditions
if grep -E "if:.*failure\(\).*&&.*['\"]" .github/workflows/security.yml; then
    echo "❌ Found invalid failure() && string expressions"
fi

# Check for format() function usage (not supported)
if grep -q "format(" .github/workflows/security.yml; then
    echo "❌ Found format() function usage (might not be supported)"
else
    echo "✅ No format() function usage"
fi

# Check job structure
echo ""
echo "Jobs found in workflow:"
python3 -c "
import yaml
with open('.github/workflows/security.yml', 'r') as f:
    data = yaml.safe_load(f)
    jobs = data.get('jobs', {})
    for job_name, job_config in jobs.items():
        print(f'  - {job_name}')
        if 'if' in job_config:
            print(f'    Condition: {job_config[\"if\"]}')
"

echo ""
echo "✅ Workflow structure appears valid. If jobs still don't run, check:"
echo "  1. Repository secrets are configured (SEMGREP_APP_TOKEN, etc.)"
echo "  2. Branch protection rules allow workflow execution"
echo "  3. GitHub Actions is enabled for the repository"