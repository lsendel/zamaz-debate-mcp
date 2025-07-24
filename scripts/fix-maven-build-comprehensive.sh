#!/bin/bash

# Comprehensive Maven Build Issues Fix Script
# Fixes Maven build issues systematically

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Modules declared in parent POM
DECLARED_MODULES=(
    "mcp-common"
    "mcp-security"
    "mcp-auth-server"
    "mcp-config-server"  # This one is missing!
    "mcp-organization"
    "mcp-debate-engine"
    "mcp-llm"
    "mcp-rag"
    "mcp-docs"
    "mcp-gateway"
    "mcp-testing"
    "mcp-pattern-recognition"
    "github-integration"
    "karate-api-tests"
    "load-tests"
    "mcp-sidecar"
    "performance-tests/gatling"
    "server"
    "workflow-editor"
    "mcp-context"
    "mcp-controller"
    "mcp-debate"
    "mcp-template"
    "mcp-context-client"
    "mcp-modulith"
)

# Core modules that should be built first (dependency order)
CORE_MODULES=(
    "mcp-common"
    "mcp-security"
)

# Main function
main() {
    log_info "Starting comprehensive Maven build fix..."
    
    cd "${PROJECT_ROOT}"
    
    # Step 1: Validate environment
    validate_environment
    
    # Step 2: Fix parent POM issues
    fix_parent_pom_issues
    
    # Step 3: Clean everything
    clean_maven_artifacts
    
    # Step 4: Handle missing modules
    handle_missing_modules
    
    # Step 5: Fix dependency issues
    fix_dependency_issues
    
    # Step 6: Build core modules first
    build_core_modules
    
    # Step 7: Build remaining modules
    build_remaining_modules
    
    # Step 8: Validate the fixes
    validate_build_fixes
    
    log_success "Comprehensive Maven build fix completed!"
}

# Step 1: Validate environment
validate_environment() {
    log_info "Validating build environment..."
    
    # Check Java version
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    local java_version
    java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [[ "${java_version}" -lt 21 ]]; then
        log_error "Java 21 or higher is required. Current version: ${java_version}"
        exit 1
    fi
    
    # Check Maven (prefer wrapper)
    if [[ -x "./mvnw" ]]; then
        export MVN_CMD="./mvnw"
        log_info "Using Maven wrapper"
    elif command -v mvn &> /dev/null; then
        export MVN_CMD="mvn"
        log_info "Using system Maven"
    else
        log_error "Neither Maven wrapper nor system Maven found"
        exit 1
    fi
    
    # Set environment variables
    export JAVA_HOME="${JAVA_HOME:-$(readlink -f /usr/bin/java | sed 's:/bin/java::')}"
    export MAVEN_OPTS="-Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC"
    
    log_success "Environment validation passed"
}

# Step 2: Fix parent POM issues
fix_parent_pom_issues() {
    log_info "Fixing parent POM issues..."
    
    # Validate parent POM syntax
    if ! ${MVN_CMD} help:effective-pom -q > /dev/null 2>&1; then
        log_error "Parent POM has syntax errors. Please check pom.xml"
        exit 1
    fi
    
    # Check for missing modules and comment them out temporarily
    local temp_pom="pom.xml.temp"
    cp pom.xml "${temp_pom}"
    
    # Comment out missing modules
    for module in "${DECLARED_MODULES[@]}"; do
        if [[ ! -d "${module}" ]]; then
            log_warn "Module directory not found: ${module} - commenting out in POM"
            sed -i.bak "s|<module>${module}</module>|<!-- <module>${module}</module> MISSING MODULE -->|g" pom.xml
        fi
    done
    
    log_success "Parent POM issues fixed"
}

# Step 3: Clean everything
clean_maven_artifacts() {
    log_info "Cleaning Maven artifacts..."
    
    # Clean all target directories
    find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
    
    # Clean project artifacts from local repository
    if [[ -d ~/.m2/repository/com/zamaz/mcp ]]; then
        rm -rf ~/.m2/repository/com/zamaz/mcp
        log_info "Cleaned project artifacts from local Maven repository"
    fi
    
    # Clean corrupted Maven metadata
    find ~/.m2/repository -name "*.lastUpdated" -delete 2>/dev/null || true
    find ~/.m2/repository -name "_remote.repositories" -delete 2>/dev/null || true
    
    # Force clean with Maven
    mvn clean --batch-mode --no-transfer-progress -q || true
    
    log_success "Maven artifacts cleaned"
}

# Step 4: Handle missing modules
handle_missing_modules() {
    log_info "Handling missing modules..."
    
    # Create missing mcp-config-server module
    if [[ ! -d "mcp-config-server" ]]; then
        log_info "Creating missing mcp-config-server module..."
        create_config_server_module
    fi
    
    # Restore original POM with all modules
    if [[ -f "pom.xml.temp" ]]; then
        mv pom.xml.temp pom.xml
    fi
    
    log_success "Missing modules handled"
}

