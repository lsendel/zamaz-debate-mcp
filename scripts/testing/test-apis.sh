#!/bin/bash

# API Testing Script for MCP Services

echo "=== MCP API Testing Script ==="
echo ""

# Base URLs
ORG_URL="http://localhost:5005"
CONTEXT_URL="http://localhost:5007"
CONTROLLER_URL="http://localhost:5013"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print test header
print_test() {
    echo -e "\n${BLUE}Testing: $1${NC}"
    echo "----------------------------------------"
}

# Function to check result
check_result() {
    if [ "$1" -eq 0 ]; then
        echo -e "${GREEN}✓ Success${NC}"
    else
        echo -e "${RED}✗ Failed${NC}"
    fi
}

# Test Organization Service
echo -e "\n${BLUE}=== ORGANIZATION SERVICE TESTS ===${NC}"

print_test "Health Check"
curl -s -X GET """$ORG_URL""/actuator/health" | jq '.' || echo "Service not available"

print_test "Create Organization"
ORG_RESPONSE=$(curl -s -X POST """$ORG_URL""/mcp/tools/create_organization" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Organization",
    "slug": "test-org",
    "description": "Test organization for API testing",
    "settings": {
      "allowedModels": ["gpt-4", "claude-3"],
      "maxUsers": 100
    }
  }')
echo """$ORG_RESPONSE""" | jq '.' || echo """$ORG_RESPONSE"""
ORG_ID=$(echo """$ORG_RESPONSE""" | jq -r '.content[0].organizationId // empty')

print_test "List Organizations"
curl -s -X GET """$ORG_URL""/api/organizations" | jq '.' || echo "Failed to list organizations"

print_test "Get Organization by ID"
if [ ! -z """$ORG_ID""" ]; then
    curl -s -X GET """$ORG_URL""/api/organizations/""$ORG_ID""" | jq '.' || echo "Failed to get organization"
else
    echo "No organization ID available"
fi

# Test Context Service
echo -e "\n${BLUE}=== CONTEXT SERVICE TESTS ===${NC}"

print_test "Health Check"
curl -s -X GET """$CONTEXT_URL""/actuator/health" | jq '.' || echo "Service not available"

print_test "Create Context"
CONTEXT_RESPONSE=$(curl -s -X POST """$CONTEXT_URL""/api/contexts" \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: ${ORG_ID:-test-org}" \
  -d '{
    "name": "Test Debate Context",
    "type": "DEBATE",
    "metadata": {
      "topic": "AI Ethics",
      "format": "OXFORD"
    }
  }')
echo """$CONTEXT_RESPONSE""" | jq '.' || echo """$CONTEXT_RESPONSE"""
CONTEXT_ID=$(echo """$CONTEXT_RESPONSE""" | jq -r '.id // empty')

print_test "Add Message to Context"
if [ ! -z """$CONTEXT_ID""" ]; then
    curl -s -X POST """$CONTEXT_URL""/api/contexts/""$CONTEXT_ID""/messages" \
      -H "Content-Type: application/json" \
      -H "X-Organization-Id: ${ORG_ID:-test-org}" \
      -d '{
        "role": "user",
        "content": "Should AI systems be required to explain their decisions?",
        "metadata": {
          "participant": "moderator"
        }
      }' | jq '.' || echo "Failed to add message"
else
    echo "No context ID available"
fi

print_test "Get Context Window"
if [ ! -z """$CONTEXT_ID""" ]; then
    curl -s -X GET """$CONTEXT_URL""/api/contexts/""$CONTEXT_ID""/window?maxTokens=1000" \
      -H "X-Organization-Id: ${ORG_ID:-test-org}" | jq '.' || echo "Failed to get context window"
else
    echo "No context ID available"
fi

# Test Controller Service
echo -e "\n${BLUE}=== CONTROLLER SERVICE TESTS ===${NC}"

print_test "Health Check"
curl -s -X GET """$CONTROLLER_URL""/actuator/health" | jq '.' || echo "Service not available"

print_test "Create Debate"
DEBATE_RESPONSE=$(curl -s -X POST """$CONTROLLER_URL""/api/debates" \
  -H "Content-Type: application/json" \
  -H "X-Organization-Id: ${ORG_ID:-test-org}" \
  -d '{
    "organizationId": "'${ORG_ID:-test-org}'",
    "title": "AI Ethics Debate",
    "description": "A debate on the ethical implications of AI",
    "topic": "Should AI systems be required to explain their decisions?",
    "format": "OXFORD",
    "maxRounds": 3,
    "settings": {
      "timePerRound": 300,
      "maxResponseLength": 500
    }
  }')
echo """$DEBATE_RESPONSE""" | jq '.' || echo """$DEBATE_RESPONSE"""
DEBATE_ID=$(echo """$DEBATE_RESPONSE""" | jq -r '.id // empty')

print_test "Add Participant to Debate"
if [ ! -z """$DEBATE_ID""" ]; then
    PARTICIPANT_RESPONSE=$(curl -s -X POST """$CONTROLLER_URL""/api/debates/""$DEBATE_ID""/participants" \
      -H "Content-Type: application/json" \
      -H "X-Organization-Id: ${ORG_ID:-test-org}" \
      -d '{
        "name": "AI Advocate",
        "type": "ai",
        "provider": "openai",
        "model": "gpt-4",
        "position": "for"
      }')
    echo """$PARTICIPANT_RESPONSE""" | jq '.' || echo """$PARTICIPANT_RESPONSE"""
    
    # Add second participant
    curl -s -X POST """$CONTROLLER_URL""/api/debates/""$DEBATE_ID""/participants" \
      -H "Content-Type: application/json" \
      -H "X-Organization-Id: ${ORG_ID:-test-org}" \
      -d '{
        "name": "AI Skeptic",
        "type": "ai",
        "provider": "anthropic",
        "model": "claude-3",
        "position": "against"
      }' | jq '.' || echo "Failed to add second participant"
else
    echo "No debate ID available"
fi

print_test "Start Debate"
if [ ! -z """$DEBATE_ID""" ]; then
    curl -s -X POST """$CONTROLLER_URL""/api/debates/""$DEBATE_ID""/start" \
      -H "X-Organization-Id: ${ORG_ID:-test-org}" | jq '.' || echo "Failed to start debate"
else
    echo "No debate ID available"
fi

print_test "List Debates"
curl -s -X GET """$CONTROLLER_URL""/api/debates?organizationId=${ORG_ID:-test-org}" | jq '.' || echo "Failed to list debates"

# Test MCP Protocol endpoints
echo -e "\n${BLUE}=== MCP PROTOCOL TESTS ===${NC}"

print_test "List Resources (Organization)"
curl -s -X GET """$ORG_URL""/mcp/resources" | jq '.' || echo "Failed to list resources"

print_test "List Tools (Organization)"
curl -s -X GET """$ORG_URL""/mcp/tools" | jq '.' || echo "Failed to list tools"

print_test "Get Prompts (Controller)"
curl -s -X GET """$CONTROLLER_URL""/mcp/prompts" | jq '.' || echo "Failed to get prompts"

# Summary
echo -e "\n${BLUE}=== TEST SUMMARY ===${NC}"
echo "Organization ID: ${ORG_ID:-Not created}"
echo "Context ID: ${CONTEXT_ID:-Not created}"
echo "Debate ID: ${DEBATE_ID:-Not created}"
echo ""
echo "Test completed. Check the output above for any failures."