#!/bin/bash

# Deploy Istio Service Mesh for MCP System
# This script installs and configures Istio with all necessary components

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
ISTIO_VERSION="1.19.0"
NAMESPACE="istio-system"
MCP_NAMESPACE="mcp-system"
ISTIO_DIR="./istio-${ISTIO_VERSION}"

echo -e "${GREEN}🚀 Starting Istio Service Mesh deployment for MCP System${NC}"

# Function to print status
print_status() {
    echo -e "${YELLOW}📋 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    
    # Check if cluster is accessible
    if ! kubectl cluster-info &> /dev/null; then
        print_error "Unable to connect to Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi
    
    # Check if running as cluster admin
    if ! kubectl auth can-i create clusterrole &> /dev/null; then
        print_error "Insufficient permissions. Please run as cluster admin."
        exit 1
    fi
    
    print_success "Prerequisites check passed"
}

# Download and install Istio
install_istio() {
    print_status "Installing Istio ${ISTIO_VERSION}..."
    
    # Download Istio if not already present
    if [ ! -d """$ISTIO_DIR""" ]; then
        print_status "Downloading Istio ${ISTIO_VERSION}..."
        curl -L https://istio.io/downloadIstio | ISTIO_VERSION=${ISTIO_VERSION} sh -
    fi
    
    # Add istioctl to PATH
    export PATH="""$PWD""/""$ISTIO_DIR""/bin:""$PATH"""
    
    # Verify installation
    if ! command -v istioctl &> /dev/null; then
        print_error "Failed to install istioctl"
        exit 1
    fi
    
    print_success "Istio CLI installed successfully"
}

# Install Istio control plane
install_control_plane() {
    print_status "Installing Istio control plane..."
    
    # Create namespace
    kubectl create namespace ""$NAMESPACE"" --dry-run=client -o yaml | kubectl apply -f -
    
    # Install Istio with custom configuration
    istioctl install --set values.pilot.env.EXTERNAL_ISTIOD=false \
        --set values.global.meshID=mcp-mesh \
        --set values.global.multiCluster.clusterName=mcp-cluster \
        --set values.global.network=mcp-network \
        --set values.istiodRemote.enabled=false \
        --set values.pilot.env.ENABLE_WORKLOAD_ENTRY_AUTOREGISTRATION=true \
        --set values.pilot.env.PILOT_ENABLE_WORKLOAD_ENTRY_AUTOREGISTRATION=true \
        --set values.pilot.env.PILOT_ENABLE_CROSS_CLUSTER_WORKLOAD_ENTRY=true \
        --set values.global.proxy.tracer=opentelemetry \
        --set values.telemetry.v2.enabled=true \
        --set values.telemetry.v2.prometheus.configOverride.disable_host_header_fallback=true \
        --set values.global.defaultPodDisruptionBudget.enabled=true \
        --set values.pilot.resources.requests.cpu=100m \
        --set values.pilot.resources.requests.memory=128Mi \
        --set values.pilot.resources.limits.cpu=500m \
        --set values.pilot.resources.limits.memory=512Mi \
        --set values.gateways.istio-ingressgateway.resources.requests.cpu=100m \
        --set values.gateways.istio-ingressgateway.resources.requests.memory=128Mi \
        --set values.gateways.istio-ingressgateway.resources.limits.cpu=500m \
        --set values.gateways.istio-ingressgateway.resources.limits.memory=512Mi \
        --set values.gateways.istio-egressgateway.enabled=true \
        --yes
    
    # Wait for control plane to be ready
    kubectl wait --for=condition=ready pod -l app=istiod -n ""$NAMESPACE"" --timeout=300s
    
    print_success "Istio control plane installed successfully"
}

# Install Istio addons
install_addons() {
    print_status "Installing Istio addons..."
    
    # Create addon directory if it doesn't exist
    mkdir -p addons
    
    # Install Kiali
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.19/samples/addons/kiali.yaml
    
    # Install Jaeger
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.19/samples/addons/jaeger.yaml
    
    # Install Grafana
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.19/samples/addons/grafana.yaml
    
    # Install Prometheus
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.19/samples/addons/prometheus.yaml
    
    # Wait for addons to be ready
    kubectl wait --for=condition=ready pod -l app=kiali -n ""$NAMESPACE"" --timeout=300s
    kubectl wait --for=condition=ready pod -l app=jaeger -n ""$NAMESPACE"" --timeout=300s
    kubectl wait --for=condition=ready pod -l app=grafana -n ""$NAMESPACE"" --timeout=300s
    
    print_success "Istio addons installed successfully"
}

# Create MCP namespace with injection
create_mcp_namespace() {
    print_status "Creating MCP namespace with sidecar injection..."
    
    kubectl create namespace ""$MCP_NAMESPACE"" --dry-run=client -o yaml | kubectl apply -f -
    kubectl label namespace ""$MCP_NAMESPACE"" istio-injection=enabled --overwrite
    
    print_success "MCP namespace created with sidecar injection enabled"
}

# Apply Istio configurations
apply_istio_configs() {
    print_status "Applying Istio configurations..."
    
    # Apply gateway configuration
    kubectl apply -f k8s/istio/gateway-config.yaml
    
    # Apply security policies
    kubectl apply -f k8s/istio/security-policies.yaml
    
    # Apply traffic management
    kubectl apply -f k8s/istio/traffic-management.yaml
    
    # Apply observability configuration
    kubectl apply -f k8s/istio/observability.yaml
    
    print_success "Istio configurations applied successfully"
}

