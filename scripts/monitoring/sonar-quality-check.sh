#!/bin/bash
# SonarQube quality gate check

set -e

echo "🔍 Running SonarQube quality check..."

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)
fi

# Check if SONAR_TOKEN is set
if [ -z """"$SONAR_TOKEN"""" ]; then
    echo "⚠️  SONAR_TOKEN not set, skipping quality check"
    exit 0
fi

# Set SonarCloud configuration
export SONAR_URL="https://sonarcloud.io"
export SONAR_PROJECT_KEY="lsendel_zamaz-debate-mcp"
export SONAR_ORGANIZATION="lsendel"

# Run SonarQube quality gate check
echo "📊 Checking SonarCloud quality gate..."

# Get quality gate status
QUALITY_GATE_STATUS=$(curl -s -H "Authorization: Bearer """$SONAR_TOKEN"""" \
    """"$SONAR_URL"""/api/qualitygates/project_status?projectKey="""$SONAR_PROJECT_KEY"""" | \
    jq -r '.projectStatus.status')

echo "Quality Gate Status: """$QUALITY_GATE_STATUS""""

if [ """"$QUALITY_GATE_STATUS"""" != "OK" ]; then
    echo "❌ SonarQube Quality Gate failed!"
    echo "Please fix issues at: """$SONAR_URL"""/project/overview?id="""$SONAR_PROJECT_KEY""""
    exit 1
fi

echo "✅ SonarQube Quality Gate passed!"