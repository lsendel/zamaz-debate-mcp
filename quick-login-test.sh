#!/bin/bash

echo "🧪 Testing login with demo credentials..."
echo ""

# Test login endpoint
echo "1. Testing login API directly..."
curl -X POST http://localhost:3001/api/organization/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}' \
  -v 2>&1 | grep -E "< HTTP|error|Error|500|401|200"

echo ""
echo "2. Testing if UI is accessible..."
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:3001

echo ""
echo "📋 Login Instructions:"
echo "   URL: http://localhost:3001"
echo "   Username: demo"
echo "   Password: demo123"
echo ""
echo "If you're getting a 500 error, the mock authentication might not be loaded."
echo "Let me check the server logs..."
echo ""
tail -20 /tmp/vite.log 2>/dev/null || echo "No log file found"