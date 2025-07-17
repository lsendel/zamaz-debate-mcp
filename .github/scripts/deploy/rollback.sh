#!/bin/bash
# Kiro GitHub Integration Emergency Rollback Script
# Quick rollback script for emergency situations

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
NAMESPACE="${NAMESPACE:-kiro-system}"
ENVIRONMENT="${1:-}"

if [[ -z "$ENVIRONMENT" ]]; then
    echo -e "${RED}Usage: $0 <environment>${NC}"
    echo "Environment must be 'staging' or 'production'"
    exit 1
fi

echo -e "${YELLOW}⚠️  EMERGENCY ROLLBACK - $ENVIRONMENT${NC}"
echo "This will immediately rollback all Kiro deployments"
read -p "Are you sure? (yes/no): " CONFIRM

if [[ "$CONFIRM" != "yes" ]]; then
    echo "Rollback cancelled"
    exit 0
fi

# Perform rollback
echo "Rolling back deployments..."
kubectl rollout undo deployment/kiro-webhook-handler -n "$NAMESPACE"
kubectl rollout undo deployment/kiro-pr-processor -n "$NAMESPACE"
kubectl rollout undo deployment/kiro-notification-service -n "$NAMESPACE"

# Check status
echo "Checking rollback status..."
kubectl rollout status deployment/kiro-webhook-handler -n "$NAMESPACE" --timeout=60s || true
kubectl rollout status deployment/kiro-pr-processor -n "$NAMESPACE" --timeout=60s || true
kubectl rollout status deployment/kiro-notification-service -n "$NAMESPACE" --timeout=60s || true

# Show current state
echo -e "\n${GREEN}Current deployment state:${NC}"
kubectl get deployments -n "$NAMESPACE" -l app=kiro

echo -e "\n${GREEN}Pod status:${NC}"
kubectl get pods -n "$NAMESPACE" -l app=kiro

echo -e "\n${YELLOW}Rollback initiated. Monitor the services for stability.${NC}"