#!/bin/bash

# Deploy Agentic Flows to Kubernetes
# Usage: ./deploy-agentic-flows.sh [environment] [version]

set -e

# Configuration
ENVIRONMENT=${1:-staging}
VERSION=${2:-latest}
NAMESPACE="agentic-flows"
REGISTRY="zamaz"
IMAGE_NAME="agentic-flows-processor"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}Deploying Agentic Flows to ${ENVIRONMENT} environment${NC}"
echo "Version: ${VERSION}"
echo "Namespace: ${NAMESPACE}"

# Function to check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}kubectl is not installed${NC}"
        exit 1
    fi
    
    # Check kubectl context
    CURRENT_CONTEXT=$(kubectl config current-context)
    echo "Current kubectl context: ${CURRENT_CONTEXT}"
    
    # Confirm deployment
    read -p "Deploy to context ${CURRENT_CONTEXT}? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}Deployment cancelled${NC}"
        exit 1
    fi
}

# Function to create namespace
create_namespace() {
    echo -e "${YELLOW}Creating namespace...${NC}"
    kubectl apply -f k8s/agentic-flows/namespace.yaml
}

# Function to apply configurations
apply_configurations() {
    echo -e "${YELLOW}Applying configurations...${NC}"
    
    # Apply environment-specific configs
    if [ -f "k8s/agentic-flows/configmap-${ENVIRONMENT}.yaml" ]; then
        kubectl apply -f k8s/agentic-flows/configmap-${ENVIRONMENT}.yaml
    else
        kubectl apply -f k8s/agentic-flows/configmap.yaml
    fi
    
    # Apply secrets (ensure they exist)
    if [ -f "k8s/agentic-flows/secret-${ENVIRONMENT}.yaml" ]; then
        kubectl apply -f k8s/agentic-flows/secret-${ENVIRONMENT}.yaml
    else
        echo -e "${RED}Warning: No secret file found for ${ENVIRONMENT}${NC}"
        echo "Please create k8s/agentic-flows/secret-${ENVIRONMENT}.yaml"
        exit 1
    fi
}

# Function to run database migrations
run_migrations() {
    echo -e "${YELLOW}Running database migrations...${NC}"
    
    # Create migration job
    cat <<EOF | kubectl apply -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: agentic-flows-migration-${VERSION//./-}
  namespace: ${NAMESPACE}
spec:
  template:
    spec:
      restartPolicy: Never
      containers:
      - name: migration
        image: ${REGISTRY}/${IMAGE_NAME}:${VERSION}
        command: ["java", "-jar", "/app/app.jar", "--spring.profiles.active=migration"]
        envFrom:
        - secretRef:
            name: agentic-flows-secret
        - configMapRef:
            name: agentic-flows-config
EOF
    
    # Wait for migration to complete
    echo "Waiting for migration to complete..."
    kubectl wait --for=condition=complete --timeout=300s \
        job/agentic-flows-migration-${VERSION//./-} -n ${NAMESPACE}
}

# Function to deploy application
deploy_application() {
    echo -e "${YELLOW}Deploying application...${NC}"
    
    # Update image version in deployment
    kubectl set image deployment/agentic-flows-processor \
        agentic-flows=${REGISTRY}/${IMAGE_NAME}:${VERSION} \
        -n ${NAMESPACE}
    
    # Apply all k8s resources
    kubectl apply -f k8s/agentic-flows/service.yaml
    kubectl apply -f k8s/agentic-flows/deployment.yaml
    kubectl apply -f k8s/agentic-flows/hpa.yaml
    kubectl apply -f k8s/agentic-flows/network-policy.yaml
    kubectl apply -f k8s/agentic-flows/pod-disruption-budget.yaml
    
    # Apply ingress for production only
    if [ "${ENVIRONMENT}" == "production" ]; then
        kubectl apply -f k8s/agentic-flows/ingress.yaml
    fi
}

# Function to wait for deployment
wait_for_deployment() {
    echo -e "${YELLOW}Waiting for deployment to be ready...${NC}"
    
    kubectl rollout status deployment/agentic-flows-processor -n ${NAMESPACE}
    
    # Check pod status
    kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=agentic-flows
}

# Function to run smoke tests
run_smoke_tests() {
    echo -e "${YELLOW}Running smoke tests...${NC}"
    
    # Get service endpoint
    if [ "${ENVIRONMENT}" == "production" ]; then
        ENDPOINT="https://api.zamaz-debate.com"
    else
        # Port forward for testing in non-production
        kubectl port-forward service/agentic-flows-service 8080:80 -n ${NAMESPACE} &
        PF_PID=$!
        sleep 5
        ENDPOINT="http://localhost:8080"
    fi
    
    # Health check
    echo "Checking health endpoint..."
    curl -f ${ENDPOINT}/actuator/health || {
        echo -e "${RED}Health check failed${NC}"
        [ ! -z "${PF_PID}" ] && kill ${PF_PID}
        exit 1
    }
    
    # API check
    echo "Checking API endpoint..."
    curl -f ${ENDPOINT}/api/v1/agentic-flows/types || {
        echo -e "${RED}API check failed${NC}"
        [ ! -z "${PF_PID}" ] && kill ${PF_PID}
        exit 1
    }
    
    [ ! -z "${PF_PID}" ] && kill ${PF_PID}
    
    echo -e "${GREEN}Smoke tests passed${NC}"
}

# Function to display deployment info
display_info() {
    echo -e "${GREEN}Deployment completed successfully!${NC}"
    echo
    echo "Deployment Information:"
    echo "----------------------"
    echo "Environment: ${ENVIRONMENT}"
    echo "Version: ${VERSION}"
    echo "Namespace: ${NAMESPACE}"
    echo
    echo "Useful commands:"
    echo "- View pods: kubectl get pods -n ${NAMESPACE}"
    echo "- View logs: kubectl logs -f deployment/agentic-flows-processor -n ${NAMESPACE}"
    echo "- Scale deployment: kubectl scale deployment/agentic-flows-processor --replicas=5 -n ${NAMESPACE}"
    echo "- Port forward: kubectl port-forward service/agentic-flows-service 8080:80 -n ${NAMESPACE}"
}

# Main deployment flow
main() {
    check_prerequisites
    create_namespace
    apply_configurations
    run_migrations
    deploy_application
    wait_for_deployment
    run_smoke_tests
    display_info
}

# Run main function
main