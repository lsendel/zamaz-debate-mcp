# Security Scan Failure: {{workflow.name}}

## 🔒 Security Issues Detected
The security scanning workflow **{{workflow.name}}** has detected potential security vulnerabilities.

## Failure Details
- **Workflow**: {{workflow.name}}
- **Severity**: {{workflow.severity}}
- **Timestamp**: {{failure.timestamp}}
- **Branch**: {{workflow.branch}}
- **Commit**: {{workflow.commit.sha}} by {{workflow.commit.author}}

## Security Findings
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

## Immediate Security Actions
- [ ] Review security scan results in detail
- [ ] Identify and assess vulnerability severity
- [ ] Check for known CVEs in dependencies
- [ ] Update vulnerable dependencies
- [ ] Review code for security best practices

## Security Checklist
- [ ] Scan for hardcoded secrets or credentials
- [ ] Check dependency vulnerabilities
- [ ] Review authentication and authorization
- [ ] Validate input sanitization
- [ ] Check for SQL injection vulnerabilities
- [ ] Review HTTPS and TLS configuration

## Security Resources
- [Security Guidelines](./docs/security.md)
- [Dependency Security Policy](./docs/dependency-security.md)
- [Incident Response Plan](./docs/incident-response.md)

## Escalation Required
⚠️ **Security issues require immediate attention and may need escalation to the security team.**

## Links
- [Security Scan Results]({{workflow.url}})
- [Commit]({{workflow.commit.url}})
{{#if context.pullRequest}}
- [Pull Request #{{context.pullRequest.number}}]({{context.pullRequest.url}})
{{/if}}

---
*This issue was automatically created by the Workflow Failure Handler*
*🚨 Security Priority: {{workflow.severity}}*