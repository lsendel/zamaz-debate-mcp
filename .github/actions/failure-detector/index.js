const core = require('@actions/core');
const github = require('@actions/github');

async function run() {
  try {
    // Get inputs
    const workflowName = core.getInput('workflow-name', { required: true });
    const failureContext = core.getInput('failure-context') || 'auto-detect';
    const userSeverity = core.getInput('severity') || 'auto';
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

    // Calculate severity if set to auto
    const calculatedSeverity = userSeverity === 'auto' 
      ? calculateSeverity(workflowName, failureData, context, workflowRun.data)
      : userSeverity;

    // Enrich failure context with commit and PR information
    const enrichedContext = await enrichFailureContext(octokit, context, workflowRun.data);

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
          author: context.payload.head_commit?.author?.name || context.actor,
          ...enrichedContext.commit
        }
      },
      failure: {
        timestamp: new Date().toISOString(),
        jobs: failureData.failedJobs,
        severity: calculatedSeverity,
        category: detectWorkflowCategory(workflowName),
        errorPatterns: failureData.errorPatterns,
        analysis: failureData.failureAnalysis
      },
      context: {
        pullRequest: context.payload.pull_request ? {
          number: context.payload.pull_request.number,
          title: context.payload.pull_request.title,
          author: context.payload.pull_request.user.login,
          ...enrichedContext.pullRequest
        } : undefined,
        environment: process.env.GITHUB_ENV || 'production',
        previousFailures: 0, // Will be calculated by issue manager
        recentCommits: enrichedContext.recentCommits,
        relatedIssues: enrichedContext.relatedIssues
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
  const extractedErrors = [];

  try {
    // Get all jobs for the workflow run
    const jobs = await octokit.rest.actions.listJobsForWorkflowRun({
      owner: context.repo.owner,
      repo: context.repo.repo,
      run_id: workflowRun.id,
      per_page: 100
    });

    // Process each failed job
    for (const job of jobs.data.jobs) {
      if (job.conclusion === 'failure' || job.conclusion === 'cancelled') {
        const failedSteps = [];
        let jobErrorData = {
          errorMessages: [],
          stackTraces: [],
          failedTests: [],
          buildErrors: [],
          lintErrors: []
        };
        
        // Get failed steps
        for (const step of job.steps || []) {
          if (step.conclusion === 'failure') {
            // Try to get step logs for better error extraction
            let stepLogs = '';
            try {
              const logResponse = await octokit.rest.actions.downloadJobLogsForWorkflowRun({
                owner: context.repo.owner,
                repo: context.repo.repo,
                job_id: job.id
              });
              stepLogs = extractStepLogs(logResponse.data, step.name);
            } catch (logError) {
              core.debug(`Could not fetch logs for step ${step.name}: ${logError.message}`);
            }

            // Extract detailed error information from logs
            const errorDetails = extractDetailedErrors(stepLogs, step.name);
            jobErrorData.errorMessages.push(...errorDetails.errorMessages);
            jobErrorData.stackTraces.push(...errorDetails.stackTraces);
            jobErrorData.failedTests.push(...errorDetails.failedTests);
            jobErrorData.buildErrors.push(...errorDetails.buildErrors);
            jobErrorData.lintErrors.push(...errorDetails.lintErrors);

            failedSteps.push({
              name: step.name,
              number: step.number,
              conclusion: step.conclusion,
              startedAt: step.started_at,
              completedAt: step.completed_at,
              duration: calculateDuration(step.started_at, step.completed_at),
              errorMessage: errorDetails.primaryError || extractErrorMessage(step),
              logUrl: `${job.html_url}#step:${step.number}:1`,
              errorType: classifyError(errorDetails, step.name)
            });

            // Extract error patterns
            const patterns = detectEnhancedErrorPatterns(step, errorDetails);
            errorPatterns.push(...patterns);
          }
        }

        // Extract job-level error summary
        const jobSummary = summarizeJobErrors(jobErrorData);

        failedJobs.push({
          name: job.name,
          id: job.id,
          conclusion: job.conclusion,
          startedAt: job.started_at,
          completedAt: job.completed_at,
          duration: calculateDuration(job.started_at, job.completed_at),
          runner: job.runner_name || 'unknown',
          steps: failedSteps,
          errorSummary: jobSummary,
          logs: jobSummary.relevantLogs
        });

        // Add to extracted errors for analysis
        extractedErrors.push(...jobErrorData.errorMessages);
      }
    }

    // Perform failure pattern recognition
    const failureAnalysis = analyzeFailurePatterns(extractedErrors, errorPatterns);

  } catch (error) {
    core.warning(`Error analyzing workflow failure: ${error.message}`);
  }

  return {
    failedJobs,
    errorPatterns: [...new Set(errorPatterns)], // Remove duplicates
    failureAnalysis: analyzeFailurePatterns(extractedErrors, errorPatterns)
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

function extractStepLogs(fullLogs, stepName) {
  // Extract logs specific to a step
  const lines = fullLogs.split('\n');
  const stepStart = lines.findIndex(line => line.includes(`##[group]Run ${stepName}`));
  const stepEnd = lines.findIndex((line, index) => index > stepStart && line.includes('##[endgroup]'));
  
  if (stepStart !== -1 && stepEnd !== -1) {
    return lines.slice(stepStart, stepEnd + 1).join('\n');
  }
  
  // Fallback: search for step name in logs
  const relevantLines = lines.filter(line => line.toLowerCase().includes(stepName.toLowerCase()));
  return relevantLines.slice(0, 100).join('\n');
}

function extractDetailedErrors(logs, stepName) {
  const result = {
    errorMessages: [],
    stackTraces: [],
    failedTests: [],
    buildErrors: [],
    lintErrors: [],
    primaryError: null
  };

  const lines = logs.split('\n');
  
  // Error extraction patterns
  const patterns = {
    // Generic error patterns
    error: /(?:error|ERROR|Error):\s*(.+)/,
    fatal: /(?:fatal|FATAL|Fatal):\s*(.+)/,
    exception: /(?:Exception|exception):\s*(.+)/,
    
    // Stack trace patterns
    stackTrace: /^\s*at\s+.+\(.+:\d+:\d+\)/,
    pythonTrace: /^\s*File\s+"[^"]+",\s+line\s+\d+/,
    
    // Test failure patterns
    testFailure: /(?:FAIL|FAILED|✗|✖|❌)\s+(.+)/,
    jestFailure: /FAIL\s+(.+\.(?:test|spec)\.[jt]sx?)/,
    pytestFailure: /FAILED\s+(.+\.py::\w+)/,
    
    // Build error patterns
    buildError: /(?:Build failed|Compilation failed|Build error):\s*(.+)/,
    npmError: /npm ERR!\s+(.+)/,
    mavenError: /\[ERROR\]\s+(.+)/,
    
    // Linting error patterns
    eslintError: /(\d+:\d+)\s+error\s+(.+)/,
    pylintError: /(.+):(\d+):(\d+):\s+([A-Z]\d+):\s+(.+)/,
    checkstyleError: /\[ERROR\]\s+(.+):(\d+):\s+(.+)/
  };

  let inStackTrace = false;
  let currentStackTrace = [];
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmedLine = line.trim();
    
    // Skip empty lines
    if (!trimmedLine) {
      if (inStackTrace && currentStackTrace.length > 0) {
        result.stackTraces.push(currentStackTrace.join('\n'));
        currentStackTrace = [];
        inStackTrace = false;
      }
      continue;
    }
    
    // Check for error messages
    for (const [type, pattern] of Object.entries(patterns)) {
      const match = line.match(pattern);
      if (match) {
        switch (type) {
          case 'error':
          case 'fatal':
          case 'exception':
            result.errorMessages.push(match[1] || match[0]);
            if (!result.primaryError) {
              result.primaryError = match[1] || match[0];
            }
            break;
            
          case 'stackTrace':
          case 'pythonTrace':
            inStackTrace = true;
            currentStackTrace.push(trimmedLine);
            break;
            
          case 'testFailure':
          case 'jestFailure':
          case 'pytestFailure':
            result.failedTests.push(match[1] || match[0]);
            break;
            
          case 'buildError':
          case 'npmError':
          case 'mavenError':
            result.buildErrors.push(match[1] || match[0]);
            break;
            
          case 'eslintError':
          case 'pylintError':
          case 'checkstyleError':
            result.lintErrors.push(match[0]);
            break;
        }
      }
    }
    
    // Continue collecting stack trace
    if (inStackTrace && (patterns.stackTrace.test(line) || patterns.pythonTrace.test(line))) {
      currentStackTrace.push(trimmedLine);
    }
  }
  
  // Capture any remaining stack trace
  if (currentStackTrace.length > 0) {
    result.stackTraces.push(currentStackTrace.join('\n'));
  }
  
  return result;
}

