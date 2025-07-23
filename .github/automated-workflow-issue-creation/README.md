# Automated Workflow Issue Creation

This system automatically creates GitHub issues when CI/CD workflows fail, providing detailed failure information, troubleshooting steps, and automated notifications to the right team members.

## Features

- 🚨 **Automatic Issue Creation**: Creates detailed GitHub issues when workflows fail
- 🔍 **Duplicate Detection**: Updates existing issues instead of creating duplicates
- 📧 **Multi-Channel Notifications**: Supports Slack, Email, and Microsoft Teams
- 🎯 **Smart Categorization**: Automatically categorizes failures and applies appropriate labels
- 📝 **Custom Templates**: Different issue templates for different types of failures
- ⚡ **Escalation Support**: Escalates repeated failures to senior team members
- 🔧 **Highly Configurable**: Workflow-specific settings and customizations

## Quick Start

### 1. Basic Integration

Add this job to any workflow to enable automatic issue creation on failure:

```yaml
jobs:
  # Your existing jobs...
  
  handle-failure:
    if: failure()
    needs: [your-job-1, your-job-2]  # List all jobs to monitor
    uses: ./.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "My Workflow"
      severity: "medium"
    secrets:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### 2. Advanced Integration with Notifications

```yaml
handle-failure:
  if: failure()
  needs: [build, test, deploy]
  uses: ./.github/workflows/workflow-failure-handler.yml
  with:
    workflow-name: "CI/CD Pipeline"
    severity: ${{ github.ref == 'refs/heads/main' && 'critical' || 'high' }}
    assignees: "backend-team,devops-team"
    labels: "ci-cd,workflow-failure,urgent"
    template: "ci-cd"
    notify-slack: true
    notify-email: ${{ github.ref == 'refs/heads/main' }}
  secrets:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    SLACK_WEBHOOK: ${{ secrets.SLACK_WEBHOOK }}
    SMTP_USER: ${{ secrets.SMTP_USER }}
    SMTP_PASS: ${{ secrets.SMTP_PASS }}
```

## Configuration

### Workflow Configuration File

Configure workflow-specific settings in `.github/config/workflow-issue-config.yml`:

```yaml
workflows:
  "My Critical Workflow":
    severity: critical
    assignees: ["oncall-team", "team-lead"]
    labels: ["critical", "production", "workflow-failure"]
    template: "deployment"
    escalation_threshold: 1
    notification_channels:
      slack: "#critical-alerts"
      email: ["oncall@company.com"]
```

### Available Templates

- **default**: General workflow failures
- **ci-cd**: Build and CI/CD pipeline failures
- **security**: Security scanning failures
- **linting**: Code quality and linting failures
- **deployment**: Deployment and release failures
- **custom**: Create your own templates in `.github/templates/workflow-issues/custom/`

## Inputs

| Input | Description | Required | Default |
|-------|-------------|----------|---------|
| `workflow-name` | Name of the workflow that failed | Yes | - |
| `failure-context` | Additional context about the failure | No | `auto-detect` |
| `severity` | Failure severity (critical, high, medium, low) | No | `medium` |
| `assignees` | Comma-separated list of GitHub usernames | No | From config |
| `labels` | Comma-separated list of labels | No | `workflow-failure,bug` |
| `template` | Template to use for issue creation | No | `auto` |
| `notify-slack` | Send Slack notifications | No | `true` |
| `notify-email` | Send email notifications | No | `false` |
| `dry-run` | Run without creating issues (testing) | No | `false` |

## Required Secrets

| Secret | Description | Required |
|--------|-------------|----------|
| `GITHUB_TOKEN` | GitHub token for creating issues | Yes |
| `SLACK_WEBHOOK` | Slack webhook URL | No |
| `SMTP_USER` | SMTP username for emails | No |
| `SMTP_PASS` | SMTP password for emails | No |
| `TEAMS_WEBHOOK` | Microsoft Teams webhook | No |

## Example Issues Created

### CI/CD Pipeline Failure

![CI/CD Failure Issue](docs/images/cicd-issue-example.png)

The system creates comprehensive issues with:
- Detailed failure information
- Links to workflow runs and commits
- Error logs and stack traces
- Troubleshooting steps specific to the failure type
- Proper labeling and assignment

### Security Scan Failure

![Security Failure Issue](docs/images/security-issue-example.png)

Security failures get special treatment with:
- Vulnerability details
- Compliance impact assessment
- Required remediation steps
- Escalation procedures

## Advanced Features

### Duplicate Detection

The system automatically detects existing open issues for the same workflow and updates them instead of creating duplicates:

```
workflow:CI/CD Pipeline
failure-type:build-failure
```

### Escalation

Configure escalation thresholds to notify senior team members when workflows fail repeatedly:

```yaml
workflows:
  "Production Deploy":
    escalation_threshold: 1  # Escalate immediately
    notification_channels:
      slack: "#production-alerts"
      email: ["oncall@company.com", "cto@company.com"]
```

### Custom Templates

Create custom templates for specific workflows:

1. Create a template file: `.github/templates/workflow-issues/custom/my-workflow.md`
2. Use Handlebars syntax for variables: `{{workflow.name}}`, `{{failure.jobs}}`
3. Reference in configuration:
   ```yaml
   workflows:
     "My Workflow":
       template: "custom/my-workflow"
   ```

## Troubleshooting

### Issues Not Being Created

1. Check workflow permissions:
   ```yaml
   permissions:
     issues: write
     actions: read
   ```

2. Verify the failure detection job has correct dependencies:
   ```yaml
   needs: [all, your, jobs]
   ```

3. Check GitHub token has required permissions

### Notifications Not Working

1. Verify webhook URLs are correct and active
2. Check secret values are properly set
3. Review notification throttling settings
4. Check workflow logs for notification errors

### Template Not Found

1. Ensure template file exists in correct location
2. Check template name in configuration
3. Falls back to default template if custom not found

## Best Practices

1. **Use Appropriate Severity Levels**
   - `critical`: Production failures, data loss risks
   - `high`: Build failures, blocking deployments
   - `medium`: Test failures, code quality issues
   - `low`: Documentation, non-blocking issues

2. **Configure Assignees Thoughtfully**
   - Assign to teams rather than individuals when possible
   - Use rotation for on-call assignments
   - Don't over-assign to avoid notification fatigue

3. **Leverage Templates**
   - Use specific templates for better troubleshooting guidance
   - Customize templates for your team's workflows
   - Include links to runbooks and documentation

4. **Set Up Escalation Properly**
   - Lower thresholds for critical workflows
   - Configure escalation contacts in advance
   - Test escalation paths regularly

## Contributing

To contribute to this system:

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Update documentation
5. Submit a pull request

## License

This automated workflow issue creation system is part of the project and follows the same license terms.