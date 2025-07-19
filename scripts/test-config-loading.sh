#!/bin/bash

# Script to test configuration loading from Config Server
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Config Server URL
CONFIG_SERVER_URL="${CONFIG_SERVER_URL:-http://localhost:8888}"

# Services to test
SERVICES=(
    "mcp-gateway"
    "mcp-auth-server"
    "mcp-sidecar"
    "mcp-organization"
    "mcp-llm"
    "mcp-rag"
    "mcp-debate-engine"
    "mcp-controller"
    "mcp-context"
    "mcp-pattern-recognition"
    "github-integration"
    "mcp-modulith"
    "mcp-template"
    "mcp-docs"
    "mcp-context-client"
    "mcp-debate"
)

# Profiles to test
PROFILES=("default" "development" "staging" "production")

# Function to check Config Server health
check_config_server() {
    echo -e "${GREEN}Checking Config Server health...${NC}"
    
    if curl -s -f "$CONFIG_SERVER_URL/actuator/health" > /dev/null; then
        echo -e "${GREEN}✓ Config Server is healthy${NC}"
        
        # Get detailed health info
        local health=$(curl -s "$CONFIG_SERVER_URL/actuator/health" | jq -r '.status')
        echo "  Status: $health"
        
        return 0
    else
        echo -e "${RED}✗ Config Server is not accessible${NC}"
        return 1
    fi
}

# Function to test configuration loading
test_config() {
    local service=$1
    local profile=$2
    local url="$CONFIG_SERVER_URL/$service/$profile"
    
    # Make request
    local response=$(curl -s -w "\n%{http_code}" "$url")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ]; then
        # Parse response
        local name=$(echo "$body" | jq -r '.name' 2>/dev/null || echo "")
        local profiles=$(echo "$body" | jq -r '.profiles[]' 2>/dev/null || echo "")
        local sources=$(echo "$body" | jq -r '.propertySources[].name' 2>/dev/null || echo "")
        
        if [ "$name" = "$service" ]; then
            echo -e "${GREEN}✓ $service/$profile - OK${NC}"
            
            # Show loaded property sources
            if [ -n "$sources" ]; then
                echo "  Property sources:"
                echo "$sources" | while read -r source; do
                    echo "    - $source"
                done
            fi
            
            return 0
        else
            echo -e "${YELLOW}⚠ $service/$profile - Unexpected response${NC}"
            return 1
        fi
    else
        echo -e "${RED}✗ $service/$profile - HTTP $http_code${NC}"
        return 1
    fi
}

# Function to test specific property
test_property() {
    local service=$1
    local profile=$2
    local property=$3
    local url="$CONFIG_SERVER_URL/$service/$profile"
    
    local value=$(curl -s "$url" | jq -r ".propertySources[].source.\"$property\"" 2>/dev/null | grep -v null | head -n1)
    
    if [ -n "$value" ]; then
        echo "    $property: $value"
        return 0
    else
        echo "    $property: <not found>"
        return 1
    fi
}

# Function to validate service configuration
validate_service_config() {
    local service=$1
    
    echo -e "${GREEN}Validating $service configuration...${NC}"
    
    # Test default profile
    if test_config "$service" "default"; then
        # Check common properties
        echo "  Checking common properties:"
        test_property "$service" "default" "server.port"
        test_property "$service" "default" "spring.application.name"
        test_property "$service" "default" "management.endpoints.web.exposure.include"
    fi
    
    # Test development profile
    if test_config "$service" "development"; then
        # Check development-specific properties
        echo "  Development profile loaded successfully"
    fi
    
    echo ""
}

# Function to generate test report
generate_report() {
    local report_file="$PROJECT_ROOT/config-test-report.txt"
    
    echo "Configuration Test Report" > "$report_file"
    echo "========================" >> "$report_file"
    echo "Date: $(date)" >> "$report_file"
    echo "Config Server: $CONFIG_SERVER_URL" >> "$report_file"
    echo "" >> "$report_file"
    
    echo "Service Configuration Status:" >> "$report_file"
    echo "----------------------------" >> "$report_file"
    
    local total=0
    local success=0
    
    for service in "${SERVICES[@]}"; do
        ((total++))
        
        # Test if service config exists
        if curl -s "$CONFIG_SERVER_URL/$service/default" | jq -e '.name' > /dev/null 2>&1; then
            echo "✓ $service" >> "$report_file"
            ((success++))
        else
            echo "✗ $service" >> "$report_file"
        fi
    done
    
    echo "" >> "$report_file"
    echo "Summary: $success/$total services configured" >> "$report_file"
    
    echo -e "${GREEN}Report saved to: $report_file${NC}"
}

# Main script
echo -e "${GREEN}Testing Configuration Loading from Config Server${NC}"
echo "================================================"
echo ""

# Check Config Server
if ! check_config_server; then
    echo -e "${RED}Config Server is not running!${NC}"
    echo "Please start it with:"
    echo "  cd infrastructure/docker-compose"
    echo "  docker-compose up -d mcp-config-server"
    exit 1
fi

echo ""

# Test each service
echo -e "${GREEN}Testing service configurations...${NC}"
echo ""

for service in "${SERVICES[@]}"; do
    validate_service_config "$service"
done

# Generate report
generate_report

echo ""
echo -e "${GREEN}Configuration testing complete!${NC}"
echo ""
echo "To view a specific configuration:"
echo "  curl $CONFIG_SERVER_URL/<service>/<profile>"
echo ""
echo "To decrypt an encrypted value:"
echo "  curl -X POST $CONFIG_SERVER_URL/decrypt -d '{cipher}...'"