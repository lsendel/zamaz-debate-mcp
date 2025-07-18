#!/bin/bash

# Detailed SonarCloud Issues Report
# Generates a comprehensive, actionable report with specific issues and fixes

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

# Check token
if [ -z """"$SONAR_TOKEN"""" ]; then
    echo -e "${RED}Error: SONAR_TOKEN environment variable is required${NC}"
    exit 1
fi

# Create reports directory
mkdir -p """"$REPORTS_DIR""""

# Generate filename
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE=""""$REPORTS_DIR"""/detailed-issues-report-${TIMESTAMP}.md"

echo -e "${BLUE}🔍 Generating Detailed Issues Report...${NC}"

# Start report
cat > """"$REPORT_FILE"""" << EOF
# 🚨 SonarCloud Detailed Issues Report

**Project**: """$PROJECT_KEY"""  
**Generated**: $(date '+%Y-%m-%d %H:%M:%S')  
**Dashboard**: [View on SonarCloud](https://sonarcloud.io/project/overview?id="""$PROJECT_KEY""")

This report lists specific issues in your codebase with exact locations and recommended fixes.

---

EOF

# Function to fetch and display issues
fetch_and_display_issues() {
    local severity=$1
    local icon=$2
    local limit=${3:-50}
    
    echo -e "${BLUE}Fetching """$severity""" issues...${NC}"
    
    # Fetch issues
    local response=$(curl -s -H "Authorization: Bearer """$SONAR_TOKEN"""" \
        "${SONAR_URL}/api/issues/search?componentKeys=${PROJECT_KEY}&severities=${severity}&ps=${limit}&s=FILE_LINE&asc=true")
    
    # Get total count
    local total=$(echo """"$response"""" | jq -r '.total // 0')
    
    if [ """"$total"""" -eq 0 ]; then
        echo "## """$icon""" """$severity""" Issues" >> """"$REPORT_FILE""""
        echo "" >> """"$REPORT_FILE""""
        echo "*No """$severity""" issues found* ✅" >> """"$REPORT_FILE""""
        echo "" >> """"$REPORT_FILE""""
        return
    fi
    
    echo "## """$icon""" """$severity""" Issues (Total: """$total""")" >> """"$REPORT_FILE""""
    echo "" >> """"$REPORT_FILE""""
    
    if [ """"$total"""" -gt """"$limit"""" ]; then
        echo "*Showing first """$limit""" of """$total""" issues*" >> """"$REPORT_FILE""""
        echo "" >> """"$REPORT_FILE""""
    fi
    
    # Process each issue
    echo """"$response"""" | jq -c '.issues[]' | while IFS= read -r issue; do
        # Extract issue details
        local file_path=$(echo """"$issue"""" | jq -r '.component' | sed "s/${PROJECT_KEY}://")
        local line=$(echo """"$issue"""" | jq -r '.line // "N/A"')
        local message=$(echo """"$issue"""" | jq -r '.message')
        local rule=$(echo """"$issue"""" | jq -r '.rule')
        local type=$(echo """"$issue"""" | jq -r '.type')
        local effort=$(echo """"$issue"""" | jq -r '.effort // "Unknown"')
        local key=$(echo """"$issue"""" | jq -r '.key')
        local status=$(echo """"$issue"""" | jq -r '.status')
        
        # Skip closed issues
        if [ """"$status"""" = "CLOSED" ]; then
            continue
        fi
        
        # Format the issue
        echo "### 📍 \$("""$file_path""":"""$line"""\)" >> """"$REPORT_FILE""""
        echo "" >> """"$REPORT_FILE""""
        echo "**Issue**: """$message"""" >> """"$REPORT_FILE""""
        echo "**Rule**: [\$("""$rule"""\)](https://rules.sonarsource.com/${rule})" >> """"$REPORT_FILE""""
        echo "**Type**: """$type""" | **Effort**: """$effort""" | **Status**: """$status"""" >> """"$REPORT_FILE""""
        echo "" >> """"$REPORT_FILE""""
        
        # Add specific fix recommendations based on rule
        case """"$rule"""" in
            "java:S6437"|"secrets:S6698")
                echo "**🔧 How to fix**:" >> """"$REPORT_FILE""""
                echo "1. Remove the hardcoded password from the code" >> """"$REPORT_FILE""""
                echo "2. Use environment variables: \$(\${DB_PASSWORD}\)" >> """"$REPORT_FILE""""
                echo "3. Or use a secrets management system" >> """"$REPORT_FILE""""
                ;;
            "java:S1192")
                echo "**🔧 How to fix**: Extract the duplicated string to a constant" >> """"$REPORT_FILE""""
                ;;
            "java:S3776")
                echo "**🔧 How to fix**: Refactor this method to reduce its Cognitive Complexity" >> """"$REPORT_FILE""""
                ;;
            *)
                echo "**🔧 How to fix**: [View rule documentation](https://rules.sonarsource.com/${rule})" >> """"$REPORT_FILE""""
                ;;
        esac
        
        echo "" >> """"$REPORT_FILE""""
        echo "[View in SonarCloud](https://sonarcloud.io/project/issues?id=${PROJECT_KEY}&open="""$key""") | [View File](https://sonarcloud.io/code?id=${PROJECT_KEY}&selected=${file_path})" >> """"$REPORT_FILE""""
        echo "" >> """"$REPORT_FILE""""
        echo "---" >> """"$REPORT_FILE""""
        echo "" >> """"$REPORT_FILE""""
    done
}

# Fetch BLOCKER issues
fetch_and_display_issues "BLOCKER" "🚨" 50

# Fetch CRITICAL issues (limited to 20)
fetch_and_display_issues "CRITICAL" "❗" 20

# Security vulnerabilities section
echo -e "${YELLOW}Fetching security vulnerabilities...${NC}"
echo "## 🔒 Security Vulnerabilities" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""

VULN_RESPONSE=$(curl -s -H "Authorization: Bearer """$SONAR_TOKEN"""" \
    "${SONAR_URL}/api/issues/search?componentKeys=${PROJECT_KEY}&types=VULNERABILITY&statuses=OPEN,REOPENED&ps=50")

VULN_TOTAL=$(echo """"$VULN_RESPONSE"""" | jq -r '.total // 0')

if [ """"$VULN_TOTAL"""" -gt 0 ]; then
    echo "Found **"""$VULN_TOTAL""" vulnerabilities** that need immediate attention:" >> """"$REPORT_FILE""""
    echo "" >> """"$REPORT_FILE""""
    
    echo """"$VULN_RESPONSE"""" | jq -c '.issues[]' | while IFS= read -r issue; do
        local file_path=$(echo """"$issue"""" | jq -r '.component' | sed "s/${PROJECT_KEY}://")
        local line=$(echo """"$issue"""" | jq -r '.line // "N/A"')
        local message=$(echo """"$issue"""" | jq -r '.message')
        local severity=$(echo """"$issue"""" | jq -r '.severity')
        local rule=$(echo """"$issue"""" | jq -r '.rule')
        
        echo "- **"""$severity"""**: \$("""$file_path""":"""$line"""\)" >> """"$REPORT_FILE""""
        echo "  - """$message"""" >> """"$REPORT_FILE""""
        echo "  - Rule: [\$("""$rule"""\)](https://rules.sonarsource.com/${rule})" >> """"$REPORT_FILE""""
        echo "" >> """"$REPORT_FILE""""
    done
else
    echo "*No open vulnerabilities found* ✅" >> """"$REPORT_FILE""""
fi

echo "" >> """"$REPORT_FILE""""

# Most problematic files
echo -e "${BLUE}Analyzing files with most issues...${NC}"
echo "## 📁 Files Requiring Most Attention" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""

FILES_RESPONSE=$(curl -s -H "Authorization: Bearer """$SONAR_TOKEN"""" \
    "${SONAR_URL}/api/issues/search?componentKeys=${PROJECT_KEY}&ps=1&facets=files")

echo "| File | Open Issues | Action |" >> """"$REPORT_FILE""""
echo "|------|-------------|--------|" >> """"$REPORT_FILE""""

echo """"$FILES_RESPONSE"""" | jq -r '.facets[] | select(.property=="files") | .values[:10][] | 
    "\(.val | sub("'""""$PROJECT_KEY""""':"; ""))|\(.count)|Review"' | while IFS='|' read -r file count action; do
    if [ """"$count"""" -gt 20 ]; then
        action="🔴 Urgent"
    elif [ """"$count"""" -gt 10 ]; then
        action="🟡 High Priority"
    else
        action="🟢 Normal"
    fi
    echo "| \$("""$file"""\) | """$count""" | """$action""" |" >> """"$REPORT_FILE""""
done

# Summary and action plan
echo "" >> """"$REPORT_FILE""""
echo "## 📊 Summary" >> """"$REPORT_FILE""""
echo "" >> """"$REPORT_FILE""""

# Get summary stats
SUMMARY=$(curl -s -H "Authorization: Bearer """$SONAR_TOKEN"""" \
    "${SONAR_URL}/api/issues/search?componentKeys=${PROJECT_KEY}&ps=1&facets=severities,types&statuses=OPEN,REOPENED")

echo "### Open Issues by Severity" >> """"$REPORT_FILE""""
echo """"$SUMMARY"""" | jq -r '.facets[] | select(.property=="severities") | .values[] | 
    "- **\(.val)**: \(.count) issues"' >> """"$REPORT_FILE""""

echo "" >> """"$REPORT_FILE""""
echo "### Open Issues by Type" >> """"$REPORT_FILE""""
echo """"$SUMMARY"""" | jq -r '.facets[] | select(.property=="types") | .values[] | 
    "- **\(.val | gsub("_"; " ") | ascii_downcase | gsub("\\b(.)"; . | ascii_upcase))**: \(.count)"' >> """"$REPORT_FILE""""

# Action plan
cat >> """"$REPORT_FILE"""" << 'EOF'

## 🎯 Recommended Fix Order

### 🚨 Immediate Actions (Day 1)
1. **Fix all BLOCKER issues** - These are critical security vulnerabilities
   - Remove hardcoded passwords from application.yml files
   - Use environment variables or secrets management

### ❗ High Priority (Week 1)
2. **Fix CRITICAL issues** - Focus on security vulnerabilities first
3. **Review Security Hotspots** - These need human verification

### ⚠️ Medium Priority (Week 2-3)
4. **Address MAJOR issues** in core service files
5. **Fix code smells** in files with >20 issues
6. **Improve test coverage** for critical paths

### 💡 Ongoing Improvements
7. **Fix MINOR issues** during regular development
8. **Refactor complex methods** when touching the code
9. **Remove code duplication** incrementally

## 🛠️ Quick Fixes You Can Do Now

1. **Password in application.yml files**:
   $()$(...)yaml
   # Replace this:
   password: postgres
   
   # With this:
   password: ${DB_PASSWORD:default_value}
   $()$(...)

2. **Then set environment variable**:
   $()$(...)bash
   export DB_PASSWORD=your_secure_password
   $()$(...)

3. **Or use Spring Cloud Config Server** for centralized configuration

## 📚 Helpful Resources

- [SonarCloud Security Hotspots](https://sonarcloud.io/project/security_hotspots?id=lsendel_zamaz-debate-mcp)
- [Spring Boot Configuration Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [OWASP Secure Coding Practices](https://owasp.org/www-project-secure-coding-practices-quick-reference-guide/)

EOF

# Create symlink
ln -sf "$(basename """"$REPORT_FILE"""")" """"$REPORTS_DIR"""/latest-detailed-report.md"

echo -e "${GREEN}✅ Detailed report generated successfully!${NC}"
echo -e "📄 Report saved to: """$REPORT_FILE""""
echo -e "🔗 Latest report: """$REPORTS_DIR"""/latest-detailed-report.md"