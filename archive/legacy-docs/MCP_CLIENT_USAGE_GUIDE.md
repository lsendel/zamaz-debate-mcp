# MCP Client Usage Guide

## Overview

The MCP client framework provides a standardized way for services to communicate with each other using the Model Context Protocol (MCP). It consists of several components that handle authentication, service discovery, error handling, and type-safe API calls.

## Architecture

### Core Components

1. **McpServiceClient** - Low-level client for generic MCP tool calls
2. **McpServiceRegistry** - Service discovery and URL management
3. **Service-Specific Clients** - High-level, type-safe clients for each service
4. **McpClientException** - Structured exception handling
5. **McpClientTestUtil** - Testing and validation utilities

### Service-Specific Clients

- **OrganizationServiceClient** - Organization management operations
- **ContextServiceClient** - Context management and messaging
- **LlmServiceClient** - LLM provider operations

## Configuration

### Service URLs

Configure service URLs in your application properties:

```yaml
mcp:
  services:
    organization:
      url: http://localhost:5005
    context:
      url: http://localhost:5007
    llm:
      url: http://localhost:5002
    controller:
      url: http://localhost:5013
    rag:
      url: http://localhost:5004
    template:
      url: http://localhost:5006
    gateway:
      url: http://localhost:8080
```

### Enable MCP Client

Add the configuration import to your Spring Boot application:

```java
@SpringBootApplication
@Import(McpClientConfiguration.class)
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

## Basic Usage

### Dependency Injection

Inject the service clients you need:

```java
@Service
@RequiredArgsConstructor
public class YourService {
    
    private final OrganizationServiceClient organizationClient;
    private final ContextServiceClient contextClient;
    private final LlmServiceClient llmClient;
    
    // Your service methods here
}
```

### Authentication Context

Most operations require an authentication context:

```java
@RestController
@RequiredArgsConstructor
public class YourController {
    
    private final OrganizationServiceClient organizationClient;
    
    @GetMapping("/organization")
    public JsonNode getOrganization(Authentication authentication) {
        return organizationClient.getOrganization(authentication);
    }
}
```

## Service Examples

### Organization Service

```java
// Get current user's organization
JsonNode org = organizationClient.getOrganization(authentication);

// Create a new organization
JsonNode newOrg = organizationClient.createOrganization(
    "My Organization", 
    "Description", 
    authentication
);

// Add user to organization
JsonNode result = organizationClient.addUserToOrganization(
    userId, 
    "member", 
    authentication
);

// Update organization
JsonNode updated = organizationClient.updateOrganization(
    "New Name", 
    "New Description", 
    true, 
    authentication
);
```

### Context Service

```java
// Create a new context
JsonNode context = contextClient.createContext(
    "Debate Context", 
    "Context for debate discussion", 
    authentication
);

// Append a message
UUID contextId = UUID.fromString(context.get("context").get("id").asText());
JsonNode message = contextClient.appendMessage(
    contextId,
    "user",
    "Hello, this is my message",
    authentication
);

// Get optimized context window
JsonNode window = contextClient.getContextWindow(
    contextId,
    4096,  // maxTokens
    authentication
);

// Search contexts
JsonNode searchResults = contextClient.searchContexts(
    "debate", 
    authentication
);

// Share context
JsonNode shareResult = contextClient.shareContextWithOrganization(
    contextId,
    targetOrgId,
    "read",
    authentication
);
```

### LLM Service

```java
// List available providers
JsonNode providers = llmClient.listProviders();

// Generate completion with Claude
JsonNode completion = llmClient.generateClaudeCompletion(
    "Write a short poem about AI",
    authentication
);

// Generate completion with specific parameters
JsonNode completion = llmClient.generateCompletion(
    "openai",           // provider
    "What is AI?",      // prompt
    "gpt-4",           // model
    1000,              // maxTokens
    0.7,               // temperature
    authentication
);

// Check provider status
JsonNode status = llmClient.getProviderStatus("claude");

// Get provider availability
Map<String, Boolean> availability = llmClient.getProviderAvailability();
```

## Low-Level Client Usage

For custom operations not covered by service-specific clients:

```java
@Component
@RequiredArgsConstructor
public class CustomService {
    
    private final McpServiceClient mcpClient;
    private final McpServiceRegistry serviceRegistry;
    
    public JsonNode customOperation(Authentication authentication) {
        String serviceUrl = serviceRegistry.getServiceUrl(
            McpServiceRegistry.McpService.ORGANIZATION
        );
        
        Map<String, Object> params = new HashMap<>();
        params.put("customParam", "customValue");
        
        return mcpClient.callTool(
            serviceUrl,
            "custom_tool",
            params,
            authentication
        );
    }
}
```

## Error Handling

The MCP client provides structured exception handling:

```java
try {
    JsonNode result = organizationClient.getOrganization(authentication);
    // Process result
} catch (McpClientException e) {
    log.error("MCP call failed: {}", e.getDetailedMessage());
    
    // Access structured error information
    McpErrorCode errorCode = e.getErrorCode();
    String requestId = e.getRequestId();
    String serviceUrl = e.getServiceUrl();
    String toolName = e.getToolName();
    
    // Handle specific error types
    switch (errorCode) {
        case AUTHENTICATION_REQUIRED:
            // Handle auth error
            break;
        case SERVICE_UNAVAILABLE:
            // Handle service unavailable
            break;
        case TOOL_NOT_FOUND:
            // Handle unknown tool
            break;
        default:
            // Handle other errors
            break;
    }
}
```

## Service Discovery

Check service availability and get service information:

```java
@Component
@RequiredArgsConstructor
public class HealthService {
    
