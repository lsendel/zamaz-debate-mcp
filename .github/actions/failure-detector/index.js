const core = require('@actions/core');
const github = require('@actions/github');

async function run() {
  try {
    // Get inputs
    const workflowName = core.getInput('workflow-name');
    const failureContext = core.getInput('failure-context');
    const severity = core.getInput('severity');
    const assignees = core.getInput('assignees');
    const labels = core.getInput('labels');

    // Basic validation
    if (!workflowName) {
      throw new Error('workflow-name is required');
    }

    // Prepare issue data structure
    const issueData = {
      workflow: {
        name: workflowName,
        context: failureContext,
        severity: severity
      },
      assignees: assignees ? assignees.split(',').map(a => a.trim()) : [],
      labels: labels ? labels.split(',').map(l => l.trim()) : ['workflow-failure', 'bug']
    };

    // Set outputs
    core.setOutput('issue-data', JSON.stringify(issueData));
    core.setOutput('should-create-issue', 'true');

    console.log(`Failure detected for workflow: ${workflowName}`);
  } catch (error) {
    core.setFailed(error.message);
  }
}

run();