function calculateDuration(startTime, endTime) {
  if (!startTime || !endTime) return 'unknown';
  
  const start = new Date(startTime);
  const end = new Date(endTime);
  const durationMs = end - start;
  
  if (durationMs < 1000) return `${durationMs}ms`;
  if (durationMs < 60000) return `${Math.round(durationMs / 1000)}s`;
  return `${Math.round(durationMs / 60000)}m`;
}

function classifyError(errorDetails, stepName) {
  // Classify error based on error details and step name
  const stepLower = stepName.toLowerCase();
  
  if (errorDetails.failedTests.length > 0) return 'test-failure';
  if (errorDetails.buildErrors.length > 0) return 'build-failure';
  if (errorDetails.lintErrors.length > 0) return 'lint-failure';
  
  if (stepLower.includes('deploy')) return 'deployment-failure';
  if (stepLower.includes('security') || stepLower.includes('scan')) return 'security-failure';
  if (stepLower.includes('docker')) return 'container-failure';
  if (stepLower.includes('database') || stepLower.includes('migration')) return 'database-failure';
  
  // Check error messages for classification
  const allErrors = [...errorDetails.errorMessages, ...errorDetails.buildErrors].join(' ').toLowerCase();
  
  if (allErrors.includes('permission') || allErrors.includes('unauthorized')) return 'permission-error';
  if (allErrors.includes('timeout') || allErrors.includes('timed out')) return 'timeout-error';
  if (allErrors.includes('network') || allErrors.includes('connection')) return 'network-error';
  if (allErrors.includes('memory') || allErrors.includes('heap')) return 'resource-error';
  
  return 'general-failure';
}

