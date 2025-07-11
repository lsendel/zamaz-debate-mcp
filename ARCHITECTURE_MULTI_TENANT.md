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
                  └─────────────┘
```

## MCP-Context Service (Multi-Tenant)

### Core Features
1. **Organization Isolation**: Complete data isolation between orgs
2. **Context Namespaces**: Organize contexts by purpose
3. **Sharing Mechanisms**: Cross-org context sharing with permissions
4. **Version Control**: Track context changes over time
5. **Access Control**: Fine-grained permissions

### Data Model
```python
class Organization:
    id: str
    name: str
    api_key: str  # For authentication
    settings: Dict[str, Any]
    created_at: datetime
    
class ContextNamespace:
    id: str
    org_id: str
    name: str  # e.g., "debates", "support", "research"
    description: str
    access_policy: AccessPolicy
    
class Context:
    id: str
    namespace_id: str
    org_id: str
    name: str
    messages: List[Message]
    metadata: Dict[str, Any]
    version: int
    created_at: datetime
    updated_at: datetime
    shared_with: List[str]  # Other org IDs
    
class ContextShare:
    id: str
    context_id: str
    source_org_id: str
    target_org_id: str
    permissions: List[str]  # ["read", "append", "fork"]
    expires_at: Optional[datetime]
```

### MCP Resources
- `/organizations/{org_id}/namespaces` - List namespaces
- `/namespaces/{namespace_id}/contexts` - List contexts
- `/contexts/{context_id}` - Get specific context
- `/contexts/{context_id}/messages` - Get messages
- `/contexts/{context_id}/versions` - Version history
- `/shared-contexts` - Contexts shared with org

### MCP Tools
- `create_context`
  ```json
  {
    "org_id": "org-123",
    "namespace": "debates",
    "name": "Climate Debate 2024",
    "initial_messages": [],
    "metadata": {}
  }
  ```
- `append_to_context`
- `get_context_window` - Get optimized window
- `share_context`
- `fork_context`
- `compress_context`
- `search_contexts`

### Storage Architecture
```
PostgreSQL Database:
├── organizations
├── namespaces
├── contexts
├── context_messages
├── context_versions
├── context_shares
└── audit_logs

Redis Cache:
├── active_contexts:{org_id}
├── context_windows:{context_id}
└── access_tokens:{org_id}
```

## MCP-RAG Service

### Purpose
Enhance LLM responses with relevant information from knowledge bases.

### Features
1. **Multi-Source Ingestion**: Documents, APIs, databases
2. **Vector Storage**: Embeddings for semantic search
3. **Hybrid Search**: Combine vector + keyword search
4. **Context Injection**: Seamlessly add to prompts
5. **Source Attribution**: Track information sources

### Data Model
```python
class KnowledgeBase:
    id: str
    org_id: str
    name: str
    description: str
    index_config: IndexConfig
    
class Document:
    id: str
    kb_id: str
    source_url: str
    content: str
    metadata: Dict[str, Any]
    embeddings: List[float]
    created_at: datetime
    
class SearchResult:
    document_id: str
    content: str
    score: float
    metadata: Dict[str, Any]
```

### MCP Tools
- `create_knowledge_base`
- `ingest_documents`
- `search`
  ```json
  {
    "kb_id": "kb-123",
    "query": "climate change impacts",
    "max_results": 5,
    "min_score": 0.7
  }
  ```
- `augment_context` - Add RAG results to context
- `update_embeddings`

## Integration Pattern

### Example: Debate with RAG Enhancement

```python
# 1. mcp-debate creates debate context
debate_context = await context_client.create_context(
    org_id="org-123",
    namespace="debates",
    name="AI Ethics Debate"
)

# 2. When preparing participant turn, query RAG
rag_results = await rag_client.search(
    kb_id="ethics-kb",
    query=last_message.content,
    max_results=3
)

# 3. Augment context with RAG results
augmented_context = await context_client.append_to_context(
    context_id=debate_context.id,
    messages=[
        Message(
            role="system",
            content=f"Relevant information:\n{format_rag_results(rag_results)}"
        )
    ]
)

# 4. Get optimized context window
context_window = await context_client.get_context_window(
    context_id=debate_context.id,
    max_tokens=8000,
    strategy="sliding_window_with_summary"
)

# 5. Call LLM with augmented context
response = await llm_client.complete(
    provider="claude",
    model="claude-3-opus",
    messages=context_window.messages
)

# 6. Store response back in context
await context_client.append_to_context(
    context_id=debate_context.id,
    messages=[
        Message(
            role="assistant",
            content=response.content,
            metadata={"sources": rag_results}
        )
    ]
)
```

## Security & Multi-Tenancy

### Authentication Flow
```
1. Client → API Gateway (API Key validation)
2. API Gateway → Service (JWT with org_id, permissions)
3. Service → Database (Row-level security by org_id)
```

### Isolation Mechanisms
1. **Database**: Row-level security policies
2. **Cache**: Namespaced keys by org_id
3. **Vector DB**: Separate indices per org
4. **API**: Org-scoped endpoints

### Context Sharing Security
```python
class ContextSharingPolicy:
    def can_share(self, source_org: str, target_org: str) -> bool:
        # Check if orgs have sharing agreement
        pass
    
    def filter_shared_content(self, context: Context) -> Context:
        # Remove sensitive data before sharing
        pass
    
    def audit_access(self, org_id: str, context_id: str):
        # Log all cross-org access
        pass
```

## Benefits of This Architecture

1. **Scalability**
   - Each service scales independently
   - Context service can be geographically distributed
   - RAG indices can be sharded by organization

2. **Multi-Tenancy**
   - Complete isolation between organizations
   - Flexible sharing mechanisms
   - Per-org usage tracking and billing

3. **Flexibility**
   - Organizations can use any combination of services
   - Easy to add new LLM providers or knowledge bases
   - Context strategies can be customized per use case

4. **Reusability**
   - Context service for any conversational AI
   - RAG service for any knowledge-enhanced application
   - LLM service as a general-purpose gateway

5. **Compliance**
   - Data residency per organization
   - Audit trails for all operations
   - GDPR-compliant data handling