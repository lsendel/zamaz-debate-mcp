# MCP Endpoints Testing Plan

## Overview

This document provides a comprehensive testing plan for all MCP (Model Context Protocol) endpoints across the zamaz-debate-mcp services.

## Services and MCP Endpoints

### 1. MCP Organization Service (Port 5005)

**Base URL**: `http://localhost:5005`

#### MCP Endpoints:
- `GET /mcp` - Get server info
- `POST /mcp/list-tools` - List available MCP tools
- `POST /mcp/call-tool` - Call a specific MCP tool

#### Available Tools:
- `create_organization` - Create a new organization
- `list_organizations` - List all organizations
- `get_organization` - Get organization details
- `update_organization` - Update organization info
- `add_user_to_organization` - Add user to organization

### 2. MCP LLM Service (Port 5002)

**Base URL**: `http://localhost:5002`

#### MCP Endpoints:
- `GET /mcp` - Get server info
- `POST /mcp/list-tools` - List available MCP tools
- `POST /mcp/call-tool` - Call a specific MCP tool

#### Available Tools:
- `list_providers` - List available LLM providers
- `generate_completion` - Generate text completion
- `get_provider_status` - Check provider health status

### 3. MCP Controller/Debate Service (Port 5013)

**Base URL**: `http://localhost:5013`

#### MCP Endpoints:
- `GET /mcp` - Get server info
- `POST /mcp/list-tools` - List available MCP tools
- `POST /mcp/call-tool` - Call a specific MCP tool

#### Available Tools:
- `create_debate` - Create a new debate
- `get_debate` - Get debate details
- `list_debates` - List debates for an organization
- `submit_turn` - Submit a turn in a debate

### 4. MCP RAG Service (Port 5018)

**Base URL**: `http://localhost:5018`

#### MCP Endpoints:
- `GET /mcp` - Get server info
- `POST /mcp/list-tools` - List available MCP tools
- `POST /mcp/call-tool` - Call a specific MCP tool

#### Available Tools:
- `index_document` - Index a document for RAG
- `search` - Search indexed documents
- `get_context` - Get context for a query

## Testing Instructions

### 1. Start Services

```bash
cd infrastructure/docker-compose

# Start all services
docker-compose up -d

# Wait for services to be healthy
docker-compose ps

# Check logs if needed
docker-compose logs -f [service-name]
```

### 2. Run Automated Tests

```bash
# Run the comprehensive test script
./test-mcp-endpoints.sh
```

### 3. Manual Testing with cURL

#### Test MCP Organization Service

```bash
# Get server info
curl -X GET http://localhost:5005/mcp

# List available tools
curl -X POST http://localhost:5005/mcp/list-tools \
  -H "Content-Type: application/json" \
  -d '{}'

# Create organization
curl -X POST http://localhost:5005/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "create_organization",
    "arguments": {
      "name": "Test Org",
      "description": "Test organization"
    }
  }'

# List organizations
curl -X POST http://localhost:5005/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list_organizations",
    "arguments": {}
  }'
```

#### Test MCP LLM Service

```bash
# Get server info
curl -X GET http://localhost:5002/mcp

# List available tools
curl -X POST http://localhost:5002/mcp/list-tools \
  -H "Content-Type: application/json" \
  -d '{}'

# List providers
curl -X POST http://localhost:5002/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list_providers",
    "arguments": {}
  }'

# Generate completion
curl -X POST http://localhost:5002/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "generate_completion",
    "arguments": {
      "provider": "openai",
      "prompt": "What is the capital of France?",
      "maxTokens": 50
    }
  }'

# Check provider status
curl -X POST http://localhost:5002/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "get_provider_status",
    "arguments": {
      "provider": "claude"
    }
  }'
```

#### Test MCP Controller/Debate Service

