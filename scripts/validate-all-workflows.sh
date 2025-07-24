#!/bin/bash

echo "🔍 Validating all workflow files..."
echo ""

# Find all workflow files
workflow_files=$(find .github/workflows -name "*.yml" -o -name "*.yaml" | sort)
total_files=$(echo "$workflow_files" | wc -l)
errors_found=0

echo "Found $total_files workflow files to validate"
echo ""

# Check each workflow file
for file in $workflow_files; do
    echo -n "Checking $file... "
    
    # Basic YAML syntax check
    if python3 -c "import yaml; yaml.safe_load(open('$file'))" 2>/dev/null; then
        echo -n "✓ YAML valid"
        
        # Check for common job dependency issues
        jobs=$(grep -E "^\s*[a-zA-Z0-9_-]+:" "$file" | grep -v "^name:" | grep -v "^on:" | sed 's/://' | tr -d ' ')
        
        # Check if any needs reference non-existent jobs
        needs_refs=$(grep -E "needs:\s*\[" "$file" | sed 's/.*needs:\s*\[//' | sed 's/\]//' | tr ',' '\n' | tr -d ' ')
        
        if [ -n "$needs_refs" ]; then
            for need in $needs_refs; do
                if ! echo "$jobs" | grep -q "^$need$"; then
                    echo ""
                    echo "  ❌ ERROR: Job dependency '$need' not found in workflow!"
                    ((errors_found++))
                fi
            done
        fi
        
        # Check for deprecated actions
        if grep -q "actions/checkout@v[123]" "$file"; then
            echo ""
            echo "  ⚠️  WARNING: Using old checkout action version (recommend v4)"
        fi
        
        if grep -q "actions/setup-node@v[123]" "$file"; then
            echo ""
            echo "  ⚠️  WARNING: Using old setup-node action version (recommend v4)"
        fi
        
        echo " ✓"
    else
        echo "❌ YAML syntax error!"
        ((errors_found++))
    fi
done

echo ""
echo "========================================="
echo "Validation Summary:"
echo "Total files checked: $total_files"
echo "Errors found: $errors_found"

if [ $errors_found -eq 0 ]; then
    echo "✅ All workflows are valid!"
else
    echo "❌ Found $errors_found workflow errors that need fixing"
    exit 1
fi