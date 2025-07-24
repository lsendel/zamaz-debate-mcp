#!/bin/bash

# Manual Maven Build Fix
# Fixes the build issues without relying on Maven commands initially

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

main() {
    log_info "Starting manual Maven build fix..."
    
    cd "${PROJECT_ROOT}"
    
    # Step 1: Fix the POM file
    fix_pom_file
    
    # Step 2: Clean build artifacts manually
    clean_manually
    
    # Step 3: Create missing config server module
    create_config_server
    
    # Step 4: Try Maven after fixes
    test_maven_after_fix
    
    log_success "Manual build fix completed!"
}

fix_pom_file() {
    log_info "Fixing parent POM file..."
    
    # Create backup
    cp pom.xml pom.xml.manual-backup
    
    # The issue might be that mcp-config-server is declared but doesn't exist
    # Let's comment it out temporarily
    if grep -q "<module>mcp-config-server</module>" pom.xml; then
        log_info "Commenting out missing mcp-config-server module..."
        sed -i.tmp 's|<module>mcp-config-server</module>|<!-- <module>mcp-config-server</module> MISSING -->|g' pom.xml
        log_success "Commented out missing module"
    fi
    
    # Check for any other potential issues in the POM
    log_info "Checking POM structure..."
    
    # Verify all declared modules exist
    local missing_modules=()
    while IFS= read -r module; do
        if [[ -n "$module" ]]; then
            if [[ ! -d "$module" ]]; then
                missing_modules+=("$module")
                log_warn "Missing module directory: $module"
            fi
        fi
    done < <(grep -o '<module>[^<]*</module>' pom.xml | sed 's/<module>//g' | sed 's/<\/module>//g')
    
    if [[ ${#missing_modules[@]} -gt 0 ]]; then
        log_warn "Found ${#missing_modules[@]} missing modules"
        for module in "${missing_modules[@]}"; do
            log_warn "  - $module"
        done
    else
        log_success "All declared modules exist"
    fi
}

clean_manually() {
    log_info "Cleaning build artifacts manually..."
    
    # Remove all target directories
    log_info "Removing target directories..."
    find . -name "target" -type d -print0 | xargs -0 rm -rf 2>/dev/null || true
    
    # Remove compiled classes
    log_info "Removing compiled classes..."
    find . -name "*.class" -delete 2>/dev/null || true
    
    # Clean Maven local repository for this project
    if [[ -d ~/.m2/repository/com/zamaz/mcp ]]; then
        log_info "Cleaning project artifacts from local Maven repository..."
        rm -rf ~/.m2/repository/com/zamaz/mcp
    fi
    
    # Clean any Maven metadata files that might be corrupted
    find ~/.m2/repository -name "*.lastUpdated" -delete 2>/dev/null || true
    find ~/.m2/repository -name "_remote.repositories" -delete 2>/dev/null || true
    
    log_success "Manual cleanup completed"
}

create_config_server() {
    log_info "Creating missing mcp-config-server module..."
    
    local module_dir="mcp-config-server"
    
    if [[ -d "$module_dir" ]]; then
        log_info "mcp-config-server already exists, skipping creation"
        return 0
    fi
    
    # Create directory structure
    mkdir -p "$module_dir/src/main/java/com/zamaz/mcp/config"
    mkdir -p "$module_dir/src/main/resources"
    mkdir -p "$module_dir/src/test/java/com/zamaz/mcp/config"
    
    # Create POM file
    cat > "$module_dir/pom.xml" << 'EOF'
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
    cat > "$module_dir/src/main/java/com/zamaz/mcp/config/ConfigServerApplication.java" << 'EOF'
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
    cat > "$module_dir/src/main/resources/application.yml" << 'EOF'
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

management:
  endpoints:
    web:
      exposure:
        include: health,info

logging:
  level:
    org.springframework.cloud.config: INFO
EOF

    # Create basic test
    cat > "$module_dir/src/test/java/com/zamaz/mcp/config/ConfigServerApplicationTest.java" << 'EOF'
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

    # Restore the original POM with the module uncommented
    if [[ -f "pom.xml.manual-backup" ]]; then
        cp pom.xml.manual-backup pom.xml
    fi
    
    log_success "Created mcp-config-server module"
}

test_maven_after_fix() {
    log_info "Testing Maven after fixes..."
    
    # Set environment variables that might help
    export MAVEN_OPTS="-Xmx2048m -XX:MaxMetaspaceSize=512m"
    
    # Try to fix Maven if the issue persists
    if command -v mvn >/dev/null 2>&1; then
        log_info "Testing Maven command..."
        
        # Try a simple Maven command
        if mvn --version >/dev/null 2>&1; then
            log_success "Maven is working!"
            
            # Try to validate the POM
            log_info "Validating parent POM..."
            if mvn help:effective-pom -q >/dev/null 2>&1; then
                log_success "Parent POM is valid"
                
                # Try to compile
                log_info "Attempting compilation..."
                if mvn clean compile --batch-mode --no-transfer-progress; then
                    log_success "Compilation successful!"
                else
                    log_warn "Compilation failed, but POM is valid"
                fi
            else
                log_error "Parent POM validation failed"
            fi
        else
            log_error "Maven command still failing"
            log_info "You may need to reinstall Maven or check your JAVA_HOME"
        fi
    else
        log_error "Maven command not found"
    fi
}

# Cleanup function
cleanup() {
    cd "${PROJECT_ROOT}"
    rm -f pom.xml.tmp
}

# Set trap for cleanup
trap cleanup EXIT

# Run main function
main "$@"