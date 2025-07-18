#!/bin/bash
# Script to fix linting issues across the project

set -e

echo "🔧 Starting comprehensive linting fix process..."
echo "============================================="

# Stats tracking
PYTHON_FIXED=0
SHELL_FIXED=0
TOTAL_ISSUES_BEFORE=0
TOTAL_ISSUES_AFTER=0

# Get initial counts
echo "📊 Getting initial issue counts..."
PYTHON_ISSUES_BEFORE=$(ruff check . --statistics 2>/dev/null | awk '{sum+=$1} END {print sum}' || echo "0")
echo "Python issues before: ""$PYTHON_ISSUES_BEFORE"""

# Fix Python issues
echo ""
echo "🐍 Fixing Python issues..."
echo "-------------------------"

# 1. Auto-fix with Ruff
echo "Running Ruff auto-fix..."
ruff check . --fix --unsafe-fixes > /dev/null 2>&1 || true
PYTHON_FIXED=$(ruff check . --fix --statistics 2>&1 | grep "fixed" | grep -oE '[0-9]+' | head -1 || echo "0")

# 2. Auto-format with Ruff
echo "Running Ruff format..."
ruff format . > /dev/null 2>&1 || true

# 3. Fix specific security issues that are safe to fix
echo "Fixing safe security issues..."

# Fix S311 - Replace random with secrets for crypto
find . -name "*.py" -type f -not -path "./node_modules/*" -not -path "./.git/*" -not -path "./debate-ui/node_modules/*" | while read -r file; do
    if grep -q "import random" """$file""" && grep -q "random\." """$file"""; then
        # Check if it's actually used for crypto (common patterns)
        if grep -qE "(token|password|secret|key|salt)" """$file"""; then
            echo "  Fixing random usage in: ""$file"""
            sed -i.bak 's/import random/import secrets/g' """$file"""
            sed -i.bak 's/random\.choice/secrets.choice/g' """$file"""
            sed -i.bak 's/random\.randint/secrets.randbelow/g' """$file"""
            rm -f "${file}.bak"
        fi
    fi
done

# Fix S113 - Add timeout to requests
find . -name "*.py" -type f | while read -r file; do
    if grep -q "requests\." """$file"""; then
        if ! grep -q "timeout=" """$file"""; then
            echo "  Adding timeouts to requests in: ""$file"""
            sed -i.bak 's/requests\.get(\([^)]*\))/requests.get(\1, timeout=30)/g' """$file"""
            sed -i.bak 's/requests\.post(\([^)]*\))/requests.post(\1, timeout=30)/g' """$file"""
            rm -f "${file}.bak"
        fi
    fi
done

# Get Python issues after fixes
PYTHON_ISSUES_AFTER=$(ruff check . --statistics 2>/dev/null | awk '{sum+=$1} END {print sum}' || echo "0")
echo "Python issues after: ""$PYTHON_ISSUES_AFTER"""
echo "Fixed: $((PYTHON_ISSUES_BEFORE - PYTHON_ISSUES_AFTER)) issues"

# Fix Shell Script issues
echo ""
echo "🐚 Fixing Shell Script issues..."
echo "-------------------------------"

# Count shell issues before
SHELL_ISSUES_BEFORE=0
find . -name "*.sh" -type f -not -path "./node_modules/*" -not -path "./.git/*" | while read -r file; do
    count=$(shellcheck """$file""" 2>/dev/null | wc -l || echo "0")
    SHELL_ISSUES_BEFORE=$((SHELL_ISSUES_BEFORE + count))
done

# Common Shell fixes
find . -name "*.sh" -type f -not -path "./node_modules/*" -not -path "./.git/*" | while read -r file; do
    echo "  Fixing: ""$file"""
    
    # Add shebang if missing (SC2148)
    if ! head -1 """$file""" | grep -q "^#!"; then
        echo "#!/bin/bash" | cat - """$file""" > temp && mv temp """$file"""
    fi
    
    # Quote variables (SC2086)
    # This is a simplified fix - manual review recommended
    sed -i.bak 's/\$\([A-Za-z_][A-Za-z0-9_]*\)\([^A-Za-z0-9_]\)/"$\1"\2/g' """$file"""
    
    # Fix command substitution - use $() instead of backticks
    sed -i.bak 's/$(\([^)]*\)$(...)/$(\1)/g' """$file"""
    
    # Add quotes to test commands
    sed -i.bak 's/\[ \$\([^ ]*\) /[ "$\1" /g' """$file"""
    sed -i.bak 's/ \$\([^ ]*\) \]/ "$\1" ]/g' """$file"""
    
    rm -f "${file}.bak"
done

# TypeScript/JavaScript fixes
echo ""
echo "📦 Fixing TypeScript/JavaScript issues..."
echo "----------------------------------------"

cd debate-ui
if [ -f "package.json" ]; then
    # Run ESLint fix
    echo "Running ESLint auto-fix..."
    npm run lint:fix 2>/dev/null || npx eslint src --ext .ts,.tsx,.js,.jsx --fix || true
    
    # Run Prettier
    echo "Running Prettier..."
    npm run format 2>/dev/null || npx prettier --write src/**/*.{ts,tsx,js,jsx,json,css,md} || true
fi
cd ..

# Java fixes
echo ""
echo "☕ Checking Java issues..."
echo "-------------------------"

# Java linters are typically run through Maven
if [ -f "pom.xml" ]; then
    echo "Java linting should be run via Maven:"
    echo "  mvn checkstyle:check"
    echo "  mvn spotbugs:check"
    echo "  mvn pmd:check"
fi

# Final report
echo ""
echo "📊 Linting Fix Summary"
echo "====================="

# Get final counts
PYTHON_ISSUES_FINAL=$(ruff check . --statistics 2>/dev/null | awk '{sum+=$1} END {print sum}' || echo "0")
SHELL_SCRIPTS=$(find . -name "*.sh" -type f -not -path "./node_modules/*" | wc -l)

echo "Python:"
echo "  Before: ""$PYTHON_ISSUES_BEFORE"" issues"
echo "  After:  ""$PYTHON_ISSUES_FINAL"" issues"
echo "  Fixed:  $((PYTHON_ISSUES_BEFORE - PYTHON_ISSUES_FINAL)) issues"
echo ""
echo "Shell Scripts:"
echo "  Total files: ""$SHELL_SCRIPTS"""
echo "  Note: Run 'shellcheck' to verify fixes"
echo ""

# Remaining issues that need manual attention
echo "⚠️  Issues requiring manual attention:"
echo "------------------------------------"

# Security issues
SECURITY_ISSUES=$(ruff check . --select S --statistics 2>/dev/null | awk '{sum+=$1} END {print sum}' || echo "0")
if [ """$SECURITY_ISSUES""" -gt 0 ]; then
    echo "Security issues remaining: ""$SECURITY_ISSUES"""
    echo "  - S311: Review random usage for cryptographic purposes"
    echo "  - S603: Review subprocess calls with shell=True"
    echo "  - S608: Review SQL query construction"
    echo "  - S106: Check for hardcoded passwords"
fi

echo ""
echo "🔍 Next steps:"
echo "1. Review remaining security issues manually"
echo "2. Run 'shellcheck' on all shell scripts"
echo "3. Run 'mvn clean verify' for Java"
echo "4. Commit the fixes"
echo ""
echo "✅ Automated fixing complete!"