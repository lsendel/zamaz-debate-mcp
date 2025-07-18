# MCP Context Service - Hexagonal Architecture Implementation

**Date**: 2025-07-17  
**Service**: mcp-context  
**Status**: Complete hexagonal architecture migration

## Architecture Overview

The mcp-context service has been successfully migrated to follow hexagonal architecture (Ports and Adapters pattern), ensuring:
- Complete separation of concerns
- Framework independence in the domain layer
- Clear dependency inversion (Domain ← Application ← Adapters)
- Enhanced testability and maintainability

## Layer Structure

```
src/main/java/com/zamaz/mcp/context/
├── domain/                          # Domain Layer (Core Business Logic)
│   ├── model/                       # Aggregates, Entities, Value Objects
│   │   ├── Context.java            # Aggregate Root - conversation context
│   │   ├── Message.java            # Entity - individual messages
│   │   ├── ContextId.java          # Value Object - context identifier
│   │   ├── MessageId.java          # Value Object - message identifier
│   │   ├── MessageRole.java        # Value Object - role enumeration
│   │   ├── ContextStatus.java      # Value Object - status enumeration
│   │   ├── TokenCount.java         # Value Object - token counting
│   │   ├── MessageContent.java     # Value Object - message content
│   │   ├── ContextMetadata.java    # Value Object - extensible metadata
│   │   ├── MessageSnapshot.java    # Value Object - immutable message snapshot
│   │   └── ContextWindow.java      # Value Object - token-limited message window
│   ├── event/                       # Domain Events
│   │   ├── ContextCreatedEvent.java
│   │   ├── MessageAppendedEvent.java
│   │   ├── MessageHiddenEvent.java
│   │   ├── ContextMetadataUpdatedEvent.java
│   │   ├── ContextArchivedEvent.java
│   │   └── ContextDeletedEvent.java
│   └── service/                     # Domain Services
│       ├── ContextDomainService.java
│       └── ContextDomainServiceImpl.java
│
├── application/                     # Application Layer (Use Cases & Orchestration)
│   ├── port/                        # Port Interfaces
│   │   ├── inbound/                # Use Case Interfaces (Inbound Ports)
│   │   │   ├── CreateContextUseCase.java
│   │   │   ├── GetContextUseCase.java
│   │   │   ├── AppendMessageUseCase.java
│   │   │   ├── GetContextWindowUseCase.java
│   │   │   ├── UpdateContextMetadataUseCase.java
│   │   │   ├── ArchiveContextUseCase.java
│   │   │   └── DeleteContextUseCase.java
│   │   └── outbound/               # Repository & Service Interfaces (Outbound Ports)
│   │       ├── ContextRepository.java
│   │       ├── TokenCountingService.java
│   │       └── ContextCacheService.java
│   ├── command/                     # Commands (CQRS Write Side)
│   │   ├── CreateContextCommand.java
│   │   ├── AppendMessageCommand.java
│   │   ├── UpdateContextMetadataCommand.java
│   │   ├── ArchiveContextCommand.java
│   │   └── DeleteContextCommand.java
│   ├── query/                       # Queries (CQRS Read Side)
│   │   ├── GetContextQuery.java
│   │   ├── GetContextWindowQuery.java
│   │   └── ContextView.java
│   └── usecase/                     # Use Case Implementations
│       ├── CreateContextUseCaseImpl.java
│       ├── GetContextUseCaseImpl.java
│       ├── AppendMessageUseCaseImpl.java
│       ├── GetContextWindowUseCaseImpl.java
│       ├── UpdateContextMetadataUseCaseImpl.java
│       ├── ArchiveContextUseCaseImpl.java
│       └── DeleteContextUseCaseImpl.java
│
└── adapter/                         # Adapter Layer (Infrastructure)
    ├── web/                         # Web Adapters (Inbound)
    │   ├── controller/
    │   │   └── ContextController.java
    │   ├── dto/                     # Web DTOs
    │   │   ├── CreateContextRequest.java
    │   │   ├── AppendMessageRequest.java
    │   │   ├── UpdateMetadataRequest.java
    │   │   ├── ContextResponse.java
    │   │   ├── ContextWindowResponse.java
    │   │   └── ErrorResponse.java
    │   └── mapper/
    │       └── ContextWebMapper.java
    ├── persistence/                 # Persistence Adapters (Outbound)
    │   ├── entity/                  # JPA Entities
    │   │   ├── ContextEntity.java
    │   │   ├── MessageEntity.java
    │   │   ├── ContextStatusEntity.java
    │   │   └── MessageRoleEntity.java
    │   ├── repository/
    │   │   ├── SpringDataContextRepository.java
    │   │   └── JpaContextRepository.java
    │   └── mapper/
    │       └── ContextPersistenceMapper.java
    ├── external/                    # External Service Adapters (Outbound)
    │   ├── JtokkitTokenCountingService.java
    │   └── RedisContextCacheService.java
    └── infrastructure/              # Infrastructure Adapters
        └── ContextDomainServiceAdapter.java
```

