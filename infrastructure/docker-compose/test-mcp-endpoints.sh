#!/bin/bash

# MCP Endpoints Testing Script
# Tests all MCP endpoints across all services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
MCP_ORGANIZATION_PORT=${MCP_ORGANIZATION_PORT:-5005}
MCP_LLM_PORT=${MCP_LLM_PORT:-5002}
MCP_CONTROLLER_PORT=${MCP_CONTROLLER_PORT:-5013}
MCP_RAG_PORT=${MCP_RAG_PORT:-5018}

# Test data
ORG_ID="test-org-$(date +%s)"
DEBATE_ID=""

# Helper functions
success() {
    echo -e "${GREEN}✓ $1${NC}"
}

error() {
    echo -e "${RED}✗ $1${NC}"
}

info() {
    echo -e "${YELLOW}→ $1${NC}"
}

test_endpoint() {
    local service=$1
    local port=$2
    local endpoint=$3
    local method=$4
    local data=$5
    local description=$6
    
    echo ""
    info "Testing: ""$description"""
    info "Service: ""$service"" (port ""$port"")"
    info "Endpoint: ""$method"" ""$endpoint"""
    
    if [ """$method""" = "GET" ]; then
        response=$(curl -s -X GET \
            -H "Content-Type: application/json" \
            "http://localhost:""$port"""$endpoint"" 2>&1)
    else
        response=$(curl -s -X POST \
            -H "Content-Type: application/json" \
            -d """$data""" \
            "http://localhost:""$port"""$endpoint"" 2>&1)
    fi
    
    if [ "$?" -eq 0 ]; then
        success "Response received:"
        echo """$response""" | jq . 2>/dev/null || echo """$response"""
        return 0
    else
        error "Failed to call endpoint"
        echo """$response"""
        return 1
    fi
}

