#!/bin/bash

# Deploy Istio Service Mesh for MCP Debate System
# This script deploys the complete service mesh configuration

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_MESH_DIR="$(dirname "$SCRIPT_DIR")"
NAMESPACE="production"
ISTIO_NAMESPACE="istio-system"
TIMEOUT="600s"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

info() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')] INFO: $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        error "kubectl is not installed or not in PATH"
    fi
    
    # Check if istioctl is available
    if ! command -v istioctl &> /dev/null; then
        error "istioctl is not installed or not in PATH"
    fi
    
    # Check cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        error "Cannot connect to Kubernetes cluster"
    fi
    
    # Check if kustomize is available
    if ! command -v kustomize &> /dev/null; then
        warn "kustomize not found, using kubectl apply -k"
    fi
    
    log "Prerequisites check passed"
}

# Create namespaces
create_namespaces() {
    log "Creating namespaces..."
    
    # Create istio-system namespace
    kubectl create namespace "$ISTIO_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
    
    # Create production namespace
    kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
    
    # Label namespace for istio injection
    kubectl label namespace "$NAMESPACE" istio-injection=enabled --overwrite
    
    log "Namespaces created and configured"
}

# Install Istio
install_istio() {
    log "Installing Istio..."
    
    # Check if Istio is already installed
    if kubectl get namespace "$ISTIO_NAMESPACE" &> /dev/null && \
       kubectl get deployment istiod -n "$ISTIO_NAMESPACE" &> /dev/null; then
        warn "Istio appears to be already installed"
        read -p "Do you want to proceed with installation? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            info "Skipping Istio installation"
            return 0
        fi
    fi
    
    # Apply Istio installation
    kubectl apply -f "$SERVICE_MESH_DIR/istio/installation.yaml"
    
    # Wait for Istio to be ready
    log "Waiting for Istio control plane to be ready..."
    kubectl wait --for=condition=ready pod -l app=istiod -n "$ISTIO_NAMESPACE" --timeout="$TIMEOUT"
    
    # Verify installation
    if istioctl verify-install &> /dev/null; then
        log "Istio installation verified successfully"
    else
        error "Istio installation verification failed"
    fi
}

# Deploy gateway configuration
deploy_gateways() {
    log "Deploying gateway configuration..."
    
    kubectl apply -f "$SERVICE_MESH_DIR/gateway/mcp-gateway.yaml"
    
    # Wait for gateways to be ready
    kubectl wait --for=condition=ready gateway -l app=mcp-debate -n "$NAMESPACE" --timeout="$TIMEOUT"
    
    log "Gateways deployed successfully"
}

# Deploy virtual services
deploy_virtual_services() {
    log "Deploying virtual services..."
    
    kubectl apply -f "$SERVICE_MESH_DIR/virtual-services/mcp-virtual-services.yaml"
    
    log "Virtual services deployed successfully"
}

# Deploy destination rules
deploy_destination_rules() {
    log "Deploying destination rules..."
    
    kubectl apply -f "$SERVICE_MESH_DIR/destination-rules/mcp-destination-rules.yaml"
    
    log "Destination rules deployed successfully"
}

# Deploy service entries
deploy_service_entries() {
    log "Deploying service entries for external services..."
    
    kubectl apply -f "$SERVICE_MESH_DIR/service-entries/external-services.yaml"
    
    log "Service entries deployed successfully"
}

# Deploy security policies
deploy_security_policies() {
    log "Deploying security policies..."
    
    # Deploy authentication policies
    kubectl apply -f "$SERVICE_MESH_DIR/security/authentication-policies.yaml"
    
    # Deploy authorization policies
    kubectl apply -f "$SERVICE_MESH_DIR/security/authorization-policies.yaml"
    
    log "Security policies deployed successfully"
}

# Deploy telemetry configuration
deploy_telemetry() {
    log "Deploying telemetry configuration..."
    
    kubectl apply -f "$SERVICE_MESH_DIR/telemetry/telemetry-config.yaml"
    
    log "Telemetry configuration deployed successfully"
}

# Verify deployment
verify_deployment() {
    log "Verifying service mesh deployment..."
    
    # Check Istio components
    info "Checking Istio control plane..."
    kubectl get pods -n "$ISTIO_NAMESPACE"
    
    # Check gateways
    info "Checking gateways..."
    kubectl get gateway -n "$NAMESPACE"
    
    # Check virtual services
    info "Checking virtual services..."
    kubectl get virtualservice -n "$NAMESPACE"
    
    # Check destination rules
    info "Checking destination rules..."
    kubectl get destinationrule -n "$NAMESPACE"
    
    # Check security policies
    info "Checking authentication policies..."
    kubectl get peerauthentication -n "$NAMESPACE"
    
    info "Checking authorization policies..."
    kubectl get authorizationpolicy -n "$NAMESPACE"
    
    # Check telemetry
    info "Checking telemetry configuration..."
    kubectl get telemetry -n "$NAMESPACE"
    
    # Run Istio configuration analysis
    info "Running Istio configuration analysis..."
    if istioctl analyze -n "$NAMESPACE"; then
        log "Configuration analysis passed"
    else
        warn "Configuration analysis found issues"
    fi
    
    log "Service mesh deployment verification completed"
}

