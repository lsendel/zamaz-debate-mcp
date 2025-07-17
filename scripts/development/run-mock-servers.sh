#!/bin/bash

# Kill any existing mock servers
pkill -f "python3 mock-api-responses.py" 2>/dev/null

# Start mock servers on different ports
echo "Starting mock servers..."

# Organization service on 5005
python3 mock-api-responses.py 5005 > mock-org.log 2>&1 &
echo "Organization service started on port 5005 (PID: $!)"

# Context service on 5007
python3 mock-api-responses.py 5007 > mock-context.log 2>&1 &
echo "Context service started on port 5007 (PID: $!)"

# Controller service on 5013
python3 mock-api-responses.py 5013 > mock-controller.log 2>&1 &
echo "Controller service started on port 5013 (PID: $!)"

echo ""
echo "All mock services started. Press Ctrl+C to stop."
echo "Logs available in: mock-org.log, mock-context.log, mock-controller.log"

# Wait for interrupt
trap 'pkill -f "python3 mock-api-responses.py"; echo "Services stopped."; exit' INT
while true; do sleep 1; done