# MCP-RAG Hexagonal Architecture Refactoring Plan

## Overview

This plan details the systematic refactoring of mcp-rag service from its current mixed architecture to a pure hexagonal (ports and adapters) architecture.

## Current State Analysis

### Problems Identified
1. **Duplicate structures** - Legacy and hexagonal implementations coexist
2. **Mixed concerns** - Business logic scattered across layers
3. **Framework coupling** - Spring annotations in business logic
4. **Incomplete boundaries** - Ports defined but not consistently used
5. **Direct dependencies** - Services directly access repositories and external systems

## Target Architecture

```
mcp-rag/
├── domain/                      # Core business logic (no framework dependencies)
│   ├── model/                   # Entities, Aggregates, Value Objects
│   │   ├── document/            # Document aggregate
│   │   ├── embedding/           # Embedding value objects
│   │   └── query/               # Query value objects
│   ├── service/                 # Domain services
│   ├── event/                   # Domain events
│   └── port/                    # Port interfaces
│       ├── inbound/             # Use case interfaces
│       └── outbound/            # Repository & external service interfaces
├── application/                 # Use cases (orchestration)
│   ├── usecase/                 # Use case implementations
│   │   ├── document/            # Document-related use cases
│   │   ├── search/              # Search use cases
│   │   └── embedding/           # Embedding use cases
│   ├── service/                 # Application services
│   └── dto/                     # Application DTOs
├── adapter/                     # External interfaces
│   ├── inbound/                 # Driving adapters
│   │   ├── web/                 # REST controllers
│   │   ├── event/               # Event listeners
│   │   └── scheduler/           # Scheduled tasks
│   └── outbound/                # Driven adapters
│       ├── persistence/         # Database adapters
│       ├── vectorstore/         # Qdrant adapter
│       ├── embedding/           # Embedding service adapters
│       └── notification/        # Event publishing adapters
└── infrastructure/              # Framework configuration
    ├── config/                  # Spring configurations
    └── security/                # Security configurations
```

## Refactoring Phases

### Phase 1: Establish Domain Model (Week 1)

#### 1.1 Create Rich Domain Models
```java
// domain/model/document/Document.java
public class Document {
    private DocumentId id;
    private DocumentName name;
    private DocumentContent content;
    private DocumentMetadata metadata;
    private EmbeddingVector embedding;
    private DocumentStatus status;
    private ProcessingHistory history;
    
    // Business methods
    public void process() { /* domain logic */ }
    public void updateEmbedding(EmbeddingVector embedding) { /* domain logic */ }
    public boolean isReadyForSearch() { /* domain logic */ }
}

// domain/model/document/DocumentId.java
public record DocumentId(String value) {
    public DocumentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DocumentId cannot be empty");
        }
    }
    
    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID().toString());
    }
}
```

#### 1.2 Define Domain Services
```java
// domain/service/EmbeddingService.java
public interface EmbeddingService {
    EmbeddingVector generateEmbedding(DocumentContent content);
    List<EmbeddingVector> generateBatchEmbeddings(List<DocumentContent> contents);
}

// domain/service/DocumentProcessingService.java
public class DocumentProcessingService {
    public ProcessingResult processDocument(Document document, EmbeddingService embeddingService) {
        // Complex domain logic for document processing
        // Chunk splitting, embedding generation, validation
    }
}
```

#### 1.3 Define Domain Events
```java
// domain/event/DocumentProcessedEvent.java
public record DocumentProcessedEvent(
    DocumentId documentId,
    ProcessingResult result,
    Instant occurredAt
) implements DomainEvent {}
```

### Phase 2: Define Ports (Week 1)

#### 2.1 Inbound Ports (Use Cases)
```java
// domain/port/inbound/UploadDocumentUseCase.java
public interface UploadDocumentUseCase {
    DocumentId execute(UploadDocumentCommand command);
}

// domain/port/inbound/SearchDocumentsUseCase.java
public interface SearchDocumentsUseCase {
    SearchResult execute(SearchQuery query);
}
```

#### 2.2 Outbound Ports
```java
// domain/port/outbound/DocumentRepository.java
public interface DocumentRepository {
    Document save(Document document);
    Optional<Document> findById(DocumentId id);
    List<Document> findByStatus(DocumentStatus status);
}

// domain/port/outbound/VectorStore.java
public interface VectorStore {
    void store(DocumentId id, EmbeddingVector vector);
    List<SearchMatch> search(EmbeddingVector query, int limit);
    void delete(DocumentId id);
}

// domain/port/outbound/EventPublisher.java
public interface EventPublisher {
    void publish(DomainEvent event);
}
```

### Phase 3: Implement Application Layer (Week 2)

#### 3.1 Use Case Implementations
```java
// application/usecase/document/UploadDocumentUseCaseImpl.java
@UseCase
@RequiredArgsConstructor
public class UploadDocumentUseCaseImpl implements UploadDocumentUseCase {
    private final DocumentRepository documentRepository;
    private final DocumentProcessingService processingService;
    private final EmbeddingService embeddingService;
    private final EventPublisher eventPublisher;
    
    @Override
    public DocumentId execute(UploadDocumentCommand command) {
        // 1. Create document from command
        Document document = Document.create(
            command.fileName(),
            command.content(),
            command.metadata()
        );
        
        // 2. Process document (domain service)
        ProcessingResult result = processingService.processDocument(
            document, 
            embeddingService
        );
        
        // 3. Save document
        Document saved = documentRepository.save(document);
        
        // 4. Publish event
        eventPublisher.publish(new DocumentProcessedEvent(
            saved.getId(),
            result,
            Instant.now()
        ));
        
        return saved.getId();
    }
}
```

