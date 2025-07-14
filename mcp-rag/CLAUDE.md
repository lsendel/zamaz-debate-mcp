# MCP RAG Service - CLAUDE.md

This file provides guidance to Claude Code when working with the mcp-rag service.

## Service Overview

The `mcp-rag` service provides Retrieval Augmented Generation capabilities for the zamaz-debate-mcp platform. It manages document ingestion, embedding generation, vector storage, and intelligent retrieval to enhance debate contexts with relevant knowledge.

## Purpose

- **Knowledge Management**: Create and manage document knowledge bases
- **Document Processing**: Ingest and process various document formats
- **Embedding Generation**: Convert text to vector embeddings
- **Semantic Search**: Find relevant information using vector similarity
- **Context Augmentation**: Enhance debate contexts with retrieved knowledge

## Technology Stack

- **Language**: Python 3.11+
- **Framework**: MCP SDK with async support
- **Vector Database**: Qdrant (primary), Pinecone (alternative)
- **Embeddings**: OpenAI, Sentence Transformers, E5 models
- **Document Processing**: PyPDF2, python-docx, BeautifulSoup4
- **ML Libraries**: transformers, sentence-transformers, torch

## Architecture Components

### 1. RAG Manager (`managers/rag_manager.py`)
Central orchestrator that:
- Manages knowledge bases and documents
- Coordinates embedding and retrieval
- Handles vector database operations
- Implements search and augmentation logic

### 2. Document Processor (`managers/document_processor.py`)
Handles document ingestion:
- **Supported Formats**: PDF, DOCX, TXT, Markdown, HTML, JSON, CSV
- **Chunking Strategies**:
  - Fixed size (default: 512 tokens)
  - Sentence-based
  - Paragraph-based
  - Sliding window with overlap
  - Semantic (experimental)
- **Text Extraction**: Format-specific parsers
- **Metadata Preservation**: Source, page numbers, sections

### 3. Embedding Manager (`managers/embedding_manager.py`)
Manages vector embeddings:
```python
# Supported models
EMBEDDING_MODELS = {
    "text-embedding-ada-002": 1536,     # OpenAI
    "text-embedding-3-small": 1536,     # OpenAI v3
    "text-embedding-3-large": 3072,     # OpenAI v3
    "all-MiniLM-L6-v2": 384,           # Sentence Transformers
    "all-mpnet-base-v2": 768,          # Sentence Transformers
    "e5-small": 384,                    # Microsoft E5
    "e5-base": 768,                     # Microsoft E5
    "e5-large": 1024                    # Microsoft E5
}
```

### 4. Vector Database Integration
**Qdrant** (Primary):
- Collection management
- Point insertion with payloads
- Similarity search with filters
- Batch operations
- Automatic index optimization

**Pinecone** (Alternative):
- Index management
- Upsert operations
- Query with metadata filtering
- Namespace support

## Data Models

### Knowledge Base
```python
class KnowledgeBase:
    id: str
    name: str
    organization_id: str
    embedding_model: str
    vector_dimensions: int
    chunking_strategy: ChunkingStrategy
    chunk_size: int
    chunk_overlap: int
    metadata: Dict[str, Any]
```

### Document
```python
class Document:
    id: str
    knowledge_base_id: str
    title: str
    source: str  # file path, URL, or "direct"
    content_type: str
    total_chunks: int
    metadata: Dict[str, Any]
    ingested_at: datetime
```

### Document Chunk
```python
class DocumentChunk:
    id: str
    document_id: str
    content: str
    chunk_index: int
    start_char: int
    end_char: int
    metadata: Dict[str, Any]
    embedding: Optional[List[float]]
```

## MCP Tools

### 1. create_knowledge_base
```json
{
  "name": "Technical Documentation",
  "organization_id": "org-123",
  "embedding_model": "text-embedding-ada-002",
  "chunking_strategy": "sliding_window",
  "chunk_size": 512,
  "chunk_overlap": 50
}
```

### 2. ingest_document
```json
{
  "knowledge_base_id": "kb-123",
  "source": "/path/to/document.pdf",
  "title": "AI Safety Guidelines",
  "metadata": {
    "category": "safety",
    "version": "2.0"
  }
}
```

### 3. search
```json
{
  "knowledge_base_id": "kb-123",
  "query": "What are the key safety considerations?",
  "limit": 5,
  "score_threshold": 0.7,
  "filters": {
    "category": "safety"
  }
}
```

### 4. augment_context
```json
{
  "knowledge_base_id": "kb-123",
  "context": [...messages...],
  "query": "Current debate topic",
  "max_results": 3,
  "insertion_strategy": "prepend"
}
```

## Document Processing Pipeline

### 1. Ingestion Flow
```python
1. Load document from source
2. Extract text based on format
3. Apply chunking strategy
4. Generate chunk metadata
5. Create embeddings (batch)
6. Store in vector database
7. Update document records
```

### 2. Chunking Strategies

**Fixed Size**: Simple token-based splits
```python
chunk_size=512, overlap=0
Use for: General documents
```

**Sliding Window**: Overlapping chunks
```python
chunk_size=512, overlap=128
Use for: Dense technical content
```

