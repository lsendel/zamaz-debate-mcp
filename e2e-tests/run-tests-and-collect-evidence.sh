#!/bin/bash

# E2E Test Runner and Evidence Collector
# This script runs all debate platform tests and collects comprehensive evidence

set -e

echo "🚀 Starting Debate Platform E2E Tests with Evidence Collection"
echo "=================================================="

# Create test evidence directory structure
TIMESTAMP=$(date +"%Y-%m-%d-%H-%M-%S")
TEST_RUN_DIR="test-evidence/test-runs/"""$TIMESTAMP""""
mkdir -p """"$TEST_RUN_DIR""""/{screenshots,videos,logs,artifacts,performance}

# Export environment variables
export NODE_ENV=test
export BASE_URL=http://localhost:3000
export API_BASE_URL=http://localhost:8080
export TEST_RUN_ID=$TIMESTAMP

# Install dependencies
echo "📦 Installing test dependencies..."
npm install

# Start services if not running
echo "🐳 Ensuring services are running..."
cd ..
docker-compose up -d
cd e2e-tests

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 30

# Run tests with evidence collection
echo "🧪 Running E2E tests..."
npx playwright test \
  --reporter=html,json,junit \
  --screenshot=on \
  --video=on \
  --trace=on \
  2>&1 | tee """"$TEST_RUN_DIR"""/logs/test-execution.log"

# Copy test results
echo "📁 Collecting test results..."
cp -r test-results/* """"$TEST_RUN_DIR"""/"

# Generate summary report
echo "📊 Generating evidence summary..."
node generate-evidence-summary.js """"$TEST_RUN_DIR""""

# Open HTML report
echo "🌐 Opening test report..."
npx playwright show-report

echo "✅ Test execution complete!"
echo "📍 Evidence collected at: """$TEST_RUN_DIR""""
echo ""
echo "Summary:"
echo "--------"
cat """"$TEST_RUN_DIR"""/executive-summary.txt"