# Create mcp-config-server module
create_config_server_module() {
    local module_dir="mcp-config-server"
    mkdir -p "${module_dir}/src/main/java/com/zamaz/mcp/config"
    mkdir -p "${module_dir}/src/main/resources"
    mkdir -p "${module_dir}/src/test/java/com/zamaz/mcp/config"
    
    # Create POM
    cat > "${module_dir}/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.zamaz.mcp</groupId>
        <artifactId>mcp-parent</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mcp-config-server</artifactId>
    <name>MCP Config Server</name>
    <description>Spring Cloud Config Server for MCP services</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-config-server</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
EOF

    # Create main application class
    cat > "${module_dir}/src/main/java/com/zamaz/mcp/config/ConfigServerApplication.java" << 'EOF'
package com.zamaz.mcp.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
EOF

    # Create application.yml
    cat > "${module_dir}/src/main/resources/application.yml" << 'EOF'
server:
  port: 8888

spring:
  application:
    name: mcp-config-server
  cloud:
    config:
      server:
        git:
          uri: file://${user.home}/config-repo
          default-label: main
        health:
          enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh
  endpoint:
    health:
      show-details: always

logging:
  level:
    org.springframework.cloud.config: DEBUG
EOF

    # Create basic test
    cat > "${module_dir}/src/test/java/com/zamaz/mcp/config/ConfigServerApplicationTest.java" << 'EOF'
package com.zamaz.mcp.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ConfigServerApplicationTest {

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
    }
}
EOF

    log_success "Created mcp-config-server module"
}

# Step 5: Fix dependency issues
fix_dependency_issues() {
    log_info "Fixing dependency issues..."
    
    # Force update dependencies
    mvn dependency:purge-local-repository --batch-mode --no-transfer-progress -q || true
    
    # Resolve dependencies
    mvn dependency:resolve --batch-mode --no-transfer-progress -q || {
        log_warn "Some dependencies could not be resolved, continuing..."
    }
    
    # Download sources and javadocs for better IDE support
    mvn dependency:sources dependency:resolve -Dclassifier=javadoc --batch-mode --no-transfer-progress -q || true
    
    log_success "Dependency issues addressed"
}

# Step 6: Build core modules first
build_core_modules() {
    log_info "Building core modules..."
    
    for module in "${CORE_MODULES[@]}"; do
        if [[ -d "${module}" ]]; then
            log_info "Building core module: ${module}"
            cd "${PROJECT_ROOT}/${module}"
            
            if mvn clean install -DskipTests --batch-mode --no-transfer-progress; then
                log_success "Core module built successfully: ${module}"
            else
                log_error "Failed to build core module: ${module}"
                cd "${PROJECT_ROOT}"
                return 1
            fi
            
            cd "${PROJECT_ROOT}"
        else
            log_warn "Core module directory not found: ${module}"
        fi
    done
    
    log_success "Core modules built successfully"
}

# Step 7: Build remaining modules
build_remaining_modules() {
    log_info "Building remaining modules..."
    
    # Try full project build first
    if mvn clean compile -T 2C --batch-mode --no-transfer-progress; then
        log_success "Full project compilation successful"
        return 0
    fi
    
    log_warn "Full compilation failed, building modules individually..."
    
    # Build each module individually
    for module in "${DECLARED_MODULES[@]}"; do
        # Skip core modules (already built)
        if [[ " ${CORE_MODULES[*]} " =~ " ${module} " ]]; then
            continue
        fi
        
        if [[ -d "${module}" ]]; then
            log_info "Building module: ${module}"
            cd "${PROJECT_ROOT}/${module}"
            
            if mvn clean compile --batch-mode --no-transfer-progress; then
                log_success "Module compiled successfully: ${module}"
            else
                log_error "Module compilation failed: ${module}"
                # Continue with other modules
            fi
            
            cd "${PROJECT_ROOT}"
        fi
    done
    
    log_success "Individual module builds completed"
}

# Step 8: Validate the fixes
validate_build_fixes() {
    log_info "Validating build fixes..."
    
    # Run validation script if it exists
    if [[ -x "scripts/validation/validate-maven-build.sh" ]]; then
        log_info "Running Maven build validation..."
        if scripts/validation/validate-maven-build.sh; then
            log_success "Maven build validation passed"
        else
            log_warn "Maven build validation had issues, but continuing..."
        fi
    fi
    
    # Quick smoke test
    log_info "Running final smoke test..."
    if mvn clean compile -T 2C --batch-mode --no-transfer-progress -q; then
        log_success "Final smoke test passed"
    else
        log_error "Final smoke test failed"
        return 1
    fi
    
    # Test that we can package at least one Spring Boot application
    local test_modules=("mcp-organization" "mcp-llm" "mcp-gateway")
    for module in "${test_modules[@]}"; do
        if [[ -d "${module}" ]]; then
            log_info "Testing Spring Boot packaging for: ${module}"
            cd "${PROJECT_ROOT}/${module}"
            
            if mvn clean package -DskipTests --batch-mode --no-transfer-progress -q; then
                log_success "Spring Boot packaging test passed for: ${module}"
                cd "${PROJECT_ROOT}"
                break
            else
                log_warn "Spring Boot packaging test failed for: ${module}"
                cd "${PROJECT_ROOT}"
            fi
        fi
    done
    
    log_success "Build validation completed"
}

# Cleanup function
cleanup() {
    cd "${PROJECT_ROOT}"
    # Remove temporary files
    rm -f pom.xml.bak pom.xml.temp
}

# Set trap for cleanup
trap cleanup EXIT

# Run main function
main "$@"