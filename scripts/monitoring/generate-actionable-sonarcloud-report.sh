#!/bin/bash

# Actionable SonarCloud Report Generator
# Creates a comprehensive report with specific issues, locations, and fix recommendations

set -e

# Configuration
SONAR_TOKEN="${SONAR_TOKEN:-}"
PROJECT_KEY="lsendel_zamaz-debate-mcp"
SONAR_URL="https://sonarcloud.io"
REPORTS_DIR="sonar-reports"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Check prerequisites
if [ -z """"$SONAR_TOKEN"""" ]; then
    echo -e "${RED}Error: SONAR_TOKEN environment variable is required${NC}"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo -e "${RED}Error: jq is required. Install with: brew install jq${NC}"
    exit 1
fi

# Create reports directory
mkdir -p """"$REPORTS_DIR""""

# Generate filename
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE=""""$REPORTS_DIR"""/actionable-sonarcloud-report-${TIMESTAMP}.md"

echo -e "${BLUE}🔍 Generating Actionable SonarCloud Report...${NC}"

# API call helper
api_call() {
    curl -s -H "Authorization: Bearer """$SONAR_TOKEN"""" "${SONAR_URL}/api/$1"
}

# Function to get rule details
get_rule_details() {
    local rule_key=$1
    api_call "rules/show?key="""$rule_key"""" | jq -r '.rule | {name: .name, severity: .severity, type: .type, description: .htmlDesc}'
}

# Start report
cat > """"$REPORT_FILE"""" << 'EOF'
# 🚨 Actionable SonarCloud Report - Fix Guide

This report provides specific, actionable information to fix issues in your codebase.

EOF

echo "**Project**: """$PROJECT_KEY"""" >> """"$REPORT_FILE""""
echo "**Generated**: $(date '+%Y-%m-%d %H:%M:%S')" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""

# Get project status
echo -e "${BLUE}📊 Fetching project status...${NC}"
QUALITY_GATE=$(api_call "qualitygates/project_status?projectKey=${PROJECT_KEY}")
QG_STATUS=$(echo """"$QUALITY_GATE"""" | jq -r '.projectStatus.status')

if [ """"$QG_STATUS"""" = "OK" ]; then
    echo "## ✅ Quality Gate: PASSED" >> """"$REPORT_FILE""""
else
    echo "## ❌ Quality Gate: FAILED" >> """"$REPORT_FILE""""
fi

echo "" >> """"$REPORT_FILE""""

# Fetch issues with details
echo -e "${BLUE}🐛 Fetching detailed issues...${NC}"

# Function to fetch and format issues by severity
fetch_issues_by_severity() {
    local severity=$1
    local icon=$2
    local page=1
    local has_more=true
    local issues_found=false
    
    echo "" >> """"$REPORT_FILE""""
    echo "## """$icon""" """$severity""" Issues" >> """"$REPORT_FILE""""
    echo "" >> """"$REPORT_FILE""""
    
    while [ """"$has_more"""" = true ]; do
        ISSUES_RESPONSE=$(api_call "issues/search?componentKeys=${PROJECT_KEY}&severities=${severity}&ps=100&p=${page}&s=FILE_LINE&asc=true")
        
        # Check if there are issues
        local total=$(echo """"$ISSUES_RESPONSE"""" | jq -r '.total // 0')
        if [ """"$total"""" -eq 0 ]; then
            echo "*No """$severity""" issues found* ✅" >> """"$REPORT_FILE""""
            break
        fi
        
        issues_found=true
        
        # Process each issue
        echo """"$ISSUES_RESPONSE"""" | jq -c '.issues[]' | while read -r issue; do
            local component=$(echo """"$issue"""" | jq -r '.component')
            local line=$(echo """"$issue"""" | jq -r '.line // "N/A"')
            local message=$(echo """"$issue"""" | jq -r '.message')
            local rule=$(echo """"$issue"""" | jq -r '.rule')
            local type=$(echo """"$issue"""" | jq -r '.type')
            local effort=$(echo """"$issue"""" | jq -r '.effort // "Unknown"')
            local key=$(echo """"$issue"""" | jq -r '.key')
            
            # Extract file path
            local file_path=$(echo """"$component"""" | sed "s/${PROJECT_KEY}://")
            
            # Write issue details
            echo "### 📍 \$("""$file_path""":"""$line"""\)" >> """"$REPORT_FILE""""
            echo "" >> """"$REPORT_FILE""""
            echo "**Issue**: """$message"""" >> """"$REPORT_FILE""""
            echo "**Rule**: \$("""$rule"""\)" >> """"$REPORT_FILE""""
            echo "**Type**: """$type""" | **Effort to fix**: """$effort"""" >> """"$REPORT_FILE""""
            echo "" >> """"$REPORT_FILE""""
            
            # Add link to view in SonarCloud
            echo "[View in SonarCloud](https://sonarcloud.io/project/issues?id=${PROJECT_KEY}&open="""$key""")" >> """"$REPORT_FILE""""
            echo "" >> """"$REPORT_FILE""""
            echo "---" >> """"$REPORT_FILE""""
            echo "" >> """"$REPORT_FILE""""
        done
        
        # Check if there are more pages
        local p=$(echo """"$ISSUES_RESPONSE"""" | jq -r '.p // 1')
        local ps=$(echo """"$ISSUES_RESPONSE"""" | jq -r '.ps // 100')
        local total=$(echo """"$ISSUES_RESPONSE"""" | jq -r '.total // 0')
        
        if [ "$((p" * ps)) -ge """"$total"""" ]; then
            has_more=false
        else
            page=$((page + 1))
        fi
    done
    
    if [ """"$issues_found"""" = true ]; then
        return 0
    else
        return 1
    fi
}

# Fetch BLOCKER issues
echo -e "${RED}🚨 Fetching BLOCKER issues...${NC}"
fetch_issues_by_severity "BLOCKER" "🚨"

# Fetch CRITICAL issues
echo -e "${RED}❗ Fetching CRITICAL issues (first 20)...${NC}"
echo "" >> """"$REPORT_FILE""""
echo "## ❗ CRITICAL Issues (Top 20)" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""

CRITICAL_ISSUES=$(api_call "issues/search?componentKeys=${PROJECT_KEY}&severities=CRITICAL&ps=20&s=FILE_LINE&asc=true")
echo """"$CRITICAL_ISSUES"""" | jq -c '.issues[]' | while read -r issue; do
    component=$(echo """"$issue"""" | jq -r '.component')
    line=$(echo """"$issue"""" | jq -r '.line // "N/A"')
    message=$(echo """"$issue"""" | jq -r '.message')
    rule=$(echo """"$issue"""" | jq -r '.rule')
    key=$(echo """"$issue"""" | jq -r '.key')
    
    file_path=$(echo """"$component"""" | sed "s/${PROJECT_KEY}://")
    
    echo "- \$("""$file_path""":"""$line"""\) - """$message""" [\$("""$rule"""\)]" >> """"$REPORT_FILE""""
done

# Security vulnerabilities detail
echo -e "${YELLOW}🔒 Fetching security vulnerabilities...${NC}"
echo "" >> """"$REPORT_FILE""""
echo "## 🔒 Security Vulnerabilities (Must Fix)" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""

VULNERABILITIES=$(api_call "issues/search?componentKeys=${PROJECT_KEY}&types=VULNERABILITY&ps=50")
vuln_count=$(echo """"$VULNERABILITIES"""" | jq -r '.total // 0')

if [ """"$vuln_count"""" -gt 0 ]; then
    echo """"$VULNERABILITIES"""" | jq -c '.issues[]' | while read -r issue; do
        component=$(echo """"$issue"""" | jq -r '.component')
        line=$(echo """"$issue"""" | jq -r '.line // "N/A"')
        message=$(echo """"$issue"""" | jq -r '.message')
        rule=$(echo """"$issue"""" | jq -r '.rule')
        severity=$(echo """"$issue"""" | jq -r '.severity')
        key=$(echo """"$issue"""" | jq -r '.key')
        
        file_path=$(echo """"$component"""" | sed "s/${PROJECT_KEY}://")
        
        echo "### 🔐 \$("""$file_path""":"""$line"""\)" >> """"$REPORT_FILE""""
        echo "**Severity**: """$severity"""" >> """"$REPORT_FILE""""
        echo "**Issue**: """$message"""" >> """"$REPORT_FILE""""
        echo "**Rule**: \$("""$rule"""\)" >> """"$REPORT_FILE""""
        echo "[Fix This Issue](https://sonarcloud.io/project/issues?id=${PROJECT_KEY}&open="""$key""")" >> """"$REPORT_FILE""""
        echo "" >> """"$REPORT_FILE""""
    done
else
    echo "*No vulnerabilities found* ✅" >> """"$REPORT_FILE""""
fi

# Security hotspots
echo -e "${YELLOW}🔥 Fetching security hotspots...${NC}"
echo "" >> """"$REPORT_FILE""""
echo "## 🔥 Security Hotspots (Need Review)" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""

HOTSPOTS=$(api_call "hotspots/search?projectKey=${PROJECT_KEY}&ps=50")
hotspot_count=$(echo """"$HOTSPOTS"""" | jq -r '.paging.total // 0')

if [ """"$hotspot_count"""" -gt 0 ]; then
    echo "Found **"""$hotspot_count""" security hotspots** that need review:" >> """"$REPORT_FILE""""
    echo "" >> """"$REPORT_FILE""""
    
    echo """"$HOTSPOTS"""" | jq -c '.hotspots[]' | head -10 | while read -r hotspot; do
        component=$(echo """"$hotspot"""" | jq -r '.component')
        line=$(echo """"$hotspot"""" | jq -r '.line // "N/A"')
        message=$(echo """"$hotspot"""" | jq -r '.message')
        key=$(echo """"$hotspot"""" | jq -r '.key')
        
        file_path=$(echo """"$component"""" | sed "s/${PROJECT_KEY}://")
        
        echo "- \$("""$file_path""":"""$line"""\) - """$message"""" >> """"$REPORT_FILE""""
    done
    
    echo "" >> """"$REPORT_FILE""""
    echo "[Review All Hotspots](https://sonarcloud.io/project/security_hotspots?id=${PROJECT_KEY})" >> """"$REPORT_FILE""""
else
    echo "*No security hotspots found* ✅" >> """"$REPORT_FILE""""
fi

# Most problematic files
echo -e "${BLUE}📁 Analyzing most problematic files...${NC}"
echo "" >> """"$REPORT_FILE""""
echo "## 📁 Files with Most Issues" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""

# Get files with most issues
FILES_WITH_ISSUES=$(api_call "issues/search?componentKeys=${PROJECT_KEY}&ps=1&facets=files" | jq -r '.facets[] | select(.property=="files") | .values[:10]')

echo "| File | Issue Count | Priority |" >> """"$REPORT_FILE""""
echo "|------|-------------|----------|" >> """"$REPORT_FILE""""

echo """"$FILES_WITH_ISSUES"""" | jq -c '.[]' | while read -r file_data; do
    file=$(echo """"$file_data"""" | jq -r '.val')
    count=$(echo """"$file_data"""" | jq -r '.count')
    
    # Remove project key prefix
    file_path=$(echo """"$file"""" | sed "s/${PROJECT_KEY}://")
    
    # Determine priority based on count
    if [ """"$count"""" -gt 20 ]; then
        priority="🔴 High"
    elif [ """"$count"""" -gt 10 ]; then
        priority="🟡 Medium"
    else
        priority="🟢 Low"
    fi
    
    echo "| \$("""$file_path"""\) | """$count""" | """$priority""" |" >> """"$REPORT_FILE""""
done

# Action plan
echo "" >> """"$REPORT_FILE""""
echo "## 🎯 Recommended Action Plan" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""

cat >> """"$REPORT_FILE"""" << 'EOF'
### Phase 1: Critical Security (Week 1)
- [ ] Fix all BLOCKER issues
- [ ] Fix all security VULNERABILITIES
- [ ] Review and fix security HOTSPOTS

### Phase 2: High Priority (Week 2)
- [ ] Fix CRITICAL issues in core services
- [ ] Address files with >20 issues
- [ ] Fix authentication/authorization issues

### Phase 3: Code Quality (Week 3-4)
- [ ] Fix remaining CRITICAL issues
- [ ] Address MAJOR issues in frequently modified files
- [ ] Reduce code duplication
- [ ] Add unit tests for uncovered code

### Quick Wins (Can do anytime)
- [ ] Fix simple code smells (naming, formatting)
- [ ] Remove unused imports
- [ ] Update deprecated method calls

EOF

# Summary statistics
echo -e "${BLUE}📈 Generating summary...${NC}"
echo "" >> """"$REPORT_FILE""""
echo "## 📊 Summary Statistics" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""

SUMMARY=$(api_call "issues/search?componentKeys=${PROJECT_KEY}&ps=1&facets=severities,types")

echo "### By Severity" >> """"$REPORT_FILE""""
echo """"$SUMMARY"""" | jq -r '.facets[] | select(.property=="severities") | .values[] | "- **\(.val)**: \(.count) issues"' >> """"$REPORT_FILE""""

echo "" >> """"$REPORT_FILE""""
echo "### By Type" >> """"$REPORT_FILE""""
echo """"$SUMMARY"""" | jq -r '.facets[] | select(.property=="types") | .values[] | "- **\(.val)**: \(.count) issues"' >> """"$REPORT_FILE""""

# Footer
cat >> """"$REPORT_FILE"""" << EOF

---

## 🔧 How to Use This Report

1. **Start with BLOCKER issues** - These prevent deployment
2. **Fix security vulnerabilities** - These are potential security risks
3. **Review security hotspots** - These need human review
4. **Use file priority list** - Focus on files with most issues
5. **Track progress** - Check off items as you complete them

## 📚 Resources

- [SonarCloud Rules Documentation](https://rules.sonarsource.com/)
- [Project Dashboard](https://sonarcloud.io/project/overview?id=${PROJECT_KEY})
- [All Issues](https://sonarcloud.io/project/issues?id=${PROJECT_KEY})

---

*Generated by Actionable SonarCloud Report Generator*
EOF

# Create symlink
ln -sf "$(basename """"$REPORT_FILE"""")" """"$REPORTS_DIR"""/latest-actionable-report.md"

echo -e "${GREEN}✅ Actionable report generated successfully!${NC}"
echo -e "📄 Report saved to: """$REPORT_FILE""""
echo -e "🔗 Latest report: """$REPORTS_DIR"""/latest-actionable-report.md"
echo ""
echo -e "${YELLOW}📋 Summary:${NC}"
echo "- Quality Gate: """$QG_STATUS""""
echo "- Vulnerabilities: """$vuln_count""""
echo "- Security Hotspots: """$hotspot_count""""