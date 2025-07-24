#!/bin/bash

echo "🧪 Testing Issue Creation Directly..."

# Test creating a simple issue without any labels
ISSUE_TITLE="Test: Workflow Health Monitor Issue Creation"
ISSUE_BODY="## Test Issue

This is a test issue created to verify that the GitHub CLI can create issues.

### Details
- Created at: $(date -u +%Y-%m-%dT%H:%M:%SZ)
- Purpose: Test issue creation without labels
- Expected result: Issue should be created successfully

If you see this issue, it means the basic issue creation is working."

echo "Creating test issue..."
if ISSUE_URL=$(gh issue create \
  --title "$ISSUE_TITLE" \
  --body "$ISSUE_BODY" 2>&1); then
  echo "✅ SUCCESS: Issue created: $ISSUE_URL"
else
  echo "❌ FAILED: Could not create issue"
  echo "Error: $ISSUE_URL"
  echo ""
  echo "Debugging information:"
  gh auth status
  echo ""
  echo "Repository info:"
  gh repo view --json name,owner
fi