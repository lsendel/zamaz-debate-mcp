# MCP-RAG Service Documentation

The MCP-RAG service provides Retrieval Augmented Generation capabilities for the Zamaz Debate MCP system, enhancing LLM responses with relevant information from knowledge bases.

## Overview

The MCP-RAG service allows organizations to create knowledge bases, ingest documents, and use the stored information to augment LLM responses. It uses vector embeddings to perform semantic search and retrieval of relevant information.

## Features

- **Knowledge Base Management**: Create and manage multiple knowledge bases
- **Document Ingestion**: Process and index various document formats
- **Vector Search**: Semantic search using embeddings
- **Context Augmentation**: Enhance LLM prompts with retrieved information
- **Multi-tenant Isolation**: Complete isolation between organizations
- **Source Attribution**: Track and attribute information sources

## Architecture

The RAG service consists of these main components:

- **Knowledge Base Manager**: Handles knowledge base CRUD operations
- **Document Processor**: Processes and chunks documents
- **Embedding Service**: Generates vector embeddings for text
- **Vector Store**: Stores and searches vector embeddings (Qdrant)
- **Retrieval Engine**: Retrieves relevant information based on queries
- **Augmentation Service**: Enhances prompts with retrieved information

## API Endpoints

### Knowledge Bases

- `POST /api/v1/knowledge-bases`: Create a knowledge base
- `GET /api/v1/knowledge-bases`: List knowledge bases
- `GET /api/v1/knowledge-bases/{id}`: Get knowledge base details
- `PUT /api/v1/knowledge-bases/{id}`: Update knowledge base
- `DELETE /api/v1/knowledge-bases/{id}`: Delete knowledge base

### Documents

- `POST /api/v1/knowledge-bases/{id}/documents`: Add documents
- `GET /api/v1/knowledge-bases/{id}/documents`: List documents
- `GET /api/v1/knowledge-bases/{id}/documents/{docId}`: Get document details
- `DELETE /api/v1/knowledge-bases/{id}/documents/{docId}`: Delete document

### Search and Retrieval

- `POST /api/v1/knowledge-bases/{id}/search`: Search knowledge base
- `POST /api/v1/augment`: Augment context with retrieved information

### MCP Tools

The service exposes the following MCP tools:

- `create_knowledge_base`: Create a new knowledge base
- `ingest_documents`: Add documents to a knowledge base
- `search`: Perform semantic search
- `augment_context`: Enhance context with retrieved information

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | postgres |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | PostgreSQL database name | rag_db |
| `DB_USER` | PostgreSQL username | postgres |
| `DB_PASSWORD` | PostgreSQL password | postgres |
| `QDRANT_URL` | Qdrant vector database URL | http://qdrant:6333 |
| `REDIS_HOST` | Redis host | redis |
| `REDIS_PORT` | Redis port | 6379 |
| `EMBEDDING_MODEL` | Default embedding model | text-embedding-3-large |
| `LOG_LEVEL` | Logging level | INFO |

### RAG Configuration

RAG-specific settings can be configured in `config/rag.yml`:

```yaml
rag:
  embedding:
    default_model: text-embedding-3-large
    dimension: 1536
    batch_size: 100
  chunking:
    default_chunk_size: 1000
    default_chunk_overlap: 200
  retrieval:
    default_top_k: 5
    default_similarity_threshold: 0.7
  augmentation:
    max_tokens_per_source: 1000
    include_metadata: true
    include_sources: true
```

## Usage Examples

### Create a Knowledge Base

```bash
curl -X POST http://localhost:5004/api/v1/knowledge-bases \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "name": "Climate Research",
    "description": "Scientific papers on climate change",
    "embeddingModel": "text-embedding-3-large"
  }'
```

### Add Documents

```bash
curl -X POST http://localhost:5004/api/v1/knowledge-bases/kb-123/documents \
  -H "Content-Type: multipart/form-data" \
  -H "X-Organization-ID: org-123" \
  -F "files=@paper1.pdf" \
  -F "files=@paper2.pdf" \
  -F "metadata={\"source\":\"IPCC Report 2023\"}"
```

### Search Knowledge Base

```bash
curl -X POST http://localhost:5004/api/v1/knowledge-bases/kb-123/search \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "query": "impact of rising sea levels on coastal cities",
    "topK": 3,
    "similarityThreshold": 0.75
  }'
```