function detectEnhancedErrorPatterns(step, errorDetails) {
  const patterns = new Set();
  
  // Add basic patterns from step name
  const basicPatterns = detectErrorPatterns(step);
  basicPatterns.forEach(p => patterns.add(p));
  
  // Add patterns from error classification
  const errorType = classifyError(errorDetails, step.name);
  patterns.add(errorType);
  
  // Add patterns from error messages
  const allErrors = [
    ...errorDetails.errorMessages,
    ...errorDetails.buildErrors,
    ...errorDetails.failedTests
  ].join(' ').toLowerCase();
  
  // Specific technology patterns
  if (allErrors.includes('typescript') || allErrors.includes('tsc')) patterns.add('typescript-error');
  if (allErrors.includes('eslint')) patterns.add('eslint-error');
  if (allErrors.includes('jest') || allErrors.includes('vitest')) patterns.add('jest-error');
  if (allErrors.includes('webpack')) patterns.add('webpack-error');
  if (allErrors.includes('docker')) patterns.add('docker-error');
  if (allErrors.includes('kubernetes') || allErrors.includes('k8s')) patterns.add('k8s-error');
  if (allErrors.includes('terraform')) patterns.add('terraform-error');
  if (allErrors.includes('maven') || allErrors.includes('gradle')) patterns.add('java-build-error');
  if (allErrors.includes('pip') || allErrors.includes('poetry')) patterns.add('python-dependency-error');
  
  return Array.from(patterns);
}

function summarizeJobErrors(jobErrorData) {
  const summary = {
    totalErrors: 0,
    errorTypes: {},
    primaryErrors: [],
    relevantLogs: ''
  };
  
  // Count error types
  summary.errorTypes.general = jobErrorData.errorMessages.length;
  summary.errorTypes.tests = jobErrorData.failedTests.length;
  summary.errorTypes.build = jobErrorData.buildErrors.length;
  summary.errorTypes.lint = jobErrorData.lintErrors.length;
  summary.errorTypes.stackTraces = jobErrorData.stackTraces.length;
  
  summary.totalErrors = Object.values(summary.errorTypes).reduce((a, b) => a + b, 0);
  
  // Get primary errors (first 3 of each type)
  if (jobErrorData.errorMessages.length > 0) {
    summary.primaryErrors.push(...jobErrorData.errorMessages.slice(0, 3));
  }
  if (jobErrorData.buildErrors.length > 0) {
    summary.primaryErrors.push(...jobErrorData.buildErrors.slice(0, 2));
  }
  if (jobErrorData.failedTests.length > 0) {
    summary.primaryErrors.push(`${jobErrorData.failedTests.length} test(s) failed`);
  }
  
  // Create relevant logs summary
  const logParts = [];
  
  if (jobErrorData.errorMessages.length > 0) {
    logParts.push('=== Error Messages ===\n' + jobErrorData.errorMessages.slice(0, 5).join('\n'));
  }
  
  if (jobErrorData.stackTraces.length > 0) {
    logParts.push('=== Stack Traces ===\n' + jobErrorData.stackTraces[0]);
  }
  
  if (jobErrorData.buildErrors.length > 0) {
    logParts.push('=== Build Errors ===\n' + jobErrorData.buildErrors.slice(0, 5).join('\n'));
  }
  
  summary.relevantLogs = logParts.join('\n\n');
  
  return summary;
}

