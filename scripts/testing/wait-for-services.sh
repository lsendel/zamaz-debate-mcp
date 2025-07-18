#!/bin/bash

# Wait for services to be ready
# Used in CI/CD pipeline for health checks

set -e

echo "Waiting for services to be ready..."

# Configuration
MAX_WAIT=300  # 5 minutes
SLEEP_INTERVAL=5
ELAPSED=0

# Services to check
declare -A SERVICES=(
    ["postgres"]="localhost:5432"
    ["redis"]="localhost:6379"
    ["mcp-gateway"]="localhost:8080/actuator/health"
    ["mcp-controller"]="localhost:5013/actuator/health"
    ["mcp-organization"]="localhost:5005/actuator/health"
    ["debate-ui"]="localhost:3000"
)

# Function to check if a service is ready
check_service() {
    local service_name=$1
    local endpoint=$2
    
    if [[ """$endpoint""" == *"/actuator/health"* ]]; then
        # Spring Boot health check
        if curl -s -f "http://"""$endpoint"""" > /dev/null 2>&1; then
            local status=$(curl -s "http://"""$endpoint"""" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
            if [[ """"$status"""" == "UP" ]]; then
                return 0
            fi
        fi
    elif [[ """$service_name""" == "postgres" ]]; then
        # PostgreSQL check
        if pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
            return 0
        fi
    elif [[ """$service_name""" == "redis" ]]; then
        # Redis check
        if redis-cli -h localhost -p 6379 ping > /dev/null 2>&1; then
            return 0
        fi
    else
        # Generic HTTP check
        if curl -s -f "http://"""$endpoint"""" > /dev/null 2>&1; then
            return 0
        fi
    fi
    
    return 1
}

# Wait for all services
while [ """$ELAPSED""" -lt """$MAX_WAIT""" ]; do
    all_ready=true
    
    for service in "${!SERVICES[@]}"; do
        endpoint="${SERVICES["""$service"""]}"
        
        if check_service """"$service"""" """"$endpoint""""; then
            echo "✅ """$service""" is ready"
        else
            echo "⏳ Waiting for """$service"""..."
            all_ready=false
        fi
    done
    
    if """$all_ready"""; then
        echo "🎉 All services are ready!"
        exit 0
    fi
    
    sleep $SLEEP_INTERVAL
    ELAPSED=$((ELAPSED + SLEEP_INTERVAL))
    echo "Elapsed time: ${ELAPSED}s / ${MAX_WAIT}s"
done

echo "❌ Timeout waiting for services to be ready"
echo "Services status:"
for service in "${!SERVICES[@]}"; do
    endpoint="${SERVICES["""$service"""]}"
    if check_service """"$service"""" """"$endpoint""""; then
        echo "  """$service""": ✅ Ready"
    else
        echo "  """$service""": ❌ Not ready"
    fi
done

exit 1