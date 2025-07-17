#!/bin/bash
# Setup pre-commit hooks for the project

set -e

echo "🔧 Setting up pre-commit hooks..."

# Install pre-commit if not installed
if ! command -v pre-commit &> /dev/null; then
    echo "📦 Installing pre-commit..."
    pip install pre-commit
fi

# Install the git hook scripts
echo "🪝 Installing git hooks..."
pre-commit install

# Install commit message hook
echo "💬 Installing commit message hook..."
pre-commit install --hook-type commit-msg

# Install push hook
echo "🚀 Installing push hook..."
pre-commit install --hook-type pre-push

# Update hooks to latest versions
echo "🔄 Updating hooks..."
pre-commit autoupdate

echo "✅ Pre-commit hooks setup complete!"
echo ""
echo "To run hooks on all files:"
echo "  pre-commit run --all-files"
echo ""
echo "To skip hooks for a commit:"
echo "  git commit --no-verify"
echo ""
echo "Hooks will now run automatically on:"
echo "  • Before each commit"
echo "  • Before each push"
echo "  • On commit messages"