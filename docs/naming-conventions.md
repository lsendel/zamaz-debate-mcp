# Naming Conventions Guide

## Overview

This document establishes consistent naming conventions for the Zamaz Debate MCP Services system to improve code readability, maintainability, and developer experience.

## Java Naming Conventions

### 1. Classes

#### Controllers
- **Pattern**: `{Entity}Controller`
- **Purpose**: Handle HTTP requests and responses
- **Examples**:
  - `OrganizationController` - Handles organization-related endpoints
  - `DebateController` - Handles debate-related endpoints
  - `UserController` - Handles user-related endpoints

#### Services
- **Pattern**: `{Entity}Service`
- **Purpose**: Contain business logic and orchestrate operations
- **Examples**:
  - `OrganizationService` - Business logic for organization operations
  - `DebateService` - Business logic for debate operations
  - `AuthenticationService` - Business logic for authentication

#### Repositories
- **Pattern**: `{Entity}Repository`
- **Purpose**: Data access layer interfaces
- **Examples**:
  - `OrganizationRepository` - Data access for organizations
  - `UserRepository` - Data access for users
  - `DebateRepository` - Data access for debates

#### Entities
- **Pattern**: `{EntityName}` (singular noun)
- **Purpose**: JPA entities representing database tables
- **Examples**:
  - `Organization` - Represents organizations table
  - `User` - Represents users table
  - `Debate` - Represents debates table
  - `DebateParticipant` - Represents debate_participants table

#### DTOs (Data Transfer Objects)
- **Pattern**: `{Entity}Dto` with nested static classes
- **Purpose**: Data transfer between layers
- **Examples**:
  ```java
  public class OrganizationDto {
      // Main DTO fields
      
      public static class CreateOrganizationRequest {
          // Request-specific fields
      }
      
      public static class UpdateOrganizationRequest {
          // Update-specific fields
      }
      
      public static class OrganizationResponse {
          // Response-specific fields
      }
  }
  ```

#### Exceptions
- **Pattern**: `{Specific}Exception` extending base exceptions
- **Purpose**: Handle specific error conditions
- **Examples**:
  - `OrganizationNotFoundException` extends `McpBusinessException`
  - `InvalidDebateStateException` extends `McpBusinessException`
  - `AuthenticationFailedException` extends `McpSecurityException`

#### Configuration Classes
- **Pattern**: `{Feature}Config`
- **Purpose**: Spring configuration classes
- **Examples**:
  - `SecurityConfig` - Security configuration
  - `DatabaseConfig` - Database configuration
  - `RedisConfig` - Redis configuration

#### Utility Classes
- **Pattern**: `{Purpose}Utils` or `{Purpose}Helper`
- **Purpose**: Static utility methods
- **Examples**:
  - `ValidationUtils` - Validation helper methods
  - `DateTimeUtils` - Date/time utility methods
  - `JsonUtils` - JSON processing utilities

### 2. Methods

#### Naming Patterns
- Use descriptive verbs that clearly indicate the method's purpose
- Follow camelCase convention
- Be specific about the action being performed

#### CRUD Operations
- **Create**: `create{Entity}`, `add{Entity}`, `register{Entity}`
  - `createOrganization(CreateOrganizationRequest request)`
  - `addUserToOrganization(UUID orgId, UUID userId)`
  - `registerDebateParticipant(UUID debateId, UUID userId)`

- **Read**: `get{Entity}`, `find{Entity}`, `list{Entities}`, `search{Entities}`
  - `getOrganization(UUID id)`
  - `findOrganizationByName(String name)`
  - `listUserOrganizations(UUID userId)`
  - `searchDebatesByTopic(String topic)`

- **Update**: `update{Entity}`, `modify{Entity}`, `change{Entity}`
  - `updateOrganization(UUID id, UpdateOrganizationRequest request)`
  - `modifyDebateSettings(UUID debateId, DebateSettings settings)`
  - `changeUserRole(UUID orgId, UUID userId, String role)`

- **Delete**: `delete{Entity}`, `remove{Entity}`, `deactivate{Entity}`
  - `deleteOrganization(UUID id)`
  - `removeUserFromOrganization(UUID orgId, UUID userId)`
  - `deactivateDebate(UUID debateId)`

