# Project Structure

## Root Level Organization

### Multi-Module Maven Project
- **pom.xml** - Parent POM defining dependencies, versions, and build configuration
- **Makefile** - User-friendly commands for development workflow
- **docker-compose.yml** - Main service orchestration
- **.env.example** - Environment variable template (never commit .env)

### Core Java Services (Microservices)
```
mcp-{service}/
├── pom.xml                    # Service-specific Maven configuration
├── Dockerfile                 # Container build instructions
├── src/main/java/com/zamaz/mcp/{service}/
│   ├── {Service}Application.java    # Spring Boot main class
│   ├── config/                      # Configuration classes
│   ├── controller/                  # REST controllers
│   ├── dto/                         # Data Transfer Objects
│   ├── entity/                      # JPA entities
│   ├── repository/                  # Data access layer
│   ├── service/                     # Business logic
│   └── exception/                   # Custom exceptions
├── src/main/resources/
│   ├── application.yml              # Main configuration
│   └── db/migration/                # Flyway database migrations
└── src/test/                        # Test classes
```

### Service Breakdown
- **mcp-common** - Shared utilities, DTOs, and configurations
- **mcp-security** - Security components and JWT handling
- **mcp-organization** (Port 5005) - Multi-tenant organization management
- **mcp-context** (Port 5001) - Context management service
- **mcp-controller** (Port 5013) - Debate orchestration (replaces mcp-debate)
- **mcp-llm** (Port 5002) - LLM provider gateway
- **mcp-rag** (Port 5004) - Retrieval Augmented Generation
- **mcp-template** (Port 5006) - Template management
- **mcp-modulith** - Modular monolith alternative

### Frontend Application
```
debate-ui/
├── package.json               # NPM dependencies and scripts
├── public/                    # Static assets
├── src/
│   ├── App.tsx               # Main React component
│   ├── index.tsx             # Application entry point
│   ├── api/                  # API client modules
│   ├── components/           # React components
│   ├── store/                # Redux store and slices
│   ├── styles/               # CSS and styling
│   ├── types/                # TypeScript type definitions
│   └── utils/                # Utility functions
└── tsconfig.json             # TypeScript configuration
```

### Infrastructure & Configuration
```
├── init-scripts/             # Database initialization SQL
├── monitoring/               # Prometheus/Grafana configuration
├── scripts/                  # Development and deployment scripts
├── logs/                     # Application logs (gitignored)
├── data/                     # Persistent data (gitignored)
└── config/                   # Service-specific configurations
```

### Testing & Quality
```
├── mcp-tests/                # Service integration tests
├── test_probe/               # E2E test evidence collection
├── checkstyle.xml            # Code style configuration
└── docs/                     # Documentation
```

## Package Structure Conventions

### Java Package Naming
```
com.zamaz.mcp.{service}.{layer}
├── config/                   # @Configuration classes
├── controller/               # @RestController classes
├── dto/                      # Data Transfer Objects
├── entity/                   # @Entity JPA classes
├── exception/                # Custom exception classes
├── repository/               # @Repository interfaces
├── service/                  # @Service business logic
└── util/                     # Utility classes
```

### Frontend Structure Conventions
```
src/
├── api/                      # API client services
│   ├── baseClient.ts         # Axios base configuration
│   └── {service}Client.ts    # Service-specific clients
├── components/               # React components
│   ├── {Feature}Page.tsx     # Page-level components
│   └── {Feature}Dialog.tsx   # Modal/dialog components
├── store/slices/             # Redux Toolkit slices
│   └── {feature}Slice.ts     # Feature-specific state
└── types/                    # TypeScript definitions
```

## Configuration Patterns

### Environment-Based Configuration
- **application.yml** - Default configuration
- **application-{profile}.yml** - Environment-specific overrides
- Environment variables override YAML properties

### Database Naming Conventions
- Tables: snake_case (e.g., `debate_participants`)
- Columns: snake_case (e.g., `created_at`, `user_id`)
- Indexes: `idx_{table}_{column}` (e.g., `idx_debates_status`)

### API Endpoint Patterns
```
/api/v1/{resource}              # REST endpoints
/tools/{tool_name}              # MCP tool endpoints
/resources/{resource_name}      # MCP resource endpoints
/actuator/{endpoint}            # Spring Boot Actuator
```

## File Naming Conventions

### Java Classes
- **Controllers**: `{Entity}Controller.java`
- **Services**: `{Entity}Service.java`
- **Repositories**: `{Entity}Repository.java`
- **DTOs**: `{Entity}Request.java`, `{Entity}Response.java`
- **Entities**: `{Entity}.java`

### Frontend Files
- **Components**: `{Feature}Page.tsx`, `{Feature}Dialog.tsx`
- **API Clients**: `{service}Client.ts`
- **Redux Slices**: `{feature}Slice.ts`
- **Types**: `{feature}.types.ts`

## Development Workflow Directories

### Logs and Data (Gitignored)
```
├── logs/                     # Application logs
│   ├── {service}.log         # Service-specific logs
│   └── controller-test.log   # Test execution logs
└── data/                     # Persistent data
    └── debates.db            # SQLite database files
```

### Scripts and Automation
```
scripts/
├── docker-cleanup.sh         # Docker maintenance
├── test-*.sh                 # Testing scripts
├── setup-*.sh                # Environment setup
└── validate-setup.sh         # System validation
```

## Key Architectural Patterns

### Dependency Flow
```
Controller → Service → Repository → Entity
     ↓         ↓          ↓
    DTO ←→ Mapper ←→ Entity
```

### Inter-Service Communication
- REST APIs for synchronous communication
- Redis pub/sub for asynchronous events
- JWT tokens for authentication between services

### Database Per Service
- Each microservice has its own database schema
- Shared database instance with separate schemas
- Flyway migrations per service