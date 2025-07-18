# Business Metrics and SLA Tracking

This directory contains the complete business metrics monitoring and SLA tracking implementation for the MCP Debate System, providing comprehensive visibility into business performance, user engagement, and service quality.

## Overview

The business metrics system provides:

- **Business KPIs**: Debate creation rates, user engagement, revenue tracking
- **SLA Monitoring**: Availability, performance, and quality metrics
- **Cost Tracking**: LLM usage costs, infrastructure efficiency
- **Predictive Analytics**: Capacity planning and growth trend analysis
- **Intelligent Alerting**: Business-impact-aware notifications

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Business      │    │   SLA Tracker   │    │   Cost         │
│   Metrics       │────│   Service       │────│   Analytics    │
│   Collector     │    │                 │    │                │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                ┌────────────────▼────────────────┐
                │         Prometheus              │
                │      (Metrics Storage)          │
                └────────────────┬────────────────┘
                                 │
                ┌────────────────▼────────────────┐
                │          Grafana               │
                │    (Business Dashboards)       │
                └────────────────┬────────────────┘
                                 │
                ┌────────────────▼────────────────┐
                │       AlertManager             │
                │  (Business-aware Alerting)     │
                └─────────────────────────────────┘
```

## Components

### 1. Business Metrics Collector
- **Purpose**: Collects and processes business metrics from all services
- **Features**: Real-time aggregation, batch processing, intelligent caching
- **Metrics**: User engagement, debate analytics, revenue tracking

### 2. SLA Tracking Service
- **Purpose**: Monitors service level agreements and business impact
- **Features**: Multi-tier SLA definitions, automated escalation, compliance reporting
- **SLAs**: Availability (99.9%), Response time (<2s), Error rates (<1%)

### 3. Cost Analytics Engine
- **Purpose**: Tracks LLM usage costs and infrastructure efficiency
- **Features**: Real-time cost calculation, optimization recommendations
- **Tracking**: Token usage, provider costs, efficiency ratios

### 4. Intelligent Alerting
- **Purpose**: Business-context-aware alert routing and escalation
- **Features**: Impact-based prioritization, automatic escalation, stakeholder notifications
- **Channels**: Slack, Email, PagerDuty, SMS

## Key Metrics

### Business KPIs

#### Debate Metrics
- **Creation Rate**: Debates created per minute
- **Completion Rate**: Successfully completed debates
- **Engagement Score**: Composite metric (messages, duration, satisfaction)
- **Success Ratio**: Percentage of successful debate completions

#### User Engagement
- **Active Users**: Users active in last 5 minutes
- **Retention Rate**: Weekly user retention percentage
- **Conversion Rate**: New user to first debate conversion
- **Growth Rate**: Weekly user growth

#### Revenue Metrics
- **Revenue per User**: Hourly revenue per active user
- **Revenue per Organization**: Organization-level revenue tracking
- **Subscription Metrics**: Plan upgrades, downgrades, churn
- **Lifetime Value**: Customer lifetime value calculations

### Technical SLAs

#### Availability SLA
- **Target**: 99.9% uptime
- **Measurement**: 5-minute rolling window
- **Escalation**: Immediate for <99.9%

#### Performance SLA
- **P95 Response Time**: <2000ms
- **P99 Response Time**: <5000ms
- **Measurement**: 5-minute rolling window
- **Escalation**: Warning at >1500ms, critical at >2000ms

#### Quality SLA
- **Error Rate**: <1% of total requests
- **LLM Quality**: >4.0/5.0 average rating
- **Safety Score**: >4.8/5.0 safety rating

### Cost Metrics

#### LLM Costs
- **Cost per Request**: Average cost per LLM API call
- **Cost per Debate**: Total LLM cost for debate completion
- **Token Efficiency**: Output/input token ratio
- **Provider Comparison**: Cost analysis across providers

#### Infrastructure Efficiency
- **Context Cache Hit Rate**: Percentage of cache hits
- **Context Optimization**: Compression ratio achieved
- **Resource Utilization**: CPU, memory, storage efficiency

## Configuration

### SLA Definitions

```yaml
sla_definitions:
  availability:
    target: 99.9
    measurement_window: "5m"
    evaluation_window: "1h"
    grace_period: "2m"
    
  response_time:
    p95_target_ms: 2000
    p99_target_ms: 5000
    measurement_window: "5m"
    evaluation_window: "15m"
    
  llm_quality:
    error_rate_threshold: 5.0
    response_quality_threshold: 4.0
    measurement_window: "10m"
    evaluation_window: "1h"
