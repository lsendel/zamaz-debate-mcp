# Java Spring Boot MCP Services Implementation Plan

## Overview
This document outlines the comprehensive plan for implementing Java Spring Boot versions of the MCP services, maintaining feature parity with the Python implementations while leveraging Java's strengths.

## 1. mcp-organization

### Purpose
Organization management service for multi-tenant support with JWT authentication and role-based access control.

### Technology Stack
- **Framework**: Spring Boot 3.2.x
- **Build Tool**: Maven
- **Java Version**: 17 (LTS)
- **Database**: PostgreSQL
- **Cache**: Redis (Spring Data Redis)
- **Security**: Spring Security + JWT
- **API Documentation**: SpringDoc OpenAPI 3.0
- **Monitoring**: Micrometer + Prometheus

### Key Features
1. **Organization Management**
   - Create, read, update, delete organizations
   - Organization metadata and settings
   - Multi-tenant data isolation

2. **User Management**
   - User registration and authentication
   - Role-based access control (RBAC)
   - JWT token generation and validation
   - Password reset functionality

3. **API Endpoints**
   ```
   POST   /api/v1/organizations                 - Create organization
   GET    /api/v1/organizations                 - List organizations
   GET    /api/v1/organizations/{id}            - Get organization details
   PUT    /api/v1/organizations/{id}            - Update organization
   DELETE /api/v1/organizations/{id}            - Delete organization
   
   POST   /api/v1/organizations/{id}/users      - Add user to organization
   GET    /api/v1/organizations/{id}/users      - List organization users
   DELETE /api/v1/organizations/{id}/users/{userId} - Remove user
   
   POST   /api/v1/auth/register                 - Register new user
   POST   /api/v1/auth/login                    - User login
   POST   /api/v1/auth/refresh                  - Refresh JWT token
   POST   /api/v1/auth/logout                   - User logout
   
   GET    /actuator/health                      - Health check
   GET    /actuator/metrics                     - Metrics endpoint
   ```

4. **MCP Protocol Support**
   ```
   POST   /tools/create_organization
   POST   /tools/get_organization
   POST   /tools/update_organization
   POST   /tools/delete_organization
   POST   /tools/add_user_to_organization
   POST   /tools/remove_user_from_organization
   GET    /resources/organizations
   ```

### Database Schema
```sql
-- Organizations table
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Organization users mapping
CREATE TABLE organization_users (
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'member',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (organization_id, user_id)
);
```

## 2. mcp-llm

### Purpose
LLM provider gateway service supporting multiple AI providers with unified interface, caching, and rate limiting.

### Technology Stack
- **Framework**: Spring Boot 3.2.x with WebFlux (Reactive)
- **Build Tool**: Maven
- **Java Version**: 17 (LTS)
- **HTTP Client**: Spring WebClient
- **Cache**: Redis (Spring Data Redis Reactive)
- **Rate Limiting**: Bucket4j
- **Circuit Breaker**: Resilience4j

### Key Features
1. **Provider Support**
   - Claude (Anthropic)
   - OpenAI (GPT-3.5, GPT-4)
   - Google Gemini
   - Ollama (Local LLMs)

2. **Core Functionality**
   - Unified completion API
   - Provider-specific adapters
   - Response streaming support
   - Token counting and limits
   - Cost tracking
   - Request/response caching
   - Rate limiting per provider
   - Circuit breaker pattern
   - Retry with exponential backoff

3. **API Endpoints**
   ```
   POST   /api/v1/completions                   - Generate completion
   POST   /api/v1/completions/stream            - Streaming completion
   GET    /api/v1/providers                     - List available providers
   GET    /api/v1/providers/{name}/status       - Provider health status
   GET    /api/v1/providers/{name}/models       - List provider models
   
   POST   /api/v1/embeddings                    - Generate embeddings
   GET    /api/v1/usage                         - Get usage statistics
   GET    /api/v1/usage/costs                   - Get cost breakdown
   
   GET    /actuator/health                      - Health check
   GET    /actuator/metrics                     - Metrics endpoint
   ```

4. **MCP Protocol Support**
   ```
   POST   /tools/generate_completion
   POST   /tools/generate_streaming_completion
   POST   /tools/generate_embedding
   POST   /tools/count_tokens
   GET    /resources/providers
   GET    /resources/models
   ```

