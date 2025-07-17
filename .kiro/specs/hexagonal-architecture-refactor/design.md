# Hexagonal Architecture Design Document

## Overview

This design document outlines the implementation of hexagonal architecture (Ports and Adapters pattern) across all microservices in the Zamaz Debate MCP Services project. The hexagonal architecture will provide clear separation of concerns, improved testability, and better maintainability by isolating business logic from external dependencies.

## Architecture

### Hexagonal Architecture Principles

The hexagonal architecture organizes code into three main layers:

1. **Domain Core (Inner Hexagon)**: Contains pure business logic, domain entities, and business rules
2. **Ports (Interfaces)**: Define contracts between the domain and external world
3. **Adapters (Outer Hexagon)**: Implement ports and handle external concerns (web, database, messaging)

### Dependency Direction

Dependencies flow inward toward the domain core:
- Adapters depend on Ports
- Ports are defined by the Domain
- Domain has no external dependencies

```
┌─────────────────────────────────────────────────────────┐
│                    Adapters Layer                       │
│  ┌─────────────┐                    ┌─────────────────┐ │
│  │   Inbound   │                    │    Outbound     │ │
│  │  Adapters   │                    │    Adapters     │ │
│  │             │                    │                 │ │
│  │ • REST API  │                    │ • Database      │ │
│  │ • GraphQL   │                    │ • External APIs │ │
│  │ • CLI       │                    │ • Message Queue │ │
│  └─────────────┘                    └─────────────────┘ │
│         │                                    │         │
│         ▼                                    ▼         │
│  ┌─────────────────────────────────────────────────────┐ │
│  │                 Ports Layer                         │ │
│  │  ┌─────────────┐                ┌─────────────────┐ │ │
│  │  │   Inbound   │                │    Outbound     │ │ │
│  │  │    Ports    │                │     Ports       │ │ │
│  │  │             │                │                 │ │ │
│  │  │ • Use Cases │                │ • Repositories  │ │ │
│  │  │ • Commands  │                │ • External APIs │ │ │
│  │  │ • Queries   │                │ • Event Bus     │ │ │
│  │  └─────────────┘                └─────────────────┘ │ │
│  └─────────────────────────────────────────────────────┘ │
│                           │                             │
│                           ▼                             │
│  ┌─────────────────────────────────────────────────────┐ │
│  │                 Domain Core                         │ │
│  │                                                     │ │
│  │ • Domain Entities                                   │ │
│  │ • Value Objects                                     │ │
│  │ • Domain Services                                   │ │
│  │ • Business Rules                                    │ │
│  │ • Domain Events                                     │ │
│  └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### Package Structure

Each microservice will follow this standardized package structure:

```
com.zamaz.mcp.{service}/
├── domain/                          # Domain Core
│   ├── model/                       # Domain entities and value objects
│   ├── service/                     # Domain services
│   ├── event/                       # Domain events
│   └── exception/                   # Domain exceptions
├── application/                     # Application Layer (Use Cases)
│   ├── port/                        # Port interfaces
│   │   ├── inbound/                 # Inbound ports (use cases)
│   │   └── outbound/                # Outbound ports (repositories, external services)
│   ├── usecase/                     # Use case implementations
│   └── service/                     # Application services
├── adapter/                         # Adapters Layer
│   ├── inbound/                     # Inbound adapters
│   │   ├── web/                     # REST controllers
│   │   ├── mcp/                     # MCP protocol handlers
│   │   └── event/                   # Event listeners
│   └── outbound/                    # Outbound adapters
│       ├── persistence/             # Database adapters
│       ├── external/                # External API clients
│       └── messaging/               # Message queue adapters
└── config/                          # Configuration and dependency injection
```

### Domain Layer Components

#### Domain Entities
Pure business objects with no framework dependencies:

```java
// Example: Organization domain entity
public class Organization {
    private final OrganizationId id;
    private OrganizationName name;
    private Description description;
    private Settings settings;
    private final List<OrganizationUser> users;
    private boolean active;
    
    // Business methods
    public void addUser(User user, Role role) {
        // Business logic for adding users
    }
    
    public void updateSettings(Settings newSettings) {
        // Business logic for updating settings
    }
}
```

#### Value Objects
Immutable objects representing domain concepts:

```java
public record OrganizationId(UUID value) {
    public OrganizationId {
        Objects.requireNonNull(value, "Organization ID cannot be null");
    }
}

public record OrganizationName(String value) {
    public OrganizationName {
        Objects.requireNonNull(value, "Organization name cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization name cannot be empty");
        }
    }
}
```

#### Domain Services
Business logic that doesn't naturally fit in entities:

```java
public class OrganizationDomainService {
    public boolean canUserJoinOrganization(User user, Organization organization) {
        // Complex business rules for user membership
    }
    
    public void validateOrganizationSettings(Settings settings) {
        // Business validation logic
    }
}
```

### Application Layer Components

#### Inbound Ports (Use Cases)
Define what the application can do:

```java
public interface CreateOrganizationUseCase {
    OrganizationId createOrganization(CreateOrganizationCommand command);
}

