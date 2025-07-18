#!/bin/bash

# Script to start backend services for UI development
# This starts the services locally with Maven instead of Docker

echo "Starting backend services for UI development..."

# Check if infrastructure is running
if ! docker ps | grep -q postgres; then
    echo "Starting infrastructure services (PostgreSQL, Redis, Qdrant)..."
    docker-compose -f infrastructure/docker-compose/docker-compose.yml up -d postgres redis qdrant
    sleep 5
fi

# Set environment variables
export JWT_SECRET="test-secret-key-that-is-at-least-256-bits-long-for-testing"
export DB_HOST=localhost
export DB_PORT=5432
export DB_USER=postgres
export DB_PASSWORD=postgres
export REDIS_HOST=localhost
export REDIS_PORT=6379
export LOG_LEVEL=INFO

# For LLM service - you'll need to set these with real API keys
export ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY:-"sk-ant-api03-test-key"}
export OPENAI_API_KEY=${OPENAI_API_KEY:-"sk-test-key"}
export GOOGLE_API_KEY=${GOOGLE_API_KEY:-"test-key"}

# Create a PID file to track processes
PID_FILE="/tmp/mcp-services.pids"
> $PID_FILE

# Function to start a service
start_service() {
    local service_name=$1
    local service_dir=$2
    local port=$3
    
    echo "Starting """$service_name""" on port """$port"""..."
    cd """"$service_dir"""" || exit 1
    
    # Start the service in background
    mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port="""$port"""" > "/tmp/"""$service_name""".log" 2>&1 &
    local pid=$!
    echo """"$pid"""" >> $PID_FILE
    echo """"$service_name""" started with PID: """$pid""" (log: /tmp/"""$service_name""".log)"
    
    cd - > /dev/null
}

# Start services
start_service "Organization API" "mcp-organization" 5005
sleep 10  # Give time for the first service to start

start_service "LLM API" "mcp-llm" 5002
sleep 10

start_service "Debate Controller" "mcp-controller" 5013
sleep 10

echo ""
echo "Services are starting up. Check logs with:"
echo "  tail -f /tmp/Organization_API.log"
echo "  tail -f /tmp/LLM_API.log"
echo "  tail -f /tmp/Debate_Controller.log"
echo ""
echo "Service URLs:"
echo "  Organization API: http://localhost:5005 (Swagger: http://localhost:5005/swagger-ui.html)"
echo "  LLM API: http://localhost:5002 (Swagger: http://localhost:5002/swagger-ui.html)"
echo "  Debate Controller: http://localhost:5013 (Swagger: http://localhost:5013/swagger-ui.html)"
echo ""
echo "To stop all services, run: ./scripts/stop-backend-services.sh"