```

### Service Tiers

```yaml
service_tiers:
  gold:
    availability_target: 99.95
    response_time_p95_ms: 500
    response_time_p99_ms: 1000
    
  silver:
    availability_target: 99.9
    response_time_p95_ms: 1000
    response_time_p99_ms: 2500
    
  bronze:
    availability_target: 99.5
    response_time_p95_ms: 2000
    response_time_p99_ms: 5000
```

### Alert Routing

```yaml
alerting:
  escalation_rules:
    - condition: "severity == 'critical' && impact == 'business'"
      escalate_after: "0m"
      notify: ["ceo", "cto", "sre-oncall"]
    - condition: "sla_type == 'availability'"
      escalate_after: "5m"
      notify: ["sre-team", "platform-leads"]
    - condition: "impact == 'revenue'"
      escalate_after: "2m"
      notify: ["billing-team", "leadership"]
```

## Deployment

### Quick Start

1. **Deploy the monitoring stack**:
   ```bash
   kubectl apply -k k8s/monitoring/business-metrics/
   ```

2. **Verify deployment**:
   ```bash
   kubectl get pods -n monitoring -l component=business-metrics
   kubectl get servicemonitors -n monitoring
   ```

3. **Access dashboards**:
   - Business Metrics: `https://grafana.mcp-debate.com/d/mcp-business-metrics`
   - SLA Dashboard: `https://grafana.mcp-debate.com/d/mcp-sla-tracking`

### Manual Deployment

1. **Deploy Prometheus rules**:
   ```bash
   kubectl apply -f prometheus-rules.yaml
   ```

2. **Deploy service monitors**:
   ```bash
   kubectl apply -f service-monitors.yaml
   ```

3. **Deploy SLA tracking service**:
   ```bash
   kubectl apply -f sla-tracking-service.yaml
   ```

4. **Deploy business metrics collector**:
   ```bash
   kubectl apply -f business-metrics-collector.yaml
   ```

5. **Configure AlertManager**:
   ```bash
   kubectl apply -f alertmanager-config.yaml
   ```

## Dashboards

### Business Metrics Dashboard

The Grafana dashboard provides:

- **KPI Overview**: Platform health score, debate creation rate, active users
- **SLA Monitoring**: Real-time availability and performance tracking
- **LLM Analytics**: Request rates, error rates, token consumption by provider
- **Organization Insights**: Activity by size, revenue by tier
- **Cost Analysis**: LLM costs, efficiency metrics

### Key Visualizations

1. **Platform Health Score**: Composite metric (0-100)
2. **SLA Compliance**: Current vs. target with trend analysis
3. **Business Trends**: Growth rates, engagement metrics
4. **Cost Optimization**: Efficiency opportunities and recommendations

## Alerting

### Alert Categories

#### Critical Business Impact
- **Triggers**: Revenue loss, major user impact, SLA violations
- **Response**: Immediate escalation to leadership and SRE
- **Channels**: PagerDuty, Slack, Email, SMS

#### Performance Degradation
- **Triggers**: Response time SLA violations, high error rates
- **Response**: Platform team notification, automatic scaling
- **Channels**: Slack, Email

#### Cost Anomalies
- **Triggers**: Unexpected cost spikes, quota exhaustion
- **Response**: Finance and engineering team notification
- **Channels**: Slack, Email

#### Capacity Planning
- **Triggers**: High utilization, growth rate changes
- **Response**: Capacity planning team notification
- **Channels**: Email, Weekly reports

### Alert Escalation

```
Level 1: Automated response (scaling, failover)
Level 2: On-call engineer (5 minutes)
Level 3: Team leads (15 minutes)
Level 4: Management (30 minutes)
Level 5: Executive team (1 hour)
```

## Business Intelligence

### Daily Reports

Automated daily reports include:
- Platform health summary
- Key business metrics trends
- SLA compliance status
- Cost analysis and recommendations

### Weekly Reports

Weekly business reviews cover:
- User growth and engagement trends
- Revenue performance and forecasts
- Infrastructure efficiency analysis
- Competitive analysis insights

### Monthly Reports

Monthly executive reports feature:
- Business performance dashboard
- Strategic metric trends
- Cost optimization achievements
- Capacity planning projections

