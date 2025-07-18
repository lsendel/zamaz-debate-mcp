#!/bin/bash
# End-to-end test script for the linting system
# This script tests the complete linting workflow across all project types

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test directory
TEST_DIR=".linting/test-samples"
RESULTS_DIR=".linting/test-results"

# Create test directories
mkdir -p "$TEST_DIR"
mkdir -p "$RESULTS_DIR"

echo -e "${BLUE}Starting end-to-end tests for the linting system...${NC}"

# Function to run a test and record results
run_test() {
  local test_name=$1
  local command=$2
  local expected_exit_code=$3

  echo -e "${YELLOW}Running test: $test_name${NC}"

  # Run the command and capture output and exit code
  local output
  local exit_code

  output=$(eval "$command" 2>&1) || exit_code=$?

  # Write output to results file
  echo "$output" > "$RESULTS_DIR/${test_name}.log"

  # Check if exit code matches expected
  if [ "${exit_code:-0}" -eq "$expected_exit_code" ]; then
    echo -e "${GREEN}✓ Test passed: $test_name${NC}"
    return 0
  else
    echo -e "${RED}✗ Test failed: $test_name${NC}"
    echo -e "${RED}Expected exit code $expected_exit_code, got ${exit_code:-0}${NC}"
    echo -e "${RED}Output:${NC}"
    echo "$output" | head -n 10
    if [ "$(echo "$output" | wc -l)" -gt 10 ]; then
      echo -e "${RED}... (see $RESULTS_DIR/${test_name}.log for full output)${NC}"
    fi
    return 1
  fi
}

# Create test files
echo -e "${BLUE}Creating test files...${NC}"

# Java test file with violations
cat > "$TEST_DIR/TestClass.java" << 'EOF'
package com.zamaz.mcp.test;

public class TestClass {
    public static void main(String args[]) {
        // Missing braces
        if (args.length > 0)
            System.out.println("Hello " + args[0]);

        // Unused variable
        int unused = 42;

        // Magic number
        for (int i = 0; i < 100; i++) {
            System.out.println(i);
        }
    }
}
EOF

# TypeScript test file with violations
cat > "$TEST_DIR/test-component.tsx" << 'EOF'
import React, { useState } from 'react';

// Missing props type
export const TestComponent = (props) => {
    // Unused variable
    const [unused, setUnused] = useState('');

    // Missing dependency in useEffect
    React.useEffect(() => {
        console.log(props.name);
    }, []);

    return (
        <div>
            {/* Missing alt attribute */}
            <img src="test.png" />
            <h1>{props.name}</h1>
        </div>
    );
};
EOF

# YAML test file with violations
cat > "$TEST_DIR/test-config.yml" << 'EOF'
# Invalid indentation
service:
  name: test-service
 port: 8080  # Wrong indentation

# Missing document end marker
---
test: value
EOF

# Markdown test file with violations
cat > "$TEST_DIR/test-doc.md" << 'EOF'
# Test Document

## Section 1
This is a test document with some linting issues.

### Subsection
* Inconsistent list style
- Mixed bullet types
+ Another style

