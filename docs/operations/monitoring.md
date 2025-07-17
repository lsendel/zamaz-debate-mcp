# Monitoring and Alerting Guide

This guide provides instructions for monitoring the Zamaz Debate MCP system and setting up alerts for potential issues.

## Monitoring Architecture

The monitoring stack consists of:

1. **Prometheus**: For metrics collection and storage
2. **Grafana**: For visualization and dashboards
3. **Alertmanager**: For alert routing and notifications
4. **Spring Boot Actuator**: For application metrics
5. **cAdvisor**: For container metrics
6. **Node Exporter**: For host metrics

## Enabling Monitoring

To enable the monitoring stack:

```bash
# Start with monitoring profile
docker-compose --profile monitoring up -d
```

This starts Prometheus, Grafana, and related services.

## Accessing Monitoring Tools

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (default credentials: admin/admin)
- **Alertmanager**: http://localhost:9093

## Key Metrics to Monitor

### System Metrics

- **CPU Usage**: High CPU usage may indicate processing bottlenecks
- **Memory Usage**: Memory leaks or insufficient allocation
- **Disk Usage**: Storage capacity and I/O performance
- **Network Traffic**: Bandwidth usage and network errors

### Application Metrics

- **Request Rate**: Requests per second by service
- **Response Time**: Latency percentiles (p50, p95, p99)
- **Error Rate**: Percentage of requests resulting in errors
- **JVM Metrics**: Heap usage, garbage collection, thread count

### Business Metrics

- **Active Debates**: Number of active debates
- **LLM Requests**: Number of LLM API calls
- **Token Usage**: Token consumption by provider
- **Organization Activity**: Requests by organization

## Grafana Dashboards

The system includes pre-configured dashboards:

1. **System Overview**: Host-level metrics
2. **Service Health**: Service status and health checks
3. **API Performance**: Request rates, latencies, and errors
4. **LLM Usage**: Provider usage, token consumption, and costs
5. **Organization Activity**: Per-organization usage metrics

### Importing Dashboards

To import a custom dashboard:

