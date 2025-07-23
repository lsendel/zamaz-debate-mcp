# Naming Conventions Examples

This document provides comprehensive examples of good and bad naming patterns for the Zamaz Debate MCP Services system.

## Java Class Examples

### Controllers

#### ✅ Good Examples
```java
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {
    
    public ResponseEntity<OrganizationDto> createOrganization(
        @RequestBody CreateOrganizationRequest request) {
        // Implementation
    }
    
    public ResponseEntity<OrganizationDto> getOrganization(
        @PathVariable UUID organizationId) {
        // Implementation
    }
    
    public ResponseEntity<List<OrganizationDto>> listUserOrganizations(
        @PathVariable UUID userId) {
        // Implementation
    }
}

@RestController
@RequestMapping("/api/v1/debates")
public class DebateController {
    
    public ResponseEntity<DebateDto> startDebate(
        @PathVariable UUID debateId) {
        // Implementation
    }
}
```

#### ❌ Bad Examples
```java
// Missing Controller suffix
@RestController
public class Organization {
    // Should be OrganizationController
}

// Abbreviated name
@RestController
public class OrgController {
    // Should be OrganizationController
}

// Generic name
@RestController
public class Handler {
    // Should be specific like OrganizationController
}
```

### Services

#### ✅ Good Examples
```java
@Service
public class OrganizationService {
    
    public OrganizationDto createOrganization(CreateOrganizationRequest request) {
        validateOrganizationRequest(request);
        Organization organization = mapRequestToEntity(request);
        return saveAndMapToDto(organization);
    }
    
    public List<OrganizationDto> findOrganizationsByUserId(UUID userId) {
        List<Organization> organizations = organizationRepository.findByUserId(userId);
        return mapEntitiesToDtos(organizations);
    }
    
    private void validateOrganizationRequest(CreateOrganizationRequest request) {
        // Validation logic
    }
    
    private OrganizationDto mapEntityToDto(Organization entity) {
        // Mapping logic
    }
}

@Service
public class DebateService {
    
    public DebateDto startDebate(UUID debateId) {
        // Implementation
    }
    
    public boolean canUserJoinDebate(UUID userId, UUID debateId) {
        // Implementation
    }
    
    public void pauseDebate(UUID debateId) {
        // Implementation
    }
}
```

#### ❌ Bad Examples
```java
// Missing Service suffix
@Service
public class OrganizationManager {
    // Should be OrganizationService
}

// Abbreviated methods
@Service
public class OrganizationService {
    
    public OrganizationDto create(CreateOrgReq req) {
        // Should be createOrganization(CreateOrganizationRequest request)
    }
    
    public List<OrganizationDto> getByUid(UUID uid) {
        // Should be findOrganizationsByUserId(UUID userId)
    }
}
```

### Repositories

#### ✅ Good Examples
```java
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    
    Optional<Organization> findByNameIgnoreCase(String name);
    
    List<Organization> findByCreatedByUserId(UUID userId);
    
    @Query("SELECT o FROM Organization o WHERE o.isActive = true")
    List<Organization> findAllActiveOrganizations();
    
    boolean existsByNameIgnoreCase(String name);
}

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    List<User> findByOrganizationId(UUID organizationId);
    
    @Query("SELECT u FROM User u WHERE u.lastLoginAt > :since")
    List<User> findRecentlyActiveUsers(@Param("since") LocalDateTime since);
}
```

#### ❌ Bad Examples
```java
// Missing Repository suffix
@Repository
public interface OrganizationDao {
    // Should be OrganizationRepository
}

// Poor method naming
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    
    Optional<Organization> getByName(String name);
    // Should be findByNameIgnoreCase for consistency
    
    List<Organization> getAllByUser(UUID uid);
    // Should be findByCreatedByUserId(UUID userId)
}
```

### DTOs (Data Transfer Objects)

#### ✅ Good Examples
```java
public class OrganizationDto {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private boolean isActive;
    
    // Nested static classes for specific requests/responses
    public static class CreateOrganizationRequest {
        @NotBlank(message = "Organization name is required")
        private String name;
        
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        private String description;
        
        // Getters and setters
    }
    
    public static class UpdateOrganizationRequest {
        @NotBlank(message = "Organization name is required")
        private String name;
        
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        private String description;
        
        // Getters and setters
    }
    
    public static class OrganizationResponse {
        private UUID id;
        private String name;
        private String description;
        private LocalDateTime createdAt;
        private boolean isActive;
        private int memberCount;
        
        // Getters and setters
    }
}

public class DebateDto {
    private UUID id;
    private String topic;
    private DebateStatus status;
    private List<ParticipantDto> participants;
    
    public static class CreateDebateRequest {
        @NotBlank(message = "Debate topic is required")
        private String topic;
        
        @Min(value = 2, message = "At least 2 participants required")
        @Max(value = 10, message = "Maximum 10 participants allowed")
        private int maxParticipants;
        
        // Getters and setters
    }
}
```

