#!/bin/bash

# This file contains the exact lines to add to your ~/.zshrc file
# Copy and paste these lines into your ~/.zshrc

# SonarCloud Configuration for zamaz-debate-mcp
export SONAR_URL="https://sonarcloud.io"
export SONAR_PROJECT_KEY="lsendel_zamaz-debate-mcp"
export SONAR_ORGANIZATION="lsendel"
export SONAR_BRANCH="main"
export REPORT_AUTHOR="Zamaz Team"
export SONAR_TOKEN="${SONAR_TOKEN:-}"

# Alias for quick report generation
alias sonar-report="cd /Users/lsendel/IdeaProjects/zamaz-debate-mcp && ./scripts/generate-sonarcloud-report.sh"