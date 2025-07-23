const { Octokit } = require('@octokit/rest');
const core = require('@actions/core');

class IssueManager {
  constructor(token, owner, repo) {
    this.octokit = new Octokit({ auth: token });
    this.owner = owner;
    this.repo = repo;
    this.maxRetries = 3;
  }

  async analyzeFailure(workflowData) {
    try {
      const issueData = {
        title: this.generateIssueTitle(workflowData),
        body: await this.generateIssueBody(workflowData),
        labels: workflowData.labels || ['workflow-failure', 'bug'],
        assignees: workflowData.assignees || [],
        metadata: {
          workflowName: workflowData.workflow.name,
          failureType: workflowData.failure.category,
          createdAt: new Date().toISOString(),
          lastUpdated: new Date().toISOString()
        }
      };

      return issueData;
    } catch (error) {
      core.error(`Error analyzing failure: ${error.message}`);
      throw error;
    }
  }

  async checkDuplicateIssue(workflowName, failureType) {
    try {
      // Search for existing open issues with the same workflow and failure type
      const query = `repo:${this.owner}/${this.repo} is:issue is:open "workflow:${workflowName}" "failure-type:${failureType}"`;
      
      const searchResult = await this.octokit.rest.search.issuesAndPullRequests({
        q: query,
        sort: 'created',
        order: 'desc',
        per_page: 10
      });

      if (searchResult.data.total_count > 0) {
        // Check if any of the issues match our criteria
        for (const issue of searchResult.data.items) {
          if (issue.body && issue.body.includes(`workflow:${workflowName}`) && 
              issue.body.includes(`failure-type:${failureType}`)) {
            return issue;
          }
        }
      }

      return null;
    } catch (error) {
      core.warning(`Error checking for duplicate issues: ${error.message}`);
      return null;
    }
  }

  async createWorkflowIssue(issueData) {
    let attempt = 0;
    
    while (attempt < this.maxRetries) {
      try {
        const response = await this.octokit.rest.issues.create({
          owner: this.owner,
          repo: this.repo,
          title: issueData.title,
          body: issueData.body,
          labels: issueData.labels,
          assignees: issueData.assignees
        });
        
        core.info(`Created issue #${response.data.number}: ${response.data.html_url}`);
        return response.data;
      } catch (error) {
        attempt++;
        
        if (error.status === 403) {
          // Rate limit exceeded
          core.warning('Rate limit exceeded, waiting 60 seconds...');
          await this.sleep(60000); // Wait 1 minute
        } else if (error.status >= 500) {
          // Server error, retry with exponential backoff
          const waitTime = Math.pow(2, attempt) * 1000;
          core.warning(`Server error, retrying in ${waitTime/1000} seconds...`);
          await this.sleep(waitTime);
        } else {
          // Client error, don't retry
          throw error;
        }
      }
    }
    
    throw new Error(`Failed to create issue after ${this.maxRetries} attempts`);
  }

  async updateExistingIssue(issueNumber, updateData) {
    try {
      // Add a comment to the existing issue
      const comment = this.generateUpdateComment(updateData);
      
      await this.octokit.rest.issues.createComment({
        owner: this.owner,
        repo: this.repo,
        issue_number: issueNumber,
        body: comment
      });

      // Update issue labels if needed
      if (updateData.labels && updateData.labels.length > 0) {
        await this.octokit.rest.issues.addLabels({
          owner: this.owner,
          repo: this.repo,
          issue_number: issueNumber,
          labels: updateData.labels
        });
      }

      // Update failure count in issue body
      const issue = await this.octokit.rest.issues.get({
        owner: this.owner,
        repo: this.repo,
        issue_number: issueNumber
      });

      const updatedBody = this.updateFailureCount(issue.data.body, updateData);
      
      await this.octokit.rest.issues.update({
        owner: this.owner,
        repo: this.repo,
        issue_number: issueNumber,
        body: updatedBody
      });

      core.info(`Updated existing issue #${issueNumber}`);
      return issue.data;
    } catch (error) {
      core.error(`Error updating issue: ${error.message}`);
      throw error;
    }
  }

  generateIssueTitle(workflowData) {
    const workflow = workflowData.workflow;
    const failure = workflowData.failure;
    
    return `[${failure.severity.toUpperCase()}] ${workflow.name} workflow failed on ${workflow.branch}`;
  }