# Deploy MCP services
deploy_mcp_services() {
    print_status "Deploying MCP services..."
    
    # Apply service deployments
    kubectl apply -f k8s/istio/deployment-configs.yaml
    
    # Wait for services to be ready
    print_status "Waiting for MCP services to be ready..."
    kubectl wait --for=condition=ready pod -l app=mcp-organization -n ""$MCP_NAMESPACE"" --timeout=300s
    kubectl wait --for=condition=ready pod -l app=mcp-llm -n ""$MCP_NAMESPACE"" --timeout=300s
    kubectl wait --for=condition=ready pod -l app=mcp-controller -n ""$MCP_NAMESPACE"" --timeout=300s
    kubectl wait --for=condition=ready pod -l app=mcp-rag -n ""$MCP_NAMESPACE"" --timeout=300s
    kubectl wait --for=condition=ready pod -l app=mcp-template -n ""$MCP_NAMESPACE"" --timeout=300s
    
    print_success "MCP services deployed successfully"
}

# Verify deployment
verify_deployment() {
    print_status "Verifying deployment..."
    
    # Check Istio installation
    istioctl verify-install
    
    # Check proxy status
    istioctl proxy-status
    
    # Check configuration
    istioctl analyze -n $MCP_NAMESPACE
    
    # Show service mesh status
    kubectl get pods -n $NAMESPACE
    kubectl get pods -n $MCP_NAMESPACE
    
    print_success "Deployment verification completed"
}

# Show access information
show_access_info() {
    print_status "Service mesh access information:"
    
    # Get ingress gateway IP
    INGRESS_HOST=$(kubectl get svc istio-ingressgateway -n ""$NAMESPACE"" -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
    INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n ""$NAMESPACE"" -o jsonpath='{.spec.ports[?(@.name=="http2")].port}')
    SECURE_INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n ""$NAMESPACE"" -o jsonpath='{.spec.ports[?(@.name=="https")].port}')
    
    # Get addon URLs
    KIALI_URL="http://""$INGRESS_HOST"":""$INGRESS_PORT""/kiali"
    JAEGER_URL="http://""$INGRESS_HOST"":""$INGRESS_PORT""/jaeger"
    GRAFANA_URL="http://""$INGRESS_HOST"":""$INGRESS_PORT""/grafana"
    
    echo ""
    echo "🌐 Access URLs:"
    echo "  Kiali (Service Mesh Dashboard): ""$KIALI_URL"""
    echo "  Jaeger (Tracing): ""$JAEGER_URL"""
    echo "  Grafana (Metrics): ""$GRAFANA_URL"""
    echo ""
    echo "🔧 Port forwarding commands (if LoadBalancer is not available):"
    echo "  kubectl port-forward svc/kiali 20001:20001 -n ""$NAMESPACE"""
    echo "  kubectl port-forward svc/jaeger 16686:16686 -n ""$NAMESPACE"""
    echo "  kubectl port-forward svc/grafana 3000:3000 -n ""$NAMESPACE"""
    echo ""
    echo "🔍 Troubleshooting commands:"
    echo "  istioctl proxy-status"
    echo "  istioctl analyze -n ""$MCP_NAMESPACE"""
    echo "  kubectl logs -l app=istiod -n ""$NAMESPACE"""
    echo ""
}

# Cleanup function
cleanup() {
    if [ "$1" = "uninstall" ]; then
        print_status "Uninstalling Istio..."
        
        # Remove MCP configurations
        kubectl delete -f k8s/istio/deployment-configs.yaml --ignore-not-found=true
        kubectl delete -f k8s/istio/observability.yaml --ignore-not-found=true
        kubectl delete -f k8s/istio/traffic-management.yaml --ignore-not-found=true
        kubectl delete -f k8s/istio/security-policies.yaml --ignore-not-found=true
        kubectl delete -f k8s/istio/gateway-config.yaml --ignore-not-found=true
        
        # Remove namespace
        kubectl delete namespace ""$MCP_NAMESPACE"" --ignore-not-found=true
        
        # Uninstall Istio
        istioctl uninstall --purge -y
        
        # Remove namespace
        kubectl delete namespace ""$NAMESPACE"" --ignore-not-found=true
        
        print_success "Istio uninstalled successfully"
        exit 0
    fi
}

# Main execution
main() {
    # Handle cleanup
    if [ "$1" = "uninstall" ]; then
        cleanup uninstall
    fi
    
    # Install process
    check_prerequisites
    install_istio
    install_control_plane
    install_addons
    create_mcp_namespace
    apply_istio_configs
    deploy_mcp_services
    verify_deployment
    show_access_info
    
    print_success "🎉 Istio service mesh deployment completed successfully!"
    print_status "Your MCP system is now running with Istio service mesh providing:"
    echo "  • Traffic management and load balancing"
    echo "  • Mutual TLS security"
    echo "  • Distributed tracing"
    echo "  • Circuit breaker and fault injection"
    echo "  • Metrics and observability"
    echo "  • Canary deployments and A/B testing"
}

# Run main function
main "$@"