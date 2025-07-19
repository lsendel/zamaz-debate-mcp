#!/bin/bash

# Security scanning script for configuration files
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
CONFIG_REPO="${CONFIG_REPO:-$PROJECT_ROOT/config-repo}"
SCAN_DIRS="${SCAN_DIRS:-$CONFIG_REPO $PROJECT_ROOT/mcp-*/src/main/resources}"
LOG_FILE="$PROJECT_ROOT/security-scan.log"

# Sensitive patterns to search for
SENSITIVE_PATTERNS=(
    # Passwords
    "password[:=][[:space:]]*[^$\{].*[^}]$"
    "pwd[:=][[:space:]]*[^$\{].*[^}]$"
    "pass[:=][[:space:]]*[^$\{].*[^}]$"
    
    # Secrets and keys
    "secret[:=][[:space:]]*[^$\{].*[^}]$"
    "key[:=][[:space:]]*[^$\{].*[^}]$"
    "apikey[:=][[:space:]]*[^$\{].*[^}]$"
    "api[_-]key[:=][[:space:]]*[^$\{].*[^}]$"
    "token[:=][[:space:]]*[^$\{].*[^}]$"
    "auth[_-]token[:=][[:space:]]*[^$\{].*[^}]$"
    
    # Credentials
    "credential[:=][[:space:]]*[^$\{].*[^}]$"
    "username[:=][[:space:]]*[^$\{].*[^}]$"
    
    # Private keys
    "private[_-]key[:=][[:space:]]*[^$\{].*[^}]$"
    "BEGIN PRIVATE KEY"
    "BEGIN RSA PRIVATE KEY"
    "BEGIN EC PRIVATE KEY"
    
    # Connection strings
    "jdbc:[^:]+://[^@]+:[^@]+@"
    "mongodb://[^:]+:[^@]+@"
    "redis://[^:]+:[^@]+@"
)

# Files to exclude from scanning
EXCLUDE_PATTERNS=(
    "*.log"
    "*.md"
    "*.txt"
    "target/*"
    "build/*"
    ".git/*"
    "test/*"
    "*test*"
)

# Initialize log
echo "Security Scan Report - $(date)" > "$LOG_FILE"
echo "================================" >> "$LOG_FILE"

# Function to check if file should be excluded
should_exclude() {
    local file=$1
    for pattern in "${EXCLUDE_PATTERNS[@]}"; do
        if [[ $file == *$pattern* ]]; then
            return 0
        fi
    done
    return 1
}

# Function to scan a file
scan_file() {
    local file=$1
    local found_issues=0
    
    # Skip if file should be excluded
    if should_exclude "$file"; then
        return 0
    fi
    
    # Check each sensitive pattern
    for pattern in "${SENSITIVE_PATTERNS[@]}"; do
        # Use grep to find matches, excluding encrypted values
        matches=$(grep -inE "$pattern" "$file" 2>/dev/null | grep -v "{cipher}" | grep -v "change-me" | grep -v "example" | grep -v "placeholder" || true)
        
        if [ -n "$matches" ]; then
            if [ $found_issues -eq 0 ]; then
                echo -e "${RED}Issues found in: $file${NC}"
                echo "File: $file" >> "$LOG_FILE"
            fi
            echo "$matches" | while IFS= read -r line; do
                echo -e "  ${YELLOW}$line${NC}"
                echo "  $line" >> "$LOG_FILE"
            done
            found_issues=1
        fi
    done
    
    return $found_issues
}

# Function to check for weak encryption
check_encryption() {
    local file=$1
    
    # Check for weak encryption indicators
    if grep -q "DES\|MD5\|SHA1" "$file" 2>/dev/null; then
        echo -e "${YELLOW}Warning: Weak encryption algorithm detected in $file${NC}"
        echo "Weak encryption in: $file" >> "$LOG_FILE"
    fi
}

# Function to check file permissions
check_permissions() {
    local file=$1
    local perms=$(stat -c "%a" "$file" 2>/dev/null || stat -f "%Lp" "$file" 2>/dev/null)
    
    if [ "$perms" -gt "644" ]; then
        echo -e "${YELLOW}Warning: Excessive permissions ($perms) on $file${NC}"
        echo "Permission issue: $file has permissions $perms" >> "$LOG_FILE"
    fi
}

# Main scanning logic
echo -e "${GREEN}Starting security scan...${NC}"
echo ""

total_files=0
files_with_issues=0

# Scan each directory
for dir in $SCAN_DIRS; do
    if [ -d "$dir" ]; then
        echo "Scanning directory: $dir"
        
        # Find all YAML and properties files
        while IFS= read -r -d '' file; do
            ((total_files++))
            
            if scan_file "$file"; then
                ((files_with_issues++))
            fi
            
            check_encryption "$file"
            check_permissions "$file"
            
        done < <(find "$dir" -type f \( -name "*.yml" -o -name "*.yaml" -o -name "*.properties" \) -print0)
    fi
done

echo ""
echo "================================"
echo -e "${GREEN}Scan Summary:${NC}"
echo "Total files scanned: $total_files"
echo "Files with issues: $files_with_issues"
echo ""

# Additional checks
echo -e "${GREEN}Additional Security Checks:${NC}"

# Check for Git configuration
if [ -d "$CONFIG_REPO/.git" ]; then
    # Check for committed secrets in Git history
    echo -n "Checking Git history for secrets... "
    cd "$CONFIG_REPO"
    
    # Use git-secrets if available
    if command -v git-secrets &> /dev/null; then
        if git secrets --scan-history 2>/dev/null; then
            echo -e "${GREEN}OK${NC}"
        else
            echo -e "${RED}Found secrets in Git history!${NC}"
            echo "Git history contains secrets" >> "$LOG_FILE"
        fi
    else
        echo -e "${YELLOW}git-secrets not installed${NC}"
    fi
fi

# Check for environment variable usage
echo -n "Checking for proper environment variable usage... "
env_vars=$(grep -r '\${[^}]*}' $SCAN_DIRS 2>/dev/null | wc -l)
echo -e "${GREEN}Found $env_vars environment variable references${NC}"

# Final report
if [ $files_with_issues -gt 0 ]; then
    echo ""
    echo -e "${RED}Security scan found issues!${NC}"
    echo "See $LOG_FILE for details"
    exit 1
else
    echo ""
    echo -e "${GREEN}Security scan completed successfully!${NC}"
    echo "No security issues found" >> "$LOG_FILE"
fi