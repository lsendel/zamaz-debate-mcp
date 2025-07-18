#!/bin/bash

# Script to install git hooks
# This script configures git to use the hooks in the .githooks directory

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Installing git hooks...${NC}"

# Configure git to use the hooks in the .githooks directory
git config core.hooksPath .githooks

# Make sure the hooks are executable
chmod +x .githooks/pre-commit

# Check if markdownlint is installed
if ! command -v markdownlint &> /dev/null; then
    echo -e "${YELLOW}Warning: markdownlint is not installed.${NC}"
    echo -e "${YELLOW}For better documentation quality checks, install markdownlint:${NC}"
    echo -e "${GREEN}npm install -g markdownlint-cli${NC}"
fi

echo -e "${GREEN}Git hooks installed successfully!${NC}"
echo -e "${YELLOW}The pre-commit hook will now check for:${NC}"
echo -e "  - Build validation"
echo -e "  - Personal notes in documentation files"
echo -e "  - Markdown linting (if markdownlint is installed)"

exit 0