#### ❌ Bad Examples
```java
// Missing Dto suffix
public class Organization {
    // Should be OrganizationDto (conflicts with entity name)
}

// Abbreviated names
public class OrgDto {
    // Should be OrganizationDto
}

// Poor structure
public class OrganizationCreateRequest {
    // Should be nested inside OrganizationDto as CreateOrganizationRequest
}
```

### Entities

#### ✅ Good Examples
```java
@Entity
@Table(name = "organizations")
public class Organization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    private List<OrganizationUser> organizationUsers = new ArrayList<>();
    
    // Constructors, getters, setters
    
    public void updateName(String newName) {
        this.name = newName;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean hasActiveMembers() {
        return organizationUsers.stream()
            .anyMatch(ou -> ou.isActive());
    }
}

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;
    
    // Methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
```

#### ❌ Bad Examples
```java
// Plural entity name
@Entity
@Table(name = "organizations")
public class Organizations {
    // Should be Organization (singular)
}

// Poor column mapping
@Entity
@Table(name = "organizations")
public class Organization {
    
    @Column(name = "orgName")  // Should be "name" or "organization_name"
    private String name;
    
    @Column(name = "isActive")  // Should be "is_active"
    private boolean active;
}
```

## Method Naming Examples

### CRUD Operations

#### ✅ Good Examples
```java
@Service
public class OrganizationService {
    
    // Create operations
    public OrganizationDto createOrganization(CreateOrganizationRequest request) { }
    public void addUserToOrganization(UUID organizationId, UUID userId) { }
    public void registerNewMember(UUID organizationId, String email) { }
    
    // Read operations
    public OrganizationDto getOrganization(UUID id) { }
    public Optional<OrganizationDto> findOrganizationByName(String name) { }
    public List<OrganizationDto> listUserOrganizations(UUID userId) { }
    public Page<OrganizationDto> searchOrganizations(String searchTerm, Pageable pageable) { }
    
    // Update operations
    public OrganizationDto updateOrganization(UUID id, UpdateOrganizationRequest request) { }
    public void modifyOrganizationSettings(UUID id, OrganizationSettings settings) { }
    public void changeUserRole(UUID orgId, UUID userId, String role) { }
    
    // Delete operations
    public void deleteOrganization(UUID id) { }
    public void removeUserFromOrganization(UUID orgId, UUID userId) { }
    public void deactivateOrganization(UUID id) { }
}
```

#### ❌ Bad Examples
```java
@Service
public class OrganizationService {
    
    // Generic/unclear names
    public OrganizationDto save(CreateOrganizationRequest request) { }
    // Should be createOrganization
    
    public OrganizationDto get(UUID id) { }
    // Should be getOrganization
    
    // Abbreviated names
    public List<OrganizationDto> getOrgs(UUID uid) { }
    // Should be listUserOrganizations(UUID userId)
    
    // Inconsistent naming
    public void remove(UUID id) { }
    // Should be deleteOrganization or removeOrganization
}
```

### Boolean Methods

#### ✅ Good Examples
```java
@Service
public class OrganizationService {
    
    public boolean isUserMemberOfOrganization(UUID userId, UUID orgId) { }
    
    public boolean hasActiveSubscription(UUID organizationId) { }
    
    public boolean canUserCreateDebate(UUID userId, UUID orgId) { }
    
    public boolean shouldNotifyMembers(UUID organizationId) { }
    
    public boolean willExceedMemberLimit(UUID orgId, int newMembers) { }
}

@Entity
public class Organization {
    
    public boolean isActive() { }
    
    public boolean hasMembers() { }
    
    public boolean canAcceptNewMembers() { }
}
```

#### ❌ Bad Examples
```java
@Service
public class OrganizationService {
    
    // Missing boolean prefix
    public boolean userIsMember(UUID userId, UUID orgId) { }
    // Should be isUserMemberOfOrganization
    
    public boolean activeSubscription(UUID organizationId) { }
    // Should be hasActiveSubscription
    
    // Unclear meaning
    public boolean check(UUID userId, UUID orgId) { }
    // Should be specific like canUserAccessOrganization
}
```

