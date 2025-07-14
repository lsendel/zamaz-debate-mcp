#!/bin/bash

echo "This script will remove sensitive data from git history"
echo "WARNING: This will rewrite git history!"
echo ""
echo "Steps to fix:"
echo "1. First, ensure .env is in .gitignore"
echo "2. Remove .env from git tracking"
echo "3. Use git filter-branch or BFG to clean history"
echo ""

# Check if .env is in .gitignore
if ! grep -q "^\.env$" .gitignore; then
    echo "Adding .env to .gitignore..."
    echo ".env" >> .gitignore
    git add .gitignore
    git commit -m "Add .env to .gitignore"
fi

# Remove .env from tracking
echo "Removing .env from git tracking..."
git rm --cached .env 2>/dev/null || true

# Option 1: Interactive rebase (if only a few commits)
echo ""
echo "Option 1: Interactive rebase to edit commit c26cd5c"
echo "Run: git rebase -i c26cd5c^"
echo "Then mark c26cd5c as 'edit', remove .env, and continue"
echo ""

# Option 2: Filter-branch (more thorough)
echo "Option 2: Use filter-branch to remove .env from all history"
echo "Run: git filter-branch --force --index-filter 'git rm --cached --ignore-unmatch .env' --prune-empty --tag-name-filter cat -- --all"
echo ""

# Option 3: BFG (recommended for large repos)
echo "Option 3: Use BFG Repo Cleaner (recommended)"
echo "1. Download BFG: https://rtyley.github.io/bfg-repo-cleaner/"
echo "2. Run: java -jar bfg.jar --delete-files .env"
echo "3. Run: git reflog expire --expire=now --all && git gc --prune=now --aggressive"
echo ""

echo "After cleaning history, you'll need to:"
echo "1. Force push: git push --force-with-lease origin main"
echo "2. Have all team members re-clone or reset their local repos"
echo ""
echo "IMPORTANT: The exposed API keys should be revoked immediately!"