```bash
# Get server info
curl -X GET http://localhost:5013/mcp

# List available tools
curl -X POST http://localhost:5013/mcp/list-tools \
  -H "Content-Type: application/json" \
  -d '{}'

# Create debate
curl -X POST http://localhost:5013/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "create_debate",
    "arguments": {
      "topic": "Should AI be regulated?",
      "format": "OXFORD",
      "organizationId": "test-org-001",
      "maxRounds": 3
    }
  }'

# List debates
curl -X POST http://localhost:5013/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list_debates",
    "arguments": {
      "organizationId": "test-org-001"
    }
  }'

# Get debate (replace DEBATE_ID with actual ID)
curl -X POST http://localhost:5013/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "get_debate",
    "arguments": {
      "debateId": "DEBATE_ID"
    }
  }'
```

## Expected Responses

### Successful MCP Server Info Response
```json
{
  "name": "mcp-[service-name]",
  "version": "1.0.0",
  "description": "[Service description]",
  "capabilities": {
    "tools": true,
    "resources": true
  }
}
```

### Successful Tool List Response
```json
{
  "tools": [
    {
      "name": "tool_name",
      "description": "Tool description",
      "parameters": {
        "type": "object",
        "properties": {
          // Parameter definitions
        }
      }
    }
  ]
}
```

### Successful Tool Call Response
The response varies by tool but should contain the requested data or confirmation of the action performed.

## Troubleshooting

### Common Issues

1. **Service Not Responding**
   - Check if the service is running: `docker-compose ps`
   - Check service logs: `docker-compose logs [service-name]`
   - Verify port is not already in use: `lsof -i :PORT`

2. **Authentication Errors**
   - Ensure JWT tokens are properly configured
   - Check environment variables in docker-compose.yml

3. **Database Connection Issues**
   - Verify PostgreSQL is running and healthy
   - Check database initialization scripts
   - Verify database credentials

4. **API Key Issues (LLM Service)**
   - Ensure API keys are set in environment variables:
     - `ANTHROPIC_API_KEY` for Claude
     - `OPENAI_API_KEY` for OpenAI
     - `GOOGLE_API_KEY` for Gemini

### Debug Commands

```bash
# Check all service health endpoints
for port in 5005 5002 5013 5018; do
  echo "Checking port $port:"
  curl -s http://localhost:$port/actuator/health | jq . || echo "Failed"
done

# View service logs
docker-compose logs -f mcp-organization
docker-compose logs -f mcp-llm
docker-compose logs -f mcp-controller
docker-compose logs -f mcp-rag

# Check database connections
docker-compose exec postgres psql -U postgres -c "\l"

# Check Redis connection
docker-compose exec redis redis-cli ping
```

## Testing Results Documentation

After running tests, document results in the following format:

### Test Results Template

```markdown
## MCP Endpoints Test Results - [DATE]

### Environment
- Docker version: X.X.X
- Services running: [list]
- Test duration: XX minutes

### Test Summary
- Total endpoints tested: XX
- Successful: XX
- Failed: XX

### Detailed Results

#### MCP Organization Service
- [ ] GET /mcp - Server info
- [ ] POST /mcp/list-tools - List tools
- [ ] POST /mcp/call-tool - create_organization
- [ ] POST /mcp/call-tool - list_organizations

#### MCP LLM Service
- [ ] GET /mcp - Server info
- [ ] POST /mcp/list-tools - List tools
- [ ] POST /mcp/call-tool - list_providers
- [ ] POST /mcp/call-tool - generate_completion
- [ ] POST /mcp/call-tool - get_provider_status

#### MCP Controller Service
- [ ] GET /mcp - Server info
- [ ] POST /mcp/list-tools - List tools
- [ ] POST /mcp/call-tool - create_debate
- [ ] POST /mcp/call-tool - list_debates
- [ ] POST /mcp/call-tool - get_debate

### Issues Found
1. [Issue description]
2. [Issue description]

### Recommendations
1. [Recommendation]
2. [Recommendation]
```

## Next Steps

1. Run the automated test script
2. Document any failures or issues
3. Create tickets for any bugs found
4. Update this documentation with new findings
5. Consider adding integration tests to CI/CD pipeline