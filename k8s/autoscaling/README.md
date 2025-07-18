# Auto-scaling and Load Balancing

This directory contains comprehensive auto-scaling and load balancing configurations for the MCP Debate System, providing intelligent resource management, cost optimization, and high availability.

## Overview

The auto-scaling system provides:

- **Horizontal Pod Autoscaling (HPA)**: Reactive scaling based on CPU, memory, and custom metrics
- **Vertical Pod Autoscaling (VPA)**: Automatic resource request/limit optimization
- **Cluster Autoscaling**: Automatic node provisioning and deprovisioning
- **Predictive Scaling**: ML-powered proactive scaling based on business patterns
- **Intelligent Load Balancing**: Advanced traffic distribution and health management

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Predictive    │    │   Horizontal    │    │   Vertical      │
│   Scaler        │────│   Pod          │────│   Pod           │
│   (ML-based)    │    │   Autoscaler    │    │   Autoscaler    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                ┌────────────────▼────────────────┐
                │      Cluster Autoscaler         │
                │    (Node-level scaling)         │
                └────────────────┬────────────────┘
                                 │
                ┌────────────────▼────────────────┐
                │       Load Balancers           │
                │   (Traffic distribution)       │
                └─────────────────────────────────┘
```

## Components

### 1. Horizontal Pod Autoscaling (HPA)
- **Reactive scaling** based on real-time metrics
- **Multi-metric support** (CPU, memory, custom business metrics)
- **Service-specific tuning** for different workload characteristics
- **Advanced behaviors** with configurable scale-up/down policies

### 2. Vertical Pod Autoscaling (VPA)
- **Automatic resource optimization** for containers
- **Historical analysis** for right-sizing recommendations
- **Multi-container support** including sidecar containers
- **Update policies** for safe resource adjustments

### 3. Cluster Autoscaling
- **Node-level scaling** based on pod scheduling needs
- **Multi-node group support** for different instance types
- **Cost optimization** through spot instance integration
- **Intelligent expander policies** for optimal node selection

### 4. Predictive Scaling
- **ML-powered predictions** based on historical patterns
- **Business-aware scaling** considering organizational activity
- **Time-series forecasting** for proactive resource provisioning
- **Confidence-based decisions** to avoid unnecessary scaling

### 5. Load Balancing
- **Application Load Balancer (ALB)** for HTTP/HTTPS traffic
- **Network Load Balancer (NLB)** for high-performance TCP traffic
- **gRPC load balancing** for inter-service communication
- **Health-aware routing** with automatic failover

## Configuration Details

### HPA Scaling Targets

#### API Gateway (mcp-gateway)
```yaml
minReplicas: 3
maxReplicas: 20
metrics:
  - CPU: 70%
  - Memory: 80%
  - Requests/sec: 100
  - Business load: 50 debates/min
```

#### LLM Service (mcp-llm)
```yaml
minReplicas: 2
maxReplicas: 8
metrics:
  - CPU: 60% (lower for AI workloads)
  - Memory: 70%
  - Queue depth: 10 requests
  - Response time P95: 5 seconds
```

#### Context Service (mcp-context)
```yaml
minReplicas: 3
maxReplicas: 15
metrics:
  - CPU: 70%
  - Memory: 75%
  - Processing queue: 50 items
  - Cache miss rate: 20%
```

### VPA Resource Policies

#### Gateway Service
```yaml
minAllowed:
  cpu: 100m
  memory: 256Mi
maxAllowed:
  cpu: 2000m
  memory: 4Gi
```

#### LLM Service (Resource-intensive)
```yaml
minAllowed:
  cpu: 500m
  memory: 1Gi
maxAllowed:
  cpu: 4000m
  memory: 8Gi
