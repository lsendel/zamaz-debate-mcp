# Migration Guide - Adding Failure Handler to Existing Workflows

This guide helps you integrate the automated workflow issue creation system into your existing GitHub Actions workflows.

## Quick Migration Steps

### 1. Identify Workflows to Migrate

List your existing workflows:
```bash
ls -la .github/workflows/*.yml
```

Prioritize by:
- **Critical**: Production deployments, security scans
- **High**: CI/CD pipelines, main branch builds
- **Medium**: Testing, code quality checks
- **Low**: Documentation, experimental workflows

### 2. Basic Integration Pattern

Add this job to the end of any workflow:

```yaml
  # Add at the end of your workflow file
  handle-failure:
    if: failure()
    needs: [job1, job2, job3]  # List ALL job names from your workflow
    uses: ./.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "Your Workflow Name"
      severity: "medium"  # or critical, high, low
    secrets:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### 3. Migration Examples

#### Simple Workflow

**Before:**
```yaml
name: Build and Test
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm install
      - run: npm test
```

**After:**
```yaml
name: Build and Test
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm install
      - run: npm test
      
  # Added: Failure handler
  handle-failure:
    if: failure()
    needs: [build]
    uses: ./.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "Build and Test"
      severity: "medium"
      assignees: "dev-team"
    secrets:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

#### Complex Multi-Job Workflow

**Before:**
```yaml
name: CI/CD Pipeline
on: [push]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps: [...]
    
  test:
    runs-on: ubuntu-latest
    needs: [lint]
    steps: [...]
    
  build:
    runs-on: ubuntu-latest
    needs: [test]
    steps: [...]
    
  deploy:
    runs-on: ubuntu-latest
    needs: [build]
    if: github.ref == 'refs/heads/main'
    steps: [...]
```

**After:**
```yaml
name: CI/CD Pipeline
on: [push]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps: [...]
    
  test:
    runs-on: ubuntu-latest
    needs: [lint]
    steps: [...]
    
  build:
    runs-on: ubuntu-latest
    needs: [test]
    steps: [...]
    
  deploy:
    runs-on: ubuntu-latest
    needs: [build]
    if: github.ref == 'refs/heads/main'
    steps: [...]
    
  # Added: Smart failure handler
  handle-failure:
    if: failure()
    needs: [lint, test, build, deploy]  # ALL jobs
    uses: ./.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "CI/CD Pipeline"
      # Dynamic severity based on what failed
      severity: ${{ 
        (needs.deploy.result == 'failure') && 'critical' ||
        (github.ref == 'refs/heads/main') && 'high' ||
        'medium'
      }}
      # Dynamic assignees
      assignees: ${{
        (needs.deploy.result == 'failure') && 'devops-team,oncall' ||
        (needs.lint.result == 'failure') && 'code-quality-team' ||
        'dev-team'
      }}
      # Comprehensive labels
      labels: "workflow-failure,ci-cd,${{
        (needs.deploy.result == 'failure') && 'deployment,' || ''
      }}${{ github.ref_name }}"
      # Template selection
      template: ${{
        (needs.deploy.result == 'failure') && 'deployment' ||
        (needs.lint.result == 'failure') && 'linting' ||
        'ci-cd'
      }}
      notify-slack: true
      notify-email: ${{ github.ref == 'refs/heads/main' }}
    secrets:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      SLACK_WEBHOOK: ${{ secrets.SLACK_WEBHOOK }}
      SMTP_USER: ${{ secrets.SMTP_USER }}
      SMTP_PASS: ${{ secrets.SMTP_PASS }}
```

## Advanced Migration Patterns

### 1. Matrix Strategy Workflows

For workflows using matrix strategies:

```yaml
jobs:
  test:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        node: [14, 16, 18]
    runs-on: ${{ matrix.os }}
    steps: [...]
    
  handle-failure:
    if: failure()
    needs: [test]
    uses: ./.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "Cross-Platform Tests"
      severity: "high"
      # Include matrix information in labels
      labels: "workflow-failure,testing,matrix-build"
```

### 2. Conditional Jobs

For workflows with conditional jobs:

