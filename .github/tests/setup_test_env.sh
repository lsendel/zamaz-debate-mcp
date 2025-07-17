#!/bin/bash
# Setup test environment for Kiro GitHub Integration

set -euo pipefail

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}Setting up test environment...${NC}"

# Create test directories
mkdir -p test-results/{unit,integration,e2e,api,performance}
mkdir -p test-evidence/{screenshots,logs,reports}

# Create test environment file
cat > .env.test << EOF
# Test Environment Configuration
GITHUB_APP_ID=test-app-12345
GITHUB_WEBHOOK_SECRET=test-webhook-secret-xyz
GITHUB_TOKEN=ghp_test_token_1234567890
KIRO_BOT_USERNAME=kiro-ai-test
KIRO_API_KEY=test-api-key-abc123

# Service URLs
REDIS_URL=redis://localhost:6379/1
DATABASE_URL=postgresql://test:test@localhost:5432/kiro_test
WEBHOOK_URL=http://localhost:8080/webhooks/github

# API Configuration
API_PORT=8080
METRICS_PORT=9090

# Monitoring
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=test123
PROMETHEUS_URL=http://localhost:9091

# Email Configuration (test)
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USER=test@example.com
SMTP_PASSWORD=test123

# Slack Configuration (test)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/TEST/TEST/test

# Test Mode
TEST_MODE=true
LOG_LEVEL=DEBUG
PYTHON_ENV=test
EOF

# Create test requirements file
cat > .github/requirements/test.txt << EOF
# Testing dependencies
pytest==7.4.0
pytest-asyncio==0.21.0
pytest-cov==4.1.0
pytest-mock==3.11.1
pytest-timeout==2.1.0
pytest-benchmark==4.0.0

# API testing
httpx==0.24.1
requests-mock==1.11.0

# UI testing
playwright==1.36.0
pytest-playwright==0.4.2

# Mocking and fixtures
faker==19.2.0
factory-boy==3.3.0

# Code quality
black==23.7.0
ruff==0.0.282
mypy==1.4.1

# Security testing
bandit==1.7.5
safety==2.3.5

# Performance testing
locust==2.15.1
memory-profiler==0.61.0

# Test reporting
pytest-html==3.2.0
pytest-json-report==1.5.0
allure-pytest==2.13.2

# Dependencies from main services
aiohttp==3.8.5
aioredis==2.0.1
redis==4.6.0
pydantic==2.1.1
cryptography==41.0.3
PyJWT==2.8.0
python-dotenv==1.0.0
sentence-transformers==2.2.2
scikit-learn==1.3.0
numpy==1.25.1
EOF

# Install Python dependencies
echo -e "${YELLOW}Installing Python dependencies...${NC}"
pip install -r .github/requirements/test.txt

# Install Playwright browsers
echo -e "${YELLOW}Installing Playwright browsers...${NC}"
playwright install chromium

# Create mock data directory
mkdir -p .github/tests/fixtures
cat > .github/tests/fixtures/mock_data.json << EOF
{
  "pull_requests": [
    {
      "number": 123,
      "title": "Fix authentication bug",
      "body": "This PR fixes the authentication timeout issue",
      "state": "open",
      "user": {
        "login": "test-user"
      },
      "head": {
        "sha": "abc123",
        "ref": "fix-auth"
      },
      "base": {
        "ref": "main"
      }
    }
  ],
  "webhooks": [
    {
      "event": "pull_request",
      "action": "opened",
      "signature": "sha256=test_signature"
    }
  ],
  "github_responses": {
    "rate_limit": {
      "rate": {
        "limit": 5000,
        "remaining": 4999,
        "reset": 1234567890
      }
    }
  }
}
EOF

# Create test runner script
cat > .github/tests/run_all_tests.py << EOF
#!/usr/bin/env python3
"""
Comprehensive test runner for Kiro GitHub Integration
"""
import os
import sys
import json
import subprocess
from datetime import datetime
from pathlib import Path

# Test results directory
RESULTS_DIR = Path("test-results")
EVIDENCE_DIR = Path("test-evidence")

def run_command(cmd, name):
    """Run a command and capture output"""
    print(f"\\n{'='*60}")
    print(f"Running: {name}")
    print(f"Command: {cmd}")
    print('='*60)
    
    result = subprocess.run(
        cmd,
        shell=True,
        capture_output=True,
        text=True
    )
    
    # Save output
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_file = EVIDENCE_DIR / "logs" / f"{name}_{timestamp}.log"
    output_file.parent.mkdir(exist_ok=True)
    
    with open(output_file, 'w') as f:
        f.write(f"Command: {cmd}\\n")
        f.write(f"Exit Code: {result.returncode}\\n")
        f.write(f"\\nSTDOUT:\\n{result.stdout}")
        f.write(f"\\nSTDERR:\\n{result.stderr}")
    
    return result.returncode == 0, result

def main():
    """Run all tests"""
    print("Starting comprehensive test suite...")
    
    # Load test environment
    os.environ['ENV_FILE'] = '.env.test'
    
    results = {}
    
    # 1. Unit Tests
    success, result = run_command(
        "python -m pytest .github/tests/test_*.py -v --cov=.github/scripts --cov-report=html",
        "unit_tests"
    )
    results['unit_tests'] = {'success': success, 'output': result.stdout}
    
    # 2. Integration Tests
    success, result = run_command(
        "python -m pytest .github/tests/integration/ -v -m integration",
        "integration_tests"
    )
    results['integration_tests'] = {'success': success, 'output': result.stdout}
    
    # 3. Docker Tests
    success, result = run_command(
        "docker-compose -f .github/docker/docker-compose.yml ps",
        "docker_status"
    )
    results['docker_tests'] = {'success': success, 'output': result.stdout}
    
    # Save results
    results_file = RESULTS_DIR / f"test_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
    with open(results_file, 'w') as f:
        json.dump(results, f, indent=2)
    
    # Print summary
    print("\\n" + "="*60)
    print("TEST SUMMARY")
    print("="*60)
    
    for test_name, result in results.items():
        status = "✅ PASS" if result['success'] else "❌ FAIL"
        print(f"{test_name}: {status}")
    
    # Exit with error if any test failed
    if not all(r['success'] for r in results.values()):
        sys.exit(1)

if __name__ == "__main__":
    main()
EOF

chmod +x .github/tests/run_all_tests.py

# Create Docker test script
cat > .github/tests/test_docker.sh << EOF
#!/bin/bash
# Test Docker containers

echo "Testing Docker containers..."

# Start containers
docker-compose -f .github/docker/docker-compose.yml up -d

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 10

# Test health endpoints
echo "Testing health endpoints..."

# Webhook handler
curl -f http://localhost:8080/health || echo "Webhook handler health check failed"

# Metrics
curl -f http://localhost:9090/metrics || echo "Metrics endpoint failed"

# Redis
docker-compose -f .github/docker/docker-compose.yml exec -T redis redis-cli ping || echo "Redis ping failed"

# Show container status
docker-compose -f .github/docker/docker-compose.yml ps

# Show logs
docker-compose -f .github/docker/docker-compose.yml logs --tail=50
EOF

chmod +x .github/tests/test_docker.sh

echo -e "${GREEN}Test environment setup complete!${NC}"
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Start Redis: redis-server"
echo "2. Run tests: python .github/tests/run_all_tests.py"
echo "3. Check results in test-results/ directory"