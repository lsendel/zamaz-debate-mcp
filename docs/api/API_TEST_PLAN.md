# API Test Plan

This document contains comprehensive curl commands to test all MCP services.

## Prerequisites

1. Start all services:
```bash
docker-compose up -d
```

2. Wait for services to be healthy:
```bash
docker-compose ps
```

## Test Plan 1: Authentication & Organization Setup

### 1.1 Register a new user
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@zamaz.com",
    "password": "SecurePass123!",
    "firstName": "Admin",
    "lastName": "User",
    "organizationName": "Zamaz Corp"
  }'
```

Expected: 201 Created with user details and JWT token

### 1.2 Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@zamaz.com",
    "password": "SecurePass123!"
  }'
```

Expected: 200 OK with JWT token
Save the token: `export AUTH_TOKEN="<token>"`

### 1.3 Get current user profile
```bash
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

Expected: 200 OK with user profile including organization

### 1.4 Create additional organization
```bash
curl -X POST http://localhost:8080/api/v1/organizations \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Organization",
    "description": "A test organization for debates",
    "plan": "PROFESSIONAL"
  }'
```

Expected: 201 Created with organization details
Save the org ID: `export ORG_ID="<org-id>"`

## Test Plan 2: Debate Management

### 2.1 Create a debate
```bash
curl -X POST http://localhost:8080/api/v1/debates \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "'$ORG_ID'",
    "title": "AI Ethics Debate",
    "topic": "Should AI have rights?",
    "description": "A debate about AI consciousness and rights",
    "format": "structured",
    "maxRounds": 5,
    "settings": {
      "turnTimeoutSeconds": 300,
      "rules": "Standard debate rules apply",
      "isPublic": false
    }
  }'
```

Expected: 201 Created with debate details
Save the debate ID: `export DEBATE_ID="<debate-id>"`

### 2.2 List debates
```bash
curl -X GET "http://localhost:8080/api/v1/debates?organizationId=$ORG_ID" \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

Expected: 200 OK with list of debates

### 2.3 Get debate details
```bash
curl -X GET "http://localhost:8080/api/v1/debates/$DEBATE_ID" \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

Expected: 200 OK with debate details

### 2.4 Start debate
```bash
curl -X POST "http://localhost:8080/api/v1/debates/$DEBATE_ID/start" \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

Expected: 200 OK with updated debate status

## Test Plan 3: LLM Provider Testing

### 3.1 List available providers
```bash
curl -X GET http://localhost:8080/api/v1/llm/providers \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

Expected: 200 OK with list of providers (Claude, OpenAI, Gemini, Llama)

### 3.2 Test completion with Claude
```bash
curl -X POST http://localhost:8080/api/v1/llm/completions \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "model": "claude-3-opus-20240229",
    "messages": [
      {
        "role": "system",
        "content": "You are a helpful assistant."
      },
      {
        "role": "user",
        "content": "What is 2+2?"
      }
    ],
    "maxTokens": 100,
    "temperature": 0.7
  }'
```

Expected: 200 OK with completion response

### 3.3 Test streaming completion
```bash
curl -X POST http://localhost:8080/api/v1/llm/completions/stream \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "provider": "claude",
    "model": "claude-3-opus-20240229",
    "messages": [
      {
        "role": "user",
        "content": "Tell me a short story about a robot."
      }
    ],
    "stream": true
  }'
```

Expected: Server-sent events with streaming response

## Test Plan 4: Template Service

### 4.1 Create a debate template
```bash
curl -X POST http://localhost:8080/api/v1/templates \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "'$ORG_ID'",
    "name": "Standard Academic Debate",
    "description": "Template for academic debates",
    "category": "ACADEMIC",
    "metadata": {
      "format": "structured",
      "maxRounds": 5,
      "turnTimeoutSeconds": 300,
      "rules": "Academic debate rules",
      "isPublic": false
    }
  }'
```

Expected: 201 Created with template details
Save template ID: `export TEMPLATE_ID="<template-id>"`

### 4.2 Create debate from template
```bash
curl -X POST http://localhost:8080/api/v1/debates/from-template \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "'$ORG_ID'",
    "templateId": "'$TEMPLATE_ID'",
    "templateVariables": {
      "title": "Climate Change Debate",
      "topic": "Is climate change primarily human-caused?",
      "description": "An academic debate on climate science"
    }
  }'
