#!/bin/bash

# Script to migrate all services to use Spring Cloud Config Server
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Services that need Config Client (excluding config-server itself)
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

# Function to add Config Client dependency to pom.xml
add_config_client_dependency() {
    local service=$1
    local pom_file="$PROJECT_ROOT/$service/pom.xml"
    
    if [ ! -f "$pom_file" ]; then
        echo -e "${RED}POM file not found for $service${NC}"
        return 1
    fi
    
    # Check if dependency already exists
    if grep -q "spring-cloud-starter-config" "$pom_file"; then
        echo -e "${YELLOW}$service already has Config Client dependency${NC}"
        return 0
    fi
    
    # Find the dependencies section and add the dependency
    # Using a more reliable approach with awk
    local temp_file=$(mktemp)
    awk '
    /<dependencies>/ { deps = 1 }
    /<\/dependencies>/ && deps { 
        print "        <dependency>"
        print "            <groupId>org.springframework.cloud</groupId>"
        print "            <artifactId>spring-cloud-starter-config</artifactId>"
        print "        </dependency>"
        print ""
        deps = 0
    }
    { print }
    ' "$pom_file" > "$temp_file"
    
    mv "$temp_file" "$pom_file"
    echo -e "${GREEN}Added Config Client dependency to $service${NC}"
}

