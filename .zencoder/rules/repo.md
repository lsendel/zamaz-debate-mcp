---
description: Repository Information Overview
alwaysApply: true
---

# Repository Information Overview

## Repository Summary
The Zamaz Debate MCP (Model Context Protocol) is a comprehensive system for managing AI-powered debates with enterprise-grade multi-tenant support, real-time processing, and advanced monitoring. It follows a microservices architecture with consolidated services for better performance.

## Repository Structure
- **mcp-*** - Java microservices (organization, llm, debate-engine, rag, security, etc.)
- **debate-ui** - React-based frontend application
- **e2e-tests** - End-to-end testing with Playwright
- **infrastructure** - Docker Compose and Kubernetes configurations
- **scripts** - Utility scripts for development, testing, and deployment
- **docs** - Comprehensive documentation
- **performance-tests** - Load and performance testing tools
- **monitoring** - Monitoring configuration (Prometheus, Grafana)
- **security-runbooks** - Security incident response procedures

### Main Repository Components
- **Backend Services**: Java-based microservices using Spring Boot
- **Frontend**: React application with TypeScript
- **Testing**: E2E tests with Playwright, performance tests with Gatling
- **Infrastructure**: Docker Compose for local development, Kubernetes for production
- **Monitoring**: Prometheus, Grafana, Jaeger for distributed tracing

## Projects

### Java Microservices
**Configuration File**: pom.xml

#### Language & Runtime
**Language**: Java
**Version**: Java 21
**Build System**: Maven
**Package Manager**: Maven

#### Dependencies
**Main Dependencies**:
- Spring Boot 3.3.6
- Spring Cloud 2023.0.4
- Spring Modulith 1.2.0
- Spring AI 0.8.1
- PostgreSQL 42.7.7
- Resilience4j 2.2.0
- Bucket4j 8.10.1
- Spring Security
- JJWT 0.12.6

**Development Dependencies**:
- JUnit Jupiter 5.11.3
- Mockito 5.15.2
- Testcontainers 1.20.4
- H2 Database 2.3.232

#### Build & Installation
```bash
# Build all services
mvn clean install

# Run a specific service
cd mcp-service-name
mvn spring-boot:run
```

#### Testing
**Framework**: JUnit Jupiter, Mockito
**Test Location**: src/test/java
**Configuration**: Maven Surefire Plugin
**Run Command**:
```bash
mvn test
```

### Frontend (debate-ui)
**Configuration File**: package.json

#### Language & Runtime
**Language**: TypeScript
**Version**: TypeScript 4.9.5
**Build System**: Vite 7.0.5
**Package Manager**: npm

#### Dependencies
**Main Dependencies**:
- React 19.1.0
- Redux Toolkit 2.8.2
- Material UI 7.2.0
- Axios 1.7.9
- React Router 7.7.0
- Socket.io-client 4.8.1

**Development Dependencies**:
- ESLint 8.57.0
- Prettier 3.2.5
- TypeScript 4.9.5
- Vite 7.0.5
- Testing Library 16.1.0

#### Build & Installation
```bash
cd debate-ui
npm install
npm run build
```

#### Testing
**Framework**: Jest, Testing Library
**Test Location**: src/__tests__
**Run Command**:
```bash
cd debate-ui
npm test
```

### E2E Tests
**Configuration File**: playwright.config.ts

#### Language & Runtime
**Language**: TypeScript
**Framework**: Playwright
**Package Manager**: npm

#### Dependencies
**Main Dependencies**:
- Playwright
- dotenv

#### Build & Installation
```bash
cd e2e-tests
npm install
```

#### Testing
**Run Command**:
```bash
cd e2e-tests
npm test
```

### Performance Tests
**Configuration File**: pom.xml (gatling)

#### Language & Runtime
**Language**: Scala (Gatling), Python (custom tools)
**Framework**: Gatling

#### Build & Installation
```bash
cd performance-tests
mvn clean install
```

#### Testing
**Run Command**:
```bash
cd performance-tests
./run-performance-tests.sh
```

## Docker
**Configuration**: Docker Compose in infrastructure/docker-compose
**Services**:
- PostgreSQL
- Redis
- Qdrant (Vector DB)
- Prometheus
- Grafana
- Jaeger
- Loki
- Ollama (optional)

**Run Command**:
```bash
# Start all services
docker-compose -f infrastructure/docker-compose/docker-compose.yml up -d

# With monitoring
docker-compose -f infrastructure/docker-compose/docker-compose.yml --profile monitoring up -d
```

## Security
**Framework**: Spring Security
**Authentication**: JWT-based authentication
**Key Features**:
- Multi-tenant data isolation
- Per-organization rate limiting
- Comprehensive audit logging
- API key management
- Role-based access control

**Security Runbooks**:
- Authentication failures
- Suspicious activity detection
- Data breach response
- DDoS mitigation
- Insider threat detection