    private final McpServiceRegistry serviceRegistry;
    
    public Map<String, Object> getSystemHealth() {
        // Get service discovery information
        Map<String, Object> discovery = serviceRegistry.getServiceDiscoveryInfo();
        
        // Check specific service
        boolean orgServiceUp = serviceRegistry.isServiceAvailable(
            McpServiceRegistry.McpService.ORGANIZATION
        );
        
        // Get all service health status
        Map<String, Boolean> healthStatus = serviceRegistry.getServiceHealthStatus();
        
        return Map.of(
            "discovery", discovery,
            "organizationService", orgServiceUp,
            "allServices", healthStatus
        );
    }
}
```

## Testing and Validation

Use the test utilities to validate MCP client functionality:

```java
@Component
@RequiredArgsConstructor
public class McpTestService {
    
    private final McpClientTestUtil testUtil;
    
    public Map<String, Object> runTests(Authentication authentication) {
        // Run comprehensive tests
        Map<String, List<McpClientTestUtil.TestResult>> results = 
            testUtil.runComprehensiveTests(authentication);
        
        // Test service connectivity
        Map<String, Boolean> connectivity = testUtil.testServiceConnectivity();
        
        // Perform health check
        boolean systemHealthy = testUtil.performHealthCheck();
        
        // Test parallel calls
        List<McpClientTestUtil.TestResult> parallelResults = 
            testUtil.testParallelCalls(authentication);
        
        return Map.of(
            "comprehensiveTests", results,
            "connectivity", connectivity,
            "systemHealthy", systemHealthy,
            "parallelTests", parallelResults
        );
    }
}
```

## Best Practices

### 1. Always Handle Exceptions

```java
try {
    JsonNode result = serviceClient.callOperation(params, authentication);
    return processResult(result);
} catch (McpClientException e) {
    log.error("Service call failed: {}", e.getDetailedMessage());
    // Handle appropriately - retry, fallback, or propagate
    throw new ServiceException("Operation failed", e);
}
```

### 2. Check Service Availability

```java
if (!serviceRegistry.isServiceAvailable(McpService.LLM)) {
    throw new ServiceUnavailableException("LLM service is not available");
}
```

### 3. Use Service-Specific Clients

Prefer service-specific clients over low-level McpServiceClient for type safety and convenience.

### 4. Configure Timeouts

The client uses resilient RestTemplate with circuit breakers and retries. Configure these in your resilience4j configuration.

### 5. Monitor and Log

```java
@EventListener
public void onMcpClientException(McpClientException e) {
    // Log metrics for monitoring
    meterRegistry.counter("mcp.client.errors", 
        "service", e.getServiceUrl(),
        "tool", e.getToolName(),
        "error", e.getErrorCode().getCode()
    ).increment();
}
```

## Service Integration Patterns

### Request-Response Pattern

```java
// Simple request-response
JsonNode response = llmClient.generateCompletion("claude", prompt, auth);
String text = response.get("text").asText();
```

### Context Management Pattern

```java
// Create context, add messages, get window
JsonNode context = contextClient.createContext("Session", "desc", auth);
UUID contextId = UUID.fromString(context.get("context").get("id").asText());

contextClient.appendMessage(contextId, "user", "Hello", auth);
contextClient.appendMessage(contextId, "assistant", "Hi there!", auth);

JsonNode window = contextClient.getContextWindow(contextId, auth);
```

### Multi-Service Workflow Pattern

```java
// Workflow spanning multiple services
public JsonNode createDebateWithContext(String topic, Authentication auth) {
    // 1. Get organization
    JsonNode org = organizationClient.getOrganization(auth);
    
    // 2. Create context for debate
    JsonNode context = contextClient.createContext(
        "Debate: " + topic, 
        "Context for debate on " + topic, 
        auth
    );
    
    // 3. Generate initial prompt using LLM
    JsonNode prompt = llmClient.generateCompletion(
        "claude",
        "Create a debate prompt for: " + topic,
        auth
    );
    
    // 4. Create debate using controller service
    // (This would require implementing DebateServiceClient)
    
    return combineResults(org, context, prompt);
}
```

## Configuration Reference

### Application Properties

```yaml
# Service URLs
mcp:
  services:
    organization:
      url: ${ORGANIZATION_SERVICE_URL:http://localhost:5005}
    context:
      url: ${CONTEXT_SERVICE_URL:http://localhost:5007}
    llm:
      url: ${LLM_SERVICE_URL:http://localhost:5002}
    controller:
      url: ${CONTROLLER_SERVICE_URL:http://localhost:5013}
    rag:
      url: ${RAG_SERVICE_URL:http://localhost:5004}

# Resilience4j configuration for MCP clients
resilience4j:
  circuitbreaker:
    instances:
      organization-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
      context-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
      llm-service:
        failure-rate-threshold: 30
        wait-duration-in-open-state: 60s
        sliding-window-size: 10
  
  retry:
    instances:
      organization-service:
        max-attempts: 3
        wait-duration: 1s
      context-service:
        max-attempts: 3
        wait-duration: 1s
      llm-service:
        max-attempts: 2
        wait-duration: 2s
```

This MCP client framework provides a robust, type-safe, and resilient way to perform inter-service communication in your MCP system.