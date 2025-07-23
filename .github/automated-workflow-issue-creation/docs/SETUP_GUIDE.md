# Setup Guide - Automated Workflow Issue Creation

This guide walks you through setting up the automated workflow issue creation system in your GitHub repository.

## Prerequisites

- GitHub repository with Actions enabled
- Node.js 20+ (for local development/testing)
- Appropriate repository permissions:
  - Write access to create issues
  - Admin access to configure secrets

## Installation Steps

### 1. Copy Required Files

Copy the following directories and files to your repository:

```bash
# Create directories
mkdir -p .github/{actions/failure-detector,scripts,templates/workflow-issues/custom,config,workflows}

# Copy action files
cp -r path/to/source/.github/actions/failure-detector/* .github/actions/failure-detector/

# Copy scripts
cp path/to/source/.github/scripts/*.js .github/scripts/

# Copy templates
cp -r path/to/source/.github/templates/workflow-issues/* .github/templates/workflow-issues/

# Copy workflows
cp path/to/source/.github/workflows/workflow-failure-handler.yml .github/workflows/
```

### 2. Install Dependencies

Navigate to each component directory and install dependencies:

```bash
# Install action dependencies
cd .github/actions/failure-detector
npm install

# Install script dependencies
cd ../../scripts
npm install
```

### 3. Configure GitHub Secrets

Add the following secrets to your repository (Settings → Secrets → Actions):

#### Required Secrets

- `GITHUB_TOKEN` - Usually available by default, but ensure it has:
  - `issues: write` permission
  - `actions: read` permission

#### Optional Secrets (for notifications)

- `SLACK_WEBHOOK` - Slack incoming webhook URL
  ```
  https://hooks.slack.com/services/YOUR/WEBHOOK/URL
  ```

- `SMTP_USER` - Email username for notifications
- `SMTP_PASS` - Email password/app-specific password
- `TEAMS_WEBHOOK` - Microsoft Teams incoming webhook URL

### 4. Configure Workflow Settings

Edit `.github/config/workflow-issue-config.yml` to match your needs:

```yaml
# Example configuration
global:
  enabled: true
  default_assignees: ["your-team"]
  default_labels: ["workflow-failure", "automated"]
  notification_channels:
    slack: "#your-alerts-channel"
    email: ["team@yourcompany.com"]

workflows:
  "Your Workflow Name":
    severity: high
    assignees: ["specific-team"]
    labels: ["ci-cd", "critical"]
    template: "ci-cd"
    escalation_threshold: 3
```

### 5. Update Existing Workflows

Add the failure handler to your existing workflows:

```yaml
name: Your Existing Workflow

on: [push, pull_request]

jobs:
  your-existing-jobs:
    # ... your existing job configuration

  # Add this job at the end
  handle-failure:
    if: failure()
    needs: [your-existing-jobs]  # List all job names to monitor
    uses: ./.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "Your Workflow Name"
      severity: "high"
    secrets:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      SLACK_WEBHOOK: ${{ secrets.SLACK_WEBHOOK }}
```

## Configuration Options

### Workflow Inputs

| Input | Description | Values | Default |
|-------|-------------|--------|---------|
| `workflow-name` | Display name for the workflow | Any string | Required |
| `severity` | Issue severity level | critical, high, medium, low | medium |
| `assignees` | GitHub usernames to assign | Comma-separated list | From config |
| `labels` | Issue labels to apply | Comma-separated list | workflow-failure,bug |
| `template` | Issue template to use | Template name or "auto" | auto |
| `notify-slack` | Send Slack notifications | true/false | true |
| `notify-email` | Send email notifications | true/false | false |
| `dry-run` | Test mode (no issues created) | true/false | false |

### Template Customization

Create custom templates in `.github/templates/workflow-issues/custom/`:

```markdown
## Custom Failure Report for {{workflow.name}}

**Severity:** {{failure.severity}}
**Time:** {{failure.timestamp}}

### Failed Jobs
{{#each failure.jobs}}
- {{name}}: {{conclusion}}
{{/each}}

### Custom Section
Add your custom content here...
```

### Notification Configuration

#### Slack Setup
1. Create Slack webhook: https://api.slack.com/messaging/webhooks
2. Add webhook URL as `SLACK_WEBHOOK` secret
3. Configure channel in workflow-issue-config.yml

#### Email Setup
1. Configure SMTP credentials:
   - `SMTP_USER`: Your email address
   - `SMTP_PASS`: App-specific password
2. Set `SMTP_HOST` and `SMTP_PORT` if not using Gmail
3. Add recipient emails in configuration

#### Teams Setup
1. Create Teams webhook in your channel
2. Add webhook URL as `TEAMS_WEBHOOK` secret

## Testing

### 1. Test Individual Components

```bash
# Test failure detector
cd .github/actions/failure-detector
npm test

# Test scripts
cd .github/scripts
npm test
```

### 2. Test Workflow Integration

Use the test workflow to verify setup:

```bash
# Trigger test workflow
gh workflow run test-failure-handler.yml \
  -f simulate-failure=true \
  -f failure-type=test
```

### 3. Dry Run Mode

Test without creating actual issues:

```yaml
handle-failure:
  uses: ./.github/workflows/workflow-failure-handler.yml
  with:
    workflow-name: "Test Workflow"
    dry-run: true
```

## Monitoring

### Enable Monitoring Dashboard

1. Run the monitoring workflow:
   ```bash
   gh workflow run workflow-failure-monitoring.yml
   ```

2. View reports in Actions artifacts

3. Set up scheduled monitoring:
   ```yaml
   on:
     schedule:
       - cron: '0 */6 * * *'  # Every 6 hours
   ```

### Health Checks

Run health checks:

```bash
cd .github/scripts
node monitoring.js health
```

## Troubleshooting

### Issues Not Being Created

1. **Check Permissions**
   ```yaml
   permissions:
     issues: write
     actions: read
   ```

2. **Verify Secrets**
   ```bash
   gh secret list
   ```

3. **Check Logs**
   - View workflow run logs in Actions tab
   - Look for error messages in failure detector step

### Notification Issues

1. **Test Webhook**
   ```bash
   curl -X POST -H 'Content-type: application/json' \
     --data '{"text":"Test message"}' \
     YOUR_WEBHOOK_URL
   ```

2. **Check SMTP Settings**
   - Verify credentials
   - Check for 2FA/app passwords
   - Test with simple email script

### Template Not Found

1. Verify template exists:
   ```bash
   ls .github/templates/workflow-issues/
   ```

2. Check template name in configuration
3. Ensure file has `.md` extension

## Best Practices

1. **Start with Dry Run**
   - Test configuration with `dry-run: true`
   - Verify issue content before enabling

2. **Configure Thoughtfully**
   - Set appropriate severity levels
   - Don't over-assign to avoid fatigue
   - Use specific templates for better guidance

3. **Monitor Regularly**
   - Review monitoring dashboard weekly
   - Adjust thresholds based on patterns
   - Update templates with common solutions

4. **Maintain Templates**
   - Keep troubleshooting steps current
   - Add links to relevant documentation
   - Include recent fixes and workarounds

## Support

For issues or questions:
1. Check the [troubleshooting guide](TROUBLESHOOTING.md)
2. Review [example configurations](../examples/)
3. Open an issue in the repository