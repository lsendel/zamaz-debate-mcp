#!/bin/bash

# SonarCloud environment variables for zamaz-debate-mcp
# Loads configuration from .env file

# Load environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)
fi

export SONAR_URL="https://sonarcloud.io"
export SONAR_PROJECT_KEY="lsendel_zamaz-debate-mcp"
export SONAR_ORGANIZATION="lsendel"
export SONAR_BRANCH="main"
export REPORT_AUTHOR="Zamaz Team"

# SONAR_TOKEN is now loaded from .env file

echo "SonarCloud environment variables:"
echo "SONAR_URL=$SONAR_URL"
echo "SONAR_PROJECT_KEY=$SONAR_PROJECT_KEY"
echo "SONAR_ORGANIZATION=$SONAR_ORGANIZATION"
echo "SONAR_BRANCH=$SONAR_BRANCH"
echo "SONAR_TOKEN=${SONAR_TOKEN:+(set)}"