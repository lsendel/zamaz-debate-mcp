# Workflow Issue Configuration

This directory contains the configuration system for automated workflow failure issue creation.

## Configuration Files

### `workflow-issue-config.yml`
Main configuration file that defines:
- Global settings for the issue creation system
- Workflow-specific configurations
- Notification settings
- Template configurations

### `schema.json`
JSON schema for validating the configuration file structure and values.

## Configuration Structure

### Global Configuration
```yaml
global:
  enabled: true                           # Enable/disable the entire system
  default_assignees: ["team-lead"]        # Default assignees for all workflows
  default_labels: ["workflow-failure"]    # Default labels for all issues
  notification_channels:                  # Default notification channels
    slack: "#ci-cd-alerts"
    email: ["team@company.com"]
  escalation_threshold: 3                 # Default escalation threshold
  duplicate_detection: true               # Enable duplicate issue detection
```

### Workflow-Specific Configuration
```yaml
workflows:
  "Workflow Name":
    severity: critical                    # critical, high, medium, low
    assignees: ["specific-team"]          # Override default assignees
    labels: ["custom-label"]              # Override default labels
    template: "custom-template"           # Template to use for issues
    escalation_threshold: 2               # Override default threshold
    notification_channels:                # Override notification channels
      slack: "#specific-alerts"
      email: ["specific@company.com"]
    enabled: true                         # Enable/disable for this workflow
```

### Notification Configuration
```yaml
notifications:
  enabled: true                          # Enable notifications
  throttle_minutes: 30                   # Minimum time between notifications
  escalation_delay_minutes: 60           # Time before escalating
  max_notifications_per_hour: 10         # Rate limiting
```

### Template Configuration
```yaml
templates:
  default_template: "default"            # Default template name
  custom_template_path: ".github/templates/workflow-issues/custom/"
```

## Severity Levels

- **critical**: Immediate attention required (deployments, security)
- **high**: Important but not blocking (builds, major features)
- **medium**: Standard priority (tests, code quality)
- **low**: Minor issues (documentation, non-critical linting)

## Template Types

Available built-in templates:
- `default`: Generic workflow failure template
- `ci-cd`: CI/CD pipeline specific template
- `security`: Security scanning template
- `linting`: Code quality and linting template
- `deployment`: Deployment failure template

## Configuration Validation

Use the validation script to check your configuration:

```bash
node .github/scripts/validate-config.js
```

For detailed testing:
```bash
node .github/scripts/validate-config.js --test
```

## Example Configurations

### Basic Setup
```yaml
global:
  enabled: true
  default_assignees: ["devops-team"]
  default_labels: ["workflow-failure", "bug"]

workflows:
  "CI Pipeline":
    severity: critical
    template: "ci-cd"
```

### Advanced Setup with Notifications
```yaml
global:
  enabled: true
  default_assignees: ["team-lead"]
  notification_channels:
    slack: "#general-alerts"

workflows:
  "Production Deploy":
    severity: critical
    assignees: ["devops-team", "team-lead"]
    labels: ["deployment", "critical"]
    template: "deployment"
    escalation_threshold: 1
    notification_channels:
      slack: "#production-alerts"
      email: ["oncall@company.com"]

  "Security Scan":
    severity: high
    assignees: ["security-team"]
    template: "security"
    notification_channels:
      slack: "#security-alerts"

notifications:
  enabled: true
  throttle_minutes: 15
  max_notifications_per_hour: 20
```

## Best Practices

1. **Start Simple**: Begin with global configuration and add workflow-specific settings as needed
2. **Use Appropriate Severity**: Match severity to business impact
3. **Configure Notifications**: Set up appropriate channels for different workflow types
4. **Test Configuration**: Always validate configuration changes
5. **Document Custom Templates**: If using custom templates, document their purpose and usage