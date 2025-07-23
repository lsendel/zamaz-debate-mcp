#!/bin/bash

# Fix GITHUB_TOKEN in all workflows
echo "Fixing GITHUB_TOKEN references in workflows..."

# Find all workflow files
workflows=$(find /Users/lsendel/IdeaProjects/zamaz-debate-mcp/.github/workflows -name "*.yml" -type f)

for workflow in $workflows; do
    echo "Checking $workflow..."
    
    # Remove GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }} from secrets section in workflow calls
    # This matches the pattern where GITHUB_TOKEN is passed in secrets to workflow-failure-handler
    sed -i '' '/uses:.*workflow-failure-handler\.yml/,/secrets:/{
        /GITHUB_TOKEN: \${{ secrets\.GITHUB_TOKEN }}/d
    }' "$workflow"
    
    echo "Fixed $workflow"
done

echo "Done!"