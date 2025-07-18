# MCP Debate System - Service Mesh

This directory contains the complete Istio service mesh configuration for the MCP Debate System, providing advanced traffic management, security, and observability features.

## Overview

The service mesh implementation includes:

- **Traffic Management**: Intelligent routing, load balancing, and circuit breaking
- **Security**: mTLS encryption, authentication, and authorization policies
- **Observability**: Comprehensive metrics, distributed tracing, and access logging
- **Resilience**: Automatic retries, timeouts, and fault injection capabilities

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Internet      │────│  Istio Gateway  │────│   Load Balancer │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
        ┌───────▼───────┐ ┌─────▼─────┐ ┌─────▼─────┐
        │ mcp-gateway   │ │mcp-debate │ │ mcp-llm   │
        │ (API Gateway) │ │ (Engine)  │ │(Provider) │
        └───────────────┘ └───────────┘ └───────────┘
                │               │               │
        ┌───────▼───────┐ ┌─────▼─────┐ ┌─────▼─────┐
        │mcp-organization│ │mcp-context│ │  mcp-rag  │
        │  (Multi-tenant)│ │(Context)  │ │   (RAG)   │
        └───────────────┘ └───────────┘ └───────────┘
                                │
                        ┌───────▼───────┐
                        │   External    │
                        │   Services    │
                        │(AI Providers) │
                        └───────────────┘
```

## Directory Structure

```
k8s/service-mesh/
├── istio/
│   └── installation.yaml              # Istio operator configuration
├── gateway/
│   └── mcp-gateway.yaml              # Ingress/egress gateways
├── virtual-services/
│   └── mcp-virtual-services.yaml     # Traffic routing rules
├── destination-rules/
│   └── mcp-destination-rules.yaml    # Load balancing & circuit breaking
├── service-entries/
│   └── external-services.yaml        # External service definitions
├── security/
│   ├── authentication-policies.yaml  # mTLS and JWT authentication
│   └── authorization-policies.yaml   # Access control policies
├── telemetry/
│   └── telemetry-config.yaml        # Metrics, tracing, logging
├── patches/
│   └── production-patches.yaml       # Production-specific overrides
├── scripts/
│   └── deploy-service-mesh.sh        # Automated deployment script
├── kustomization.yaml                # Kustomize configuration
└── README.md                         # This file
```

## Quick Start

### Prerequisites

1. **Kubernetes Cluster**: Running cluster with adequate resources
2. **kubectl**: Configured to access your cluster
3. **istioctl**: Istio command-line tool (v1.20.1+)
4. **kustomize**: For configuration management (optional)

### Installation

1. **Deploy the complete service mesh**:
   ```bash
   cd k8s/service-mesh
   ./scripts/deploy-service-mesh.sh
   ```

2. **Verify installation**:
   ```bash
   kubectl get pods -n istio-system
   kubectl get gateway,virtualservice,destinationrule -n production
   istioctl proxy-status
   ```

3. **Check configuration**:
   ```bash
   istioctl analyze -n production
   ```

### Manual Deployment

If you prefer step-by-step deployment:

1. **Install Istio**:
   ```bash
   kubectl apply -f istio/installation.yaml
   kubectl wait --for=condition=ready pod -l app=istiod -n istio-system --timeout=600s
   ```

2. **Deploy gateways**:
   ```bash
   kubectl apply -f gateway/mcp-gateway.yaml
   ```

3. **Configure traffic management**:
   ```bash
   kubectl apply -f virtual-services/mcp-virtual-services.yaml
   kubectl apply -f destination-rules/mcp-destination-rules.yaml
   kubectl apply -f service-entries/external-services.yaml
   ```

4. **Apply security policies**:
   ```bash
   kubectl apply -f security/authentication-policies.yaml
   kubectl apply -f security/authorization-policies.yaml
   ```

5. **Enable telemetry**:
   ```bash
   kubectl apply -f telemetry/telemetry-config.yaml
   ```

## Configuration Details

### Traffic Management

#### Virtual Services
- **Route-based traffic splitting**: Direct traffic to appropriate services
- **Canary deployments**: Support for gradual rollouts
- **Retry policies**: Automatic retry for failed requests
- **Timeout configuration**: Service-specific timeout settings

#### Destination Rules
- **Load balancing**: Round-robin, least connection, consistent hash
- **Circuit breaking**: Prevent cascade failures
- **Connection pooling**: Optimize resource usage
- **Outlier detection**: Automatic unhealthy instance removal

#### Service Entries
- **External AI providers**: OpenAI, Anthropic, Google AI
- **Monitoring services**: Prometheus, Grafana, Jaeger
- **Package repositories**: PyPI, NPM, Docker Hub
- **Cloud services**: AWS, GCP services

### Security

#### Authentication
- **mTLS**: Automatic mutual TLS between services
- **JWT validation**: API token verification
- **Multi-issuer support**: Google OAuth, internal auth
- **API key authentication**: For service-to-service calls

#### Authorization
- **Role-based access**: Organization owners, moderators, users
- **Resource-level permissions**: Debate-specific access control
- **Service-to-service policies**: Inter-service communication rules
- **Rate limiting**: Prevent abuse and ensure fair usage

### Observability

#### Metrics
- **Business metrics**: Debate creation, LLM usage, user activity
- **Infrastructure metrics**: Request rates, error rates, latencies
- **Custom metrics**: Token usage, cache hit rates, organization activity
- **Security metrics**: Authentication attempts, authorization failures

#### Distributed Tracing
- **Request correlation**: Track requests across all services
- **Performance analysis**: Identify bottlenecks and slow operations
- **Error tracking**: Correlate errors with specific requests
- **Custom spans**: Business logic tracing

#### Access Logging
- **Structured logs**: JSON format for easy parsing
- **Request context**: User, organization, debate information
- **Performance data**: Response times, payload sizes
- **Security events**: Authentication and authorization events

## Production Considerations

### Resource Requirements

#### Istio Control Plane
- **CPU**: 1-2 cores (pilot)
- **Memory**: 4-8 GB (pilot)
- **Replicas**: 3+ for high availability

#### Ingress Gateway
- **CPU**: 0.5-4 cores per replica
- **Memory**: 512MB-2GB per replica
- **Replicas**: 5+ for production load

#### Sidecar Proxies
- **CPU**: 100m-200m per service instance
- **Memory**: 128MB-256MB per service instance

### Networking

#### External Access
- **Load Balancer**: Cloud provider load balancer for ingress
- **SSL Termination**: TLS certificates managed by cert-manager
- **DNS Configuration**: Point domains to load balancer IP

#### Internal Communication
- **Service Discovery**: Automatic via Kubernetes DNS
- **Load Balancing**: Configurable algorithms per service
- **Circuit Breaking**: Prevent cascade failures

### Security Hardening

#### Network Policies
- **Zero Trust**: Deny all by default, explicit allow policies
- **Namespace Isolation**: Strict inter-namespace communication
- **External Access**: Controlled egress to external services

#### Certificate Management
- **Automatic Rotation**: Istio manages internal certificates
- **External Certificates**: Use cert-manager for public-facing TLS
- **SPIFFE/SPIRE**: Optional identity framework integration

## Monitoring and Troubleshooting

### Health Checks

```bash
# Check Istio control plane
kubectl get pods -n istio-system

