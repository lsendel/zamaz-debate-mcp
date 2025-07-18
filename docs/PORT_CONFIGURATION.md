# Port Configuration Guide

This document describes how ports are configured throughout the application using environment variables.

## Overview

All ports in the application are configurable through environment variables defined in the `.env` file. This allows for flexible deployment across different environments without changing code.

## Environment Variables

### Service Ports

These variables define the ports each service listens on:

- `MCP_ORGANIZATION_PORT` - Organization service (default: 5005)
- `MCP_LLM_PORT` - LLM service (default: 5002)
- `MCP_CONTROLLER_PORT` - Controller/Debate service (default: 5013)
- `MCP_RAG_PORT` - RAG service (default: 5004)
- `MCP_TEMPLATE_PORT` - Template service (default: 5006)
- `MCP_GATEWAY_PORT` - API Gateway (default: 8080)
- `MCP_CONTEXT_PORT` - Context service (default: 5001)
- `UI_PORT` / `VITE_PORT` - Frontend UI (default: 3001)

### Service URLs

For inter-service communication:

- `ORGANIZATION_SERVICE_URL` - Full URL for organization service
- `LLM_SERVICE_URL` - Full URL for LLM service
- `CONTROLLER_SERVICE_URL` - Full URL for controller service

### UI-Specific Variables

The UI uses Vite environment variables (prefixed with `VITE_`):

- `VITE_PORT` - UI development server port
- `VITE_ORGANIZATION_API_URL` - Organization API endpoint
- `VITE_LLM_API_URL` - LLM API endpoint
- `VITE_DEBATE_API_URL` - Debate API endpoint
- `VITE_RAG_API_URL` - RAG API endpoint
- `VITE_WS_URL` - WebSocket base URL

## Configuration Files

### 1. Root `.env` File

The main `.env` file at the project root contains all service ports and global configuration.

### 2. UI `.env` File

The `debate-ui/.env` file contains UI-specific environment variables that are injected at build time.

### 3. Service Configuration

Each Java service uses its port variable in `application.yml`:

```yaml
server:
  port: ${MCP_SERVICE_PORT:${SERVER_PORT:default_port}}
```

This provides multiple fallback levels:
1. Service-specific port (e.g., `MCP_ORGANIZATION_PORT`)
2. Generic server port (`SERVER_PORT`)
3. Default hardcoded port

### 4. CORS Configuration

CORS origins now use environment variables:

```yaml
cors:
  allowed-origins: ${CORS_ORIGINS:http://localhost:${UI_PORT:3001},http://localhost:${MCP_GATEWAY_PORT:8080}}
```

## Docker Compose

Docker Compose files use the same environment variables with default fallbacks:

```yaml
ports:
  - "${MCP_ORGANIZATION_PORT:-5005}:5005"
```

## Benefits

1. **Flexibility**: Easy to change ports for different environments
2. **No Code Changes**: Deploy to different environments without modifying code
3. **Conflict Avoidance**: Easily run multiple instances on different ports
4. **Security**: Production ports can be different from development
5. **Documentation**: All ports are clearly documented in `.env.example`

## Best Practices

1. Always use environment variables for ports
2. Provide sensible defaults
3. Document all variables in `.env.example`
4. Use consistent naming conventions
5. Never hardcode ports in application code

## Deployment

When deploying to different environments:

1. Copy `.env.example` to `.env`
2. Adjust port values as needed
3. Ensure no port conflicts with existing services
4. Update firewall rules if necessary
5. Restart services to apply changes