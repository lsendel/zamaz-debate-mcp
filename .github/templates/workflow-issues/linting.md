# Code Quality Failure: {{workflow.name}}

## 📋 Code Quality Issues Detected
The code quality workflow **{{workflow.name}}** has detected issues that need to be addressed.

## Failure Details
- **Workflow**: {{workflow.name}}
- **Severity**: {{workflow.severity}}
- **Timestamp**: {{failure.timestamp}}
- **Branch**: {{workflow.branch}}
- **Commit**: {{workflow.commit.sha}} by {{workflow.commit.author}}

## Quality Issues
{{#each failure.jobs}}
### {{name}}
{{#each steps}}
- **{{name}}**: {{conclusion}}
  {{#if errorMessage}}
  ```
  {{errorMessage}}
  ```
  {{/if}}
{{/each}}
{{/each}}

## Common Fixes
- [ ] Run linting tools locally: `npm run lint` or `mvn checkstyle:check`
- [ ] Auto-fix issues where possible: `npm run lint:fix`
- [ ] Review code style guidelines
- [ ] Check for unused imports or variables
- [ ] Verify proper formatting and indentation

## Linting Configuration
- [ ] Check `.eslintrc.js` or `checkstyle.xml` configuration
- [ ] Verify linting rules are up to date
- [ ] Review any recent changes to linting configuration
- [ ] Ensure all team members use the same linting setup

## Code Quality Resources
- [Project Style Guide](./docs/style-guide.md)
- [Linting Configuration](./linting/config/)
- [Code Quality Standards](./docs/code-quality.md)

## Links
- [Workflow Run]({{workflow.url}})
- [Commit]({{workflow.commit.url}})
{{#if context.pullRequest}}
- [Pull Request #{{context.pullRequest.number}}]({{context.pullRequest.url}})
{{/if}}

---
*This issue was automatically created by the Workflow Failure Handler*