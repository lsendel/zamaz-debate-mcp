# MCP LLM Service - Hexagonal Architecture Implementation

**Date**: 2025-07-17  
**Service**: mcp-llm  
**Status**: Complete hexagonal architecture migration

## Architecture Overview

The mcp-llm service has been successfully migrated to follow hexagonal architecture (Ports and Adapters pattern), providing:
- Clean separation between business logic and external provider integrations
- Framework independence in the domain layer
- Clear dependency inversion (Domain ← Application ← Adapters)
- Enhanced testability and maintainability for LLM operations

## Layer Structure

```
src/main/java/com/zamaz/mcp/llm/
├── domain/                          # Domain Layer (Core Business Logic)
│   ├── model/                       # Aggregates, Entities, Value Objects
│   │   ├── Provider.java           # Aggregate Root - LLM provider management
│   │   ├── LlmModel.java           # Entity - individual models with capabilities
│   │   ├── CompletionRequest.java  # Aggregate Root - completion requests
│   │   ├── ProviderId.java         # Value Object - provider identifier
│   │   ├── ModelName.java          # Value Object - model name
│   │   ├── TokenUsage.java         # Value Object - token counting and costs
│   │   ├── ProviderStatus.java     # Value Object - provider health status
│   │   ├── RequestId.java          # Value Object - request identifier
│   │   └── PromptContent.java      # Value Object - validated prompt content
│   ├── event/                       # Domain Events
│   │   ├── ProviderStatusChangedEvent.java
│   │   └── CompletionRequestCreatedEvent.java
│   └── service/                     # Domain Services
│       ├── ProviderSelectionService.java
│       └── ProviderSelectionServiceImpl.java
│
├── application/                     # Application Layer (Use Cases & Orchestration)
│   ├── port/                        # Port Interfaces
│   │   ├── inbound/                # Use Case Interfaces (Inbound Ports)
│   │   │   ├── GenerateCompletionUseCase.java
│   │   │   ├── StreamCompletionUseCase.java
│   │   │   ├── ListProvidersUseCase.java
│   │   │   └── CheckProviderHealthUseCase.java
│   │   └── outbound/               # Repository & Service Interfaces (Outbound Ports)
│   │       ├── ProviderRepository.java
│   │       ├── LlmProviderGateway.java
│   │       └── CompletionCacheService.java
│   ├── command/                     # Commands (CQRS Write Side)
│   │   ├── GenerateCompletionCommand.java
│   │   ├── StreamCompletionCommand.java
│   │   └── CheckProviderHealthCommand.java
│   ├── query/                       # Queries (CQRS Read Side)
│   │   ├── ListProvidersQuery.java
│   │   ├── CompletionResult.java
│   │   ├── CompletionChunk.java
│   │   ├── ProviderListResult.java
│   │   └── ProviderHealthResult.java
│   └── usecase/                     # Use Case Implementations
│       ├── GenerateCompletionUseCaseImpl.java
│       ├── StreamCompletionUseCaseImpl.java
│       ├── ListProvidersUseCaseImpl.java
│       └── CheckProviderHealthUseCaseImpl.java
│
└── adapter/                         # Adapter Layer (Infrastructure)
    ├── web/                         # Web Adapters (Inbound)
    │   ├── controller/
    │   │   └── CompletionController.java
    │   ├── dto/                     # Web DTOs
    │   │   ├── CompletionRequest.java
    │   │   ├── CompletionResponse.java
    │   │   ├── ProviderResponse.java
    │   │   └── ErrorResponse.java
    │   └── mapper/
    │       └── LlmWebMapper.java
    ├── external/                    # External Service Adapters (Outbound)
    │   ├── ClaudeProviderAdapter.java
    │   ├── OpenAiProviderAdapter.java
    │   ├── RedisCompletionCacheService.java
    │   └── CompositeProviderGateway.java
    └── infrastructure/              # Infrastructure Adapters
        └── ProviderSelectionServiceAdapter.java
```

## Key Domain Concepts

### Provider Aggregate
The `Provider` is the main aggregate root that:
- Manages LLM providers (Claude, OpenAI, Gemini, etc.)
- Contains multiple models as child entities
- Tracks health status and capabilities
- Enforces business rules for model selection
- Publishes domain events for status changes

### Core Business Rules
1. **Provider Selection**: Smart routing based on cost, performance, and capabilities
2. **Model Compatibility**: Ensures requests match model capabilities and limits
3. **Health Monitoring**: Tracks provider availability and response times
4. **Token Management**: Accurate token counting and cost calculation
5. **Request Lifecycle**: PENDING → PROCESSING → COMPLETED/FAILED progression

### Value Objects
- **ProviderId/RequestId**: Strong typing for identifiers
- **TokenUsage**: Rich cost calculation with input/output token breakdown
- **PromptContent**: Validated content with length limits and truncation
- **ModelName**: Standardized model identification across providers
- **ProviderStatus**: Health status with availability checking

