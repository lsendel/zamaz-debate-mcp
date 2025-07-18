#!/bin/bash

# Full SonarCloud Report Generator
# Fetches comprehensive data from SonarCloud API and generates detailed markdown report

set -e

# Configuration
SONAR_TOKEN="${SONAR_TOKEN:-}"
PROJECT_KEY="lsendel_zamaz-debate-mcp"
SONAR_URL="https://sonarcloud.io"
REPORTS_DIR="sonar-reports"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check token
if [ -z """"$SONAR_TOKEN"""" ]; then
    echo -e "${RED}Error: SONAR_TOKEN environment variable is required${NC}"
    exit 1
fi

# Create reports directory
mkdir -p """"$REPORTS_DIR""""

# Generate filename
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE=""""$REPORTS_DIR"""/full-sonarcloud-report-${TIMESTAMP}.md"

echo "🔄 Generating comprehensive SonarCloud report..."

# API call helper
api_call() {
    curl -s -H "Authorization: Bearer """$SONAR_TOKEN"""" "${SONAR_URL}/api/$1"
}

# Fetch all data
echo "📊 Fetching project data..."
PROJECT_INFO=$(api_call "components/show?component=${PROJECT_KEY}")
MEASURES=$(api_call "measures/component?component=${PROJECT_KEY}&metricKeys=bugs,vulnerabilities,code_smells,security_hotspots,security_hotspots_reviewed,coverage,duplicated_lines_density,ncloc,sqale_index,reliability_rating,security_rating,sqale_rating,alert_status,duplicated_blocks,duplicated_lines,complexity,cognitive_complexity,test_errors,test_failures,tests,skipped_tests")
QUALITY_GATE=$(api_call "qualitygates/project_status?projectKey=${PROJECT_KEY}")
ISSUES=$(api_call "issues/search?componentKeys=${PROJECT_KEY}&ps=1&facets=severities,types,resolutions,rules")
HOTSPOTS=$(api_call "hotspots/search?projectKey=${PROJECT_KEY}&ps=1")
ANALYSIS=$(api_call "project_analyses/search?project=${PROJECT_KEY}&ps=5")
ACTIVITY=$(api_call "ce/activity?component=${PROJECT_KEY}&ps=1")

# Extract project name and version
PROJECT_NAME=$(echo """"$PROJECT_INFO"""" | grep -o '"name":"[^"]*"' | head -1 | sed 's/.*"name":"\([^"]*\)".*/\1/')

# Start report
cat > """"$REPORT_FILE"""" << EOF
# 📊 SonarCloud Full Analysis Report

**Project**: """$PROJECT_NAME"""  
**Key**: """$PROJECT_KEY"""  
**Generated**: $(date '+%Y-%m-%d %H:%M:%S')  
**SonarCloud Dashboard**: [View Online](https://sonarcloud.io/project/overview?id="""$PROJECT_KEY""")

---

## 🎯 Quality Gate Status

EOF

# Quality Gate Status
QG_STATUS=$(echo """"$QUALITY_GATE"""" | grep -o '"status":"[^"]*"' | sed 's/.*"status":"\([^"]*\)".*/\1/')
if [ """"$QG_STATUS"""" = "OK" ]; then
    echo "### ✅ PASSED" >> """"$REPORT_FILE""""
else
    echo "### ❌ FAILED" >> """"$REPORT_FILE""""
fi

# Quality Gate Conditions
echo "" >> """"$REPORT_FILE""""
echo "#### Failed Conditions:" >> """"$REPORT_FILE""""
echo """"$QUALITY_GATE"""" | grep -o '"metricKey":"[^"]*","status":"ERROR"[^}]*' | while read -r condition; do
    metric=$(echo """"$condition"""" | grep -o '"metricKey":"[^"]*"' | sed 's/.*"metricKey":"\([^"]*\)".*/\1/')
    actual=$(echo """"$condition"""" | grep -o '"actualValue":"[^"]*"' | sed 's/.*"actualValue":"\([^"]*\)".*/\1/')
    echo "- **"""$metric"""**: """$actual"""" >> """"$REPORT_FILE""""
done

# Overall Metrics Section
cat >> """"$REPORT_FILE"""" << EOF

---

## 📈 Overall Code Quality

### Reliability
EOF

# Extract metrics
bugs=$(echo """"$MEASURES"""" | grep -o '"metric":"bugs","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')
reliability_rating=$(echo """"$MEASURES"""" | grep -o '"metric":"reliability_rating","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')

# Convert rating to letter
rating_letter() {
    case $1 in
        1) echo "A" ;;
        2) echo "B" ;;
        3) echo "C" ;;
        4) echo "D" ;;
        5) echo "E" ;;
        *) echo "-" ;;
    esac
}

echo "- **Bugs**: """$bugs"""" >> """"$REPORT_FILE""""
echo "- **Rating**: $(rating_letter """$reliability_rating""")" >> """"$REPORT_FILE""""

# Security Section
vulnerabilities=$(echo """"$MEASURES"""" | grep -o '"metric":"vulnerabilities","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')
security_rating=$(echo """"$MEASURES"""" | grep -o '"metric":"security_rating","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')
security_hotspots=$(echo """"$MEASURES"""" | grep -o '"metric":"security_hotspots","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')
hotspots_reviewed=$(echo """"$MEASURES"""" | grep -o '"metric":"security_hotspots_reviewed","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')

cat >> """"$REPORT_FILE"""" << EOF

### Security
- **Vulnerabilities**: $vulnerabilities
- **Security Hotspots**: $security_hotspots
- **Hotspots Reviewed**: ${hotspots_reviewed}%
- **Rating**: $(rating_letter """$security_rating""")

EOF

# Maintainability Section
code_smells=$(echo """"$MEASURES"""" | grep -o '"metric":"code_smells","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')
tech_debt=$(echo """"$MEASURES"""" | grep -o '"metric":"sqale_index","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')
maintainability_rating=$(echo """"$MEASURES"""" | grep -o '"metric":"sqale_rating","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')

# Convert tech debt minutes to readable format
format_debt() {
    local minutes=$1
    if [ -z """"$minutes"""" ] || [ """"$minutes"""" = "0" ]; then
        echo "0min"
    elif [ """$minutes""" -lt 60 ]; then
        echo "${minutes}min"
    elif [ """$minutes""" -lt 480 ]; then  # Less than 8 hours
        echo "$((minutes / 60))h $((minutes % 60))min"
    else
        echo "$((minutes / 480))d"  # Convert to days (8h = 1d)
    fi
}

cat >> """"$REPORT_FILE"""" << EOF
### Maintainability
- **Code Smells**: $code_smells
- **Technical Debt**: $(format_debt """$tech_debt""")
- **Rating**: $(rating_letter """$maintainability_rating""")

EOF

# Coverage and Duplications
coverage=$(echo """"$MEASURES"""" | grep -o '"metric":"coverage","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')
duplications=$(echo """"$MEASURES"""" | grep -o '"metric":"duplicated_lines_density","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')
duplicated_blocks=$(echo """"$MEASURES"""" | grep -o '"metric":"duplicated_blocks","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')

cat >> """"$REPORT_FILE"""" << EOF
### Coverage & Duplications
- **Test Coverage**: ${coverage:-N/A}%
- **Duplicated Lines**: ${duplications}%
- **Duplicated Blocks**: ${duplicated_blocks:-0}

EOF

# Size Metrics
ncloc=$(echo """"$MEASURES"""" | grep -o '"metric":"ncloc","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')
complexity=$(echo """"$MEASURES"""" | grep -o '"metric":"complexity","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')
cognitive=$(echo """"$MEASURES"""" | grep -o '"metric":"cognitive_complexity","value":"[^"]*"' | sed 's/.*"value":"\([^"]*\)".*/\1/')

cat >> """"$REPORT_FILE"""" << EOF
### Size & Complexity
- **Lines of Code**: $ncloc
- **Cyclomatic Complexity**: ${complexity:-N/A}
- **Cognitive Complexity**: ${cognitive:-N/A}

---

## 🐛 Issues Breakdown

EOF

# Parse issues by severity
echo "### By Severity" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""
echo "| Severity | Count |" >> """"$REPORT_FILE""""
echo "|----------|-------|" >> """"$REPORT_FILE""""

for severity in BLOCKER CRITICAL MAJOR MINOR INFO; do
    count=$(echo """"$ISSUES"""" | grep -o "\""""$severity"""\",\"count\":[0-9]*" | grep -o "[0-9]*$" || echo "0")
    if [ """"$count"""" != "0" ]; then
        case """$severity""" in
            BLOCKER) icon="🚨" ;;
            CRITICAL) icon="❗" ;;
            MAJOR) icon="⚠️" ;;
            MINOR) icon="ℹ️" ;;
            INFO) icon="💡" ;;
        esac
        echo "| """$icon""" """$severity""" | """$count""" |" >> """"$REPORT_FILE""""
    fi
done

# Issues by type
echo "" >> """"$REPORT_FILE""""
echo "### By Type" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""
echo "| Type | Count |" >> """"$REPORT_FILE""""
echo "|------|-------|" >> """"$REPORT_FILE""""

for type in BUG VULNERABILITY CODE_SMELL SECURITY_HOTSPOT; do
    count=$(echo """"$ISSUES"""" | grep -o "\""""$type"""\",\"count\":[0-9]*" | grep -o "[0-9]*$" || echo "0")
    if [ """"$count"""" != "0" ]; then
        type_display=$(echo """"$type"""" | sed 's/_/ /g' | tr '[:upper:]' '[:lower:]' | sed 's/\b\(.\)/\u\1/g')
        echo "| """$type_display""" | """$count""" |" >> """"$REPORT_FILE""""
    fi
done

# Top 5 Rules with most violations
echo "" >> """"$REPORT_FILE""""
echo "### Top Rules with Violations" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""
echo """"$ISSUES"""" | grep -o '"rules":\[.*\]' | grep -o '"val":"[^"]*","count":[0-9]*' | sort -t: -k2 -nr | head -5 | while read -r rule; do
    rule_key=$(echo """"$rule"""" | grep -o '"val":"[^"]*"' | sed 's/.*"val":"\([^"]*\)".*/\1/')
    count=$(echo """"$rule"""" | grep -o '"count":[0-9]*' | grep -o '[0-9]*')
    echo "- **"""$rule_key"""**: """$count""" violations" >> """"$REPORT_FILE""""
done

# Recent Analysis History
cat >> """"$REPORT_FILE"""" << EOF

---

## 📅 Recent Analysis History

| Date | Events |
|------|--------|
EOF

echo """"$ANALYSIS"""" | grep -o '"date":"[^"]*"[^}]*"events":\[[^]]*\]' | head -5 | while read -r analysis; do
    date=$(echo """"$analysis"""" | grep -o '"date":"[^"]*"' | sed 's/.*"date":"\([^"]*\)".*/\1/')
    date_formatted=$(date -j -f "%Y-%m-%dT%H:%M:%S" "${date%+*}" "+%Y-%m-%d %H:%M" 2>/dev/null || echo """"$date"""")
    events=$(echo """"$analysis"""" | grep -o '"name":"[^"]*"' | sed 's/.*"name":"\([^"]*\)".*/\1/' | tr '\n' ', ' | sed 's/,$//')
    echo "| """$date_formatted""" | ${events:-No events} |" >> """"$REPORT_FILE""""
done

# Add recommendations
cat >> """"$REPORT_FILE"""" << EOF

---

## 💡 Recommendations

Based on the analysis:

EOF

# Add specific recommendations based on metrics
if [ """"$vulnerabilities"""" -gt 0 ]; then
    echo "1. **Security**: Fix """$vulnerabilities""" vulnerabilities to improve security rating" >> """"$REPORT_FILE""""
fi

if [ """"$security_hotspots"""" -gt 0 ]; then
    echo "2. **Security Review**: Review """$security_hotspots""" security hotspots" >> """"$REPORT_FILE""""
fi

if [ """"$code_smells"""" -gt 20 ]; then
    echo "3. **Code Quality**: Address """$code_smells""" code smells to improve maintainability" >> """"$REPORT_FILE""""
fi

if [ -z """"$coverage"""" ] || [ """"$coverage"""" = "0" ]; then
    echo "4. **Testing**: Add unit tests to improve code coverage" >> """"$REPORT_FILE""""
fi

# Footer
cat >> """"$REPORT_FILE"""" << EOF

---

## 🔗 Quick Links

- [Project Dashboard](https://sonarcloud.io/project/overview?id="""$PROJECT_KEY""")
- [Issues List](https://sonarcloud.io/project/issues?id="""$PROJECT_KEY""")
- [Security Hotspots](https://sonarcloud.io/project/security_hotspots?id="""$PROJECT_KEY""")
- [Measures](https://sonarcloud.io/component_measures?id="""$PROJECT_KEY""")
- [Activity](https://sonarcloud.io/project/activity?id="""$PROJECT_KEY""")

---

*Report generated by SonarCloud Report Generator*
EOF

# Create symlink
ln -sf "$(basename """"$REPORT_FILE"""")" """"$REPORTS_DIR"""/latest-full-report.md"

echo -e "${GREEN}✅ Comprehensive report generated successfully!${NC}"
echo "📄 Report saved to: """$REPORT_FILE""""
echo "🔗 Latest report: """$REPORTS_DIR"""/latest-full-report.md"