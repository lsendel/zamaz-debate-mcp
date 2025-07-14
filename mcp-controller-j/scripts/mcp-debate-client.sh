#!/bin/bash

# MCP Debate Client Script
# This script demonstrates how to use the debate MCP service with Claude CLI

echo "🎭 MCP DEBATE CLIENT"
echo "==================="
echo ""

# Configuration
MCP_SERVICE="zamaz-debate"
ORG_ID="123e4567-e89b-12d3-a456-426614174000"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Function to call MCP tool
call_mcp_tool() {
    local tool_name=$1
    local args=$2
    echo -e "${BLUE}Calling MCP tool: $tool_name${NC}"
    echo "claude --mcp $MCP_SERVICE call-tool $tool_name '$args'"
    echo ""
}

echo "Prerequisites:"
echo "-------------"
echo "1. Ensure services are running: ./start-mcp-services.sh"
echo "2. Add to ~/Library/Application Support/Claude/claude_desktop_config.json:"
echo ""
cat << 'EOF'
{
  "mcpServers": {
    "zamaz-debate": {
      "url": "http://localhost:5013/mcp",
      "transport": "http"
    }
  }
}
EOF
echo ""
echo "Commands to run:"
echo "---------------"
echo ""

# 1. List available tools
echo -e "${PURPLE}1. List available debate tools:${NC}"
call_mcp_tool "list-tools" "{}"
echo "claude --mcp $MCP_SERVICE list-tools"
echo ""

# 2. Create a debate
echo -e "${PURPLE}2. Create a new debate:${NC}"
DEBATE_ARGS=$(cat << EOF
{
  "topic": "Should I use Airtable and DAG in the system?",
  "format": "OXFORD",
  "organizationId": "$ORG_ID",
  "maxRounds": 3
}
EOF
)
call_mcp_tool "create_debate" "$DEBATE_ARGS"

# 3. Get debate status
echo -e "${PURPLE}3. Get debate details:${NC}"
call_mcp_tool "get_debate" '{"debateId": "DEBATE_ID_HERE"}'

# 4. List all debates
echo -e "${PURPLE}4. List debates for organization:${NC}"
call_mcp_tool "list_debates" "{\"organizationId\": \"$ORG_ID\"}"

# 5. Submit a turn
echo -e "${PURPLE}5. Submit a debate turn:${NC}"
TURN_ARGS=$(cat << EOF
{
  "debateId": "DEBATE_ID_HERE",
  "participantId": "PARTICIPANT_ID_HERE",
  "content": "Airtable provides rapid development with a visual interface..."
}
EOF
)
call_mcp_tool "submit_turn" "$TURN_ARGS"

echo ""
echo -e "${YELLOW}=== Natural Language Examples ===${NC}"
echo ""
echo "You can also use natural language in Claude Desktop:"
echo ""
echo '1. "Using zamaz-debate, create a debate about AI ethics"'
echo '2. "List all debates in organization 123e4567-e89b-12d3-a456-426614174000"'
echo '3. "Get the status of debate [debate-id]"'
echo '4. "Submit a turn arguing for open source AI in debate [debate-id]"'
echo ""

echo -e "${GREEN}=== Direct HTTP Examples ===${NC}"
echo ""
echo "If MCP isn't working, use direct HTTP:"
echo ""

# Direct HTTP examples
cat << 'EOF'
# Create debate
curl -X POST http://localhost:5013/api/v1/debates \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "123e4567-e89b-12d3-a456-426614174000",
    "title": "Airtable vs PostgreSQL",
    "topic": "Should I use Airtable and DAG?",
    "description": "Evaluating database choices",
    "format": "OXFORD",
    "maxRounds": 3
  }'

# List debates
curl "http://localhost:5013/api/v1/debates?organizationId=123e4567-e89b-12d3-a456-426614174000"

# Get specific debate
curl "http://localhost:5013/api/v1/debates/{debate-id}"
EOF

echo ""
echo -e "${BLUE}=== Testing MCP Connection ===${NC}"
echo ""
echo "Test if MCP endpoint is available:"
echo "curl http://localhost:5013/mcp"
echo ""
echo "Get MCP server info:"
echo 'curl -X POST http://localhost:5013/mcp/list-tools -H "Content-Type: application/json" -d "{}"'