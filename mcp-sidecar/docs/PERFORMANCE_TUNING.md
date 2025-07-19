# MCP Sidecar Performance Tuning Guide

## Overview

This guide provides comprehensive instructions for optimizing the performance of the MCP Sidecar service. The sidecar includes built-in performance optimization features that can significantly improve throughput, reduce latency, and optimize resource utilization.

## Performance Features

### 1. Request Batching
Automatically batches multiple requests to reduce overhead and improve throughput.

**Configuration:**
```yaml
app:
  performance:
    batch:
      size: 100              # Maximum batch size
      timeout: 100ms         # Maximum wait time before flushing
      auto-batch: true       # Enable automatic batching
```

**Usage:**
```java
// Execute with batching
performanceService.executeWithBatching("cache-lookup", "key123")
    .subscribe(result -> log.info("Result: {}", result));
```

### 2. Connection Pooling
Manages reusable connections to reduce connection overhead.

**Configuration:**
```yaml
app:
  performance:
    connection-pool:
      size: 50                    # Pool size
      idle-timeout: 5m            # Idle connection timeout
      acquisition-timeout: 30s    # Timeout for acquiring connection
```

### 3. Request Deduplication
Prevents duplicate requests from being processed simultaneously.

**Configuration:**
```yaml
app:
  performance:
    deduplication:
      enabled: true
      cache-size: 1000
      timeout: 5s
```

**Usage:**
```java
// Execute with deduplication
performanceService.executeWithDeduplication(
    "unique-key", 
    expensiveOperation()
);
```

### 4. Compression
Automatically compresses large payloads to reduce network overhead.

**Configuration:**
```yaml
app:
  performance:
    compression:
      enabled: true
      min-size: 1024    # Minimum size for compression (bytes)
      level: 6          # Compression level (1-9)
```

### 5. Resource Pooling
Pools frequently used objects to reduce garbage collection overhead.

**Available Pools:**
- ByteBuffer pool for network I/O
- StringBuilder pool for string operations

**Usage:**
```java
// Acquire resource
ByteBuffer buffer = performanceService.acquireResource(ByteBuffer.class);

// Use resource
buffer.put(data);

// Release resource
performanceService.releaseResource(ByteBuffer.class, buffer);
```

### 6. Bulkhead Pattern
Isolates resources to prevent cascading failures.

**Configuration:**
```yaml
app:
  performance:
    bulkhead:
      services:
        database:
          max-concurrent-calls: 20
          max-wait-duration: 30s
        external-api:
          max-concurrent-calls: 50
          max-wait-duration: 60s
```

**Usage:**
```java
// Execute with bulkhead protection
performanceService.executeWithBulkhead("database", databaseOperation());
```

### 7. Intelligent Caching
Multi-level caching with predictive preloading.

**Configuration:**
```yaml
app:
  performance:
    cache:
      preload:
        enabled: true
        interval: 5m
        threshold: 10    # Minimum access count for preloading
      multi-level:
        enabled: true
        l1:
          max-size: 1000
          ttl: 5m
        l2:
          ttl: 30m
```

## Performance Optimization Strategies

### 1. Pipeline Optimization
Use the `optimizePipeline` method to automatically apply optimizations:

```java
OptimizationOptions options = OptimizationOptions.builder()
    .withRetry(3)
    .withTimeout(Duration.ofSeconds(30))
    .withCache("cache-key", Duration.ofMinutes(5))
    .withBulkhead("database")
    .asIoOperation()
    .withMetricsKey("my-operation");

Mono<Result> optimized = performanceService.optimizePipeline(
    originalOperation(), 
    options
);
```

### 2. JVM Tuning
Recommended JVM flags for optimal performance:

```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-XX:+AlwaysPreTouch
-XX:+DisableExplicitGC
-XX:InitialRAMPercentage=50.0
-XX:MaxRAMPercentage=75.0
-XX:MetaspaceSize=256M
-XX:MaxMetaspaceSize=512M
```