```yaml
jobs:
  dev-build:
    if: github.ref != 'refs/heads/main'
    runs-on: ubuntu-latest
    steps: [...]
    
  prod-build:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps: [...]
    
  handle-failure:
    if: failure()
    needs: [dev-build, prod-build]
    uses: ./.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "Conditional Build Pipeline"
      # Different handling for prod vs dev
      severity: ${{
        (needs.prod-build.result == 'failure') && 'critical' ||
        'low'
      }}
```

### 3. Reusable Workflows

For workflows that call other workflows:

```yaml
jobs:
  call-build:
    uses: ./.github/workflows/build.yml
    
  call-test:
    uses: ./.github/workflows/test.yml
    needs: [call-build]
    
  handle-failure:
    if: failure()
    needs: [call-build, call-test]
    uses: ./.github/workflows/workflow-failure-handler.yml
    with:
      workflow-name: "Main Pipeline"
      severity: "high"
```

## Configuration Migration

### 1. Create Workflow-Specific Config

Add your workflows to `.github/config/workflow-issue-config.yml`:

```yaml
workflows:
  "Your Existing Workflow":
    severity: medium
    assignees: ["your-team"]
    labels: ["workflow-failure", "your-component"]
    template: "default"  # or create custom template
    escalation_threshold: 5
    notification_channels:
      slack: "#your-channel"
```

### 2. Custom Templates

Create custom templates for specific workflows:

```bash
# Create custom template
touch .github/templates/workflow-issues/custom/your-workflow.md
```

Template example:
```markdown
## {{workflow.name}} Failure

**Component:** Your Component
**Impact:** Describe impact here

### Failure Details
{{> default }}  # Include default template content

### Specific Troubleshooting
- Check your specific configuration
- Review recent changes to X
- Contact @your-team for help
```

## Testing Your Migration

### 1. Dry Run Test

First test with dry-run mode:

```yaml
with:
  workflow-name: "Your Workflow"
  dry-run: true  # No issues created
```

### 2. Force a Test Failure

Create a test branch with intentional failure:

```yaml
- name: Test failure
  run: exit 1  # Force failure
```

### 3. Verify Issue Creation

Check that:
- Issue is created with correct title
- Labels are applied correctly
- Assignees are set
- Template renders properly
- Notifications are sent (if configured)

## Migration Checklist

For each workflow:

- [ ] Add failure handler job
- [ ] List all job dependencies in `needs:`
- [ ] Set appropriate severity level
- [ ] Configure assignees and labels
- [ ] Choose or create template
- [ ] Add to workflow-issue-config.yml
- [ ] Test in dry-run mode
- [ ] Test with real failure
- [ ] Document any custom configuration

## Common Migration Issues

### 1. Missing Job Dependencies

**Problem**: Failure handler doesn't trigger
**Solution**: Ensure ALL jobs are listed in `needs:`

```yaml
# BAD - Missing jobs
needs: [build]

# GOOD - All jobs listed
needs: [lint, test, build, deploy]
```

### 2. Incorrect Conditionals

**Problem**: Handler runs when it shouldn't
**Solution**: Use proper conditional

```yaml
# BAD - Always runs
if: always()

# GOOD - Only on failure
if: failure()
```

### 3. Dynamic Value Errors

**Problem**: Expression syntax errors
**Solution**: Test expressions carefully

```yaml
# Test your expressions
severity: ${{ (false && 'high') || 'medium' }}  # Results in 'medium'
```

## Gradual Migration Strategy

### Phase 1: Non-Critical Workflows (Week 1)
- Documentation builds
- Non-production branches
- Experimental workflows

### Phase 2: Testing Workflows (Week 2)
- Unit test workflows
- Integration test workflows
- E2E test workflows

### Phase 3: Build Workflows (Week 3)
- Development builds
- PR validation workflows
- Branch protection workflows

### Phase 4: Critical Workflows (Week 4)
- Production deployments
- Security scans
- Release workflows

## Post-Migration

### 1. Monitor Issue Creation
- Review created issues weekly
- Adjust severity levels as needed
- Refine assignee lists

### 2. Optimize Templates
- Update templates based on common failures
- Add workflow-specific troubleshooting
- Include links to documentation

### 3. Review Metrics
- Track issue resolution time
- Monitor false positive rate
- Measure team response time

## Rollback Plan

If issues arise:

1. **Quick Disable**: Comment out the handler job
2. **Selective Disable**: Add `if: false` to handler job
3. **Full Removal**: Delete handler job from workflow

Always maintain a backup of the original workflow file before migration.