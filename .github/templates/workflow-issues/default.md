## Workflow Failure Report

**workflow:{{workflow.name}}**
**failure-type:{{failure.category}}**

### Summary
The **{{workflow.name}}** workflow failed with {{failure.jobs.length}} failed job(s).

### Details
- **Workflow Run:** [#{{workflow.runNumber}}]({{workflow.url}})
- **Branch:** `{{workflow.branch}}`
- **Commit:** [`{{workflow.commit.sha|truncate:7}}`](https://github.com/{{owner}}/{{repo}}/commit/{{workflow.commit.sha}})
- **Commit Message:** {{workflow.commit.message}}
- **Triggered By:** @{{workflow.triggeredBy}}
- **Timestamp:** {{failure.timestamp}}
- **Severity:** {{failure.severity}}
- **Category:** {{failure.category}}
{{#if context.pullRequest}}
- **Pull Request:** [#{{context.pullRequest.number}}](https://github.com/{{owner}}/{{repo}}/pull/{{context.pullRequest.number}}) - {{context.pullRequest.title}}
{{/if}}

### Failed Jobs

{{#each failure.jobs}}
#### ❌ {{name}}
- **Job ID:** {{id}}
- **Conclusion:** {{conclusion}}
{{#if steps}}
- **Failed Steps:**
{{#each steps}}
  - **{{name}}**: {{errorMessage}} ([View logs]({{logUrl}}))
{{/each}}
{{/if}}
{{#if logs}}

<details>
<summary>Error Logs</summary>

```
{{logs}}
```
</details>
{{/if}}
{{/each}}

{{#if failure.errorPatterns}}
### Error Patterns Detected
{{#each failure.errorPatterns}}
- {{this}}
{{/each}}
{{/if}}

### Troubleshooting Steps
{{troubleshootingSteps}}

### Metadata
<!-- failure-count:1 -->
<!-- last-failure:{{failure.timestamp}} -->
<!-- workflow-category:{{failure.category}} -->