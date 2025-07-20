# MCP RAG Java Service - CLAUDE.md

This file provides guidance to Claude Code when working with the mcp-rag service.

## Service Overview

The `mcp-rag` service is a Spring Boot-based Java implementation for Retrieval Augmented Generation functionality. Currently undergoing refactoring to implement hexagonal architecture (Ports and Adapters pattern) for better separation of concerns and testability.

## Purpose

- **RAG Implementation**: Future Java-based RAG service
- **Spring Boot Foundation**: Modern Java microservice architecture
- **Alternative Implementation**: Java option alongside Python RAG service
- **Enterprise Integration**: Java ecosystem compatibility

## Technology Stack

- **Language**: Java 8 (should be upgraded to Java 11+)
- **Framework**: Spring Boot 2.5.4
- **Architecture**: Hexagonal Architecture (Ports and Adapters)
- **Build Tool**: Maven
- **Vector Database**: Qdrant (planned)
- **Embedding Service**: OpenAI API (planned)

## Current Implementation

### Hexagonal Architecture Implementation

The service is being refactored to follow hexagonal architecture principles:

#### Domain Layer (Core)
- **Location**: `domain/model/`, `domain/service/`, `domain/event/`, `domain/exception/`
- **Rich Domain Models**: Document, Embedding, SearchQuery, SearchResult
- **Value Objects**: DocumentId, ChunkId, EmbeddingVector, etc.
- **Domain Services**: ChunkingStrategy
- **Domain Events**: DocumentCreatedEvent, DocumentProcessedEvent, etc.
- **No Framework Dependencies**: Pure Java/business logic

#### Application Layer
- **Location**: `application/port/in/`, `application/port/out/`, `application/service/`
- **Inbound Ports**: Use case interfaces (CreateDocumentUseCase, SearchDocumentsUseCase, etc.)
- **Outbound Ports**: Infrastructure interfaces (DocumentRepository, VectorStore, etc.)
- **Application Services**: Orchestrate use cases, handle transactions

#### Infrastructure Layer (Adapters)
- **Location**: `infrastructure/adapter/in/`, `infrastructure/adapter/out/`
- **Inbound Adapters**: REST controllers, message listeners
- **Outbound Adapters**: Database repositories, external service clients

### Main Application
```java
@SpringBootApplication
public class McpRagJApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpRagJApplication.class, args);
    }
}
```

## Planned Architecture

### Components to Implement

1. **Document Processing**
   - PDF, DOCX, TXT parsers
   - Chunking strategies
   - Metadata extraction

2. **Embedding Generation**
   - OpenAI integration
   - Local embedding models
   - Batch processing

3. **Vector Storage**
   - Qdrant client
   - Pinecone integration
   - Search functionality

4. **RAG Pipeline**
   - Document ingestion
   - Query processing
   - Context augmentation

## Development Guidelines

### Running the Service
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Package
mvn package
java -jar target/mcp-rag-0.0.1-SNAPSHOT.jar
```

### Project Structure (Planned)
```
src/main/java/com/mcp/ragj/
├── config/
│   ├── VectorDbConfig.java
│   └── EmbeddingConfig.java
├── controller/
│   └── RagController.java
├── service/
│   ├── DocumentService.java
│   ├── EmbeddingService.java
│   └── SearchService.java
├── model/
│   ├── Document.java
│   ├── Chunk.java
│   └── SearchResult.java
├── repository/
│   └── VectorRepository.java
└── McpRagJApplication.java
```

## Configuration

### Current Dependencies
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Future Dependencies
- Spring Boot Web Starter
- Apache PDFBox (PDF processing)
- Apache POI (DOCX processing)
- Qdrant Java Client
- OpenAI Java SDK
- Spring Data JPA
- PostgreSQL Driver

## Implementation Roadmap

### Phase 1: Basic Setup
- [ ] Add Spring Web dependency
- [ ] Create REST controllers
- [ ] Define data models
- [ ] Setup application properties

### Phase 2: Document Processing
- [ ] Implement file upload endpoint
- [ ] Add document parsers
- [ ] Create chunking logic
- [ ] Store documents in database

### Phase 3: Embedding Integration
- [ ] OpenAI embedding client
- [ ] Embedding service layer
- [ ] Batch processing support
- [ ] Error handling

### Phase 4: Vector Database
- [ ] Qdrant configuration
- [ ] Vector storage operations
- [ ] Search implementation
- [ ] Metadata filtering

### Phase 5: RAG Pipeline
- [ ] Query processing
- [ ] Context augmentation
- [ ] Result ranking
- [ ] API integration

## Planned API Endpoints

```java
@RestController
@RequestMapping("/api/rag")
public class RagController {
    