### 3. Thread Pool Configuration
Optimize thread pools based on workload:

```yaml
app:
  performance:
    thread-pool:
      core-size: 10        # CPU-bound: number of CPU cores
      max-size: 50         # I/O-bound: higher values
      keep-alive-time: 60s
      queue-capacity: 1000
```

### 4. Network Optimization
Configure network settings for optimal performance:

```yaml
app:
  performance:
    network:
      tcp-no-delay: true
      send-buffer-size: 65536
      receive-buffer-size: 65536
      connect-timeout: 30s
      read-timeout: 60s
```

## Performance Monitoring

### 1. Metrics Endpoint
Access performance metrics via REST API:

```bash
GET /api/v1/performance/metrics
```

Response includes:
- Request latencies
- Cache hit rates
- Compression savings
- Connection pool usage
- Bulkhead statistics

### 2. Performance Score
Get overall optimization score:

```bash
GET /api/v1/performance/status
```

Score interpretation:
- **90-100**: EXCELLENT
- **75-89**: GOOD
- **50-74**: MODERATE
- **25-49**: POOR
- **0-24**: CRITICAL

### 3. Real-time Monitoring
Monitor key metrics in real-time:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## Best Practices

### 1. Use Appropriate Optimization Options
Choose optimizations based on operation type:

**For Database Operations:**
```java
OptimizationOptions.builder()
    .withBulkhead("database")
    .withRetry(3)
    .withCache(cacheKey, Duration.ofMinutes(5))
    .withTimeout(Duration.ofSeconds(30))
```

**For External API Calls:**
```java
OptimizationOptions.builder()
    .withBulkhead("external-api")
    .withRetry(3)
    .withTimeout(Duration.ofSeconds(60))
    .asIoOperation()
```

**For Compute-Intensive Operations:**
```java
OptimizationOptions.builder()
    .withBulkhead("compute")
    .withTimeout(Duration.ofMinutes(5))
```

### 2. Monitor and Adjust
1. Start with default configurations
2. Monitor metrics under load
3. Identify bottlenecks
4. Adjust specific settings
5. Test and validate improvements

### 3. Resource Management
- Always release pooled resources
- Use try-with-resources where possible
- Monitor pool utilization
- Adjust pool sizes based on usage

### 4. Caching Strategy
- Cache frequently accessed, rarely changing data
- Use appropriate TTL values
- Monitor cache hit rates
- Enable preloading for critical data

## Troubleshooting

### High Latency
1. Check bulkhead saturation
2. Verify connection pool availability
3. Review thread pool configuration
4. Enable request batching

### High Memory Usage
1. Reduce cache sizes
2. Decrease resource pool sizes
3. Enable compression
4. Review off-heap memory settings

### Low Throughput
1. Increase thread pool sizes
2. Enable request batching
3. Increase connection pool size
4. Review network buffer sizes

### Connection Timeouts
1. Increase acquisition timeout
2. Increase pool size
3. Check for connection leaks
4. Monitor pool metrics

## Performance Testing

### Load Testing Script
```bash
# Run load test
artillery run performance-test.yml

# Monitor during test
watch -n 1 'curl -s localhost:8080/api/v1/performance/metrics | jq .'
```

### Sample Load Test Configuration
```yaml
config:
  target: "http://localhost:8080"
  phases:
    - duration: 60
      arrivalRate: 10
      rampTo: 100
scenarios:
  - name: "Performance Test"
    flow:
      - post:
          url: "/api/v1/performance/optimize"
          json:
            retryEnabled: true
            cacheEnabled: true
            bulkheadName: "test"
```

## Conclusion

The MCP Sidecar's performance optimization features provide comprehensive tools for achieving optimal performance. Start with the default configurations and adjust based on your specific workload and monitoring data. Regular monitoring and tuning will ensure sustained high performance.