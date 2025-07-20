# Hexagonal Architecture Ports Summary

## Inbound Ports (Use Case Interfaces)

Located in `application/port/in/`:

1. **CreateDocumentUseCase**
   - Creates new documents in the system
   - Input: CreateDocumentCommand (fileName, content, contentType, organizationId, source)
   - Output: DocumentId

2. **ProcessDocumentUseCase**
   - Processes documents by chunking and generating embeddings
   - Input: ProcessDocumentCommand (documentId, chunkingConfig, embeddingConfig)
   - Output: void (async processing)

3. **SearchDocumentsUseCase**
   - Searches documents using vector similarity
   - Input: SearchQuery (queryText, organizationId, topK, minScore, tags, includeContent)
   - Output: List<SearchResult>

4. **GetDocumentUseCase**
   - Retrieves document details
   - Input: GetDocumentQuery (documentId, organizationId, includeChunks, includeProcessingHistory)
   - Output: DocumentDto

## Outbound Ports (Infrastructure Interfaces)

Located in `application/port/out/`:

1. **DocumentRepository**
   - Persistence for Document aggregate
   - Methods: save, findById, findByOrganization, delete, etc.

2. **EmbeddingService**
   - External service for generating embeddings
   - Methods: generateEmbedding, generateEmbeddings (sync and async)
   - Supports multiple models

3. **VectorStore**
   - Storage and search for embedding vectors
   - Methods: storeEmbedding, search, deleteByDocument
   - Supports similarity search with filters

4. **EventPublisher**
   - Publishing domain events to external systems
   - Methods: publish, publishAll, publishAsync

5. **DocumentParser**
   - Parsing various document formats (PDF, DOCX, TXT)
   - Methods: parse, isSupported, getSupportedTypes
   - Returns structured text and metadata

## Key Design Decisions

1. **Command/Query Separation**: Each use case has its own command/query object with validation
2. **DTOs for Responses**: Use cases return DTOs, not domain objects
3. **Async Support**: Several ports support async operations for performance
4. **Defensive Copying**: All collections in records are defensively copied
5. **Rich Return Types**: Operations return detailed results with metadata
6. **Error Handling**: Parse results and other operations include error information

## Next Steps

- Implement application services that use these ports
- Create adapters that implement the outbound ports
- Wire everything together with dependency injection