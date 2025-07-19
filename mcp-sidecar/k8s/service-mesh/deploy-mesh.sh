#!/bin/bash

# Deploy Service Mesh for MCP Sidecar
# This script deploys Istio service mesh configuration for the MCP platform

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE=${NAMESPACE:-default}
MESH_TYPE=${MESH_TYPE:-istio}
DRY_RUN=${DRY_RUN:-false}

echo -e "${BLUE}🚀 Deploying Service Mesh for MCP Platform${NC}"
echo -e "${BLUE}Namespace: ${NAMESPACE}${NC}"
echo -e "${BLUE}Mesh Type: ${MESH_TYPE}${NC}"
echo ""

# Function to print status
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo -e "${BLUE}📋 Checking prerequisites...${NC}"

if ! command_exists kubectl; then
    print_error "kubectl is not installed or not in PATH"
    exit 1
fi

if ! command_exists istioctl; then
    print_warning "istioctl is not installed. Install it from https://istio.io/latest/docs/setup/getting-started/"
    echo "You can still deploy the manifests manually."
fi

# Check if cluster is accessible
if ! kubectl cluster-info >/dev/null 2>&1; then
    print_error "Cannot connect to Kubernetes cluster"
    exit 1
fi

print_status "Prerequisites check completed"

# Function to apply manifests
apply_manifest() {
    local file=$1
    local description=$2
    
    echo -e "${BLUE}📄 Applying $description...${NC}"
    
    if [ "$DRY_RUN" = "true" ]; then
        echo "DRY RUN: Would apply $file"
        kubectl apply -f "$file" --dry-run=client --validate=true
    else
        kubectl apply -f "$file" -n "$NAMESPACE"
    fi
    
    if [ $? -eq 0 ]; then
        print_status "$description applied successfully"
    else
        print_error "Failed to apply $description"
        return 1
    fi
}

# Check if Istio is installed
check_istio_installation() {
    echo -e "${BLUE}🔍 Checking Istio installation...${NC}"
    
    if kubectl get namespace istio-system >/dev/null 2>&1; then
        print_status "Istio namespace found"
        
        if kubectl get pods -n istio-system -l app=istiod | grep -q Running; then
            print_status "Istio control plane is running"
        else
            print_warning "Istio control plane may not be fully ready"
        fi
    else
        print_error "Istio is not installed. Please install Istio first:"
        echo "  istioctl install --set values.defaultRevision=default"
        echo "  kubectl label namespace $NAMESPACE istio-injection=enabled"
        exit 1
    fi
}

# Enable Istio injection for namespace
enable_istio_injection() {
    echo -e "${BLUE}💉 Enabling Istio injection for namespace $NAMESPACE...${NC}"
    
    if [ "$DRY_RUN" = "true" ]; then
        echo "DRY RUN: Would enable Istio injection for namespace $NAMESPACE"
    else
        kubectl label namespace "$NAMESPACE" istio-injection=enabled --overwrite
        print_status "Istio injection enabled for namespace $NAMESPACE"
    fi
}

# Create TLS certificate secret (if not exists)
create_tls_secret() {
    echo -e "${BLUE}🔐 Checking TLS certificate...${NC}"
    
    if kubectl get secret mcp-tls-cert -n istio-system >/dev/null 2>&1; then
        print_status "TLS certificate secret already exists"
    else
        print_warning "Creating self-signed TLS certificate"
        
        if [ "$DRY_RUN" = "true" ]; then
            echo "DRY RUN: Would create TLS certificate"
        else
            # Create self-signed certificate
            openssl req -x509 -newkey rsa:4096 -keyout tls.key -out tls.crt -days 365 -nodes \
                -subj "/C=US/ST=CA/L=San Francisco/O=MCP/OU=Platform/CN=*.mcp.local"
            
            kubectl create secret tls mcp-tls-cert \
                --key=tls.key \
                --cert=tls.crt \
                -n istio-system
            
            # Clean up temporary files
            rm -f tls.key tls.crt
            
            print_status "TLS certificate created"
        fi
    fi
}

# Function to wait for rollout
wait_for_rollout() {
    local resource=$1
    local name=$2
    
    echo -e "${BLUE}⏳ Waiting for $resource/$name to be ready...${NC}"
    
    if [ "$DRY_RUN" = "false" ]; then
        kubectl rollout status "$resource/$name" -n "$NAMESPACE" --timeout=300s
        if [ $? -eq 0 ]; then
            print_status "$resource/$name is ready"
        else
            print_warning "$resource/$name rollout may have timed out"
        fi
    fi
}

