#!/bin/bash

# Test Database Connections Script
echo "🧪 Testing Database Connections"
echo "==============================="

# Test PostgreSQL
echo -n "PostgreSQL: "
if docker exec zamaz-debate-mcp-postgres-1 pg_isready -U postgres > /dev/null 2>&1; then
    echo "✅ Connected"
else
    echo "❌ Failed"
fi

# Test Redis
echo -n "Redis: "
if docker exec zamaz-debate-mcp-redis-1 redis-cli ping > /dev/null 2>&1; then
    echo "✅ Connected"
else
    echo "❌ Failed"
fi

# Test Qdrant
echo -n "Qdrant: "
if curl -s http://localhost:6333/ > /dev/null 2>&1; then
    echo "✅ Connected"
else
    echo "❌ Failed"
fi

echo ""
echo "Database services are ready for MCP testing!"