#!/bin/bash

# Script to start MCP services using existing Docker images

echo "Starting MCP services using Docker..."

# Source the .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Ensure network exists
docker network create mcp-network 2>/dev/null || true

# Start Organization Service
echo "Starting Organization Service..."
docker run -d \
    --name mcp-organization \
    --network mcp-network \
    -p ${MCP_ORGANIZATION_PORT:-5005}:5005 \
    -e DB_HOST=host.docker.internal \
    -e DB_PORT=${DB_PORT:-5432} \
    -e DB_NAME=organization_db \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres123 \
    -e REDIS_HOST=host.docker.internal \
    -e REDIS_PORT=${REDIS_PORT:-6379} \
    -e JWT_SECRET="${JWT_SECRET}" \
    -e SERVER_PORT=5005 \
    -e MCP_ORGANIZATION_PORT=5005 \
    -e CORS_ORIGINS="http://localhost:${UI_PORT:-3001}" \
    -e LOG_LEVEL=${LOG_LEVEL:-INFO} \
    zamaz-debate-mcp-mcp-organization:latest

# Start LLM Service
echo "Starting LLM Service..."
docker run -d \
    --name mcp-llm \
    --network mcp-network \
    -p ${MCP_LLM_PORT:-5002}:5002 \
    -e REDIS_HOST=host.docker.internal \
    -e REDIS_PORT=${REDIS_PORT:-6379} \
    -e ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY}" \
    -e OPENAI_API_KEY="${OPENAI_API_KEY}" \
    -e GOOGLE_API_KEY="${GOOGLE_API_KEY}" \
    -e SERVER_PORT=5002 \
    -e MCP_LLM_PORT=5002 \
    -e LOG_LEVEL=${LOG_LEVEL:-INFO} \
    zamaz-debate-mcp-mcp-llm:latest

# Start Controller Service
echo "Starting Controller Service..."
docker run -d \
    --name mcp-controller \
    --network mcp-network \
    -p ${MCP_CONTROLLER_PORT:-5013}:5013 \
    -e DB_HOST=host.docker.internal \
    -e DB_PORT=${DB_PORT:-5432} \
    -e DB_NAME=debate_db \
    -e DB_USER=postgres \
    -e DB_PASSWORD=postgres123 \
    -e REDIS_HOST=host.docker.internal \
    -e REDIS_PORT=${REDIS_PORT:-6379} \
    -e ORGANIZATION_SERVICE_URL=http://mcp-organization:5005 \
    -e LLM_SERVICE_URL=http://mcp-llm:5002 \
    -e SERVER_PORT=5013 \
    -e MCP_CONTROLLER_PORT=5013 \
    -e CORS_ALLOWED_ORIGINS="http://localhost:${UI_PORT:-3001}" \
    -e LOG_LEVEL=${LOG_LEVEL:-INFO} \
    zamaz-debate-mcp-mcp-controller:latest

echo ""
echo "Services started. Checking status..."
sleep 5

# Check if services are running
echo ""
echo "Service Status:"
echo "==============="
for service in mcp-organization mcp-llm mcp-controller; do
    if docker ps | grep -q "$service"; then
        echo "✅ "$service" is running"
    else
        echo "❌ "$service" failed to start"
        echo "Logs:"
        docker logs "$service" 2>&1 | tail -20
    fi
done

echo ""
echo "Service URLs:"
echo "============="
echo "Organization API: http://localhost:${MCP_ORGANIZATION_PORT:-5005}"
echo "LLM API: http://localhost:${MCP_LLM_PORT:-5002}"
echo "Controller API: http://localhost:${MCP_CONTROLLER_PORT:-5013}"
echo ""
echo "To view logs: docker logs -f <service-name>"
echo "To stop services: ./scripts/stop-services-docker.sh"