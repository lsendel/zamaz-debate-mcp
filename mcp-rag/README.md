# MCP RAG Service

The Retrieval Augmented Generation (RAG) service provides document storage, embedding generation, and semantic search capabilities for the MCP system.

## Features

- **Document Management**: Store and manage documents with multi-tenant support
- **Multiple Format Support**: PDF, Word (DOC/DOCX), TXT, Markdown
- **Automatic Chunking**: Intelligent document chunking with overlap
- **Embedding Generation**: OpenAI embeddings (with mock fallback)
- **Vector Storage**: In-memory or Qdrant vector database
- **Semantic Search**: Find relevant documents based on meaning
- **MCP Integration**: Full MCP protocol support

## Architecture

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────┐
│   REST API      │────▶│  Document    │────▶│  Chunking   │
│   Controller    │     │  Service     │     │  Service    │
└─────────────────┘     └──────────────┘     └─────────────┘
                               │                     │
                               ▼                     ▼
                        ┌──────────────┐     ┌─────────────┐
                        │  Embedding   │     │   Vector    │
                        │  Service     │     │   Store     │
                        └──────────────┘     └─────────────┘
```

## API Endpoints

### REST API

- `POST /api/documents` - Store a document
- `POST /api/documents/upload` - Upload a document file
- `GET /api/documents/{organizationId}/{documentId}` - Get document
- `GET /api/documents/{organizationId}` - List documents
- `POST /api/documents/search` - Search documents
- `DELETE /api/documents/{organizationId}/{documentId}` - Delete document

### MCP Tools

- `store_document` - Store a document for retrieval
- `search_documents` - Search for relevant documents
- `delete_document` - Delete a stored document
- `generate_rag_context` - Generate context from documents
- `list_documents` - List stored documents

## Configuration

```yaml
# Vector Storage
mcp.vector.type: in-memory  # or 'qdrant'
mcp.vector.dimension: 1536

# OpenAI (for embeddings)
openai.api.key: ${OPENAI_API_KEY}
openai.embedding.model: text-embedding-ada-002

# Chunking
rag.chunking.default-size: 1000
rag.chunking.overlap: 200
rag.chunking.max-size: 2000
```

## Document Processing Flow

1. **Upload**: Document uploaded via API
2. **Parse**: Extract text from various formats
3. **Chunk**: Split into overlapping chunks
4. **Embed**: Generate embeddings for each chunk
5. **Store**: Save to vector database
6. **Index**: Make searchable

## Usage Examples

### Store a Document
```bash
curl -X POST http://localhost:5004/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "org-123",
    "documentId": "doc-001",
    "title": "Product Manual",
    "content": "This is the product manual content..."
  }'
```

### Search Documents
```bash
curl -X POST http://localhost:5004/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "org-123",
    "query": "How to install the product?",
    "limit": 5
  }'
```

### MCP Tool Call
```bash
curl -X POST http://localhost:5004/mcp/call-tool \
  -H "Content-Type: application/json" \
  -d '{
    "name": "generate_rag_context",
    "arguments": {
      "organizationId": "org-123",
      "query": "What are the key features?",
      "maxTokens": 2000
    }
  }'
```

## Development

### Running Locally
```bash
mvn spring-boot:run
```

### Running Tests
```bash
mvn test
```

### Building
```bash
mvn clean package
```

## Environment Variables

- `OPENAI_API_KEY`: OpenAI API key for embeddings
- `QDRANT_HOST`: Qdrant host (default: localhost)
- `QDRANT_PORT`: Qdrant port (default: 6334)

## Dependencies

- Spring Boot 3.2
- PostgreSQL (metadata storage)
- Redis (caching)
- Qdrant (vector storage, optional)
- OpenAI API (embeddings)
- Apache PDFBox (PDF parsing)
- Apache POI (Word documents)