# MCP RAG Service - Claude Development Guide

## Quick Reference
- **Port**: 5004
- **Database**: Qdrant (vector DB) + PostgreSQL
- **Primary Purpose**: Retrieval-Augmented Generation for debate evidence
- **Dependencies**: Qdrant, PostgreSQL, Context Service

## Key Files to Check
```
mcp-rag/
├── src/
│   ├── mcp_server.py                    # MCP interface - START HERE
│   ├── models.py                        # RAG data models
│   ├── clients/
│   │   └── context_client.py            # Integration with Context
│   └── managers/
│       ├── rag_manager.py               # Core RAG logic
│       ├── embedding_manager.py         # Vector embeddings
│       └── document_processor.py        # Document parsing
```

## Current Implementation Status
✅ **Implemented**:
- Document upload and parsing
- Vector embedding generation
- Semantic search
- Context integration
- Multiple file format support
- Chunk management
- Metadata handling

❌ **Not Implemented**:
- Document versioning
- Citation tracking
- Source verification
- Incremental indexing
- Document permissions
- Multi-language support

## Document Processing Pipeline

```mermaid
graph LR
    A[Upload Document] --> B[Parse Content]
    B --> C[Split into Chunks]
    C --> D[Generate Embeddings]
    D --> E[Store in Qdrant]
    E --> F[Update Metadata]
    F --> G[Ready for Search]
```

## Common Development Tasks

### 1. Uploading Documents
```python
# Document upload flow:
{
    "name": "upload_document",
    "arguments": {
        "filename": "ai_ethics_paper.pdf",
        "content": "base64_encoded_content",
        "metadata": {
            "source": "Stanford AI Lab",
            "year": 2024,
            "category": "ethics",
            "debate_id": "debate-123"
        },
        "collection_name": "debate_evidence"
    }
}
```

### 2. Searching Documents
```python
# Semantic search:
{
    "name": "search",
    "arguments": {
        "query": "What are the risks of AGI?",
        "collection_name": "debate_evidence",
        "limit": 5,
        "metadata_filter": {
            "category": "ethics",
            "year": {"$gte": 2023}
        }
    }
}
```

### 3. Augmenting Context
```python
# RAG for debate context:
{
    "name": "augment_context",
    "arguments": {
        "context_id": "ctx-123",
        "query": "Find evidence about AI regulation",
        "max_chunks": 3,
        "include_sources": true
    }
}
```

## Embedding Management

### Embedding Models
```python
# Currently using OpenAI embeddings:
EMBEDDING_MODEL = "text-embedding-ada-002"
EMBEDDING_DIMENSION = 1536

# Future: Support multiple models
models = {
    "openai": "text-embedding-ada-002",
    "cohere": "embed-english-v3.0",
    "sentence-transformers": "all-MiniLM-L6-v2"
}
```

### Embedding Generation
```python
async def generate_embeddings(texts: List[str]):
    # Batch processing for efficiency
    embeddings = await openai.embeddings.create(
        model=EMBEDDING_MODEL,
        input=texts
    )
    return [e.embedding for e in embeddings.data]
```

## Document Processing

### Supported Formats
```python
SUPPORTED_FORMATS = {
    ".pdf": process_pdf,
    ".txt": process_text,
    ".md": process_markdown,
    ".docx": process_docx,
    ".html": process_html,
    ".json": process_json
}
```

### Chunking Strategies
```python
# Current: Fixed-size chunks with overlap
CHUNK_SIZE = 500  # tokens
CHUNK_OVERLAP = 50  # tokens

# Better strategies to implement:
1. Semantic chunking (by paragraphs/sections)
2. Sliding window with sentence boundaries
3. Hierarchical chunking (doc -> section -> paragraph)
```

## Vector Database (Qdrant)

### Collection Structure
```python
# Collection schema:
{
    "vectors": {
        "size": 1536,
        "distance": "Cosine"
    },
    "payload_schema": {
        "document_id": "string",
        "chunk_id": "string",
        "content": "text",
        "metadata": "json",
        "created_at": "datetime"
    }
}
```

### Search Operations
```python
# Semantic search with filters:
results = await qdrant_client.search(
    collection_name="debate_evidence",
    query_vector=query_embedding,
    query_filter=Filter(
        must=[
            FieldCondition(
                key="metadata.category",
                match=MatchValue(value="ethics")
            )
        ]
    ),
    limit=5,
    with_payload=True
)
```

## Integration with Debate System

