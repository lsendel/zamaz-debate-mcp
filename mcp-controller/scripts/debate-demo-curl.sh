#!/bin/bash

# Debate Demo: "Should I use Airtable and DAG in the system?"
# This script demonstrates a full debate flow using curl

echo "🎭 ZAMAZ DEBATE SYSTEM - DEMO"
echo "============================="
echo "Topic: Should I use Airtable and DAG in the system?"
echo ""

# Base URLs
ORG_URL="http://localhost:5005"
LLM_URL="http://localhost:5002"
DEBATE_URL="http://localhost:5013"
API_V1="/api/v1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Create an organization
echo -e "${BLUE}Step 1: Creating organization...${NC}"
ORG_RESPONSE=$(curl -s -X POST """"$ORG_URL"""""$API_V1""/organizations" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tech Architecture Debates",
    "description": "Debates about technology choices"
  }')

ORG_ID=$(echo """$ORG_RESPONSE""" | jq -r '.id // empty')
if [ -z """"$ORG_ID"""" ]; then
  echo -e "${RED}Failed to create organization. Response: """$ORG_RESPONSE"""${NC}"
  # Try to get existing organization
  ORG_ID="org-123" # fallback
fi
echo -e "${GREEN}✅ Organization ID: """$ORG_ID"""${NC}"
echo ""

# Step 2: Create a debate
echo -e "${BLUE}Step 2: Creating debate...${NC}"
DEBATE_RESPONSE=$(curl -s -X POST """"$DEBATE_URL"""""$API_V1""/debates" \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Should I use Airtable and DAG in the system?",
    "description": "Evaluating the pros and cons of using Airtable as a database and DAG for workflow orchestration",
    "format": "OXFORD",
    "organizationId": "'"""$ORG_ID"""'",
    "maxRounds": 3,
    "maxResponseLength": 500,
    "participantNames": ["Pro-Airtable (Claude)", "Anti-Airtable (GPT-4)"]
  }')

DEBATE_ID=$(echo """$DEBATE_RESPONSE""" | jq -r '.id // empty')
if [ -z """"$DEBATE_ID"""" ]; then
  echo -e "${RED}Failed to create debate. Response: """$DEBATE_RESPONSE"""${NC}"
  exit 1
fi
echo -e "${GREEN}✅ Debate ID: """$DEBATE_ID"""${NC}"
echo -e "Topic: ${YELLOW}Should I use Airtable and DAG in the system?${NC}"
echo ""

# Get participant IDs
sleep 2
DEBATE_DETAILS=$(curl -s """"$DEBATE_URL"""""$API_V1""/debates/"""$DEBATE_ID"""")
PARTICIPANT_1_ID=$(echo """$DEBATE_DETAILS""" | jq -r '.participants[0].id // empty')
PARTICIPANT_2_ID=$(echo """$DEBATE_DETAILS""" | jq -r '.participants[1].id // empty')

echo "Participants:"
echo -e "  - ${GREEN}Pro-Airtable (Claude)${NC}: """$PARTICIPANT_1_ID""""
echo -e "  - ${RED}Anti-Airtable (GPT-4)${NC}: """$PARTICIPANT_2_ID""""
echo ""

# Function to submit a turn
submit_turn() {
  local participant_id=$1
  local content=$2
  local participant_name=$3
  
  echo -e "${BLUE}"""$participant_name""" is speaking...${NC}"
  
  TURN_RESPONSE=$(curl -s -X POST """"$DEBATE_URL"""""$API_V1""/debates/"""$DEBATE_ID"""/turns" \
    -H "Content-Type: application/json" \
    -d '{
      "participantId": "'"""$participant_id"""'",
      "content": "'""""$content""""'"
    }')
  
  echo """"$content""""
  echo ""
  sleep 2
}

# Round 1: Opening Statements
echo -e "${YELLOW}=== ROUND 1: OPENING STATEMENTS ===${NC}"
echo ""

submit_turn """"$PARTICIPANT_1_ID"""" \
  "Airtable and DAGs are excellent choices for modern systems. Airtable provides a flexible, no-code database with built-in UI, perfect for rapid prototyping and business user access. DAGs ensure clear workflow orchestration with dependency management, making complex processes maintainable and scalable. Together, they enable rapid development while maintaining system clarity." \
  "Pro-Airtable (Claude)"

submit_turn """"$PARTICIPANT_2_ID"""" \
  "While Airtable and DAGs seem appealing, they introduce significant limitations. Airtable lacks the performance and query capabilities of proper databases, with API rate limits and vendor lock-in risks. DAGs can overcomplicate simple workflows and create rigid structures that resist agile changes. Traditional databases with flexible orchestration provide better long-term value." \
  "Anti-Airtable (GPT-4)"

# Round 2: Rebuttals
echo -e "${YELLOW}=== ROUND 2: REBUTTALS ===${NC}"
echo ""

submit_turn """"$PARTICIPANT_1_ID"""" \
  "The concerns about limitations are overstated. Airtable's API limits are generous for most use cases, and its visual interface reduces development time by 70%. DAGs don't create rigidity - they provide clarity. Tools like Airflow or Prefect make modifications simple. The rapid iteration and reduced maintenance costs far outweigh theoretical performance concerns that may never materialize." \
  "Pro-Airtable (Claude)"

submit_turn """"$PARTICIPANT_2_ID"""" \
  "A 70% development time reduction means nothing when you hit Airtable's 5 requests/second limit in production. Real systems need sub-millisecond queries, not 200ms API calls. DAG modifications aren't simple when you have hundreds of interdependent tasks. PostgreSQL with Redis and simple async workers provide better performance, lower costs, and no vendor dependencies." \
  "Anti-Airtable (GPT-4)"

# Round 3: Closing Arguments
echo -e "${YELLOW}=== ROUND 3: CLOSING ARGUMENTS ===${NC}"
echo ""

submit_turn """"$PARTICIPANT_1_ID"""" \
  "Consider the total cost of ownership. Airtable eliminates database administration, provides instant collaboration features, and includes versioning, forms, and automations out-of-the-box. DAGs make system behavior predictable and testable. For startups and MVPs, shipping features quickly matters more than optimizing for Twitter-scale traffic you don't have yet." \
  "Pro-Airtable (Claude)"

submit_turn """"$PARTICIPANT_2_ID"""" \
  "Technical debt compounds quickly. Starting with Airtable means eventual migration pain when you need real database features like transactions, complex queries, or geographic replication. DAGs lock you into specific execution patterns. Build on proven foundations: PostgreSQL for data, Redis for caching, and simple service orchestration. Your future self will thank you." \
  "Anti-Airtable (GPT-4)"

# Step 3: Get full debate results
echo -e "${YELLOW}=== DEBATE COMPLETE ===${NC}"
echo ""
echo -e "${BLUE}Fetching final debate results...${NC}"
echo ""

FINAL_DEBATE=$(curl -s """"$DEBATE_URL"""""$API_V1""/debates/"""$DEBATE_ID"""")

echo "📊 DEBATE SUMMARY:"
echo "=================="
echo "Topic: $(echo """$FINAL_DEBATE""" | jq -r '.topic')"
echo "Status: $(echo """$FINAL_DEBATE""" | jq -r '.status')"
echo "Format: $(echo """$FINAL_DEBATE""" | jq -r '.format')"
echo "Total Rounds: $(echo """$FINAL_DEBATE""" | jq -r '.currentRound') / $(echo """$FINAL_DEBATE""" | jq -r '.maxRounds')"
echo ""

# Get all turns
echo "📝 ALL TURNS:"
echo "============="
TURNS=$(curl -s """"$DEBATE_URL"""""$API_V1""/debates/"""$DEBATE_ID"""/turns")
echo """$TURNS""" | jq -r '.[] | "Round \(.round) - \(.participant.name):\n\(.content)\n"'

# Step 4: Analyze the debate
echo -e "${YELLOW}🤔 ANALYSIS & VERDICT:${NC}"
echo "======================"
echo ""
echo "Key Arguments FOR Airtable/DAG:"
echo "• Rapid development and prototyping"
echo "• Built-in UI and collaboration"
echo "• Clear workflow orchestration"
echo "• Lower operational overhead"
echo ""
echo "Key Arguments AGAINST Airtable/DAG:"
echo "• Performance limitations and API rate limits"
echo "• Vendor lock-in risks"
echo "• Limited query capabilities"
echo "• Potential rigidity in workflows"
echo ""

# Optional: Get AI verdict
echo -e "${BLUE}Requesting AI verdict...${NC}"
VERDICT_RESPONSE=$(curl -s -X POST """"$LLM_URL"""""$API_V1""/completions" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "prompt": "Based on this debate about using Airtable and DAG in a system, provide a brief verdict on which approach is better and why. Consider the use case of a startup building an MVP.",
    "maxTokens": 200
  }')

if [ "$?" -eq 0 ]; then
  VERDICT=$(echo """$VERDICT_RESPONSE""" | jq -r '.text // "Unable to generate verdict"')
  echo -e "${GREEN}AI Verdict:${NC}"
  echo """"$VERDICT""""
else
  echo -e "${GREEN}Verdict:${NC}"
  echo "For startups and MVPs, Airtable + DAG provides faster time-to-market. For scale-ups expecting rapid growth, traditional databases with flexible orchestration offer better long-term sustainability."
fi

echo ""
echo "✅ Demo complete!"

# Additional curl commands reference
echo ""
echo -e "${BLUE}Additional useful curl commands:${NC}"
echo "================================"
echo ""
echo "# List all debates for the organization:"
echo "curl '"""$DEBATE_URL"""""$API_V1""/debates?organizationId="""$ORG_ID"""'"
echo ""
echo "# Get specific turn details:"
echo "curl '"""$DEBATE_URL"""""$API_V1""/debates/"""$DEBATE_ID"""/turns/{turnId}'"
echo ""
echo "# Submit a vote (if implemented):"
echo "curl -X POST '"""$DEBATE_URL"""/api/debates/"""$DEBATE_ID"""/vote' \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"participantId\": \""""$PARTICIPANT_1_ID"""\", \"userId\": \"user-123\"}'"
echo ""
echo "# Export debate transcript:"
echo "curl '"""$DEBATE_URL"""""$API_V1""/debates/"""$DEBATE_ID"""/export?format=markdown'"