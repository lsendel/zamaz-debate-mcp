# System Architecture: Zamaz Debate MCP Microservices

## Overview

The Zamaz Debate MCP System is a comprehensive microservices-based application designed for managing structured debates, integrating with various LLM providers, and providing a rich user interface. It adheres to domain-driven design principles, emphasizing clear separation of concerns, scalability, and security.

## Core Components & Services

The system is composed of several independent, deployable microservices, primarily built with Java and Spring Boot, and a modern React frontend.

### Backend Microservices (Java/Spring Boot)
*   **mcp-gateway**: The API Gateway, serving as the single entry point for all client requests, handling routing, authentication, and rate limiting.
*   **mcp-organization**: Manages organizational data, user authentication, and authorization.
*   **mcp-security**: Provides core security functionalities, potentially including JWT management, access control, and secure communication.
*   **mcp-debate-engine**: The central service for managing debate sessions, rules, state, and participant interactions.
*   **mcp-llm**: Integrates with various Large Language Model (LLM) providers (e.g., OpenAI, Gemini, Claude) to facilitate AI participant responses.
*   **mcp-rag**: Implements Retrieval Augmented Generation (RAG) capabilities, likely for fetching and processing relevant information for LLMs.
*   **mcp-pattern-recognition**: Potentially analyzes debate patterns, participant behavior, or LLM responses for insights.
*   **github-integration**: Handles integration with GitHub, possibly for CI/CD triggers, code analysis, or other development-related workflows.
*   **mcp-common**: A shared library or module containing common utilities, data models, and configurations used across multiple microservices.
*   **load-tests / performance-tests/gatling**: Modules dedicated to performance and load testing the system.

**Note on Legacy Modules:** The project currently includes several legacy modules (`mcp-context`, `mcp-controller`, `mcp-debate`, `mcp-template`, `mcp-context-client`, `mcp-modulith`) that are in the process of being phased out or refactored. The services listed above represent the active and strategic components.

### Frontend Application
*   **debate-ui**: A modern web application built with React and TypeScript, providing the user interface for interacting with the debate system.

## Architecture Principles

### 1. Domain-Driven Design
- Clear bounded contexts for each microservice.
- Emphasis on aggregate roots and domain events.

### 2. Microservices
- Service autonomy with independent deployment and scaling.
- API-first design for clear contracts.
- Event-driven communication for loose coupling.
- Service discovery for dynamic service location.

### 3. Security by Design
- Zero-trust networking principles.
- JWT-based authentication and Role-Based Access Control (RBAC).
- End-to-end encryption where applicable.

### 4. Scalability
- Horizontal scaling of stateless services.
- Database sharding and read replicas.
- Multi-layered caching.
- Load balancing across service instances.

## Data Flow

### Debate Creation Flow
1. User initiates debate via **UI (React)**.
2. Request is routed through **API Gateway (mcp-gateway)**.
3. **Organization Service (mcp-organization)** validates user permissions.
4. **Debate Engine (mcp-debate-engine)** creates debate record in PostgreSQL.
5. Context is initialized in Redis.
6. WebSocket connection established for real-time updates.
7. AI participants are notified (potentially via Kafka events).

### Message Processing Flow
1. AI provider generates response (via **LLM Gateway**).
2. **LLM Gateway (mcp-llm)** receives and validates message.
3. **Debate Engine (mcp-debate-engine)** updates debate state.
4. Message is broadcast via WebSocket to **UI (React)**.
5. UI updates in real-time.

## Technology Stack

### Backend
- **Core Language**: Java 21
- **Framework**: Spring Boot 3.3.6
- **API Gateway**: Spring Cloud Gateway
- **Reactive Programming**: Spring WebFlux
- **Security**: Spring Security, JJWT (JSON Web Token)
- **AI Integration**: Spring AI
- **Modular Design**: Spring Modulith
- **Utilities**: Lombok (boilerplate reduction), MapStruct (object mapping)
- **API Documentation**: Springdoc OpenAPI
- **Resilience**: Resilience4j (Circuit Breaker, Rate Limiter), Bucket4j (Rate Limiting)

### Frontend
- **Framework**: React
- **Language**: TypeScript
- **Build Tool**: Vite
- **UI Components**: Material-UI (MUI)
- **State Management**: Redux Toolkit

### Data Storage
- **Primary Database**: PostgreSQL
- **Caching & Sessions**: Redis
- **Vector Database**: Qdrant (for RAG capabilities)

### Testing
- **Unit/Integration**: JUnit 5, Mockito
- **Containerized Tests**: Testcontainers
- **API Testing**: Rest-Assured

### Infrastructure & Observability
- **Containerization**: Docker
- **Orchestration**: Kubernetes
- **Metrics**: Prometheus
- **Visualization**: Grafana
- **Distributed Tracing**: Jaeger
- **Log Aggregation**: ELK Stack (Elasticsearch, Logstash, Kibana) or Loki/Promtail

## Design Patterns

### 1. Circuit Breaker
Prevents cascading failures when downstream services are unavailable.

### 2. Saga Pattern
Manages distributed transactions across multiple services.

### 3. Event Sourcing
Stores debate history as a sequence of immutable events.

### 4. CQRS (Command Query Responsibility Segregation)
Separates read and write operations for optimized performance and scalability.

## Performance Considerations

### Caching Strategy
- **L1 Cache**: In-memory (e.g., Caffeine)
- **L2 Cache**: Distributed (Redis)
- **HTTP Cache**: CDN for static assets.

### Database Optimization
- Connection pooling, query optimization, indexing, read replicas.

### Async Processing
- Message queues, event streaming, batch processing, worker pools.

## Security Architecture

### Authentication Flow
1. User provides credentials to **API Gateway**.
2. **API Gateway** validates credentials against **Organization Service**.
3. JWT token is issued upon successful authentication.
4. Token is validated on each subsequent request.

### Authorization
- Role-Based Access Control (RBAC).
- Organization-scoped and resource-level permissions.
- API rate limiting to prevent abuse.

## Monitoring and Observability

### Metrics
- Business metrics (e.g., debates/hour, AI response times).
- Technical metrics (e.g., latency, error rates, resource utilization).

### Logging
- Structured logging with correlation IDs.
- Centralized log aggregation for analysis and alerting.

### Tracing
- End-to-end request tracing across all microservices.
- Identification of performance bottlenecks and error propagation.

## Future Enhancements

1. **Service Mesh**: Istio integration for advanced traffic management, resilience, and security.
2. **Event Streaming**: Deeper integration with Kafka for high-volume event processing and real-time data pipelines.
3. **MLOps Pipeline**: Formalized pipeline for custom model training, versioning, and deployment.
4. **Serverless Workloads**: Migration of specific, event-driven tasks to FaaS for cost and operational efficiency.
5. **Multi-Region Deployment**: Global deployment strategy for high availability and disaster recovery.

## Contribution Guidelines

1. Adhere to architectural principles and established patterns.
2. Document all changes thoroughly.
3. Write comprehensive unit, integration, and end-to-end tests.
4. Participate actively in code reviews.
5. Prioritize security best practices in all development phases.

## Contact

For architectural questions or further details, please contact the core development team.
