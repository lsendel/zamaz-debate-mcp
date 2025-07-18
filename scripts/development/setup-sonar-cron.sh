#!/bin/bash

# Script to set up cron job for regular SonarQube report generation

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORT_SCRIPT="${SCRIPT_DIR}/generate-sonar-report.sh"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "================================================"
echo "SonarQube Report Cron Job Setup"
echo "================================================"
echo

# Check if report script exists
if [ ! -f """$REPORT_SCRIPT""" ]; then
    echo -e "${RED}Error: Report generation script not found at ""$REPORT_SCRIPT""${NC}"
    exit 1
fi

# Function to add cron job
add_cron_job() {
    local schedule=$1
    local env_vars=$2
    
    # Create cron command
    CRON_CMD="${env_vars} ${REPORT_SCRIPT} >> ${SCRIPT_DIR}/../sonar-reports/cron.log 2>&1"
    CRON_JOB="${schedule} ${CRON_CMD}"
    
    # Check if cron job already exists
    if crontab -l 2>/dev/null | grep -q """$REPORT_SCRIPT"""; then
        echo -e "${YELLOW}Warning: A cron job for SonarQube reports already exists${NC}"
        echo "Current cron job:"
        crontab -l | grep """$REPORT_SCRIPT"""
        echo
        read -p "Do you want to replace it? (y/n): " -n 1 -r
        echo
        if [[ ! ""$REPLY"" =~ ^[Yy]$ ]]; then
            echo "Keeping existing cron job."
            return
        fi
        # Remove existing job
        crontab -l | grep -v """$REPORT_SCRIPT""" | crontab -
    fi
    
    # Add new cron job
    (crontab -l 2>/dev/null; echo """$CRON_JOB""") | crontab -
    echo -e "${GREEN}✓ Cron job added successfully${NC}"
    echo "Schedule: ""$schedule"""
}

# Interactive setup
echo "Choose report generation schedule:"
echo "1) Daily at 2 AM"
echo "2) Weekly on Mondays at 2 AM"
echo "3) Every 12 hours"
echo "4) Custom schedule"
echo "5) Remove existing cron job"
echo

read -p "Enter your choice (1-5): " choice

# Get environment variables
read -p "Enter SonarQube URL (default: http://localhost:9000): " SONAR_URL
SONAR_URL=${SONAR_URL:-http://localhost:9000}

read -p "Enter SonarQube Token (leave empty for anonymous): " SONAR_TOKEN

read -p "Enter Project Key (default: com.zamaz.mcp:mcp-parent): " SONAR_PROJECT_KEY
SONAR_PROJECT_KEY=${SONAR_PROJECT_KEY:-com.zamaz.mcp:mcp-parent}

read -p "Enter Branch (default: main): " SONAR_BRANCH
SONAR_BRANCH=${SONAR_BRANCH:-main}

# Build environment variables string
ENV_VARS="SONAR_URL='""$SONAR_URL""' SONAR_PROJECT_KEY='""$SONAR_PROJECT_KEY""' SONAR_BRANCH='""$SONAR_BRANCH""'"
if [ -n """$SONAR_TOKEN""" ]; then
    ENV_VARS="""$ENV_VARS"" SONAR_TOKEN='""$SONAR_TOKEN""'"
fi

case ""$choice"" in
    1)
        add_cron_job "0 2 * * *" """$ENV_VARS"""
        ;;
    2)
        add_cron_job "0 2 * * 1" """$ENV_VARS"""
        ;;
    3)
        add_cron_job "0 */12 * * *" """$ENV_VARS"""
        ;;
    4)
        echo "Enter custom cron schedule (e.g., '0 2 * * *' for daily at 2 AM):"
        echo "Format: minute hour day month day-of-week"
        echo "Use https://crontab.guru/ for help with cron syntax"
        read -p "Schedule: " custom_schedule
        if [ -n """$custom_schedule""" ]; then
            add_cron_job """$custom_schedule""" """$ENV_VARS"""
        else
            echo -e "${RED}Error: No schedule provided${NC}"
            exit 1
        fi
        ;;
    5)
        if crontab -l 2>/dev/null | grep -q """$REPORT_SCRIPT"""; then
            crontab -l | grep -v """$REPORT_SCRIPT""" | crontab -
            echo -e "${GREEN}✓ Cron job removed successfully${NC}"
        else
            echo -e "${YELLOW}No existing cron job found${NC}"
        fi
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo
echo "================================================"
echo "Setup completed!"
echo "================================================"
echo
echo "Current cron jobs:"
crontab -l 2>/dev/null | grep """$REPORT_SCRIPT""" || echo "No SonarQube report cron jobs found"
echo
echo "To view all cron jobs: crontab -l"
echo "To edit cron jobs manually: crontab -e"
echo "To view report logs: tail -f ${SCRIPT_DIR}/../sonar-reports/cron.log"