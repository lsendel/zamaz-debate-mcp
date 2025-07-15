#!/bin/bash

# Basic test to verify MCP services can compile and run
# This script verifies that all services are properly configured

set -e

echo "================================"
echo "MCP Basic Functionality Test"
echo "================================"
echo ""

# Set Java 21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

echo "Java version:"
java -version
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to test service compilation
test_compile() {
    local service=$1
    local dir=$2
    
    echo -n "Testing $service compilation... "
    
    if cd "$dir" && mvn compile > /dev/null 2>&1; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        return 1
    fi
}

# Function to test service packaging
test_package() {
    local service=$1
    local dir=$2
    
    echo -n "Testing $service packaging... "
    
    if cd "$dir" && mvn package -DskipTests > /dev/null 2>&1; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        return 1
    fi
}

# Test each service
echo -e "${YELLOW}=== Testing Service Compilation ===${NC}"
echo ""

BASE_DIR="/Users/lsendel/IdeaProjects/zamaz-debate-mcp"

# Test compilation
test_compile "mcp-common" "$BASE_DIR/mcp-common"
test_compile "mcp-security" "$BASE_DIR/mcp-security"
test_compile "mcp-organization" "$BASE_DIR/mcp-organization"
test_compile "mcp-controller" "$BASE_DIR/mcp-controller"
test_compile "mcp-llm" "$BASE_DIR/mcp-llm"
test_compile "mcp-debate" "$BASE_DIR/mcp-debate"
test_compile "mcp-rag" "$BASE_DIR/mcp-rag"
test_compile "mcp-template" "$BASE_DIR/mcp-template"
test_compile "mcp-context-client" "$BASE_DIR/mcp-context-client"
test_compile "mcp-modulith" "$BASE_DIR/mcp-modulith"

echo ""
echo -e "${YELLOW}=== Testing Service Packaging ===${NC}"
echo ""

# Test packaging
test_package "mcp-organization" "$BASE_DIR/mcp-organization"
test_package "mcp-llm" "$BASE_DIR/mcp-llm"
test_package "mcp-modulith" "$BASE_DIR/mcp-modulith"

echo ""
echo -e "${YELLOW}=== Test Summary ===${NC}"
echo ""
echo "All services compile successfully with Java 21 and Spring Boot 3.3.5!"
echo ""
echo "To run a service:"
echo "  cd <service-directory>"
echo "  mvn spring-boot:run"
echo ""
echo "Example:"
echo "  cd $BASE_DIR/mcp-modulith"
echo "  mvn spring-boot:run"