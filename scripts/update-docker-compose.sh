#!/bin/bash

# Script to update docker-compose.yml with Config Server dependencies
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCKER_COMPOSE="$PROJECT_ROOT/infrastructure/docker-compose/docker-compose.yml"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Services to update
SERVICES=(
    "mcp-gateway"
    "mcp-auth-server"
    "mcp-sidecar"
    "mcp-debate-engine"
    "mcp-pattern-recognition"
    "github-integration"
    "mcp-modulith"
    "mcp-docs"
    "mcp-context-client"
    "mcp-debate"
)

# Create backup
cp "$DOCKER_COMPOSE" "$DOCKER_COMPOSE.backup"
echo -e "${GREEN}Created backup: $DOCKER_COMPOSE.backup${NC}"

# Update services that already exist in docker-compose
echo -e "${GREEN}Updating docker-compose.yml...${NC}"

# For services that already have Config Server configured, we'll skip them
# For new services, we need to check if they exist in docker-compose

for service in "${SERVICES[@]}"; do
    # Check if service exists in docker-compose
    if ! grep -q "^  $service:" "$DOCKER_COMPOSE"; then
        echo -e "${YELLOW}Service $service not found in docker-compose.yml${NC}"
        continue
    fi
    
    # Check if already has CONFIG_SERVER_URI
    if grep -A20 "^  $service:" "$DOCKER_COMPOSE" | grep -q "CONFIG_SERVER_URI"; then
        echo -e "${YELLOW}Service $service already configured for Config Server${NC}"
        continue
    fi
    
    echo -e "${GREEN}Updating $service...${NC}"
    
    # This is complex to do with sed, so we'll provide manual instructions
    echo "  Add the following to the $service service:"
    echo "    environment:"
    echo "      - CONFIG_SERVER_URI=http://mcp-config-server:8888"
    echo "      - SPRING_PROFILES_ACTIVE=docker"
    echo "    depends_on:"
    echo "      mcp-config-server:"
    echo "        condition: service_healthy"
    echo ""
done

# Create a comprehensive docker-compose override file
cat > "$PROJECT_ROOT/infrastructure/docker-compose/docker-compose.config.yml" <<EOF
# Docker Compose override for Config Server integration
# Use with: docker-compose -f docker-compose.yml -f docker-compose.config.yml up

version: '3.8'

services:
  # Gateway Service
  mcp-gateway:
    environment:
      - CONFIG_SERVER_URI=http://mcp-config-server:8888
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      mcp-config-server:
        condition: service_healthy

  # Auth Server
  mcp-auth-server:
    environment:
      - CONFIG_SERVER_URI=http://mcp-config-server:8888
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      mcp-config-server:
        condition: service_healthy

  # Sidecar Service
  mcp-sidecar:
    build:
      context: ../..
      dockerfile: mcp-sidecar/Dockerfile
    environment:
      - CONFIG_SERVER_URI=http://mcp-config-server:8888
      - SPRING_PROFILES_ACTIVE=docker
      - SERVER_PORT=8090
    ports:
      - "\${MCP_SIDECAR_PORT:-8090}:8090"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      mcp-config-server:
        condition: service_healthy
    networks:
      - mcp-network

  # Debate Engine Service
  mcp-debate-engine:
    build:
      context: ../..
      dockerfile: mcp-debate-engine/Dockerfile
    environment:
      - CONFIG_SERVER_URI=http://mcp-config-server:8888
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=debate_engine_db
      - DB_USER=\${POSTGRES_USER:-postgres}
      - DB_PASSWORD=\${POSTGRES_PASSWORD:-postgres}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - SERVER_PORT=5020
    ports:
      - "\${MCP_DEBATE_ENGINE_PORT:-5020}:5020"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      mcp-config-server:
        condition: service_healthy
    networks:
      - mcp-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5020/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 40s

  # Pattern Recognition Service
  mcp-pattern-recognition:
    build:
      context: ../..
      dockerfile: mcp-pattern-recognition/Dockerfile
    environment:
      - CONFIG_SERVER_URI=http://mcp-config-server:8888
      - SPRING_PROFILES_ACTIVE=docker
      - SERVER_PORT=5030
    ports:
      - "\${MCP_PATTERN_PORT:-5030}:5030"
    depends_on:
      mcp-config-server:
        condition: service_healthy
    networks:
      - mcp-network
    volumes:
      - ../../:/workspace:ro

  # GitHub Integration Service
  github-integration:
    build:
      context: ../..
      dockerfile: github-integration/Dockerfile
    environment:
      - CONFIG_SERVER_URI=http://mcp-config-server:8888
      - SPRING_PROFILES_ACTIVE=docker
      - GITHUB_TOKEN=\${GITHUB_TOKEN}
      - SERVER_PORT=5040
    ports:
      - "\${GITHUB_INTEGRATION_PORT:-5040}:5040"
    depends_on:
      mcp-config-server:
        condition: service_healthy
    networks:
      - mcp-network

  # Modulith Service
  mcp-modulith:
    build:
      context: ../..
      dockerfile: mcp-modulith/Dockerfile
    environment:
      - CONFIG_SERVER_URI=http://mcp-config-server:8888
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=modulith_db
      - DB_USER=\${POSTGRES_USER:-postgres}
      - DB_PASSWORD=\${POSTGRES_PASSWORD:-postgres}
      - SERVER_PORT=5050
    ports:
      - "\${MCP_MODULITH_PORT:-5050}:5050"
    depends_on:
      postgres:
        condition: service_healthy
      mcp-config-server:
        condition: service_healthy
    networks:
      - mcp-network

  # Documentation Service
  mcp-docs:
    build:
      context: ../..
      dockerfile: mcp-docs/Dockerfile
    environment:
      - CONFIG_SERVER_URI=http://mcp-config-server:8888
      - SPRING_PROFILES_ACTIVE=docker
      - SERVER_PORT=8095
    ports:
      - "\${MCP_DOCS_PORT:-8095}:8095"
    depends_on:
      mcp-config-server:
        condition: service_healthy
    networks:
      - mcp-network
    volumes:
      - ../../docs:/app/docs:ro

  # Context Client Service
  mcp-context-client:
    build:
      context: ../..
      dockerfile: mcp-context-client/Dockerfile
    environment:
      - CONFIG_SERVER_URI=http://mcp-config-server:8888
      - SPRING_PROFILES_ACTIVE=docker
      - SERVER_PORT=5015
    ports:
      - "\${MCP_CONTEXT_CLIENT_PORT:-5015}:5015"
    depends_on:
      mcp-config-server:
        condition: service_healthy
    networks:
      - mcp-network

  # Legacy Debate Service
  mcp-debate:
    build:
      context: ../..
      dockerfile: mcp-debate/Dockerfile
    environment:
      - CONFIG_SERVER_URI=http://mcp-config-server:8888
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=debate_db
      - DB_USER=\${POSTGRES_USER:-postgres}
      - DB_PASSWORD=\${POSTGRES_PASSWORD:-postgres}
      - SERVER_PORT=5001
    ports:
      - "\${MCP_DEBATE_LEGACY_PORT:-5001}:5001"
    depends_on:
      postgres:
        condition: service_healthy
      mcp-config-server:
        condition: service_healthy
    networks:
      - mcp-network
    profiles:
      - legacy
EOF

echo -e "${GREEN}Created docker-compose.config.yml override file${NC}"
echo ""
echo "To use the Config Server with all services:"
echo "  cd infrastructure/docker-compose"
echo "  docker-compose -f docker-compose.yml -f docker-compose.config.yml up -d"
echo ""
echo "Or manually update docker-compose.yml with the configurations shown above."