    @PostMapping("/knowledge-base")
    public KnowledgeBase createKnowledgeBase(@RequestBody CreateKBRequest request);
    
    @PostMapping("/ingest")
    public IngestResponse ingestDocument(@RequestParam("file") MultipartFile file);
    
    @PostMapping("/search")
    public List<SearchResult> search(@RequestBody SearchRequest request);
    
    @PostMapping("/augment")
    public AugmentedContext augmentContext(@RequestBody AugmentRequest request);
}
```

## Integration Plans

### With Other Services
- REST API communication
- Shared data models
- Common authentication
- Event-driven updates

### With MCP Protocol
- Future Java MCP SDK integration
- Tool implementations
- Resource exposure
- Protocol compliance

## Development Best Practices

1. **Use Spring conventions** for consistency
2. **Implement service interfaces** for flexibility
3. **Add comprehensive logging** with SLF4J
4. **Write integration tests** with @SpringBootTest
5. **Use @Value for configuration**
6. **Implement proper exception handling**
7. **Add OpenAPI documentation**

## Configuration Properties

### application.yml (Planned)
```yaml
spring:
  application:
    name: mcp-rag
  servlet:
    multipart:
      max-file-size: 50MB

rag:
  embedding:
    model: text-embedding-ada-002
    batch-size: 100
  vector-db:
    type: qdrant
    host: localhost
    port: 6333
  chunking:
    strategy: sliding-window
    size: 512
    overlap: 128
```

## Known Limitations

1. **Not Implemented**: Only Spring Boot shell exists
2. **No Functionality**: No RAG features yet
3. **Java 8**: Should upgrade to Java 11+
4. **No Tests**: Test infrastructure needed
5. **No Documentation**: API docs required

## Future Enhancements

- Async processing with CompletableFuture
- Reactive programming with Spring WebFlux
- Kubernetes deployment configs
- Monitoring with Micrometer
- Caching with Spring Cache
- Message queue integration
- Multi-language support
- Custom embedding models

## Hexagonal Architecture Learnings

### Key Benefits Observed
1. **Clear Boundaries**: Domain logic is completely isolated from infrastructure concerns
2. **Testability**: Domain and application layers can be tested without any framework dependencies
3. **Flexibility**: Easy to swap implementations (e.g., different vector stores, embedding services)
4. **Maintainability**: Changes in external systems don't affect core business logic

### Implementation Patterns
1. **Rich Domain Models**: Entities contain business logic, not just data
2. **Value Objects**: Immutable objects for IDs, configurations, results
3. **Domain Events**: Capture important business occurrences
4. **Command/Query Objects**: Validate input at the boundary
5. **DTOs for Output**: Keep domain objects internal, expose DTOs

### Challenges and Solutions
1. **Legacy Code**: Existing code mixed concerns - solution: gradual refactoring
2. **Event Structure**: Domain events had package dependencies - solution: fix imports
3. **Validation**: Where to validate? - solution: value objects validate themselves
4. **Async Operations**: How to handle? - solution: CompletableFuture in ports

### Best Practices Applied
1. **No Null Values**: Use Optional or provide sensible defaults
2. **Immutability**: Value objects and DTOs are immutable
3. **Defensive Copying**: Collections in constructors are copied
4. **Factory Methods**: Static factory methods for object creation
5. **Builder Pattern**: For complex objects like DocumentMetadata

## Getting Started

To continue the hexagonal architecture implementation:
1. Complete Phase 3: Implement application services
2. Complete Phase 4: Create infrastructure adapters
3. Complete Phase 5: Remove legacy code
4. Complete Phase 6: Add comprehensive tests
5. Complete Phase 7: Add architectural fitness functions

For new features:
1. Start with the domain model
2. Define use case interfaces (inbound ports)
3. Define infrastructure interfaces (outbound ports)
4. Implement application services
5. Create adapters last