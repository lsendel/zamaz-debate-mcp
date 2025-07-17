# Getting Started with Kiro GitHub Integration

This guide will help you start using Kiro for automated code reviews in your GitHub workflow.

## Prerequisites

- Access to a GitHub repository with Kiro integration installed
- Basic familiarity with GitHub pull requests
- Understanding of your team's code review process

## Your First Kiro Review

### Step 1: Create a Pull Request

Create a pull request as you normally would:

```bash
git checkout -b feature/my-new-feature
# Make your changes
git add .
git commit -m "Add new feature"
git push origin feature/my-new-feature
```

Then create a PR through GitHub's interface.

### Step 2: Request a Kiro Review

There are several ways to request a Kiro review:

#### Method 1: Assign Kiro as Reviewer
1. In your pull request, click "Reviewers" in the right sidebar
2. Search for and select "Kiro" from the list
3. Kiro will automatically start reviewing your changes

#### Method 2: Mention Kiro in Comments
Comment on your PR with:
```
@kiro please review this PR
```

#### Method 3: Request Review (if configured)
If your repository is configured for automatic reviews, simply:
1. Add the label `kiro-review` to your PR, or
2. Include `[kiro]` in your PR title

### Step 3: Wait for Review

Kiro typically completes reviews within:
- **Small PRs** (< 100 lines): 2-5 minutes
- **Medium PRs** (100-500 lines): 5-15 minutes  
- **Large PRs** (> 500 lines): 15-30 minutes

You'll receive a GitHub notification when the review is complete.

### Step 4: Review Kiro's Feedback

Kiro will post comments directly on your PR with:

- **Issue Identification**: Problems found in your code
- **Severity Levels**: Critical, Major, Minor, or Suggestions
- **Explanations**: Why the issue matters and how to fix it
- **Code Suggestions**: Specific fixes you can apply

## Understanding Kiro's Comments

### Comment Structure

Each Kiro comment includes:

```markdown
🔍 **Issue Type**: Security Vulnerability
⚠️ **Severity**: Critical
📍 **Location**: src/auth.js:42

**Problem**: Potential SQL injection vulnerability

**Explanation**: Direct string concatenation in SQL queries can allow 
attackers to inject malicious SQL code.

**Suggestion**:
```javascript
// Instead of:
const query = "SELECT * FROM users WHERE id = " + userId;

// Use parameterized queries:
const query = "SELECT * FROM users WHERE id = ?";
db.query(query, [userId]);
```

**Learn More**: [OWASP SQL Injection Prevention](https://owasp.org/...)
```

### Severity Levels

- 🚨 **Critical**: Security vulnerabilities, breaking changes
- ⚠️ **Major**: Bugs, performance issues, maintainability problems  
- ℹ️ **Minor**: Style issues, minor improvements
- 💡 **Suggestion**: Optional improvements, best practices

### Applying Suggestions

For simple fixes, Kiro provides "Apply suggestion" buttons:

1. Click the "Apply suggestion" button in the comment
2. Kiro will create a new commit with the fix
3. The fix is automatically pushed to your PR branch

For complex changes, follow the provided guidance manually.

## Working with Kiro Effectively

### Best Practices

1. **Provide Context**: Include detailed PR descriptions
2. **Link Issues**: Reference related GitHub issues in your PR
3. **Small PRs**: Smaller PRs get faster, more focused reviews
4. **Respond to Feedback**: Mark comments as resolved when addressed

### PR Description Template

Use this template for better Kiro reviews:

```markdown
## What This PR Does
Brief description of the changes

## Related Issues
Fixes #123
Related to #456

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests pass
- [ ] Manual testing completed

## Special Considerations
Any specific areas you want Kiro to focus on or ignore
```

### Getting Focused Reviews

You can guide Kiro's review focus:

```markdown
@kiro please focus on security and performance in this PR
```

```markdown
@kiro please review the authentication logic in src/auth/
```

## Common Workflows

### Feature Development
1. Create feature branch
2. Implement feature with tests
3. Create PR with detailed description
4. Request Kiro review
5. Address feedback
6. Merge when approved

### Bug Fixes
1. Create fix branch
2. Implement fix
3. Add regression test
4. Create PR linking to bug report
5. Request Kiro review for security/edge cases
6. Merge after review

### Refactoring
1. Create refactor branch
2. Make incremental changes
3. Create PR explaining refactoring goals
4. Request Kiro review for maintainability
5. Address suggestions
6. Merge when clean

## Next Steps

- [Learn about Kiro's feedback types](understanding-feedback.md)
- [Discover advanced usage patterns](best-practices.md)
- [Configure team settings](../admin-guide/configuration.md)

## Need Help?

- Check the [troubleshooting guide](../troubleshooting/common-issues.md)
- Ask questions in your team's Slack channel
- Contact support at support@kiro.dev