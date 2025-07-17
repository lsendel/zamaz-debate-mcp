# MCP API Test Examples

This document contains curl commands to test the MCP services APIs.

## Prerequisites

Ensure the following services are running:
- Organization Service on port 5005
- Context Service on port 5007
- Controller Service on port 5013

## 1. Organization Service API Tests

### Health Check
```bash
curl -X GET http://localhost:5005/actuator/health
```

### Create Organization
```bash
curl -X POST http://localhost:5005/mcp/tools/create_organization \
  -H "Content-Type: application/json" \
  -d '{
    "name": "AI Research Institute",
    "slug": "ai-research",
    "description": "Leading research organization for AI ethics and safety",
    "settings": {
      "allowedModels": ["gpt-4", "claude-3", "llama-3"],
      "maxUsers": 500,
      "features": {
        "advancedAnalytics": true,
        "customPrompts": true,
        "multiTenancy": true
      }
    }
  }'
```

### List Organizations
```bash
curl -X GET http://localhost:5005/api/organizations
```

### Get Organization by ID
```bash
# Replace {org-id} with actual organization ID
curl -X GET http://localhost:5005/api/organizations/{org-id}
```

### Update Organization
```bash
curl -X PUT http://localhost:5005/api/organizations/{org-id} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "AI Research Institute - Updated",
    "settings": {
      "maxUsers": 1000
    }
  }'
```

### Create User
```bash
curl -X POST http://localhost:5005/api/users \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: {org-id}" \
  -d '{
    "username": "john.doe",
    "email": "john.doe@example.com",
    "password": "SecurePassword123!",
    "roles": ["DEBATE_CREATOR", "PARTICIPANT"]
  }'
```

### User Login
```bash
curl -X POST http://localhost:5005/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePassword123!"
  }'
```

## 2. Context Service API Tests

### Health Check
```bash
curl -X GET http://localhost:5007/actuator/health
```

### Create Context
```bash
curl -X POST http://localhost:5007/api/contexts \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}" \
  -d '{
    "name": "Climate Change Debate Context",
    "type": "DEBATE",
    "metadata": {
      "topic": "Should carbon taxes be implemented globally?",
      "format": "OXFORD",
      "debateId": "debate-123",
      "tags": ["climate", "economics", "policy"]
    }
  }'
```

### Add Message to Context
```bash
curl -X POST http://localhost:5007/api/contexts/{context-id}/messages \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}" \
  -d '{
    "role": "moderator",
    "content": "Welcome to our debate on global carbon taxes. Our first speaker will argue in favor of implementation.",
    "metadata": {
      "participant": "moderator",
      "timestamp": "2025-07-16T10:00:00Z",
      "round": 0
    }
  }'
```

### Add AI Response
```bash
curl -X POST http://localhost:5007/api/contexts/{context-id}/messages \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}" \
  -d '{
    "role": "assistant",
    "content": "Thank you, moderator. Carbon taxes represent a crucial market-based solution to climate change...",
    "metadata": {
      "participant": "ai-speaker-1",
      "model": "gpt-4",
      "position": "for",
      "round": 1,
      "tokenCount": 245
    }
  }'
```

### Get Context Window
```bash
curl -X GET "http://localhost:5007/api/contexts/{context-id}/window?maxTokens=4096" \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}"
```

### Get Context Version History
```bash
curl -X GET "http://localhost:5007/api/contexts/{context-id}/versions" \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}"
```

### Share Context
```bash
curl -X POST http://localhost:5007/api/contexts/{context-id}/share \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}" \
  -d '{
    "targetOrganizationId": "{target-org-id}",
    "permissions": ["READ", "ADD_MESSAGE"],
    "expiresAt": "2025-12-31T23:59:59Z"
  }'
```

## 3. Controller Service API Tests

### Health Check
```bash
curl -X GET http://localhost:5013/actuator/health
```

### Create Debate
```bash
curl -X POST http://localhost:5013/api/debates \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}" \
  -d '{
    "organizationId": "{org-id}",
    "title": "The Future of Work: AI vs Human Employment",
    "description": "A comprehensive debate on whether AI will replace human workers",
    "topic": "Will artificial intelligence lead to mass unemployment?",
    "format": "LINCOLN_DOUGLAS",
    "maxRounds": 5,
    "settings": {
      "timePerRound": 300,
      "maxResponseLength": 1000,
      "votingEnabled": true,
      "publicAccess": true,
      "moderationLevel": "standard"
    }
  }'
```

### Add AI Participants
```bash
# Add first AI participant (Pro-AI)
curl -X POST http://localhost:5013/api/debates/{debate-id}/participants \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}" \
  -d '{
    "name": "TechOptimist AI",
    "type": "ai",
    "provider": "openai",
    "model": "gpt-4",
    "position": "for",
    "settings": {
      "temperature": 0.7,
      "maxTokens": 800,
      "systemPrompt": "You are an optimistic futurist who believes AI will create more jobs than it destroys."
    }
  }'

# Add second AI participant (AI Skeptic)
curl -X POST http://localhost:5013/api/debates/{debate-id}/participants \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}" \
  -d '{
    "name": "Labor Advocate AI",
    "type": "ai",
    "provider": "anthropic",
    "model": "claude-3-opus",
    "position": "against",
    "settings": {
      "temperature": 0.7,
      "maxTokens": 800,
      "systemPrompt": "You are a labor economist concerned about the displacement of workers by AI."
    }
  }'
```