## API Integration

### Metrics API

The business metrics collector exposes REST APIs:

```bash
# Get current business metrics
GET /api/v1/metrics/business

# Get SLA status
GET /api/v1/sla/status

# Get cost analysis
GET /api/v1/costs/analysis

# Get organization insights
GET /api/v1/organizations/{id}/insights
```

### Webhook Integration

Supports webhook notifications for:
- Real-time business events
- SLA violation notifications
- Cost threshold alerts
- Custom business rule triggers

## Security and Compliance

### Data Protection
- **Encryption**: All metrics data encrypted at rest and in transit
- **Access Control**: Role-based access to business metrics
- **Audit Trail**: Complete audit log of metric access and modifications
- **Data Retention**: Configurable retention policies (default: 90 days)

### Privacy Compliance
- **GDPR Compliance**: User data anonymization and deletion capabilities
- **Data Minimization**: Only necessary business metrics collected
- **Consent Management**: User consent tracking for analytics

### Security Monitoring
- **Anomaly Detection**: Unusual pattern detection in business metrics
- **Access Monitoring**: Tracking of metric dashboard access
- **Threat Detection**: Business metric manipulation detection

## Troubleshooting

### Common Issues

#### Missing Metrics
```bash
# Check service monitors
kubectl get servicemonitors -n monitoring

# Verify Prometheus targets
kubectl port-forward -n monitoring svc/prometheus 9090:9090
# Access http://localhost:9090/targets

# Check collector logs
kubectl logs -n monitoring deployment/mcp-business-metrics-collector
```

#### SLA Tracking Issues
```bash
# Check SLA tracker status
kubectl get pods -n monitoring -l app=mcp-sla-tracker

# Review SLA configuration
kubectl get configmap -n monitoring sla-tracker-config -o yaml

# Check database connectivity
kubectl exec -n monitoring deployment/mcp-sla-tracker -- pg_isready
```

#### Alert Delivery Problems
```bash
# Check AlertManager status
kubectl get pods -n monitoring -l app=alertmanager

# Review alert routing
kubectl get configmap -n monitoring alertmanager-config -o yaml

# Test notification channels
kubectl exec -n monitoring deployment/alertmanager -- amtool config routes test
```

### Performance Optimization

#### Metrics Collection
- **Batch Size**: Increase for high-volume environments
- **Collection Interval**: Balance between freshness and load
- **Cache Configuration**: Optimize Redis cache settings

#### Dashboard Performance
- **Query Optimization**: Use recording rules for complex queries
- **Time Range Limits**: Set appropriate default time ranges
- **Concurrent Users**: Scale Grafana for multiple users

## Cost Optimization

### LLM Cost Management
- **Provider Comparison**: Real-time cost analysis across providers
- **Token Optimization**: Context compression and caching
- **Usage Quotas**: Automated quota management and alerts

### Infrastructure Efficiency
- **Resource Right-sizing**: Automated recommendations
- **Scaling Optimization**: Predictive scaling based on business metrics
- **Storage Optimization**: Intelligent data lifecycle management

## Future Enhancements

### Machine Learning Integration
- **Predictive Analytics**: Business trend forecasting
- **Anomaly Detection**: Automated business metric anomaly detection
- **Optimization Recommendations**: AI-powered cost and performance optimization

### Advanced Reporting
- **Custom Dashboards**: Self-service business intelligence
- **Executive Reporting**: Automated C-level reporting
- **Competitive Intelligence**: Market trend analysis integration

### Real-time Processing
- **Stream Processing**: Real-time business event processing
- **Edge Analytics**: Distributed metrics collection
- **Event-driven Scaling**: Business metric-driven infrastructure scaling

## Support

### Documentation
- [Business Metrics API Reference](../../../docs/api/business-metrics.md)
- [SLA Definition Guide](../../../docs/operations/sla-definitions.md)
- [Cost Optimization Playbook](../../../docs/operations/cost-optimization.md)

### Monitoring
- [Grafana Dashboards](https://grafana.mcp-debate.com)
- [Alert Status](https://alertmanager.mcp-debate.com)
- [SLA Reports](https://reports.mcp-debate.com/sla)

### Contact
- **Business Metrics Team**: business-metrics@mcp-debate.com
- **SRE Team**: sre@mcp-debate.com
- **On-call Support**: +1-XXX-XXX-XXXX