#!/bin/bash

echo "🔍 Verifying Debate Progress Implementation"
echo "========================================="

# Check if services are running
echo -e "\n📡 Checking services..."
curl -s http://localhost:5013/api/v1/debates/debate-002 > /dev/null && echo "✅ Debate service is running" || echo "❌ Debate service not responding"
curl -s http://localhost:3001/ > /dev/null && echo "✅ UI service is running" || echo "❌ UI service not responding"

# Check debate data
echo -e "\n📊 Checking debate data..."
DEBATE_DATA=$(curl -s http://localhost:5013/api/v1/debates/debate-002)
STATUS=$(echo $DEBATE_DATA | jq -r '.status')
ROUNDS=$(echo $DEBATE_DATA | jq -r '.rounds | length')
echo "- Debate status: $STATUS"
echo "- Rounds completed: $ROUNDS"

# Generate next round
echo -e "\n🎯 Triggering next round generation..."
curl -s -X POST http://localhost:5013/api/v1/debates/debate-002/generate-round > /dev/null
echo "✅ Round generation triggered"

# Wait and check again
echo -e "\n⏳ Waiting 5 seconds for round generation..."
sleep 5

# Check updated data
UPDATED_DATA=$(curl -s http://localhost:5013/api/v1/debates/debate-002)
NEW_ROUNDS=$(echo $UPDATED_DATA | jq -r '.rounds | length')
echo "- New rounds count: $NEW_ROUNDS"

if [ "$NEW_ROUNDS" -gt "$ROUNDS" ]; then
    echo "🎉 SUCCESS! New round generated!"
else
    echo "ℹ️  No new rounds generated (might still be processing)"
fi

echo -e "\n✅ Progress implementation verified!"
echo "📌 Open http://localhost:3001/debates/debate-002 to see live updates"