### Evidence Injection
```python
# During debate turn generation:
1. Extract key topics from conversation
2. Search for relevant evidence
3. Inject top results into context
4. Generate response with citations

# Example flow:
evidence = await rag_service.search({
    "query": current_argument,
    "metadata_filter": {
        "debate_id": debate.id
    }
})

enhanced_prompt = f"""
Consider this evidence:
{format_evidence(evidence)}

Now respond to: {opponent_argument}
"""
```

### Citation Tracking
```python
# Track sources in responses:
{
    "response": "According to research...",
    "citations": [
        {
            "document_id": "doc-123",
            "chunk_id": "chunk-456",
            "source": "AI Safety Paper 2024",
            "page": 15
        }
    ]
}
```

## Performance Optimization

### Current Optimizations
1. **Batch embeddings**: Process multiple chunks together
2. **Async operations**: Non-blocking document processing
3. **Caching**: Cache frequently accessed embeddings

### Optimization Opportunities
```python
# Incremental indexing:
async def update_document(doc_id: str, changes: List[Change]):
    # Only reindex changed chunks
    # Maintain document version history

# Hybrid search:
async def hybrid_search(query: str):
    # Combine vector search with keyword search
    semantic_results = await vector_search(query)
    keyword_results = await full_text_search(query)
    return merge_results(semantic_results, keyword_results)
```

## Testing RAG Quality

### Relevance Testing
```python
# Test search quality:
test_queries = [
    ("What is AGI?", ["artificial general intelligence", "human-level"]),
    ("AI risks", ["existential risk", "alignment problem"]),
]

for query, expected_terms in test_queries:
    results = await rag_service.search(query)
    assert any(term in result.content for term in expected_terms)
```

### Performance Testing
```python
# Measure search latency:
import time

start = time.time()
results = await rag_service.search("test query")
latency = time.time() - start
assert latency < 0.5  # 500ms target
```

## Common Issues & Solutions

### Issue: "Poor search results"
```python
# Check: Embedding quality
# Check: Chunk size (too large/small)
# Solution: Tune chunking parameters
# Solution: Add metadata filtering
```

### Issue: "Slow document processing"
```python
# Large PDFs can be slow
# Check: File size
# Solution: Process asynchronously
# Solution: Implement progress tracking
```

### Issue: "Out of memory"
```python
# Large documents consume memory
# Check: Document size
# Solution: Stream processing
# Solution: Increase container memory
```

## Security & Privacy

### Document Access Control
```python
# TODO: Implement permissions
{
    "document_permissions": {
        "owner": "user-123",
        "organization_id": "org-456",
        "access_level": "organization",  # public, organization, private
        "allowed_users": ["user-789"]
    }
}
```

### Data Sanitization
```python
# Remove sensitive data:
def sanitize_document(content: str) -> str:
    # Remove emails
    content = re.sub(r'\S+@\S+', '[EMAIL]', content)
    # Remove phone numbers
    content = re.sub(r'\b\d{3}[-.]?\d{3}[-.]?\d{4}\b', '[PHONE]', content)
    return content
```

## Environment Variables
```bash
# Vector Database
QDRANT_HOST=qdrant
QDRANT_PORT=6333
QDRANT_API_KEY=optional_api_key

# Embeddings
OPENAI_API_KEY=sk-...
EMBEDDING_MODEL=text-embedding-ada-002

# Context Service
CONTEXT_SERVICE_URL=http://mcp-context:5001

# Service Config
MCP_PORT=5004
LOG_LEVEL=INFO
MAX_CHUNK_SIZE=500
CHUNK_OVERLAP=50
```

## Quick Commands
```bash
# Health check
curl http://localhost:5004/health

# List collections
curl http://localhost:5004/resources/rag://collections

# Search documents
curl -X POST http://localhost:5004/tools/search \
  -d '{"query": "AI ethics", "limit": 5}'
```

## Advanced Features (Future)

### Document Graph
```python
# Link related documents:
{
    "document_relations": {
        "cites": ["doc-123", "doc-456"],
        "cited_by": ["doc-789"],
        "related": ["doc-abc", "doc-def"]
    }
}
```

### Fact Verification
```python
# Verify claims against sources:
async def verify_claim(claim: str, sources: List[str]):
    evidence_for = await search_supporting(claim)
    evidence_against = await search_contradicting(claim)
    return {
        "claim": claim,
        "support_score": calculate_support(evidence_for),
        "contradiction_score": calculate_contradiction(evidence_against),
        "confidence": calculate_confidence()
    }
```

## Next Development Priorities
1. Implement document versioning
2. Add citation tracking in responses
3. Create fact verification system
4. Implement document permissions
5. Add incremental indexing
6. Create document graph relationships
7. Implement multi-language support
8. Add source credibility scoring