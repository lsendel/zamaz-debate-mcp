# SonarQube Report Generation Guide

This guide explains how to generate SonarQube reports in markdown format on a regular basis using the CNES Report tool.

## Overview

The SonarQube report generation system allows you to:
- Generate comprehensive code quality reports in markdown format
- Schedule automatic report generation (daily, weekly, or custom)
- Integrate with CI/CD pipelines (GitHub Actions)
- Track quality metrics over time

## Prerequisites

1. **Java 8 or higher** installed
2. **SonarQube server** accessible
3. **SonarQube token** (optional, for authenticated access)
4. **Project already analyzed** in SonarQube

## Quick Start

### 1. Generate a Report Manually

```bash
# Set environment variables
export SONAR_URL="http://your-sonarqube-server:9000"
export SONAR_TOKEN="your-sonarqube-token"
export SONAR_PROJECT_KEY="com.zamaz.mcp:mcp-parent"

# Run the report generator
./scripts/generate-sonar-report.sh
```

### 2. Set Up Automated Reports (Cron)

```bash
# Run the interactive setup
./scripts/setup-sonar-cron.sh

# Choose your schedule:
# 1) Daily at 2 AM
# 2) Weekly on Mondays at 2 AM
# 3) Every 12 hours
# 4) Custom schedule
```

### 3. GitHub Actions Integration

The project includes a GitHub Actions workflow that:
- Runs daily at 2 AM UTC
- Can be triggered manually
- Runs after SonarQube analysis completes

Required GitHub Secrets:
- `SONAR_URL`: Your SonarQube server URL
- `SONAR_TOKEN`: Authentication token

## Configuration Options

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SONAR_URL` | SonarQube server URL | `http://localhost:9000` |
| `SONAR_TOKEN` | Authentication token | (empty - anonymous) |
| `SONAR_PROJECT_KEY` | Project key in SonarQube | `com.zamaz.mcp:mcp-parent` |
| `SONAR_BRANCH` | Branch to analyze | `main` |
| `REPORT_AUTHOR` | Report author name | `MCP Team` |

### Command Line Options

```bash
./scripts/generate-sonar-report.sh [OPTIONS]

OPTIONS:
    -h, --help              Show help message
    -u, --url URL           SonarQube server URL
    -t, --token TOKEN       Authentication token
    -p, --project KEY       Project key
    -b, --branch BRANCH     Branch name
    -a, --author AUTHOR     Report author
```

## Report Outputs

Reports are generated in the `sonar-reports/` directory:

```
sonar-reports/
├── sonar-report-com-zamaz-mcp-mcp-parent-20240716_141523.md
├── sonar-report-com-zamaz-mcp-mcp-parent-20240716_141523.xlsx
├── sonar-report-com-zamaz-mcp-mcp-parent-20240716_141523.docx
├── latest-sonar-report.md -> (symlink to latest markdown)
└── report-summary.json
```

### Report Contents

The markdown report includes:
- **Project Overview**: Name, version, quality gate status
- **Metrics Summary**: Issues, coverage, duplications, complexity
- **Issues Breakdown**: By severity and type
- **Code Coverage**: Line and branch coverage
- **Technical Debt**: Effort required to fix issues
- **Duplications**: Duplicate code blocks
- **Hotspots**: Security vulnerabilities

## Scheduling Options

### 1. Local Cron Job

The `setup-sonar-cron.sh` script helps you configure cron jobs:

```bash
# View current cron jobs
crontab -l

# Edit cron jobs manually
crontab -e

# View cron logs
tail -f sonar-reports/cron.log
```

Example cron schedules:
- Daily at 2 AM: `0 2 * * *`
- Every Monday at 3 AM: `0 3 * * 1`
- Every 6 hours: `0 */6 * * *`
- First day of month: `0 0 1 * *`

### 2. GitHub Actions

The workflow runs:
- **Scheduled**: Daily at 2 AM UTC
- **Manual**: Via workflow dispatch
- **Triggered**: After SonarQube analysis

To trigger manually:
1. Go to Actions tab
2. Select "Generate SonarQube Report"
3. Click "Run workflow"
4. Choose branch (optional)

### 3. CI/CD Integration

Example Jenkins pipeline:

```groovy
pipeline {
    agent any
    
    triggers {
        cron('0 2 * * *')
    }
    
    environment {
        SONAR_URL = credentials('sonar-url')
        SONAR_TOKEN = credentials('sonar-token')
    }
    
    stages {
        stage('Generate Report') {
            steps {
                sh './scripts/generate-sonar-report.sh'
            }
        }
        
        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: 'sonar-reports/**/*'
            }
        }
    }
}
```

## Troubleshooting

### Common Issues

1. **Connection Failed**
   - Check `SONAR_URL` is correct
   - Verify network connectivity
   - Check firewall rules

2. **Authentication Failed**
   - Verify `SONAR_TOKEN` is valid
   - Check token permissions
   - Try regenerating token

3. **Project Not Found**
   - Verify `SONAR_PROJECT_KEY`
   - Ensure project is analyzed
   - Check branch name

4. **Java Version Error**
   - Install Java 8 or higher
   - Check `JAVA_HOME` environment variable

### Debug Mode

```bash
# Run with verbose output
bash -x ./scripts/generate-sonar-report.sh

# Check Java version
java -version

# Test SonarQube connection
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://your-sonarqube:9000/api/system/status
```

## Advanced Usage

### Custom Report Templates

The CNES tool supports custom templates. Place template files in:
```
tools/templates/
├── custom-markdown.vm
├── custom-excel.vm
└── custom-word.vm
```

### Multiple Projects

Generate reports for multiple projects:

```bash
#!/bin/bash
PROJECTS=(
    "com.zamaz.mcp:mcp-parent"
    "com.zamaz.mcp:mcp-common"
    "com.zamaz.mcp:mcp-security"
)

for project in "${PROJECTS[@]}"; do
    ./scripts/generate-sonar-report.sh -p "$project"
done
```

### Email Reports

Add email notification to cron job:

```bash
# In crontab
0 2 * * * /path/to/generate-sonar-report.sh && \
    mail -s "SonarQube Report" team@example.com < /path/to/latest-sonar-report.md
```

## Best Practices

1. **Regular Analysis**: Run SonarQube analysis before generating reports
2. **Version Control**: Commit report configuration but not generated reports
3. **Access Control**: Secure SonarQube tokens and limit access
4. **Retention Policy**: Clean up old reports periodically
5. **Quality Gates**: Set up quality gates to track improvements

## Integration with Development Workflow

1. **Pre-commit Hooks**: Generate mini-reports before commits
2. **Pull Request Comments**: Post summary in PR comments
3. **Dashboard Integration**: Display metrics in project dashboards
4. **Slack/Teams Notifications**: Send report summaries to chat

## Metrics Tracking

Track quality trends over time:

```bash
# Extract metrics to CSV
grep -E "(Issues|Coverage|Duplications):" sonar-reports/*.md | \
    awk -F: '{print $1","$3}' > metrics-history.csv
```

## References

- [CNES Report Tool](https://github.com/cnescatlab/sonar-cnes-report)
- [SonarQube Documentation](https://docs.sonarqube.org/)
- [Cron Expression Generator](https://crontab.guru/)
- [GitHub Actions Cron Syntax](https://docs.github.com/en/actions/using-workflows/events-that-trigger-workflows#schedule)