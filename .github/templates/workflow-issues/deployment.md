# Deployment Failure: {{workflow.name}}

## 🚀 Deployment Failed
The deployment workflow **{{workflow.name}}** has failed and may require rollback procedures.

## Failure Details
- **Deployment**: {{workflow.name}}
- **Severity**: {{workflow.severity}}
- **Environment**: {{context.environment}}
- **Timestamp**: {{failure.timestamp}}
- **Branch**: {{workflow.branch}}
- **Commit**: {{workflow.commit.sha}} by {{workflow.commit.author}}

## Deployment Status
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

## Immediate Actions
- [ ] Check application health and availability
- [ ] Verify if rollback is needed
- [ ] Monitor error rates and performance metrics
- [ ] Check infrastructure status
- [ ] Validate configuration changes

## Deployment Troubleshooting
- [ ] Review deployment logs for specific errors
- [ ] Check container/service startup issues
- [ ] Verify database migrations completed successfully
- [ ] Check environment variables and secrets
- [ ] Validate network connectivity and DNS
- [ ] Review resource allocation (CPU, memory, disk)

## Rollback Procedures
If the deployment cannot be fixed quickly:
- [ ] Execute rollback to previous stable version
- [ ] Verify rollback completed successfully
- [ ] Monitor application stability post-rollback
- [ ] Document rollback reason and timeline

## Environment Checks
- [ ] **{{context.environment}}** environment status
- [ ] Database connectivity and health
- [ ] External service dependencies
- [ ] Load balancer and routing configuration
- [ ] SSL certificates and security settings

## Post-Deployment Validation
- [ ] Run smoke tests
- [ ] Check critical user journeys
- [ ] Monitor application metrics
- [ ] Verify feature flags and configurations

## Links
- [Deployment Logs]({{workflow.url}})
- [Commit]({{workflow.commit.url}})
- [Environment Dashboard](./monitoring/{{context.environment}})
{{#if context.pullRequest}}
- [Pull Request #{{context.pullRequest.number}}]({{context.pullRequest.url}})
{{/if}}

## Escalation
🚨 **Deployment failures may impact users and require immediate escalation to on-call team.**

---
*This issue was automatically created by the Workflow Failure Handler*
*Environment: {{context.environment}} | Priority: {{workflow.severity}}*