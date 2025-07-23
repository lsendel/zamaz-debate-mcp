# Troubleshooting Guide - Automated Workflow Issue Creation

This guide helps diagnose and resolve common issues with the automated workflow issue creation system.

## Common Issues

### 1. Issues Not Being Created

#### Symptom
Workflow fails but no GitHub issue is created.

#### Possible Causes & Solutions

**A. Missing Permissions**
```yaml
# Ensure your workflow has these permissions
permissions:
  issues: write
  actions: read
```

**B. Job Dependencies Not Set**
```yaml
# BAD - Missing needs declaration
handle-failure:
  if: failure()
  uses: ./.github/workflows/workflow-failure-handler.yml

# GOOD - Properly declares dependencies
handle-failure:
  if: failure()
  needs: [build, test, deploy]  # List ALL jobs to monitor
  uses: ./.github/workflows/workflow-failure-handler.yml
```

**C. GitHub Token Issues**
- Verify `GITHUB_TOKEN` is available
- Check token permissions in repository settings
- For private repos, ensure token has appropriate scope

**D. Workflow Not Actually Failing**
- Check if the monitored jobs actually failed
- Verify `if: failure()` condition is correct
- Review job statuses in workflow run

### 2. Duplicate Issues Being Created

#### Symptom
Multiple issues created for the same workflow failure.

#### Solutions

**A. Check Duplicate Detection**
```bash
# Verify duplicate detection is working
cd .github/scripts
node -e "
  const { IssueManager } = require('./issue-manager');
  const manager = new IssueManager(process.env.GITHUB_TOKEN, 'owner', 'repo');
  manager.checkDuplicateIssue('Workflow Name', 'failure-type')
    .then(result => console.log('Duplicate found:', result))
    .catch(err => console.error('Error:', err));
"
```

**B. Verify Issue Metadata**
Issues must contain these markers for duplicate detection:
```markdown
**workflow:Workflow Name**
**failure-type:category**
```

**C. Configuration Issues**
```yaml
# In workflow-issue-config.yml
global:
  duplicate_detection: true  # Ensure this is enabled
```

### 3. Notifications Not Working

#### Slack Notifications

**A. Test Webhook**
```bash
curl -X POST -H 'Content-type: application/json' \
  --data '{"text":"Test notification from workflow failure handler"}' \
  $SLACK_WEBHOOK_URL
```

**B. Check Secret**
```bash
# Verify secret exists
gh secret list | grep SLACK_WEBHOOK

# Re-set if needed
gh secret set SLACK_WEBHOOK
```

