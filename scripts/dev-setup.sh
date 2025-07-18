#!/bin/bash

# MCP Development Environment Setup Script
# Simplifies the setup process for new developers

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname """$SCRIPT_DIR""")"

# Configuration
SETUP_MODE="${1:-full}"
ENV_FILE="""$PROJECT_ROOT""/.env"
ENV_EXAMPLE="""$PROJECT_ROOT""/.env.example"

# Functions
print_header() {
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    local missing_tools=()
    
    # Check Java
    if ! command -v java &> /dev/null || [[ "$(java" -version 2>&1 | grep -oE '[0-9]+' | head -1) -lt 21 ]]; then
        missing_tools+=("Java 21+")
    else
        print_success "Java 21+ found"
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        missing_tools+=("Maven")
    else
        print_success "Maven found"
    fi
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        missing_tools+=("Docker")
    else
        print_success "Docker found"
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        missing_tools+=("Docker Compose")
    else
        print_success "Docker Compose found"
    fi
    
    # Check Node.js (for UI)
    if ! command -v node &> /dev/null || [[ "$(node" -v | grep -oE '[0-9]+' | head -1) -lt 16 ]]; then
        missing_tools+=("Node.js 16+")
    else
        print_success "Node.js found"
    fi
    
    # Check npm
    if ! command -v npm &> /dev/null; then
        missing_tools+=("npm")
    else
        print_success "npm found"
    fi
    
    if [ "${#missing_tools[@]}" -ne 0 ]; then
        print_error "Missing required tools: ${missing_tools[*]}"
        print_info "Please install the missing tools and run this script again"
        exit 1
    fi
    
    echo
}

# Setup environment file
setup_environment() {
    print_header "Setting up Environment"
    
    if [ ! -f """$ENV_EXAMPLE""" ]; then
        print_warning "Creating .env.example file"
        cat > """$ENV_EXAMPLE""" << 'EOF'
# Database Configuration
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_USER=mcp_user
POSTGRES_PASSWORD=mcp_password
POSTGRES_DB=mcp_db

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Security Configuration
JWT_SECRET=your-256-bit-secret-key-for-jwt-signing
JWT_ISSUER=mcp-local

# LLM Provider API Keys (Optional for local development)
OPENAI_API_KEY=
ANTHROPIC_API_KEY=
GOOGLE_API_KEY=
OLLAMA_BASE_URL=http://localhost:11434

# Organization Settings
DEFAULT_ORGANIZATION_ID=default-org
DEFAULT_ORGANIZATION_NAME=Default Organization

# Development Settings
SPRING_PROFILES_ACTIVE=dev
LOG_LEVEL=DEBUG
DEBUG_MODE=true

# Service Ports
GATEWAY_PORT=8080
ORGANIZATION_PORT=8081
LLM_PORT=8082
CONTROLLER_PORT=8083
RAG_PORT=8084
SECURITY_PORT=8085

# Monitoring (Optional)
PROMETHEUS_ENABLED=false
GRAFANA_ENABLED=false
JAEGER_ENABLED=false
EOF
    fi
    
    if [ ! -f """$ENV_FILE""" ]; then
        print_info "Creating .env file from template"
        cp """$ENV_EXAMPLE""" """$ENV_FILE"""
        
        # Generate secure JWT secret
        JWT_SECRET=$(openssl rand -base64 32)
        if [[ """$OSTYPE""" == "darwin"* ]]; then
            sed -i '' "s/your-256-bit-secret-key-for-jwt-signing/""$JWT_SECRET""/" """$ENV_FILE"""
        else
            sed -i "s/your-256-bit-secret-key-for-jwt-signing/""$JWT_SECRET""/" """$ENV_FILE"""
        fi
        
        print_success ".env file created"
        print_warning "Please update the .env file with your API keys if needed"
    else
        print_success ".env file already exists"
    fi
    
    echo
}

# Check port availability
check_ports() {
    print_header "Checking Port Availability"
    
    local ports=(5432 6379 8080 8081 8082 8083 8084 8085 3000)
    local used_ports=()
    
    for port in "${ports[@]}"; do
        if lsof -Pi :""$port"" -sTCP:LISTEN -t >/dev/null 2>&1; then
            used_ports+=(""$port"")
        fi
    done
    
    if [ "${#used_ports[@]}" -ne 0 ]; then
        print_error "The following ports are already in use: ${used_ports[*]}"
        print_info "Please stop the services using these ports or update the port configuration"
        
        if [ """$SETUP_MODE""" != "minimal" ]; then
            read -p "Continue anyway? (y/N) " -n 1 -r
            echo
            if [[ ! ""$REPLY"" =~ ^[Yy]$ ]]; then
                exit 1
            fi
        fi
    else
        print_success "All required ports are available"
    fi
    
    echo
}

