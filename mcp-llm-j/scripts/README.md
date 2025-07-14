# MCP LLM Scripts

This directory contains scripts specifically for testing and demonstrating the MCP LLM service.

## Available Scripts

### `llm-demo-curl.sh`
Demonstrates LLM service operations using curl commands.

### `simple-llm-example.sh`
A simple example showing basic LLM completion requests.

### `sky-color-example.sh`
Example script demonstrating the classic "What color is the sky?" prompt.

### `sky-color-mcp.sh`
Sky color example using MCP protocol.

### `curl-sky-example.sh`
Curl-based sky color example for quick testing.

### `test-llm.sh`
Comprehensive test script for LLM service functionality.

## Usage Examples

```bash
# Test basic LLM functionality
./simple-llm-example.sh

# Run LLM demo with various prompts
./llm-demo-curl.sh

# Test sky color prompt
./sky-color-example.sh

# Run comprehensive LLM tests
./test-llm.sh
```

## Prerequisites

- MCP LLM service must be running (port 5011)
- Valid API keys must be configured in environment
- Redis must be available for caching