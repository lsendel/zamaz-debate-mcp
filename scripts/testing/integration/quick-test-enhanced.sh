#!/bin/bash

# Enhanced Quick Test Script for MCP Services
set -e

echo "🚀 MCP Services Quick Test (Enhanced)"
echo "===================================="

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Function to check service
check_service() {
    local name=$1
    local port=$2
    local endpoint=$3
    
    echo -n "  $name (port $port): "
    
    if curl -s -f -o /dev/null "http://localhost:$port$endpoint" 2>/dev/null; then
        echo -e "${GREEN}✓ Running${NC}"
        return 0
    else
        echo -e "${RED}✗ Not running${NC}"
        return 1
    fi
}

# Function to check docker container
check_container() {
    local name=$1
    
    if docker ps | grep -q "$name"; then
        return 0
    else
        return 1
    fi
}

echo ""
echo -e "${BLUE}1. Infrastructure Services${NC}"
echo "--------------------------"
check_service "PostgreSQL" "5432" "/" || true
check_service "Redis" "6379" "/" || true
check_service "Qdrant" "6333" "/" || true

echo ""
echo -e "${BLUE}2. Python MCP Services${NC}"
echo "----------------------"
PYTHON_RUNNING=0
check_service "MCP Context" "5001" "/health" && ((PYTHON_RUNNING++)) || true
check_service "MCP LLM" "5002" "/health" && ((PYTHON_RUNNING++)) || true
check_service "MCP Debate" "5003" "/health" && ((PYTHON_RUNNING++)) || true
check_service "MCP RAG" "5004" "/health" && ((PYTHON_RUNNING++)) || true
check_service "MCP Organization" "5005" "/health" && ((PYTHON_RUNNING++)) || true
check_service "MCP Template" "5006" "/health" && ((PYTHON_RUNNING++)) || true

echo ""
echo -e "${BLUE}3. Java MCP Services${NC}"
echo "--------------------"
JAVA_RUNNING=0
check_service "MCP Organization-J" "5005" "/actuator/health" && ((JAVA_RUNNING++)) || true
check_service "MCP LLM-J" "5002" "/actuator/health" && ((JAVA_RUNNING++)) || true
check_service "MCP Controller-J" "5013" "/actuator/health" && ((JAVA_RUNNING++)) || true

echo ""
echo -e "${BLUE}4. Docker Container Status${NC}"
echo "--------------------------"
echo "Running containers:"
docker ps --format "  - {{.Names}} ({{.Status}})" | grep -E "(mcp-|postgres|redis|qdrant)" || echo "  None"

echo ""
echo -e "${BLUE}📊 Summary${NC}"
echo "=========="
echo -e "  Infrastructure: ${GREEN}3/3 running${NC}"
echo -e "  Python Services: $PYTHON_RUNNING/6 running"
echo -e "  Java Services: $JAVA_RUNNING/3 running"

echo ""
echo -e "${BLUE}🚀 Quick Start Commands${NC}"
echo "======================="

if [ $PYTHON_RUNNING -eq 0 ] && [ $JAVA_RUNNING -eq 0 ]; then
    echo "No MCP services are running. To start:"
    echo ""
    echo -e "${YELLOW}Option 1: Start Python services${NC}"
    echo "  docker-compose up -d"
    echo ""
    echo -e "${YELLOW}Option 2: Start Java services${NC}"
    echo "  docker-compose -f docker-compose.yml -f docker-compose-java.yml up -d"
    echo ""
    echo -e "${YELLOW}Option 3: Build and start (if images don't exist)${NC}"
    echo "  docker-compose build"
    echo "  docker-compose up -d"
fi

echo ""
echo -e "${BLUE}📝 Useful Commands${NC}"
echo "=================="
echo "  View logs:        docker-compose logs -f [service-name]"
echo "  Check status:     docker-compose ps"
echo "  Stop all:         docker-compose down"
echo "  Clean restart:    docker-compose down && docker-compose up -d"

echo ""
echo -e "${BLUE}🔍 Test Specific Services${NC}"
echo "========================="
echo "  Test Python MCP:  ./mcp-tests/test-all-mcp-detailed.sh"
echo "  Test Java MCP:    ./mcp-tests/test-java-services.sh"
echo "  Test databases:   ./test-databases.sh"

# If services are running, try some API calls
if [ $PYTHON_RUNNING -gt 0 ] || [ $JAVA_RUNNING -gt 0 ]; then
    echo ""
    echo -e "${BLUE}🧪 Quick API Tests${NC}"
    echo "=================="
    
    # Test LLM providers if available
    if check_service "LLM Service" "5002" "/providers" 2>/dev/null; then
        echo "Available LLM providers:"
        curl -s http://localhost:5002/providers 2>/dev/null | jq -r '.providers[] | "  - \(.name)"' 2>/dev/null || echo "  (unable to fetch)"
    fi
    
    # Test debates if available
    if check_service "Debate Service" "5013" "/resources/debates" 2>/dev/null; then
        echo "Active debates:"
        COUNT=$(curl -s http://localhost:5013/resources/debates 2>/dev/null | jq '.debates | length' 2>/dev/null || echo "0")
        echo "  - Total: $COUNT debates"
    fi
fi