1. Go to Grafana (http://localhost:3000)
2. Navigate to Dashboards > Import
3. Upload JSON file or paste dashboard JSON
4. Select Prometheus data source
5. Click Import

## Alert Configuration

Alerts are configured in Prometheus and routed through Alertmanager.

### Key Alerts

1. **Service Down**: Any service becomes unavailable
2. **High Error Rate**: Error rate exceeds threshold
3. **High Latency**: Response time exceeds threshold
4. **Disk Space Low**: Disk usage exceeds threshold
5. **Memory Usage High**: Memory usage exceeds threshold
6. **API Key Expiring**: LLM provider API key nearing expiration
7. **Rate Limit Approaching**: Nearing provider rate limits

### Alert Rules

Alert rules are defined in `monitoring/prometheus/rules/alerts.yml`:

```yaml
groups:
- name: service_alerts
  rules:
  - alert: ServiceDown
    expr: up{job=~"mcp-.*"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Service {{ $labels.job }} is down"
      description: "Service {{ $labels.job }} has been down for more than 1 minute."

  - alert: HighErrorRate
    expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "High error rate on {{ $labels.instance }}"
      description: "Error rate is above 5% for {{ $labels.job }} ({{ $value | printf \"%.2f\" }})"
```

### Notification Channels

Configure notification channels in Alertmanager:

```yaml
# monitoring/alertmanager/config.yml
route:
  group_by: ['alertname', 'job']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'team-email'

receivers:
- name: 'team-email'
  email_configs:
  - to: 'alerts@example.com'
    from: 'alertmanager@example.com'
    smarthost: 'smtp.example.com:587'
    auth_username: 'alertmanager'
    auth_password: 'password'

- name: 'slack'
  slack_configs:
  - api_url: 'https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX'
    channel: '#alerts'
    text: "{{ range .Alerts }}{{ .Annotations.summary }}\n{{ .Annotations.description }}\n{{ end }}"
```

## Log Monitoring

### Centralized Logging

For production environments, set up centralized logging:

```yaml
# Add to docker-compose.prod.yml
services:
  filebeat:
    image: docker.elastic.co/beats/filebeat:8.8.0
    volumes:
      - ./filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
    networks:
      - mcp-network
```

### Log Patterns to Monitor

1. **Error Logs**: `level=ERROR` or `level=FATAL`
2. **Authentication Failures**: Failed login attempts
3. **API Key Issues**: Invalid or expired API keys
4. **Rate Limiting**: Rate limit exceeded messages
5. **Slow Queries**: Database queries taking too long

## Performance Monitoring

### JVM Monitoring

Monitor JVM performance metrics:

- Heap usage
- Garbage collection frequency and duration
- Thread count and deadlocks
- Class loading

### Database Monitoring

Monitor PostgreSQL performance:

- Connection count
- Query performance
- Transaction rate
- Index usage

### Redis Monitoring

Monitor Redis performance:

- Memory usage
- Hit/miss ratio
- Connection count
- Eviction rate

## Cost Monitoring

### LLM API Cost Tracking

Track API usage costs:

```sql
-- Example query for token usage by organization
SELECT 
  organization_id,
  SUM(prompt_tokens) as total_prompt_tokens,
  SUM(completion_tokens) as total_completion_tokens,
  SUM(prompt_tokens + completion_tokens) as total_tokens
FROM llm_requests
WHERE created_at >= NOW() - INTERVAL '30 days'
GROUP BY organization_id
ORDER BY total_tokens DESC;
```

### Cost Alerting

Set up alerts for unusual cost patterns:

```yaml
- alert: UnusualTokenUsage
  expr: rate(llm_tokens_total[1h]) > 2 * avg_over_time(rate(llm_tokens_total[1h])[1d:1h])
  for: 15m
  labels:
    severity: warning
  annotations:
    summary: "Unusual token usage detected"
    description: "Token usage is significantly higher than the 24-hour average"
```

## Health Checks

Each service exposes health endpoints:

- `GET /actuator/health`: Overall service health
- `GET /actuator/health/liveness`: Service liveness
- `GET /actuator/health/readiness`: Service readiness

Monitor these endpoints for service health:

```yaml
- job_name: 'mcp-services-health'
  metrics_path: '/actuator/health'
  scrape_interval: 10s
  static_configs:
    - targets: ['mcp-organization:5005', 'mcp-llm:5002', 'mcp-controller:5013', 'mcp-rag:5004', 'mcp-template:5006']
```

## Custom Metrics

### Adding Custom Metrics

Add custom metrics to your services:

```java
@Component
public class MetricsService {
    private final MeterRegistry registry;
    
    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
    }
    
    public void recordTokenUsage(String provider, String model, int tokens) {
        registry.counter("llm.tokens.total", 
            "provider", provider, 
            "model", model).increment(tokens);
    }
}
```

### Exposing Custom Metrics

Custom metrics are automatically exposed at:

```
http://localhost:5002/actuator/prometheus
```

## Dashboards

### System Dashboard

![System Dashboard](../images/system-dashboard.png)

Key panels:
- CPU Usage
- Memory Usage
- Disk Usage
- Network Traffic

### Service Health Dashboard

![Service Health Dashboard](../images/service-health.png)

Key panels:
- Service Status
- Response Time
- Error Rate
- Request Rate

### LLM Usage Dashboard

![LLM Usage Dashboard](../images/llm-usage.png)

Key panels:
- Requests by Provider
- Token Usage by Model
- Cost by Organization
- Cache Hit Ratio

## Monitoring Best Practices

1. **Set Appropriate Thresholds**: Adjust alert thresholds based on normal patterns
2. **Reduce Alert Noise**: Avoid alert fatigue by tuning alert sensitivity
3. **Implement Runbooks**: Create runbooks for common alerts
4. **Regular Review**: Regularly review and update monitoring configuration
5. **Historical Analysis**: Analyze trends to predict future issues

## Capacity Planning

Use monitoring data for capacity planning:

1. **Resource Trending**: Track resource usage over time
2. **Growth Forecasting**: Project future resource needs
3. **Scaling Triggers**: Define when to scale services
4. **Cost Optimization**: Identify opportunities to optimize costs

## Troubleshooting with Monitoring

### Using Prometheus for Troubleshooting

1. **PromQL Queries**: Use Prometheus Query Language to investigate issues
2. **Time Range Analysis**: Compare metrics before and during incidents
3. **Correlation**: Correlate metrics across services
4. **Alert History**: Review past alerts for patterns

### Example PromQL Queries

```
# Error rate by service
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service) / 
sum(rate(http_server_requests_seconds_count[5m])) by (service)

# 95th percentile response time
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, service))

# JVM memory usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

## Setting Up Monitoring in Production

For production environments:

1. **Persistent Storage**: Configure persistent storage for Prometheus
2. **High Availability**: Set up Prometheus in HA mode
3. **Retention Policy**: Configure appropriate data retention
4. **Access Control**: Secure access to monitoring tools
5. **Backup Monitoring Data**: Regularly backup monitoring data

## Monitoring Configuration

### Prometheus Configuration

```yaml
# monitoring/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules/alerts.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'mcp-services'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
    static_configs:
      - targets: ['mcp-organization:5005', 'mcp-llm:5002', 'mcp-controller:5013', 'mcp-rag:5004', 'mcp-template:5006']
```

### Grafana Configuration

```yaml
# monitoring/grafana/provisioning/datasources/datasource.yml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

## Conclusion

Effective monitoring is essential for maintaining the health and performance of the Zamaz Debate MCP system. By following this guide, you can set up comprehensive monitoring, alerting, and visualization to ensure system reliability and performance.