### Validation Methods

#### ✅ Good Examples
```java
@Service
public class OrganizationService {
    
    private void validateOrganizationRequest(CreateOrganizationRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Organization name is required");
        }
    }
    
    private void validateUserPermissions(UUID userId, UUID organizationId) {
        if (!isUserMemberOfOrganization(userId, organizationId)) {
            throw new UnauthorizedAccessException("User not member of organization");
        }
    }
    
    private void validateMembershipLimit(UUID organizationId, int newMemberCount) {
        Organization org = getOrganization(organizationId);
        if (org.getMemberCount() + newMemberCount > org.getMaxMembers()) {
            throw new BusinessRuleException("Would exceed membership limit");
        }
    }
}
```

#### ❌ Bad Examples
```java
@Service
public class OrganizationService {
    
    // Generic validation name
    private void validate(CreateOrganizationRequest request) { }
    // Should be validateOrganizationRequest
    
    // Unclear purpose
    private void check(UUID userId, UUID orgId) { }
    // Should be validateUserPermissions or similar
}
```

## Variable Naming Examples

### Field Names

#### ✅ Good Examples
```java
@Service
public class OrganizationService {
    
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    // Business logic variables
    public void processOrganizationCreation(CreateOrganizationRequest request) {
        List<String> validationErrors = new ArrayList<>();
        Set<String> existingOrganizationNames = getExistingNames();
        Map<UUID, UserDto> organizationAdmins = new HashMap<>();
        LocalDateTime creationTimestamp = LocalDateTime.now();
        Duration processingTimeout = Duration.ofMinutes(5);
        
        // Boolean variables
        boolean isNameUnique = !existingOrganizationNames.contains(request.getName());
        boolean hasValidEmail = isValidEmail(request.getAdminEmail());
        boolean canCreateOrganization = isNameUnique && hasValidEmail;
        boolean shouldSendWelcomeEmail = request.isSendWelcomeEmail();
    }
}
```

#### ❌ Bad Examples
```java
@Service
public class OrganizationService {
    
    // Abbreviated field names
    private final OrganizationRepository orgRepo;  // Should be organizationRepository
    private final UserRepository userRepo;         // Should be userRepository
    private final EmailService emailSvc;          // Should be emailService
    
    // Generic variable names
    public void processOrganizationCreation(CreateOrganizationRequest request) {
        List<String> list = new ArrayList<>();     // Should be validationErrors
        Set<String> set = getExistingNames();      // Should be existingOrganizationNames
        Map<UUID, UserDto> map = new HashMap<>();  // Should be organizationAdmins
        
        // Poor boolean naming
        boolean flag = true;                       // Should be descriptive
        boolean check = isValid(request);          // Should be isRequestValid
    }
}
```

### Constants

#### ✅ Good Examples
```java
public class OrganizationConstants {
    
    public static final String DEFAULT_ROLE = "member";
    public static final int MAX_ORGANIZATION_NAME_LENGTH = 255;
    public static final int MIN_ORGANIZATION_NAME_LENGTH = 3;
    public static final int MAX_MEMBERS_PER_ORGANIZATION = 1000;
    public static final Duration DEFAULT_SESSION_TIMEOUT = Duration.ofHours(24);
    
    // Error messages
    public static final String ERROR_ORGANIZATION_NOT_FOUND = "Organization not found";
    public static final String ERROR_DUPLICATE_ORGANIZATION_NAME = "Organization name already exists";
    public static final String ERROR_INSUFFICIENT_PERMISSIONS = "Insufficient permissions";
    
    // Configuration keys
    public static final String CONFIG_KEY_EMAIL_ENABLED = "organization.email.enabled";
    public static final String CONFIG_KEY_MAX_MEMBERS = "organization.limits.max-members";
}

public class DebateConstants {
    
    public static final int MIN_PARTICIPANTS = 2;
    public static final int MAX_PARTICIPANTS = 10;
    public static final Duration DEFAULT_ROUND_DURATION = Duration.ofMinutes(5);
    public static final Duration MAX_DEBATE_DURATION = Duration.ofHours(2);
}
```

