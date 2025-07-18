#!/bin/bash

# Test Environment Setup Script
# This script sets up the test environment for Karate API tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DOCKER_COMPOSE_FILE="infrastructure/docker-compose/docker-compose.yml"
TEST_DATABASE_NAME="test_debate_db"
TEST_USER="test_user"
TEST_PASSWORD="test_password"
TIMEOUT=300  # 5 minutes timeout for service startup

print_banner() {
    echo -e "${BLUE}"
    echo "=========================================="
    echo "  Karate API Tests Environment Setup"
    echo "=========================================="
    echo -e "${NC}"
}

check_prerequisites() {
    echo -e "${BLUE}Checking prerequisites...${NC}"
    
    local missing_tools=()
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        missing_tools+=("docker-compose")
    fi
    
    # Check Java
    if ! command -v java &> /dev/null; then
        missing_tools+=("java")
    else
        local java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        if [[ ! "$java_version" =~ ^(11|17|21) ]]; then
            echo -e "${YELLOW}Warning: Java version $java_version detected. Java 11, 17, or 21 is recommended.${NC}"
        fi
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        missing_tools+=("maven")
    fi
    
    # Check curl
    if ! command -v curl &> /dev/null; then
        missing_tools+=("curl")
    fi
    
    # Check jq
    if ! command -v jq &> /dev/null; then
        missing_tools+=("jq")
    fi
    
    if [ ${#missing_tools[@]} -gt 0 ]; then
        echo -e "${RED}Error: Missing required tools: ${missing_tools[*]}${NC}"
        echo -e "${YELLOW}Please install the missing tools and try again.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}All prerequisites are satisfied.${NC}"
}

setup_environment_files() {
    echo -e "${BLUE}Setting up environment files...${NC}"
    
    # Create .env file for test environment
    cat > .env.test << EOF
# Test Environment Configuration
NODE_ENV=test
DATABASE_URL=postgresql://test_user:test_password@localhost:5432/test_debate_db
REDIS_URL=redis://localhost:6379
QDRANT_URL=http://localhost:6333
RABBITMQ_URL=amqp://localhost:5672

# Service Ports
MCP_GATEWAY_PORT=8080
MCP_ORGANIZATION_PORT=5005
MCP_LLM_PORT=5002
MCP_CONTROLLER_PORT=5013
MCP_RAG_PORT=5004
MCP_TEMPLATE_PORT=5006
MCP_CONTEXT_PORT=5007
UI_PORT=3001

# API Keys (for testing)
CLAUDE_API_KEY=test-claude-key
OPENAI_API_KEY=test-openai-key
GEMINI_API_KEY=test-gemini-key

# JWT Configuration
JWT_SECRET=test-jwt-secret-key-for-testing-only
JWT_EXPIRATION=3600

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=test_debate_db
DB_USER=test_user
DB_PASSWORD=test_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Test Configuration
TEST_TIMEOUT=30000
TEST_PARALLEL_THREADS=4
TEST_REPORT_DIR=target/karate-reports
EOF

    echo -e "${GREEN}Environment files created successfully.${NC}"
}

setup_docker_compose() {
    echo -e "${BLUE}Setting up Docker Compose configuration...${NC}"
    
    # Create test-specific docker-compose file
    cat > docker-compose.test.yml << 'EOF'
version: '3.8'

services:
  postgres-test:
    image: postgres:15
    container_name: postgres-test
    environment:
      POSTGRES_DB: test_debate_db
      POSTGRES_USER: test_user
      POSTGRES_PASSWORD: test_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_test_data:/var/lib/postgresql/data
      - ./scripts/init-test-db.sql:/docker-entrypoint-initdb.d/init-test-db.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U test_user -d test_debate_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis-test:
    image: redis:7-alpine
    container_name: redis-test
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_test_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  qdrant-test:
    image: qdrant/qdrant:v1.7.0
    container_name: qdrant-test
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_test_data:/qdrant/storage
    environment:
      QDRANT__SERVICE__HTTP_PORT: 6333
      QDRANT__SERVICE__GRPC_PORT: 6334
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:6333/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  rabbitmq-test:
    image: rabbitmq:3-management-alpine
    container_name: rabbitmq-test
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: test_user
      RABBITMQ_DEFAULT_PASS: test_password
    volumes:
      - rabbitmq_test_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmqctl", "status"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_test_data:
  redis_test_data:
  qdrant_test_data:
  rabbitmq_test_data:

networks:
  default:
    name: karate-test-network
EOF

    echo -e "${GREEN}Docker Compose configuration created successfully.${NC}"
}

create_database_init_script() {
    echo -e "${BLUE}Creating database initialization script...${NC}"
    
    mkdir -p scripts
    
    cat > scripts/init-test-db.sql << 'EOF'
-- Test Database Initialization Script

-- Create additional databases for different services
CREATE DATABASE test_organization_db;
CREATE DATABASE test_context_db;
CREATE DATABASE test_rag_db;
CREATE DATABASE test_template_db;

-- Grant permissions to test user
GRANT ALL PRIVILEGES ON DATABASE test_debate_db TO test_user;
GRANT ALL PRIVILEGES ON DATABASE test_organization_db TO test_user;
GRANT ALL PRIVILEGES ON DATABASE test_context_db TO test_user;
GRANT ALL PRIVILEGES ON DATABASE test_rag_db TO test_user;
GRANT ALL PRIVILEGES ON DATABASE test_template_db TO test_user;

-- Create test data schema
\c test_debate_db;

-- Create test organizations table
CREATE TABLE IF NOT EXISTS test_organizations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    settings JSONB,
    tier VARCHAR(20) DEFAULT 'BASIC',
    features JSONB,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create test users table
CREATE TABLE IF NOT EXISTS test_users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER',
    organization_id INTEGER REFERENCES test_organizations(id),
    active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert test data
INSERT INTO test_organizations (name, description, settings, tier, features) VALUES
('Test Organization', 'Default test organization', 
 '{"allowPublicDebates": true, "maxDebateParticipants": 10}', 
 'ENTERPRISE', 
 '{"aiAssistant": true, "advancedAnalytics": true, "apiAccess": true}')
ON CONFLICT (name) DO NOTHING;

INSERT INTO test_users (email, name, password_hash, role, organization_id) VALUES
('demo@zamaz.com', 'Demo User', '$2a$10$demoPasswordHash', 'USER', 1),
('admin@zamaz.com', 'Admin User', '$2a$10$adminPasswordHash', 'ADMIN', 1)
ON CONFLICT (email) DO NOTHING;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_test_users_email ON test_users(email);
CREATE INDEX IF NOT EXISTS idx_test_users_organization_id ON test_users(organization_id);
CREATE INDEX IF NOT EXISTS idx_test_organizations_name ON test_organizations(name);
EOF

    echo -e "${GREEN}Database initialization script created successfully.${NC}"
}

start_infrastructure() {
    echo -e "${BLUE}Starting test infrastructure...${NC}"
    
    # Start Docker Compose services
    docker-compose -f docker-compose.test.yml up -d
    
    echo -e "${YELLOW}Waiting for services to start...${NC}"
    
    # Wait for PostgreSQL
    echo -e "${BLUE}Waiting for PostgreSQL...${NC}"
    wait_for_service "postgres-test" "postgresql" "localhost:5432"
    
    # Wait for Redis
    echo -e "${BLUE}Waiting for Redis...${NC}"
    wait_for_service "redis-test" "redis" "localhost:6379"
    
    # Wait for Qdrant
    echo -e "${BLUE}Waiting for Qdrant...${NC}"
    wait_for_service "qdrant-test" "qdrant" "localhost:6333"
    
    # Wait for RabbitMQ
    echo -e "${BLUE}Waiting for RabbitMQ...${NC}"
    wait_for_service "rabbitmq-test" "rabbitmq" "localhost:5672"
    
    echo -e "${GREEN}All infrastructure services are running!${NC}"
}

wait_for_service() {
    local service_name=$1
    local service_type=$2
    local connection_string=$3
    local timeout=${TIMEOUT}
    local start_time=$(date +%s)
    
    while [ $(($(date +%s) - start_time)) -lt $timeout ]; do
        if docker-compose -f docker-compose.test.yml ps | grep -q "$service_name.*Up"; then
            case $service_type in
                "postgresql")
                    if docker exec postgres-test pg_isready -U test_user -d test_debate_db &> /dev/null; then
                        echo -e "${GREEN}✓ PostgreSQL is ready${NC}"
                        return 0
                    fi
                    ;;
                "redis")
                    if docker exec redis-test redis-cli ping &> /dev/null; then
                        echo -e "${GREEN}✓ Redis is ready${NC}"
                        return 0
                    fi
                    ;;
                "qdrant")
                    if curl -s -f http://localhost:6333/health &> /dev/null; then
                        echo -e "${GREEN}✓ Qdrant is ready${NC}"
                        return 0
                    fi
                    ;;
                "rabbitmq")
                    if docker exec rabbitmq-test rabbitmqctl status &> /dev/null; then
                        echo -e "${GREEN}✓ RabbitMQ is ready${NC}"
                        return 0
                    fi
                    ;;
            esac
        fi
        
        echo -e "${YELLOW}Waiting for $service_name...${NC}"
        sleep 5
    done
    
    echo -e "${RED}Error: $service_name failed to start within $timeout seconds${NC}"
    return 1
}

