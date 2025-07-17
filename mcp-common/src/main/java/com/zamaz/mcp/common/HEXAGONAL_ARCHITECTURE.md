# Hexagonal Architecture Foundation

This package contains the foundation classes and interfaces for implementing hexagonal architecture (Ports and Adapters) across all MCP services.

## Package Structure

```
com.zamaz.mcp.common/
├── domain/                    # Domain Layer (Core Business Logic)
│   ├── model/                 # Domain models
│   │   ├── DomainEntity      # Base class for entities
│   │   ├── ValueObject       # Marker interface for value objects
│   │   └── AggregateRoot     # Base class for aggregate roots
│   ├── event/                 # Domain events
│   │   ├── DomainEvent       # Base interface for events
│   │   └── AbstractDomainEvent # Base implementation
│   ├── exception/             # Domain exceptions
│   │   ├── DomainException   # Base domain exception
│   │   ├── EntityNotFoundException
│   │   └── DomainRuleViolationException
│   └── service/               # Domain services
│       └── DomainService     # Marker interface
│
├── application/               # Application Layer (Use Cases)
│   ├── port/                  # Port interfaces
│   │   ├── inbound/          # Inbound ports (use cases)
│   │   │   ├── UseCase       # Base use case interface
│   │   │   └── VoidUseCase   # Use case with no return
│   │   └── outbound/         # Outbound ports
│   │       └── Repository    # Base repository interface
│   ├── command/               # Commands (CQRS)
│   │   └── Command           # Command marker interface
│   ├── query/                 # Queries (CQRS)
│   │   └── Query             # Query marker interface
│   ├── exception/             # Application exceptions
│   │   ├── ApplicationException # Base exception
│   │   ├── ResourceNotFoundException
│   │   └── UseCaseException
│   └── service/               # Application services
│       └── ApplicationService # Marker interface
│
└── architecture/              # Architecture Support
    ├── adapter/               # Adapter layer markers
    │   ├── web/              # Web adapters
    │   │   └── WebAdapter    # Inbound adapter marker
    │   ├── persistence/      # Persistence adapters
    │   │   └── PersistenceAdapter # Outbound adapter marker
    │   └── external/         # External service adapters
    │       └── ExternalServiceAdapter # Outbound adapter marker
    ├── exception/             # Adapter exceptions
    │   ├── AdapterException  # Base adapter exception
    │   ├── PersistenceException
    │   └── ExternalServiceException
    └── mapper/                # Mapping utilities
        └── DomainMapper      # Base mapper interface
```

## Key Principles

### 1. Domain Layer Independence
- **NO framework dependencies** in domain layer
- Pure Java/business logic only
- No Spring annotations, no JPA annotations, no external libraries

### 2. Dependency Direction
- Dependencies flow inward: Adapters → Application → Domain
- Domain knows nothing about Application or Infrastructure
- Application knows Domain but not Infrastructure
- Adapters know both Application and Domain

### 3. Port Interfaces
- Inbound ports (use cases) define what the application can do
- Outbound ports (repositories, services) define what the application needs
- Ports belong to the application layer, not domain

## Usage Examples

### Domain Entity
```java
public class Organization extends AggregateRoot<OrganizationId> {
    private OrganizationName name;
    private Description description;
    private boolean active;
    
    public Organization(OrganizationId id, OrganizationName name) {
        super(id);
        this.name = name;
        this.active = true;
        
        // Raise domain event
        registerEvent(new OrganizationCreatedEvent(id.value()));
    }
    
    @Override
    public void validateInvariants() {
        if (name == null || name.value().isEmpty()) {
            throw new DomainRuleViolationException(
                "Organization.name.required",
                "Organization must have a name"
            );
        }
    }
}
```

### Value Object (Java 14+ Record)
```java
public record OrganizationId(UUID value) implements ValueObject {
    public OrganizationId {
        Objects.requireNonNull(value, "Organization ID cannot be null");
    }
    
    public static OrganizationId generate() {
        return new OrganizationId(UUID.randomUUID());
    }
}
```

### Use Case (Inbound Port)
```java
public interface CreateOrganizationUseCase 
    extends UseCase<CreateOrganizationCommand, OrganizationId> {
    // Interface defines the contract
}
```

