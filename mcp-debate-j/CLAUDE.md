# MCP Debate Java Service - CLAUDE.md

This file provides guidance to Claude Code when working with the mcp-debate-j service.

## Service Overview

The `mcp-debate-j` service is a Java implementation of the debate management service. It provides core debate domain models and business logic, serving as an alternative or complement to the Python mcp-debate service.

## Purpose

- **Domain Modeling**: Java-based debate entity management
- **Business Logic**: Service layer for debate operations
- **Future MCP Integration**: Foundation for Java MCP implementation
- **Type Safety**: Strongly-typed debate management

## Technology Stack

- **Language**: Java 11
- **Build Tool**: Maven
- **Testing**: JUnit 5
- **Architecture**: Layered service pattern
- **Future**: Spring Boot integration planned

## Current Implementation

### Domain Models

#### Debate Entity
```java
public class Debate {
    private String id;
    private String name;
    private String topic;
    private String description;
    private DebateStatus status;
    private List<Participant> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Participant Model
```java
public class Participant {
    private String id;
    private String name;
    private String role;
    private Map<String, Object> metadata;
}
```

#### Status Enum
```java
public enum DebateStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED
}
```

### Service Layer

The `DebateService` provides:
- `createDebate(String name, String topic)` - Create new debate
- `getDebate(String id)` - Retrieve debate by ID
- `getAllDebates()` - List all debates
- `startDebate(String id)` - Transition to ACTIVE status

### Repository Pattern

- **Interface**: `DebateRepository` defines data operations
- **Implementation**: `InMemoryDebateRepository` for testing
- **Future**: JPA/Hibernate implementation planned

## Development Guidelines

### Running the Service
```bash
# Build
mvn clean install

# Run tests
mvn test

# Package
mvn package
```

### Adding New Features

1. **Domain Models**: Add to com.example package
2. **Services**: Implement business logic with repository injection
3. **Repositories**: Define interface, provide implementations
4. **Tests**: Write unit tests for all components

### Code Structure
```
src/
├── main/java/com/example/
│   ├── Debate.java
│   ├── DebateRepository.java
│   ├── DebateService.java
│   ├── DebateStatus.java
│   ├── InMemoryDebateRepository.java
│   ├── Main.java
│   └── Participant.java
└── test/java/com/example/
    └── DebateServiceTest.java
```

## Integration Points

### With Python Services
- Share data models via JSON serialization
- Future: REST API for cross-language communication
- Potential: Shared database with Python services

### MCP Protocol
- Not yet implemented
- Future: Java MCP SDK integration
- Will expose same tools as Python service

## Testing Strategy

### Unit Tests
```java
@Test
void testCreateDebate() {
    DebateService service = new DebateService(repository);
    Debate debate = service.createDebate("Test", "AI Ethics");
    assertNotNull(debate.getId());
    assertEquals(DebateStatus.DRAFT, debate.getStatus());
}
```

### Integration Tests
- Test with real repositories
- Verify service interactions
- Database transaction tests

## Configuration

### Maven Dependencies
```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.8.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Future Dependencies
- Spring Boot Starter Web
- Spring Data JPA
- PostgreSQL Driver
- Java MCP SDK (when available)

## Known Limitations

1. **No Web Layer**: REST API not implemented
2. **In-Memory Only**: No persistent storage
3. **No MCP**: Model Context Protocol not integrated
4. **Basic Features**: Limited compared to Python service
5. **No Authentication**: Security not implemented

## Future Enhancements

### Phase 1: Spring Boot Integration
- Add Spring Boot framework
- Implement REST controllers
- Configure dependency injection
- Add application properties

### Phase 2: Database Integration
- JPA entities with annotations
- PostgreSQL configuration
- Migration scripts
- Transaction management

### Phase 3: MCP Implementation
- Integrate Java MCP SDK
- Implement MCP tools
- Add MCP resources
- Protocol compliance

### Phase 4: Feature Parity
- WebSocket support
- Debate orchestration
- LLM integration
- Multi-tenant support

## Development Best Practices

1. **Use dependency injection** for loose coupling
2. **Write tests first** (TDD approach)
3. **Document public APIs** with Javadoc
4. **Follow Java naming conventions**
5. **Use Optional for nullable returns**
6. **Implement proper equals/hashCode**
7. **Use builders for complex objects**

## Debugging Tips

1. **Enable debug logging** in future Spring config
2. **Use breakpoints** in service methods
3. **Inspect repository state** during tests
4. **Verify object initialization**
5. **Check null pointer scenarios**

## Security Considerations

- Input validation on all public methods
- Future: Organization-based access control
- Sanitize user-provided content
- Implement rate limiting
- Use prepared statements for queries

## Performance Considerations

- Lazy loading for participant lists
- Pagination for debate listings
- Caching for frequently accessed debates
- Connection pooling for database
- Async processing for long operations