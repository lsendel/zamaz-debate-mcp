#!/bin/bash

# Workflow Editor Deployment Script
# Supports multiple environments: development, staging, production

set -euo pipefail

# Configuration
APP_NAME="workflow-editor"
REGISTRY="your-registry.com"
VERSION=${1:-"latest"}
ENVIRONMENT=${2:-"development"}

echo "🚀 Starting deployment of $APP_NAME:$VERSION to $ENVIRONMENT"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Validate environment
case $ENVIRONMENT in
    development|staging|production)
        print_status "Environment: $ENVIRONMENT"
        ;;
    *)
        print_error "Invalid environment: $ENVIRONMENT"
        echo "Valid environments: development, staging, production"
        exit 1
        ;;
esac

# Pre-deployment checks
echo "🔍 Running pre-deployment checks..."

# Check if required tools are installed
command -v npm >/dev/null 2>&1 || { print_error "npm is required but not installed. Aborting."; exit 1; }
command -v docker >/dev/null 2>&1 || { print_error "docker is required but not installed. Aborting."; exit 1; }

print_status "All required tools are available"

# Install dependencies
echo "📦 Installing dependencies..."
npm ci --silent
print_status "Dependencies installed"

# Run tests
echo "🧪 Running tests..."
npm run test:run
print_status "All tests passed"

# Type checking
echo "🔍 Running type checks..."
npm run type-check
print_status "Type checking passed"

# Linting
echo "🔍 Running linter..."
npm run lint
print_status "Linting passed"

# Build application
echo "🏗️ Building application..."
npm run build
print_status "Application built successfully"

# Security audit
echo "🔒 Running security audit..."
npm audit --audit-level moderate || {
    print_warning "Security vulnerabilities found. Consider running 'npm audit fix'"
}

# Build Docker image
echo "🐳 Building Docker image..."
DOCKER_TAG="${REGISTRY}/${APP_NAME}:${VERSION}"
docker build -t "$DOCKER_TAG" .
print_status "Docker image built: $DOCKER_TAG"

# Environment-specific deployment
case $ENVIRONMENT in
    development)
        echo "🔧 Deploying to development environment..."
        docker-compose -f docker-compose.yml up -d --build
        ;;
    staging)
        echo "🔧 Deploying to staging environment..."
        # Push to registry
        docker push "$DOCKER_TAG"
        # Deploy to staging (customize based on your infrastructure)
        # kubectl apply -f k8s/staging/ || echo "Kubernetes deployment skipped"
        ;;
    production)
        echo "🔧 Deploying to production environment..."
        # Additional production checks
        echo "📊 Generating build report..."
        npm run build 2>&1 | tee build-report.txt
        
        # Push to registry
        docker push "$DOCKER_TAG"
        
        # Deploy to production (customize based on your infrastructure)
        # kubectl apply -f k8s/production/ || echo "Kubernetes deployment skipped"
        
        # Health check after deployment
        # curl -f http://your-production-url/health || print_warning "Health check failed"
        ;;
esac

print_status "Deployment completed successfully!"

# Post-deployment actions
echo "🎉 Post-deployment summary:"
echo "   - Application: $APP_NAME"
echo "   - Version: $VERSION"
echo "   - Environment: $ENVIRONMENT"
echo "   - Docker Image: $DOCKER_TAG"

if [ "$ENVIRONMENT" = "development" ]; then
    echo "   - Local URL: http://localhost:3002"
fi

echo ""
echo "📝 Next steps:"
echo "   - Monitor application logs"
echo "   - Verify all features are working"
echo "   - Update documentation if needed"

print_status "Deployment script completed!"