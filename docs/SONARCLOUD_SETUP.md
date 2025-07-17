# SonarCloud Setup for zamaz-debate-mcp

## Your Project Information

- **SonarCloud URL**: https://sonarcloud.io
- **Organization**: lsendel  
- **Project Key**: lsendel_zamaz-debate-mcp
- **Project URL**: https://sonarcloud.io/project/overview?id=lsendel_zamaz-debate-mcp
- **Region**: EU

## Step 1: Get Your SonarCloud Token

1. Go to https://sonarcloud.io/account/security
2. Generate a new token with a descriptive name like "zamaz-report-generator"
3. Copy the token (you won't be able to see it again!)

## Step 2: Add to ~/.zshrc

Add these lines to your `~/.zshrc` file:

```bash
# SonarCloud Configuration for zamaz-debate-mcp
export SONAR_URL="https://sonarcloud.io"
export SONAR_PROJECT_KEY="lsendel_zamaz-debate-mcp"
export SONAR_ORGANIZATION="lsendel"
export SONAR_BRANCH="main"
export REPORT_AUTHOR="Zamaz Team"
export SONAR_TOKEN="your-token-here"  # Replace with your actual token
```

## Step 3: Reload Your Shell

```bash
source ~/.zshrc
```

## Step 4: Test the Configuration

```bash
# Verify environment variables are set
echo "SONAR_URL: $SONAR_URL"
echo "SONAR_PROJECT_KEY: $SONAR_PROJECT_KEY"
echo "SONAR_TOKEN: ${SONAR_TOKEN:0:10}..." # Shows first 10 chars only

# Test API connection
curl -H "Authorization: Bearer $SONAR_TOKEN" \
  "https://sonarcloud.io/api/project_analyses/search?project=$SONAR_PROJECT_KEY" \
  | jq .
```

## Step 5: Generate Your First Report

```bash
cd /Users/lsendel/IdeaProjects/zamaz-debate-mcp
./scripts/generate-sonar-report.sh
```

## Expected Output

If everything is configured correctly, you should see:
- Report files in `sonar-reports/` directory
- A markdown file: `sonar-report-lsendel_zamaz-debate-mcp-[timestamp].md`
- A symlink: `latest-sonar-report.md`

## Troubleshooting

### Authentication Error
If you get a 401 error, your token might be:
- Incorrect (check for copy/paste errors)
- Expired (generate a new one)
- Missing required permissions

### Project Not Found
If the project isn't found:
- Verify the project key: `lsendel_zamaz-debate-mcp`
- Ensure the project has been analyzed at least once
- Check the branch name (default is `main`)

### Connection Issues
For connection problems:
- Check your internet connection
- Verify no proxy is blocking access
- Try the direct API test command above

## Setting Up Automated Reports

### Daily Reports with Cron

```bash
./scripts/setup-sonar-cron.sh
# Choose option 1 for daily at 2 AM
```

### GitHub Actions

Add these secrets to your GitHub repository:
- `SONAR_TOKEN`: Your SonarCloud token
- `SONAR_URL`: https://sonarcloud.io

The workflow will run automatically.

## View Your Project Quality

Visit your project dashboard:
https://sonarcloud.io/project/overview?id=lsendel_zamaz-debate-mcp

## Notes

- The CNES Report tool works with SonarCloud without needing the organization parameter
- Reports include quality gates, issues, coverage, and duplications
- You can customize report templates if needed