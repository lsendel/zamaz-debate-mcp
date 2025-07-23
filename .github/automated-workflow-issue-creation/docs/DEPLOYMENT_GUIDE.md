# Deployment Guide - Automated Workflow Issue Creation

This guide covers deploying and maintaining the automated workflow issue creation system across different environments.

## Deployment Strategies

### 1. Gradual Rollout (Recommended)

Start with non-critical workflows and gradually expand:

```yaml
# Phase 1: Test workflows only
- test-failure-handler.yml

# Phase 2: Non-critical workflows
- code-quality.yml
- documentation.yml

# Phase 3: Critical workflows
- ci-cd-pipeline.yml
- deployment.yml
```

### 2. Per-Environment Deployment

Deploy to different environments progressively:

```yaml
# Development environment
on:
  push:
    branches: [develop]
    
# Staging environment  
on:
  push:
    branches: [staging]
    
# Production environment
on:
  push:
    branches: [main, master]
```

## Pre-Deployment Checklist

### ✅ Required Steps

- [ ] All required files copied to repository
- [ ] Dependencies installed in action and scripts directories
- [ ] GitHub secrets configured
- [ ] Basic configuration file created
- [ ] Test workflow runs successfully
- [ ] Dry run mode tested

### ✅ Recommended Steps

- [ ] Custom templates created for your workflows
- [ ] Notification channels configured and tested
- [ ] Team assignments reviewed
- [ ] Escalation paths defined
- [ ] Monitoring dashboard set up
- [ ] Documentation shared with team

## Environment-Specific Configuration

### Development Environment

```yaml
# .github/config/workflow-issue-config-dev.yml
global:
  enabled: true
  default_assignees: ["dev-team"]
  default_labels: ["workflow-failure", "dev"]
  notification_channels:
    slack: "#dev-alerts"
    
workflows:
  "Dev Pipeline":
    severity: low  # Lower severity for dev
    assignees: ["dev-oncall"]
    escalation_threshold: 10  # Higher threshold
```

### Production Environment

```yaml
# .github/config/workflow-issue-config-prod.yml
global:
  enabled: true
  default_assignees: ["prod-team", "oncall"]
  default_labels: ["workflow-failure", "production", "urgent"]
  notification_channels:
    slack: "#prod-alerts"
    email: ["oncall@company.com", "management@company.com"]
    teams: true
    
workflows:
  "Production Deploy":
    severity: critical
    assignees: ["prod-oncall", "team-lead", "sre-team"]
    escalation_threshold: 1  # Immediate escalation
```

## Secret Management

### Using GitHub Environments

```yaml
# In your workflow
jobs:
  handle-failure:
    environment: production  # Use environment-specific secrets
    uses: ./.github/workflows/workflow-failure-handler.yml
    secrets:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      SLACK_WEBHOOK: ${{ secrets.PROD_SLACK_WEBHOOK }}
```

### Secret Rotation

1. Create new secret with temporary name:
   ```bash
   gh secret set SLACK_WEBHOOK_NEW
   ```

2. Update workflows to use new secret
3. Delete old secret:
   ```bash
   gh secret delete SLACK_WEBHOOK
   ```

4. Rename new secret:
   ```bash
   gh secret set SLACK_WEBHOOK
   ```

## Performance Optimization

### 1. Caching Dependencies

```yaml
# In failure-detector action
- name: Cache node modules
  uses: actions/cache@v3
  with:
    path: ~/.npm
    key: ${{ runner.os }}-node-${{ hashFiles('**/package-lock.json') }}
```

### 2. Conditional Execution

```yaml
# Only run for main branch failures
handle-failure:
  if: failure() && github.ref == 'refs/heads/main'
  uses: ./.github/workflows/workflow-failure-handler.yml
```

### 3. Resource Limits

```yaml
# Set appropriate resource limits
jobs:
  handle-failure:
    runs-on: ubuntu-latest
    timeout-minutes: 5  # Prevent hanging
```

## Monitoring Deployment

