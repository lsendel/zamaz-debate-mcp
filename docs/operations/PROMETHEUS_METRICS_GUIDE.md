# Prometheus Metrics Integration Guide

This guide explains how to use and monitor the Prometheus metrics integration for rate limiting and circuit breaker features in the MCP system.

## Overview

The MCP Common module provides comprehensive metrics integration with Prometheus through Micrometer, enabling real-time monitoring of:
- Rate limiting operations
- Circuit breaker health and performance
- System resilience metrics

## Rate Limiting Metrics

### Available Metrics

#### Counters
- `rate_limiter.requests` - Total rate limit requests (tagged by key)
- `rate_limiter.violations` - Rate limit violations (tagged by key)
- `rate_limiter.requests.total` - Global total of all rate limit requests

#### Gauges
- `rate_limiter.active` - Number of active rate limiters
- `rate_limiter.violations.total` - Total violations across all rate limiters
- `rate_limiter.violation.rate` - Current violation rate percentage
- `rate_limiter.users.active` - Number of users with active rate limits

#### Timers
- `rate_limiter.operation.duration` - Duration of rate limit operations (tagged by key)

### Example Prometheus Queries

```promql
# Rate limit violation rate over last 5 minutes
rate(rate_limiter_violations_total[5m])

# Average rate limit operation duration
rate_limiter_operation_duration_seconds_mean

# Rate limiters by violation count
topk(10, rate_limiter_violations)

# Active users trend
rate_limiter_users_active
```

## Circuit Breaker Metrics

### Available Metrics

#### Counters
- `circuitbreaker.execution.duration` - Execution duration (tagged by circuit breaker name)
- `circuitbreaker.calls.not.permitted` - Rejected calls counter
- `circuitbreaker.state.transitions` - State transition counter

#### Gauges
- `circuit_breaker_health_score` - Health score (0-100%)
- `circuit_breaker_success_rate_percent` - Success rate percentage
- `circuit_breaker_failure_rate_percent` - Failure rate percentage
- `circuit_breaker_call_not_permitted_rate_percent` - Call rejection rate
- `circuit_breaker_fallback_success_rate_percent` - Fallback success rate
- `circuit_breaker_avg_execution_time_ms` - Average execution time
- `circuit_breaker_total_circuit_breakers` - Total circuit breakers
- `circuit_breaker_calls_not_permitted` - Total calls not permitted
- `circuit_breaker_fallback_executions` - Total fallback executions

### Example Prometheus Queries

```promql
# Circuit breakers with low health scores
circuit_breaker_health_score < 70

# Circuit breaker failure rate trend
rate(circuitbreaker_execution_duration_seconds_count{result="failure"}[5m])

# Circuit breakers in OPEN state
circuitbreaker_state{state="OPEN"}

# Average response time by circuit breaker
avg by (circuitbreaker_name) (circuit_breaker_avg_execution_time_ms)
```

## Configuration

### Enable Prometheus Endpoint

Add to your `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:default}
```

### Access Prometheus Metrics

Once configured, metrics are available at:
```
http://localhost:8080/actuator/prometheus
```

## Grafana Dashboard Example

Create a Grafana dashboard with these panels:

### Rate Limiting Overview
```json
{
  "panels": [
    {
      "title": "Rate Limit Violations",
      "targets": [{
        "expr": "rate(rate_limiter_violations_total[5m])"
      }]
    },
    {
      "title": "Active Rate Limiters",
      "targets": [{
        "expr": "rate_limiter_active"
      }]
    },
    {
      "title": "Violation Rate %",
      "targets": [{
        "expr": "rate_limiter_violation_rate"
      }]
    }
  ]
}
```

### Circuit Breaker Health
```json
{
  "panels": [
    {
      "title": "Circuit Breaker Health Scores",
      "targets": [{
        "expr": "circuit_breaker_health_score"
      }]
    },
    {
      "title": "Circuit Breaker States",
      "targets": [{
        "expr": "circuitbreaker_state"
      }]
    },
    {
      "title": "Failed Calls Rate",
      "targets": [{
        "expr": "rate(circuitbreaker_calls_not_permitted[5m])"
      }]
    }
  ]
}
```

## Alerting Rules

### Prometheus Alert Examples

```yaml
groups:
  - name: rate_limiting_alerts
    rules:
      - alert: HighRateLimitViolationRate
        expr: rate_limiter_violation_rate > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High rate limit violation rate ({{ $value }}%)"
          
      - alert: TooManyActiveRateLimiters
        expr: rate_limiter_active > 1000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Too many active rate limiters ({{ $value }})"

  - name: circuit_breaker_alerts
    rules:
      - alert: CircuitBreakerOpen
        expr: circuitbreaker_state{state="OPEN"} == 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Circuit breaker {{ $labels.circuitbreaker_name }} is OPEN"
          
      - alert: LowCircuitBreakerHealth
        expr: circuit_breaker_health_score < 50
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Circuit breaker {{ $labels.circuit_breaker_name }} health is low ({{ $value }}%)"
```

## Custom Metrics

### Adding Custom Rate Limit Metrics

```java
@Autowired
private McpRateLimitingController rateLimitingController;

// Record custom metric
rateLimitingController.recordRateLimitRequest("custom-operation", allowed);

// Record timing
rateLimitingController.recordRateLimitTiming("custom-operation", () -> {
    // Your operation here
});
```

### Adding Custom Circuit Breaker Metrics

```java
@Autowired
private CircuitBreakerMetricsCollector metricsCollector;

// Record custom execution
metricsCollector.recordSuccessfulExecution("custom-cb", Duration.ofMillis(100));
metricsCollector.recordFailedExecution("custom-cb", Duration.ofMillis(200), exception);
```

## Performance Considerations

1. **Metric Cardinality**: Be careful with tag values to avoid high cardinality
2. **Retention**: Configure appropriate retention policies in Prometheus
3. **Sampling**: For high-volume metrics, consider sampling strategies
4. **Aggregation**: Use recording rules for frequently-queried aggregations

## Troubleshooting

### Metrics Not Appearing

1. Check if Prometheus endpoint is enabled:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. Verify MeterRegistry is properly injected:
   ```bash
   curl http://localhost:8080/api/v1/mcp/rate-limits/metrics
   ```
   Look for `"micrometerIntegrated": true`

3. Check application logs for initialization messages:
   ```
   Rate limiting metrics initialized with MeterRegistry
   ```

### High Memory Usage

If experiencing high memory usage:
1. Reduce metric retention in Prometheus
2. Limit tag cardinality
3. Use metric filters to exclude unnecessary metrics

### Missing Tags

Ensure common tags are configured:
```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
      service: mcp-common
```

## Best Practices

1. **Naming Convention**: Use consistent metric names following Prometheus conventions
2. **Tag Usage**: Keep tag cardinality low (< 100 unique values per tag)
3. **Documentation**: Document all custom metrics in your service
4. **Dashboards**: Create service-specific dashboards for key metrics
5. **Alerts**: Set up alerts for critical thresholds
6. **Testing**: Test metrics collection in integration tests

## References

- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/naming/)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)