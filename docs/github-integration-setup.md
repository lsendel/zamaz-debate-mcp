# GitHub Integration Setup Guide

This guide explains how to set up the Kiro GitHub integration, which allows you to assign pull requests to Kiro for automated code review and suggestions.

## Creating a GitHub App

1. Go to your GitHub organization settings (or personal settings)
2. Navigate to "Developer settings" > "GitHub Apps" > "New GitHub App"
3. Fill in the following information:
   - **GitHub App name**: Kiro AI
   - **Homepage URL**: Your organization's website or repository URL
   - **Webhook URL**: `https://your-kiro-server.example.com/api/webhooks/github`
   - **Webhook secret**: Generate a secure random string (save this for later)

4. Set the following permissions:
   - **Repository permissions**:
     - **Pull requests**: Read & write (to comment on PRs)
     - **Contents**: Read & write (to read code and create fix commits)
     - **Issues**: Read (to read linked issues for context)
     - **Metadata**: Read (to access repository metadata)
     - **Workflows**: Read (to understand CI/CD context)

5. Subscribe to the following events:
   - **Pull request**
   - **Pull request review**
   - **Issue comment**
   - **Push**

6. Choose where the app can be installed:
   - Any account (for public apps)
   - Only this account (for private use)

7. Click "Create GitHub App"

8. After creation, note down the following:
   - **App ID**: Shown on the app settings page
   - **Client ID**: Shown on the app settings page
   - **Client secret**: Generate and save this
   - **Private key**: Generate and download this

## Configuring the Kiro Server

1. Set up the following environment variables on your Kiro server:
   ```
   GITHUB_APP_ID=your-app-id
   GITHUB_CLIENT_ID=your-client-id
   GITHUB_CLIENT_SECRET=your-client-secret
   GITHUB_WEBHOOK_SECRET=your-webhook-secret
   ```

2. Store the private key securely:
   - Option 1: Store as an environment variable (base64 encoded)
     ```
     GITHUB_PRIVATE_KEY=$(cat path/to/private-key.pem | base64)
     ```
   - Option 2: Store in a secure vault (recommended for production)
   - Option 3: Store in a Kubernetes secret

## Installing the GitHub App

1. Go to the GitHub App's public page: `https://github.com/apps/your-app-name`
2. Click "Install"
3. Choose which repositories to install the app on
4. Complete the installation

## Using the GitHub App

### Assigning PRs to Kiro

1. Create a pull request as normal
2. Add "Kiro AI" as a reviewer
3. Kiro will automatically analyze the code and add comments

### Converting Issues to Specs

1. Create a GitHub issue describing the feature
2. Add the label `kiro-spec` to the issue
3. Optionally, add the label `create-sub-issues` if you want Kiro to create sub-issues for each task
4. Kiro will create a spec and open a PR with the spec files

### Configuration

You can configure Kiro's behavior by adding a `.kiro/config/github.yml` file to your repository:

```yaml
# Kiro GitHub Integration Configuration

# Review settings
review:
  depth: thorough  # Options: basic, standard, thorough
  focus_areas:
    - security
    - performance
    - style
    - documentation
  auto_fix: true  # Whether to suggest automated fixes
  comment_style: educational  # Options: concise, educational, detailed

# Rules configuration
rules:
  custom_rules_enabled: true
  rule_sets:
    - name: "Security Rules"
      enabled: true
    - name: "Performance Rules"
      enabled: true
    - name: "Style Guide"
      enabled: true

# Notifications
notifications:
  channels:
    - slack
    - email
    - github
  events:
    - review_complete
    - critical_issue
    - fix_applied
```

## Troubleshooting

### Webhook Delivery Issues

1. Check the webhook delivery logs in GitHub App settings
2. Verify your server is accessible from GitHub's IP ranges
3. Confirm the webhook secret matches

### Authentication Problems

1. Verify the App ID and private key are correct
2. Check that the installation is active
3. Ensure the app has the necessary permissions

### Review Not Starting

1. Confirm the PR is assigned to Kiro
2. Check if the repository is in the app's installation list
3. Verify the webhook events are being received

## Security Considerations

- The GitHub App private key should be kept secure and rotated regularly
- Use the principle of least privilege when setting up permissions
- Consider implementing IP allowlisting for webhook endpoints
- Audit the app's activities regularly through GitHub's audit logs