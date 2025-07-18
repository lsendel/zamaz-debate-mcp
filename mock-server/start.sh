#!/bin/bash

# Stop any existing services
../scripts/stop-services-docker.sh 2>/dev/null

# Start mock servers for all services
echo "Starting mock servers..."

# Organization API on port 5005
PORT=5005 node server.js > organization.log 2>&1 &
echo "Organization API mock started on port 5005"

# LLM API on port 5002
PORT=5002 node server.js > llm.log 2>&1 &
echo "LLM API mock started on port 5002"

# Debate Controller on port 5013
PORT=5013 node server.js > controller.log 2>&1 &
echo "Debate Controller mock started on port 5013"

echo ""
echo "All mock services started!"
echo "Logs: organization.log, llm.log, controller.log"
echo ""
echo "To stop: pkill -f 'node server.js'"