#### Business Logic Methods
- Use action verbs that describe business operations
- **Examples**:
  - `startDebate(UUID debateId)`
  - `pauseDebate(UUID debateId)`
  - `calculateDebateScore(UUID debateId)`
  - `validateOrganizationAccess(UUID orgId, UUID userId)`
  - `processDebateRound(UUID debateId, DebateRound round)`

#### Validation Methods
- **Pattern**: `validate{What}`, `is{Condition}`, `has{Condition}`
- **Examples**:
  - `validateOrganizationRequest(CreateOrganizationRequest request)`
  - `isUserAuthorized(UUID userId, String permission)`
  - `hasActiveSubscription(UUID orgId)`

#### Conversion/Mapping Methods
- **Pattern**: `to{TargetType}`, `from{SourceType}`, `map{SourceToTarget}`
- **Examples**:
  - `toDto(Organization entity)`
  - `fromDto(OrganizationDto dto)`
  - `mapEntityToResponse(Organization entity)`

### 3. Variables

#### Field Names
- Use descriptive nouns that clearly indicate the content
- Follow camelCase convention
- Avoid abbreviations unless they are widely understood

#### Good Examples
```java
// Service fields
private final OrganizationService organizationService;
private final UserRepository userRepository;
private final PasswordEncoder passwordEncoder;

// Business logic variables
List<DebateParticipant> activeParticipants;
Map<UUID, OrganizationSettings> organizationSettingsCache;
Set<String> allowedRoles;
LocalDateTime debateStartTime;
Duration debateTimeLimit;

// Request/Response variables
CreateOrganizationRequest organizationRequest;
OrganizationDto organizationResponse;
Page<OrganizationDto> organizationPage;
```

#### Avoid These Patterns
```java
// Too generic
List<Object> list;
Map<String, String> map;
String str;

// Abbreviations
List<Org> orgs;
Map<String, Usr> usrs;
String orgName; // Use organizationName instead

// Single letters (except for loops)
String n; // Use name instead
UUID i; // Use id instead
```

#### Collection Naming
- Use plural nouns for collections
- Be specific about the content type
- **Examples**:
  ```java
  List<Organization> organizations;
  Set<UUID> participantIds;
  Map<String, DebateSettings> debateSettingsByType;
  Queue<DebateMessage> pendingMessages;
  ```

#### Boolean Variables
- Use descriptive names that read like questions
- **Examples**:
  ```java
  boolean isActive;
  boolean hasPermission;
  boolean canStartDebate;
  boolean isDebateFinished;
  boolean shouldNotifyUsers;
  ```

#### Constants
- Use UPPER_SNAKE_CASE for static final fields
- Group related constants in dedicated classes
- **Examples**:
  ```java
  public static final String DEFAULT_ROLE = "member";
  public static final int MAX_PARTICIPANTS = 10;
  public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);
  
  // In a constants class
  public class DebateConstants {
      public static final int MIN_PARTICIPANTS = 2;
      public static final int MAX_PARTICIPANTS = 10;
      public static final Duration DEFAULT_ROUND_DURATION = Duration.ofMinutes(5);
  }
  ```

## Method Parameter Guidelines

### Parameter Naming
- Use descriptive names that indicate the parameter's purpose
- Match the naming style of the corresponding field or variable
- **Examples**:
  ```java
  public OrganizationDto createOrganization(CreateOrganizationRequest request);
  public void addUserToOrganization(UUID organizationId, UUID userId, String role);
  public Page<DebateDto> searchDebates(String searchTerm, Pageable pageable);
  ```

### Parameter Order
1. Primary entity identifiers (IDs)
2. Request/data objects
3. Configuration parameters
4. Pagination/sorting parameters

## Package Naming

### Standard Package Structure
```
com.zamaz.mcp.{service}.{layer}
├── config/          # @Configuration classes
├── controller/      # @RestController classes  
├── dto/            # Data Transfer Objects
├── entity/         # @Entity JPA classes
├── exception/      # Custom exception classes
├── repository/     # @Repository interfaces
├── service/        # @Service business logic
└── util/           # Utility classes
```

### Package Naming Rules
- Use lowercase letters only
- Use dots to separate package levels
- Be descriptive but concise
- Follow the established hierarchy

## Database Naming Conventions

### Table Names
- Use **snake_case** with **plural** nouns
- Examples: `organizations`, `users`, `debate_participants`
- Junction tables: `{entity1}_{entity2}` (e.g., `organization_users`)

