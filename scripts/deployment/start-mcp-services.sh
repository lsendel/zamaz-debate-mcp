#!/bin/bash

echo "🚀 Starting MCP Services for Claude CLI Integration"
echo "=================================================="
echo ""

# Check if services are already running
if docker-compose -f docker-compose.yml -f docker-compose-java.yml ps | grep -q "Up"; then
    echo "✅ Services are already running!"
else
    echo "Starting services..."
    docker-compose -f docker-compose.yml -f docker-compose-java.yml up -d
    
    echo "Waiting for services to be healthy..."
    sleep 10
fi

echo ""
echo "🔌 MCP Services Available:"
echo "========================="
echo ""
echo "1️⃣  Organization Service"
echo "   - HTTP API: http://localhost:5005"
echo "   - MCP Endpoint: http://localhost:5005/mcp"
echo "   - Swagger UI: http://localhost:5005/swagger-ui.html"
echo ""
echo "2️⃣  LLM Gateway Service"
echo "   - HTTP API: http://localhost:5002"
echo "   - MCP Endpoint: http://localhost:5002/mcp"
echo "   - Swagger UI: http://localhost:5002/swagger-ui.html"
echo ""
echo "3️⃣  Debate Controller Service"
echo "   - HTTP API: http://localhost:5013"
echo "   - MCP Endpoint: http://localhost:5013/mcp"
echo "   - Swagger UI: http://localhost:5013/swagger-ui.html"
echo ""

echo "📋 Claude CLI Configuration:"
echo "==========================="
echo ""
echo "Add to your Claude Desktop config file (~/Library/Application Support/Claude/claude_desktop_config.json):"
echo ""
cat << 'EOF'
{
  "mcpServers": {
    "zamaz-organization": {
      "url": "http://localhost:5005/mcp",
      "transport": "http"
    },
    "zamaz-llm": {
      "url": "http://localhost:5002/mcp",
      "transport": "http"
    },
    "zamaz-debate": {
      "url": "http://localhost:5013/mcp",
      "transport": "http"
    }
  }
}
EOF

echo ""
echo "🎯 Sample Claude CLI Commands:"
echo "============================="
echo ""
echo "# List available tools from organization service:"
echo "claude --mcp zamaz-organization list-tools"
echo ""
echo "# Create a new organization:"
echo 'claude --mcp zamaz-organization call-tool create_organization '"'"'{"name": "My Org", "description": "Test organization"}'"'"
echo ""
echo "# List LLM providers:"
echo "claude --mcp zamaz-llm call-tool list_providers"
echo ""
echo "# Create a new debate:"
echo 'claude --mcp zamaz-debate call-tool create_debate '"'"'{"topic": "AI Ethics", "format": "OXFORD", "participants": ["claude", "gpt-4"]}"'"''
echo ""
echo "# Get debate status:"
echo 'claude --mcp zamaz-debate call-tool get_debate '"'"'{"debate_id": "123"}"'"''
echo ""

echo "🔍 Health Check:"
echo "==============="
echo ""
for port in 5005 5002 5013; do
    if curl -s http://localhost:"""$port"""/actuator/health > /dev/null; then
        echo "✅ Service on port """$port""" is healthy"
    else
        echo "❌ Service on port """$port""" is not responding"
    fi
done

echo ""
echo "📚 Documentation:"
echo "================"
echo "- Organization API Docs: http://localhost:5005/swagger-ui.html"
echo "- LLM Gateway API Docs: http://localhost:5002/swagger-ui.html"
echo "- Debate Controller API Docs: http://localhost:5013/swagger-ui.html"
echo ""
echo "✨ MCP Services are ready for Claude CLI!"