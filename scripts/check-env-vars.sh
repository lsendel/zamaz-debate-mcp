#!/bin/bash
# Check for hardcoded secrets in Java/config files

set -e

echo "🔍 Checking for hardcoded secrets..."

# Check for common hardcoded values
HARDCODED_PATTERNS=(
    "password.*=.*[^$]"
    "secret.*=.*[^$]"
    "token.*=.*[^$]"
    "key.*=.*[^$]"
    "jdbc:postgresql://.*:[0-9]*/.*"
    "postgres.*password.*postgres"
    "sk-[a-zA-Z0-9]{20,}"
    "xai-[a-zA-Z0-9]{20,}"
    "AIzaSy[a-zA-Z0-9]{33}"
)

FOUND_ISSUES=0

for pattern in "${HARDCODED_PATTERNS[@]}"; do
    if grep -r -E "$pattern" --include="*.java" --include="*.yml" --include="*.yaml" --include="*.properties" . | grep -v ".env" | grep -v "test" | grep -v "example"; then
        echo "❌ Found potential hardcoded secret: $pattern"
        FOUND_ISSUES=1
    fi
done

# Check for specific prohibited patterns
if grep -r "password.*postgres" --include="*.java" --include="*.yml" --include="*.yaml" --include="*.properties" . | grep -v ".env" | grep -v "test"; then
    echo "❌ Found hardcoded postgres password"
    FOUND_ISSUES=1
fi

if grep -r "mcp_pass" --include="*.java" --include="*.yml" --include="*.yaml" --include="*.properties" . | grep -v ".env" | grep -v "test"; then
    echo "❌ Found hardcoded mcp_pass"
    FOUND_ISSUES=1
fi

if [ $FOUND_ISSUES -eq 1 ]; then
    echo ""
    echo "🚨 SECURITY ISSUE: Hardcoded secrets found!"
    echo "Please use environment variables instead. Example:"
    echo "  password: \${DB_PASSWORD:changeme}"
    echo "  api_key: \${API_KEY:}"
    echo ""
    echo "Add secrets to .env file and reference them with \${VAR_NAME:default}"
    exit 1
fi

echo "✅ No hardcoded secrets found"