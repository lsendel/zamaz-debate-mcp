# Workflow Failure: {{workflow.name}}

## Summary
The workflow **{{workflow.name}}** has failed and requires attention.

## Failure Details
- **Workflow**: {{workflow.name}}
- **Severity**: {{workflow.severity}}
- **Timestamp**: {{failure.timestamp}}
- **Branch**: {{workflow.branch}}
- **Commit**: {{workflow.commit.sha}}

## Failed Jobs
{{#each failure.jobs}}
- **{{name}}**: {{conclusion}}
  {{#each steps}}
  - Step: {{name}} - {{conclusion}}
  {{/each}}
{{/each}}

## Troubleshooting Steps
- [ ] Check the workflow logs for detailed error messages
- [ ] Verify the commit changes didn't introduce breaking changes
- [ ] Check if dependencies or environment variables have changed
- [ ] Review recent configuration changes

## Links
- [Workflow Run]({{workflow.url}})
- [Commit]({{workflow.commit.url}})

---
*This issue was automatically created by the Workflow Failure Handler*