## Key Domain Concepts

### Context Aggregate
The `Context` is the main aggregate root that:
- Manages conversation contexts with multi-tenant isolation
- Contains messages as child entities
- Enforces business rules (token limits, message ordering)
- Publishes domain events for state changes
- Maintains consistency boundaries

### Core Business Rules
1. **Multi-tenancy**: All operations must be scoped to organizationId
2. **Token Management**: Tracks and limits token usage per context
3. **Message Ordering**: Messages maintain chronological order
4. **Context Lifecycle**: ACTIVE → ARCHIVED → DELETED progression
5. **Versioning**: Immutable snapshots for context history

### Value Objects
- **ContextId/MessageId**: Strong typing for identifiers
- **TokenCount**: Ensures non-negative token counting with math operations
- **MessageContent**: Validates content length and provides truncation
- **ContextMetadata**: Immutable, extensible key-value store
- **ContextWindow**: Token-limited view of messages

## Use Cases

### Primary Operations
1. **CreateContext**: Creates new conversation context
2. **AppendMessage**: Adds messages with token counting and validation
3. **GetContext**: Retrieves full context with all messages
4. **GetContextWindow**: Creates token-limited message windows
5. **UpdateMetadata**: Updates context metadata
6. **ArchiveContext**: Marks context as archived
7. **DeleteContext**: Soft deletes context

### Advanced Features
- **Token Counting**: Accurate token calculation using Jtokkit library
- **Caching**: Redis-based caching for performance
- **Domain Validation**: Business rule enforcement through domain services
- **Event Publishing**: Domain events for audit trails and integration

## Adapter Implementations

### Web Adapters
- **ContextController**: REST API with OpenAPI documentation
- **DTOs**: Clean separation between API contracts and domain models
- **Validation**: Jakarta validation annotations on request DTOs
- **Error Handling**: Structured error responses with appropriate HTTP status codes

### Persistence Adapters
- **JPA Entities**: Optimized for PostgreSQL with JSON metadata storage
- **Repository**: Spring Data JPA with custom queries
- **Mapping**: Clean conversion between entities and domain models
- **Indexing**: Performance-optimized database indexes

### External Service Adapters
- **Token Counting**: Jtokkit integration with multiple model support
- **Caching**: Redis integration with TTL and pattern-based eviction
- **Error Handling**: Graceful degradation with fallback mechanisms

## Dependency Injection

All components are wired through Spring configuration:

```java
@Bean
public CreateContextUseCase createContextUseCase(
    ContextRepository contextRepository,
    TransactionManager transactionManager
) {
    return new CreateContextUseCaseImpl(contextRepository, transactionManager);
}
```

## Testing Strategy

### Domain Layer Tests
- Pure unit tests with no framework dependencies
- Test business logic and invariants
- No mocks needed for value objects and entities

### Application Layer Tests
- Mock outbound ports (repositories, services)
- Test use case orchestration logic
- Verify command/query handling

### Adapter Layer Tests
- Integration tests for web endpoints
- Repository tests with test containers
- Cache service tests with embedded Redis

## Migration Benefits

1. **Testability**: Each layer can be tested independently
2. **Flexibility**: Easy to swap implementations (JPA → MongoDB, Redis → Caffeine)
3. **Maintainability**: Clear separation of concerns reduces coupling
4. **Framework Independence**: Domain logic is completely portable
5. **Business Focus**: Core business rules are isolated and prominent

## API Examples

### Create Context
```bash
POST /api/v1/contexts
X-Organization-Id: org-123
{
  "name": "New Discussion",
  "metadata": {
    "topic": "AI Architecture",
    "participants": ["user1", "user2"]
  }
}
```

### Append Message
```bash
POST /api/v1/contexts/{contextId}/messages
X-Organization-Id: org-123
{
  "role": "user",
  "content": "What are the benefits of hexagonal architecture?",
  "model": "gpt-4"
}
```

### Get Context Window
```bash
GET /api/v1/contexts/{contextId}/window?maxTokens=2000&maxMessages=10
X-Organization-Id: org-123
```

## Performance Considerations

- **Caching**: Context windows cached for 1 hour
- **Database**: Optimized indexes for common query patterns
- **Token Counting**: Efficient encoding with fallback estimation
- **Pagination**: Large context lists support pagination
- **Connection Pooling**: HikariCP for database connections

## Monitoring & Observability

- **Structured Logging**: Context and correlation IDs in all log entries
- **Domain Events**: Audit trail for all business operations
- **Metrics**: Token usage, cache hit rates, API response times
- **Health Checks**: Database, Redis, and external service connectivity

This implementation provides a solid foundation for the context management service with clean architecture principles, ensuring long-term maintainability and evolution capability.