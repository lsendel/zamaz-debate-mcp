#!/bin/bash

echo "Testing Redis connectivity from containers..."

# Test from each container
for service in mcp-organization mcp-llm mcp-controller; do
    echo ""
    echo "=== Testing from """$service""" ==="
    container_name="zamaz-debate-mcp-${service}-1"
    
    # Check if container exists and is running
    if docker ps --format "table {{.Names}}" | grep -q """"$container_name""""; then
        # Test Redis connectivity
        echo "Testing Redis connection..."
        docker exec """$container_name""" sh -c "nc -zv redis 6379" 2>&1
        
        # Check environment variables
        echo "Redis environment variables:"
        docker exec """$container_name""" printenv | grep REDIS
    else
        echo "Container """$container_name""" is not running"
    fi
done

echo ""
echo "=== Direct Redis test ==="
docker exec zamaz-debate-mcp-redis-1 redis-cli ping