```

### Cluster Autoscaler Node Groups

#### General Purpose Nodes
- **Instance Types**: m5.large, m5.xlarge
- **Min Size**: 3 nodes
- **Max Size**: 20 nodes
- **Use Case**: Standard API and web services

#### Compute-Optimized Nodes
- **Instance Types**: c5.2xlarge, c5.4xlarge
- **Min Size**: 1 node
- **Max Size**: 10 nodes
- **Use Case**: LLM inference workloads

#### Memory-Optimized Nodes
- **Instance Types**: r5.xlarge, r5.2xlarge
- **Min Size**: 1 node
- **Max Size**: 8 nodes
- **Use Case**: Context processing and caching

#### Spot Instance Nodes
- **Instance Types**: m5.large (spot)
- **Min Size**: 0 nodes
- **Max Size**: 15 nodes
- **Use Case**: Batch processing and cost optimization

### Predictive Scaling Features

#### Business Pattern Recognition
- **Time-based patterns**: Business hours, weekdays vs weekends
- **Event-driven scaling**: Product launches, marketing campaigns
- **Seasonal adjustments**: Holiday traffic, academic calendars
- **Organization growth**: User acquisition and engagement trends

#### ML Model Features
- **Historical metrics**: Past 30 days of usage patterns
- **Time series data**: Hourly, daily, weekly cycles
- **Business metrics**: Debate creation rate, user activity
- **External factors**: Marketing events, system updates

#### Scaling Algorithms
- **Linear Regression**: Simple trend-based predictions
- **Random Forest**: Complex pattern recognition
- **Neural Networks**: Deep learning for non-linear patterns
- **Time Series Models**: ARIMA, Prophet for seasonal data

## Load Balancing Strategy

### External Load Balancing

#### Application Load Balancer (ALB)
- **Host-based routing**: api.mcp-debate.com, admin.mcp-debate.com
- **Path-based routing**: /api/v1/*, /admin/*, /monitoring/*
- **SSL termination**: Certificate management with ACM
- **WAF integration**: Web Application Firewall protection

#### Network Load Balancer (NLB)
- **High performance**: TCP load balancing for API traffic
- **Connection persistence**: Session affinity for stateful services
- **Health checks**: Deep health monitoring
- **Cross-zone balancing**: Even distribution across AZs

### Internal Load Balancing

#### Service Mesh Integration
- **Istio-based routing**: Advanced traffic management
- **Circuit breaking**: Automatic failover protection
- **Retry policies**: Intelligent request retry logic
- **Canary deployments**: Safe progressive rollouts

#### Database Load Balancing
- **Read replica routing**: Intelligent read/write splitting
- **Connection pooling**: Efficient connection management
- **Health-aware routing**: Automatic unhealthy instance removal

## Node Affinity and Anti-Affinity

### Service-Specific Placement

#### API Gateway
- **Node preference**: General purpose instances
- **Zone distribution**: Required across all AZs
- **Co-location**: Avoid clustering on single nodes

#### LLM Services
- **Node requirement**: Compute-optimized instances only
- **Dedicated nodes**: Tainted nodes for LLM workloads
- **Anti-affinity**: Never co-locate LLM instances

#### Database Services
- **Node requirement**: Memory-optimized or dedicated DB nodes
- **Storage requirement**: SSD/NVMe storage
- **Strict anti-affinity**: Never co-locate database replicas

### Cost Optimization Rules

#### Spot Instance Usage
- **Batch workloads**: Prefer spot instances
- **Fault tolerance**: Handle spot interruptions gracefully
- **Mixed instance types**: Combine on-demand and spot

#### Right-sizing
- **Resource analysis**: Historical usage patterns
- **Waste elimination**: Remove over-provisioned resources
- **Performance monitoring**: Ensure quality isn't compromised

## Deployment

### Quick Start

1. **Deploy autoscaling components**:
   ```bash
   kubectl apply -k k8s/autoscaling/
   ```

2. **Verify autoscaler installation**:
   ```bash
   kubectl get hpa -n production
   kubectl get vpa -n production
   kubectl get pods -n kube-system -l app=cluster-autoscaler
   ```

3. **Check scaling behavior**:
   ```bash
   kubectl describe hpa mcp-gateway-hpa -n production
   kubectl top pods -n production
   ```

### Manual Deployment

1. **Deploy HPA configurations**:
   ```bash
   kubectl apply -f horizontal-pod-autoscalers.yaml
   ```

2. **Deploy VPA configurations**:
   ```bash
   kubectl apply -f vertical-pod-autoscalers.yaml
   ```

3. **Deploy cluster autoscaler**:
   ```bash
   kubectl apply -f cluster-autoscaler.yaml
   ```

4. **Deploy load balancer configs**:
   ```bash
   kubectl apply -f load-balancer-config.yaml
   ```

5. **Deploy predictive scaling**:
   ```bash
   kubectl apply -f predictive-scaling.yaml
   ```

6. **Apply node affinity rules**:
   ```bash
   kubectl apply -f node-affinity-rules.yaml
   ```

## Monitoring and Observability

### Scaling Metrics

#### HPA Metrics
- **Current replicas**: Real-time pod count
- **Target replicas**: Desired pod count based on metrics
- **Resource utilization**: CPU, memory usage percentages
- **Custom metrics**: Business-specific scaling triggers

#### VPA Metrics
- **Resource recommendations**: CPU/memory suggestions
- **Update events**: Resource limit adjustments
- **Recommendation accuracy**: Historical accuracy tracking

#### Cluster Autoscaler Metrics
- **Node scaling events**: Scale-up/down operations
- **Node utilization**: Resource usage across nodes
- **Pending pods**: Pods waiting for node capacity
- **Cost impact**: Spending changes from scaling decisions

### Scaling Dashboards

#### Autoscaling Overview
- **System-wide scaling status**
- **Resource utilization trends**
- **Cost impact analysis**
- **Scaling event timeline**

#### Service-Specific Views
- **Per-service scaling behavior**
- **Resource efficiency metrics**
- **Performance impact analysis**
- **Scaling recommendation accuracy**

#### Predictive Analytics
- **Prediction accuracy tracking**
- **Model performance metrics**
- **Business pattern recognition**
- **Cost savings from predictive scaling**

## Cost Optimization

### Intelligent Instance Selection

#### Instance Type Optimization
- **Workload matching**: Right instance type for workload
- **Cost-performance ratio**: Optimal price/performance balance
- **Reserved instance usage**: Long-term capacity planning
- **Spot instance integration**: Cost savings for fault-tolerant workloads

#### Resource Right-Sizing
- **Historical analysis**: Usage pattern identification
- **Waste elimination**: Remove unused resources
- **Performance monitoring**: Maintain service quality
- **Continuous optimization**: Regular re-evaluation

### Scaling Cost Management

#### Predictive Cost Control
- **Cost-aware scaling**: Factor cost into scaling decisions
- **Budget constraints**: Enforce spending limits
- **ROI optimization**: Balance performance and cost
- **Cost allocation**: Track spending by service/team

#### Efficiency Improvements
- **Load balancing efficiency**: Optimal traffic distribution
- **Resource utilization**: Maximize instance usage
- **Consolidation opportunities**: Reduce infrastructure footprint

## Business-Aware Scaling

### Activity Pattern Recognition

#### Time-Based Scaling
- **Business hours**: 8 AM - 6 PM increased capacity
- **Weekend patterns**: Reduced capacity for non-business days
- **Holiday adjustments**: Special event scaling
- **Time zone considerations**: Global user base support

#### Event-Driven Scaling
- **Marketing campaigns**: Anticipated traffic spikes
- **Product launches**: Coordinated capacity increases
- **Maintenance windows**: Planned scale-down during updates
- **Emergency scaling**: Rapid response to incidents

### Organization-Specific Scaling

#### Multi-Tenant Considerations
- **Per-organization quotas**: Resource limits by customer
- **Tier-based scaling**: Different SLAs for different plans
- **Growth patterns**: Scale based on customer expansion
- **Isolation requirements**: Ensure tenant separation

## Security Considerations

### Access Control

#### RBAC for Autoscaling
- **Principle of least privilege**: Minimal required permissions
- **Service account separation**: Dedicated accounts per component
- **Cross-namespace policies**: Secure inter-namespace communication

#### Scaling Security
- **Node security groups**: Network-level protection
- **Pod security policies**: Container-level security
- **Resource quotas**: Prevent resource exhaustion attacks
- **Audit logging**: Track all scaling decisions

### Network Security

#### Load Balancer Security
- **Security group rules**: Restrictive network access
- **SSL/TLS termination**: Encrypted traffic handling
- **DDoS protection**: AWS Shield integration
- **WAF rules**: Application-level protection

## Troubleshooting

### Common Scaling Issues

#### HPA Not Scaling
```bash
# Check HPA status
kubectl describe hpa mcp-gateway-hpa -n production