```

Expected: 201 Created with new debate

## Test Plan 5: RAG Service

### 5.1 Create knowledge base
```bash
curl -X POST http://localhost:8080/api/v1/rag/knowledge-bases \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "'$ORG_ID'",
    "name": "Climate Science KB",
    "description": "Knowledge base for climate science debates",
    "type": "RESEARCH"
  }'
```

Expected: 201 Created with KB details
Save KB ID: `export KB_ID="<kb-id>"`

### 5.2 Upload document
```bash
curl -X POST "http://localhost:8080/api/v1/rag/knowledge-bases/$KB_ID/documents" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -F "file=@climate-research.pdf" \
  -F "metadata={\"source\":\"IPCC Report\",\"year\":\"2023\"}"
```

Expected: 201 Created with document details

### 5.3 Query knowledge base
```bash
curl -X POST "http://localhost:8080/api/v1/rag/knowledge-bases/$KB_ID/query" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is the current global temperature increase?",
    "topK": 5,
    "threshold": 0.7
  }'
```

Expected: 200 OK with relevant chunks

## Test Plan 6: Error Handling

### 6.1 Unauthorized access
```bash
curl -X GET http://localhost:8080/api/v1/debates
```

Expected: 401 Unauthorized

### 6.2 Invalid debate ID
```bash
curl -X GET http://localhost:8080/api/v1/debates/invalid-id \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

Expected: 404 Not Found

### 6.3 Rate limiting test
```bash
# Run this in a loop to trigger rate limiting
for i in {1..150}; do
  curl -X GET http://localhost:8080/api/v1/debates \
    -H "Authorization: Bearer $AUTH_TOKEN" &
done
wait
```

Expected: Some requests return 429 Too Many Requests

## Test Plan 7: WebSocket Testing

### 7.1 Connect to debate WebSocket
```bash
# Using wscat (install with: npm install -g wscat)
wscat -c "ws://localhost:8080/api/v1/debates/$DEBATE_ID/ws" \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

Expected: Connected to debate real-time updates

### 7.2 Send message
```
> {"type":"message","content":"This is a test argument","round":1}
```

Expected: Message broadcast to all participants

## Test Plan 8: Health Checks

### 8.1 Gateway health
```bash
curl http://localhost:8080/health
```

Expected: 200 OK with aggregated health status

### 8.2 Individual service health
```bash
# Organization service
curl http://localhost:5005/actuator/health

# LLM service
curl http://localhost:5002/actuator/health

# Controller service
curl http://localhost:5013/actuator/health

# RAG service
curl http://localhost:5004/actuator/health
```

Expected: 200 OK with service health details

## Test Plan 9: Performance Testing

### 9.1 Concurrent debate creation
```bash
# Create 10 debates concurrently
for i in {1..10}; do
  (curl -X POST http://localhost:8080/api/v1/debates \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "organizationId": "'$ORG_ID'",
      "title": "Concurrent Debate '$i'",
      "topic": "Test Topic '$i'",
      "format": "casual",
      "maxRounds": 3
    }' &)
done
wait
```

Expected: All debates created successfully

### 9.2 Load test LLM service
```bash
# Using Apache Bench
ab -n 100 -c 10 \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -p completion_request.json \
  http://localhost:8080/api/v1/llm/completions
```

Expected: Response times under 2s for 95th percentile

## Test Plan 10: Security Testing

### 10.1 SQL injection attempt
```bash
curl -X GET "http://localhost:8080/api/v1/debates?organizationId=' OR '1'='1" \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

Expected: 400 Bad Request or proper escaping

### 10.2 XSS attempt
```bash
curl -X POST http://localhost:8080/api/v1/debates \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "'$ORG_ID'",
    "title": "<script>alert(\"XSS\")</script>",
    "topic": "Test",
    "format": "casual"
  }'
```

Expected: Title properly escaped in response

### 10.3 JWT tampering
```bash
# Modify JWT token
TAMPERED_TOKEN=$(echo $AUTH_TOKEN | sed 's/./X/50')
curl -X GET http://localhost:8080/api/v1/debates \
  -H "Authorization: Bearer $TAMPERED_TOKEN"
```

Expected: 401 Unauthorized

## Cleanup

```bash
# Stop all services
docker-compose down

# Remove volumes (optional)
docker-compose down -v
```

## Notes

- Replace placeholder values (emails, IDs) with actual values from responses
- Some tests require files (e.g., PDF for RAG upload)
- WebSocket tests require wscat or similar tool
- Performance tests may require Apache Bench (ab) or similar tools
- Monitor logs during testing: `docker-compose logs -f`