#!/bin/bash

echo "🎭 COMPLETE DEBATE DEMO: Airtable vs Traditional DB"
echo "=================================================="
echo ""

# Configuration
DEBATE_URL="http://localhost:5013/api/v1"
ORG_ID="123e4567-e89b-12d3-a456-426614174000"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Step 1: Create Debate
echo -e "${BLUE}Step 1: Creating debate...${NC}"
DEBATE_RESPONSE=$(curl -s -X POST """$DEBATE_URL"""/debates \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "'"""$ORG_ID"""'",
    "title": "Airtable vs Traditional Database",
    "topic": "Should I use Airtable and DAG in the system?",
    "description": "Evaluating the pros and cons of using Airtable as a database and DAG for workflow orchestration",
    "format": "OXFORD",
    "maxRounds": 3
  }')

DEBATE_ID=$(echo """$DEBATE_RESPONSE""" | jq -r '.id // empty')
if [ -z """"$DEBATE_ID"""" ]; then
  echo -e "${RED}Failed to create debate. Response:${NC}"
  echo """$DEBATE_RESPONSE""" | jq .
  exit 1
fi

echo -e "${GREEN}✅ Created debate: """$DEBATE_ID"""${NC}"
echo """$DEBATE_RESPONSE""" | jq '{id, title, topic, status, currentRound, maxRounds}'
echo ""

# Step 2: Add Participants
echo -e "${BLUE}Step 2: Adding participants...${NC}"

# Add Pro-Airtable participant
PARTICIPANT1_RESPONSE=$(curl -s -X POST """$DEBATE_URL"""/debates/"""$DEBATE_ID"""/participants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pro-Airtable Advocate",
    "type": "AI",
    "metadata": {
      "position": "for",
      "llmProvider": "claude"
    }
  }')

PARTICIPANT1_ID=$(echo """$PARTICIPANT1_RESPONSE""" | jq -r '.id // empty')
echo -e "${GREEN}✅ Added participant 1: """$PARTICIPANT1_ID"""${NC}"

# Add Anti-Airtable participant
PARTICIPANT2_RESPONSE=$(curl -s -X POST """$DEBATE_URL"""/debates/"""$DEBATE_ID"""/participants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Traditional DB Advocate",
    "type": "AI",
    "metadata": {
      "position": "against",
      "llmProvider": "openai"
    }
  }')

PARTICIPANT2_ID=$(echo """$PARTICIPANT2_RESPONSE""" | jq -r '.id // empty')
echo -e "${GREEN}✅ Added participant 2: """$PARTICIPANT2_ID"""${NC}"
echo ""

# Step 3: Submit Turns
echo -e "${BLUE}Step 3: Running debate rounds...${NC}"
echo ""

# Round 1 - Opening Statements
echo -e "${YELLOW}=== ROUND 1: OPENING STATEMENTS ===${NC}"

echo "Pro-Airtable Opening:"
curl -s -X POST """$DEBATE_URL"""/debates/"""$DEBATE_ID"""/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "'"""$PARTICIPANT1_ID"""'",
    "content": "Airtable and DAGs are excellent choices for modern systems. Airtable provides a flexible, no-code database with built-in UI, perfect for rapid prototyping and business user access. DAGs ensure clear workflow orchestration with dependency management, making complex processes maintainable and scalable.",
    "round": 1
  }' | jq .

echo ""
echo "Traditional DB Opening:"
curl -s -X POST """$DEBATE_URL"""/debates/"""$DEBATE_ID"""/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "'"""$PARTICIPANT2_ID"""'",
    "content": "While Airtable and DAGs seem appealing, they introduce significant limitations. Airtable lacks the performance and query capabilities of proper databases, with API rate limits and vendor lock-in risks. DAGs can overcomplicate simple workflows. Traditional databases with flexible orchestration provide better long-term value.",
    "round": 1
  }' | jq .

echo ""

# Round 2 - Rebuttals
echo -e "${YELLOW}=== ROUND 2: REBUTTALS ===${NC}"

echo "Pro-Airtable Rebuttal:"
curl -s -X POST """$DEBATE_URL"""/debates/"""$DEBATE_ID"""/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "'"""$PARTICIPANT1_ID"""'",
    "content": "The concerns about limitations are overstated. Airtable'\''s API limits are generous for most use cases, and its visual interface reduces development time by 70%. DAGs provide clarity, not complexity. The rapid iteration and reduced maintenance costs far outweigh theoretical performance concerns.",
    "round": 2
  }' | jq .

echo ""
echo "Traditional DB Rebuttal:"
curl -s -X POST """$DEBATE_URL"""/debates/"""$DEBATE_ID"""/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "'"""$PARTICIPANT2_ID"""'",
    "content": "A 70% development time reduction means nothing when you hit Airtable'\''s 5 requests/second limit in production. Real systems need sub-millisecond queries, not 200ms API calls. PostgreSQL with Redis and simple async workers provide better performance, lower costs, and no vendor dependencies.",
    "round": 2
  }' | jq .

echo ""

# Round 3 - Closing Arguments
echo -e "${YELLOW}=== ROUND 3: CLOSING ARGUMENTS ===${NC}"

echo "Pro-Airtable Closing:"
curl -s -X POST """$DEBATE_URL"""/debates/"""$DEBATE_ID"""/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "'"""$PARTICIPANT1_ID"""'",
    "content": "Consider the total cost of ownership. Airtable eliminates database administration, provides instant collaboration features, and includes versioning, forms, and automations out-of-the-box. For startups and MVPs, shipping features quickly matters more than optimizing for scale you don'\''t have yet.",
    "round": 3
  }' | jq .

echo ""
echo "Traditional DB Closing:"
curl -s -X POST """$DEBATE_URL"""/debates/"""$DEBATE_ID"""/turns \
  -H "Content-Type: application/json" \
  -d '{
    "participantId": "'"""$PARTICIPANT2_ID"""'",
    "content": "Technical debt compounds quickly. Starting with Airtable means eventual migration pain when you need real database features. Build on proven foundations: PostgreSQL for data, Redis for caching, and simple service orchestration. Your future self will thank you.",
    "round": 3
  }' | jq .

echo ""

# Step 4: Get Complete Debate
echo -e "${BLUE}Step 4: Fetching complete debate...${NC}"
COMPLETE_DEBATE=$(curl -s """$DEBATE_URL"""/debates/"""$DEBATE_ID""")
echo """$COMPLETE_DEBATE""" | jq .

echo ""

# Step 5: Get All Turns
echo -e "${BLUE}Step 5: Fetching all turns...${NC}"
TURNS=$(curl -s """$DEBATE_URL"""/debates/"""$DEBATE_ID"""/turns)
echo """$TURNS""" | jq '.[] | {round, participant: .participant.name, content}'

echo ""
echo -e "${GREEN}✅ Debate Complete!${NC}"
echo ""
echo "Summary:"
echo "--------"
echo "Debate ID: """$DEBATE_ID""""
echo "Status: $(echo """$COMPLETE_DEBATE""" | jq -r .status)"
echo "Rounds: $(echo """$COMPLETE_DEBATE""" | jq -r .currentRound)/$(echo """$COMPLETE_DEBATE""" | jq -r .maxRounds)"
echo ""
echo "View full debate at: """$DEBATE_URL"""/debates/"""$DEBATE_ID""""