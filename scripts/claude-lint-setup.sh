#!/bin/bash
# Setup script for claude-lint

set -e

echo "🚀 Setting up claude-lint for easy AI code validation..."

# Get the directory of this script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CLAUDE_LINT="""$SCRIPT_DIR""/claude-lint"

# Make sure claude-lint is executable
chmod +x """$CLAUDE_LINT"""

# Create symlink in /usr/local/bin if it doesn't exist
if [ ! -L /usr/local/bin/claude-lint ]; then
    echo "Creating symlink in /usr/local/bin..."
    sudo ln -sf """$CLAUDE_LINT""" /usr/local/bin/claude-lint
    echo "✅ Symlink created"
else
    echo "✅ Symlink already exists"
fi

# Add aliases to shell configuration
add_aliases() {
    local shell_config="$1"
    if [ -f """$shell_config""" ]; then
        if ! grep -q "claude-lint aliases" """$shell_config"""; then
            echo "" >> """$shell_config"""
            echo "# claude-lint aliases for AI code validation" >> """$shell_config"""
            echo "alias cl='claude-lint'" >> """$shell_config"""
            echo "alias clpy='claude-lint --stdin --lang python'" >> """$shell_config"""
            echo "alias cljs='claude-lint --stdin --lang javascript'" >> """$shell_config"""
            echo "alias clsh='claude-lint --stdin --lang shell'" >> """$shell_config"""
            echo "alias clf='claude-lint --fix'" >> """$shell_config"""
            echo "alias clq='claude-lint --quick'" >> """$shell_config"""
            echo "" >> """$shell_config"""
            echo "# Quick validation functions" >> """$shell_config"""
            echo 'clv() { echo "$1" | claude-lint --stdin --auto; }' >> """$shell_config"""
            echo 'clvf() { claude-lint "$1" --fix --format human; }' >> """$shell_config"""
            echo "" >> """$shell_config"""
            echo "✅ Added aliases to ""$shell_config"""
        else
            echo "✅ Aliases already exist in ""$shell_config"""
        fi
    fi
}

# Add to various shell configs
add_aliases """$HOME""/.bashrc"
add_aliases """$HOME""/.zshrc"

# Check available linters
echo ""
echo "🔍 Checking available linters..."
claude-lint --available

# Create example configuration
CONFIG_DIR="""$HOME""/.config/claude-lint"
mkdir -p """$CONFIG_DIR"""

cat > """$CONFIG_DIR""/config.yml" << 'EOF'
# Claude Lint Configuration
defaults:
  format: human
  auto_detect: true
  
quick_mode:
  checks:
    - security
    - syntax-errors
  
languages:
  python:
    prefer_ruff: true
    autofix_categories:
      - formatting
      - imports
      - quotes
  
  shell:
    severity: warning
    exclude_codes:
      - SC1091  # Source not found
  
  javascript:
    use_prettier: true
    
performance:
  cache: true
  timeout: 5
EOF

echo "✅ Created configuration at ""$CONFIG_DIR""/config.yml"

# Test the installation
echo ""
echo "🧪 Testing claude-lint installation..."

# Test Python
echo 'print("Hello from Claude!")' | claude-lint --stdin --lang python
echo ""

# Test shell
echo 'echo ""$unquoted_var""' | claude-lint --stdin --lang shell
echo ""

echo "✅ Setup complete!"
echo ""
echo "Usage examples:"
echo "  cl file.py                    # Lint a file"
echo "  echo 'code' | cl -           # Lint from stdin"
echo "  clpy 'print(\"test\")'        # Quick Python check"
echo "  clf file.js                  # Fix issues in file"
echo "  cl --available               # Show available linters"
echo ""
echo "For Claude/AI usage:"
echo "  Always validate code before suggesting it"
echo "  Use 'cl -' for quick stdin validation"
echo "  Check security issues with --quick mode"
echo ""
echo "🎉 claude-lint is ready for AI-powered code validation!"