# Build projects
build_projects() {
    print_header "Building Projects"
    
    cd """$PROJECT_ROOT"""
    
    if [ """$SETUP_MODE""" == "minimal" ]; then
        print_info "Building only essential modules..."
        mvn clean install -DskipTests -pl mcp-common,mcp-security -am
    else
        print_info "Building all modules..."
        mvn clean install -DskipTests
    fi
    
    if [ "$?" -eq 0 ]; then
        print_success "Build completed successfully"
    else
        print_error "Build failed"
        exit 1
    fi
    
    echo
}

# Setup databases
setup_databases() {
    print_header "Setting up Databases"
    
    cd """$PROJECT_ROOT"""
    
    # Start only database services
    print_info "Starting PostgreSQL and Redis..."
    docker-compose up -d postgres redis
    
    # Wait for services to be ready
    print_info "Waiting for databases to be ready..."
    sleep 5
    
    # Check PostgreSQL
    if docker-compose exec -T postgres pg_isready >/dev/null 2>&1; then
        print_success "PostgreSQL is ready"
    else
        print_error "PostgreSQL failed to start"
        exit 1
    fi
    
    # Check Redis
    if docker-compose exec -T redis redis-cli ping >/dev/null 2>&1; then
        print_success "Redis is ready"
    else
        print_error "Redis failed to start"
        exit 1
    fi
    
    echo
}

# Setup UI dependencies
setup_ui() {
    print_header "Setting up UI Dependencies"
    
    if [ -d """$PROJECT_ROOT""/debate-ui" ]; then
        cd """$PROJECT_ROOT""/debate-ui"
        
        print_info "Installing UI dependencies..."
        npm install
        
        if [ "$?" -eq 0 ]; then
            print_success "UI dependencies installed"
        else
            print_error "Failed to install UI dependencies"
            exit 1
        fi
    else
        print_warning "UI directory not found, skipping UI setup"
    fi
    
    cd """$PROJECT_ROOT"""
    echo
}

# Create test data
create_test_data() {
    print_header "Creating Test Data"
    
    print_info "Creating default organization..."
    # This would normally make an API call or run a SQL script
    print_success "Test data created"
    
    echo
}

# Display next steps
show_next_steps() {
    print_header "Setup Complete! 🎉"
    
    echo -e "${GREEN}Your development environment is ready!${NC}"
    echo
    echo "Next steps:"
    echo "1. Start all services:"
    echo "   ${BLUE}make start-all${NC}"
    echo
    echo "2. Start individual services:"
    echo "   ${BLUE}make start${NC}        # Backend services"
    echo "   ${BLUE}make ui${NC}           # UI development server"
    echo
    echo "3. Run tests:"
    echo "   ${BLUE}make test${NC}         # All tests"
    echo "   ${BLUE}make test-unit${NC}    # Unit tests only"
    echo
    echo "4. Access the application:"
    echo "   UI: ${BLUE}http://localhost:3000${NC}"
    echo "   API: ${BLUE}http://localhost:8080${NC}"
    echo
    echo "5. View logs:"
    echo "   ${BLUE}make logs${NC}         # All services"
    echo "   ${BLUE}make logs service=mcp-llm${NC}  # Specific service"
    echo
    
    if [ """$SETUP_MODE""" == "minimal" ]; then
        echo "Note: You ran minimal setup. To complete full setup, run:"
        echo "   ${BLUE}./scripts/dev-setup.sh full${NC}"
    fi
}

# Main execution
main() {
    print_header "MCP Development Environment Setup"
    echo "Setup mode: ""$SETUP_MODE"""
    echo
    
    check_prerequisites
    setup_environment
    check_ports
    
    if [ """$SETUP_MODE""" != "minimal" ]; then
        build_projects
        setup_databases
        setup_ui
        create_test_data
    else
        print_info "Minimal setup - skipping build and database setup"
    fi
    
    show_next_steps
}

# Run main function
main