#!/bin/bash

# Script to deploy MCP Config Server
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Default values
ENVIRONMENT="development"
DEPLOY_METHOD="docker-compose"
CONFIG_REPO=""
ENCRYPTION_KEY=""
NAMESPACE="mcp-system"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -m|--method)
            DEPLOY_METHOD="$2"
            shift 2
            ;;
        -r|--repo)
            CONFIG_REPO="$2"
            shift 2
            ;;
        -k|--key)
            ENCRYPTION_KEY="$2"
            shift 2
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -e, --environment    Environment (development, staging, production)"
            echo "  -m, --method         Deployment method (docker-compose, k8s, helm)"
            echo "  -r, --repo           Git repository URI for configurations"
            echo "  -k, --key            Encryption key for sensitive data"
            echo "  -n, --namespace      Kubernetes namespace (for k8s/helm deployment)"
            echo "  -h, --help           Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

echo -e "${GREEN}Deploying MCP Config Server${NC}"
echo "Environment: $ENVIRONMENT"
echo "Method: $DEPLOY_METHOD"

# Generate encryption key if not provided
if [ -z "$ENCRYPTION_KEY" ]; then
    echo -e "${YELLOW}Generating encryption key...${NC}"
    ENCRYPTION_KEY=$(openssl rand -base64 32)
    echo -e "${YELLOW}Generated key: $ENCRYPTION_KEY${NC}"
    echo -e "${YELLOW}Please save this key securely!${NC}"
fi

# Set default config repo if not provided
if [ -z "$CONFIG_REPO" ]; then
    CONFIG_REPO="https://github.com/zamaz/mcp-config-repo.git"
fi

# Deploy based on method
case $DEPLOY_METHOD in
    docker-compose)
        echo -e "${GREEN}Deploying with Docker Compose...${NC}"
        cd "$PROJECT_ROOT/infrastructure/docker-compose"
        
        # Create .env file
        cat > .env <<EOF
# Config Server Settings
CONFIG_GIT_REPO_URI=$CONFIG_REPO
CONFIG_ENCRYPTION_KEY=$ENCRYPTION_KEY
CONFIG_SERVER_USERNAME=admin
CONFIG_SERVER_PASSWORD=$(openssl rand -base64 16)
CONFIG_SERVER_PORT=8888

# RabbitMQ Settings
RABBITMQ_USER=admin
RABBITMQ_PASS=$(openssl rand -base64 16)
RABBITMQ_PORT=5672
RABBITMQ_MGMT_PORT=15672

# Environment
SPRING_PROFILES_ACTIVE=$ENVIRONMENT,docker,bus
EOF
        
        # Start Config Server and dependencies
        docker-compose up -d rabbitmq
        echo "Waiting for RabbitMQ to start..."
        sleep 20
        
        docker-compose up -d mcp-config-server
        echo "Waiting for Config Server to start..."
        sleep 30
        
        # Check health
        if curl -f http://localhost:8888/actuator/health >/dev/null 2>&1; then
            echo -e "${GREEN}Config Server is healthy!${NC}"
        else
            echo -e "${RED}Config Server health check failed${NC}"
            docker-compose logs mcp-config-server
            exit 1
        fi
        ;;
        
    k8s)
        echo -e "${GREEN}Deploying to Kubernetes...${NC}"
        cd "$PROJECT_ROOT/infrastructure/k8s/config-server"
        
        # Create namespace
        kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
        
        # Update secrets
        kubectl create secret generic config-server-secrets \
            --from-literal=encryption.key="$ENCRYPTION_KEY" \
            --from-literal=server.username="admin" \
            --from-literal=server.password="$(openssl rand -base64 16)" \
            --namespace=$NAMESPACE \
            --dry-run=client -o yaml | kubectl apply -f -
        
        kubectl create secret generic rabbitmq-secrets \
            --from-literal=username="admin" \
            --from-literal=password="$(openssl rand -base64 16)" \
            --namespace=$NAMESPACE \
            --dry-run=client -o yaml | kubectl apply -f -
        
        # Update ConfigMap
        kubectl apply -f configmap.yaml
        
        # Deploy RabbitMQ
        echo "Deploying RabbitMQ..."
        helm repo add bitnami https://charts.bitnami.com/bitnami
        helm repo update
        helm upgrade --install rabbitmq bitnami/rabbitmq \
            --namespace=$NAMESPACE \
            --set auth.username=admin \
            --set auth.existingPasswordSecret=rabbitmq-secrets \
            --wait
        
        # Deploy Config Server
        kubectl apply -f deployment.yaml
        kubectl apply -f networkpolicy.yaml
        kubectl apply -f hpa.yaml
        
        # Wait for deployment
        kubectl rollout status deployment/mcp-config-server -n $NAMESPACE
        
        echo -e "${GREEN}Config Server deployed successfully!${NC}"
        ;;
        
    helm)
        echo -e "${GREEN}Deploying with Helm...${NC}"
        cd "$PROJECT_ROOT/infrastructure/helm/mcp-config-server"
        
        # Create namespace
        kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
        
        # Create values file for environment
        cat > values-$ENVIRONMENT.yaml <<EOF
config:
  git:
    uri: $CONFIG_REPO
    branch: $ENVIRONMENT
  profiles:
    active: "$ENVIRONMENT,kubernetes,bus"

secrets:
  encryptionKey: $(echo -n "$ENCRYPTION_KEY" | base64)
  serverPassword: $(openssl rand -base64 16)

rabbitmq:
  enabled: true
  auth:
    password: $(openssl rand -base64 16)

resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1"

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 5
EOF
        
        # Install/upgrade
        helm upgrade --install mcp-config-server . \
            --namespace=$NAMESPACE \
            --values values.yaml \
            --values values-$ENVIRONMENT.yaml \
            --wait
        
        echo -e "${GREEN}Config Server deployed with Helm!${NC}"
        ;;
        
    *)
        echo -e "${RED}Unknown deployment method: $DEPLOY_METHOD${NC}"
        exit 1
        ;;
esac

# Post-deployment steps
echo -e "${GREEN}Post-deployment steps:${NC}"
echo "1. Update all microservices to use Config Server:"
echo "   - Set CONFIG_SERVER_URI environment variable"
echo "   - Add spring-cloud-starter-config dependency"
echo "   - Configure bootstrap.yml with application name"
echo ""
echo "2. Test configuration refresh:"
echo "   curl -X POST http://config-server:8888/actuator/bus-refresh"
echo ""
echo "3. View configurations:"
echo "   curl http://config-server:8888/{application}/{profile}"
echo ""

if [ "$DEPLOY_METHOD" = "k8s" ] || [ "$DEPLOY_METHOD" = "helm" ]; then
    echo "4. Port forward to access locally:"
    echo "   kubectl port-forward -n $NAMESPACE svc/mcp-config-server 8888:8888"
fi

echo -e "${GREEN}Deployment complete!${NC}"