### Use Case Implementation
```java
@Component  // Spring annotation only in application layer
public class CreateOrganizationUseCaseImpl implements CreateOrganizationUseCase {
    private final OrganizationRepository repository;
    
    @Override
    public OrganizationId execute(CreateOrganizationCommand command) {
        // Business logic orchestration
        var organization = new Organization(
            OrganizationId.generate(),
            command.name()
        );
        
        repository.save(organization);
        
        return organization.getId();
    }
}
```

### Repository (Outbound Port)
```java
public interface OrganizationRepository extends Repository<Organization, OrganizationId> {
    List<Organization> findByName(OrganizationName name);
    boolean existsByName(OrganizationName name);
}
```

### Web Adapter
```java
@RestController  // Spring annotations in adapter layer
@RequestMapping("/api/v1/organizations")
public class OrganizationController implements WebAdapter {
    private final CreateOrganizationUseCase createUseCase;
    
    @PostMapping
    public ResponseEntity<OrganizationResponse> create(
            @RequestBody CreateOrganizationRequest request) {
        
        // Map DTO to Command
        var command = new CreateOrganizationCommand(
            new OrganizationName(request.name()),
            new Description(request.description())
        );
        
        // Execute use case
        var id = createUseCase.execute(command);
        
        // Return response
        return ResponseEntity.created(URI.create("/api/v1/organizations/" + id.value()))
            .body(new OrganizationResponse(id.value()));
    }
}
```

### Persistence Adapter
```java
@Component  // Spring annotations in adapter layer
public class JpaOrganizationRepository 
    implements OrganizationRepository, PersistenceAdapter {
    
    private final SpringDataOrganizationRepository jpaRepository;
    private final OrganizationMapper mapper;
    
    @Override
    public Organization save(Organization organization) {
        var entity = mapper.fromDomain(organization);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Organization> findById(OrganizationId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain);
    }
}
```

## Testing Strategy

### Domain Layer Tests
```java
class OrganizationTest {
    @Test
    void shouldCreateOrganizationWithValidData() {
        // Pure unit test, no mocks needed
        var org = new Organization(
            OrganizationId.generate(),
            new OrganizationName("Test Org")
        );
        
        assertNotNull(org.getId());
        assertEquals("Test Org", org.getName().value());
        assertTrue(org.isActive());
    }
}
```

### Application Layer Tests
```java
class CreateOrganizationUseCaseTest {
    @Mock
    private OrganizationRepository repository;
    
    @InjectMocks
    private CreateOrganizationUseCaseImpl useCase;
    
    @Test
    void shouldCreateOrganization() {
        // Test with mocked dependencies
        var command = new CreateOrganizationCommand(
            new OrganizationName("Test"),
            new Description("Test Desc")
        );
        
        var result = useCase.execute(command);
        
        assertNotNull(result);
        verify(repository).save(any(Organization.class));
    }
}
```

### Adapter Layer Tests
```java
@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {
    @MockBean
    private CreateOrganizationUseCase createUseCase;
    
    @Test
    void shouldCreateOrganization() throws Exception {
        // Test HTTP layer
        given(createUseCase.execute(any()))
            .willReturn(OrganizationId.generate());
        
        mockMvc.perform(post("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Test Org",
                        "description": "Test Description"
                    }
                    """))
            .andExpect(status().isCreated());
    }
}
```

## Migration Guidelines

When migrating existing services to hexagonal architecture:

1. **Start with Domain**: Extract pure business logic into domain objects
2. **Define Ports**: Create use case and repository interfaces
3. **Implement Application Layer**: Create use case implementations
4. **Refactor Adapters**: Move controllers and repositories to adapter layer
5. **Remove Framework Dependencies**: Ensure domain has no framework code
6. **Add Tests**: Test each layer independently

## Benefits

- **Testability**: Each layer can be tested in isolation
- **Flexibility**: Easy to swap implementations (database, framework, etc.)
- **Maintainability**: Clear separation of concerns
- **Business Focus**: Domain logic is isolated and framework-agnostic
- **Dependency Control**: Dependencies flow in one direction