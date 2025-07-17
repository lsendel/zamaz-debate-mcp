# Using Kiro Reviews

This guide covers how to effectively interact with Kiro's code review capabilities.

## Requesting Reviews

### Automatic Reviews

If your repository is configured for automatic reviews, Kiro will review PRs when:

- PR is created with specific labels (e.g., `kiro-review`)
- PR title contains trigger keywords (e.g., `[kiro]`)
- PR targets specific branches (e.g., `main`, `develop`)
- PR author is in a specific team

### Manual Reviews

#### Assigning Kiro as Reviewer

1. Open your pull request
2. Click "Reviewers" in the right sidebar
3. Type "kiro" and select from the dropdown
4. Kiro will start reviewing immediately

#### Mentioning Kiro in Comments

Use these commands in PR comments:

```markdown
@kiro review this PR
```

```markdown
@kiro please check the security of this authentication code
```

```markdown
@kiro focus on performance in the database queries
```

### Review Scope Control

#### Focusing on Specific Areas

```markdown
@kiro please review only the files in src/auth/
```

```markdown
@kiro check for security issues in this PR
```

```markdown
@kiro review the performance implications of these changes
```

#### Excluding Areas

```markdown
@kiro ignore the test files, focus on production code
```

```markdown
@kiro skip style issues, focus on logic bugs
```

## Understanding Review Results

### Review Summary

Kiro provides a summary comment at the top level:

```markdown
## 🤖 Kiro Review Summary

**Files Reviewed**: 8 files, 247 lines changed
**Issues Found**: 2 critical, 1 major, 3 minor, 5 suggestions
**Auto-fixable**: 4 issues can be automatically fixed

### Critical Issues 🚨
- SQL injection vulnerability in auth.js:42
- Hardcoded API key in config.js:15

### Major Issues ⚠️
- Memory leak in event handler (utils.js:89)

**Recommendation**: Address critical and major issues before merging.
```

### File-Level Comments

Kiro adds comments directly on the relevant lines:

#### Security Issues
```markdown
🔒 **Security Issue**: Potential XSS vulnerability

This user input is rendered without sanitization, which could allow 
script injection attacks.

**Fix**: Use a sanitization library:
```javascript
import DOMPurify from 'dompurify';
const cleanInput = DOMPurify.sanitize(userInput);
```

**Apply Fix** | **Learn More**
```

#### Performance Issues
```markdown
⚡ **Performance**: Inefficient database query

This query runs inside a loop, causing N+1 query problem.

**Impact**: Could slow down page load by 2-3 seconds with large datasets.

**Fix**: Use batch loading or JOIN query:
```sql
-- Instead of multiple queries:
SELECT * FROM users WHERE team_id = ?

-- Use a single query:
SELECT u.*, t.name as team_name 
FROM users u 
JOIN teams t ON u.team_id = t.id 
WHERE u.team_id IN (?)
```

**Apply Fix** | **Learn More**
```

#### Code Quality Issues
```markdown
🧹 **Code Quality**: Complex function needs refactoring

This function has high cyclomatic complexity (12). Consider breaking 
it into smaller functions.

**Benefits**:
- Easier to test
- Better maintainability  
- Reduced bug risk

**Suggestion**: Extract validation logic into separate functions.

**Learn More**
```

## Interacting with Kiro

### Applying Automated Fixes

For simple issues, Kiro provides one-click fixes:

1. **Single Fix**: Click "Apply suggestion" button
2. **Multiple Fixes**: Use "Apply all suggestions" for the file
3. **Batch Fixes**: Apply all auto-fixable issues at once

#### What Gets Fixed Automatically

- Code formatting and style issues
- Simple refactoring (variable renaming, etc.)
- Adding missing imports or dependencies
- Basic security fixes (sanitization, validation)

#### What Requires Manual Fixes

- Complex logic changes
- Architecture modifications
- Breaking API changes
- Business logic decisions

### Providing Feedback to Kiro

#### Marking Comments as Helpful

Use GitHub's reaction system:
- 👍 for helpful comments
- 👎 for unhelpful comments
- ❤️ for particularly valuable insights

#### Detailed Feedback

Reply to Kiro's comments with context:

```markdown
@kiro This suggestion doesn't work because we need to maintain 
backward compatibility with the v1 API. Can you suggest an 
alternative approach?
```

```markdown
@kiro Great catch! This would have been a production bug. 
Applied the fix.
```

### Re-requesting Reviews

After making changes, you can ask for a follow-up review:

```markdown
@kiro I've addressed the security issues. Please review the changes.
```

```markdown
@kiro please check if the performance fix resolves the N+1 query issue
```

## Advanced Usage

### Custom Review Templates

Create `.kiro/review-template.md` in your repository:

```markdown
## Review Focus Areas

Please pay special attention to:
- [ ] Security vulnerabilities
- [ ] Performance implications  
- [ ] API contract changes
- [ ] Database migration safety

## Context

This PR is part of the user authentication refactor project.
Related documentation: docs/auth-architecture.md
```

### Integration with CI/CD

#### Blocking Merges on Critical Issues

Configure branch protection rules to require Kiro approval:

1. Go to repository Settings > Branches
2. Add rule for your main branch
3. Enable "Require review from code owners"
4. Add Kiro as a code owner in `CODEOWNERS` file

#### Status Checks

Kiro can provide status checks that integrate with your CI:

- ✅ **No critical issues found**
- ❌ **Critical issues must be resolved**
- ⚠️ **Major issues found - review recommended**

### Team Workflows

#### Code Review Process

1. **Developer** creates PR and requests Kiro review
2. **Kiro** provides initial feedback within minutes
3. **Developer** addresses critical and major issues
4. **Human reviewer** focuses on business logic and architecture
5. **Team** merges after both reviews pass

#### Learning from Reviews

- Track common issues in team retrospectives
- Update coding standards based on Kiro feedback
- Share particularly valuable Kiro insights with the team

## Troubleshooting Reviews

### Review Not Starting

1. Check if Kiro has repository access
2. Verify PR is not in draft mode
3. Ensure repository has Kiro integration enabled
4. Check for rate limiting or service issues

### Incomplete Reviews

1. Large PRs may timeout - consider splitting
2. Check for unsupported file types
3. Verify network connectivity
4. Look for error messages in PR comments

### Unexpected Feedback

1. Provide context in PR description
2. Use review focus commands
3. Check repository configuration
4. Report false positives to improve Kiro

## Best Practices

### For Better Reviews

1. **Write clear PR descriptions** with context and goals
2. **Link related issues** for better understanding
3. **Keep PRs focused** on single features or fixes
4. **Include tests** to demonstrate expected behavior
5. **Respond to feedback** to help Kiro learn

### For Team Adoption

1. **Start with non-critical repositories** to build confidence
2. **Train team members** on effective Kiro usage
3. **Customize settings** to match team preferences
4. **Monitor analytics** to measure impact
5. **Iterate on configuration** based on feedback

## Next Steps

- [Understanding Kiro's feedback types](understanding-feedback.md)
- [Best practices for teams](best-practices.md)
- [Configuring repository settings](../admin-guide/configuration.md)