# MCP Endpoints Quick Reference

## Base URLs
- **Organization Service**: http://localhost:5005
- **LLM Service**: http://localhost:5002
- **Controller/Debate Service**: http://localhost:5013
- **RAG Service**: http://localhost:5018
- **Context Service**: http://localhost:5007

## Common MCP Endpoints (All Services)

```bash
# Get server info
GET /mcp

# List available tools
POST /mcp/list-tools

# Call a tool
POST /mcp/call-tool
```

---

## 🏢 Organization Service Tools

### Create Organization
```bash
curl -X POST http://localhost:5005/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "create_organization",
    "arguments": {
      "name": "My Organization",
      "description": "Organization description"
    }
  }'
```

### List Organizations
```bash
curl -X POST http://localhost:5005/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "list_organizations",
    "arguments": {}
  }'
```

### Get Organization
```bash
curl -X POST http://localhost:5005/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "get_organization",
    "arguments": {
      "id": "org_123456"
    }
  }'
```

### Update Organization
```bash
curl -X POST http://localhost:5005/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "update_organization",
    "arguments": {
      "id": "org_123456",
      "name": "Updated Name",
      "description": "Updated description"
    }
  }'
```

### Add User to Organization
```bash
curl -X POST http://localhost:5005/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "add_user_to_organization",
    "arguments": {
      "organizationId": "org_123456",
      "userId": "user_789",
      "role": "member"
    }
  }'
```

---

## 🤖 LLM Service Tools

### List Providers
```bash
curl -X POST http://localhost:5002/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list_providers",
    "arguments": {}
  }'
```

### Generate Completion
```bash
curl -X POST http://localhost:5002/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "generate_completion",
    "arguments": {
      "provider": "claude",
      "prompt": "Write a debate argument about climate change",
      "model": "claude-3-sonnet",
      "maxTokens": 500,
      "temperature": 0.7
    }
  }'
```

### Get Provider Status
```bash
curl -X POST http://localhost:5002/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "get_provider_status",
    "arguments": {
      "provider": "openai"
    }
  }'
```

---

## 💬 Controller/Debate Service Tools

### Create Debate
```bash
curl -X POST http://localhost:5013/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "create_debate",
    "arguments": {
      "topic": "Should AI be regulated?",
      "format": "OXFORD",
      "organizationId": "org_123456",
      "participants": ["user_1", "user_2", "ai_claude"],
      "maxRounds": 3
    }
  }'
```

### Get Debate
```bash
curl -X POST http://localhost:5013/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "get_debate",
    "arguments": {
      "debateId": "debate_abc123"
    }
  }'
```

### List Debates
```bash
curl -X POST http://localhost:5013/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "list_debates",
    "arguments": {
      "organizationId": "org_123456"
    }
  }'
```

### Submit Turn
```bash
curl -X POST http://localhost:5013/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "submit_turn",
    "arguments": {
      "debateId": "debate_abc123",
      "participantId": "user_1",
      "content": "I believe that AI regulation is necessary because..."
    }
  }'
```

---

## 📚 RAG Service Tools

### Index Document
```bash
curl -X POST http://localhost:5018/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "index_document",
    "arguments": {
      "organizationId": "org_123456",
      "documentId": "doc_research_001",
      "content": "Full text of the research paper...",
      "metadata": {
        "title": "AI Ethics Research",
        "author": "Dr. Smith",
        "date": "2025-07-18",
        "category": "ethics"
      }
    }
  }'
```

### Search Documents
```bash
curl -X POST http://localhost:5018/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "search",
    "arguments": {
      "organizationId": "org_123456",
      "query": "AI regulation ethics",
      "limit": 10
    }
  }'
```

### Get Context
```bash
curl -X POST http://localhost:5018/mcp/call-tool \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "get_context",
    "arguments": {
      "organizationId": "org_123456",
      "query": "What are the main arguments for AI regulation?",
      "maxTokens": 2000
    }
  }'
```

---

## 🔍 Context Service Tools

### Create Context
```bash
curl -X POST http://localhost:5007/tools/create_context \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "Debate Context",
    "description": "Context for AI regulation debate"
  }'
```

### Append Message
```bash
curl -X POST http://localhost:5007/tools/append_message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "contextId": "ctx_123456",
    "role": "user",
    "content": "What are the key points in favor of AI regulation?"
  }'
```

### Get Context Window
```bash
curl -X POST http://localhost:5007/tools/get_context_window \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "contextId": "ctx_123456",
    "maxTokens": 4096,
    "messageLimit": 50
  }'
```

### Search Contexts
```bash
curl -X POST http://localhost:5007/tools/search_contexts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "query": "AI regulation",
    "page": 0,
    "size": 20
  }'
```

### Share Context
```bash
curl -X POST http://localhost:5007/tools/share_context \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "contextId": "ctx_123456",
    "targetOrganizationId": "org_789012",
    "permission": "read"
  }'
```

---

## 🔐 Authentication

Most endpoints require JWT authentication. Include the token in the Authorization header:

```bash
-H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 📊 Rate Limits

| Operation Type | Default Limit | Examples |
|----------------|---------------|----------|
| READ | 100 req/sec | get_organization, list_debates |
| WRITE | 50 req/sec | update_organization, append_message |
| EXPENSIVE | 10 req/min | generate_completion, get_context_window |
| ADMIN | 3 req/hour | create_organization, delete_organization |

---

## 🚨 Common Response Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Process response |
| 401 | Unauthorized | Check JWT token |
| 403 | Forbidden | Check permissions |
| 429 | Rate Limited | Wait and retry |
| 500 | Server Error | Report issue |

---

## 📝 Testing Tools

```bash
# Pretty print JSON responses
| jq .

# Save response to file
> response.json

# Extract specific field
| jq '.debateId'

# Format timestamps
| jq '.createdAt | strftime("%Y-%m-%d %H:%M:%S")'
```