function analyzeFailurePatterns(errors, patterns) {
  const analysis = {
    commonPatterns: {},
    likelyRootCause: null,
    suggestedActions: [],
    errorFrequency: {}
  };
  
  // Count pattern frequency
  patterns.forEach(pattern => {
    analysis.commonPatterns[pattern] = (analysis.commonPatterns[pattern] || 0) + 1;
  });
  
  // Analyze error messages for common themes
  const errorThemes = {
    dependency: 0,
    syntax: 0,
    configuration: 0,
    network: 0,
    permission: 0,
    resource: 0
  };
  
  errors.forEach(error => {
    const errorLower = error.toLowerCase();
    if (errorLower.includes('dependency') || errorLower.includes('module not found')) errorThemes.dependency++;
    if (errorLower.includes('syntax') || errorLower.includes('unexpected token')) errorThemes.syntax++;
    if (errorLower.includes('config') || errorLower.includes('missing required')) errorThemes.configuration++;
    if (errorLower.includes('network') || errorLower.includes('connection')) errorThemes.network++;
    if (errorLower.includes('permission') || errorLower.includes('access denied')) errorThemes.permission++;
    if (errorLower.includes('memory') || errorLower.includes('heap') || errorLower.includes('timeout')) errorThemes.resource++;
  });
  
  // Determine likely root cause
  const sortedPatterns = Object.entries(analysis.commonPatterns).sort((a, b) => b[1] - a[1]);
  if (sortedPatterns.length > 0) {
    analysis.likelyRootCause = sortedPatterns[0][0];
  }
  
  // Suggest actions based on patterns and themes
  const maxTheme = Object.entries(errorThemes).reduce((a, b) => b[1] > a[1] ? b : a);
  
  if (maxTheme[0] === 'dependency') {
    analysis.suggestedActions.push('Check package dependencies and run install commands');
    analysis.suggestedActions.push('Verify all required packages are in package.json/requirements.txt');
  }
  if (maxTheme[0] === 'syntax') {
    analysis.suggestedActions.push('Review recent code changes for syntax errors');
    analysis.suggestedActions.push('Run linting tools locally to catch syntax issues');
  }
  if (maxTheme[0] === 'configuration') {
    analysis.suggestedActions.push('Verify environment variables and configuration files');
    analysis.suggestedActions.push('Check if all required secrets are set in GitHub');
  }
  if (maxTheme[0] === 'network') {
    analysis.suggestedActions.push('Check network connectivity and firewall rules');
    analysis.suggestedActions.push('Verify external service availability');
  }
  if (maxTheme[0] === 'permission') {
    analysis.suggestedActions.push('Check file and directory permissions');
    analysis.suggestedActions.push('Verify GitHub token has required permissions');
  }
  if (maxTheme[0] === 'resource') {
    analysis.suggestedActions.push('Consider increasing runner resources or timeout limits');
    analysis.suggestedActions.push('Optimize code to reduce memory usage');
  }
  
  return analysis;
}