# Main deployment function
deploy_service_mesh() {
    echo -e "${BLUE}🚀 Starting service mesh deployment...${NC}"
    
    # Check Istio installation
    if [ "$MESH_TYPE" = "istio" ]; then
        check_istio_installation
        enable_istio_injection
        create_tls_secret
    fi
    
    # Apply service mesh configurations
    echo -e "${BLUE}📦 Applying service mesh configurations...${NC}"
    
    # Apply Istio Gateway and VirtualServices
    apply_manifest "istio-gateway.yaml" "Istio Gateway and VirtualServices"
    
    # Apply MCP services mesh configuration
    apply_manifest "mcp-services-mesh.yaml" "MCP Services Mesh Configuration"
    
    # Apply Envoy filters
    apply_manifest "envoy-filters.yaml" "Envoy Filters"
    
    echo ""
    print_status "Service mesh deployment completed!"
    
    # Display useful information
    echo -e "${BLUE}📊 Service Mesh Information:${NC}"
    echo "Namespace: $NAMESPACE"
    echo "Mesh Type: $MESH_TYPE"
    echo ""
    
    if [ "$DRY_RUN" = "false" ]; then
        echo -e "${BLUE}🔍 Checking deployed resources:${NC}"
        
        echo "Gateways:"
        kubectl get gateways -n "$NAMESPACE"
        echo ""
        
        echo "VirtualServices:"
        kubectl get virtualservices -n "$NAMESPACE"
        echo ""
        
        echo "DestinationRules:"
        kubectl get destinationrules -n "$NAMESPACE"
        echo ""
        
        echo "PeerAuthentication:"
        kubectl get peerauthentication -n "$NAMESPACE"
        echo ""
        
        echo "AuthorizationPolicy:"
        kubectl get authorizationpolicy -n "$NAMESPACE"
        echo ""
        
        echo "EnvoyFilters:"
        kubectl get envoyfilters -n "$NAMESPACE"
        echo ""
        
        # Check if Istio gateway is ready
        if command_exists istioctl; then
            echo -e "${BLUE}🌐 Gateway Information:${NC}"
            kubectl get svc istio-ingressgateway -n istio-system
            echo ""
            
            GATEWAY_IP=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
            if [ -z "$GATEWAY_IP" ]; then
                GATEWAY_IP=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
            fi
            
            if [ -n "$GATEWAY_IP" ]; then
                echo -e "${GREEN}Gateway External IP/Hostname: $GATEWAY_IP${NC}"
                echo "Add this to your /etc/hosts file:"
                echo "$GATEWAY_IP mcp-sidecar.local"
                echo "$GATEWAY_IP api.mcp.local"
            else
                print_warning "Gateway external IP not yet assigned"
            fi
        fi
    fi
}

# Function to verify deployment
verify_deployment() {
    echo -e "${BLUE}🔍 Verifying deployment...${NC}"
    
    if [ "$DRY_RUN" = "true" ]; then
        print_status "DRY RUN: Skipping verification"
        return
    fi
    
    # Check if pods have Istio sidecars
    echo "Checking for Istio sidecars:"
    kubectl get pods -n "$NAMESPACE" -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].name}{"\n"}{end}' | grep istio-proxy || print_warning "No Istio sidecars found"
    
    # Check gateway status
    if kubectl get gateway mcp-sidecar-gateway -n "$NAMESPACE" >/dev/null 2>&1; then
        print_status "Gateway is configured"
    else
        print_warning "Gateway not found"
    fi
    
    # Check virtual services
    VS_COUNT=$(kubectl get virtualservices -n "$NAMESPACE" --no-headers | wc -l)
    print_status "Found $VS_COUNT VirtualServices"
    
    # Check destination rules
    DR_COUNT=$(kubectl get destinationrules -n "$NAMESPACE" --no-headers | wc -l)
    print_status "Found $DR_COUNT DestinationRules"
}

# Function to show help
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Deploy service mesh for MCP platform"
    echo ""
    echo "Options:"
    echo "  -n, --namespace NAME    Kubernetes namespace (default: default)"
    echo "  -m, --mesh-type TYPE    Mesh type: istio|linkerd|consul (default: istio)"
    echo "  -d, --dry-run          Perform dry run without applying changes"
    echo "  -v, --verify           Verify deployment after applying"
    echo "  -h, --help             Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  NAMESPACE      Override default namespace"
    echo "  MESH_TYPE      Override default mesh type"
    echo "  DRY_RUN        Set to 'true' for dry run"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Deploy with defaults"
    echo "  $0 -n mcp-system -m istio           # Deploy to specific namespace"
    echo "  $0 --dry-run                         # Dry run deployment"
    echo "  $0 --verify                          # Deploy and verify"
}

# Parse command line arguments
VERIFY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -m|--mesh-type)
            MESH_TYPE="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -v|--verify)
            VERIFY=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Main execution
main() {
    echo -e "${BLUE}=====================================Mesh Configuration=================================${NC}"
    echo -e "${BLUE}MCP Service Mesh Deployment${NC}"
    echo -e "${BLUE}===============================================================================${NC}"
    echo ""
    
    # Deploy service mesh
    deploy_service_mesh
    
    # Verify if requested
    if [ "$VERIFY" = "true" ]; then
        echo ""
        verify_deployment
    fi
    
    echo ""
    echo -e "${GREEN}🎉 Service mesh deployment completed successfully!${NC}"
    echo ""
    echo -e "${BLUE}📚 Next Steps:${NC}"
    echo "1. Check that all pods have Istio sidecars injected"
    echo "2. Configure DNS or /etc/hosts for *.mcp.local domains"
    echo "3. Test connectivity through the mesh"
    echo "4. Monitor mesh metrics in Grafana/Kiali"
    echo ""
    echo -e "${BLUE}📖 Useful Commands:${NC}"
    echo "  kubectl get pods -n $NAMESPACE                    # Check pod status"
    echo "  istioctl proxy-status                            # Check proxy status"
    echo "  istioctl analyze -n $NAMESPACE                   # Analyze configuration"
    echo "  kubectl logs -n $NAMESPACE <pod> -c istio-proxy  # Check sidecar logs"
}

# Run main function
main "$@"