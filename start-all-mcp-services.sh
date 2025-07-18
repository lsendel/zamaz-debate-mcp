#!/bin/bash

# Comprehensive MCP Services Startup Script
echo "🚀 Starting All MCP Services for Testing..."

# Set environment variables
export JWT_SECRET="test-secret-key-that-is-at-least-256-bits-long-for-testing-purposes-only"
export DB_HOST=localhost
export DB_PORT=5432
export DB_USER=postgres
export DB_PASSWORD=postgres
export REDIS_HOST=localhost
export REDIS_PORT=6379
export QDRANT_HOST=localhost
export QDRANT_PORT=6333

# Function to check if port is available
check_port() {
    local port=$1
    if lsof -Pi :""$port"" -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo "⚠️  Port ""$port"" is already in use"
        return 1
    fi
    return 0
}

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1
    
    echo "⏳ Waiting for ""$service_name"" to be ready..."
    while [ ""$attempt"" -le ""$max_attempts"" ]; do
        if curl -s """$url""" > /dev/null 2>&1; then
            echo "✅ ""$service_name"" is ready!"
            return 0
        fi
        echo "   Attempt ""$attempt""/""$max_attempts"" - waiting..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "❌ ""$service_name"" failed to start within timeout"
    return 1
}

# Check if supporting services are running
echo "🔍 Checking supporting services..."
if ! docker ps | grep -q postgres; then
    echo "❌ PostgreSQL is not running. Please start with: docker-compose up -d postgres"
    exit 1
fi

if ! docker ps | grep -q redis; then
    echo "❌ Redis is not running. Please start with: docker-compose up -d redis"
    exit 1
fi

# Check ports
echo "🔍 Checking port availability..."
check_port 5005 || exit 1  # Organization
check_port 5007 || exit 1  # Context
check_port 5002 || exit 1  # LLM
check_port 5013 || exit 1  # Controller
check_port 5018 || exit 1  # RAG

# Start services
cd /Users/lsendel/IdeaProjects/zamaz-debate-mcp

echo "📋 Starting Organization Service (port 5005)..."
cd mcp-organization
nohup mvn spring-boot:run -Dspring-boot.run.profiles=test > ../logs/organization.log 2>&1 &
ORG_PID=$!
cd ..

echo "🔍 Starting Context Service (port 5007)..."
cd mcp-context
nohup mvn spring-boot:run -Dspring-boot.run.profiles=test > ../logs/context.log 2>&1 &
CONTEXT_PID=$!
cd ..

echo "🤖 Starting LLM Service (port 5002)..."
cd mcp-llm
nohup mvn spring-boot:run -Dspring-boot.run.profiles=test > ../logs/llm.log 2>&1 &
LLM_PID=$!
cd ..

echo "💬 Starting Controller Service (port 5013)..."
cd mcp-controller
nohup mvn spring-boot:run -Dspring-boot.run.profiles=test > ../logs/controller.log 2>&1 &
CONTROLLER_PID=$!
cd ..

echo "📚 Starting RAG Service (port 5018)..."
cd mcp-rag
nohup mvn spring-boot:run -Dspring-boot.run.profiles=test > ../logs/rag.log 2>&1 &
RAG_PID=$!
cd ..

# Store PIDs for cleanup
echo """$ORG_PID"" ""$CONTEXT_PID"" ""$LLM_PID"" ""$CONTROLLER_PID"" ""$RAG_PID""" > mcp-services.pids

echo "📝 Service PIDs:"
echo "   Organization: ""$ORG_PID"""
echo "   Context: ""$CONTEXT_PID"""
echo "   LLM: ""$LLM_PID"""
echo "   Controller: ""$CONTROLLER_PID"""
echo "   RAG: ""$RAG_PID"""

# Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 10

# Check each service
wait_for_service "http://localhost:5005/actuator/health" "Organization Service" &
wait_for_service "http://localhost:5007/actuator/health" "Context Service" &
wait_for_service "http://localhost:5002/actuator/health" "LLM Service" &
wait_for_service "http://localhost:5013/actuator/health" "Controller Service" &
wait_for_service "http://localhost:5018/actuator/health" "RAG Service" &

# Wait for all background checks to complete
wait

echo "🎉 All MCP services are running!"
echo "📊 Ready to run comprehensive tests."
echo ""
echo "Service URLs:"
echo "   Organization: http://localhost:5005"
echo "   Context:      http://localhost:5007"
echo "   LLM:          http://localhost:5002"
echo "   Controller:   http://localhost:5013"
echo "   RAG:          http://localhost:5018"
echo ""
echo "💡 To stop services, run: ./stop-mcp-services.sh"