function calculateSeverity(workflowName, failureData, context, workflowRun) {
  let severityScore = 0;
  
  // Base severity on workflow category
  const category = detectWorkflowCategory(workflowName);
  const categorySeverity = {
    'deployment': 40,
    'security': 35,
    'ci-cd': 25,
    'testing': 15,
    'code-quality': 10,
    'general': 20
  };
  severityScore += categorySeverity[category] || 20;
  
  // Branch-based severity
  const branch = context.ref.replace('refs/heads/', '');
  if (branch === 'main' || branch === 'master') {
    severityScore += 30;
  } else if (branch === 'develop' || branch === 'staging') {
    severityScore += 20;
  } else if (branch.startsWith('release/')) {
    severityScore += 25;
  } else if (branch.startsWith('hotfix/')) {
    severityScore += 35;
  }
  
  // Error type based severity
  const errorPatterns = failureData.errorPatterns || [];
  if (errorPatterns.includes('security-failure')) severityScore += 30;
  if (errorPatterns.includes('deployment-failure')) severityScore += 25;
  if (errorPatterns.includes('database-failure')) severityScore += 20;
  if (errorPatterns.includes('permission-error')) severityScore += 15;
  if (errorPatterns.includes('timeout-error')) severityScore += 10;
  
  // Failure analysis based severity
  const analysis = failureData.failureAnalysis;
  if (analysis && analysis.likelyRootCause) {
    const rootCauseSeverity = {
      'security-failure': 30,
      'deployment-failure': 25,
      'database-failure': 20,
      'permission-error': 15,
      'build-failure': 10,
      'test-failure': 5
    };
    severityScore += rootCauseSeverity[analysis.likelyRootCause] || 5;
  }
  
  // Number of failed jobs
  const failedJobCount = failureData.failedJobs?.length || 0;
  if (failedJobCount > 5) severityScore += 20;
  else if (failedJobCount > 3) severityScore += 15;
  else if (failedJobCount > 1) severityScore += 10;
  
  // Event type based severity
  if (context.eventName === 'schedule') {
    severityScore += 10; // Scheduled runs are usually important
  } else if (context.eventName === 'workflow_dispatch') {
    severityScore += 15; // Manual runs might be urgent
  }
  
  // Recent failure history would be added by issue manager
  
  // Convert score to severity level
  if (severityScore >= 80) return 'critical';
  if (severityScore >= 60) return 'high';
  if (severityScore >= 40) return 'medium';
  return 'low';
}

async function enrichFailureContext(octokit, context, workflowRun) {
  const enrichedData = {
    commit: {},
    pullRequest: {},
    recentCommits: [],
    relatedIssues: []
  };
  
  try {
    // Get commit details
    if (context.sha) {
      const commit = await octokit.rest.repos.getCommit({
        owner: context.repo.owner,
        repo: context.repo.repo,
        ref: context.sha
      });
      
      enrichedData.commit = {
        url: commit.data.html_url,
        changedFiles: commit.data.files?.length || 0,
        additions: commit.data.stats?.additions || 0,
        deletions: commit.data.stats?.deletions || 0,
        parents: commit.data.parents.map(p => p.sha)
      };
    }
    
    // Get PR details if this is a PR
    if (context.payload.pull_request) {
      const pr = await octokit.rest.pulls.get({
        owner: context.repo.owner,
        repo: context.repo.repo,
        pull_number: context.payload.pull_request.number
      });
      
      enrichedData.pullRequest = {
        state: pr.data.state,
        mergeable: pr.data.mergeable,
        reviewStatus: pr.data.mergeable_state,
        labels: pr.data.labels.map(l => l.name),
        reviewers: pr.data.requested_reviewers.map(r => r.login),
        changedFiles: pr.data.changed_files,
        additions: pr.data.additions,
        deletions: pr.data.deletions
      };
    }
    
    // Get recent commits on the branch (last 5)
    const commits = await octokit.rest.repos.listCommits({
      owner: context.repo.owner,
      repo: context.repo.repo,
      sha: context.ref,
      per_page: 5
    });
    
    enrichedData.recentCommits = commits.data.map(commit => ({
      sha: commit.sha,
      message: commit.commit.message,
      author: commit.commit.author.name,
      date: commit.commit.author.date
    }));
    
    // Search for related issues (workflow failures in last 7 days)
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
    
    const issues = await octokit.rest.issues.listForRepo({
      owner: context.repo.owner,
      repo: context.repo.repo,
      labels: 'workflow-failure',
      state: 'all',
      since: sevenDaysAgo.toISOString(),
      per_page: 10
    });
    
    enrichedData.relatedIssues = issues.data
      .filter(issue => issue.title.includes(workflowRun.name))
      .map(issue => ({
        number: issue.number,
        title: issue.title,
        state: issue.state,
        created: issue.created_at,
        url: issue.html_url
      }));
    
  } catch (error) {
    core.warning(`Error enriching context: ${error.message}`);
  }
  
  return enrichedData;
}

// Run the action
run();