**C. Verify Channel Access**
- Ensure webhook has access to specified channel
- Check channel name in configuration (include #)

#### Email Notifications

**A. SMTP Configuration**
```bash
# Test SMTP settings
node -e "
  const nodemailer = require('nodemailer');
  const transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST || 'smtp.gmail.com',
    port: process.env.SMTP_PORT || 587,
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASS
    }
  });
  transporter.verify((error, success) => {
    if (error) console.error('SMTP Error:', error);
    else console.log('SMTP Ready');
  });
"
```

**B. Common Email Issues**
- Gmail: Use app-specific password, not regular password
- 2FA enabled: Generate app password
- Port issues: Try 587 (TLS) or 465 (SSL)

### 4. Template Not Found

#### Symptom
Error: "Template not found" or using wrong template.

#### Solutions

**A. Verify Template Files**
```bash
# List available templates
ls -la .github/templates/workflow-issues/

# Expected output:
# default.md
# ci-cd.md
# security.md
# linting.md
# deployment.md
# custom/
```

**B. Check Template Name**
```yaml
# In workflow call
with:
  template: "ci-cd"  # Must match filename without .md

# Or use auto-detection
with:
  template: "auto"
```

**C. Custom Template Issues**
```bash
# For custom templates
ls -la .github/templates/workflow-issues/custom/
# Ensure your-template.md exists
```

### 5. Wrong Issue Content

#### Symptom
Issue created but content is incorrect or incomplete.

#### Solutions

**A. Template Syntax Issues**
```markdown
# Check for proper Handlebars syntax
{{variable}}              # Simple variable
{{nested.property}}       # Nested property
{{#if condition}}...{{/if}}  # Conditional
{{#each array}}...{{/each}}  # Loop
```

**B. Missing Data**
```javascript
// Debug template data
const { TemplateEngine } = require('./template-engine');
const engine = new TemplateEngine();
const data = JSON.parse(process.env.ISSUE_DATA);
console.log('Available data:', JSON.stringify(data, null, 2));
```

**C. Template Testing**
```bash
# Test template rendering
cd .github/scripts
node template-engine.js ci-cd test-data.json
```

### 6. Performance Issues

#### Symptom
Workflow takes too long or times out.

#### Solutions

**A. API Rate Limiting**
- Check for 403 errors in logs
- Implement caching for duplicate checks
- Use pagination for large result sets

**B. Large Log Files**
```javascript
// Limit log extraction in failure-detector/index.js
function extractRelevantLogs(logs) {
  const lines = logs.split('\n');
  // Limit to last 100 lines instead of processing all
  return lines.slice(-100).join('\n');
}
```

**C. Timeout Issues**
```yaml
# Increase timeout in workflow
jobs:
  handle-failure:
    timeout-minutes: 10  # Default is 6
```

### 7. Configuration Not Loading

#### Symptom
Default settings used instead of custom configuration.

#### Solutions

**A. File Location**
```bash
# Verify config file exists
cat .github/config/workflow-issue-config.yml

# Check for syntax errors
npx js-yaml .github/config/workflow-issue-config.yml
```

**B. Workflow Name Mismatch**
```yaml
# Configuration uses exact workflow name
workflows:
  "CI/CD Pipeline":  # Must match exactly
    severity: high

# In workflow call
with:
  workflow-name: "CI/CD Pipeline"  # Must match config
```

### 8. Escalation Not Working

#### Symptom
Repeated failures don't trigger escalation.

#### Solutions

**A. Check Failure Count**
Look for metadata in issue body:
```markdown
<!-- failure-count:3 -->
```

**B. Verify Escalation Config**
```yaml
workflows:
  "Your Workflow":
    escalation_threshold: 3  # Number of failures before escalation
```

**C. Check Escalation Channels**
```bash
# Verify escalation secrets
gh secret list | grep ESCALATION
```

## Debugging Tools

### 1. Enable Debug Logging

```yaml
# In workflow
env:
  ACTIONS_STEP_DEBUG: true
  ACTIONS_RUNNER_DEBUG: true
```

### 2. Dry Run Mode

Test without creating issues:
```yaml
with:
  dry-run: true
```

### 3. Check Component Health

```bash
cd .github/scripts
node monitoring.js health
```

### 4. View Workflow Logs

```bash
# Download workflow logs
gh run view RUN_ID --log

# Search for errors
gh run view RUN_ID --log | grep -i error
```

### 5. Test Individual Components

```bash
# Test failure detection
cd .github/actions/failure-detector
npm test

# Test issue creation
cd .github/scripts
npm test

# Test templates
node template-engine.js default test-data.json
```

## Error Messages

### "Error: Cannot find module '@actions/core'"
```bash
cd .github/actions/failure-detector
npm install
```

### "Error: Not Found" (404) when creating issue
- Check repository name and owner
- Verify token has issues:write permission
- Ensure repository has issues enabled

### "Error: Validation Failed" (422)
- Check for missing required fields
- Verify assignees exist
- Ensure labels are valid

### "Error: Bad credentials" (401)
- Token expired or invalid
- Re-create and update GITHUB_TOKEN secret

## Getting Help

### 1. Check Logs
Always start by checking the workflow run logs for specific error messages.

### 2. Enable Debugging
Use debug mode to get detailed output.

### 3. Test in Isolation
Test individual components before full integration.

### 4. Community Support
- Open an issue with:
  - Error messages
  - Configuration files (sanitized)
  - Workflow YAML
  - Steps to reproduce