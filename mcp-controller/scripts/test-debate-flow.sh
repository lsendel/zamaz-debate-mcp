#!/bin/bash

echo "🎭 DEBATE SYSTEM TEST"
echo "===================="
echo ""

# Use a valid UUID for organization
ORG_ID="123e4567-e89b-12d3-a456-426614174000"

# Step 1: Create a debate with valid UUID
echo "1. Creating debate..."
echo "-------------------"
DEBATE_JSON='{
  "topic": "Should I use Airtable and DAG in the system?",
  "description": "Evaluating Airtable as database and DAG for workflows",
  "format": "OXFORD",
  "organizationId": "'""$ORG_ID""'",
  "maxRounds": 3,
  "maxResponseLength": 500
}'

echo "Request:"
echo """$DEBATE_JSON""" | jq .
echo ""

echo "Response:"
curl -X POST http://localhost:5013/api/v1/debates \
  -H "Content-Type: application/json" \
  -d """$DEBATE_JSON"""

echo ""
echo ""

# Step 2: List debates (if endpoint exists)
echo "2. Listing debates..."
echo "-------------------"
curl http://localhost:5013/api/v1/debates?organizationId=$ORG_ID

echo ""
echo ""

# Step 3: Check health
echo "3. Service Health..."
echo "------------------"
curl http://localhost:5013/actuator/health | jq .

echo ""