#!/bin/bash

# Pre-commit Setup Script
# This script installs and configures pre-commit hooks for the project

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔧 Setting up pre-commit hooks...${NC}"

# Check if pre-commit is installed
if ! command -v pre-commit &> /dev/null; then
    echo -e "${YELLOW}⚠️ pre-commit not found. Installing...${NC}"

    # Try different installation methods
    if command -v pip &> /dev/null; then
        pip install pre-commit
    elif command -v pip3 &> /dev/null; then
        pip3 install pre-commit
    elif command -v brew &> /dev/null; then
        brew install pre-commit
    elif command -v apt-get &> /dev/null; then
        sudo apt-get update && sudo apt-get install -y pre-commit
    else
        echo -e "${RED}❌ Could not install pre-commit. Please install manually:${NC}"
        echo "  pip install pre-commit"
        echo "  or visit: https://pre-commit.com/#installation"
        exit 1
    fi
fi

echo -e "${GREEN}✓ pre-commit is available${NC}"

# Install the git hook scripts
echo -e "${BLUE}📦 Installing pre-commit hooks...${NC}"
pre-commit install

# Install commit-msg hook for commit message linting
echo -e "${BLUE}📝 Installing commit-msg hook...${NC}"
pre-commit install --hook-type commit-msg

# Install pre-push hook for additional checks
echo -e "${BLUE}🚀 Installing pre-push hook...${NC}"
pre-commit install --hook-type pre-push

# Update hooks to latest versions
echo -e "${BLUE}🔄 Updating hooks to latest versions...${NC}"
pre-commit autoupdate

# Install additional tools if not present
echo -e "${BLUE}🛠️ Checking additional linting tools...${NC}"

# Check for Node.js tools
if command -v npm &> /dev/null; then
    echo -e "${YELLOW}📦 Installing Node.js linting tools...${NC}"
    npm install -g markdownlint-cli markdown-link-check
else
    echo -e "${YELLOW}⚠️ npm not found. Skipping Node.js tools installation.${NC}"
fi

# Check for Python tools
if command -v pip &> /dev/null; then
    echo -e "${YELLOW}🐍 Installing Python linting tools...${NC}"
    pip install yamllint
elif command -v pip3 &> /dev/null; then
    echo -e "${YELLOW}🐍 Installing Python linting tools...${NC}"
    pip3 install yamllint
else
    echo -e "${YELLOW}⚠️ pip not found. Skipping Python tools installation.${NC}"
fi

# Check for hadolint (Docker linting)
if ! command -v hadolint &> /dev/null; then
    echo -e "${YELLOW}🐳 Installing hadolint...${NC}"
    if command -v brew &> /dev/null; then
        brew install hadolint
    elif command -v apt-get &> /dev/null; then
        wget -O /tmp/hadolint https://github.com/hadolint/hadolint/releases/latest/download/hadolint-Linux-x86_64
        chmod +x /tmp/hadolint
        sudo mv /tmp/hadolint /usr/local/bin/hadolint
    else
        echo -e "${YELLOW}⚠️ Please install hadolint manually from: https://github.com/hadolint/hadolint${NC}"
    fi
fi

# Test the installation
echo -e "${BLUE}🧪 Testing pre-commit installation...${NC}"
if pre-commit run --all-files --show-diff-on-failure; then
    echo -e "${GREEN}✅ Pre-commit hooks are working correctly!${NC}"
else
    echo -e "${YELLOW}⚠️ Some hooks failed. This is normal for the first run.${NC}"
    echo -e "${YELLOW}   Fix the issues and commit again.${NC}"
fi

# Create .pre-commit-config.local.yaml template if it doesn't exist
if [ ! -f ".pre-commit-config.local.yaml" ]; then
    echo -e "${BLUE}📝 Creating local pre-commit config template...${NC}"
    cat > .pre-commit-config.local.yaml << 'EOF'
# Local pre-commit configuration overrides
# This file is ignored by git and can be used for local customizations

# Example: Disable specific hooks locally
# exclude: |
#   (?x)^(
#       path/to/file/to/exclude\.py|
#       another/file\.js
#   )$

# Example: Add local-only hooks
# repos:
# -   repo: local
#     hooks:
#     -   id: local-custom-hook
#         name: Local Custom Hook
#         entry: ./scripts/local-hook.sh
#         language: system
EOF
    echo -e "${GREEN}✓ Created .pre-commit-config.local.yaml template${NC}"
fi

# Add .pre-commit-config.local.yaml to .gitignore if not already there
if ! grep -q ".pre-commit-config.local.yaml" .gitignore 2>/dev/null; then
    echo ".pre-commit-config.local.yaml" >> .gitignore
    echo -e "${GREEN}✓ Added .pre-commit-config.local.yaml to .gitignore${NC}"
fi

echo ""
echo -e "${GREEN}🎉 Pre-commit setup complete!${NC}"
echo ""
echo -e "${BLUE}Usage:${NC}"
echo "  • Hooks will run automatically on commit"
echo "  • Run manually: ${YELLOW}pre-commit run --all-files${NC}"
echo "  • Update hooks: ${YELLOW}pre-commit autoupdate${NC}"
echo "  • Skip hooks: ${YELLOW}git commit --no-verify${NC} (not recommended)"
echo ""
echo -e "${BLUE}Hook stages:${NC}"
echo "  • ${YELLOW}commit${NC}: Runs on every commit (linting, formatting)"
echo "  • ${YELLOW}commit-msg${NC}: Validates commit message format"
echo "  • ${YELLOW}push${NC}: Runs on push (tests, builds)"
echo "  • ${YELLOW}manual${NC}: Run with ${YELLOW}pre-commit run --hook-stage manual${NC}"
echo ""
echo -e "${BLUE}Configuration files:${NC}"
echo "  • ${YELLOW}.pre-commit-config.yaml${NC}: Main configuration (committed)"
echo "  • ${YELLOW}.pre-commit-config.local.yaml${NC}: Local overrides (ignored by git)"
echo ""
echo -e "${GREEN}Happy coding! 🚀${NC}"