## Use Cases

### Primary Operations
1. **GenerateCompletion**: Synchronous text completion with provider selection
2. **StreamCompletion**: Real-time streaming completions with delta support
3. **ListProviders**: Provider discovery with filtering and pagination
4. **CheckProviderHealth**: Health monitoring and status updates

### Advanced Features
- **Provider Selection**: Intelligent routing based on multiple criteria
- **Caching**: Redis-based response caching for improved performance
- **Streaming**: Server-Sent Events with delta and full content modes
- **Health Monitoring**: Comprehensive provider and model health tracking
- **Cost Optimization**: Token usage tracking and cost calculation

## Adapter Implementations

### Web Adapters
- **CompletionController**: REST API with OpenAPI documentation
- **Streaming Support**: Server-Sent Events for real-time completions
- **DTOs**: Clean separation between API contracts and domain models
- **Error Handling**: Provider-specific error responses with proper HTTP status codes

### External Service Adapters
- **ClaudeProviderAdapter**: Anthropic Claude API integration with circuit breakers
- **OpenAiProviderAdapter**: OpenAI GPT models with dynamic model discovery
- **RedisCompletionCacheService**: High-performance caching with TTL management
- **CompositeProviderGateway**: Central dispatcher routing to appropriate providers

### Provider Capabilities
- **Claude**: Opus, Sonnet, Haiku models with vision support
- **OpenAI**: GPT-4, GPT-4 Turbo, GPT-3.5 Turbo with function calling
- **Health Checks**: Real-time provider availability monitoring
- **Resilience**: Circuit breakers, retries, and graceful degradation

## Dependency Injection

All components are wired through Spring configuration:

```java
@Bean
public GenerateCompletionUseCase generateCompletionUseCase(
    ProviderRepository providerRepository,
    LlmProviderGateway providerGateway,
    CompletionCacheService cacheService,
    ProviderSelectionService selectionService
) {
    return new GenerateCompletionUseCaseImpl(
        providerRepository, providerGateway, cacheService, selectionService
    );
}
```

## Provider Selection Logic

The service implements sophisticated provider selection:

### Selection Strategies
- **COST_OPTIMIZED**: Minimizes token costs
- **PERFORMANCE_FIRST**: Prioritizes speed and capabilities
- **QUALITY_FIRST**: Selects highest quality models
- **BALANCED**: Balances cost, performance, and quality

### Selection Criteria
- Required token capacity
- Model capabilities (streaming, vision, system messages)
- Provider health status
- Cost thresholds
- User preferences

## API Examples

### Generate Completion
```bash
POST /api/v1/llm/completions
X-Organization-Id: org-123
{
  "prompt": "Explain hexagonal architecture",
  "maxTokens": 1000,
  "temperature": 0.7,
  "model": "claude-3-sonnet-20240229",
  "enableCaching": true
}
```

### Stream Completion
```bash
POST /api/v1/llm/completions/stream
X-Organization-Id: org-123
Content-Type: application/json

{
  "prompt": "Write a story about AI",
  "maxTokens": 2000,
  "temperature": 0.8,
  "streaming": true
}
```

### List Providers
```bash
GET /api/v1/llm/providers?status=available&capability=streaming&includeMetrics=true
X-Organization-Id: org-123
```

## Performance Considerations

- **Caching**: Completion responses cached for 1 hour by default
- **Connection Pooling**: Optimized HTTP client configurations per provider
- **Streaming**: Efficient Server-Sent Events with configurable buffer sizes
- **Health Checks**: Cached health status to reduce API calls
- **Token Estimation**: Fast local estimation with provider-specific algorithms

## Monitoring & Observability

- **Structured Logging**: Request/response correlation IDs and provider context
- **Domain Events**: Provider status changes and completion requests
- **Metrics**: Token usage, cache hit rates, provider response times, error rates
- **Health Checks**: Real-time provider availability and model status
- **Circuit Breakers**: Automatic failure detection and recovery

## Security & Compliance

- **API Key Management**: Environment-based configuration, no hardcoded secrets
- **Request Sanitization**: Input validation and content filtering
- **Rate Limiting**: Provider-specific rate limit handling
- **Audit Trail**: Complete request/response logging for compliance
- **Error Isolation**: Provider failures don't affect other providers

## Migration Benefits

1. **Provider Flexibility**: Easy to add new LLM providers (Gemini, Llama, etc.)
2. **Testability**: Domain logic completely isolated from external APIs
3. **Resilience**: Provider failures isolated with automatic failover
4. **Cost Optimization**: Smart provider selection based on cost and quality
5. **Performance**: Intelligent caching and connection management
6. **Monitoring**: Comprehensive observability for production operations

This implementation provides a robust, scalable foundation for LLM operations with clean architecture principles, ensuring long-term maintainability and easy extensibility for new providers and capabilities.