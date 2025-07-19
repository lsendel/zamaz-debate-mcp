#!/bin/bash

# Script to add Spring Cloud Config Client dependency to all services
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

# Dependency to add
CONFIG_CLIENT_DEPENDENCY='        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>'

# Function to check if dependency already exists
has_config_client_dependency() {
    local pom_file=$1
    grep -q "spring-cloud-starter-config" "$pom_file"
}

# Function to add dependency to pom.xml
add_dependency() {
    local pom_file=$1
    local service_name=$2
    
    if has_config_client_dependency "$pom_file"; then
        echo -e "${YELLOW}$service_name already has Config Client dependency${NC}"
        return 0
    fi
    
    # Backup the file
    cp "$pom_file" "$pom_file.bak"
    
    # Add the dependency after the last </dependency> in <dependencies> section
    # This is a bit tricky with sed, so we'll use a temporary file
    local temp_file=$(mktemp)
    local in_dependencies=false
    local last_dependency_line=0
    local line_num=0
    
    while IFS= read -r line; do
        ((line_num++))
        if [[ "$line" =~ \<dependencies\> ]]; then
            in_dependencies=true
        elif [[ "$line" =~ \</dependencies\> ]]; then
            in_dependencies=false
        elif [[ "$in_dependencies" == true ]] && [[ "$line" =~ \</dependency\> ]]; then
            last_dependency_line=$line_num
        fi
    done < "$pom_file"
    
    # Insert the new dependency after the last </dependency>
    awk -v n="$last_dependency_line" -v dep="$CONFIG_CLIENT_DEPENDENCY" '
        NR == n { print $0; print "\n" dep; next }
        { print }
    ' "$pom_file" > "$temp_file"
    
    mv "$temp_file" "$pom_file"
    
    echo -e "${GREEN}Added Config Client dependency to $service_name${NC}"
}

# Function to add Spring Cloud Bus dependency (optional)
add_bus_dependency() {
    local pom_file=$1
    local service_name=$2
    
    if grep -q "spring-cloud-starter-bus-amqp" "$pom_file"; then
        return 0
    fi
    
    local bus_dependency='        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-bus-amqp</artifactId>
        </dependency>'
    
    # Add after config client dependency
    sed -i.tmp "/<artifactId>spring-cloud-starter-config<\/artifactId>/a\\
        <\/dependency>\\
\\
$bus_dependency" "$pom_file"
    
    rm -f "$pom_file.tmp"
    echo -e "${GREEN}Added Spring Cloud Bus dependency to $service_name${NC}"
}

# Main script
echo -e "${GREEN}Adding Spring Cloud Config Client to all services...${NC}"
echo ""

for service in "${SERVICES[@]}"; do
    pom_file="$PROJECT_ROOT/$service/pom.xml"
    
    if [ ! -f "$pom_file" ]; then
        echo -e "${RED}POM file not found for $service${NC}"
        continue
    fi
    
    echo "Processing $service..."
    add_dependency "$pom_file" "$service"
    
    # Optionally add Spring Cloud Bus for dynamic refresh
    read -p "Add Spring Cloud Bus dependency to $service for dynamic refresh? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        add_bus_dependency "$pom_file" "$service"
    fi
    
    echo ""
done

echo -e "${GREEN}Configuration dependencies added successfully!${NC}"
echo ""
echo "Next steps:"
echo "1. Create bootstrap.yml for each service"
echo "2. Move configurations to config-repo"
echo "3. Update docker-compose.yml to add Config Server dependency"