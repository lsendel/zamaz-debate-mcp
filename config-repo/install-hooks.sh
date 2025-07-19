#!/bin/bash

# Script to install Git hooks for the configuration repository

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOOKS_DIR="$SCRIPT_DIR/.githooks"
GIT_HOOKS_DIR="$SCRIPT_DIR/.git/hooks"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "Installing Git hooks..."
echo "======================"

# Check if .git directory exists
if [ ! -d "$SCRIPT_DIR/.git" ]; then
    echo -e "${YELLOW}Warning: Not in a Git repository${NC}"
    echo "Please run this script from the root of the configuration repository"
    exit 1
fi

# Create hooks directory if it doesn't exist
mkdir -p "$GIT_HOOKS_DIR"

# Copy pre-commit hook
if [ -f "$HOOKS_DIR/pre-commit" ]; then
    cp "$HOOKS_DIR/pre-commit" "$GIT_HOOKS_DIR/pre-commit"
    chmod +x "$GIT_HOOKS_DIR/pre-commit"
    echo -e "${GREEN}✓ Installed pre-commit hook${NC}"
else
    echo -e "${YELLOW}! Pre-commit hook not found${NC}"
fi

# Configure Git to use the hooks
git config core.hooksPath "$GIT_HOOKS_DIR"

echo
echo -e "${GREEN}Git hooks installed successfully!${NC}"
echo
echo "The following checks will run before each commit:"
echo "- Sensitive data detection"
echo "- YAML syntax validation"
echo "- Commit message format validation"
echo
echo "To bypass hooks (not recommended), use: git commit --no-verify"