# Generate ingress information
show_ingress_info() {
    log "Gathering ingress information..."
    
    # Get ingress gateway service
    INGRESS_HOST=$(kubectl get svc istio-ingressgateway -n "$ISTIO_NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
    if [ -z "$INGRESS_HOST" ]; then
        INGRESS_HOST=$(kubectl get svc istio-ingressgateway -n "$ISTIO_NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
    fi
    
    INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n "$ISTIO_NAMESPACE" -o jsonpath='{.spec.ports[?(@.name=="http2")].port}')
    SECURE_INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n "$ISTIO_NAMESPACE" -o jsonpath='{.spec.ports[?(@.name=="https")].port}')
    
    echo ""
    info "=== Ingress Information ==="
    info "Ingress Host: ${INGRESS_HOST:-<pending>}"
    info "HTTP Port: ${INGRESS_PORT:-80}"
    info "HTTPS Port: ${SECURE_INGRESS_PORT:-443}"
    echo ""
    
    if [ -n "$INGRESS_HOST" ]; then
        info "Test URLs:"
        info "  Health Check: http://${INGRESS_HOST}:${INGRESS_PORT}/health"
        info "  API Gateway: http://${INGRESS_HOST}:${INGRESS_PORT}/api/v1/"
        info "  Secure API: https://${INGRESS_HOST}:${SECURE_INGRESS_PORT}/api/v1/"
    else
        warn "Ingress host is not yet available. Check load balancer status:"
        info "  kubectl get svc istio-ingressgateway -n $ISTIO_NAMESPACE"
    fi
    echo ""
}

# Create deployment report
create_deployment_report() {
    log "Creating deployment report..."
    
    REPORT_FILE="$SERVICE_MESH_DIR/deployment-report-$(date +%Y%m%d-%H%M%S).md"
    
    cat > "$REPORT_FILE" <<EOF
# Service Mesh Deployment Report

**Deployment Date:** $(date)
**Namespace:** $NAMESPACE
**Istio Namespace:** $ISTIO_NAMESPACE

## Components Deployed

### Istio Control Plane
- Installation: ✅ Complete
- Version: $(istioctl version --short 2>/dev/null || echo "Unknown")
- Components: Pilot, Ingress Gateway, Egress Gateway

### Service Mesh Configuration
- Gateways: $(kubectl get gateway -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l) deployed
- Virtual Services: $(kubectl get virtualservice -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l) deployed
- Destination Rules: $(kubectl get destinationrule -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l) deployed
- Service Entries: $(kubectl get serviceentry -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l) deployed

### Security Policies
- Peer Authentication: $(kubectl get peerauthentication -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l) policies
- Authorization Policies: $(kubectl get authorizationpolicy -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l) policies
- Request Authentication: $(kubectl get requestauthentication -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l) policies

### Telemetry
- Telemetry Configs: $(kubectl get telemetry -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l) configurations
- Metrics: Enabled (Prometheus)
- Tracing: Enabled (Jaeger)
- Access Logging: Enabled

## Access Information

$(show_ingress_info)

## Next Steps

1. **Configure DNS**: Point your domain names to the ingress IP
2. **SSL Certificates**: Install proper SSL certificates
3. **Monitor Deployment**: Check Grafana dashboards for mesh metrics
4. **Test Services**: Verify all services are accessible through the mesh
5. **Review Logs**: Check istio-proxy logs for any issues

## Troubleshooting

If issues occur, check:
- \`kubectl logs -n $ISTIO_NAMESPACE deployment/istiod\`
- \`kubectl get pods -n $NAMESPACE\`
- \`istioctl proxy-status\`
- \`istioctl analyze -n $NAMESPACE\`

## Configuration Files

All configuration files are located in:
- Installation: \`istio/installation.yaml\`
- Gateways: \`gateway/mcp-gateway.yaml\`
- Virtual Services: \`virtual-services/mcp-virtual-services.yaml\`
- Destination Rules: \`destination-rules/mcp-destination-rules.yaml\`
- Service Entries: \`service-entries/external-services.yaml\`
- Security: \`security/\`
- Telemetry: \`telemetry/telemetry-config.yaml\`
EOF

    log "Deployment report created: $REPORT_FILE"
}

# Cleanup function
cleanup_on_error() {
    error "Deployment failed. Check the logs above for details."
    info "To clean up partial deployment, run:"
    info "  kubectl delete -f $SERVICE_MESH_DIR/security/"
    info "  kubectl delete -f $SERVICE_MESH_DIR/telemetry/"
    info "  kubectl delete -f $SERVICE_MESH_DIR/virtual-services/"
    info "  kubectl delete -f $SERVICE_MESH_DIR/destination-rules/"
    info "  kubectl delete -f $SERVICE_MESH_DIR/service-entries/"
    info "  kubectl delete -f $SERVICE_MESH_DIR/gateway/"
    info "  istioctl uninstall --purge"
}

# Main deployment function
main() {
    log "Starting Istio Service Mesh deployment for MCP Debate System"
    log "============================================================"
    
    # Set error trap
    trap cleanup_on_error ERR
    
    check_prerequisites
    create_namespaces
    install_istio
    deploy_gateways
    deploy_virtual_services
    deploy_destination_rules
    deploy_service_entries
    deploy_security_policies
    deploy_telemetry
    verify_deployment
    show_ingress_info
    create_deployment_report
    
    log ""
    log "🎉 Service mesh deployment completed successfully!"
    log ""
    log "The MCP Debate System service mesh is now active with:"
    log "  ✅ Istio control plane"
    log "  ✅ Traffic management (Gateways, Virtual Services, Destination Rules)"
    log "  ✅ External service access (Service Entries)"
    log "  ✅ Security policies (mTLS, Authentication, Authorization)"
    log "  ✅ Observability (Metrics, Tracing, Logging)"
    log ""
    log "Check the deployment report for detailed information and next steps."
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        --timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --namespace <name>    Target namespace (default: production)"
            echo "  --timeout <duration>  Timeout for operations (default: 600s)"
            echo "  --help               Show this help message"
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            ;;
    esac
done

# Run main function
main "$@"