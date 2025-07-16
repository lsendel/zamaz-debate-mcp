# Technology Stack

## Build System
- **Maven** multi-module project with parent POM
- **Java 21** as target version
- **Spring Boot 3.3.5** with Spring Framework ecosystem
- **Docker** and **Docker Compose** for containerization

## Backend Technologies

### Core Framework
- **Spring Boot 3.3.5** - Main application framework
- **Spring WebFlux** - Reactive web framework (for LLM service)
- **Spring MVC** - Traditional web framework (for other services)
- **Spring Data JPA** - Database abstraction
- **Spring Security** - Authentication and authorization
- **Spring AI 1.0.0-M3** - LLM integration framework

### Database & Storage
- **PostgreSQL 16** - Primary database
- **Redis 7** - Caching and session storage
- **Qdrant** - Vector database for RAG
- **Flyway** - Database migrations

### Security & Authentication
- **JWT (JJWT 0.12.6)** - Token-based authentication
- **Spring Security** - Security framework
- **BCrypt** - Password hashing

### Monitoring & Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **Prometheus** - Metrics storage
- **Grafana** - Visualization (optional)

### Utilities
- **Lombok 1.18.30** - Boilerplate code reduction
- **MapStruct 1.6.3** - Object mapping
- **Jackson 2.18.0** - JSON processing
- **SpringDoc OpenAPI 2.6.0** - API documentation

### Testing
- **JUnit 5** - Unit testing
- **Mockito** - Mocking framework
- **TestContainers** - Integration testing
- **REST Assured** - API testing

## Frontend Technologies
- **React 18.2.0** - UI framework
- **TypeScript 4.9.5** - Type safety
- **Material-UI 5.14.20** - Component library
- **Redux Toolkit 1.9.7** - State management
- **React Router 6.20.1** - Navigation
- **Axios 1.6.2** - HTTP client
- **Socket.io** - WebSocket communication

## Common Build Commands

### Maven Commands
```bash
# Build all modules
mvn clean install

# Run specific service
cd mcp-organization && mvn spring-boot:run

# Run tests
mvn test

# Run integration tests
mvn verify

# Build with code quality checks
mvn clean install -P code-quality
```

### Docker Commands
```bash
# Start all services
docker-compose up -d

# Build and start
docker-compose up -d --build

# Start with specific profile
docker-compose --profile monitoring up -d

# View logs
docker-compose logs -f [service-name]
```

### Makefile Commands
```bash
# First time setup
make setup

# Start all services
make start-all

# Start UI only
make start-ui

# Run tests
make test-mcp-all

# Check service health
make check-health

# Clean up
make clean
```

### Frontend Commands
```bash
cd debate-ui

# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Run tests
npm test
```

## Code Quality Tools
- **Checkstyle** - Code style enforcement
- **SpotBugs** - Static analysis
- **JaCoCo** - Code coverage
- **SonarQube** - Code quality analysis (configurable)

## Development Profiles
- **default** - Local development
- **test** - Testing environment
- **prod** - Production environment
- **monitoring** - With Prometheus/Grafana
- **llama** - With Ollama support