### Configuration Structure
```yaml
llm:
  providers:
    claude:
      enabled: true
      api-key: ${CLAUDE_API_KEY}
      base-url: https://api.anthropic.com
      default-model: claude-3-opus-20240229
      max-tokens: 4096
      timeout: 30s
      retry:
        max-attempts: 3
        initial-interval: 1s
    openai:
      enabled: true
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
      default-model: gpt-4-turbo-preview
      max-tokens: 4096
    gemini:
      enabled: true
      api-key: ${GEMINI_API_KEY}
      base-url: https://generativelanguage.googleapis.com
      default-model: gemini-pro
    ollama:
      enabled: false
      base-url: http://localhost:11434
      default-model: llama2
  cache:
    enabled: true
    ttl: 1h
    max-size: 1000
  rate-limiting:
    enabled: true
    default-requests-per-minute: 60
```

## 3. mcp-controller

### Purpose
Central orchestration service for managing debate workflows, coordinating between services, and handling business logic.

### Technology Stack
- **Framework**: Spring Boot 3.2.x
- **Build Tool**: Maven
- **Java Version**: 17 (LTS)
- **Workflow**: Spring State Machine
- **Messaging**: Spring Integration
- **Database**: PostgreSQL
- **Event Bus**: Redis Pub/Sub

### Key Features
1. **Debate Management**
   - Create and manage debates
   - Participant coordination
   - Round management
   - State transitions
   - Result calculation

2. **Service Orchestration**
   - Coordinate with mcp-organization
   - Manage LLM requests via mcp-llm
   - Handle context management
   - Implement debate strategies

3. **Workflow States**
   ```
   CREATED -> INITIALIZED -> IN_PROGRESS -> ROUND_COMPLETE -> DEBATE_COMPLETE -> ARCHIVED
   ```

4. **API Endpoints**
   ```
   POST   /api/v1/debates                       - Create debate
   GET    /api/v1/debates                       - List debates
   GET    /api/v1/debates/{id}                  - Get debate details
   PUT    /api/v1/debates/{id}                  - Update debate
   DELETE /api/v1/debates/{id}                  - Delete debate
   
   POST   /api/v1/debates/{id}/start            - Start debate
   POST   /api/v1/debates/{id}/rounds           - Create new round
   POST   /api/v1/debates/{id}/rounds/{roundId}/responses - Submit response
   GET    /api/v1/debates/{id}/rounds           - List rounds
   GET    /api/v1/debates/{id}/results          - Get debate results
   
   POST   /api/v1/debates/{id}/participants     - Add participant
   DELETE /api/v1/debates/{id}/participants/{participantId} - Remove participant
   
   GET    /actuator/health                      - Health check
   GET    /actuator/metrics                     - Metrics endpoint
   ```

5. **MCP Protocol Support**
   ```
   POST   /tools/create_debate
   POST   /tools/start_debate
   POST   /tools/submit_response
   POST   /tools/end_debate
   POST   /tools/get_debate_status
   GET    /resources/debates
   GET    /resources/debate_strategies
   ```

