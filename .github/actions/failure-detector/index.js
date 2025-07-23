const core = require('@actions/core');
const github = require('@actions/github');

async function run() {
  try {
    // Get inputs
    const workflowName = core.getInput('workflow-name', { required: true });
    const failureContext = core.getInput('failure-context') || 'auto-detect';
    const severity = core.getInput('severity') || 'medium';
    const assignees = core.getInput('assignees') || '';
    const labels = core.getInput('labels') || 'workflow-failure,bug';

    // Get GitHub context
    const context = github.context;
    const octokit = github.getOctokit(process.env.GITHUB_TOKEN || core.getInput('github-token'));

    // Fetch workflow run details
    const workflowRun = await octokit.rest.actions.getWorkflowRun({
      owner: context.repo.owner,
      repo: context.repo.repo,
      run_id: context.runId
    });

    // Analyze failure data
    const failureData = await analyzeWorkflowFailure(octokit, context, workflowRun.data);

    // Prepare issue data
    const issueData = {
      workflow: {
        name: workflowName,
        id: workflowRun.data.id,
        runId: context.runId,
        runNumber: context.runNumber,
        url: workflowRun.data.html_url,
        triggeredBy: context.actor,
        branch: context.ref.replace('refs/heads/', ''),
        commit: {
          sha: context.sha,
          message: context.payload.head_commit?.message || '',
          author: context.payload.head_commit?.author?.name || context.actor
        }
      },
      failure: {
        timestamp: new Date().toISOString(),
        jobs: failureData.failedJobs,
        severity: severity,
        category: detectWorkflowCategory(workflowName),
        errorPatterns: failureData.errorPatterns
      },
      context: {
        pullRequest: context.payload.pull_request ? {
          number: context.payload.pull_request.number,
          title: context.payload.pull_request.title,
          author: context.payload.pull_request.user.login
        } : undefined,
        environment: process.env.GITHUB_ENV || 'production',
        previousFailures: 0 // Will be calculated by issue manager
      },
      labels: labels.split(',').map(l => l.trim()),
      assignees: assignees ? assignees.split(',').map(a => a.trim()) : []
    };

    // Output the issue data
    core.setOutput('issue-data', JSON.stringify(issueData));
    core.setOutput('should-create-issue', 'true');

    core.info(`Workflow failure detected for ${workflowName}`);
    core.info(`Issue data prepared with ${failureData.failedJobs.length} failed job(s)`);

  } catch (error) {
    core.error(`Error in failure detector: ${error.message}`);
    core.setFailed(error.message);
  }
}

async function analyzeWorkflowFailure(octokit, context, workflowRun) {
  const failedJobs = [];
  const errorPatterns = [];

  try {
    // Get all jobs for the workflow run
    const jobs = await octokit.rest.actions.listJobsForWorkflowRun({
      owner: context.repo.owner,
      repo: context.repo.repo,
      run_id: workflowRun.id
    });

    // Process each failed job
    for (const job of jobs.data.jobs) {
      if (job.conclusion === 'failure' || job.conclusion === 'cancelled') {
        const failedSteps = [];
        
        // Get failed steps
        for (const step of job.steps || []) {
          if (step.conclusion === 'failure') {
            failedSteps.push({
              name: step.name,
              conclusion: step.conclusion,
              errorMessage: extractErrorMessage(step),
              logUrl: `${job.html_url}#step:${step.number}:1`
            });

            // Extract error patterns
            const patterns = detectErrorPatterns(step);
            errorPatterns.push(...patterns);
          }
        }

        // Try to get job logs (may require additional permissions)
        let logs = '';
        try {
          const logResponse = await octokit.rest.actions.downloadJobLogsForWorkflowRun({
            owner: context.repo.owner,
            repo: context.repo.repo,
            job_id: job.id
          });
          logs = extractRelevantLogs(logResponse.data);
        } catch (logError) {
          core.warning(`Could not fetch logs for job ${job.name}: ${logError.message}`);
        }

        failedJobs.push({
          name: job.name,
          id: job.id,
          conclusion: job.conclusion,
          steps: failedSteps,
          logs: logs
        });
      }
    }
  } catch (error) {
    core.warning(`Error analyzing workflow failure: ${error.message}`);
  }

  return {
    failedJobs,
    errorPatterns: [...new Set(errorPatterns)] // Remove duplicates
  };
}

function extractErrorMessage(step) {
  // Extract error message from step output
  // This is a simplified version - actual implementation would parse step outputs
  return step.name + ' failed';
}

function detectErrorPatterns(step) {
  const patterns = [];
  const stepName = step.name.toLowerCase();

  // Common error pattern detection
  if (stepName.includes('test')) {
    patterns.push('test-failure');
  }
  if (stepName.includes('build')) {
    patterns.push('build-failure');
  }
  if (stepName.includes('lint') || stepName.includes('eslint') || stepName.includes('pylint')) {
    patterns.push('linting-failure');
  }
  if (stepName.includes('deploy')) {
    patterns.push('deployment-failure');
  }
  if (stepName.includes('security') || stepName.includes('scan')) {
    patterns.push('security-failure');
  }

  return patterns;
}

function detectWorkflowCategory(workflowName) {
  const name = workflowName.toLowerCase();
  
  if (name.includes('ci') || name.includes('cd') || name.includes('build')) {
    return 'ci-cd';
  }
  if (name.includes('security') || name.includes('scan')) {
    return 'security';
  }
  if (name.includes('lint') || name.includes('quality')) {
    return 'code-quality';
  }
  if (name.includes('deploy') || name.includes('release')) {
    return 'deployment';
  }
  if (name.includes('test')) {
    return 'testing';
  }
  
  return 'general';
}

function extractRelevantLogs(logs) {
  // Extract only relevant error logs (last 50 lines or lines containing error keywords)
  const lines = logs.split('\n');
  const relevantLines = [];
  const errorKeywords = ['error', 'failed', 'failure', 'exception', 'fatal'];
  
  // Get last 50 lines
  const lastLines = lines.slice(-50);
  
  // Also get lines with error keywords
  for (const line of lines) {
    const lowerLine = line.toLowerCase();
    if (errorKeywords.some(keyword => lowerLine.includes(keyword))) {
      relevantLines.push(line);
    }
  }
  
  // Combine and deduplicate
  const combined = [...new Set([...relevantLines, ...lastLines])];
  return combined.slice(-100).join('\n'); // Limit to 100 lines
}

// Run the action
run();