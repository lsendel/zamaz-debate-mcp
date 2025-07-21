#!/bin/bash

# Verify Agentic Flows deployment
# Usage: ./verify-deployment.sh [namespace]

set -e

NAMESPACE=${1:-agentic-flows}
EXPECTED_REPLICAS=3
TIMEOUT=300

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Verifying Agentic Flows deployment in namespace: ${NAMESPACE}${NC}"

# Function to check deployment status
check_deployment() {
    echo -e "\n${YELLOW}Checking deployment status...${NC}"
    
    READY=$(kubectl get deployment agentic-flows-processor -n ${NAMESPACE} -o jsonpath='{.status.readyReplicas}')
    DESIRED=$(kubectl get deployment agentic-flows-processor -n ${NAMESPACE} -o jsonpath='{.spec.replicas}')
    
    if [ "${READY}" == "${DESIRED}" ]; then
        echo -e "${GREEN}✓ Deployment is ready: ${READY}/${DESIRED} replicas${NC}"
        return 0
    else
        echo -e "${RED}✗ Deployment not ready: ${READY}/${DESIRED} replicas${NC}"
        return 1
    fi
}

# Function to check pod status
check_pods() {
    echo -e "\n${YELLOW}Checking pod status...${NC}"
    
    kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=agentic-flows
    
    # Check for any pods in error state
    ERROR_PODS=$(kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=agentic-flows \
        -o jsonpath='{.items[?(@.status.phase!="Running")].metadata.name}')
    
    if [ -z "${ERROR_PODS}" ]; then
        echo -e "${GREEN}✓ All pods are running${NC}"
        return 0
    else
        echo -e "${RED}✗ Pods in error state: ${ERROR_PODS}${NC}"
        return 1
    fi
}

# Function to check service endpoints
check_services() {
    echo -e "\n${YELLOW}Checking service endpoints...${NC}"
    
    ENDPOINTS=$(kubectl get endpoints agentic-flows-service -n ${NAMESPACE} \
        -o jsonpath='{.subsets[*].addresses[*].ip}' | wc -w)
    
    if [ "${ENDPOINTS}" -ge 1 ]; then
        echo -e "${GREEN}✓ Service has ${ENDPOINTS} endpoints${NC}"
        return 0
    else
        echo -e "${RED}✗ Service has no endpoints${NC}"
        return 1
    fi
}

# Function to test health endpoint
test_health() {
    echo -e "\n${YELLOW}Testing health endpoint...${NC}"
    
    # Port forward to test
    kubectl port-forward service/agentic-flows-service 8080:80 -n ${NAMESPACE} &
    PF_PID=$!
    sleep 5
    
    # Test health endpoint
    if curl -s -f http://localhost:8080/actuator/health > /dev/null; then
        echo -e "${GREEN}✓ Health endpoint is responding${NC}"
        HEALTH_OK=0
    else
        echo -e "${RED}✗ Health endpoint is not responding${NC}"
        HEALTH_OK=1
    fi
    
    kill ${PF_PID} 2>/dev/null || true
    return ${HEALTH_OK}
}

# Function to check database connectivity
check_database() {
    echo -e "\n${YELLOW}Checking database connectivity...${NC}"
    
    # Run a test query in a pod
    POD=$(kubectl get pod -n ${NAMESPACE} -l app.kubernetes.io/name=agentic-flows \
        -o jsonpath='{.items[0].metadata.name}')
    
    if kubectl exec ${POD} -n ${NAMESPACE} -- \
        curl -s http://localhost:9090/actuator/health/db | grep -q "UP"; then
        echo -e "${GREEN}✓ Database connection is healthy${NC}"
        return 0
    else
        echo -e "${RED}✗ Database connection failed${NC}"
        return 1
    fi
}

# Function to check Redis connectivity
check_redis() {
    echo -e "\n${YELLOW}Checking Redis connectivity...${NC}"
    
    POD=$(kubectl get pod -n ${NAMESPACE} -l app.kubernetes.io/name=agentic-flows \
        -o jsonpath='{.items[0].metadata.name}')
    
    if kubectl exec ${POD} -n ${NAMESPACE} -- \
        curl -s http://localhost:9090/actuator/health/redis | grep -q "UP"; then
        echo -e "${GREEN}✓ Redis connection is healthy${NC}"
        return 0
    else
        echo -e "${RED}✗ Redis connection failed${NC}"
        return 1
    fi
}

