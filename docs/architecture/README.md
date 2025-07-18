# Architecture Documentation

## Overview

The MCP Debate System follows a microservices architecture with clear separation of concerns and domain-driven design principles.

## Table of Contents

1. [System Architecture](ARCHITECTURE.md)
2. [Multi-Tenant Design](ARCHITECTURE_MULTI_TENANT.md)
3. [MCP Integration](MCP_CLAUDE_INTEGRATION.md)
4. [Service Communication](ARCHITECTURE_DUAL_MCP.md)
5. [Context Management](CONTEXT_STRATEGY.md)

## Architecture Principles

### 1. Domain-Driven Design
- Clear bounded contexts
- Aggregate roots
- Domain events
- Value objects

### 2. Microservices
- Service autonomy
- API-first design
- Event-driven communication
- Service discovery

### 3. Security by Design
- Zero-trust networking
- JWT authentication
- Role-based access control
- End-to-end encryption

### 4. Scalability
- Horizontal scaling
- Database sharding
- Cache layers
- Load balancing

## Service Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   UI (React)    │────▶│   MCP Gateway   │────▶│ Load Balancer   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                │                          │
                    ┌───────────┴───────────┬─────────────┴─────────┐
                    │                       │                       │
              ┌─────▼─────┐          ┌─────▼─────┐          ┌─────▼─────┐
              │Organization│          │  Debate   │          │    LLM    │
              │  Service   │          │  Engine   │          │  Gateway  │
              └───────────┘          └───────────┘          └───────────┘
                    │                       │                       │
              ┌─────▼─────┐          ┌─────▼─────┐          ┌─────▼─────┐
              │  Security │          │    RAG    │          │  Pattern  │
              │  Service  │          │  Service  │          │Recognition│
              └───────────┘          └───────────┘          └───────────┘
                    │                       │                       │
              ┌─────▼─────┐          ┌─────▼─────┐          ┌─────▼─────┐
              │  GitHub   │          │PostgreSQL │          │   Redis   │
              │Integration│          └───────────┘          └───────────┘
              └───────────┘                    │
                                     ┌─────────▼─────────┐
                                     │   AI Providers    │
                                     └─────────────────┘
```

**Note on Legacy Modules:** The project currently includes several legacy modules (e.g., `mcp-context`, `mcp-controller`, `mcp-debate`, `mcp-template`, `mcp-context-client`, `mcp-modulith`) that are in the process of being phased out or refactored. The diagram above focuses on the active and strategic services.

## Data Flow

### Debate Creation Flow
1. User creates debate through UI
2. Request validated at API Gateway
3. Organization service checks permissions
4. Debate Engine creates debate record
5. Context initialized in Redis
6. WebSocket connection established
7. AI participants notified

### Message Processing Flow
1. AI provider generates response
2. LLM Gateway receives message
3. Message validated and sanitized
4. Debate Engine updates state
5. Message broadcast via WebSocket
6. UI updates in real-time

## Technology Stack

### Backend
- **Java 21**: Core language
- **Spring Boot 3.3.6**: Framework
- **Spring Cloud Gateway**: API Gateway
- **Spring WebFlux**: Reactive programming
- **Spring Security**: Authentication/Authorization
- **Spring AI**: AI integration
- **Spring Modulith**: Modular application design
- **Lombok**: Boilerplate code reduction
- **MapStruct**: Object mapping
- **Resilience4j**: Fault tolerance and resilience
- **Bucket4j**: Rate limiting

### Frontend
- **React**: JavaScript library for building user interfaces
- **TypeScript**: Superset of JavaScript for type safety
- **Vite**: Fast frontend build tool

### API Documentation
- **Springdoc OpenAPI**: API documentation generation

### Data Storage
- **PostgreSQL**: Primary relational database
- **Redis**: Caching, session management, and message broker
- **Qdrant**: Vector database for RAG (Retrieval Augmented Generation)

### Testing
- **JUnit 5**: Unit and integration testing framework
- **Mockito**: Mocking framework for Java
- **Testcontainers**: Lightweight, throwaway containers for tests
- **Rest-Assured**: REST API testing

### Infrastructure
- **Docker**: Containerization
- **Kubernetes**: Container orchestration
- **Prometheus**: Monitoring and alerting
- **Grafana**: Data visualization and dashboards
- **Jaeger**: Distributed tracing
- **Loki**: Log aggregation
- **Promtail**: Log collector for Loki

## Design Patterns

### 1. Circuit Breaker
Prevents cascading failures when services are unavailable.

### 2. Saga Pattern
Manages distributed transactions across services.

### 3. Event Sourcing
Stores debate history as a sequence of events.

### 4. CQRS
Separates read and write operations for performance.

## Performance Considerations

### Caching Strategy
- **L1 Cache**: In-memory (Caffeine)
- **L2 Cache**: Distributed (Redis)
- **HTTP Cache**: CDN for static assets

### Database Optimization
- Connection pooling
- Query optimization
- Index strategies
- Read replicas

### Async Processing
- Message queues
- Event streaming
- Batch processing
- Worker pools

## Security Architecture

### Authentication Flow
1. User provides credentials
2. Gateway validates against organization service
3. JWT token issued
4. Token validated on each request

### Authorization
- Role-based (RBAC)
- Organization-scoped
- Resource-level permissions
- API rate limiting

## Monitoring and Observability

### Metrics
- Business metrics (debates/hour, AI response times)
- Technical metrics (latency, error rates)
- Resource metrics (CPU, memory, disk)

### Logging
- Structured logging (JSON)
- Correlation IDs
- Log aggregation (ELK stack)
- Alert rules

### Tracing
- Request tracing across services
- Performance bottleneck identification
- Error tracking
- Dependency mapping

## Future Enhancements

1. **Service Mesh**: Istio integration for advanced traffic management
2. **GraphQL**: Alternative API for flexible queries
3. **Event Streaming**: Kafka for high-volume event processing
4. **ML Pipeline**: Custom model training and deployment
5. **Multi-Region**: Global deployment with data sovereignty