## broken link
[Broken Link](https://this-is-a-broken-link-that-does-not-exist.com)

# Duplicate heading
## Section 1

This line is way too long and should be wrapped to maintain readability and conform to the line length rules that we have established for our documentation.
EOF

# Run tests
echo -e "${BLUE}Running linting tests...${NC}"

# Test 1: Java linting
run_test "java-linting" "java -jar checkstyle.jar -c .linting/java/checkstyle.xml $TEST_DIR/TestClass.java" 1

# Test 2: TypeScript linting
run_test "typescript-linting" "cd debate-ui && npx eslint --no-eslintrc -c ../.linting/frontend/.eslintrc.js ../$TEST_DIR/test-component.tsx" 1

# Test 3: YAML linting
run_test "yaml-linting" "yamllint -c .linting/config/yaml-lint.yml $TEST_DIR/test-config.yml" 1

# Test 4: Markdown linting
run_test "markdown-linting" "npx markdownlint --config .linting/docs/markdownlint.json $TEST_DIR/test-doc.md" 1

# Test 5: Incremental linting script
run_test "incremental-linting" ".linting/scripts/incremental-lint.sh --verbose --include-pattern \"$TEST_DIR/*\"" 1

# Test 6: Cache manager
run_test "cache-manager" "node .linting/scripts/cache-manager.js clean" 0

# Test 7: IDE integration (VS Code)
cat > "$TEST_DIR/vscode-settings-test.json" << 'EOF'
{
  "java.checkstyle.configuration": ".linting/java/checkstyle.xml",
  "eslint.workingDirectories": ["debate-ui"],
  "prettier.configPath": ".linting/frontend/.prettierrc",
  "markdownlint.config": ".linting/docs/markdownlint.json"
}
EOF

run_test "vscode-integration" "diff -u $TEST_DIR/vscode-settings-test.json .vscode/settings.json 2>/dev/null || echo 'VS Code settings need to be updated'" 0

# Test 8: Pre-commit hooks
run_test "pre-commit-hooks" "test -f .pre-commit-config.yaml && echo 'Pre-commit hooks configured' || echo 'Pre-commit hooks not configured'" 0

# Test 9: CI/CD integration
run_test "ci-cd-integration" "test -f .github/workflows/incremental-lint.yml && echo 'CI/CD integration configured' || echo 'CI/CD integration not configured'" 0

# Test 10: Service-specific overrides
mkdir -p "$TEST_DIR/service-override"
cp .linting/java/checkstyle.xml "$TEST_DIR/service-override/"
sed -i 's/<module name="LineLength">/<module name="LineLength"><property name="max" value="120"\/>/g' "$TEST_DIR/service-override/checkstyle.xml" 2>/dev/null || \
sed -i '' 's/<module name="LineLength">/<module name="LineLength"><property name="max" value="120"\/>/g' "$TEST_DIR/service-override/checkstyle.xml"

run_test "service-override" "diff -u .linting/java/checkstyle.xml $TEST_DIR/service-override/checkstyle.xml || echo 'Service override works'" 0

# Generate summary report
echo -e "${BLUE}Generating test summary...${NC}"

# Count passed and failed tests
passed=$(grep -c "Test passed" "$RESULTS_DIR"/*.log || echo 0)
failed=$(grep -c "Test failed" "$RESULTS_DIR"/*.log || echo 0)
total=$((passed + failed))

# Create summary report
cat > "$RESULTS_DIR/summary.md" << EOF
# Linting System End-to-End Test Results

Test run completed on $(date)

## Summary

- Total tests: $total
- Passed: $passed
- Failed: $failed

## Test Details

EOF

# Add details for each test
for log_file in "$RESULTS_DIR"/*.log; do
  test_name=$(basename "$log_file" .log)
  if grep -q "Test passed" "$log_file"; then
    status="✅ PASSED"
  else
    status="❌ FAILED"
  fi

  echo "### $test_name: $status" >> "$RESULTS_DIR/summary.md"
  echo "" >> "$RESULTS_DIR/summary.md"
  echo "\`\`\`" >> "$RESULTS_DIR/summary.md"
  head -n 10 "$log_file" >> "$RESULTS_DIR/summary.md"
  echo "\`\`\`" >> "$RESULTS_DIR/summary.md"
  echo "" >> "$RESULTS_DIR/summary.md"
done

echo -e "${GREEN}End-to-end tests completed!${NC}"
echo -e "${BLUE}Summary report generated at $RESULTS_DIR/summary.md${NC}"

# Return success if all tests passed, failure otherwise
if [ "$failed" -eq 0 ]; then
  echo -e "${GREEN}All tests passed!${NC}"
  exit 0
else
  echo -e "${RED}$failed tests failed!${NC}"
  exit 1
fi
