#!/bin/bash
# Quick runner for Project Linter Integration Tests
# Ensures 80% functionality coverage

set -e

echo "🚀 Starting Project Linter Integration Tests"
echo "Target: 80% functionality coverage"
echo ""

# Run the master integration test suite
.linting/scripts/master-integration-test.sh

echo ""
echo "✅ Integration tests completed!"
echo "📊 Check .linting/test-results/master/ for detailed reports"