# Function to test API endpoints
test_api() {
    echo -e "\n${YELLOW}Testing API endpoints...${NC}"
    
    # Port forward
    kubectl port-forward service/agentic-flows-service 8080:80 -n ${NAMESPACE} &
    PF_PID=$!
    sleep 5
    
    # Test flow types endpoint
    echo "Testing GET /api/v1/agentic-flows/types..."
    if curl -s http://localhost:8080/api/v1/agentic-flows/types | grep -q "INTERNAL_MONOLOGUE"; then
        echo -e "${GREEN}✓ Flow types endpoint working${NC}"
    else
        echo -e "${RED}✗ Flow types endpoint failed${NC}"
    fi
    
    kill ${PF_PID} 2>/dev/null || true
}

# Function to check metrics
check_metrics() {
    echo -e "\n${YELLOW}Checking metrics endpoint...${NC}"
    
    POD=$(kubectl get pod -n ${NAMESPACE} -l app.kubernetes.io/name=agentic-flows \
        -o jsonpath='{.items[0].metadata.name}')
    
    if kubectl exec ${POD} -n ${NAMESPACE} -- \
        curl -s http://localhost:9090/actuator/prometheus | grep -q "agentic_flow"; then
        echo -e "${GREEN}✓ Metrics are being collected${NC}"
        return 0
    else
        echo -e "${RED}✗ Metrics not available${NC}"
        return 1
    fi
}

# Function to check logs for errors
check_logs() {
    echo -e "\n${YELLOW}Checking logs for errors...${NC}"
    
    ERROR_COUNT=$(kubectl logs -n ${NAMESPACE} -l app.kubernetes.io/name=agentic-flows \
        --tail=1000 --since=10m | grep -i "ERROR" | wc -l)
    
    if [ "${ERROR_COUNT}" -eq 0 ]; then
        echo -e "${GREEN}✓ No errors in recent logs${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ Found ${ERROR_COUNT} errors in recent logs${NC}"
        echo "Recent errors:"
        kubectl logs -n ${NAMESPACE} -l app.kubernetes.io/name=agentic-flows \
            --tail=1000 --since=10m | grep -i "ERROR" | tail -5
        return 1
    fi
}

# Function to generate report
generate_report() {
    echo -e "\n${YELLOW}Deployment Verification Report${NC}"
    echo "================================"
    echo "Namespace: ${NAMESPACE}"
    echo "Timestamp: $(date)"
    echo ""
    
    # Summary of checks
    echo "Check Results:"
    echo "- Deployment: ${DEPLOYMENT_STATUS}"
    echo "- Pods: ${POD_STATUS}"
    echo "- Services: ${SERVICE_STATUS}"
    echo "- Health: ${HEALTH_STATUS}"
    echo "- Database: ${DB_STATUS}"
    echo "- Redis: ${REDIS_STATUS}"
    echo "- API: ${API_STATUS}"
    echo "- Metrics: ${METRICS_STATUS}"
    echo "- Logs: ${LOG_STATUS}"
    
    if [ "${OVERALL_STATUS}" == "PASS" ]; then
        echo -e "\n${GREEN}Overall Status: PASS${NC}"
        return 0
    else
        echo -e "\n${RED}Overall Status: FAIL${NC}"
        return 1
    fi
}

# Main verification flow
main() {
    OVERALL_STATUS="PASS"
    
    # Run checks
    if check_deployment; then
        DEPLOYMENT_STATUS="PASS"
    else
        DEPLOYMENT_STATUS="FAIL"
        OVERALL_STATUS="FAIL"
    fi
    
    if check_pods; then
        POD_STATUS="PASS"
    else
        POD_STATUS="FAIL"
        OVERALL_STATUS="FAIL"
    fi
    
    if check_services; then
        SERVICE_STATUS="PASS"
    else
        SERVICE_STATUS="FAIL"
        OVERALL_STATUS="FAIL"
    fi
    
    if test_health; then
        HEALTH_STATUS="PASS"
    else
        HEALTH_STATUS="FAIL"
        OVERALL_STATUS="FAIL"
    fi
    
    if check_database; then
        DB_STATUS="PASS"
    else
        DB_STATUS="FAIL"
        # Don't fail overall for DB in case it's external
    fi
    
    if check_redis; then
        REDIS_STATUS="PASS"
    else
        REDIS_STATUS="FAIL"
        # Don't fail overall for Redis in case it's external
    fi
    
    if test_api; then
        API_STATUS="PASS"
    else
        API_STATUS="FAIL"
        OVERALL_STATUS="FAIL"
    fi
    
    if check_metrics; then
        METRICS_STATUS="PASS"
    else
        METRICS_STATUS="FAIL"
        # Don't fail overall for metrics
    fi
    
    if check_logs; then
        LOG_STATUS="PASS"
    else
        LOG_STATUS="WARNING"
        # Don't fail overall for log warnings
    fi
    
    # Generate report
    generate_report
}

# Run main
main