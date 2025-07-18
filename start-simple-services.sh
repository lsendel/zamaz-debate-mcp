#!/bin/bash

echo "🚀 Starting Simple MCP Services..."
echo "================================="

# Kill any existing processes
echo "Stopping existing services..."
pkill -f "simple-organization-service.js" 2>/dev/null || true
pkill -f "simple-llm-service.js" 2>/dev/null || true
pkill -f "simple-debate-service.js" 2>/dev/null || true
sleep 2

# Check if Node.js is available
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js first."
    exit 1
fi

# Install dependencies if needed
if [ ! -f package.json ]; then
    echo "📦 Creating package.json..."
    cat > package.json << EOF
{
  "name": "simple-mcp-services",
  "version": "1.0.0",
  "description": "Simple MCP services for testing",
  "main": "index.js",
  "scripts": {
    "start": "node simple-organization-service.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5"
  }
}
EOF
fi

# Install dependencies
if [ ! -d node_modules ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

# Start services in background
echo ""
echo "🏢 Starting Organization Service on port 5005..."
node simple-organization-service.js > logs/organization-simple.log 2>&1 &
ORG_PID=$!

sleep 2

echo "🧠 Starting LLM Service on port 5002..."
node simple-llm-service.js > logs/llm-simple.log 2>&1 &
LLM_PID=$!

sleep 2

echo "💬 Starting Debate Service on port 5013..."
node simple-debate-service.js > logs/debate-simple.log 2>&1 &
DEBATE_PID=$!

sleep 3

echo ""
echo "✅ All services started successfully!"
echo "Service PIDs:"
echo "  Organization: $ORG_PID"
echo "  LLM: $LLM_PID"
echo "  Debate: $DEBATE_PID"
echo ""

# Health check
echo "🔍 Health Check:"
echo "==============="
for port in 5005 5002 5013; do
    if curl -s http://localhost:$port/actuator/health > /dev/null; then
        echo "✅ Service on port $port is healthy"
    else
        echo "❌ Service on port $port is not responding"
    fi
done

echo ""
echo "🌐 Service URLs:"
echo "==============="
echo "Organization API: http://localhost:5005/api/v1/organizations"
echo "LLM API: http://localhost:5002/api/v1/providers"
echo "Debate API: http://localhost:5013/api/v1/debates"
echo ""
echo "📱 UI Application: http://localhost:3001"
echo ""
echo "🛑 To stop services, run: ./stop-simple-services.sh"
echo ""
echo "✨ Simple MCP Services are ready!"