# Function to create bootstrap.yml
create_bootstrap_yml() {
    local service=$1
    local resources_dir="$PROJECT_ROOT/$service/src/main/resources"
    local bootstrap_file="$resources_dir/bootstrap.yml"
    
    # Create resources directory if it doesn't exist
    mkdir -p "$resources_dir"
    
    # Skip if bootstrap.yml already exists
    if [ -f "$bootstrap_file" ]; then
        echo -e "${YELLOW}bootstrap.yml already exists for $service${NC}"
        return 0
    fi
    
    # Create bootstrap.yml
    cat > "$bootstrap_file" <<EOF
spring:
  application:
    name: ${service}
  cloud:
    config:
      uri: \${CONFIG_SERVER_URI:http://localhost:8888}
      fail-fast: true
      retry:
        initial-interval: 1000
        max-attempts: 5
        max-interval: 2000
        multiplier: 1.1
  profiles:
    active: \${SPRING_PROFILES_ACTIVE:development}

# Enable configuration refresh
management:
  endpoints:
    web:
      exposure:
        include: refresh,health,info
EOF
    
    echo -e "${GREEN}Created bootstrap.yml for $service${NC}"
}

# Function to create configuration file in config-repo
create_config_file() {
    local service=$1
    local config_repo="$PROJECT_ROOT/config-repo"
    local config_file="$config_repo/${service}.yml"
    local app_yml="$PROJECT_ROOT/$service/src/main/resources/application.yml"
    local app_props="$PROJECT_ROOT/$service/src/main/resources/application.properties"
    
    # Create config-repo directory if it doesn't exist
    mkdir -p "$config_repo"
    
    # Skip if config file already exists
    if [ -f "$config_file" ]; then
        echo -e "${YELLOW}Configuration already exists for $service in config-repo${NC}"
        return 0
    fi
    
    # Check if service has existing application.yml or properties
    if [ -f "$app_yml" ]; then
        echo -e "${GREEN}Copying existing application.yml for $service to config-repo${NC}"
        cp "$app_yml" "$config_file"
        # Comment out the original file
        mv "$app_yml" "$app_yml.backup"
    elif [ -f "$app_props" ]; then
        echo -e "${GREEN}Converting application.properties for $service to YAML in config-repo${NC}"
        # Simple conversion (this is basic, might need manual adjustment)
        echo "# Converted from application.properties" > "$config_file"
        echo "# Please review and adjust as needed" >> "$config_file"
        echo "" >> "$config_file"
        while IFS='=' read -r key value; do
            # Skip comments and empty lines
            [[ "$key" =~ ^[[:space:]]*# ]] && continue
            [[ -z "$key" ]] && continue
            
            # Convert dot notation to YAML
            echo "$key: $value" >> "$config_file"
        done < "$app_props"
        mv "$app_props" "$app_props.backup"
    else
        echo -e "${GREEN}Creating default configuration for $service${NC}"
        # Create a default configuration
        cat > "$config_file" <<EOF
# Configuration for ${service}
server:
  port: \${SERVER_PORT:8080}

spring:
  application:
    name: ${service}

# Add service-specific configuration here
# Database, caching, security, etc.

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  health:
    readinessState:
      enabled: true
    livenessState:
      enabled: true

logging:
  level:
    com.zamaz.mcp: \${LOG_LEVEL:INFO}
EOF
    fi
}

# Function to create environment-specific configs
create_environment_configs() {
    local service=$1
    local config_repo="$PROJECT_ROOT/config-repo"
    local environments=("development" "staging" "production")
    
    for env in "${environments[@]}"; do
        local env_config="$config_repo/${service}-${env}.yml"
        
        if [ -f "$env_config" ]; then
            continue
        fi
        
        echo -e "${GREEN}Creating $env configuration for $service${NC}"
        
        case $env in
            "development")
                cat > "$env_config" <<EOF
# Development configuration for ${service}
logging:
  level:
    com.zamaz.mcp: DEBUG
    org.springframework.web: DEBUG

spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

# Development database
spring:
  datasource:
    url: \${DB_URL:jdbc:postgresql://localhost:5432/${service//-/_}_db}
    username: \${DB_USER:dev_user}
    password: \${DB_PASSWORD:{cipher}encrypted_dev_password}
EOF
                ;;
            "staging")
                cat > "$env_config" <<EOF
# Staging configuration for ${service}
logging:
  level:
    com.zamaz.mcp: INFO

# Staging database
spring:
  datasource:
    url: \${DB_URL:jdbc:postgresql://postgres-staging:5432/${service//-/_}_db}
    username: \${DB_USER:staging_user}
    password: \${DB_PASSWORD:{cipher}encrypted_staging_password}
EOF
                ;;
            "production")
                cat > "$env_config" <<EOF
# Production configuration for ${service}
logging:
  level:
    com.zamaz.mcp: WARN
    root: INFO

# Production database
spring:
  datasource:
    url: \${DB_URL:jdbc:postgresql://postgres-prod:5432/${service//-/_}_db}
    username: \${DB_USER:prod_user}
    password: \${DB_PASSWORD:{cipher}encrypted_prod_password}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

# Production-specific settings
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
EOF
                ;;
        esac
    done
}

# Main migration process
echo -e "${GREEN}Starting migration to Spring Cloud Config Server${NC}"
echo ""

# Step 1: Add dependencies
echo -e "${GREEN}Step 1: Adding Config Client dependencies...${NC}"
for service in "${SERVICES[@]}"; do
    add_config_client_dependency "$service"
done
echo ""

# Step 2: Create bootstrap.yml files
echo -e "${GREEN}Step 2: Creating bootstrap.yml files...${NC}"
for service in "${SERVICES[@]}"; do
    create_bootstrap_yml "$service"
done
echo ""

# Step 3: Move configurations to config-repo
echo -e "${GREEN}Step 3: Moving configurations to config-repo...${NC}"
for service in "${SERVICES[@]}"; do
    create_config_file "$service"
    create_environment_configs "$service"
done
echo ""

# Step 4: Create global application.yml if it doesn't exist
GLOBAL_CONFIG="$PROJECT_ROOT/config-repo/application.yml"
if [ ! -f "$GLOBAL_CONFIG" ]; then
    echo -e "${GREEN}Creating global application.yml...${NC}"
    cat > "$GLOBAL_CONFIG" <<EOF
# Global configuration for all services
spring:
  jackson:
    default-property-inclusion: non_null
    time-zone: UTC

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized

# Global resilience settings
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 100
        permittedNumberOfCallsInHalfOpenState: 10
        waitDurationInOpenState: 10000
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 1000
        retryExceptions:
          - java.io.IOException
          - java.net.ConnectException

# Global logging configuration
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
EOF
fi

# Step 5: Update docker-compose.yml
echo -e "${GREEN}Step 5: Updating docker-compose.yml...${NC}"
DOCKER_COMPOSE="$PROJECT_ROOT/infrastructure/docker-compose/docker-compose.yml"
if [ -f "$DOCKER_COMPOSE" ]; then
    echo -e "${YELLOW}Please manually update docker-compose.yml to add Config Server dependencies${NC}"
    echo "Add the following to each service:"
    echo "  environment:"
    echo "    - CONFIG_SERVER_URI=http://mcp-config-server:8888"
    echo "    - SPRING_PROFILES_ACTIVE=docker"
    echo "  depends_on:"
    echo "    mcp-config-server:"
    echo "      condition: service_healthy"
else
    echo -e "${RED}docker-compose.yml not found${NC}"
fi

echo ""
echo -e "${GREEN}Migration preparation complete!${NC}"
echo ""
echo "Next steps:"
echo "1. Review and adjust the generated configuration files in config-repo/"
echo "2. Encrypt sensitive values using: ./config-repo/encrypt-value.sh"
echo "3. Commit configuration files to Git repository"
echo "4. Update docker-compose.yml as indicated above"
echo "5. Test each service with Config Server"
echo ""
echo "To encrypt a value:"
echo "  cd config-repo && ./encrypt-value.sh 'your-secret-value'"
echo ""
echo "To test a service configuration:"
echo "  curl http://localhost:8888/service-name/profile"