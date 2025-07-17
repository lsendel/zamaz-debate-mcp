# Hexagonal Architecture Implementation

This document describes the complete implementation of hexagonal architecture (Ports and Adapters) for the MCP Organization service.

## Architecture Overview

The implementation follows the hexagonal architecture pattern with strict dependency direction from outer layers to inner layers:

```
┌─────────────────────────────────────────────────────────────┐
│                    Adapters (Infrastructure)                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ Web Adapter │  │ Persistence │  │ External Services   │ │
│  │             │  │ Adapter     │  │ Adapter             │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────┬─────────────────┬─────────────────────────┘
                  │                 │
            ┌─────▼─────────────────▼─────┐
            │      Application Layer     │
            │   ┌─────┐         ┌─────┐   │
            │   │ Use │   ...   │ Use │   │
            │   │Cases│         │Cases│   │
            │   └─────┘         └─────┘   │
            └─────────────┬───────────────┘
                          │
                    ┌─────▼─────┐
                    │  Domain   │
                    │   Layer   │
                    │           │
                    └───────────┘
```

## Layer Breakdown

### 1. Domain Layer (Core)

**Location**: `src/main/java/com/zamaz/mcp/organization/domain/`

**Characteristics**:
- **Zero framework dependencies**
- Pure business logic
- Framework-agnostic
- Contains aggregates, entities, value objects, domain services, and events

**Components**:

#### Aggregates & Entities
- `Organization` - Main aggregate root for organization management
- `User` - User entity with organization context
- `OrganizationMember` - Value object for membership

#### Value Objects
- `OrganizationId` - Organization identifier
- `OrganizationName` - Validated organization name
- `OrganizationDescription` - Organization description
- `OrganizationSettings` - Immutable settings configuration
- `UserId` - User identifier
- `Role` - Hierarchical role enumeration

#### Domain Events
- `OrganizationCreatedEvent`
- `OrganizationUpdatedEvent`
- `UserAddedToOrganizationEvent`
- `UserRemovedFromOrganizationEvent`

#### Domain Services
- `OrganizationDomainService` - Complex business logic spanning aggregates

### 2. Application Layer

**Location**: `src/main/java/com/zamaz/mcp/organization/application/`

**Characteristics**:
- Orchestrates domain operations
- Defines ports (interfaces)
- Contains use cases
- Handles cross-cutting concerns

#### Inbound Ports (Use Cases)
- `CreateOrganizationUseCase`
- `GetOrganizationUseCase`
- `UpdateOrganizationUseCase`
- `AddUserToOrganizationUseCase`
- `RemoveUserFromOrganizationUseCase`

#### Outbound Ports (Repository & Service Interfaces)
- `OrganizationRepository`
- `UserRepository`
- `NotificationService`
- `AuthenticationService`

#### Commands & Queries (CQRS)
- `CreateOrganizationCommand`
- `UpdateOrganizationCommand`
- `AddUserToOrganizationCommand`
- `RemoveUserFromOrganizationCommand`
- `GetOrganizationQuery`
- `OrganizationView`

#### Use Case Implementations
- `CreateOrganizationUseCaseImpl`
- `GetOrganizationUseCaseImpl`
- `UpdateOrganizationUseCaseImpl`
- `AddUserToOrganizationUseCaseImpl`
- `RemoveUserFromOrganizationUseCaseImpl`

### 3. Adapter Layer (Infrastructure)

**Location**: `src/main/java/com/zamaz/mcp/organization/adapter/`

**Characteristics**:
- Framework-specific implementations
- Implements ports defined in application layer
- Handles external concerns

#### Web Adapters
- `OrganizationController` - REST API endpoints
- Request/Response DTOs
- `OrganizationWebMapper` - DTO↔Domain mapping

#### Persistence Adapters
- `JpaOrganizationRepository` - JPA implementation of repository port
- `JpaUserRepository` - JPA implementation of user repository
- JPA entities with proper mapping
- `OrganizationPersistenceMapper` - Entity↔Domain mapping

#### External Service Adapters
- `NotificationServiceAdapter` - Email/notification integration
- `AuthenticationServiceAdapter` - JWT authentication

