# Multi-Tenant MCP Services Architecture

## Overview

Three separate MCP services with multi-tenant support:
1. **mcp-context**: Centralized context management service (multi-tenant)
2. **mcp-llm**: LLM provider gateway
3. **mcp-debate**: Debate orchestration service
4. **mcp-rag**: RAG (Retrieval Augmented Generation) service

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Organizations                         │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐   │
│  │  Org A  │  │  Org B  │  │  Org C  │  │  Org D  │   │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘   │
└───────┼────────────┼────────────┼────────────┼─────────┘
        │            │            │            │
        └────────────┴────────────┴────────────┘
                           │
                     MCP Protocol
                           │
        ┌──────────────────┴───────────────────┐
        │                                      │
        ▼                                      ▼
┌───────────────┐                    ┌──────────────┐
│  mcp-debate   │                    │   mcp-rag    │
│               │                    │              │
│ - Orchestrate │                    │ - Embeddings │
│ - Rules       │                    │ - Vector DB  │
│ - Flow        │                    │ - Retrieval  │
└───────┬───────┘                    └──────┬───────┘
        │                                   │
        │         ┌─────────────┐           │
        └────────►│ mcp-context │◄──────────┘
                  │             │
                  │ - Multi-org │
                  │ - Isolation │
                  │ - Storage   │
                  │ - Sharing   │
                  └──────┬──────┘
                         │
                         ▼
                  ┌─────────────┐
                  │   mcp-llm   │
                  │             │
                  │ - Providers │
                  │ - Gateway   │
                  │ - Caching   │
                  └─────────────┘
```

## Multi-Tenant Design

### Organization Model

```json
{
  "id": "org-123456",
  "name": "Example Organization",
  "tier": "enterprise",
  "settings": {
    "max_contexts": 1000,
    "max_tokens_per_context": 100000,
    "allowed_models": ["claude-3-opus", "gpt-4", "gemini-pro"],
    "rate_limits": {
      "requests_per_minute": 60,
      "requests_per_day": 10000
    }
  },
  "api_keys": [
    {
      "id": "key-abcdef",
      "name": "Production API Key",
      "scopes": ["context:read", "context:write", "debate:*"],
      "created_at": "2023-06-15T10:30:00Z",
      "last_used_at": "2023-06-20T14:22:15Z"
    }
  ]
}
```

### Multi-Tenant Isolation

1. **Data Isolation**
   - Each organization's data is completely isolated
   - Separate database schemas or collections
   - No cross-organization data access by default

2. **Resource Limits**
   - Per-organization quotas and rate limits
   - Tiered service levels (Basic, Pro, Enterprise)
   - Usage monitoring and alerts

3. **Authentication & Authorization**
   - Organization-specific API keys
   - JWT tokens with organization claims
   - Role-based access control within organizations

## MCP-Context Service

### Multi-Tenant Context Management

1. **Context Namespaces**
   - Each organization has isolated namespaces
   - Namespaces can be shared with specific organizations
   - Global namespaces for system-wide contexts

2. **Context Storage**
   - Efficient storage with compression
   - Tiered storage (hot/cold)
   - Automatic archiving of old contexts

3. **Context Operations**
   - Create context (with organization ID)
   - Append to context (with auth check)
   - Retrieve context (with auth check)
   - Share context (with explicit permission)

### Context Sharing Model

```json
{
  "context_id": "ctx-789012",
  "owner_org_id": "org-123456",
  "shared_with": [
    {
      "org_id": "org-654321",
      "access_level": "read",
      "expires_at": "2023-07-15T00:00:00Z"
    }
  ],
  "sharing_settings": {
    "allow_resharing": false,
    "require_attribution": true
  }
}
```

## MCP-LLM Service

### Multi-Tenant Provider Management

1. **Provider Configuration**
   - Global default providers
   - Organization-specific provider overrides
   - Custom provider endpoints per organization

2. **API Key Management**
   - System-managed keys (shared across orgs)
   - Organization-provided keys (isolated)
   - Key rotation and monitoring

3. **Usage Tracking**
   - Per-organization token counting
   - Cost allocation and billing
   - Usage analytics and reporting

### Provider Selection Logic

1. Check for organization-specific provider configuration
2. Fall back to system default providers
3. Apply organization tier restrictions
4. Select appropriate model based on request
5. Route to provider with available capacity

## MCP-Debate Service

### Multi-Tenant Debate Management

1. **Debate Isolation**
   - Each debate belongs to a single organization
   - Cross-organization debates with explicit sharing
   - Debate templates by organization

2. **Participant Management**
   - Internal participants (within organization)
   - External participants (invited from other orgs)
   - System participants (available to all orgs)

3. **Debate Analytics**
   - Organization-specific analytics
   - Comparative analytics (anonymized)
   - Usage patterns and optimization suggestions

### Debate Sharing Model

```json
{
  "debate_id": "dbt-345678",
  "owner_org_id": "org-123456",
  "participants": [
    {
      "id": "part-111",
      "name": "Internal Expert",
      "org_id": "org-123456",
      "role": "proponent"
    },
    {
      "id": "part-222",
      "name": "External Reviewer",
      "org_id": "org-654321",
      "role": "opponent",
      "access_level": "participant"
    }
  ]
}
```

## MCP-RAG Service

### Multi-Tenant Knowledge Management

1. **Knowledge Base Isolation**
   - Organization-specific knowledge bases
   - Shared knowledge bases (with permissions)
   - System knowledge bases (available to all)

2. **Document Management**
   - Organization-owned documents
   - Access control per document
   - Version control and audit trail

3. **Retrieval Policies**
   - Organization-specific retrieval settings
   - Custom relevance thresholds
   - Source attribution requirements

### Knowledge Base Sharing

```json
{
  "kb_id": "kb-901234",
  "owner_org_id": "org-123456",
  "name": "Industry Research",
  "shared_with": [
    {
      "org_id": "org-654321",
      "access_level": "read",
      "expires_at": null
    }
  ],
  "documents_count": 1250,
  "total_tokens": 3500000
}
```

## Cross-Service Authentication

### Authentication Flow

1. Client includes organization ID in request header
2. Gateway validates organization ID and API key
3. Gateway issues internal JWT with organization claims
4. Services validate JWT and enforce organization isolation
5. Services apply organization-specific policies and limits

### JWT Structure

```json
{
  "iss": "mcp-gateway",
  "sub": "api-key-12345",
  "org_id": "org-123456",
  "tier": "enterprise",
  "scopes": ["context:read", "context:write", "debate:*"],
  "exp": 1687267200,
  "jti": "unique-token-id"
}
```

## Implementation Considerations

1. **Database Design**
   - Organization ID as part of primary key
   - Foreign key constraints within organization boundary
   - Indexes optimized for organization-scoped queries

2. **API Design**
   - Organization ID required in all requests
   - Consistent error responses for isolation violations
   - Clear documentation of multi-tenant behavior

3. **Caching Strategy**
   - Cache keys include organization ID
   - Separate cache partitions per organization
   - Cache invalidation scoped to organization

4. **Monitoring and Alerting**
   - Per-organization usage metrics
   - Anomaly detection for unusual access patterns
   - Resource utilization by organization

5. **Testing Strategy**
   - Multi-tenant isolation tests
   - Cross-organization access tests
   - Performance tests with multi-tenant workloads
