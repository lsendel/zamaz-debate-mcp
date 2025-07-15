# MCP Modulith

A Spring Modulith-based modular monolith implementation of the MCP system. This demonstrates how to build a well-structured monolithic application with clear module boundaries that can later be decomposed into microservices if needed.

## Architecture Overview

The application is structured as a modular monolith using Spring Modulith, which provides:
- Clear module boundaries
- Event-driven communication between modules
- Module isolation and dependency management
- Built-in observability and documentation

## Modules

### 1. Organization Module (`com.zamaz.mcp.modulith.organization`)
- Manages multi-tenant organizations
- Handles subscription tiers and features
- Publishes domain events when organizations are created/updated

### 2. Debate Module (`com.zamaz.mcp.modulith.debate`)
- Manages debate lifecycle
- Orchestrates participant interactions
- Depends on Organization and LLM modules
- Listens to organization events

### 3. LLM Module (`com.zamaz.mcp.modulith.llm`)
- Provides unified interface to LLM providers
- Abstracts provider-specific implementations
- Manages provider registry

### 4. Shared Module (`com.zamaz.mcp.modulith.shared`)
- Contains cross-cutting concerns
- Domain events
- Common utilities

## Key Features

### Event-Driven Architecture
- Modules communicate through domain events
- Guaranteed event delivery with publication registry
- Example: When an organization is created, the debate module automatically creates a welcome debate

### Module Dependencies
- Strictly controlled through `@ApplicationModule` annotations
- Clear dependency graph prevents circular dependencies
- Example: Debate module can access Organization service but not vice versa

### REST API
- `/api/organizations` - Organization management
- `/api/debates` - Debate operations

## Getting Started

### Prerequisites
- Java 21
- PostgreSQL
- Maven

### Database Setup
```sql
CREATE DATABASE mcp_modulith;
CREATE USER mcp_user WITH PASSWORD 'mcp_pass';
GRANT ALL PRIVILEGES ON DATABASE mcp_modulith TO mcp_user;
```

### Running the Application
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Or with JAR
java -jar target/mcp-modulith-1.0.0.jar
```

### Configuration
Edit `src/main/resources/application.yml` to configure:
- Database connection
- LLM provider API keys
- Module-specific settings

## Testing

### Architecture Tests
```bash
# Verify module structure
mvn test -Dtest=ModulithArchitectureTest#verifyModularStructure

# Generate module documentation
mvn test -Dtest=ModulithArchitectureTest#createModuleDocumentation
```

### Integration Tests
```bash
# Run all tests
mvn test

# Run specific module tests
mvn test -Dtest=OrganizationServiceTest
mvn test -Dtest=DebateServiceTest
```

## Module Documentation

After running the architecture tests, documentation is generated in:
- `target/spring-modulith-docs/` - Module diagrams and documentation

## Migration Path

This modular monolith can be decomposed into microservices:

1. **Extract Modules**: Each module can become a separate service
2. **Replace Events**: Domain events become message queue events
3. **Add Service Discovery**: Replace direct calls with HTTP/gRPC
4. **Separate Databases**: Each service gets its own database

## Benefits of Modulith Approach

1. **Simpler Operations**: Single deployment unit
2. **Lower Latency**: In-process communication
3. **Easier Development**: All code in one place
4. **Clear Boundaries**: Enforced module isolation
5. **Future-Proof**: Can evolve to microservices

## Monitoring

Spring Modulith provides built-in observability:
- Module interaction metrics
- Event publication tracking
- Performance monitoring

## Best Practices

1. **Keep Modules Independent**: Minimize inter-module dependencies
2. **Use Events**: Prefer events over direct module calls
3. **Test Module Boundaries**: Use architecture tests
4. **Document Public APIs**: Clear module interfaces
5. **Version Events**: Plan for event evolution