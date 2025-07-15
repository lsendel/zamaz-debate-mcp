#!/bin/bash

echo "Simple CURL Example: What color is the sky?"
echo "==========================================="
echo ""
echo "Command:"
echo "--------"
cat << 'EOF'
curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "messages": [
      {
        "role": "user",
        "content": "What color is the sky?"
      }
    ],
    "maxTokens": 100,
    "temperature": 0.7
  }'
EOF

echo ""
echo "Executing..."
echo "-----------"

curl -X POST http://localhost:5002/api/v1/completions \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "claude",
    "messages": [
      {
        "role": "user",
        "content": "What color is the sky?"
      }
    ],
    "maxTokens": 100,
    "temperature": 0.7
  }'

echo ""