setup_maven_configuration() {
    echo -e "${BLUE}Setting up Maven configuration...${NC}"
    
    # Create Maven settings for test environment
    mkdir -p ~/.m2
    
    cat > ~/.m2/settings-test.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <profiles>
        <profile>
            <id>test</id>
            <properties>
                <karate.env>test</karate.env>
                <test.database.url>jdbc:postgresql://localhost:5432/test_debate_db</test.database.url>
                <test.database.username>test_user</test.database.username>
                <test.database.password>test_password</test.database.password>
                <test.redis.host>localhost</test.redis.host>
                <test.redis.port>6379</test.redis.port>
            </properties>
        </profile>
    </profiles>
    
    <activeProfiles>
        <activeProfile>test</activeProfile>
    </activeProfiles>
</settings>
EOF

    echo -e "${GREEN}Maven configuration created successfully.${NC}"
}

validate_setup() {
    echo -e "${BLUE}Validating test environment setup...${NC}"
    
    local validation_errors=()
    
    # Check database connection
    if ! docker exec postgres-test psql -U test_user -d test_debate_db -c "SELECT 1;" &> /dev/null; then
        validation_errors+=("PostgreSQL connection failed")
    fi
    
    # Check Redis connection
    if ! docker exec redis-test redis-cli ping &> /dev/null; then
        validation_errors+=("Redis connection failed")
    fi
    
    # Check Qdrant connection
    if ! curl -s -f http://localhost:6333/health &> /dev/null; then
        validation_errors+=("Qdrant connection failed")
    fi
    
    # Check RabbitMQ connection
    if ! docker exec rabbitmq-test rabbitmqctl status &> /dev/null; then
        validation_errors+=("RabbitMQ connection failed")
    fi
    
    if [ ${#validation_errors[@]} -gt 0 ]; then
        echo -e "${RED}Validation failed with errors:${NC}"
        for error in "${validation_errors[@]}"; do
            echo -e "${RED}  - $error${NC}"
        done
        exit 1
    fi
    
    echo -e "${GREEN}Environment validation completed successfully!${NC}"
}

cleanup_environment() {
    echo -e "${BLUE}Cleaning up test environment...${NC}"
    
    # Stop Docker Compose services
    docker-compose -f docker-compose.test.yml down -v
    
    # Remove test networks
    docker network ls | grep "karate-test-network" | awk '{print $1}' | xargs -r docker network rm
    
    # Remove test volumes
    docker volume ls | grep "karate-api-tests" | awk '{print $2}' | xargs -r docker volume rm
    
    echo -e "${GREEN}Environment cleanup completed.${NC}"
}

print_summary() {
    echo -e "${GREEN}"
    echo "=========================================="
    echo "  Test Environment Setup Complete!"
    echo "=========================================="
    echo -e "${NC}"
    echo -e "${BLUE}Service URLs:${NC}"
    echo "  PostgreSQL: localhost:5432"
    echo "  Redis: localhost:6379"
    echo "  Qdrant: http://localhost:6333"
    echo "  RabbitMQ: http://localhost:15672"
    echo ""
    echo -e "${BLUE}Next steps:${NC}"
    echo "  1. Start the application services"
    echo "  2. Run tests: ./scripts/run-tests.sh"
    echo "  3. View reports in: target/karate-reports/"
    echo ""
    echo -e "${BLUE}Useful commands:${NC}"
    echo "  - Run smoke tests: ./scripts/run-tests.sh --tags @smoke"
    echo "  - Run in parallel: ./scripts/run-tests.sh --parallel 4"
    echo "  - Clean environment: ./scripts/setup-test-env.sh --cleanup"
}

# Main execution
main() {
    print_banner
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --cleanup)
                cleanup_environment
                exit 0
                ;;
            --validate)
                validate_setup
                exit 0
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "  --cleanup    Clean up test environment"
                echo "  --validate   Validate test environment"
                echo "  -h, --help   Show this help message"
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    check_prerequisites
    setup_environment_files
    setup_docker_compose
    create_database_init_script
    start_infrastructure
    setup_maven_configuration
    validate_setup
    print_summary
}

# Run main function
main "$@"