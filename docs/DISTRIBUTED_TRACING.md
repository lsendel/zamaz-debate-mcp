# Distributed Tracing with OpenTelemetry

## Overview

This document describes the distributed tracing implementation for the MCP system using OpenTelemetry (OTel). The system provides comprehensive observability across all microservices with support for multiple exporters and advanced sampling strategies.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  MCP Services   │────▶│ OTel Collector  │────▶│     Jaeger      │
│  (Java Apps)    │     │                 │     │  (Trace Store)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
                               ├────▶ Prometheus (Metrics)
                               ├────▶ File (Backup)
                               └────▶ Logging (Debug)
```

## Components

### 1. DistributedTracingConfig
Main configuration class that sets up OpenTelemetry with:
- Multiple span exporters (OTLP, Jaeger, Logging)
- Advanced sampling strategies
- Context propagation (W3C, B3, Jaeger)
- Spring WebMVC/WebFlux integration

### 2. RateLimitingSampler
Custom sampler that limits traces per second to prevent overwhelming the backend:
- Configurable max traces per second
- Time-windowed rate limiting
- Automatic probability adjustment

### 3. TracingInterceptor
Spring MVC interceptor that:
- Adds request context to spans
- Manages baggage propagation
- Injects trace IDs into response headers

### 4. Enhanced Components
- **CustomSpanProcessor**: Enriches spans with user/organization context
- **TracingAspect**: AOP-based automatic method tracing
- **Traced Annotation**: Mark methods for explicit tracing

## Configuration

### Environment Variables

```bash
# OTLP Exporter
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_SERVICE_NAME=mcp-organization
OTEL_TRACES_EXPORTER=otlp

# Sampling
SAMPLING_STRATEGY=parent_based  # Options: always_on, always_off, trace_id_ratio, parent_based, rate_limiting
SAMPLING_RATIO=0.1              # For trace_id_ratio strategy
MAX_TRACES_PER_SECOND=100       # For rate_limiting strategy

# Optional Exporters
JAEGER_ENABLED=false
TRACE_LOGGING_ENABLED=false
PROMETHEUS_PORT=9464
```

### Application Configuration

```yaml
# application-tracing.yml
mcp:
  tracing:
    enabled: true
    exporters:
      otlp:
        enabled: true
        endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT}
      jaeger:
        enabled: false
      prometheus:
        enabled: true
        port: 9464
      logging:
        enabled: false
    sampling:
      strategy: parent_based
      ratio: 0.1
    propagation:
      w3c-trace-context: true
      w3c-baggage: true
      b3: true
```

## Usage

### 1. Automatic Tracing

All Spring components are automatically traced:
- `@RestController` methods
- `@Service` methods
- `@Repository` methods

### 2. Manual Tracing

```java
// Using @Traced annotation
@Traced("custom-operation")
public void performOperation() {
    // Method will be traced
}

// Using OpenTelemetry API
@Autowired
private Tracer tracer;

public void manualTrace() {
    Span span = tracer.spanBuilder("manual-operation")
        .setAttribute("custom.attribute", "value")
        .startSpan();
    
    try (Scope scope = span.makeCurrent()) {
        // Your code here
    } finally {
        span.end();
    }
}
```

### 3. Context Propagation

The system automatically propagates context through:
- HTTP headers (W3C Trace Context)
- Baggage items (user ID, organization ID, request ID)
- Thread locals for async operations

## Viewing Traces

### Jaeger UI
Access traces at http://localhost:16686

Features:
- Search by service, operation, tags
- Trace timeline visualization
- Service dependency graph
- Performance analysis

### Trace Headers

Response headers include trace information:
```
X-Trace-ID: 0af7651916cd43dd8448eb211c80319c
X-Span-ID: b7ad6b7169203331
X-Request-ID: 550e8400-e29b-41d4-a716-446655440000
```

## Sampling Strategies

### 1. Always On
```yaml
sampling:
  strategy: always_on
```
Samples 100% of traces. Use only in development.

### 2. Trace ID Ratio
```yaml
sampling:
  strategy: trace_id_ratio
  ratio: 0.1  # Sample 10% of traces
```
Deterministic sampling based on trace ID.

### 3. Parent Based
```yaml
sampling:
  strategy: parent_based
  ratio: 0.1
```
Respects parent span's sampling decision.

### 4. Rate Limiting
```yaml
sampling:
  strategy: rate_limiting
  max-traces-per-second: 100
```
Limits traces to prevent overload.

## Performance Impact

Tracing overhead is minimal:
- ~1-2% CPU overhead with 10% sampling
- ~100 bytes per span in memory
- Batch processing reduces network calls

## Best Practices

1. **Use Appropriate Sampling**
   - Development: 100% sampling
   - Staging: 10-50% sampling
   - Production: 1-10% sampling

2. **Add Meaningful Attributes**
   ```java
   span.setAttribute("user.id", userId);
   span.setAttribute("order.total", orderTotal);
   ```

3. **Record Errors**
   ```java
   span.recordException(exception);
   span.setStatus(StatusCode.ERROR);
   ```

4. **Use Baggage Sparingly**
   - Only for cross-service context
   - Keep values small
   - Limit to essential data

5. **Name Spans Descriptively**
   - Use dot notation: `service.operation`
   - Be consistent across services
   - Avoid high cardinality

## Troubleshooting

### No Traces Appearing
1. Check if tracing is enabled: `mcp.tracing.enabled=true`
2. Verify collector endpoint is reachable
3. Check sampling configuration
4. Look for errors in logs

### Missing Span Attributes
1. Ensure CustomSpanProcessor is registered
2. Check security context is available
3. Verify request headers are present

### High Memory Usage
1. Reduce batch size in exporter config
2. Lower sampling rate
3. Check for span leaks (unclosed spans)

## Docker Compose Services

```bash
# Start all services with tracing
docker-compose up -d

# View collector logs
docker-compose logs -f otel-collector

# Access Jaeger UI
open http://localhost:16686

# Check collector health
curl http://localhost:13133/
```

## Future Enhancements

1. **Metrics Integration**
   - Span-derived metrics
   - RED metrics (Rate, Errors, Duration)
   - Custom business metrics

2. **Advanced Features**
   - Tail-based sampling
   - Dynamic sampling rules
   - Trace analysis automation

3. **Additional Exporters**
   - AWS X-Ray
   - Google Cloud Trace
   - Azure Monitor