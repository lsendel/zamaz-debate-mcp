# MCP RAG Java Service - CLAUDE.md

This file provides guidance to Claude Code when working with the mcp-rag-j service.

## Service Overview

The `mcp-rag-j` service is a Spring Boot-based Java implementation scaffold for Retrieval Augmented Generation functionality. Currently in early development stage with only basic Spring Boot setup.

## Purpose

- **RAG Implementation**: Future Java-based RAG service
- **Spring Boot Foundation**: Modern Java microservice architecture
- **Alternative Implementation**: Java option alongside Python RAG service
- **Enterprise Integration**: Java ecosystem compatibility

## Technology Stack

- **Language**: Java 8
- **Framework**: Spring Boot 2.5.4
- **Build Tool**: Maven
- **Future**: Vector databases, embedding libraries

## Current Implementation

### Main Application
```java
@SpringBootApplication
public class McpRagJApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpRagJApplication.class, args);
    }
}
```

Currently only contains Spring Boot application entry point.

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
java -jar target/mcp-rag-j-0.0.1-SNAPSHOT.jar
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
    name: mcp-rag-j
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

## Getting Started

To begin implementation:
1. Update Java version to 11+
2. Add Spring Web Starter
3. Create basic controller
4. Define domain models
5. Implement file upload
6. Add document processing