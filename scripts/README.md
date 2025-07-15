# Scripts Directory

This directory contains general infrastructure, testing, and utility scripts for the Zamaz Debate MCP project.

## Script Categories

### Docker Management
- `docker-cleanup.sh` - Clean up Docker containers and volumes
- `docker-cleanup-force.sh` - Force cleanup of Docker resources
- `fix-docker.sh` - Fix common Docker issues
- `wait-for-docker.sh` - Wait for Docker services to be ready

### Testing Scripts
- `run-all-tests.sh` - Run all project tests
- `run-ui-tests.sh` - Run UI-specific tests
- `test-databases.sh` - Test database connections
- `test-redis.sh` - Test Redis connection
- `validate-setup.sh` - Validate project setup
- `test-concurrency.js` - Test concurrent operations
- `test-websocket.js` - Test WebSocket connections
- `quick-ui-test.js` - Quick UI smoke test

### Development Utilities
- `start-mcp-services.sh` - Start all MCP services
- `mcp-full-client.sh` - Full MCP client testing
- `check-hardcoded-values.sh` - Check for hardcoded values in code
- `mock_llm_service.py` - Mock LLM service for testing

### Environment Setup
- `set-java-21.sh` - Set Java 21 environment
- `fix-lombok.sh` - Fix Lombok issues
- `fix-git-history.sh` - Fix git history issues

## Service-Specific Scripts

Service-specific scripts have been moved to their respective project directories:

- **Debate/Controller Scripts**: `mcp-controller/scripts/`
  - `debate-demo-curl.sh`
  - `complete-debate-demo.sh`
  - `mcp-debate-client.sh`
  - `test-debate-flow.sh`

- **LLM Scripts**: `mcp-llm/scripts/`
  - `llm-demo-curl.sh`
  - `simple-llm-example.sh`
  - `sky-color-example.sh`
  - `sky-color-mcp.sh`
  - `curl-sky-example.sh`
  - `test-llm.sh`