### Database Schema
```sql
-- Debates table
CREATE TABLE debates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    topic TEXT NOT NULL,
    format VARCHAR(50) NOT NULL,
    max_rounds INTEGER DEFAULT 3,
    current_round INTEGER DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- Participants table
CREATE TABLE participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debate_id UUID REFERENCES debates(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'human' or 'ai'
    provider VARCHAR(50), -- for AI participants
    model VARCHAR(100), -- for AI participants
    position VARCHAR(50), -- 'for' or 'against'
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Rounds table
CREATE TABLE rounds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debate_id UUID REFERENCES debates(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    UNIQUE(debate_id, round_number)
);

-- Responses table
CREATE TABLE responses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id UUID REFERENCES rounds(id) ON DELETE CASCADE,
    participant_id UUID REFERENCES participants(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    token_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Project Structure

### mcp-organization
```
mcp-organization/
├── pom.xml
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/zamaz/mcp/organization/
│   │   │       ├── McpOrganizationApplication.java
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── RedisConfig.java
│   │   │       │   └── OpenApiConfig.java
│   │   │       ├── controller/
│   │   │       │   ├── OrganizationController.java
│   │   │       │   ├── AuthController.java
│   │   │       │   └── McpToolsController.java
│   │   │       ├── service/
│   │   │       │   ├── OrganizationService.java
│   │   │       │   ├── UserService.java
│   │   │       │   └── JwtService.java
│   │   │       ├── repository/
│   │   │       │   ├── OrganizationRepository.java
│   │   │       │   └── UserRepository.java
│   │   │       ├── entity/
│   │   │       │   ├── Organization.java
│   │   │       │   ├── User.java
│   │   │       │   └── OrganizationUser.java
│   │   │       ├── dto/
│   │   │       │   ├── OrganizationDto.java
│   │   │       │   ├── UserDto.java
│   │   │       │   └── AuthRequest.java
│   │   │       └── exception/
│   │   │           └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/
│   │           └── V1__Initial_schema.sql
│   └── test/
│       └── java/
│           └── com/zamaz/mcp/organization/
└── README.md
```

### mcp-llm
```
mcp-llm/
├── pom.xml
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/zamaz/mcp/llm/
│   │   │       ├── McpLlmApplication.java
│   │   │       ├── config/
│   │   │       │   ├── WebClientConfig.java
│   │   │       │   ├── RedisConfig.java
│   │   │       │   └── Resilience4jConfig.java
│   │   │       ├── controller/
│   │   │       │   ├── CompletionController.java
│   │   │       │   ├── ProviderController.java
│   │   │       │   └── McpToolsController.java
│   │   │       ├── service/
│   │   │       │   ├── CompletionService.java
│   │   │       │   ├── ProviderRegistry.java
│   │   │       │   └── CacheService.java
│   │   │       ├── provider/
│   │   │       │   ├── LlmProvider.java
│   │   │       │   ├── ClaudeProvider.java
│   │   │       │   ├── OpenAiProvider.java
│   │   │       │   ├── GeminiProvider.java
│   │   │       │   └── OllamaProvider.java
│   │   │       ├── model/
│   │   │       │   ├── CompletionRequest.java
│   │   │       │   ├── CompletionResponse.java
│   │   │       │   └── ProviderConfig.java
│   │   │       └── exception/
│   │   │           └── LlmException.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
└── README.md
```

### mcp-controller
```
mcp-controller/
├── pom.xml
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/zamaz/mcp/controller/
│   │   │       ├── McpControllerApplication.java
│   │   │       ├── config/
│   │   │       │   ├── StateMachineConfig.java
│   │   │       │   ├── IntegrationConfig.java
│   │   │       │   └── AsyncConfig.java
│   │   │       ├── controller/
│   │   │       │   ├── DebateController.java
│   │   │       │   ├── ParticipantController.java
│   │   │       │   └── McpToolsController.java
│   │   │       ├── service/
│   │   │       │   ├── DebateService.java
│   │   │       │   ├── OrchestrationService.java
│   │   │       │   └── WorkflowService.java
│   │   │       ├── repository/
│   │   │       │   ├── DebateRepository.java
│   │   │       │   └── ParticipantRepository.java
│   │   │       ├── entity/
│   │   │       │   ├── Debate.java
│   │   │       │   ├── Participant.java
│   │   │       │   ├── Round.java
│   │   │       │   └── Response.java
│   │   │       ├── statemachine/
│   │   │       │   ├── DebateStateMachine.java
│   │   │       │   └── DebateStateListener.java
│   │   │       └── integration/
│   │   │           ├── LlmServiceClient.java
│   │   │           └── OrganizationServiceClient.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
└── README.md
```

## Implementation Timeline

1. **Week 1**: Project setup and mcp-organization
2. **Week 2**: mcp-llm implementation
3. **Week 3**: mcp-controller implementation
4. **Week 4**: Integration testing and deployment

## Testing Strategy

1. **Unit Tests**: JUnit 5 + Mockito
2. **Integration Tests**: Spring Boot Test + TestContainers
3. **API Tests**: REST Assured
4. **Performance Tests**: JMeter
5. **MCP Protocol Tests**: Custom test suite

## Deployment

All services will be containerized using Docker and orchestrated with docker-compose, maintaining compatibility with the existing Python services.