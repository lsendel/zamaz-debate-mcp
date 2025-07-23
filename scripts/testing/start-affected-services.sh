#!/bin/bash

# Start Affected Services
# Starts only the services that are affected by changes for efficient testing

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Parse command line arguments
AFFECTED_MODULES=""
ENVIRONMENT="test"

while [[ $# -gt 0 ]]; do
    case $1 in
        --modules=*)
            AFFECTED_MODULES="${1#*=}"
            shift
            ;;
        --environment=*)
            ENVIRONMENT="${1#*=}"
            shift
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

if [[ -z "${AFFECTED_MODULES}" ]]; then
    echo "Usage: $0 --modules=<json> [--environment=<env>]"
    exit 1
fi

echo "Starting affected services for testing"
echo "Affected modules: ${AFFECTED_MODULES}"
echo "Environment: ${ENVIRONMENT}"

# Parse affected modules
MODULES=$(echo "${AFFECTED_MODULES}" | jq -r '.[]' 2>/dev/null || echo "")

# Create docker-compose override for affected services
COMPOSE_OVERRIDE="${PROJECT_ROOT}/docker-compose.affected.yml"

cat > "${COMPOSE_OVERRIDE}" << 'EOF'
version: '3.8'

services:
EOF

# Always start core infrastructure services
cat >> "${COMPOSE_OVERRIDE}" << 'EOF'
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: zamaz_debate
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
EOF

# Add affected services to compose override
SERVICES_TO_START=()

while IFS= read -r module; do
    [[ -z "${module}" ]] && continue
    
    case "${module}" in
        mcp-organization)
            SERVICES_TO_START+=("mcp-organization")
            cat >> "${COMPOSE_OVERRIDE}" << 'EOF'

  mcp-organization:
    build:
      context: ./mcp-organization
      dockerfile: Dockerfile
    ports:
      - "5005:5005"
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/zamaz_debate
      - SPRING_REDIS_HOST=redis
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5005/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
EOF
            ;;
        mcp-gateway)
            SERVICES_TO_START+=("mcp-gateway")
            cat >> "${COMPOSE_OVERRIDE}" << 'EOF'

  mcp-gateway:
    build:
      context: ./mcp-gateway
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - SPRING_REDIS_HOST=redis
    depends_on:
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
EOF
            ;;
        mcp-controller|mcp-debate-engine)
            SERVICES_TO_START+=("mcp-controller")
            cat >> "${COMPOSE_OVERRIDE}" << 'EOF'

  mcp-controller:
    build:
      context: ./mcp-controller
      dockerfile: Dockerfile
    ports:
      - "5013:5013"
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/zamaz_debate
      - SPRING_REDIS_HOST=redis
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5013/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
EOF
            ;;
        mcp-llm)
            SERVICES_TO_START+=("mcp-llm")
            cat >> "${COMPOSE_OVERRIDE}" << 'EOF'

  mcp-llm:
    build:
      context: ./mcp-llm
      dockerfile: Dockerfile
    ports:
      - "5002:5002"
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - SPRING_REDIS_HOST=redis
    depends_on:
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5002/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
EOF
            ;;
        mcp-rag)
            SERVICES_TO_START+=("mcp-rag")
            cat >> "${COMPOSE_OVERRIDE}" << 'EOF'

  mcp-rag:
    build:
      context: ./mcp-rag
      dockerfile: Dockerfile
    ports:
      - "5004:5004"
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/zamaz_debate
      - SPRING_REDIS_HOST=redis
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5004/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
EOF
            ;;
        mcp-template)
            SERVICES_TO_START+=("mcp-template")
            cat >> "${COMPOSE_OVERRIDE}" << 'EOF'

  mcp-template:
    build:
      context: ./mcp-template
      dockerfile: Dockerfile
    ports:
      - "5006:5006"
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/zamaz_debate
      - SPRING_REDIS_HOST=redis
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5006/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
EOF
            ;;
        mcp-context)
            SERVICES_TO_START+=("mcp-context")
            cat >> "${COMPOSE_OVERRIDE}" << 'EOF'

  mcp-context:
    build:
      context: ./mcp-context
      dockerfile: Dockerfile
    ports:
      - "5001:5001"
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/zamaz_debate
      - SPRING_REDIS_HOST=redis
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5001/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
EOF
            ;;
        debate-ui)
            # Frontend doesn't need to be started for backend integration tests
            echo "Frontend module detected - will be handled separately"
            ;;
        infrastructure|ci-cd|config)
            # These don't correspond to specific services
            echo "Infrastructure/config module detected - no specific service to start"
            ;;
    esac
done <<< "${MODULES}"

# Start the services
echo "Starting services: ${SERVICES_TO_START[*]}"

cd "${PROJECT_ROOT}"

# Stop any existing services first
docker-compose -f "${COMPOSE_OVERRIDE}" down --remove-orphans 2>/dev/null || true

