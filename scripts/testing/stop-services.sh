#!/bin/bash

# Stop Services
# Stops services that were started for testing

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "Stopping test services..."

# Check if we have information about started services
STARTED_SERVICES_FILE="${PROJECT_ROOT}/.github/cache/started-services.txt"
COMPOSE_OVERRIDE_FILE="${PROJECT_ROOT}/.github/cache/compose-override.txt"

if [[ -f "${COMPOSE_OVERRIDE_FILE}" ]]; then
    COMPOSE_OVERRIDE=$(cat "${COMPOSE_OVERRIDE_FILE}")
    
    if [[ -f "${COMPOSE_OVERRIDE}" ]]; then
        echo "Stopping services using: ${COMPOSE_OVERRIDE}"
        
        cd "${PROJECT_ROOT}"
        
        # Stop and remove containers
        docker-compose -f "${COMPOSE_OVERRIDE}" down --remove-orphans --volumes
        
        # Remove the override file
        rm -f "${COMPOSE_OVERRIDE}"
        
        echo "✅ Services stopped successfully"
    else
        echo "⚠️ Compose override file not found: ${COMPOSE_OVERRIDE}"
    fi
    
    # Clean up cache files
    rm -f "${COMPOSE_OVERRIDE_FILE}"
    rm -f "${STARTED_SERVICES_FILE}"
else
    echo "⚠️ No service information found - attempting to stop default services"
    
    cd "${PROJECT_ROOT}"
    
    # Try to stop common test services
    docker-compose down --remove-orphans --volumes 2>/dev/null || true
    
    # Stop any containers that might be running
    docker stop $(docker ps -q --filter "label=com.docker.compose.project=zamaz-debate-mcp" 2>/dev/null) 2>/dev/null || true
    
    echo "✅ Default cleanup completed"
fi

# Clean up any dangling volumes and networks
echo "Cleaning up Docker resources..."
docker volume prune -f 2>/dev/null || true
docker network prune -f 2>/dev/null || true

echo "✅ Service cleanup completed"