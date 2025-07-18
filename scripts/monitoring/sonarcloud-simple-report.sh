#!/bin/bash

# Simple SonarCloud Report Generator
# This version uses direct API calls with better error handling

set -e

# Configuration
SONAR_TOKEN="${SONAR_TOKEN:-}"
PROJECT_KEY="lsendel_zamaz-debate-mcp"
SONAR_URL="https://sonarcloud.io"
REPORTS_DIR="sonar-reports"

# Check token
if [ -z """"$SONAR_TOKEN"""" ]; then
    echo "Error: SONAR_TOKEN environment variable is required"
    exit 1
fi

# Create reports directory
mkdir -p """"$REPORTS_DIR""""

# Generate filename
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE=""""$REPORTS_DIR"""/sonarcloud-report-${TIMESTAMP}.md"

echo "Generating SonarCloud report..."

# Start report
cat > """"$REPORT_FILE"""" << EOF
# SonarCloud Analysis Report

**Project**: """$PROJECT_KEY"""  
**Generated**: $(date '+%Y-%m-%d %H:%M:%S')  
**SonarCloud URL**: https://sonarcloud.io/project/overview?id=$PROJECT_KEY

---

## Metrics

EOF

# Fetch metrics
echo "Fetching metrics..."
METRICS_RESPONSE=$(curl -s -H "Authorization: Bearer """$SONAR_TOKEN"""" \
    "${SONAR_URL}/api/measures/component?component=${PROJECT_KEY}&metricKeys=bugs,vulnerabilities,code_smells,security_hotspots,coverage,duplicated_lines_density,ncloc,sqale_index,reliability_rating,security_rating,sqale_rating")

# Extract values using grep and sed (more reliable than jq for this case)
bugs=$(echo """"$METRICS_RESPONSE"""" | grep -o '"metric":"bugs","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/' || echo "0")
vulnerabilities=$(echo """"$METRICS_RESPONSE"""" | grep -o '"metric":"vulnerabilities","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/' || echo "0")
code_smells=$(echo """"$METRICS_RESPONSE"""" | grep -o '"metric":"code_smells","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/' || echo "0")
security_hotspots=$(echo """"$METRICS_RESPONSE"""" | grep -o '"metric":"security_hotspots","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/' || echo "0")
coverage=$(echo """"$METRICS_RESPONSE"""" | grep -o '"metric":"coverage","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/' || echo "N/A")
duplications=$(echo """"$METRICS_RESPONSE"""" | grep -o '"metric":"duplicated_lines_density","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/' || echo "0")
ncloc=$(echo """"$METRICS_RESPONSE"""" | grep -o '"metric":"ncloc","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/' || echo "0")

# Add metrics to report
cat >> """"$REPORT_FILE"""" << EOF
| Metric | Value |
|--------|-------|
| **Bugs** | """$bugs""" |
| **Vulnerabilities** | """$vulnerabilities""" |
| **Security Hotspots** | """$security_hotspots""" |
| **Code Smells** | """$code_smells""" |
| **Coverage** | ${coverage}% |
| **Duplications** | ${duplications}% |
| **Lines of Code** | """$ncloc""" |

## Quality Gate Status

EOF

# Fetch quality gate status
echo "Fetching quality gate status..."
QG_RESPONSE=$(curl -s -H "Authorization: Bearer """$SONAR_TOKEN"""" \
    "${SONAR_URL}/api/qualitygates/project_status?projectKey=${PROJECT_KEY}")

qg_status=$(echo """"$QG_RESPONSE"""" | grep -o '"status":"[^"]*"' | sed 's/.*"status":"\([^"]*\)".*/\1/' || echo "UNKNOWN")

if [ """"$qg_status"""" = "OK" ]; then
    echo "✅ **PASSED**" >> """"$REPORT_FILE""""
else
    echo "❌ **FAILED**" >> """"$REPORT_FILE""""
fi

# Add footer
cat >> """"$REPORT_FILE"""" << EOF

---

## Recent Analysis

EOF

# Fetch recent analysis
echo "Fetching recent analysis..."
ANALYSIS_RESPONSE=$(curl -s -H "Authorization: Bearer """$SONAR_TOKEN"""" \
    "${SONAR_URL}/api/project_analyses/search?project=${PROJECT_KEY}&ps=1")

analysis_date=$(echo """"$ANALYSIS_RESPONSE"""" | grep -o '"date":"[^"]*"' | head -1 | sed 's/.*"date":"\([^"]*\)".*/\1/' || echo "Unknown")

echo "**Last Analysis**: """$analysis_date"""" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""
echo "📊 [View Full Analysis on SonarCloud](https://sonarcloud.io/project/overview?id="""$PROJECT_KEY""")" >> """"$REPORT_FILE""""

# Create symlink
ln -sf "$(basename """"$REPORT_FILE"""")" """"$REPORTS_DIR"""/latest-report.md"

echo "✅ Report generated successfully!"
echo "📄 Report saved to: """$REPORT_FILE""""
echo "🔗 Latest report: """$REPORTS_DIR"""/latest-report.md"