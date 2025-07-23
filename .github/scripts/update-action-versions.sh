#!/bin/bash
# Script to update deprecated GitHub Action versions

set -e

WORKFLOWS_DIR=".github/workflows"
UPDATED_COUNT=0

echo "🔄 Updating deprecated GitHub Action versions..."
echo "=============================================="

# Define the replacements
declare -A ACTION_UPDATES=(
    ["actions/upload-artifact@v1"]="actions/upload-artifact@v4"
    ["actions/upload-artifact@v2"]="actions/upload-artifact@v4"
    ["actions/upload-artifact@v3"]="actions/upload-artifact@v4"
    ["actions/download-artifact@v1"]="actions/download-artifact@v4"
    ["actions/download-artifact@v2"]="actions/download-artifact@v4"
    ["actions/download-artifact@v3"]="actions/download-artifact@v4"
    ["actions/setup-java@v1"]="actions/setup-java@v4"
    ["actions/setup-java@v2"]="actions/setup-java@v4"
    ["actions/setup-java@v3"]="actions/setup-java@v4"
    ["actions/setup-node@v1"]="actions/setup-node@v4"
    ["actions/setup-node@v2"]="actions/setup-node@v4"
    ["actions/setup-node@v3"]="actions/setup-node@v4"
    ["actions/github-script@v1"]="actions/github-script@v7"
    ["actions/github-script@v2"]="actions/github-script@v7"
    ["actions/github-script@v3"]="actions/github-script@v7"
    ["actions/github-script@v4"]="actions/github-script@v7"
    ["actions/github-script@v5"]="actions/github-script@v7"
    ["actions/github-script@v6"]="actions/github-script@v7"
    ["actions/cache@v1"]="actions/cache@v4"
    ["actions/cache@v2"]="actions/cache@v4"
    ["actions/cache@v3"]="actions/cache@v4"
    ["actions/checkout@v1"]="actions/checkout@v4"
    ["actions/checkout@v2"]="actions/checkout@v4"
    ["actions/checkout@v3"]="actions/checkout@v4"
)

# Function to update actions in a file
update_file() {
    local file=$1
    local filename=$(basename "$file")
    local changes_made=false
    
    echo -n "Checking $filename... "
    
    # Create a temporary file
    local temp_file="${file}.tmp"
    cp "$file" "$temp_file"
    
    # Apply all replacements
    for old_action in "${!ACTION_UPDATES[@]}"; do
        new_action="${ACTION_UPDATES[$old_action]}"
        if grep -q "$old_action" "$temp_file"; then
            sed -i.bak "s|$old_action|$new_action|g" "$temp_file"
            changes_made=true
            echo -n "✓ "
        fi
    done
    
    # If changes were made, update the file
    if [ "$changes_made" = true ]; then
        mv "$temp_file" "$file"
        rm -f "${temp_file}.bak"
        echo "Updated!"
        ((UPDATED_COUNT++))
    else
        rm -f "$temp_file"
        echo "No updates needed"
    fi
}

# Update all workflow files
for workflow_file in "$WORKFLOWS_DIR"/*.yml "$WORKFLOWS_DIR"/*.yaml; do
    if [ -f "$workflow_file" ]; then
        update_file "$workflow_file"
    fi
done

echo ""
echo "=============================================="
echo "✅ Update complete!"
echo "   Files updated: $UPDATED_COUNT"
echo ""
echo "Summary of updates:"
echo "  - upload-artifact: v1/v2/v3 → v4"
echo "  - download-artifact: v1/v2/v3 → v4"
echo "  - setup-java: v1/v2/v3 → v4"
echo "  - setup-node: v1/v2/v3 → v4"
echo "  - github-script: v1-v6 → v7"
echo "  - cache: v1/v2/v3 → v4"
echo "  - checkout: v1/v2/v3 → v4"
echo ""
echo "⚠️  Note: Review the changes to ensure compatibility with your workflows"