### Column Names
- Use **snake_case** with **singular** nouns
- Primary keys: Always use `id`
- Foreign keys: `{referenced_table_singular}_id` (e.g., `organization_id`)
- Boolean flags: `is_{condition}` (e.g., `is_active`)
- Timestamps: `created_at`, `updated_at`, `deleted_at`

### Index Names
- Pattern: `idx_{table}_{column(s)}` (e.g., `idx_organizations_name`)
- Unique indexes: `uk_{table}_{column(s)}` (e.g., `uk_users_email`)
- Foreign key indexes: `fk_{table}_{referenced_table}`

### Constraint Names
- Foreign keys: `fk_{table}_{column}_{referenced_table}`
- Check constraints: `chk_{table}_{column}_{condition}`
- Unique constraints: `uk_{table}_{column(s)}`

For detailed database naming conventions, see: [Database Naming Conventions](database-naming-conventions.md)

## Best Practices

### 1. Consistency
- Follow the same naming pattern throughout the codebase
- Use the same terminology across related classes
- Maintain consistency between entity names and their corresponding DTOs, services, etc.

### 2. Clarity Over Brevity
- Choose descriptive names over short ones
- Avoid abbreviations unless they are domain-standard
- Make the code self-documenting through good naming

### 3. Domain Language
- Use business domain terminology
- Align with the language used by stakeholders
- Be consistent with terms used in requirements and documentation

### 4. Avoid Misleading Names
- Don't use names that imply different functionality
- Avoid names that are too similar to existing ones
- Don't use technical jargon when business terms are clearer

## Examples of Good vs. Bad Naming

### Good Examples
```java
@Service
public class OrganizationService {
    
    public OrganizationDto createOrganization(CreateOrganizationRequest request) {
        Organization organization = mapRequestToEntity(request);
        Organization savedOrganization = organizationRepository.save(organization);
        return mapEntityToDto(savedOrganization);
    }
    
    public List<OrganizationDto> listUserOrganizations(UUID userId) {
        List<Organization> userOrganizations = organizationRepository.findByUserId(userId);
        return userOrganizations.stream()
            .map(this::mapEntityToDto)
            .collect(Collectors.toList());
    }
}
```

### Bad Examples
```java
@Service
public class OrgSvc {
    
    public OrgDto create(OrgReq req) {
        Org org = map(req);
        Org saved = repo.save(org);
        return toDto(saved);
    }
    
    public List<OrgDto> getUserOrgs(UUID uid) {
        List<Org> orgs = repo.findByUid(uid);
        return orgs.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
}
```

## Validation and Enforcement

These naming conventions are enforced through:

### 1. Automated Checkstyle Rules
- **Location**: `checkstyle.xml` and `.linting/java/checkstyle.xml`
- **Enforcement**: Build-time validation with Maven Checkstyle plugin
- **Coverage**: Class names, method names, variable names, package structure
- **Integration**: Runs automatically during `mvn compile` and `mvn test`

### 2. Custom Validation Script
- **Location**: `scripts/validate-naming-conventions.sh`
- **Purpose**: Additional validation beyond Checkstyle capabilities
- **Usage**: `./scripts/validate-naming-conventions.sh`
- **Features**: 
  - CRUD method pattern validation
  - Boolean method naming validation
  - Class type suffix validation
  - Variable naming pattern checks

### 3. IDE Configuration
- **IntelliJ IDEA**: Import Checkstyle configuration for real-time feedback
- **VS Code**: Use Checkstyle extension with project configuration
- **Eclipse**: Configure Checkstyle plugin with project rules

### 4. Code Review Process
- **Pre-commit**: Automated validation via git hooks
- **Pull Request**: Manual review checklist includes naming convention compliance
- **Documentation**: This guide serves as the authoritative reference

### 5. Continuous Integration
- **GitHub Actions**: Automated validation on every commit
- **Quality Gates**: Build fails if naming conventions are violated
- **Reporting**: Detailed reports available in CI/CD pipeline

## Migration Guidelines

When updating existing code to follow these conventions:
1. Update one service at a time to minimize disruption
2. Ensure all tests pass after renaming
3. Update related documentation and comments
4. Coordinate with team members on breaking changes
5. Use IDE refactoring tools to ensure consistency