# Development Guide

## Overview

This guide covers everything you need to know to develop, test, and contribute to the MCP Debate System.

## Table of Contents

1. [Environment Setup](setup.md)
2. [Development Workflow](#development-workflow)
3. [Coding Standards](coding-standards.md)
4. [Testing Guide](testing.md)
5. [Debugging Tips](#debugging-tips)
6. [Contributing](contributing.md)

## Quick Start

### Prerequisites

```bash
# Required software
java -version  # 17+
node --version # 18+
docker --version
docker-compose --version

# Install dependencies
make setup
```

### Running Locally

```bash
# Start all services
make start-all

# Start backend only
make start

# Start UI only
make ui

# Run tests
make test
```

## Development Workflow

### 1. Branch Strategy

We follow GitHub Flow:

```bash
# Create feature branch
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit -m "feat: add new feature"

# Push and create PR
git push -u origin feature/your-feature-name
```

### 2. Commit Convention

We use Conventional Commits:

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation
- `style:` Code style
- `refactor:` Code refactoring
- `test:` Tests
- `chore:` Maintenance

### 3. Code Review Process

1. Create pull request
2. Automated checks run
3. Peer review required
4. Merge after approval

## Project Structure

```
zamaz-debate-mcp/
├── mcp-organization/      # Organization management
├── mcp-gateway/          # API Gateway
├── mcp-debate-engine/    # Core debate logic
├── mcp-llm/             # LLM integrations
├── mcp-rag/             # RAG service
├── debate-ui/           # React frontend
├── docker/              # Docker configs
├── k8s/                 # Kubernetes manifests
├── performance-tests/   # Load tests
└── docs/               # Documentation
```

## Development Tools

### IDE Setup

#### IntelliJ IDEA
1. Import as Maven project
2. Enable annotation processing
3. Install Lombok plugin
4. Configure code style

#### VS Code
1. Install Java Extension Pack
2. Install Spring Boot Extension
3. Configure settings.json

### Useful Commands

```bash
# Build all services
make build

# Run specific service
cd mcp-debate-engine
mvn spring-boot:run

# Run with profile
mvn spring-boot:run -Dspring.profiles.active=dev

# Format code
mvn spotless:apply

# Check code quality
mvn verify -P code-quality
```

## Debugging Tips

### 1. Enable Debug Logging

```yaml
logging:
  level:
    com.zamaz.mcp: DEBUG
    org.springframework.web: DEBUG
```

### 2. Remote Debugging

```bash
# Start with debug port
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar
```

### 3. Docker Debugging

```bash
# View logs
docker-compose logs -f service-name

# Execute commands in container
docker-compose exec service-name bash

# Inspect container
docker inspect container-id
```

## Database Management

### Migrations

```bash
# Create new migration
mvn flyway:generate -Dflyway.description=add_new_table

# Run migrations
mvn flyway:migrate

# Check status
mvn flyway:info
```

### Local Database Access

```bash
# Connect to PostgreSQL
psql -h localhost -p 5432 -U postgres -d debate_db

# Connect to Redis
redis-cli -h localhost -p 6379
```

## API Development

### Adding New Endpoints

1. Define in controller:
```java
@RestController
@RequestMapping("/api/v1/resource")
public class ResourceController {
    @PostMapping
    public ResponseEntity<ResourceDTO> create(@Valid @RequestBody CreateResourceRequest request) {
        // Implementation
    }
}
```

2. Add service logic:
```java
@Service
@Transactional
public class ResourceService {
    public ResourceDTO create(CreateResourceRequest request) {
        // Business logic
    }
}
```

3. Update API documentation
4. Write tests

### API Testing

```bash
# Run API tests
cd docs/api
./run-api-tests.sh

# Test specific endpoint
curl -X GET http://localhost:8080/api/v1/health
```

## Frontend Development

### Setup

```bash
cd debate-ui
npm install
npm run dev
```

### Component Structure

```
src/
├── components/      # Reusable components
├── pages/          # Page components
├── hooks/          # Custom hooks
├── services/       # API services
├── store/          # Redux store
└── utils/          # Utilities
```

### Testing

```bash
# Run tests
npm test

# Run with coverage
npm run test:coverage

# E2E tests
npm run test:e2e
```

## Performance Considerations

### 1. Database Queries
- Use pagination
- Add appropriate indexes
- Avoid N+1 queries
- Use projections

### 2. Caching
- Cache frequently accessed data
- Set appropriate TTLs
- Use cache-aside pattern
- Monitor cache hit rates

### 3. Async Processing
- Use async endpoints for long operations
- Implement proper timeouts
- Handle failures gracefully

## Security Best Practices

### 1. Input Validation
```java
@Valid
@NotNull
@Size(min = 1, max = 100)
private String input;
```

### 2. SQL Injection Prevention
```java
// Use parameterized queries
@Query("SELECT d FROM Debate d WHERE d.organizationId = :orgId")
List<Debate> findByOrganizationId(@Param("orgId") String orgId);
```

### 3. Authentication
```java
@PreAuthorize("hasRole('USER') and #orgId == authentication.organizationId")
public void secureMethod(String orgId) {
    // Implementation
}
```

## Monitoring and Logging

### Structured Logging

```java
@Slf4j
public class Service {
    public void process(String id) {
        log.info("Processing started", 
            kv("debateId", id),
            kv("timestamp", Instant.now()));
    }
}
```

### Metrics

```java
@Component
public class Metrics {
    private final MeterRegistry registry;
    
    public void recordDebateCreation() {
        registry.counter("debate.created").increment();
    }
}
```

## Troubleshooting

### Common Issues

1. **Port already in use**
   ```bash
   lsof -i :8080
   kill -9 <PID>
   ```

2. **Database connection failed**
   - Check Docker containers: `docker-compose ps`
   - Verify credentials in `.env`

3. **Maven build failures**
   ```bash
   mvn clean install -U
   rm -rf ~/.m2/repository
   ```

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [Docker Documentation](https://docs.docker.com)
- [Kubernetes Documentation](https://kubernetes.io/docs)