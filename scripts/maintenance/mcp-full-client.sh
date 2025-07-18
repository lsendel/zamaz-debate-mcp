#!/bin/bash

# MCP Full Client Script - All Services Demo
# This script shows how to use all MCP services together

echo "🚀 MCP SERVICES FULL CLIENT DEMO"
echo "================================"
echo ""

# Configuration
ORG_SERVICE="zamaz-organization"
LLM_SERVICE="zamaz-llm"
DEBATE_SERVICE="zamaz-debate"
ORG_ID="123e4567-e89b-12d3-a456-426614174000"

# Base URLs for direct HTTP
ORG_URL="http://localhost:5005"
LLM_URL="http://localhost:5002"
DEBATE_URL="http://localhost:5013"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}=== MCP CONFIGURATION ===${NC}"
echo ""
echo "Add this to ~/Library/Application Support/Claude/claude_desktop_config.json:"
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
echo -e "${BLUE}=== 1. ORGANIZATION SERVICE ===${NC}"
echo ""

echo "List tools:"
echo "claude --mcp """$ORG_SERVICE""" list-tools"
echo ""

echo "Create organization:"
echo 'claude --mcp zamaz-organization call-tool create_organization \'{"name": "AI Debate Society", "description": "Debates about AI topics"}\''
echo ""

echo "Direct HTTP:"
echo "curl -X POST """$ORG_URL"""/api/v1/organizations -H 'Content-Type: application/json' -d '{\"name\": \"AI Debate Society\"}'"
echo ""

echo -e "${BLUE}=== 2. LLM SERVICE ===${NC}"
echo ""

echo "List providers:"
echo "claude --mcp """$LLM_SERVICE""" call-tool list_providers"
echo ""

echo "Generate completion:"
echo 'claude --mcp zamaz-llm call-tool generate_completion \'{"provider": "claude", "prompt": "What is MCP?", "maxTokens": 100}\''
echo ""

echo "Direct HTTP (correct format):"
cat << 'EOF'
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "messages": [
      {"role": "user", "content": "What is MCP?"}
    ],
    "maxTokens": 100
  }'
EOF
echo ""

echo -e "${BLUE}=== 3. DEBATE SERVICE ===${NC}"
echo ""

echo "Create debate:"
cat << EOF
claude --mcp """$DEBATE_SERVICE""" call-tool create_debate '{
  "topic": "Should we use Airtable?",
  "format": "OXFORD",
  "organizationId": """"$ORG_ID"""",
  "participants": ["Pro-Airtable", "Anti-Airtable"],
  "maxRounds": 3
}'
EOF
echo ""

echo "Direct HTTP (working example):"
cat << EOF
curl -X POST """$DEBATE_URL"""/api/v1/debates \\
  -H "Content-Type: application/json" \\
  -d '{
    "organizationId": """"$ORG_ID"""",
    "title": "Airtable vs PostgreSQL",
    "topic": "Should I use Airtable and DAG?",
    "description": "Database choice evaluation",
    "format": "OXFORD",
    "maxRounds": 3
  }'
EOF
echo ""

echo -e "${PURPLE}=== COMPLETE WORKFLOW EXAMPLE ===${NC}"
echo ""

echo "1. Create organization (if needed):"
echo "   → Use organization service to create 'Tech Debates Inc'"
echo ""

echo "2. Create a debate:"
echo "   → Use debate service to create 'Airtable vs PostgreSQL' debate"
echo ""

echo "3. Add participants:"
echo "   → Add 'Pro-Airtable Advocate' (using Claude)"
echo "   → Add 'PostgreSQL Expert' (using GPT-4)"
echo ""

echo "4. Run debate rounds:"
echo "   → Use LLM service to generate arguments for each participant"
echo "   → Submit turns to debate service"
echo ""

echo "5. Get results:"
echo "   → Fetch complete debate transcript"
echo "   → Use LLM service to analyze and summarize"
echo ""

echo -e "${GREEN}=== WORKING EXAMPLES ===${NC}"
echo ""

echo "1. Create a debate (verified working):"
echo ""
cat << EOF
DEBATE_ID=\$(curl -s -X POST """$DEBATE_URL"""/api/v1/debates \\
  -H "Content-Type: application/json" \\
  -d '{
    "organizationId": """"$ORG_ID"""",
    "title": "Database Architecture Decision",
    "topic": "Airtable + DAG vs PostgreSQL + Queues",
    "description": "Evaluating modern vs traditional architectures",
    "format": "OXFORD",
    "maxRounds": 3
  }' | jq -r .id)

echo "Created debate: \"""$DEBATE_ID""""
EOF
echo ""

echo "2. List all debates:"
echo "curl '"""$DEBATE_URL"""/api/v1/debates?organizationId="""$ORG_ID"""' | jq ."
echo ""

echo "3. Get debate details:"
echo "curl '"""$DEBATE_URL"""/api/v1/debates/\"""$DEBATE_ID"""' | jq ."
echo ""

echo -e "${RED}=== CURRENT STATUS ===${NC}"
echo ""
echo "✅ Working:"
echo "   - Debate creation endpoint"
echo "   - Debate listing endpoint"
echo "   - Service health checks"
echo ""
echo "⚠️  Needs Implementation:"
echo "   - MCP endpoints (/mcp routes)"
echo "   - Participant management"
echo "   - Turn submission"
echo "   - LLM message handling"
echo ""

echo -e "${YELLOW}=== QUICK TEST ===${NC}"
echo ""
echo "Run this to create a test debate right now:"
echo ""
echo "curl -X POST """$DEBATE_URL"""/api/v1/debates -H 'Content-Type: application/json' -d '{\"organizationId\":\""""$ORG_ID"""\",\"title\":\"Test Debate\",\"topic\":\"Is MCP useful?\",\"format\":\"OXFORD\",\"maxRounds\":2}' | jq ."