### Augment Context

```bash
curl -X POST http://localhost:5004/api/v1/augment \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "knowledgeBaseId": "kb-123",
    "query": "What are the economic impacts of climate change?",
    "topK": 5,
    "maxTokens": 2000,
    "includeSources": true
  }'
```

## Document Processing

### Supported Document Formats

- PDF (`.pdf`)
- Word (`.docx`, `.doc`)
- Text (`.txt`)
- Markdown (`.md`)
- HTML (`.html`, `.htm`)
- CSV (`.csv`)
- JSON (`.json`)

### Document Processing Pipeline

1. **Document Upload**: Files are uploaded to the service
2. **Text Extraction**: Text is extracted from documents
3. **Chunking**: Documents are split into manageable chunks
4. **Embedding Generation**: Vector embeddings are created for each chunk
5. **Indexing**: Embeddings are stored in the vector database
6. **Metadata Storage**: Document metadata is stored in PostgreSQL

## Vector Search

The RAG service uses Qdrant for vector search with these features:

- **Approximate Nearest Neighbor (ANN) Search**: Fast similarity search
- **Filtering**: Filter results based on metadata
- **Hybrid Search**: Combine vector and keyword search
- **Pagination**: Paginate through search results

## Augmentation Strategies

The service supports multiple augmentation strategies:

1. **Basic Retrieval**: Simple retrieval of relevant chunks
2. **Reranking**: Rerank retrieved chunks for better relevance
3. **Query Expansion**: Expand the query for better retrieval
4. **Hybrid Search**: Combine vector and keyword search
5. **Multi-query Retrieval**: Generate multiple queries for better coverage

## Multi-tenant Isolation

The RAG service implements strict multi-tenant isolation:

- Each knowledge base belongs to a specific organization
- Vector collections are prefixed with organization ID
- Database queries filter by organization ID
- API endpoints validate organization access

## Monitoring and Metrics

The service exposes the following metrics:

- Knowledge base count per organization
- Document count per knowledge base
- Embedding generation time
- Search latency
- Cache hit/miss ratio

Access metrics at: `http://localhost:5004/actuator/metrics`

## Troubleshooting

### Common Issues

1. **Document Processing Errors**
   - Check file format compatibility
   - Verify file is not corrupted
   - Check file size limits

2. **Search Performance Issues**
   - Monitor Qdrant performance
   - Check embedding generation time
   - Optimize chunk size and overlap

3. **Storage Issues**
   - Monitor disk usage for document storage
   - Check vector database size
   - Implement document retention policies

### Logs

Service logs can be accessed via:

```bash
docker-compose logs mcp-rag
```

## Development

### Building the Service

```bash
cd mcp-rag
mvn clean install
```

### Running Tests

```bash
cd mcp-rag
mvn test
```

### Local Development

```bash
cd mcp-rag
mvn spring-boot:run
```

## Advanced Features

### Custom Embedding Models

The service supports custom embedding models:

```bash
curl -X POST http://localhost:5004/api/v1/knowledge-bases \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "name": "Custom Embeddings KB",
    "description": "Using custom embedding model",
    "embeddingModel": "custom-model",
    "embeddingConfig": {
      "provider": "openai",
      "modelName": "text-embedding-3-large",
      "dimension": 1536
    }
  }'
```

### Document Preprocessing

Configure custom document preprocessing:

```bash
curl -X POST http://localhost:5004/api/v1/knowledge-bases/kb-123/documents \
  -H "Content-Type: multipart/form-data" \
  -H "X-Organization-ID: org-123" \
  -F "files=@document.pdf" \
  -F "processingConfig={\"chunkSize\":500,\"chunkOverlap\":50,\"skipSections\":[\"references\",\"appendix\"]}"
```

### Scheduled Reindexing

Set up scheduled reindexing for knowledge bases:

```bash
curl -X PUT http://localhost:5004/api/v1/knowledge-bases/kb-123 \
  -H "Content-Type: application/json" \
  -H "X-Organization-ID: org-123" \
  -d '{
    "name": "Updated KB",
    "reindexSchedule": "0 0 * * 0"  // Weekly reindexing
  }'
```
