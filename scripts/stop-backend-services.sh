#!/bin/bash

# Script to stop backend services

PID_FILE="/tmp/mcp-services.pids"

if [ ! -f """$PID_FILE""" ]; then
    echo "No services found to stop (PID file not found)"
    exit 0
fi

echo "Stopping backend services..."

while read -r pid; do
    if [ ! -z """$pid""" ] && kill -0 """$pid""" 2>/dev/null; then
        echo "Stopping process ""$pid""..."
        kill """$pid"""
    fi
done < """$PID_FILE"""

# Give processes time to shutdown gracefully
sleep 2

# Force kill any remaining processes
while read -r pid; do
    if [ ! -z """$pid""" ] && kill -0 """$pid""" 2>/dev/null; then
        echo "Force stopping process ""$pid""..."
        kill -9 """$pid"""
    fi
done < """$PID_FILE"""

rm -f """$PID_FILE"""

echo "All services stopped."