  async generateIssueBody(workflowData) {
    const workflow = workflowData.workflow;
    const failure = workflowData.failure;
    const context = workflowData.context;
    
    let body = `## Workflow Failure Report\n\n`;
    body += `**workflow:${workflow.name}**\n`;
    body += `**failure-type:${failure.category}**\n\n`;
    
    body += `### Summary\n`;
    body += `The **${workflow.name}** workflow failed with ${failure.jobs.length} failed job(s).\n\n`;
    
    body += `### Details\n`;
    body += `- **Workflow Run:** [#${workflow.runNumber}](${workflow.url})\n`;
    body += `- **Branch:** \`${workflow.branch}\`\n`;
    body += `- **Commit:** [\`${workflow.commit.sha.substring(0, 7)}\`](https://github.com/${this.owner}/${this.repo}/commit/${workflow.commit.sha})\n`;
    body += `- **Commit Message:** ${workflow.commit.message}\n`;
    body += `- **Triggered By:** @${workflow.triggeredBy}\n`;
    body += `- **Timestamp:** ${failure.timestamp}\n`;
    body += `- **Severity:** ${failure.severity}\n`;
    body += `- **Category:** ${failure.category}\n`;
    
    if (context.pullRequest) {
      body += `- **Pull Request:** [#${context.pullRequest.number}](https://github.com/${this.owner}/${this.repo}/pull/${context.pullRequest.number}) - ${context.pullRequest.title}\n`;
    }
    
    body += `\n### Failed Jobs\n`;
    
    for (const job of failure.jobs) {
      body += `\n#### ❌ ${job.name}\n`;
      body += `- **Job ID:** ${job.id}\n`;
      body += `- **Conclusion:** ${job.conclusion}\n`;
      
      if (job.steps && job.steps.length > 0) {
        body += `- **Failed Steps:**\n`;
        for (const step of job.steps) {
          body += `  - **${step.name}**: ${step.errorMessage} ([View logs](${step.logUrl}))\n`;
        }
      }
      
      if (job.logs) {
        body += `\n<details>\n<summary>Error Logs</summary>\n\n\`\`\`\n${job.logs}\n\`\`\`\n</details>\n`;
      }
    }
    
    if (failure.errorPatterns && failure.errorPatterns.length > 0) {
      body += `\n### Error Patterns Detected\n`;
      body += failure.errorPatterns.map(pattern => `- ${pattern}`).join('\n');
      body += '\n';
    }
    
    body += `\n### Troubleshooting Steps\n`;
    body += this.generateTroubleshootingSteps(failure.category, failure.errorPatterns);
    
    body += `\n### Metadata\n`;
    body += `<!-- failure-count:1 -->\n`;
    body += `<!-- last-failure:${failure.timestamp} -->\n`;
    body += `<!-- workflow-category:${failure.category} -->\n`;
    
    return body;
  }

  generateUpdateComment(updateData) {
    const workflow = updateData.workflow;
    const failure = updateData.failure;
    
    let comment = `## 🔄 Workflow Failed Again\n\n`;
    comment += `The **${workflow.name}** workflow has failed again.\n\n`;
    comment += `### Latest Failure Details\n`;
    comment += `- **Workflow Run:** [#${workflow.runNumber}](${workflow.url})\n`;
    comment += `- **Commit:** [\`${workflow.commit.sha.substring(0, 7)}\`](https://github.com/${this.owner}/${this.repo}/commit/${workflow.commit.sha})\n`;
    comment += `- **Timestamp:** ${failure.timestamp}\n`;
    comment += `- **Failed Jobs:** ${failure.jobs.map(j => j.name).join(', ')}\n`;
    
    if (failure.errorPatterns && failure.errorPatterns.length > 0) {
      comment += `\n### Error Patterns\n`;
      comment += failure.errorPatterns.map(pattern => `- ${pattern}`).join('\n');
    }
    
    return comment;
  }