# Verify proxy configuration
istioctl proxy-status

# Check configuration issues
istioctl analyze -n production

# Verify mTLS status
istioctl authn tls-check mcp-gateway.production.svc.cluster.local
```

### Debugging

#### Common Commands
```bash
# View proxy configuration
istioctl proxy-config cluster mcp-gateway-pod-name.production

# Check virtual service routing
istioctl proxy-config route mcp-gateway-pod-name.production

# Inspect listeners
istioctl proxy-config listener mcp-gateway-pod-name.production

# View security policies
istioctl proxy-config authz mcp-gateway-pod-name.production
```

#### Log Analysis
```bash
# Istio control plane logs
kubectl logs -n istio-system deployment/istiod

# Service sidecar logs
kubectl logs -n production deployment/mcp-gateway -c istio-proxy

# Access logs
kubectl logs -n istio-system deployment/istio-ingressgateway
```

### Performance Tuning

#### Pilot Configuration
- **Trace sampling**: Adjust based on volume (0.1% for production)
- **Push optimization**: Configure for cluster size
- **Resource allocation**: Scale based on service count

#### Proxy Configuration
- **Concurrency**: Match to service requirements
- **Circuit breaker**: Tune based on service capacity
- **Connection pooling**: Optimize for service characteristics

## Upgrading

### Istio Upgrades
1. **Canary upgrade**: Deploy new control plane alongside existing
2. **Data plane migration**: Gradually update sidecar proxies
3. **Configuration validation**: Ensure compatibility
4. **Rollback plan**: Maintain previous version capability

### Configuration Updates
1. **Gradual rollout**: Update one service at a time
2. **Validation**: Use `istioctl analyze` before applying
3. **Monitoring**: Watch metrics during updates
4. **Rollback**: Keep previous configurations available

## Integration with CI/CD

### GitOps Workflow
```yaml
# Example CI/CD integration
deploy-service-mesh:
  stage: deploy
  script:
    - istioctl analyze k8s/service-mesh/
    - kubectl apply -k k8s/service-mesh/
    - ./k8s/service-mesh/scripts/verify-deployment.sh
  environment:
    name: production
```

### Canary Deployments
- **Traffic splitting**: Gradual percentage-based rollouts
- **Automated validation**: Health checks and metrics validation
- **Automatic rollback**: On failure detection
- **Blue-green support**: Full environment switching

## Support and Documentation

### Additional Resources
- [Istio Documentation](https://istio.io/latest/docs/)
- [MCP Architecture Guide](../../docs/architecture/ARCHITECTURE.md)
- [Security Guidelines](../../docs/security/SECURITY.md)
- [Monitoring Setup](../../docs/operations/monitoring.md)

### Getting Help
- **Issues**: Create GitHub issues for bugs or feature requests
- **Discussions**: Use GitHub discussions for questions
- **Support**: Check documentation and troubleshooting guides

## License

This service mesh configuration is part of the MCP Debate System and follows the same licensing terms as the main project.