# Verify metrics availability
kubectl get --raw "/apis/metrics.k8s.io/v1beta1/pods" | jq

# Check resource usage
kubectl top pods -n production
```

#### VPA Not Updating
```bash
# Check VPA status
kubectl describe vpa mcp-gateway-vpa -n production

# Verify admission controller
kubectl get pods -n kube-system -l app=vpa-admission-controller

# Check recommendation
kubectl get vpa mcp-gateway-vpa -n production -o yaml
```

#### Cluster Autoscaler Issues
```bash
# Check autoscaler logs
kubectl logs -n kube-system deployment/cluster-autoscaler

# Verify node group discovery
kubectl get configmap cluster-autoscaler-status -n kube-system -o yaml

# Check pending pods
kubectl get pods --all-namespaces --field-selector=status.phase=Pending
```

### Performance Optimization

#### Scaling Responsiveness
- **Metric collection frequency**: Balance accuracy and overhead
- **Scaling cooldown periods**: Prevent thrashing
- **Threshold tuning**: Optimize trigger points
- **Prediction accuracy**: Improve ML model performance

#### Resource Efficiency
- **Node utilization**: Maximize resource usage
- **Pod density**: Optimal pods per node
- **Resource requests**: Accurate sizing
- **Waste elimination**: Identify and remove unused resources

## Best Practices

### Scaling Strategy
1. **Start conservative**: Begin with higher thresholds
2. **Monitor closely**: Watch scaling behavior initially
3. **Iterate gradually**: Make small adjustments
4. **Test thoroughly**: Validate scaling under load
5. **Document changes**: Track configuration evolution

### Resource Management
1. **Set appropriate limits**: Prevent resource hogging
2. **Use quality of service**: Prioritize critical workloads
3. **Monitor resource waste**: Identify optimization opportunities
4. **Plan for growth**: Design for future scale requirements

### Cost Management
1. **Track spending**: Monitor cost impact of scaling
2. **Use spot instances**: Reduce costs for fault-tolerant workloads
3. **Right-size regularly**: Adjust based on actual usage
4. **Implement budgets**: Set spending limits and alerts

## Future Enhancements

### Advanced ML Models
- **Deep learning**: More sophisticated pattern recognition
- **Ensemble methods**: Combine multiple prediction models
- **Real-time learning**: Adapt to changing patterns
- **External data**: Incorporate business and market data

### Multi-Cloud Support
- **Cloud-agnostic scaling**: Support AWS, GCP, Azure
- **Cross-cloud load balancing**: Distribute across providers
- **Cost comparison**: Optimize across cloud providers
- **Disaster recovery**: Multi-cloud failover capabilities

### Enhanced Business Integration
- **Revenue-based scaling**: Scale based on business value
- **Customer SLA**: Enforce per-customer performance guarantees
- **Predictive maintenance**: Anticipate infrastructure needs
- **Business impact analysis**: Quantify scaling decisions

## Support and Documentation

### Additional Resources
- [Kubernetes HPA Documentation](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)
- [VPA Documentation](https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler)
- [Cluster Autoscaler Guide](https://github.com/kubernetes/autoscaler/tree/master/cluster-autoscaler)
- [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)

### Monitoring and Alerts
- [Scaling Dashboard](https://grafana.mcp-debate.com/d/autoscaling-overview)
- [Cost Analysis](https://grafana.mcp-debate.com/d/cost-optimization)
- [Performance Impact](https://grafana.mcp-debate.com/d/scaling-performance)

### Contact Information
- **Platform Team**: platform@mcp-debate.com
- **SRE Team**: sre@mcp-debate.com
- **Cost Optimization**: finops@mcp-debate.com