#### Infrastructure Adapters
- `SpringDomainEventPublisher` - Event publishing
- `SpringTransactionManager` - Transaction management
- `BeanValidationService` - Bean validation
- `Slf4jDomainLoggerFactory` - Logging abstraction

## Dependency Injection Configuration

### ApplicationConfig
Wires use case implementations with their dependencies:

```java
@Bean
public CreateOrganizationUseCase createOrganizationUseCase(
    OrganizationRepository organizationRepository,
    UserRepository userRepository,
    // ... other dependencies
) {
    return new CreateOrganizationUseCaseImpl(
        organizationRepository,
        userRepository,
        // ...
    );
}
```

### Component Scanning
All adapter implementations use `@Component` annotation for automatic discovery:

```java
@Component
@RequiredArgsConstructor
public class JpaOrganizationRepository implements OrganizationRepository, PersistenceAdapter {
    // Implementation
}
```

## Key Architectural Benefits

### 1. Testability
Each layer can be tested independently:

```java
// Domain layer - no mocks needed
@Test
void shouldCreateOrganizationWithValidData() {
    var org = new Organization(
        OrganizationId.generate(),
        OrganizationName.from("Test Org"),
        OrganizationDescription.empty(),
        UserId.generate()
    );
    // Pure unit test
}

// Application layer - mock repositories
@Test
void shouldCreateOrganization() {
    // Use case test with mocked dependencies
}

// Adapter layer - integration tests
@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {
    // HTTP layer tests
}
```

### 2. Flexibility
Easy to swap implementations:

```java
// Can easily switch from JPA to MongoDB
@Component
public class MongoOrganizationRepository implements OrganizationRepository {
    // Different persistence implementation
}

// Can switch from email to SMS notifications
@Component
public class SmsNotificationService implements NotificationService {
    // Different notification implementation
}
```

### 3. Business Logic Isolation
Domain logic is completely isolated from infrastructure:

```java
// Pure domain logic - no framework dependencies
public class Organization extends AggregateRoot<OrganizationId> {
    public void addUser(UserId userId, Role role) {
        // Business rules
        if (!active) {
            throw new DomainRuleViolationException(/* ... */);
        }
        // More business logic
    }
}
```

### 4. Clear Boundaries
Explicit interfaces define what each layer needs:

```java
// Application layer defines what it needs from infrastructure
public interface OrganizationRepository extends Repository<Organization, OrganizationId> {
    Optional<Organization> findByName(OrganizationName name);
    boolean existsByName(OrganizationName name);
    // Clear contract
}

// Infrastructure provides implementation
@Component
public class JpaOrganizationRepository implements OrganizationRepository {
    // Implementation details hidden
}
```

## Migration Path

The implementation allows for gradual migration:

1. **Domain First**: Extract business logic to domain objects
2. **Application Layer**: Create use case interfaces and implementations
3. **Adapter Replacement**: Replace infrastructure one piece at a time
4. **Testing**: Add layer-specific tests

## Usage Patterns

### Creating an Organization
```java
// Client → Web Adapter → Use Case → Domain → Persistence Adapter
POST /api/v1/organizations
{
    "name": "My Organization",
    "description": "Description"
}

// Flow:
// 1. OrganizationController.createOrganization()
// 2. CreateOrganizationUseCase.execute()
// 3. Organization domain object created
// 4. OrganizationRepository.save()
// 5. Domain events published
// 6. Notifications sent
```

### Querying an Organization
```java
// Client → Web Adapter → Use Case → Repository Adapter
GET /api/v1/organizations/{id}

// Flow:
// 1. OrganizationController.getOrganization()
// 2. GetOrganizationUseCase.execute()
// 3. Repository query
// 4. Domain→View mapping
// 5. View→DTO mapping
```

## Error Handling

Each layer has appropriate error handling:

- **Domain**: `DomainException` for business rule violations
- **Application**: `UseCaseException` for application logic errors
- **Adapter**: `AdapterException` for infrastructure failures

## Monitoring & Observability

- Structured logging with context
- Domain events for audit trails
- Metrics at adapter boundaries
- Transaction boundaries clearly defined

This implementation provides a solid foundation for maintainable, testable, and flexible software that can evolve with changing requirements while preserving business logic integrity.