### Phase 4: Implement Adapters (Week 2-3)

#### 4.1 Inbound Adapters
```java
// adapter/inbound/web/DocumentController.java
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final SearchDocumentsUseCase searchDocumentsUseCase;
    
    @PostMapping
    public ResponseEntity<UploadResponse> upload(@RequestParam MultipartFile file) {
        // 1. Convert MultipartFile to domain command
        UploadDocumentCommand command = UploadDocumentCommand.from(
            file.getOriginalFilename(),
            file.getBytes(),
            extractMetadata(file)
        );
        
        // 2. Execute use case
        DocumentId documentId = uploadDocumentUseCase.execute(command);
        
        // 3. Convert to response DTO
        return ResponseEntity.ok(new UploadResponse(documentId.value()));
    }
}
```

#### 4.2 Outbound Adapters
```java
// adapter/outbound/persistence/JpaDocumentRepository.java
@Repository
@RequiredArgsConstructor
public class JpaDocumentRepository implements DocumentRepository {
    private final DocumentJpaRepository jpaRepository;
    private final DocumentMapper mapper;
    
    @Override
    public Document save(Document document) {
        DocumentEntity entity = mapper.toEntity(document);
        DocumentEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}

// adapter/outbound/vectorstore/QdrantVectorStore.java
@Component
@RequiredArgsConstructor
public class QdrantVectorStore implements VectorStore {
    private final QdrantClient qdrantClient;
    
    @Override
    public void store(DocumentId id, EmbeddingVector vector) {
        // Qdrant-specific implementation
    }
}
```

### Phase 5: Remove Legacy Code (Week 3)

1. **Delete duplicate packages**:
   - Remove `/controller` (use `/adapter/inbound/web`)
   - Remove `/entity` (use `/adapter/outbound/persistence/entity`)
   - Remove `/repository` (use domain ports)
   - Remove `/dto` (use adapter-specific DTOs)
   - Remove `/model` (use domain models)

2. **Refactor service layer**:
   - Move business logic to domain services or use cases
   - Convert remaining services to application services (orchestration only)
   - Remove `@Transactional` from domain/application layers

3. **Clean up utilities**:
   - Move domain utilities to domain layer
   - Move technical utilities to infrastructure

### Phase 6: Testing Strategy (Week 3-4)

#### 6.1 Domain Tests (No Spring)
```java
class DocumentTest {
    @Test
    void shouldProcessDocumentSuccessfully() {
        // Pure unit test, no framework
        Document document = Document.create("test.pdf", content, metadata);
        ProcessingResult result = document.process();
        
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSED);
        assertThat(result).isNotNull();
    }
}
```

#### 6.2 Use Case Tests (Mocked Ports)
```java
class UploadDocumentUseCaseTest {
    @Mock DocumentRepository repository;
    @Mock EmbeddingService embeddingService;
    
    @Test
    void shouldUploadDocument() {
        // Test use case with mocked dependencies
        UploadDocumentUseCase useCase = new UploadDocumentUseCaseImpl(
            repository, processingService, embeddingService, eventPublisher
        );
        
        DocumentId id = useCase.execute(command);
        
        verify(repository).save(any(Document.class));
        assertThat(id).isNotNull();
    }
}
```

#### 6.3 Integration Tests (Spring)
```java
@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerIntegrationTest {
    @Autowired MockMvc mockMvc;
    
    @Test
    void shouldUploadDocumentEndToEnd() {
        // Full integration test with Spring context
    }
}
```

### Phase 7: Architectural Enforcement (Week 4)

1. **Package-private access**:
   - Make domain classes package-private where possible
   - Only expose through ports

2. **ArchUnit tests**:
```java
@ArchTest
static final ArchRule domainShouldNotDependOnFrameworks = 
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "javax.persistence..");
```

3. **Module boundaries**:
   - Consider using Java modules for stronger encapsulation
   - Or use Maven modules for physical separation

## Migration Steps

### Week 1: Foundation
- [ ] Create new domain model structure
- [ ] Define all port interfaces
- [ ] Create value objects
- [ ] Implement domain services

### Week 2: Core Implementation  
- [ ] Implement use cases
- [ ] Create adapter implementations
- [ ] Wire components with Spring configuration
- [ ] Ensure all tests pass

### Week 3: Cleanup
- [ ] Remove duplicate code
- [ ] Refactor service layer
- [ ] Update all dependencies
- [ ] Fix compilation errors

### Week 4: Validation
- [ ] Add architecture tests
- [ ] Complete test coverage
- [ ] Performance testing
- [ ] Documentation update

## Success Criteria

1. **No framework dependencies in domain layer** ✓
2. **All business logic in domain or use cases** ✓
3. **Clean port interfaces** ✓
4. **Testable without Spring** ✓
5. **No duplicate implementations** ✓
6. **Clear dependency direction** ✓
7. **Architecture tests passing** ✓

## Risk Mitigation

1. **Gradual migration**: Keep old code working while building new
2. **Feature flags**: Toggle between old and new implementations
3. **Parallel run**: Run both implementations and compare results
4. **Incremental deployment**: Deploy one use case at a time
5. **Rollback plan**: Keep ability to revert to old implementation

## Benefits After Refactoring

1. **Testability**: Domain logic testable without Spring
2. **Maintainability**: Clear separation of concerns
3. **Flexibility**: Easy to change frameworks or databases
4. **Understanding**: Business logic clearly visible in domain
5. **Performance**: Optimizable at each layer independently