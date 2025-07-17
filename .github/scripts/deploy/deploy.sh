#!/bin/bash
# Kiro GitHub Integration Deployment Script
# This script handles deployment to staging and production environments

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT=""
VERSION=""
DRY_RUN=false
ROLLBACK=false
NAMESPACE="kiro-system"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --rollback)
            ROLLBACK=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 -e <environment> -v <version> [options]"
            echo ""
            echo "Options:"
            echo "  -e, --environment  Environment to deploy to (staging|production)"
            echo "  -v, --version      Version to deploy (e.g., v1.2.3 or git SHA)"
            echo "  -n, --namespace    Kubernetes namespace (default: kiro-system)"
            echo "  --dry-run          Show what would be deployed without applying"
            echo "  --rollback         Rollback to previous version"
            echo "  -h, --help         Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Validate required arguments
if [[ -z "$ENVIRONMENT" ]]; then
    echo -e "${RED}Error: Environment is required${NC}"
    exit 1
fi

if [[ -z "$VERSION" ]] && [[ "$ROLLBACK" == false ]]; then
    echo -e "${RED}Error: Version is required${NC}"
    exit 1
fi

# Validate environment
if [[ "$ENVIRONMENT" != "staging" ]] && [[ "$ENVIRONMENT" != "production" ]]; then
    echo -e "${RED}Error: Environment must be 'staging' or 'production'${NC}"
    exit 1
fi

# Load environment-specific configuration
CONFIG_FILE=".github/deploy/config/${ENVIRONMENT}.env"
if [[ -f "$CONFIG_FILE" ]]; then
    source "$CONFIG_FILE"
else
    echo -e "${YELLOW}Warning: Configuration file not found: $CONFIG_FILE${NC}"
fi

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed"
        exit 1
    fi
    
    # Check Kubernetes connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Unable to connect to Kubernetes cluster"
        exit 1
    fi
    
    # Check namespace exists
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_error "Namespace $NAMESPACE does not exist"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Deploy function
deploy() {
    log_info "Deploying version $VERSION to $ENVIRONMENT..."
    
    # Update image tags
    if [[ "$DRY_RUN" == true ]]; then
        log_info "[DRY RUN] Would update images to:"
        echo "  webhook-handler: ghcr.io/kiro/webhook-handler:$VERSION"
        echo "  pr-processor: ghcr.io/kiro/pr-processor:$VERSION"
        echo "  notification-service: ghcr.io/kiro/notification-service:$VERSION"
    else
        # Apply Kubernetes manifests
        kubectl set image deployment/kiro-webhook-handler \
            webhook-handler=ghcr.io/kiro/webhook-handler:$VERSION \
            -n "$NAMESPACE"
        
        kubectl set image deployment/kiro-pr-processor \
            pr-processor=ghcr.io/kiro/pr-processor:$VERSION \
            -n "$NAMESPACE"
        
        kubectl set image deployment/kiro-notification-service \
            notification-service=ghcr.io/kiro/notification-service:$VERSION \
            -n "$NAMESPACE"
    fi
    
    # Wait for rollout
    if [[ "$DRY_RUN" == false ]]; then
        log_info "Waiting for deployments to roll out..."
        kubectl rollout status deployment/kiro-webhook-handler -n "$NAMESPACE" --timeout=300s
        kubectl rollout status deployment/kiro-pr-processor -n "$NAMESPACE" --timeout=300s
        kubectl rollout status deployment/kiro-notification-service -n "$NAMESPACE" --timeout=300s
    fi
    
    log_success "Deployment completed successfully"
}

# Rollback function
rollback() {
    log_info "Rolling back deployments in $ENVIRONMENT..."
    
    if [[ "$DRY_RUN" == true ]]; then
        log_info "[DRY RUN] Would rollback the following deployments:"
        echo "  - kiro-webhook-handler"
        echo "  - kiro-pr-processor"
        echo "  - kiro-notification-service"
    else
        kubectl rollout undo deployment/kiro-webhook-handler -n "$NAMESPACE"
        kubectl rollout undo deployment/kiro-pr-processor -n "$NAMESPACE"
        kubectl rollout undo deployment/kiro-notification-service -n "$NAMESPACE"
        
        # Wait for rollback
        kubectl rollout status deployment/kiro-webhook-handler -n "$NAMESPACE" --timeout=300s
        kubectl rollout status deployment/kiro-pr-processor -n "$NAMESPACE" --timeout=300s
        kubectl rollout status deployment/kiro-notification-service -n "$NAMESPACE" --timeout=300s
    fi
    
    log_success "Rollback completed successfully"
}

# Health check function
health_check() {
    log_info "Running health checks..."
    
    # Check pod status
    UNHEALTHY_PODS=$(kubectl get pods -n "$NAMESPACE" -l app=kiro --field-selector=status.phase!=Running -o name | wc -l)
    
    if [[ $UNHEALTHY_PODS -gt 0 ]]; then
        log_error "Found $UNHEALTHY_PODS unhealthy pods"
        kubectl get pods -n "$NAMESPACE" -l app=kiro --field-selector=status.phase!=Running
        return 1
    fi
    
    # Check service endpoints
    for service in webhook-handler pr-processor notification-service; do
        ENDPOINTS=$(kubectl get endpoints "kiro-$service" -n "$NAMESPACE" -o jsonpath='{.subsets[*].addresses[*].ip}' | wc -w)
        if [[ $ENDPOINTS -eq 0 ]]; then
            log_error "No endpoints found for service: kiro-$service"
            return 1
        fi
    done
    
    log_success "Health checks passed"
}

# Post-deployment tests
post_deployment_tests() {
    log_info "Running post-deployment tests..."
    
    # Get service URL
    if [[ "$ENVIRONMENT" == "production" ]]; then
        SERVICE_URL="https://kiro.example.com"
    else
        SERVICE_URL="https://kiro-staging.example.com"
    fi
    
    # Test health endpoint
    if command -v curl &> /dev/null; then
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$SERVICE_URL/health" || echo "000")
        if [[ "$HTTP_CODE" == "200" ]]; then
            log_success "Health endpoint returned 200 OK"
        else
            log_error "Health endpoint returned HTTP $HTTP_CODE"
            return 1
        fi
    fi
    
    log_success "Post-deployment tests passed"
}

# Main execution
main() {
    echo -e "${BLUE}Kiro GitHub Integration Deployment${NC}"
    echo "=================================="
    echo "Environment: $ENVIRONMENT"
    echo "Version: ${VERSION:-N/A}"
    echo "Namespace: $NAMESPACE"
    echo "Dry Run: $DRY_RUN"
    echo "Rollback: $ROLLBACK"
    echo ""
    
    # Check prerequisites
    check_prerequisites
    
    # Execute deployment or rollback
    if [[ "$ROLLBACK" == true ]]; then
        rollback
    else
        deploy
    fi
    
    # Run health checks
    if [[ "$DRY_RUN" == false ]]; then
        sleep 10  # Give pods time to start
        health_check || {
            log_error "Health checks failed, consider rolling back"
            exit 1
        }
        
        # Run post-deployment tests for production
        if [[ "$ENVIRONMENT" == "production" ]]; then
            post_deployment_tests || {
                log_error "Post-deployment tests failed"
                exit 1
            }
        fi
    fi
    
    log_success "Deployment process completed successfully!"
}

# Run main function
main