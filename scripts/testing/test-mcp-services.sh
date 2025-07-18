#!/bin/bash

# Test script for MCP services
# This script tests basic functionality of each MCP service

set -e

echo "===================="
echo "MCP Services Test"
echo "===================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to test HTTP endpoint
test_endpoint() {
    local name=$1
    local url=$2
    local expected_status=${3:-200}
    
    echo -n "Testing ""$name""... "
    
    status=$(curl -s -o /dev/null -w "%{http_code}" """$url""" 2>/dev/null || echo "000")
    
    if [ """$status""" = """$expected_status""" ]; then
        echo -e "${GREEN}✓ OK${NC} (HTTP ""$status"")"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC} (HTTP ""$status"", expected ""$expected_status"")"
        return 1
    fi
}

# Function to test MCP endpoint
test_mcp_endpoint() {
    local name=$1
    local url=$2
    
    echo -n "Testing MCP ""$name""... "
    
    response=$(curl -s """$url""" 2>/dev/null || echo "{}")
    
    if echo """$response""" | grep -q '"name"'; then
        echo -e "${GREEN}✓ OK${NC}"
        echo "  Response: $(echo """$response""" | head -1)"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        echo "  Response: ""$response"""
        return 1
    fi
}

# Check if services are configured
echo "Checking service configuration..."
echo ""

# Test MCP Organization Service
echo -e "${YELLOW}=== MCP Organization Service ===${NC}"
if lsof -i :8081 > /dev/null 2>&1; then
    test_endpoint "Health Check" "http://localhost:8081/health"
    test_endpoint "API Docs" "http://localhost:8081/v3/api-docs"
    test_mcp_endpoint "MCP Info" "http://localhost:8081/mcp"
else
    echo -e "${RED}Service not running on port 8081${NC}"
fi
echo ""

# Test MCP LLM Service
echo -e "${YELLOW}=== MCP LLM Service ===${NC}"
if lsof -i :8082 > /dev/null 2>&1; then
    test_endpoint "Health Check" "http://localhost:8082/health"
    test_endpoint "Providers" "http://localhost:8082/api/v1/completions/providers"
    test_mcp_endpoint "MCP Info" "http://localhost:8082/mcp"
else
    echo -e "${RED}Service not running on port 8082${NC}"
fi
echo ""

# Test MCP Controller Service
echo -e "${YELLOW}=== MCP Controller Service ===${NC}"
if lsof -i :8083 > /dev/null 2>&1; then
    test_endpoint "Health Check" "http://localhost:8083/health"
    test_mcp_endpoint "MCP Info" "http://localhost:8083/mcp"
else
    echo -e "${RED}Service not running on port 8083${NC}"
fi
echo ""

# Test MCP Modulith
echo -e "${YELLOW}=== MCP Modulith ===${NC}"
if lsof -i :8080 > /dev/null 2>&1; then
    test_endpoint "Health Check" "http://localhost:8080/actuator/health"
    test_endpoint "Organizations API" "http://localhost:8080/api/organizations"
else
    echo -e "${RED}Service not running on port 8080${NC}"
fi
echo ""

# Summary
echo -e "${YELLOW}===================="
echo "Test Summary"
echo "===================="
echo -e "${NC}"
echo "To run services:"
echo "1. Start each service: cd <service-dir> && mvn spring-boot:run"
echo "2. Or use docker-compose if configured"
echo ""
echo "Example commands:"
echo "  cd mcp-organization && mvn spring-boot:run"
echo "  cd mcp-llm && mvn spring-boot:run"
echo "  cd mcp-modulith && mvn spring-boot:run"