#### ❌ Bad Examples
```java
public class Constants {
    
    // Poor naming
    public static final String ROLE = "member";           // Should be DEFAULT_ROLE
    public static final int MAX_LEN = 255;                // Should be MAX_ORGANIZATION_NAME_LENGTH
    public static final int TIMEOUT = 24;                 // Should be DEFAULT_SESSION_TIMEOUT_HOURS
    
    // Abbreviated names
    public static final String ORG_NOT_FOUND = "Not found";  // Should be ERROR_ORGANIZATION_NOT_FOUND
    public static final String DUP_NAME = "Duplicate";       // Should be ERROR_DUPLICATE_ORGANIZATION_NAME
}
```

## Database Naming Examples

### Table Names

#### ✅ Good Examples
```sql
-- Main entity tables (plural, snake_case)
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Junction tables
CREATE TABLE organization_users (
    organization_id UUID NOT NULL REFERENCES organizations(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(50) NOT NULL DEFAULT 'member',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (organization_id, user_id)
);

-- Audit tables
CREATE TABLE organizations_audit (
    audit_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by UUID REFERENCES users(id),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### ❌ Bad Examples
```sql
-- Wrong case and singular names
CREATE TABLE Organization (  -- Should be organizations (plural, lowercase)
    ID UUID PRIMARY KEY,     -- Should be id (lowercase)
    Name VARCHAR(255),       -- Should be name (lowercase)
    isActive BOOLEAN         -- Should be is_active (snake_case)
);

-- Poor naming
CREATE TABLE org (           -- Should be organizations (full name)
    id UUID PRIMARY KEY,
    orgName VARCHAR(255),    -- Should be name
    createdAt TIMESTAMP      -- Should be created_at (snake_case)
);
```

### Column Names

#### ✅ Good Examples
```sql
CREATE TABLE debates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Descriptive names
    topic VARCHAR(500) NOT NULL,
    description TEXT,
    
    -- Foreign keys with clear naming
    organization_id UUID NOT NULL REFERENCES organizations(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    
    -- Status and flags
    debate_status VARCHAR(50) NOT NULL DEFAULT 'pending',
    is_public BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    -- Numeric fields
    max_participants INTEGER NOT NULL DEFAULT 10,
    round_duration_minutes INTEGER NOT NULL DEFAULT 5,
    
    -- Timestamps
    scheduled_start_time TIMESTAMP,
    actual_start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- JSON configuration
    debate_settings JSONB DEFAULT '{}',
    participant_preferences JSONB DEFAULT '{}'
);
```

#### ❌ Bad Examples
```sql
CREATE TABLE debates (
    id UUID PRIMARY KEY,
    
    -- Poor naming
    name VARCHAR(500),           -- Should be topic
    desc TEXT,                   -- Should be description
    
    -- Inconsistent foreign key naming
    orgId UUID,                  -- Should be organization_id
    userId UUID,                 -- Should be created_by_user_id
    
    -- Poor boolean naming
    public BOOLEAN,              -- Should be is_public
    active BOOLEAN,              -- Should be is_active
    
    -- Unclear numeric fields
    maxPart INTEGER,             -- Should be max_participants
    duration INTEGER,            -- Should be round_duration_minutes
    
    -- Inconsistent timestamp naming
    createdAt TIMESTAMP,         -- Should be created_at
    startTime TIMESTAMP          -- Should be scheduled_start_time
);
```

### Index Names

#### ✅ Good Examples
```sql
-- Regular indexes
CREATE INDEX idx_organizations_name ON organizations(name);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_debates_status ON debates(debate_status);
CREATE INDEX idx_organization_users_user_id ON organization_users(user_id);

-- Composite indexes
CREATE INDEX idx_debates_org_status ON debates(organization_id, debate_status);
CREATE INDEX idx_users_active_created ON users(is_active, created_at);

-- Unique indexes
CREATE UNIQUE INDEX uk_organizations_name ON organizations(name);
CREATE UNIQUE INDEX uk_users_email ON users(email);

-- Foreign key indexes
CREATE INDEX fk_organization_users_organization ON organization_users(organization_id);
CREATE INDEX fk_organization_users_user ON organization_users(user_id);
```

#### ❌ Bad Examples
```sql
-- Poor index naming
CREATE INDEX org_name ON organizations(name);        -- Should be idx_organizations_name
CREATE INDEX user_idx ON users(email);               -- Should be idx_users_email
CREATE INDEX debate_status_idx ON debates(status);   -- Should be idx_debates_status

-- Unclear purpose
CREATE INDEX idx1 ON organizations(name);            -- Should be descriptive
CREATE INDEX temp_idx ON users(email);               -- Should be permanent name
```

This comprehensive guide should help developers understand and apply consistent naming conventions across the entire codebase.