# Check service health
check_services() {
    info "Checking service health..."
    
    services=("mcp-organization:""$MCP_ORGANIZATION_PORT""" "mcp-llm:""$MCP_LLM_PORT""" "mcp-controller:""$MCP_CONTROLLER_PORT""")
    
    for service_port in "${services[@]}"; do
        IFS=':' read -r service port <<< """$service_port"""
        if curl -s "http://localhost:""$port""/actuator/health" > /dev/null 2>&1; then
            success """$service"" is healthy"
        else
            error """$service"" is not responding on port ""$port"""
            info "Trying basic health endpoint..."
            curl -s "http://localhost:""$port""/health" || true
        fi
    done
}

# Test MCP Organization endpoints
test_organization_endpoints() {
    echo ""
    echo "====================================="
    echo "Testing MCP Organization Endpoints"
    echo "====================================="
    
    # Test server info
    test_endpoint "mcp-organization" ""$MCP_ORGANIZATION_PORT"" "/mcp" "GET" "" \
        "Get MCP server info"
    
    # Test list tools
    test_endpoint "mcp-organization" ""$MCP_ORGANIZATION_PORT"" "/mcp/list-tools" "POST" "{}" \
        "List available MCP tools"
    
    # Test create organization tool
    test_endpoint "mcp-organization" ""$MCP_ORGANIZATION_PORT"" "/mcp/call-tool" "POST" '{
        "name": "create_organization",
        "arguments": {
            "name": "Test Organization",
            "description": "Test organization for MCP testing"
        }
    }' "Create organization via MCP tool"
    
    # Test list organizations tool
    test_endpoint "mcp-organization" ""$MCP_ORGANIZATION_PORT"" "/mcp/call-tool" "POST" '{
        "name": "list_organizations",
        "arguments": {}
    }' "List organizations via MCP tool"
}

# Test MCP LLM endpoints
test_llm_endpoints() {
    echo ""
    echo "====================================="
    echo "Testing MCP LLM Endpoints"
    echo "====================================="
    
    # Test server info
    test_endpoint "mcp-llm" ""$MCP_LLM_PORT"" "/mcp" "GET" "" \
        "Get MCP server info"
    
    # Test list tools
    test_endpoint "mcp-llm" ""$MCP_LLM_PORT"" "/mcp/list-tools" "POST" "{}" \
        "List available MCP tools"
    
    # Test list providers tool
    test_endpoint "mcp-llm" ""$MCP_LLM_PORT"" "/mcp/call-tool" "POST" '{
        "name": "list_providers",
        "arguments": {}
    }' "List LLM providers via MCP tool"
    
    # Test generate completion tool (with a simple prompt)
    test_endpoint "mcp-llm" ""$MCP_LLM_PORT"" "/mcp/call-tool" "POST" '{
        "name": "generate_completion",
        "arguments": {
            "provider": "openai",
            "prompt": "What color is the sky?",
            "maxTokens": 50
        }
    }' "Generate completion via MCP tool"
    
    # Test get provider status
    test_endpoint "mcp-llm" ""$MCP_LLM_PORT"" "/mcp/call-tool" "POST" '{
        "name": "get_provider_status",
        "arguments": {
            "provider": "openai"
        }
    }' "Get provider status via MCP tool"
}

# Test MCP Controller (Debate) endpoints
test_controller_endpoints() {
    echo ""
    echo "====================================="
    echo "Testing MCP Controller/Debate Endpoints"
    echo "====================================="
    
    # Test server info
    test_endpoint "mcp-controller" ""$MCP_CONTROLLER_PORT"" "/mcp" "GET" "" \
        "Get MCP server info"
    
    # Test list tools
    test_endpoint "mcp-controller" ""$MCP_CONTROLLER_PORT"" "/mcp/list-tools" "POST" "{}" \
        "List available MCP tools"
    
    # Test create debate tool
    response=$(test_endpoint "mcp-controller" ""$MCP_CONTROLLER_PORT"" "/mcp/call-tool" "POST" '{
        "name": "create_debate",
        "arguments": {
            "topic": "Should AI be regulated?",
            "format": "OXFORD",
            "organizationId": "test-org-001",
            "maxRounds": 3
        }
    }' "Create debate via MCP tool")
    
    # Extract debate ID from response if successful
    if echo """$response""" | grep -q "debateId"; then
        DEBATE_ID=$(echo """$response""" | jq -r '.debateId // empty' 2>/dev/null || echo "")
        if [ -n """$DEBATE_ID""" ]; then
            success "Created debate with ID: ""$DEBATE_ID"""
        fi
    fi
    
    # Test list debates tool
    test_endpoint "mcp-controller" ""$MCP_CONTROLLER_PORT"" "/mcp/call-tool" "POST" '{
        "name": "list_debates",
        "arguments": {
            "organizationId": "test-org-001"
        }
    }' "List debates via MCP tool"
    
    # Test get debate tool (if we have a debate ID)
    if [ -n """$DEBATE_ID""" ]; then
        test_endpoint "mcp-controller" ""$MCP_CONTROLLER_PORT"" "/mcp/call-tool" "POST" "{
            \"name\": \"get_debate\",
            \"arguments\": {
                \"debateId\": \"""$DEBATE_ID""\"
            }
        }" "Get debate details via MCP tool"
    fi
}

# Test standard REST endpoints
test_rest_endpoints() {
    echo ""
    echo "====================================="
    echo "Testing Standard REST Endpoints"
    echo "====================================="
    
    # Test LLM completion endpoint
    test_endpoint "mcp-llm" ""$MCP_LLM_PORT"" "/api/v1/completions" "POST" '{
        "messages": [{
            "role": "user",
            "content": "Hello"
        }],
        "provider": "openai",
        "maxTokens": 50
    }' "Test LLM completion REST endpoint"
    
    # Test LLM providers endpoint
    test_endpoint "mcp-llm" ""$MCP_LLM_PORT"" "/api/v1/providers" "GET" "" \
        "List providers via REST endpoint"
}

# Main execution
main() {
    echo "====================================="
    echo "MCP Endpoints Testing Suite"
    echo "====================================="
    echo "Testing MCP endpoints across all services"
    echo ""
    
    # Check if services are running
    check_services
    
    # Test each service's MCP endpoints
    test_organization_endpoints
    test_llm_endpoints
    test_controller_endpoints
    test_rest_endpoints
    
    echo ""
    echo "====================================="
    echo "Testing Summary"
    echo "====================================="
    success "MCP endpoints testing completed!"
    echo ""
    echo "Note: Some endpoints may fail if:"
    echo "  - Services are not running"
    echo "  - API keys are not configured"
    echo "  - Database is not initialized"
    echo ""
    echo "To start all services, run:"
    echo "  cd infrastructure/docker-compose"
    echo "  docker-compose up -d"
}

# Run main function
main "$@"