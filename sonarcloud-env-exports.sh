#!/bin/bash

# SonarCloud environment variables for zamaz-debate-mcp
# Add these lines to your ~/.zshrc file:

export SONAR_URL="https://sonarcloud.io"
export SONAR_PROJECT_KEY="lsendel_zamaz-debate-mcp"
export SONAR_ORGANIZATION="lsendel"
export SONAR_BRANCH="main"
export REPORT_AUTHOR="Zamaz Team"

# Note: You'll need to add your SonarCloud token
# Get it from: https://sonarcloud.io/account/security
# export SONAR_TOKEN="your-sonarcloud-token-here"

echo "SonarCloud environment variables:"
echo "SONAR_URL=$SONAR_URL"
echo "SONAR_PROJECT_KEY=$SONAR_PROJECT_KEY"
echo "SONAR_ORGANIZATION=$SONAR_ORGANIZATION"
echo "SONAR_BRANCH=$SONAR_BRANCH"
echo "SONAR_TOKEN=${SONAR_TOKEN:+(set)}"