  updateFailureCount(body, updateData) {
    // Extract current failure count
    const countMatch = body.match(/<!-- failure-count:(\d+) -->/);
    const currentCount = countMatch ? parseInt(countMatch[1]) : 1;
    const newCount = currentCount + 1;
    
    // Update failure count
    body = body.replace(/<!-- failure-count:\d+ -->/, `<!-- failure-count:${newCount} -->`);
    
    // Update last failure timestamp
    body = body.replace(
      /<!-- last-failure:.*? -->/,
      `<!-- last-failure:${updateData.failure.timestamp} -->`
    );
    
    // Add failure count to summary if not already present
    if (!body.includes('**Total Failures:**')) {
      body = body.replace(
        '### Summary\n',
        `### Summary\n**Total Failures:** ${newCount}\n`
      );
    } else {
      body = body.replace(
        /\*\*Total Failures:\*\* \d+/,
        `**Total Failures:** ${newCount}`
      );
    }
    
    return body;
  }

  generateTroubleshootingSteps(category, errorPatterns) {
    const steps = [];
    
    // Category-specific troubleshooting
    switch (category) {
      case 'ci-cd':
        steps.push('1. Check build dependencies and versions');
        steps.push('2. Verify environment variables are correctly set');
        steps.push('3. Review recent dependency updates');
        steps.push('4. Check for disk space and resource availability');
        break;
      
      case 'security':
        steps.push('1. Review security scan results and vulnerabilities');
        steps.push('2. Check for newly introduced dependencies');
        steps.push('3. Verify security policies are up to date');
        steps.push('4. Review code changes for security best practices');
        break;
      
      case 'code-quality':
        steps.push('1. Run linting locally to reproduce issues');
        steps.push('2. Check linting configuration files');
        steps.push('3. Review code formatting standards');
        steps.push('4. Verify pre-commit hooks are working');
        break;
      
      case 'deployment':
        steps.push('1. Verify deployment credentials and permissions');
        steps.push('2. Check target environment availability');
        steps.push('3. Review deployment configuration');
        steps.push('4. Check for infrastructure changes');
        break;
      
      case 'testing':
        steps.push('1. Run tests locally to reproduce failures');
        steps.push('2. Check for flaky tests');
        steps.push('3. Review test dependencies and fixtures');
        steps.push('4. Verify test environment setup');
        break;
      
      default:
        steps.push('1. Review the error logs above');
        steps.push('2. Check recent commits for potential causes');
        steps.push('3. Verify workflow configuration is correct');
        steps.push('4. Run workflow locally if possible');
    }
    
    // Error pattern-specific troubleshooting
    if (errorPatterns.includes('test-failure')) {
      steps.push('- Consider running tests with verbose output');
      steps.push('- Check if tests are environment-dependent');
    }
    
    if (errorPatterns.includes('build-failure')) {
      steps.push('- Clear build cache and retry');
      steps.push('- Check for breaking changes in dependencies');
    }
    
    if (errorPatterns.includes('linting-failure')) {
      steps.push('- Run auto-fix commands if available');
      steps.push('- Update linting rules if needed');
    }
    
    return steps.join('\n');
  }

  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}

// Main execution function
async function main() {
  try {
    const issueDataInput = process.argv[2] || process.env.ISSUE_DATA;
    const dryRun = process.argv.includes('--dry-run');
    
    if (!issueDataInput) {
      throw new Error('No issue data provided');
    }
    
    const workflowData = JSON.parse(issueDataInput);
    const token = process.env.GITHUB_TOKEN;
    const [owner, repo] = process.env.GITHUB_REPOSITORY.split('/');
    
    const issueManager = new IssueManager(token, owner, repo);
    
    // Analyze failure and prepare issue data
    const issueData = await issueManager.analyzeFailure(workflowData);
    
    if (dryRun) {
      console.log('Dry run mode - Issue data:');
      console.log(JSON.stringify(issueData, null, 2));
      return;
    }
    
    // Check for duplicate issues
    const existingIssue = await issueManager.checkDuplicateIssue(
      workflowData.workflow.name,
      workflowData.failure.category
    );
    
    if (existingIssue) {
      // Update existing issue
      await issueManager.updateExistingIssue(existingIssue.number, workflowData);
    } else {
      // Create new issue
      await issueManager.createWorkflowIssue(issueData);
    }
    
  } catch (error) {
    core.setFailed(`Issue manager failed: ${error.message}`);
    process.exit(1);
  }
}

// Export for testing
module.exports = { IssueManager };

// Run if called directly
if (require.main === module) {
  main();
}