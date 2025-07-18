#!/bin/bash

# Script to fix BLOCKER security issues identified by SonarCloud

echo "🚨 Fixing BLOCKER Security Issues..."
echo ""

# Fix 1: Remove SonarCloud token from zshrc-sonarcloud-config.sh
echo "1️⃣ Fixing exposed SonarCloud token in zshrc-sonarcloud-config.sh..."
if [ -f "zshrc-sonarcloud-config.sh" ]; then
    # Replace the hardcoded token with a placeholder
    sed -i.bak 's/export SONAR_TOKEN="[^"]*"/export SONAR_TOKEN="${SONAR_TOKEN:-}"/' zshrc-sonarcloud-config.sh
    echo "   ✅ Removed hardcoded token from zshrc-sonarcloud-config.sh"
    echo "   📝 Note: You'll need to set SONAR_TOKEN in your environment or .env file"
else
    echo "   ⚠️  File not found: zshrc-sonarcloud-config.sh"
fi

echo ""

# Fix 2: Update mcp-debate application.properties
echo "2️⃣ Fixing exposed password in mcp-debate/src/main/resources/application.properties..."
PROPERTIES_FILE="mcp-debate/src/main/resources/application.properties"
if [ -f """$PROPERTIES_FILE""" ]; then
    # Check if the file has a hardcoded password
    if grep -q "spring.datasource.password=" """$PROPERTIES_FILE"""; then
        # Replace with environment variable
        sed -i.bak 's/spring.datasource.password=.*/spring.datasource.password=${DB_PASSWORD:changeme}/' """$PROPERTIES_FILE"""
        echo "   ✅ Updated application.properties to use environment variable"
    else
        echo "   ℹ️  Password line not found in application.properties"
    fi
else
    echo "   ⚠️  File not found: ""$PROPERTIES_FILE"""
fi

echo ""
echo "3️⃣ Next Steps:"
echo "   1. Review the changes made by this script"
echo "   2. Set your environment variables:"
echo "      export SONAR_TOKEN='your-new-token'"
echo "      export DB_PASSWORD='your-database-password'"
echo "   3. Regenerate your SonarCloud token at:"
echo "      https://sonarcloud.io/account/security"
echo "   4. Commit and push these security fixes"
echo ""
echo "✅ Script completed. Please review the changes before committing."