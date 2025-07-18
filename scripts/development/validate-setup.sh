#!/bin/bash

# Comprehensive setup validation script
# This script validates that everything is properly configured

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 MCP Debate System - Setup Validation${NC}"
echo "========================================"

# Function to check command exists
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✅ $1 is installed${NC}"
        return 0
    else
        echo -e "${RED}❌ $1 is not installed${NC}"
        return 1
    fi
}

# Function to check file exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✅ $1 exists${NC}"
        return 0
    else
        echo -e "${RED}❌ $1 missing${NC}"
        return 1
    fi
}

# Function to check directory exists
check_directory() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✅ $1 directory exists${NC}"
        return 0
    else
        echo -e "${RED}❌ $1 directory missing${NC}"
        return 1
    fi
}

# Function to check environment variable
check_env_var() {
    if [ -n "${!1}" ]; then
        echo -e "${GREEN}✅ $1 is set${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️ $1 is not set${NC}"
        return 1
    fi
}

echo ""
echo -e "${BLUE}📋 Checking Dependencies...${NC}"
echo "------------------------"

DEPS_OK=true

check_command "docker" || DEPS_OK=false
check_command "docker-compose" || DEPS_OK=false
check_command "node" || DEPS_OK=false
check_command "npm" || DEPS_OK=false
check_command "python3" || DEPS_OK=false
check_command "git" || DEPS_OK=false
check_command "curl" || DEPS_OK=false
check_command "make" || DEPS_OK=false

if [ """"$DEPS_OK"""" = false ]; then
    echo -e "${RED}❌ Some required dependencies are missing${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}📁 Checking Project Structure...${NC}"
echo "------------------------------"

STRUCTURE_OK=true

# Core directories
check_directory "mcp-context" || STRUCTURE_OK=false
check_directory "mcp-llm" || STRUCTURE_OK=false
check_directory "mcp-debate" || STRUCTURE_OK=false
check_directory "mcp-rag" || STRUCTURE_OK=false
check_directory "debate-ui" || STRUCTURE_OK=false
check_directory "e2e-tests" || STRUCTURE_OK=false

# Key files
check_file "docker-compose.yml" || STRUCTURE_OK=false
check_file "Makefile" || STRUCTURE_OK=false
check_file ".env.example" || STRUCTURE_OK=false
check_file "SECURITY.md" || STRUCTURE_OK=false

# GitIgnore files
check_file ".gitignore" || STRUCTURE_OK=false
check_file "mcp-context/.gitignore" || STRUCTURE_OK=false
check_file "mcp-llm/.gitignore" || STRUCTURE_OK=false
check_file "mcp-debate/.gitignore" || STRUCTURE_OK=false
check_file "mcp-rag/.gitignore" || STRUCTURE_OK=false
check_file "debate-ui/.gitignore" || STRUCTURE_OK=false
check_file "e2e-tests/.gitignore" || STRUCTURE_OK=false

if [ """"$STRUCTURE_OK"""" = false ]; then
    echo -e "${RED}❌ Project structure is incomplete${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}🔒 Checking Security Configuration...${NC}"
echo "-----------------------------------"

SECURITY_OK=true

# Check for .env file
if [ -f ".env" ]; then
    echo -e "${GREEN}✅ .env file exists${NC}"
    
    # Check if .env contains placeholder values
    if grep -q "your-.*-key-here\|change-this" .env; then
        echo -e "${YELLOW}⚠️ .env contains placeholder values - remember to add real API keys${NC}"
    else
        echo -e "${GREEN}✅ .env appears to have real values${NC}"
    fi
else
    echo -e "${YELLOW}⚠️ .env file not found - will be created from template${NC}"
    cp .env.example .env
    echo -e "${YELLOW}📝 Created .env from template - please edit with your API keys${NC}"
    SECURITY_OK=false
fi

# Check git doesn't track .env files
if git ls-files | grep -E '\.env$' > /dev/null; then
    echo -e "${RED}❌ .env files are tracked by git! This is a security risk!${NC}"
    echo -e "${RED}   Run: git rm --cached .env${NC}"
    SECURITY_OK=false
else
    echo -e "${GREEN}✅ No .env files in git${NC}"
fi

# Check for any committed secrets
if git grep -l "sk-" 2>/dev/null | grep -v ".example" > /dev/null; then
    echo -e "${RED}❌ Potential API keys found in git!${NC}"
    SECURITY_OK=false
else
    echo -e "${GREEN}✅ No obvious API keys in git${NC}"
fi

echo ""
echo -e "${BLUE}📦 Checking Dependencies Installation...${NC}"
echo "--------------------------------------"

INSTALL_OK=true

# Check UI dependencies
if [ -d "debate-ui/node_modules" ]; then
    echo -e "${GREEN}✅ UI dependencies installed${NC}"
else
    echo -e "${YELLOW}⚠️ UI dependencies not installed${NC}"
    echo -e "${BLUE}Installing UI dependencies...${NC}"
    cd debate-ui && npm install && cd ..
fi

# Check test dependencies
if [ -d "e2e-tests/node_modules" ]; then
    echo -e "${GREEN}✅ Test dependencies installed${NC}"
else
    echo -e "${YELLOW}⚠️ Test dependencies not installed${NC}"
    echo -e "${BLUE}Installing test dependencies...${NC}"
    cd e2e-tests && npm install && cd ..
fi

echo ""
echo -e "${BLUE}🐳 Checking Docker Configuration...${NC}"
echo "--------------------------------"

# Check Docker is running
if docker info > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Docker is running${NC}"
else
    echo -e "${RED}❌ Docker is not running${NC}"
    exit 1
fi

# Validate docker-compose file
if docker-compose config > /dev/null 2>&1; then
    echo -e "${GREEN}✅ docker-compose.yml is valid${NC}"
else
    echo -e "${RED}❌ docker-compose.yml has errors${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}🧪 Quick Functionality Test...${NC}"
echo "---------------------------"

# Try to build images
echo -e "${BLUE}Building Docker images...${NC}"
if docker-compose build > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Docker images built successfully${NC}"
else
    echo -e "${RED}❌ Failed to build Docker images${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}📊 Summary${NC}"
echo "========="

if [ """"$DEPS_OK"""" = true ] && [ """"$STRUCTURE_OK"""" = true ] && [ """"$INSTALL_OK"""" = true ]; then
    echo -e "${GREEN}✅ All validations passed!${NC}"
    echo ""
    echo -e "${GREEN}🚀 Ready to run:${NC}"
    echo "  make up          # Start all services"
    echo "  make test        # Run tests"
    echo "  make ui          # Start UI development"
    echo ""
    
    if [ """"$SECURITY_OK"""" = false ]; then
        echo -e "${YELLOW}⚠️ Security setup incomplete:${NC}"
        echo "  1. Edit .env with your real API keys"
        echo "  2. Never commit .env files"
        echo "  3. Run 'make security-audit' to verify"
        echo ""
    fi
    
    echo -e "${BLUE}💡 Quick Start:${NC}"
    echo "  1. Edit .env with your API keys"
    echo "  2. make up"
    echo "  3. Open http://localhost:3000"
    echo ""
    
else
    echo -e "${RED}❌ Some validations failed${NC}"
    echo -e "${YELLOW}Please fix the issues above and run again${NC}"
    exit 1
fi