**Sentence-Based**: Natural sentence boundaries
```python
min_sentences=3, max_sentences=10
Use for: Narrative content
```

**Paragraph-Based**: Preserve paragraph structure
```python
min_words=100, max_words=500
Use for: Structured documents
```

**Semantic**: AI-powered logical sections
```python
Uses NLP to identify topic boundaries
Use for: Complex documents
```

## Search and Retrieval

### Vector Search Process
1. Generate query embedding
2. Search vector database
3. Apply metadata filters
4. Score and rank results
5. Optional reranking
6. Return top K results

### Search Parameters
- **score_threshold**: Minimum similarity (0.0-1.0)
- **limit**: Maximum results
- **filters**: Metadata constraints
- **rerank**: Use cross-encoder for better ranking

### Augmentation Strategies
- **prepend**: Add before context
- **append**: Add after context
- **interleave**: Mix with messages
- **summary**: Add as system message

## Configuration

### Environment Variables
```bash
# Service Configuration
MCP_HOST=0.0.0.0
MCP_PORT=5004

# Vector Database
QDRANT_HOST=localhost
QDRANT_PORT=6333
QDRANT_API_KEY=optional
PINECONE_API_KEY=optional
PINECONE_ENVIRONMENT=us-east-1

# Embeddings
OPENAI_API_KEY=sk-...
HF_TOKEN=hf_...  # For private models

# Performance
MAX_BATCH_SIZE=100
EMBEDDING_BATCH_SIZE=32
CHUNK_PROCESSING_WORKERS=4

# Storage
DOCUMENT_STORAGE_PATH=/data/documents
MAX_DOCUMENT_SIZE_MB=50
```

### Running the Service
```bash
# Development
python -m src.mcp_server

# Docker
docker build -t mcp-rag .
docker run -p 5004:5004 mcp-rag

# With Qdrant
docker-compose up qdrant mcp-rag
```

## Integration Patterns

### With Context Service
```python
# Get current context
context = await context_client.get_context(context_id)

# Augment with RAG
augmented = await rag_client.augment_context(
    knowledge_base_id=kb_id,
    context=context.messages,
    query=extract_topic(context),
    max_results=3
)
```

### With Debate Service
```python
# Search for relevant info during turn
results = await rag_client.search(
    knowledge_base_id=debate.knowledge_base_id,
    query=turn.content,
    limit=5
)

# Include in prompt
prompt = build_prompt_with_rag(context, results)
```

## Performance Optimization

### Embedding Generation
1. **Batch Processing**: Process multiple chunks together
2. **Caching**: Cache embeddings for identical text
3. **Model Selection**: Balance quality vs speed
4. **GPU Acceleration**: Use CUDA when available

### Vector Search
1. **Index Types**: HNSW for accuracy, IVF for speed
2. **Preprocessing**: Normalize vectors
3. **Filtering**: Apply metadata filters early
4. **Pagination**: Use offset/limit for large results

### Document Processing
1. **Async I/O**: Non-blocking file operations
2. **Streaming**: Process large files in chunks
3. **Worker Pool**: Parallel chunk processing
4. **Memory Management**: Clear buffers regularly

## Development Guidelines

### Adding New Document Formats
1. Create parser in `document_processor.py`
2. Add format detection logic
3. Implement text extraction
4. Handle format-specific metadata
5. Add tests with sample files

### Adding New Embedding Models
1. Add to `EMBEDDING_MODELS` dict
2. Implement generation logic
3. Handle API authentication
4. Add dimension validation
5. Update documentation

### Testing RAG Pipeline
```bash
# Unit tests
pytest tests/test_rag_manager.py

# Integration tests
pytest tests/integration/test_full_pipeline.py

# Performance tests
python tests/performance/test_embedding_speed.py
```

## Error Handling

### Common Issues
1. **Document Too Large**: Split or compress
2. **Unsupported Format**: Convert to supported
3. **Embedding API Error**: Retry with backoff
4. **Vector DB Full**: Increase storage
5. **Memory Overflow**: Reduce batch size

### Error Recovery
- Automatic retry for transient failures
- Checkpoint resume for large ingestions
- Graceful degradation without embeddings
- Fallback to keyword search

## Security Considerations

- Organization-scoped knowledge bases
- Input validation for all documents
- Sanitization of extracted text
- API key security for embeddings
- Access control for search operations
- No direct file system access

## Monitoring and Metrics

### Key Metrics
- Documents ingested per hour
- Average chunk size
- Embedding generation time
- Search latency (P50, P95, P99)
- Vector database size
- Cache hit rate

### Health Checks
- Vector database connectivity
- Embedding API availability
- Storage space remaining
- Memory usage
- Queue depths

## Known Limitations

1. **Document Size**: 50MB maximum
2. **Concurrent Ingestion**: Limited by workers
3. **Language Support**: English-optimized
4. **Real-time Updates**: No live document sync
5. **Version Control**: No document versioning

## Future Enhancements

- Multi-language support
- Real-time document sync
- Hybrid search (vector + keyword)
- Document versioning
- OCR for scanned documents
- Audio/video transcription
- Knowledge graph integration
- Federated search across organizations
- Custom embedding fine-tuning
- Incremental index updates