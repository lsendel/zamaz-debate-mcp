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
```java
public class Organization {
    private String id;
    private String name;
    private String apiKey;  // For authentication
    private Map<String, Object> settings;
    private LocalDateTime createdAt;
}
    
public class ContextNamespace {
    private String id;
    private String orgId;
    private String name;  // e.g., "debates", "support", "research"
    private String description;
    private AccessPolicy accessPolicy;
}
    
public class Context {
    private String id;
    private String namespaceId;
    private String orgId;
    private String name;
    private List<Message> messages;
    private Map<String, Object> metadata;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> sharedWith;  // Other org IDs
}
    
public class ContextShare {
    private String id;
    private String contextId;
    private String sourceOrgId;
    private String targetOrgId;
    private List<String> permissions;  // ["read", "append", "fork"]
    private LocalDateTime expiresAt;
}
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
```java
public class KnowledgeBase {
    private String id;
    private String orgId;
    private String name;
    private String description;
    private IndexConfig indexConfig;
}
    
public class Document {
    private String id;
    private String kbId;
    private String sourceUrl;
    private String content;
    private Map<String, Object> metadata;
    private List<Float> embeddings;
    private LocalDateTime createdAt;
}
    
public class SearchResult {
    private String documentId;
    private String content;
    private float score;
    private Map<String, Object> metadata;
}
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

```java
// 1. mcp-debate creates debate context
Context debateContext = contextClient.createContext(CreateContextRequest.builder()
    .orgId("org-123")
    .namespace("debates")
    .name("AI Ethics Debate")
    .build())
    .block();

// 2. When preparing participant turn, query RAG
List<SearchResult> ragResults = ragClient.search(SearchRequest.builder()
    .kbId("ethics-kb")
    .query(lastMessage.getContent())
    .maxResults(3)
    .build())
    .collectList()
    .block();

// 3. Augment context with RAG results
Context augmentedContext = contextClient.appendToContext(
    debateContext.getId(),
    AppendMessagesRequest.builder()
        .messages(List.of(
            Message.builder()
                .role("system")
                .content("Relevant information:\n" + formatRagResults(ragResults))
                .build()
        ))
        .build())
    .block();

// 4. Get optimized context window
ContextWindow contextWindow = contextClient.getContextWindow(
    GetContextWindowRequest.builder()
        .contextId(debateContext.getId())
        .maxTokens(8000)
        .strategy("sliding_window_with_summary")
        .build())
    .block();

// 5. Call LLM with augmented context
CompletionResponse response = llmClient.complete(
    CompletionRequest.builder()
        .provider("claude")
        .model("claude-3-opus")
        .messages(contextWindow.getMessages())
        .build())
    .block();

// 6. Store response back in context
contextClient.appendToContext(
    debateContext.getId(),
    AppendMessagesRequest.builder()
        .messages(List.of(
            Message.builder()
                .role("assistant")
                .content(response.getContent())
                .metadata(Map.of("sources", ragResults))
                .build()
        ))
        .build())
    .block();
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