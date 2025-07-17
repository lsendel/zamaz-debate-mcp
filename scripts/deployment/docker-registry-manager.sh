#!/bin/bash

# Docker Registry Management Script
# This script helps with Docker image management and registry operations

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
REGISTRY="${REGISTRY:-ghcr.io}"
REGISTRY_BASE="${REGISTRY_BASE:-$(git config --get remote.origin.url | sed 's/.*github.com[:/]\([^/]*\).*/\1/')}"
VERSION="${VERSION:-latest}"
SERVICES=()
ACTION=""

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_help() {
    echo "Docker Registry Management Script"
    echo ""
    echo "Usage: $0 [options] <action> [service1 service2 ...]"
    echo ""
    echo "Actions:"
    echo "  build       Build Docker images"
    echo "  push        Push Docker images to registry"
    echo "  pull        Pull Docker images from registry"
    echo "  scan        Scan Docker images for vulnerabilities"
    echo "  sign        Sign Docker images with cosign"
    echo "  verify      Verify Docker image signatures"
    echo "  list        List available Docker images"
    echo "  clean       Clean up old Docker images"
    echo ""
    echo "Options:"
    echo "  -r, --registry <registry>    Docker registry (default: ghcr.io)"
    echo "  -b, --base <base>           Registry base path (default: github username)"
    echo "  -v, --version <version>     Image version (default: latest)"
    echo "  -a, --all                   Apply to all services"
    echo "  -h, --help                  Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 build mcp-organization mcp-llm"
    echo "  $0 --version 1.0.0 push mcp-organization"
    echo "  $0 --all scan"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        build|push|pull|scan|sign|verify|list|clean)
            ACTION="$1"
            shift
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -b|--base)
            REGISTRY_BASE="$2"
            shift 2
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -a|--all)
            # Find all services with Dockerfiles
            while IFS= read -r dockerfile; do
                service=$(echo "$dockerfile" | sed 's|./\([^/]*\)/.*|\1|')
                SERVICES+=("$service")
            done < <(find . -name "Dockerfile" -path "./mcp-*" | sort)
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        -*)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
        *)
            SERVICES+=("$1")
            shift
            ;;
    esac
done

# Validate arguments
if [[ -z "$ACTION" ]]; then
    log_error "No action specified"
    show_help
    exit 1
fi

if [[ ${#SERVICES[@]} -eq 0 && "$ACTION" != "list" ]]; then
    log_error "No services specified"
    show_help
    exit 1
fi

# Execute action
case $ACTION in
    build)
        log_info "Building Docker images for: ${SERVICES[*]}"
        for service in "${SERVICES[@]}"; do
            log_info "Building $service:$VERSION"
            docker build \
                --build-arg MODULE_NAME="$service" \
                --build-arg VERSION="$VERSION" \
                --build-arg BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
                --build-arg VCS_REF="$(git rev-parse --short HEAD)" \
                --build-arg VCS_URL="$(git config --get remote.origin.url)" \
                -t "$REGISTRY/$REGISTRY_BASE/$service:$VERSION" \
                -f "$service/Dockerfile" .
            log_success "Built $service:$VERSION"
        done
        ;;
    push)
        log_info "Pushing Docker images to registry: ${SERVICES[*]}"
        for service in "${SERVICES[@]}"; do
            log_info "Pushing $service:$VERSION to $REGISTRY/$REGISTRY_BASE/$service:$VERSION"
            docker push "$REGISTRY/$REGISTRY_BASE/$service:$VERSION"
            log_success "Pushed $service:$VERSION"
        done
        ;;
    pull)
        log_info "Pulling Docker images from registry: ${SERVICES[*]}"
        for service in "${SERVICES[@]}"; do
            log_info "Pulling $REGISTRY/$REGISTRY_BASE/$service:$VERSION"
            docker pull "$REGISTRY/$REGISTRY_BASE/$service:$VERSION"
            log_success "Pulled $service:$VERSION"
        done
        ;;
    scan)
        log_info "Scanning Docker images for vulnerabilities: ${SERVICES[*]}"
        for service in "${SERVICES[@]}"; do
            log_info "Scanning $REGISTRY/$REGISTRY_BASE/$service:$VERSION"
            if command -v trivy &> /dev/null; then
                trivy image "$REGISTRY/$REGISTRY_BASE/$service:$VERSION"
            else
                log_warning "Trivy not installed, using Docker Scout"
                docker scout cves "$REGISTRY/$REGISTRY_BASE/$service:$VERSION"
            fi
            log_success "Scanned $service:$VERSION"
        done
        ;;
    sign)
        log_info "Signing Docker images: ${SERVICES[*]}"
        if ! command -v cosign &> /dev/null; then
            log_error "cosign not installed. Please install it first."
            exit 1
        fi
        for service in "${SERVICES[@]}"; do
            log_info "Signing $REGISTRY/$REGISTRY_BASE/$service:$VERSION"
            COSIGN_EXPERIMENTAL=1 cosign sign --yes "$REGISTRY/$REGISTRY_BASE/$service:$VERSION"
            log_success "Signed $service:$VERSION"
        done
        ;;
    verify)
        log_info "Verifying Docker image signatures: ${SERVICES[*]}"
        if ! command -v cosign &> /dev/null; then
            log_error "cosign not installed. Please install it first."
            exit 1
        fi
        for service in "${SERVICES[@]}"; do
            log_info "Verifying $REGISTRY/$REGISTRY_BASE/$service:$VERSION"
            COSIGN_EXPERIMENTAL=1 cosign verify "$REGISTRY/$REGISTRY_BASE/$service:$VERSION"
            log_success "Verified $service:$VERSION"
        done
        ;;
    list)
        log_info "Listing available Docker images"
        if [[ ${#SERVICES[@]} -eq 0 ]]; then
            # List all images in registry
            log_info "Available services:"
            while IFS= read -r dockerfile; do
                service=$(echo "$dockerfile" | sed 's|./\([^/]*\)/.*|\1|')
                echo "- $service"
            done < <(find . -name "Dockerfile" -path "./mcp-*" | sort)
        else
            # List specific services
            for service in "${SERVICES[@]}"; do
                log_info "Available versions for $service:"
                if command -v skopeo &> /dev/null; then
                    skopeo list-tags "docker://$REGISTRY/$REGISTRY_BASE/$service" | jq -r '.Tags[]'
                else
                    log_warning "skopeo not installed, using Docker CLI (limited functionality)"
                    docker image ls "$REGISTRY/$REGISTRY_BASE/$service"
                fi
            done
        fi
        ;;
    clean)
        log_info "Cleaning up old Docker images: ${SERVICES[*]}"
        for service in "${SERVICES[@]}"; do
            log_info "Cleaning up $service"
            # Remove all images except the latest version
            docker image ls --format "{{.Repository}}:{{.Tag}}" | grep "$REGISTRY/$REGISTRY_BASE/$service" | grep -v "$VERSION" | xargs -r docker image rm
            log_success "Cleaned up $service"
        done
        ;;
    *)
        log_error "Unknown action: $ACTION"
        show_help
        exit 1
        ;;
esac

log_success "Docker registry management completed successfully"