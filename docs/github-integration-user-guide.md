# Kiro GitHub Integration - User Guide

## Overview

The Kiro GitHub Integration provides automated code review capabilities directly within your GitHub workflow. When you assign pull requests to Kiro, it analyzes your code changes and provides intelligent feedback, suggestions, and automated fixes.

## Getting Started

### Prerequisites

- GitHub repository with admin access
- Basic understanding of pull requests and code reviews

### Installation

1. **Install the Kiro GitHub App**
   - Visit the [Kiro GitHub App page](https://github.com/apps/kiro-ai)
   - Click "Install" and select your repositories
   - Grant the necessary permissions

2. **Configure Your Repository**
   - Create a `.kiro/config/github.yml` file in your repository
   - Use the example configuration provided below

### Basic Configuration

Create `.kiro/config/github.yml` in your repository:

```yaml
# Kiro GitHub Integration Configuration
review:
  depth: standard  # Options: basic, standard, thorough
  focus_areas:
    - security     # Focus on security issues
    - performance  # Focus on performance issues
    - style        # Focus on code style
    - documentation # Focus on documentation
  auto_fix: true   # Enable automated fix suggestions
  comment_style: educational  # Options: concise, educational, detailed

rules:
  custom_rules_enabled: true
  rule_sets:
    - name: "Security Rules"
      enabled: true
    - name: "Performance Rules"
      enabled: true
    - name: "Style Guide"
      enabled: true

notifications:
  channels:
    - github  # In-GitHub notifications
  events:
    - review_complete
    - critical_issue
```

## Using Kiro for Code Reviews

### Assigning Pull Requests

1. **Create a Pull Request**
   - Create your PR as usual in GitHub

2. **Assign Kiro as Reviewer**
   - In the PR sidebar, click "Reviewers"
   - Add "kiro-ai" as a reviewer
   - Kiro will automatically start reviewing your code

3. **Request Review via Comment**
   - Comment `/kiro review` in your PR
   - Kiro will begin the review process

### Understanding Kiro's Comments

Kiro provides different types of feedback:

#### Issue Comments
- **🔴 Critical**: Security vulnerabilities, syntax errors
- **🟠 Major**: Performance issues, potential bugs
- **🟡 Minor**: Style violations, minor improvements
- **💡 Suggestion**: Best practice recommendations

#### Fix Suggestions
Kiro provides automated fix suggestions using GitHub's suggestion blocks:

```suggestion
// Improved code goes here
```

You can apply these suggestions directly by clicking "Add suggestion to batch" in GitHub.

### Commands

Use these commands in PR comments to interact with Kiro:

- `/kiro review` - Request a code review
- `/kiro help` - Show available commands
- `/kiro explain [file:line]` - Get explanation for specific code
- `/kiro fix [file:line]` - Request automated fix
- `/apply-suggestion` - Apply a suggested fix

## Advanced Features

### Custom Rules

You can define custom rules for your team:

```yaml
rules:
  rule_sets:
    - name: "Team Standards"
      enabled: true
      rules:
        - id: "no-console-log"
          severity: minor
          description: "Avoid console.log in production code"
          pattern: "console\\.log\\("
          message: "Use proper logging instead of console.log"
```

### Team Coding Standards

Configure team-specific standards:

```yaml
team_standards:
  indentation:
    style: spaces
    size: 2
  line_length: 80
  naming_conventions:
    variables: camelCase
    functions: camelCase
    classes: PascalCase
    constants: UPPER_CASE
```

### Notification Settings

Configure how you receive notifications:

```yaml
notifications:
  channels:
    - github
    - slack:
        webhook_url: "https://hooks.slack.com/services/..."
        channel: "#code-reviews"
    - email:
        recipients:
          - "team@example.com"
  events:
    - review_complete
    - critical_issue
    - fix_applied
```

## Best Practices

### For Developers

1. **Review Kiro's Feedback**
   - Read through all comments carefully
   - Apply suggested fixes when appropriate
   - Provide feedback on suggestions (👍/👎)

2. **Use Commands Effectively**
   - Use `/kiro explain` for unclear issues
   - Use `/kiro fix` for specific problems
   - Use `/apply-suggestion` for automated fixes

3. **Provide Feedback**
   - React to Kiro's comments with emojis
   - Leave comments about suggestion quality
   - This helps Kiro learn and improve

### For Team Leads

1. **Configure Rules Appropriately**
   - Start with standard rules
   - Add custom rules gradually
   - Monitor rule effectiveness

2. **Monitor Analytics**
   - Review suggestion acceptance rates
   - Check for frequently ignored rules
   - Adjust configuration based on team feedback

3. **Train Your Team**
   - Share this guide with team members
   - Conduct training sessions on Kiro usage
   - Establish team conventions for Kiro interaction

## Troubleshooting

### Common Issues

**Kiro doesn't respond to PR assignment**
- Check that the GitHub App is installed
- Verify Kiro has necessary permissions
- Ensure the repository is included in the installation

**Reviews take too long**
- Large PRs take more time to review
- Check your review depth setting
- Consider breaking large PRs into smaller ones

**Too many false positives**
- Adjust rule severity levels
- Disable problematic rules
- Provide feedback to help Kiro learn

**Suggestions don't apply cleanly**
- Code may have changed since suggestion was made
- Apply suggestions manually if needed
- Refresh the PR if conflicts occur

### Getting Help

1. **Check the Documentation**
   - Review this guide thoroughly
   - Check the API documentation
   - Look at example configurations

2. **Use Built-in Help**
   - Comment `/kiro help` in any PR
   - Check the GitHub App settings page

3. **Contact Support**
   - Create an issue in the repository
   - Contact your system administrator
   - Check the troubleshooting guide

## Privacy and Security

### Data Handling

- Kiro only processes code that you explicitly assign for review
- Code analysis happens in secure, isolated environments
- No code is stored permanently after review completion
- All data processing complies with GDPR and privacy regulations

### Permissions

Kiro requires these GitHub permissions:
- **Pull requests**: Read & write (to comment on PRs)
- **Contents**: Read (to analyze code)
- **Issues**: Read (to understand context)
- **Metadata**: Read (to access repository information)

### Security Features

- All webhook communications are signed and verified
- Access tokens are encrypted and regularly rotated
- Audit logs track all Kiro activities
- Data is encrypted in transit and at rest

## Feedback and Improvement

Kiro learns from your feedback to provide better suggestions over time:

- **Positive Feedback**: 👍 reactions, "helpful" comments
- **Negative Feedback**: 👎 reactions, "not helpful" comments
- **Specific Feedback**: Detailed comments about suggestion quality

Your feedback helps Kiro:
- Improve suggestion accuracy
- Learn team preferences
- Adapt to coding standards
- Reduce false positives

## Conclusion

The Kiro GitHub Integration streamlines your code review process by providing intelligent, automated feedback. By following this guide and configuring Kiro appropriately for your team, you can significantly improve code quality while reducing manual review overhead.

For more advanced configuration options and API documentation, see the [Administrator Guide](github-integration-admin-guide.md) and [API Documentation](github-integration-api.md).