public interface GetOrganizationUseCase {
    Organization getOrganization(OrganizationId id);
}
```

#### Outbound Ports
Define what the application needs from external systems:

```java
public interface OrganizationRepository {
    void save(Organization organization);
    Optional<Organization> findById(OrganizationId id);
    List<Organization> findByUserId(UserId userId);
    boolean existsByName(OrganizationName name);
}

public interface NotificationService {
    void sendOrganizationCreatedNotification(Organization organization);
}
```

#### Use Case Implementations
Orchestrate business operations:

```java
@Component
public class CreateOrganizationUseCaseImpl implements CreateOrganizationUseCase {
    private final OrganizationRepository organizationRepository;
    private final NotificationService notificationService;
    private final OrganizationDomainService domainService;
    
    @Override
    public OrganizationId createOrganization(CreateOrganizationCommand command) {
        // Validate business rules
        domainService.validateOrganizationSettings(command.settings());
        
        // Check for duplicates
        if (organizationRepository.existsByName(command.name())) {
            throw new DuplicateOrganizationException(command.name());
        }
        
        // Create domain entity
        Organization organization = new Organization(
            OrganizationId.generate(),
            command.name(),
            command.description(),
            command.settings()
        );
        
        // Persist
        organizationRepository.save(organization);
        
        // Send notification
        notificationService.sendOrganizationCreatedNotification(organization);
        
        return organization.getId();
    }
}
```

### Adapter Layer Components

#### Inbound Adapters (Web Controllers)
Handle HTTP requests and translate to use case calls:

```java
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationWebAdapter {
    private final CreateOrganizationUseCase createOrganizationUseCase;
    private final GetOrganizationUseCase getOrganizationUseCase;
    
    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @RequestBody CreateOrganizationRequest request) {
        
        CreateOrganizationCommand command = new CreateOrganizationCommand(
            new OrganizationName(request.name()),
            new Description(request.description()),
            new Settings(request.settings())
        );
        
        OrganizationId id = createOrganizationUseCase.createOrganization(command);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new OrganizationResponse(id.value()));
    }
}
```

#### Outbound Adapters (Persistence)
Implement repository interfaces using JPA:

```java
@Component
public class JpaOrganizationRepository implements OrganizationRepository {
    private final SpringDataOrganizationRepository springRepository;
    private final OrganizationMapper mapper;
    
    @Override
    public void save(Organization organization) {
        OrganizationEntity entity = mapper.toEntity(organization);
        springRepository.save(entity);
    }
    
    @Override
    public Optional<Organization> findById(OrganizationId id) {
        return springRepository.findById(id.value())
            .map(mapper::toDomain);
    }
}
```

## Data Models

### Domain Models vs Persistence Models

#### Domain Models
Pure business objects with no persistence annotations:

```java
public class Organization {
    private final OrganizationId id;
    private OrganizationName name;
    private Description description;
    private Settings settings;
    private boolean active;
    private final List<OrganizationUser> users;
    
    // Constructor, getters, business methods
}
```

#### Persistence Models
JPA entities for database mapping:

```java
@Entity
@Table(name = "organizations")
public class OrganizationEntity {
    @Id
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode settings;
    
    @Column(name = "is_active")
    private boolean active;
    
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    private List<OrganizationUserEntity> organizationUsers;
    
    // JPA constructors, getters, setters
}
```

#### Mapping Between Models
Use MapStruct for clean mapping:

```java
@Mapper(componentModel = "spring")
public interface OrganizationMapper {
    Organization toDomain(OrganizationEntity entity);
    OrganizationEntity toEntity(Organization domain);
    
    default OrganizationId mapId(UUID id) {
        return new OrganizationId(id);
    }
    
    default UUID mapId(OrganizationId id) {
        return id.value();
    }
}
```

## Error Handling

### Domain Exceptions
Business rule violations:

```java
public class DuplicateOrganizationException extends DomainException {
    public DuplicateOrganizationException(OrganizationName name) {
        super("Organization with name '" + name.value() + "' already exists");
    }
}

public class InvalidOrganizationSettingsException extends DomainException {
    public InvalidOrganizationSettingsException(String message) {
        super(message);
    }
}
```

### Application Exceptions
Use case specific errors:

```java
public class OrganizationNotFoundException extends ApplicationException {
    public OrganizationNotFoundException(OrganizationId id) {
        super("Organization not found: " + id.value());
    }
}
```

### Adapter Exception Handling
Global exception handler for web layer:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("BUSINESS_RULE_VIOLATION", ex.getMessage()));
    }
    
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage()));
    }
}
```

## Testing Strategy

### Domain Layer Testing
Pure unit tests with no external dependencies:

```java
class OrganizationTest {
    @Test
    void shouldAddUserWithValidRole() {
        // Given
        Organization organization = new Organization(
            OrganizationId.generate(),
            new OrganizationName("Test Org"),
            new Description("Test Description"),
            Settings.defaultSettings()
        );
        User user = new User(UserId.generate(), "test@example.com");
        
        // When
        organization.addUser(user, Role.MEMBER);
        
        // Then
        assertThat(organization.getUsers()).hasSize(1);
        assertThat(organization.hasUser(user.getId())).isTrue();
    }
}
```

