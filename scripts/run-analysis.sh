#!/bin/bash

# GitHub Actions Workflow Failure Analysis Script
# This script sets up and runs the Playwright analysis

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== GitHub Actions Workflow Failure Analysis ==="
echo "Setting up environment..."

# Check if Node.js is available
if ! command -v node &> /dev/null; then
    echo "Error: Node.js is required but not installed."
    echo "Please install Node.js and try again."
    exit 1
fi

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Install Playwright browsers if needed
if [ ! -d "node_modules/@playwright/test" ]; then
    echo "Installing Playwright browsers..."
    npx playwright install chromium
fi

# Create output directory
mkdir -p github-actions-analysis

echo ""
echo "Starting analysis..."
echo "This will:"
echo "1. Navigate to https://github.com/lsendel/zamaz-debate-mcp/actions"
echo "2. Examine all failing workflows"
echo "3. Check for workflow health monitor runs"
echo "4. Analyze issue creation attempts"
echo "5. Generate a comprehensive report"
echo ""
echo "The browser will open and you'll see the analysis in progress."
echo "Please ensure you're logged into GitHub in your default browser."
echo ""

# Run the analysis
node analyze-github-actions-failures.js

echo ""
echo "=== Analysis Complete ==="
echo "Results saved to: $SCRIPT_DIR/github-actions-analysis/"
echo ""
echo "Files created:"
echo "- workflow-failures-analysis.json (raw data)"
echo "- workflow-failures-report.md (formatted report)"
echo "- Screenshots of all analyzed workflows"
echo ""
echo "Review the report file for detailed findings and recommendations."