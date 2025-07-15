# MCP Controller Scripts

This directory contains scripts specifically for testing and demonstrating the MCP Controller (Debate orchestration) service.

## Available Scripts

### `debate-demo-curl.sh`
Demonstrates basic debate operations using curl commands against the debate API.

### `complete-debate-demo.sh`
A comprehensive demo that creates and runs a complete debate from start to finish.

### `mcp-debate-client.sh`
MCP client script for testing debate service MCP endpoints.

### `test-debate-flow.sh`
Tests the complete debate flow including creation, rounds, and completion.

## Usage Examples

```bash
# Run a basic debate demo
./debate-demo-curl.sh

# Run a complete debate demo
./complete-debate-demo.sh

# Test debate MCP endpoints
./mcp-debate-client.sh

# Test debate flow
./test-debate-flow.sh
```

## Prerequisites

- MCP Controller service must be running (port 5013)
- LLM service must be available (port 5011)
- Database must be initialized