### 1. Initial Deployment Metrics

Monitor these metrics after deployment:

- Issue creation success rate
- Average time to create issue
- Notification delivery rate
- False positive rate
- Duplicate issue rate

### 2. Set Up Dashboards

```yaml
# Enable monitoring workflow
on:
  schedule:
    - cron: '0 */6 * * *'  # Every 6 hours
  workflow_dispatch:  # Manual trigger
```

### 3. Alert Configuration

```javascript
// Configure alerts for deployment issues
const alertThresholds = {
  failureRate: 0.1,      // Alert if >10% of workflows fail
  issueCreationTime: 5000, // Alert if takes >5 seconds
  notificationFailure: 0.2 // Alert if >20% notifications fail
};
```

## Rollback Procedures

### 1. Quick Disable

Add kill switch to configuration:

```yaml
# In workflow-issue-config.yml
global:
  enabled: false  # Disables all issue creation
```

### 2. Partial Rollback

Disable for specific workflows:

```yaml
workflows:
  "Problem Workflow":
    enabled: false  # Disable just this workflow
```

### 3. Full Rollback

```bash
# Revert to previous version
git revert HEAD
git push

# Or remove the failure handler job from workflows
```

## Maintenance

### Regular Tasks

#### Daily
- Check monitoring dashboard for anomalies
- Review any escalated issues
- Verify notification delivery

#### Weekly
- Review issue creation patterns
- Update assignee lists if needed
- Check for workflow name changes

#### Monthly
- Audit template effectiveness
- Review and update troubleshooting steps
- Rotate secrets if required
- Update documentation

### Updating Components

#### Update Action
```bash
cd .github/actions/failure-detector
npm update
npm test
```

#### Update Scripts
```bash
cd .github/scripts
npm update
npm test
```

#### Update Templates
1. Test new template locally
2. Deploy to dev environment
3. Monitor for issues
4. Deploy to production

## Security Considerations

### 1. Token Permissions

Use minimal required permissions:

```yaml
permissions:
  issues: write
  actions: read
  contents: read  # Only if needed
```

### 2. Secret Protection

- Use GitHub Environments for production secrets
- Enable secret scanning
- Rotate secrets regularly
- Never log secret values

### 3. Input Validation

The system validates all inputs, but ensure:
- Workflow names don't contain injection attempts
- Template variables are properly escaped
- User inputs are sanitized

## Scaling Considerations

### High-Volume Repositories

For repos with many workflows:

1. **Implement Queuing**
   ```javascript
   // Add to issue-manager.js
   const queue = [];
   const MAX_CONCURRENT = 5;
   ```

2. **Batch Similar Failures**
   ```yaml
   # Group by time window
   batch_window_minutes: 5
   ```

3. **Use Dedicated Runners**
   ```yaml
   runs-on: [self-hosted, failure-handler]
   ```

### Multiple Repositories

To deploy across organization:

1. Create reusable workflow in `.github` repo
2. Reference from other repos:
   ```yaml
   uses: your-org/.github/.github/workflows/failure-handler.yml@main
   ```

## Success Metrics

Track these KPIs:

1. **Mean Time to Detection (MTTD)**
   - Time from failure to issue creation
   - Target: < 2 minutes

2. **Issue Resolution Time**
   - Time from creation to closure
   - Track by severity level

3. **Automation Rate**
   - % of failures with issues created
   - Target: > 95%

4. **False Positive Rate**
   - % of issues closed as "not a problem"
   - Target: < 5%

5. **Notification Effectiveness**
   - % of critical issues acknowledged within SLA
   - Target: > 90%

## Post-Deployment Support

### Training Materials

1. Create team documentation
2. Record demo video
3. Set up office hours
4. Create FAQ document

### Feedback Loop

1. Survey users after 1 month
2. Track feature requests
3. Monitor issue comments
4. Iterate on templates

### Continuous Improvement

1. Analyze failure patterns
2. Update templates with solutions
3. Optimize notification rules
4. Refine escalation thresholds