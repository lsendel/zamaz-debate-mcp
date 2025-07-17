# Circuit Breaker Pattern Implementation

## Overview

This document describes the comprehensive resilience patterns implementation for the MCP system using Resilience4j. The implementation provides circuit breaker, retry, rate limiting, and bulkhead patterns to ensure service resilience and prevent cascading failures.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Application   │────▶│   Resilience    │────▶│     Service     │
│     Code        │     │    Patterns     │     │      Call       │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
                        ┌──────┴──────┬─────────┬──────────┐
                        │             │         │          │
                  Circuit Breaker   Retry   Rate Limiter  Bulkhead
```

## Resilience Patterns

### 1. Circuit Breaker
Prevents cascading failures by monitoring service health and blocking calls when failure threshold is reached.

**States:**
- **CLOSED**: Normal operation, all calls pass through
- **OPEN**: Service is failing, calls are blocked
- **HALF_OPEN**: Testing if service has recovered

**Configuration:**
```yaml
mcp:
  resilience:
    circuit-breaker:
      global:
        failure-rate-threshold: 50.0      # Open circuit at 50% failure rate
        slow-call-rate-threshold: 100.0   # Consider all slow calls as failures
        slow-call-duration-threshold: 2s  # Calls slower than 2s are "slow"
        wait-duration-in-open-state: 60s  # Wait 60s before trying again
```

### 2. Retry
Automatically retries failed operations with exponential backoff and jitter.

**Features:**
- Exponential backoff with configurable multiplier
- Jitter to prevent thundering herd
- Configurable retry exceptions

**Configuration:**
```yaml
mcp:
  resilience:
    retry:
      global:
        max-attempts: 3
        wait-duration: 1s
        exponential-multiplier: 2.0
        use-jitter: true
```

### 3. Rate Limiter
Limits the number of calls to prevent overwhelming services.

**Features:**
- Token bucket algorithm
- Configurable refresh period
- Timeout for waiting threads

**Configuration:**
```yaml
mcp:
  resilience:
    rate-limiter:
      global:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 5s
```

### 4. Bulkhead
Isolates resources to prevent total system failure.

**Types:**
- **Semaphore**: Limits concurrent calls
- **Thread Pool**: Isolates calls in separate thread pool

**Configuration:**
```yaml
mcp:
  resilience:
    bulkhead:
      global:
        max-concurrent-calls: 25
        max-wait-duration: 1s
```

## Usage Examples

### 1. Basic Circuit Breaker
```java
@Service
public class UserService {
    
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserFallback")
    public User getUser(Long id) {
        return userApiClient.fetchUser(id);
    }
    
    public User getUserFallback(Long id, Exception ex) {
        log.warn("Falling back for user {}: {}", id, ex.getMessage());
        return User.createDefault(id);
    }
}
```

### 2. Combined Patterns
```java
@Service
public class PaymentService {
    
    @CircuitBreaker(name = "payment-gateway")
    @Retry(maxAttempts = 3)
    @RateLimiter(limitForPeriod = 100)
    public PaymentResult processPayment(Payment payment) {
        return paymentGateway.process(payment);
    }
}
```

### 3. Custom Configuration
```java
@CircuitBreaker(
    name = "critical-service",
    failureRateThreshold = 20.0f,
    waitDurationInOpenStateSeconds = 30,
    slowCallDurationThresholdMs = 1000
)
public Result criticalOperation() {
    return externalService.call();
}
```

### 4. RestTemplate Integration
```java
@Autowired
@Qualifier("llmRestTemplate")
private RestTemplate llmRestTemplate;

public LLMResponse callLLM(LLMRequest request) {
    // Automatically protected by circuit breaker and retry
    return llmRestTemplate.postForObject(
        llmServiceUrl + "/generate",
        request,
        LLMResponse.class
    );
}
```

## Monitoring

### 1. Health Checks
Access circuit breaker health at: `http://localhost:8080/actuator/health`

```json
{
  "circuitBreaker": {
    "status": "UP",
    "details": {
      "user-service": {
        "state": "CLOSED",
        "failureRate": "5.23%",
        "slowCallRate": "2.10%",
        "healthPercentage": "94.77%"
      }
    }
  }
}
```

### 2. Metrics
Prometheus metrics available at: `http://localhost:8080/actuator/prometheus`

Key metrics:
- `resilience4j_circuitbreaker_state` - Current state
- `resilience4j_circuitbreaker_failure_rate` - Failure percentage
- `resilience4j_circuitbreaker_calls` - Call counts by result
- `resilience4j_retry_calls` - Retry attempts
- `resilience4j_ratelimiter_available_permissions` - Available tokens

### 3. Management Endpoints
- `/actuator/circuitbreakers` - List all circuit breakers
- `/actuator/retries` - List all retry instances
- `/actuator/ratelimiters` - List all rate limiters
- `/actuator/bulkheads` - List all bulkheads

## Best Practices

### 1. Circuit Breaker
- Set failure threshold based on service criticality
- Use shorter wait times for non-critical services
- Always provide meaningful fallback methods
- Monitor slow calls as they often precede failures

### 2. Retry
- Use exponential backoff for external services
- Add jitter to prevent synchronized retries
- Limit retries for non-idempotent operations
- Log retry attempts for debugging

### 3. Rate Limiting
- Set limits based on downstream capacity
- Use different limits for different clients
- Monitor rate limit violations
- Provide clear error messages when limited

### 4. Bulkhead
- Use thread pool bulkhead for I/O operations
- Use semaphore bulkhead for CPU-bound operations
- Size pools based on expected load
- Monitor queue depths

## Configuration Guidelines

### Development Environment
```yaml
spring:
  profiles: development

mcp:
  resilience:
    circuit-breaker:
      global:
        failure-rate-threshold: 80.0  # More tolerant
        wait-duration-in-open-state: 10s  # Faster recovery
```

### Production Environment
```yaml
spring:
  profiles: production

mcp:
  resilience:
    circuit-breaker:
      global:
        failure-rate-threshold: 30.0  # More strict
        wait-duration-in-open-state: 120s  # Slower recovery
```

## Troubleshooting

### Circuit Breaker Always Open
1. Check failure rate threshold
2. Verify minimum number of calls
3. Check for persistent failures
4. Review fallback method implementation

### Retries Not Working
1. Verify retry configuration
2. Check exception types
3. Review retry event logs
4. Ensure aspect ordering

### Rate Limiter Too Restrictive
1. Increase limit-for-period
2. Decrease limit-refresh-period
3. Monitor actual usage patterns
4. Consider bulkhead instead

## Testing

### Unit Testing
```java
@Test
void testCircuitBreakerOpens() {
    // Force failures
    for (int i = 0; i < 10; i++) {
        assertThrows(Exception.class, 
            () -> service.unreliableMethod());
    }
    
    // Circuit should be open
    assertThrows(CallNotPermittedException.class,
        () -> service.unreliableMethod());
}
```

### Integration Testing
1. Use WireMock to simulate failures
2. Test fallback scenarios
3. Verify metrics collection
4. Test recovery behavior

## Migration Guide

### From Hystrix
1. Replace `@HystrixCommand` with `@CircuitBreaker`
2. Update configuration properties
3. Migrate fallback methods
4. Update dashboards to use Micrometer

### Adding to Existing Service
1. Add dependencies to pom.xml
2. Import resilience configuration
3. Add annotations to critical methods
4. Configure service-specific settings
5. Add health checks and monitoring