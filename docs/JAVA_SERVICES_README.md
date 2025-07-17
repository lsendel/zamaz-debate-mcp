# Java MCP Services

This document provides detailed information about the Java implementations of the MCP services.

## Services Overview

### 1. mcp-organization (Port 5005)
Organization management service with JWT authentication and multi-tenant support.

**Key Features:**
- User authentication and authorization
- Organization CRUD operations
- JWT token generation and validation
- Role-based access control (RBAC)
- Redis caching for sessions
- OpenAPI 3.0 documentation

**API Endpoints:**
```
# Authentication
POST   /api/v1/auth/register         - Register new user
POST   /api/v1/auth/login            - User login
POST   /api/v1/auth/refresh          - Refresh JWT token
POST   /api/v1/auth/logout           - User logout

# Organizations
POST   /api/v1/organizations         - Create organization
GET    /api/v1/organizations         - List organizations
GET    /api/v1/organizations/{id}    - Get organization
PUT    /api/v1/organizations/{id}    - Update organization
DELETE /api/v1/organizations/{id}    - Delete organization

# Organization Users
POST   /api/v1/organizations/{id}/users      - Add user
GET    /api/v1/organizations/{id}/users      - List users
DELETE /api/v1/organizations/{id}/users/{userId} - Remove user

# MCP Tools
POST   /tools/create_organization    - MCP tool endpoint
POST   /tools/get_organization       - MCP tool endpoint
POST   /tools/update_organization    - MCP tool endpoint
POST   /tools/delete_organization    - MCP tool endpoint
GET    /resources/organizations      - MCP resource endpoint

# Health & Monitoring
GET    /actuator/health              - Health check
GET    /actuator/metrics             - Metrics
GET    /actuator/prometheus          - Prometheus metrics
GET    /swagger-ui.html              - Swagger UI
GET    /api-docs                     - OpenAPI specification
```

### 2. mcp-llm (Port 5002)
LLM provider gateway with support for multiple AI providers.

**Key Features:**
- Multi-provider support (Claude, OpenAI, Gemini, Ollama)
- Request/response caching with Redis
- Rate limiting per provider
- Circuit breaker pattern for resilience
- Streaming response support
- Token counting and cost estimation
- Reactive programming with Spring WebFlux

**API Endpoints:**
```
# Completions
POST   /api/v1/completions           - Generate completion
POST   /api/v1/completions/stream    - Streaming completion

# Providers
GET    /api/v1/providers             - List available providers
GET    /api/v1/providers/{name}/status - Provider health
GET    /api/v1/providers/{name}/models - List models

# Embeddings
POST   /api/v1/embeddings            - Generate embeddings

# Usage & Costs
GET    /api/v1/usage                 - Usage statistics
GET    /api/v1/usage/costs           - Cost breakdown

# MCP Tools
POST   /tools/generate_completion    - MCP tool endpoint
POST   /tools/count_tokens           - MCP tool endpoint
GET    /resources/providers          - MCP resource endpoint

# Health & Monitoring
GET    /actuator/health              - Health check
GET    /actuator/metrics             - Metrics
GET    /actuator/prometheus          - Prometheus metrics
GET    /swagger-ui.html              - Swagger UI
GET    /api-docs                     - OpenAPI specification
```

### 3. mcp-controller (Port 5013)
Central orchestration service for debate management.

**Key Features:**
- Debate lifecycle management
- Spring State Machine for workflow
- Participant management (human & AI)
- Round-based debate orchestration
- Automatic AI response generation
- Integration with Organization and LLM services
- Event-driven architecture with Redis pub/sub

