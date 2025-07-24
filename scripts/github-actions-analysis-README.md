# GitHub Actions Workflow Failure Analysis

This directory contains a comprehensive Playwright script that systematically analyzes failing workflows on GitHub Actions to identify issues with the workflow health monitor and issue creation system.

## What This Script Does

The analysis script performs the following comprehensive examination:

1. **Navigates to GitHub Actions page**: Opens https://github.com/lsendel/zamaz-debate-mcp/actions
2. **Systematic workflow examination**: Analyzes up to 20 most recent workflow runs
3. **Deep dive into failures**: For each failing workflow:
   - Clicks into the workflow run details
   - Examines job failures and error logs
   - Looks specifically for workflow health monitor runs
   - Searches for issue creation attempts and failures
   - Captures screenshots for visual verification
4. **Issues tab verification**: Checks if any issues were actually created
5. **Pattern analysis**: Identifies common error patterns and failure modes
6. **Comprehensive reporting**: Generates detailed analysis and recommendations

## Key Analysis Areas

### Workflow Health Monitor Detection
- Identifies which workflows are actually health monitor runs
- Checks if the health monitor is running at all
- Analyzes health monitor execution success/failure

### Issue Creation Analysis
- Searches for evidence of issue creation attempts
- Identifies authentication/permission errors
- Checks for token-related failures
- Verifies if issues were successfully created

### Error Pattern Recognition
- Permission denied errors
- Authentication/token issues
- API rate limiting
- Workflow configuration problems
- Resource access issues

## Files Created

### Input Files
- `analyze-github-actions-failures.js` - Main Playwright script
- `package.json` - Dependencies configuration
- `run-analysis.sh` - Setup and execution script

### Output Files (created in `github-actions-analysis/` directory)
- `workflow-failures-analysis.json` - Raw analysis data
- `workflow-failures-report.md` - Human-readable report with findings and recommendations
- `*.png` - Screenshots of workflow runs, job logs, and issues pages

## How to Run

### Prerequisites
- Node.js installed on your system
- Access to the GitHub repository (logged into GitHub)

### Quick Start
```bash
# Navigate to the scripts directory
cd scripts/

# Run the analysis (installs dependencies automatically)
./run-analysis.sh
```

### Manual Setup
```bash
# Install dependencies
npm install

# Install Playwright browser
npx playwright install chromium

# Run the analysis
node analyze-github-actions-failures.js
```

## What to Expect

1. **Browser opens**: Chromium browser will open and navigate to GitHub Actions
2. **Automated analysis**: The script will systematically click through workflows
3. **Screenshots captured**: Visual evidence is saved for each step
4. **Progress updates**: Console output shows analysis progress
5. **Results generated**: Comprehensive report created with findings

## Key Findings The Script Will Identify

### If Workflow Health Monitor is Not Running
- Missing or disabled workflow files
- Incorrect trigger configurations
- Workflow permission issues

### If Health Monitor Runs But Fails
- Environment variable problems
- Authentication issues
- Script execution errors
- Permission problems

### If Issue Creation is Attempted But Fails
- GitHub token permission issues (`issues: write` missing)
- API authentication problems
- Rate limiting issues
- Repository access problems

### If Issues Are Not Being Created
- Workflow not reaching issue creation step
- Silent failures in issue creation logic
- Incorrect issue creation API usage

## Troubleshooting

### Script Won't Start
- Ensure Node.js is installed: `node --version`
- Check internet connectivity
- Verify GitHub repository access

### Browser Issues
- Make sure you're logged into GitHub in your default browser
- Clear browser cache if needed
- Ensure popup blockers aren't interfering

### Analysis Incomplete
- Check console output for specific errors
- Verify GitHub repository URL is accessible
- Check if workflow runs exist in the Actions tab

## Understanding the Report

The generated report (`workflow-failures-report.md`) includes:

1. **Executive Summary**: High-level statistics and findings
2. **Detailed Workflow Analysis**: Per-workflow breakdown of failures
3. **Health Monitor Specific Analysis**: Focus on health monitor runs
4. **Common Error Patterns**: Categorized error types and messages
5. **Issues Tab Analysis**: Verification of actual issue creation
6. **Key Findings and Recommendations**: Actionable next steps

## Next Steps After Analysis

Based on the report findings, you can:

1. **Fix permission issues**: Update GitHub token permissions
2. **Correct workflow configuration**: Fix trigger conditions or environment variables
3. **Debug authentication**: Resolve token or credential problems
4. **Update workflow logic**: Fix issue creation code based on identified errors
5. **Monitor improvements**: Re-run analysis after fixes to verify resolution

This comprehensive analysis will provide the real data needed to understand and fix the workflow health monitor and issue creation system.