# Performance Testing Guide

## Overview

This directory contains comprehensive performance testing tools and scripts for the MCP Debate API. We use multiple tools to ensure thorough testing coverage:

- **k6**: Modern load testing tool with JavaScript scripting
- **Gatling**: High-performance load testing framework
- **Locust**: Python-based distributed load testing
- **JMeter**: Traditional load testing tool (configured in `/tests/performance/`)

## Test Types

### 1. Load Testing
Tests the system under expected normal load conditions.
- **Duration**: 30+ minutes
- **Users**: Gradual ramp-up to expected peak (100-200 users)
- **Goal**: Verify system handles normal traffic

### 2. Stress Testing
Tests system limits and breaking points.
- **Duration**: 30+ minutes
- **Users**: Beyond expected capacity (200-300+ users)
- **Goal**: Find system limits and observe failure behavior

### 3. Spike Testing
Tests system response to sudden traffic spikes.
- **Duration**: 10-15 minutes
- **Users**: Rapid increase to 10x normal load
- **Goal**: Verify system handles traffic bursts

### 4. Soak Testing
Tests system stability over extended periods.
- **Duration**: 4+ hours
- **Users**: Steady moderate load (50-100 users)
- **Goal**: Detect memory leaks and performance degradation

## Quick Start

### Using the Test Runner Script

```bash
# Run default load test with k6
./run-performance-tests.sh

# Run stress test with k6
./run-performance-tests.sh stress k6

# Run spike test with Gatling
./run-performance-tests.sh spike gatling

# Run with monitoring stack
./run-performance-tests.sh load k6 --with-monitoring
```

### Manual Testing

#### k6 Tests

```bash
# Run load test
k6 run k6/debate-api-load-test.js

# Run with custom parameters
k6 run -e BASE_URL=https://api.example.com \
       -e JWT_TOKEN=your-token \
       --vus 100 \
       --duration 30m \
       k6/debate-api-load-test.js

# Run with InfluxDB output
k6 run --out influxdb=http://localhost:8086/k6 \
       k6/debate-api-load-test.js
```

#### Gatling Tests

```bash
# Build and run with Maven
cd gatling
mvn gatling:test -Dgatling.simulationClass=com.zamaz.mcp.performance.DebateAPILoadTest

# Run with Docker
docker build -t mcp-gatling -f gatling/Dockerfile gatling/
docker run --rm -v $(pwd)/gatling/results:/opt/gatling/results mcp-gatling
```

#### Locust Tests

```bash
# Start Locust with Docker Compose
docker-compose -f docker-compose.perf.yml up locust-master locust-worker

# Access web UI at http://localhost:8089
# Configure test parameters in the UI
```

## Performance Targets

### Response Time SLAs

| Endpoint | 95th Percentile | 99th Percentile |
|----------|-----------------|-----------------|
| Health Check | < 100ms | < 200ms |
| List Debates | < 1000ms | < 2000ms |
| Create Debate | < 2000ms | < 3000ms |
| Get Debate Status | < 500ms | < 1000ms |
| Search Debates | < 1500ms | < 2500ms |

### Throughput Targets

- **Minimum**: 100 requests/second
- **Target**: 500 requests/second
- **Stretch**: 1000 requests/second

### Error Rate Targets

- **Normal Load**: < 0.1% error rate
- **Peak Load**: < 1% error rate
- **Stress Conditions**: < 5% error rate

## Monitoring

### Real-time Monitoring

1. Start monitoring stack:
```bash
docker-compose -f docker-compose.perf.yml up -d influxdb grafana-perf
```

2. Access dashboards:
- Grafana: http://localhost:3001 (admin/admin123)
- InfluxDB: http://localhost:8086

### Metrics to Monitor

1. **Response Times**
   - Average, median, 95th, 99th percentiles
   - Response time distribution
   - Slowest endpoints

2. **Throughput**
   - Requests per second
   - Successful vs failed requests
   - Request rate over time

3. **System Resources**
   - CPU utilization
   - Memory usage
   - Database connections
   - Cache hit rates

4. **Business Metrics**
   - Debates created per minute
   - Active concurrent debates
   - AI provider response times

## Analyzing Results

### k6 Results

Results are saved in JSON format in the `results/` directory.

```bash
# View summary
cat results/k6-load-*.json | jq '.metrics.http_req_duration'

# Generate HTML report
k6 convert results/k6-load-*.json -o results/report.html
```

### Gatling Results

HTML reports are automatically generated in `gatling/results/`.

### Performance Issues Checklist

When performance issues are detected:

1. **High Response Times**
   - Check database query performance
   - Review N+1 query problems
   - Verify cache effectiveness
   - Check external API latencies

2. **High Error Rates**
   - Check rate limiting configuration
   - Verify database connection pool
   - Review circuit breaker settings
   - Check memory allocation

3. **Resource Exhaustion**
   - Monitor JVM heap usage
   - Check thread pool sizes
   - Review connection pools
   - Verify file descriptor limits

## CI/CD Integration

### GitHub Actions

```yaml
- name: Run Performance Tests
  run: |
    cd performance-tests
    ./run-performance-tests.sh load k6
    
- name: Upload Results
  uses: actions/upload-artifact@v4
  with:
    name: performance-results
    path: performance-tests/results/
```

### Jenkins

```groovy
stage('Performance Tests') {
    steps {
        sh 'cd performance-tests && ./run-performance-tests.sh load k6'
        publishHTML([
            allowMissing: false,
            alwaysLinkToLastBuild: true,
            keepAll: true,
            reportDir: 'performance-tests/results',
            reportFiles: '*.html',
            reportName: 'Performance Test Report'
        ])
    }
}
```

## Troubleshooting

### Common Issues

1. **"Connection refused" errors**
   - Ensure services are running: `docker-compose ps`
   - Check service health: `curl http://localhost:8080/health`
   - Verify network connectivity

2. **"Out of memory" during tests**
   - Increase Docker memory allocation
   - Reduce concurrent users
   - Check for memory leaks in application

3. **"Too many open files"**
   - Increase ulimit: `ulimit -n 65536`
   - Reduce connection pool sizes
   - Enable connection reuse

### Debug Mode

Run tests with debug output:

```bash
# k6 debug mode
k6 run --http-debug k6/debate-api-load-test.js

# Gatling debug mode
mvn gatling:test -Dlogback.configurationFile=logback-debug.xml
```

## Best Practices

1. **Baseline First**: Always establish baseline performance before changes
2. **Incremental Load**: Start with small loads and gradually increase
3. **Monitor Everything**: Use comprehensive monitoring during tests
4. **Test in Production-like Environment**: Match production hardware/network
5. **Regular Testing**: Include performance tests in CI/CD pipeline
6. **Document Results**: Keep historical data for trend analysis