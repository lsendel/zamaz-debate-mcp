#!/bin/bash

echo "🔍 Checking bash syntax in workflow-health-monitor.yml..."

# Extract the run script section and check it
awk '/run: \|/{flag=1; next} /^[[:space:]]*[a-zA-Z_-]+:/{flag=0} flag && /^[[:space:]]+/' .github/workflows/workflow-health-monitor.yml > /tmp/workflow-script.sh

# Check syntax
if bash -n /tmp/workflow-script.sh 2>&1; then
  echo "✅ Bash syntax is valid!"
else
  echo "❌ Bash syntax errors found:"
  bash -n /tmp/workflow-script.sh
fi

# Clean up
rm -f /tmp/workflow-script.sh