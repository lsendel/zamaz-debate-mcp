#!/bin/bash

# Configuration Validation Script
# Validates that all required environment variables are set

set -e

echo "🔍 Validating configuration..."

# Required environment variables
required_vars=(
    "ORGANIZATION_SERVICE_URL"
    "LLM_SERVICE_URL"
    "CONTROLLER_SERVICE_URL"
    "RAG_SERVICE_URL"
    "TEMPLATE_SERVICE_URL"
    "CONTEXT_SERVICE_URL"
    "SECURITY_SERVICE_URL"
    "GATEWAY_SERVICE_URL"
    "WEBSOCKET_URL"
    "CORS_ALLOWED_ORIGINS"
    "MCP_ORGANIZATION_PORT"
    "MCP_LLM_PORT"
    "MCP_CONTROLLER_PORT"
    "MCP_RAG_PORT"
    "MCP_TEMPLATE_PORT"
    "MCP_CONTEXT_PORT"
    "MCP_SECURITY_PORT"
    "MCP_GATEWAY_PORT"
    "UI_PORT"
    "POSTGRES_USER"
    "POSTGRES_PASSWORD"
    "POSTGRES_DB"
    "POSTGRES_HOST"
    "POSTGRES_PORT"
    "REDIS_HOST"
    "REDIS_PORT"
    "CLAUDE_ENDPOINT"
    "OPENAI_ENDPOINT"
    "GEMINI_ENDPOINT"
    "OLLAMA_ENDPOINT"
)

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "❌ Error: .env file not found!"
    exit 1
fi

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

# Validate each required variable
missing_vars=()
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        missing_vars+=("$var")
    fi
done

if [ ${#missing_vars[@]} -eq 0 ]; then
    echo "✅ All required environment variables are set!"
else
    echo "❌ Missing required environment variables:"
    for var in "${missing_vars[@]}"; do
        echo "  - $var"
    done
    exit 1
fi

# Validate URLs format
echo "🔗 Validating URL formats..."
url_vars=(
    "ORGANIZATION_SERVICE_URL"
    "LLM_SERVICE_URL"
    "CONTROLLER_SERVICE_URL"
    "RAG_SERVICE_URL"
    "TEMPLATE_SERVICE_URL"
    "CONTEXT_SERVICE_URL"
    "SECURITY_SERVICE_URL"
    "GATEWAY_SERVICE_URL"
    "CLAUDE_ENDPOINT"
    "OPENAI_ENDPOINT"
    "GEMINI_ENDPOINT"
    "OLLAMA_ENDPOINT"
)

for var in "${url_vars[@]}"; do
    url="${!var}"
    if [[ ! $url =~ ^https?:// ]]; then
        echo "❌ Invalid URL format for $var: $url"
        exit 1
    fi
done

# Validate WebSocket URL
if [[ ! $WEBSOCKET_URL =~ ^wss?:// ]]; then
    echo "❌ Invalid WebSocket URL format: $WEBSOCKET_URL"
    exit 1
fi

# Validate port numbers
echo "🔌 Validating port numbers..."
port_vars=(
    "MCP_ORGANIZATION_PORT"
    "MCP_LLM_PORT"
    "MCP_CONTROLLER_PORT"
    "MCP_RAG_PORT"
    "MCP_TEMPLATE_PORT"
    "MCP_CONTEXT_PORT"
    "MCP_SECURITY_PORT"
    "MCP_GATEWAY_PORT"
    "UI_PORT"
    "POSTGRES_PORT"
    "REDIS_PORT"
)

for var in "${port_vars[@]}"; do
    port="${!var}"
    if ! [[ $port =~ ^[0-9]+$ ]] || [ $port -lt 1 ] || [ $port -gt 65535 ]; then
        echo "❌ Invalid port number for $var: $port"
        exit 1
    fi
done

echo "✅ Configuration validation completed successfully!"