### Start Debate
```bash
curl -X POST http://localhost:5013/api/debates/{debate-id}/start \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}"
```

### Submit Manual Response
```bash
curl -X POST http://localhost:5013/api/debates/{debate-id}/rounds/{round-id}/responses \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}" \
  -d '{
    "participantId": "{participant-id}",
    "content": "I believe that while AI will displace some jobs, history shows that technological progress creates new opportunities...",
    "metadata": {
      "sources": ["Economic History of Automation", "MIT Study on AI Impact"],
      "confidence": 0.85
    }
  }'
```

### Get Debate Status
```bash
curl -X GET http://localhost:5013/api/debates/{debate-id} \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}"
```

### List Debates with Filters
```bash
# Get all active debates
curl -X GET "http://localhost:5013/api/debates?status=IN_PROGRESS&organizationId={org-id}" \
  -H "Authorization: Bearer {jwt-token}"

# Get debates by format
curl -X GET "http://localhost:5013/api/debates?format=OXFORD&page=0&size=10" \
  -H "Authorization: Bearer {jwt-token}"
```

### Get Debate Results
```bash
curl -X GET http://localhost:5013/api/debates/{debate-id}/results \
  -H "X-Organization-Id: {org-id}" \
  -H "Authorization: Bearer {jwt-token}"
```

## 4. MCP Protocol Endpoints

### List Available Resources
```bash
# Organization Service
curl -X GET http://localhost:5005/mcp/resources

# Context Service
curl -X GET http://localhost:5007/mcp/resources

# Controller Service
curl -X GET http://localhost:5013/mcp/resources
```

### List Available Tools
```bash
# Each service exposes its tools
curl -X GET http://localhost:5005/mcp/tools
curl -X GET http://localhost:5007/mcp/tools
curl -X GET http://localhost:5013/mcp/tools
```

### Get Available Prompts
```bash
# Controller service prompts for debate management
curl -X GET http://localhost:5013/mcp/prompts
```

### Call MCP Tool
```bash
# Example: Create debate using MCP tool interface
curl -X POST http://localhost:5013/mcp/tools/create_debate \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: {org-id}" \
  -d '{
    "arguments": {
      "title": "Quick Debate on AI Safety",
      "topic": "Should AI development be paused?",
      "format": "OXFORD",
      "participants": [
        {
          "name": "Safety First AI",
          "model": "claude-3",
          "position": "for"
        },
        {
          "name": "Progress Advocate AI",
          "model": "gpt-4",
          "position": "against"
        }
      ]
    }
  }'
```

## 5. Advanced Scenarios

### Create and Start a Complete Debate Flow
```bash
# 1. Create organization
ORG_RESPONSE=$(curl -s -X POST http://localhost:5005/mcp/tools/create_organization \
  -H "Content-Type: application/json" \
  -d '{"name": "Debate Club", "slug": "debate-club"}')
ORG_ID=$(echo $ORG_RESPONSE | jq -r '.content[0].organizationId')

# 2. Create debate
DEBATE_RESPONSE=$(curl -s -X POST http://localhost:5013/api/debates \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: $ORG_ID" \
  -d '{
    "organizationId": "'$ORG_ID'",
    "title": "AI Ethics Debate",
    "topic": "Should AI have rights?",
    "format": "OXFORD"
  }')
DEBATE_ID=$(echo $DEBATE_RESPONSE | jq -r '.id')

# 3. Add participants
curl -X POST http://localhost:5013/api/debates/$DEBATE_ID/participants \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: $ORG_ID" \
  -d '{"name": "AI Rights Advocate", "type": "ai", "provider": "openai", "model": "gpt-4", "position": "for"}'

curl -X POST http://localhost:5013/api/debates/$DEBATE_ID/participants \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: $ORG_ID" \
  -d '{"name": "Human Rights Focus", "type": "ai", "provider": "anthropic", "model": "claude-3", "position": "against"}'

# 4. Start debate
curl -X POST http://localhost:5013/api/debates/$DEBATE_ID/start \
  -H "X-Organization-Id: $ORG_ID"

# 5. Check status
curl -X GET http://localhost:5013/api/debates/$DEBATE_ID \
  -H "X-Organization-Id: $ORG_ID"
```

## Error Handling Examples

### Invalid Request
```bash
# Missing required fields
curl -X POST http://localhost:5013/api/debates \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: {org-id}" \
  -d '{
    "title": "Incomplete Debate"
  }'
# Expected: 400 Bad Request with validation errors
```

### Unauthorized Access
```bash
# Missing authentication
curl -X GET http://localhost:5007/api/contexts/{context-id} \
  -H "X-Organization-Id: {wrong-org-id}"
# Expected: 403 Forbidden
```

### Resource Not Found
```bash
curl -X GET http://localhost:5013/api/debates/non-existent-id
# Expected: 404 Not Found
```