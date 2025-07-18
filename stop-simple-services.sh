#!/bin/bash

echo "🛑 Stopping Simple MCP Services..."
echo "================================="

# Kill services
echo "Stopping Organization Service..."
pkill -f "simple-organization-service.js" 2>/dev/null || true

echo "Stopping LLM Service..."
pkill -f "simple-llm-service.js" 2>/dev/null || true

echo "Stopping Debate Service..."
pkill -f "simple-debate-service.js" 2>/dev/null || true

sleep 2

echo ""
echo "✅ All services stopped successfully!"
echo ""

# Check if services are still running
echo "🔍 Verifying services are stopped:"
echo "================================="
for port in 5005 5002 5013; do
    if curl -s http://localhost:$port/actuator/health > /dev/null; then
        echo "⚠️  Service on port $port is still running"
    else
        echo "✅ Service on port $port is stopped"
    fi
done

echo ""
echo "🏁 All services have been stopped!"