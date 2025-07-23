# CI/CD Pipeline Failure: {{workflow.name}}

## 🚨 Critical Pipeline Failure
The CI/CD pipeline **{{workflow.name}}** has failed and requires immediate attention.

## Failure Details
- **Pipeline**: {{workflow.name}}
- **Severity**: {{workflow.severity}}
- **Timestamp**: {{failure.timestamp}}
- **Branch**: {{workflow.branch}}
- **Commit**: {{workflow.commit.sha}} by {{workflow.commit.author}}
- **Triggered by**: {{workflow.triggeredBy}}

## Failed Jobs
{{#each failure.jobs}}
### {{name}}
- **Status**: {{conclusion}}
- **Error Patterns**: {{#each ../failure.errorPatterns}}{{this}} {{/each}}

#### Failed Steps:
{{#each steps}}
- **{{name}}**: {{conclusion}}
  - Error: {{errorMessage}}
  - [View Logs]({{logUrl}})
{{/each}}
{{/each}}

## Impact Assessment
- [ ] Deployment blocked
- [ ] Production release delayed
- [ ] Development workflow interrupted

## Immediate Actions Required
- [ ] Review build logs for compilation errors
- [ ] Check dependency conflicts or version mismatches
- [ ] Verify environment configuration
- [ ] Test locally to reproduce the issue
- [ ] Check for infrastructure or service dependencies

## CI/CD Specific Troubleshooting
- [ ] Verify Docker image builds successfully
- [ ] Check environment variables and secrets
- [ ] Validate deployment configuration
- [ ] Review infrastructure changes
- [ ] Check service health and dependencies

## Links
- [Failed Workflow Run]({{workflow.url}})
- [Commit Details]({{workflow.commit.url}})
{{#if context.pullRequest}}
- [Pull Request #{{context.pullRequest.number}}]({{context.pullRequest.url}})
{{/if}}

## Escalation
{{#if escalation}}
This failure has occurred {{context.previousFailures}} times. Consider escalating to senior team members.
{{/if}}

---
*This issue was automatically created by the Workflow Failure Handler*
*Priority: {{workflow.severity}} | Assignees: {{assignees}}*