# Start the affected services
docker-compose -f "${COMPOSE_OVERRIDE}" up -d

# Wait for services to be healthy
echo "Waiting for services to be healthy..."
TIMEOUT=300  # 5 minutes
ELAPSED=0
INTERVAL=10

while [[ ${ELAPSED} -lt ${TIMEOUT} ]]; do
    ALL_HEALTHY=true
    
    # Check infrastructure services
    if ! docker-compose -f "${COMPOSE_OVERRIDE}" ps postgres | grep -q "healthy"; then
        ALL_HEALTHY=false
    fi
    
    if ! docker-compose -f "${COMPOSE_OVERRIDE}" ps redis | grep -q "healthy"; then
        ALL_HEALTHY=false
    fi
    
    # Check application services
    for service in "${SERVICES_TO_START[@]}"; do
        if ! docker-compose -f "${COMPOSE_OVERRIDE}" ps "${service}" | grep -q "healthy"; then
            ALL_HEALTHY=false
            break
        fi
    done
    
    if [[ "${ALL_HEALTHY}" == "true" ]]; then
        echo "✅ All services are healthy"
        break
    fi
    
    echo "⏳ Waiting for services to be healthy... (${ELAPSED}s/${TIMEOUT}s)"
    sleep ${INTERVAL}
    ELAPSED=$((ELAPSED + INTERVAL))
done

if [[ ${ELAPSED} -ge ${TIMEOUT} ]]; then
    echo "❌ Timeout waiting for services to be healthy"
    echo "Service status:"
    docker-compose -f "${COMPOSE_OVERRIDE}" ps
    echo "Logs:"
    docker-compose -f "${COMPOSE_OVERRIDE}" logs --tail=50
    exit 1
fi

# Verify service connectivity
echo "Verifying service connectivity..."
CONNECTIVITY_SUCCESS=true

# Test database connectivity
if ! docker-compose -f "${COMPOSE_OVERRIDE}" exec -T postgres pg_isready -U postgres; then
    echo "❌ Database connectivity failed"
    CONNECTIVITY_SUCCESS=false
fi

# Test Redis connectivity
if ! docker-compose -f "${COMPOSE_OVERRIDE}" exec -T redis redis-cli ping | grep -q "PONG"; then
    echo "❌ Redis connectivity failed"
    CONNECTIVITY_SUCCESS=false
fi

# Test application service endpoints
for service in "${SERVICES_TO_START[@]}"; do
    case "${service}" in
        mcp-organization)
            if ! curl -f -s http://localhost:5005/actuator/health >/dev/null; then
                echo "❌ ${service} health check failed"
                CONNECTIVITY_SUCCESS=false
            fi
            ;;
        mcp-gateway)
            if ! curl -f -s http://localhost:8080/actuator/health >/dev/null; then
                echo "❌ ${service} health check failed"
                CONNECTIVITY_SUCCESS=false
            fi
            ;;
        mcp-controller)
            if ! curl -f -s http://localhost:5013/actuator/health >/dev/null; then
                echo "❌ ${service} health check failed"
                CONNECTIVITY_SUCCESS=false
            fi
            ;;
        mcp-llm)
            if ! curl -f -s http://localhost:5002/actuator/health >/dev/null; then
                echo "❌ ${service} health check failed"
                CONNECTIVITY_SUCCESS=false
            fi
            ;;
        mcp-rag)
            if ! curl -f -s http://localhost:5004/actuator/health >/dev/null; then
                echo "❌ ${service} health check failed"
                CONNECTIVITY_SUCCESS=false
            fi
            ;;
        mcp-template)
            if ! curl -f -s http://localhost:5006/actuator/health >/dev/null; then
                echo "❌ ${service} health check failed"
                CONNECTIVITY_SUCCESS=false
            fi
            ;;
        mcp-context)
            if ! curl -f -s http://localhost:5001/actuator/health >/dev/null; then
                echo "❌ ${service} health check failed"
                CONNECTIVITY_SUCCESS=false
            fi
            ;;
    esac
done

if [[ "${CONNECTIVITY_SUCCESS}" == "true" ]]; then
    echo "✅ All services are running and accessible"
    echo "Services started successfully:"
    for service in "${SERVICES_TO_START[@]}"; do
        echo "  - ${service}"
    done
    
    # Save service information for cleanup
    echo "${SERVICES_TO_START[*]}" > "${PROJECT_ROOT}/.github/cache/started-services.txt"
    echo "${COMPOSE_OVERRIDE}" > "${PROJECT_ROOT}/.github/cache/compose-override.txt"
    
    exit 0
else
    echo "❌ Some services failed connectivity checks"
    echo "Service logs:"
    docker-compose -f "${COMPOSE_OVERRIDE}" logs --tail=100
    exit 1
fi