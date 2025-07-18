#!/bin/bash

# Script to stop Docker MCP services

echo "Stopping MCP Docker services..."

# Stop and remove containers
for service in mcp-organization mcp-llm mcp-controller; do
    if docker ps -a | grep -q $service; then
        echo "Stopping $service..."
        docker stop $service
        docker rm $service
    fi
done

echo "All services stopped."