### Application Layer Testing
Test use cases with mocked ports:

```java
class CreateOrganizationUseCaseTest {
    @Mock
    private OrganizationRepository organizationRepository;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private CreateOrganizationUseCaseImpl useCase;
    
    @Test
    void shouldCreateOrganizationSuccessfully() {
        // Given
        CreateOrganizationCommand command = new CreateOrganizationCommand(
            new OrganizationName("Test Org"),
            new Description("Test Description"),
            Settings.defaultSettings()
        );
        
        when(organizationRepository.existsByName(command.name())).thenReturn(false);
        
        // When
        OrganizationId result = useCase.createOrganization(command);
        
        // Then
        assertThat(result).isNotNull();
        verify(organizationRepository).save(any(Organization.class));
        verify(notificationService).sendOrganizationCreatedNotification(any());
    }
}
```

### Adapter Layer Testing
Integration tests for adapters:

```java
@SpringBootTest
@Testcontainers
class JpaOrganizationRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    
    @Autowired
    private JpaOrganizationRepository repository;
    
    @Test
    void shouldSaveAndRetrieveOrganization() {
        // Given
        Organization organization = new Organization(
            OrganizationId.generate(),
            new OrganizationName("Test Org"),
            new Description("Test Description"),
            Settings.defaultSettings()
        );
        
        // When
        repository.save(organization);
        Optional<Organization> retrieved = repository.findById(organization.getId());
        
        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo(organization.getName());
    }
}
```

### End-to-End Testing
Full integration tests:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrganizationE2ETest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateOrganizationViaAPI() {
        // Given
        CreateOrganizationRequest request = new CreateOrganizationRequest(
            "Test Organization",
            "Test Description",
            Map.of("key", "value")
        );
        
        // When
        ResponseEntity<OrganizationResponse> response = restTemplate.postForEntity(
            "/api/v1/organizations",
            request,
            OrganizationResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isNotNull();
    }
}
```

## Configuration and Dependency Injection

### Spring Configuration
Wire adapters to ports:

```java
@Configuration
public class OrganizationConfiguration {
    
    @Bean
    public CreateOrganizationUseCase createOrganizationUseCase(
            OrganizationRepository organizationRepository,
            NotificationService notificationService,
            OrganizationDomainService domainService) {
        return new CreateOrganizationUseCaseImpl(
            organizationRepository,
            notificationService,
            domainService
        );
    }
    
    @Bean
    public OrganizationRepository organizationRepository(
            SpringDataOrganizationRepository springRepository,
            OrganizationMapper mapper) {
        return new JpaOrganizationRepository(springRepository, mapper);
    }
    
    @Bean
    public NotificationService notificationService() {
        return new EmailNotificationService();
    }
}
```

### Profile-Based Configuration
Different implementations for different environments:

```java
@Configuration
@Profile("test")
public class TestConfiguration {
    
    @Bean
    @Primary
    public NotificationService testNotificationService() {
        return new InMemoryNotificationService();
    }
}

@Configuration
@Profile("production")
public class ProductionConfiguration {
    
    @Bean
    public NotificationService productionNotificationService() {
        return new KafkaNotificationService();
    }
}
```

## Service-Specific Implementations

### MCP Organization Service
- **Domain**: Organization, User, Role management
- **Inbound Ports**: CreateOrganization, GetOrganization, ManageUsers
- **Outbound Ports**: OrganizationRepository, UserRepository, AuthenticationService
- **Adapters**: REST API, MCP Protocol, PostgreSQL, JWT Authentication

### MCP LLM Service
- **Domain**: LLM Provider, Chat Session, Message handling
- **Inbound Ports**: ProcessChatRequest, ManageProviders
- **Outbound Ports**: LLMProviderGateway, SessionRepository, RateLimitService
- **Adapters**: WebFlux API, OpenAI/Claude/Gemini clients, Redis cache

### MCP Controller Service
- **Domain**: Debate, Participant, Turn management
- **Inbound Ports**: StartDebate, ManageParticipants, ProcessTurns
- **Outbound Ports**: DebateRepository, LLMService, NotificationService
- **Adapters**: REST API, WebSocket, Database, External service clients

### MCP RAG Service
- **Domain**: Document, Vector, Search operations
- **Inbound Ports**: IndexDocument, SearchSimilar, ManageCollections
- **Outbound Ports**: VectorRepository, DocumentRepository, EmbeddingService
- **Adapters**: REST API, Qdrant client, File storage, Embedding models

## Migration Strategy

The refactoring will be implemented incrementally:

1. **Phase 1**: Establish domain models and core business logic
2. **Phase 2**: Create port interfaces and use case implementations
3. **Phase 3**: Refactor existing services to use ports
4. **Phase 4**: Implement new adapter structure
5. **Phase 5**: Update configuration and dependency injection
6. **Phase 6**: Enhance testing with new architecture
7. **Phase 7**: Documentation and examples