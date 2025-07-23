# Debugging Guide: Workflow Failure Not Creating Issues

## Quick Checklist

### 1. Check if the Failure Handler Job Ran

Look at your workflow run and verify:
- [ ] The `handle-failure` job appears in the workflow
- [ ] The `handle-failure` job shows as "Completed" or "Failed" (not "Skipped")
- [ ] The job condition `if: failure()` was met

### 2. Common Issues and Solutions

#### Issue: Failure Handler Job Skipped
**Symptom**: The `handle-failure` job shows as "Skipped" in gray

**Possible Causes**:
1. The `needs:` array doesn't include all jobs
2. The `if: failure()` condition wasn't met

**Solution**:
```yaml
handle-failure:
  if: failure()  # or always() if you want it to run always
  needs: [job1, job2, job3]  # List ALL jobs here
```

#### Issue: Missing GitHub Token
**Symptom**: Error message about authentication or 401/403 errors

**Solution**:
```yaml
handle-failure:
  uses: ./.github/workflows/workflow-failure-handler.yml
  secrets:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}  # This is required!
```

#### Issue: Workflow File Not Found
**Symptom**: Error "Unable to find workflow file"

**Solution**: Ensure the path is correct:
```yaml
uses: ./.github/workflows/workflow-failure-handler.yml  # Note the dot at the beginning
```

### 3. Check Workflow Logs

1. Go to the Actions tab in your repository
2. Click on the failed workflow run
3. Click on the `handle-failure` job
4. Look for these key log messages:

```
Workflow failure detected for [workflow name]
Issue data prepared with X failed job(s)
Creating issue: [issue title]
```

### 4. Verify Required Files Exist

Check that these files exist in your repository:
- [ ] `.github/workflows/workflow-failure-handler.yml`
- [ ] `.github/actions/failure-detector/action.yml`
- [ ] `.github/actions/failure-detector/index.js`
- [ ] `.github/scripts/issue-manager.js`
- [ ] `.github/config/workflow-issue-config.yml`

### 5. Token Permissions

The `GITHUB_TOKEN` needs these permissions:
- `issues: write` - To create and update issues
- `actions: read` - To read workflow run data
- `contents: read` - To read repository content

In your workflow, you can explicitly set permissions:
```yaml
jobs:
  handle-failure:
    permissions:
      issues: write
      actions: read
      contents: read
```

### 6. Debug Mode

Add debug mode to get more information:
```yaml
handle-failure:
  uses: ./.github/workflows/workflow-failure-handler.yml
  with:
    workflow-name: "Your Workflow Name"
    dry-run: true  # This will log what would happen without creating issues
```

### 7. Check for Duplicate Issues

The system may have found an existing open issue. Check:
1. Go to Issues tab
2. Search for: `label:workflow-failure is:issue "[Your Workflow Name]"`
3. Check both open and closed issues from the last 7 days

### 8. Manual Test

Create a test workflow to verify the setup:

```yaml
name: Test Failure Handler
on: workflow_dispatch

jobs:
  fail-on-purpose:
    runs-on: ubuntu-latest
    steps:
      - name: Fail
        run: exit 1
        
  handle-failure:
    if: failure()
    needs: [fail-on-purpose]
    uses: ./.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "Test Failure Handler"
      severity: "low"
      dry-run: false
    secrets:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## Detailed Debugging Steps

### Step 1: Verify Workflow Structure

Your workflow should have this structure:
```yaml
jobs:
  your-job-1:
    # ... your job steps
    
  your-job-2:
    # ... your job steps
    
  # This should be the LAST job
  handle-failure:
    if: failure()  # This is crucial
    needs: [your-job-1, your-job-2]  # List ALL job names
    uses: ./.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "Your Workflow Name"
    secrets:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Step 2: Check Action Logs

In the workflow run, expand the `handle-failure` job and look for:

1. **Set up job** - Should show green
2. **Run ./.github/workflows/workflow-failure-handler.yml** - Check for errors
3. Look for specific error messages:
   - "Error: Unable to find reusable workflow"
   - "Error: Input required and not supplied"
   - "Error creating issue: HttpError"

### Step 3: Verify Configuration

Check `.github/config/workflow-issue-config.yml`:
```yaml
workflows:
  "Your Workflow Name":  # Must match exactly
    severity: medium
    assignees: ["your-team"]
    labels: ["workflow-failure"]
```

### Step 4: Test Individual Components

Test the failure detector action directly:
```bash
cd .github/actions/failure-detector
npm install
GITHUB_TOKEN=your-token node index.js
```

## Common Error Messages and Solutions

| Error Message | Solution |
|--------------|----------|
| "Resource not accessible by integration" | Add `permissions: issues: write` to the job |
| "Unable to find workflow file" | Check the path starts with `./.github/` |
| "Input required and not supplied: workflow-name" | Add `workflow-name` to the `with:` section |
| "Cannot read property 'context' of undefined" | Ensure you're passing `GITHUB_TOKEN` in secrets |
| "API rate limit exceeded" | Wait and retry, or use a PAT with higher limits |

## Still Not Working?

If issues still aren't being created:

1. **Enable Debug Logging**:
   ```yaml
   env:
     ACTIONS_STEP_DEBUG: true
     ACTIONS_RUNNER_DEBUG: true
   ```

2. **Check Repository Settings**:
   - Ensure Issues are enabled in repository settings
   - Verify Actions are enabled
   - Check branch protection rules aren't blocking

3. **Try Minimal Configuration**:
   Remove all optional parameters and try the simplest setup:
   ```yaml
   handle-failure:
     if: failure()
     needs: [your-job]
     uses: ./.github/workflows/workflow-failure-handler.yml
     with:
       workflow-name: "Test"
     secrets:
       GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
   ```

4. **Verify File Permissions**:
   Ensure all files are committed and have proper permissions:
   ```bash
   git ls-files .github/workflows/workflow-failure-handler.yml
   git ls-files .github/actions/failure-detector/action.yml
   ```

## Getting Help

If you're still having issues, gather this information:
1. The complete workflow YAML file
2. The full logs from the `handle-failure` job
3. Any error messages from the Actions tab
4. The contents of your workflow configuration

Then you can share these details for further debugging assistance.