**API Endpoints:**
```
# Debates
POST   /api/v1/debates               - Create debate
GET    /api/v1/debates               - List debates
GET    /api/v1/debates/{id}          - Get debate
PUT    /api/v1/debates/{id}          - Update debate
DELETE /api/v1/debates/{id}          - Delete debate
POST   /api/v1/debates/{id}/start    - Start debate

# Participants
POST   /api/v1/debates/{id}/participants - Add participant
DELETE /api/v1/debates/{id}/participants/{participantId} - Remove

# Rounds & Responses
POST   /api/v1/debates/{id}/rounds/{roundId}/responses - Submit response
GET    /api/v1/debates/{id}/rounds   - List rounds
GET    /api/v1/debates/{id}/results  - Get results

# MCP Tools
POST   /tools/create_debate          - MCP tool endpoint
POST   /tools/start_debate           - MCP tool endpoint
POST   /tools/submit_response        - MCP tool endpoint
GET    /resources/debates            - MCP resource endpoint

# Health & Monitoring
GET    /actuator/health              - Health check
GET    /actuator/metrics             - Metrics
GET    /actuator/prometheus          - Prometheus metrics
GET    /swagger-ui.html              - Swagger UI
GET    /api-docs                     - OpenAPI specification
```

## Running the Java Services

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL (via Docker)
- Redis (via Docker)

### Quick Start

1. **Start infrastructure services:**
```bash
docker-compose up -d postgres redis qdrant
```

2. **Start Java services with Docker Compose:**
```bash
docker-compose -f docker-compose.yml -f docker-compose-java.yml up -d
```

3. **Or run services locally:**
```bash
# Terminal 1 - Organization Service
cd mcp-organization
mvn spring-boot:run

# Terminal 2 - LLM Service
cd mcp-llm
mvn spring-boot:run

# Terminal 3 - Controller Service
cd mcp-controller
mvn spring-boot:run
```

### Configuration

Each service uses Spring Boot's configuration system. Key configuration files:

- `application.yml` - Main configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production settings

Environment variables override configuration properties.

### Testing

1. **Run unit tests:**
```bash
cd mcp-organization
mvn test
```

2. **Run integration tests:**
```bash
mvn verify
```

3. **Test all Java services:**
```bash
./mcp-tests/test-java-services.sh
```

### Building Docker Images

```bash
# Build all Java service images
docker-compose -f docker-compose-java.yml build

# Build individual service
cd mcp-organization
docker build -t mcp-organization:latest .
```

### Monitoring

All Java services expose:
- Health endpoints at `/actuator/health`
- Prometheus metrics at `/actuator/prometheus`
- Custom metrics via Micrometer

### API Documentation

Each service provides:
- Swagger UI at `/swagger-ui.html`
- OpenAPI 3.0 spec at `/api-docs`
- JSON schema at `/api-docs.json`

### Security Considerations

1. **JWT Configuration:**
   - Set strong JWT_SECRET in production
   - Tokens expire after 24 hours by default
   - Refresh tokens valid for 7 days

2. **API Keys:**
   - Store securely in environment variables
   - Never commit to version control
   - Rotate regularly

3. **Database Security:**
   - Use strong passwords
   - Enable SSL in production
   - Regular backups

### Performance Tuning

1. **JVM Options:**
```bash
java -Xmx2g -Xms1g -XX:+UseG1GC -jar app.jar
```

2. **Connection Pools:**
   - Hikari CP configured for optimal performance
   - Redis connection pooling enabled
   - HTTP client connection pooling

3. **Caching:**
   - Redis caching for frequently accessed data
   - Spring Cache abstraction
   - TTL configuration per cache type

### Troubleshooting

1. **Service won't start:**
   - Check port availability
   - Verify database connectivity
   - Check logs: `docker logs mcp-organization`

2. **Authentication issues:**
   - Verify JWT_SECRET matches across services
   - Check token expiration
   - Validate CORS configuration

3. **Performance issues:**
   - Monitor JVM heap usage
   - Check database query performance
   - Review connection pool metrics

### Development Tips

1. **Hot Reload:**
   - Add spring-boot-devtools dependency
   - Enable automatic restart

2. **Debugging:**
   - Remote debugging on port 5005 (configurable)
   - Extensive logging with SLF4J/Logback

3. **Testing with Postman:**
   - Import OpenAPI spec
   - Use environment variables for auth tokens
   - Collection available in `/docs/postman`