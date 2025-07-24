#!/bin/bash

# Maven Build Validation Script
# Validates that all Maven modules can be built successfully

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

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

# Maven modules to validate
MAVEN_MODULES=(
    "mcp-common"
    "mcp-security"
    "mcp-auth-server"
    "mcp-config-server"
    "mcp-organization"
    "mcp-debate-engine"
    "mcp-llm"
    "mcp-rag"
    "mcp-docs"
    "mcp-gateway"
    "mcp-testing"
    "mcp-pattern-recognition"
    "github-integration"
    "workflow-editor"
    "mcp-context"
    "mcp-controller"
    "mcp-debate"
    "mcp-template"
    "mcp-context-client"
    "mcp-modulith"
)

# Main validation function
main() {
    log_info "Starting Maven build validation..."
    
    cd "${PROJECT_ROOT}"
    
    local failed_modules=()
    local success_count=0
    
    # Validate parent POM first
    log_info "Validating parent POM..."
    if validate_parent_pom; then
        log_success "Parent POM validation passed"
    else
        log_error "Parent POM validation failed"
        exit 1
    fi
    
    # Validate each module
    for module in "${MAVEN_MODULES[@]}"; do
        if [[ -d "${module}" ]]; then
            log_info "Validating module: ${module}"
            if validate_module "${module}"; then
                log_success "Module ${module} validation passed"
                ((success_count++))
            else
                log_error "Module ${module} validation failed"
                failed_modules+=("${module}")
            fi
        else
            log_warn "Module directory not found: ${module}"
        fi
    done
    
    # Report results
    log_info "Validation complete:"
    log_info "  Successful modules: ${success_count}"
    log_info "  Failed modules: ${#failed_modules[@]}"
    
    if [[ ${#failed_modules[@]} -gt 0 ]]; then
        log_error "Failed modules:"
        for module in "${failed_modules[@]}"; do
            log_error "  - ${module}"
        done
        exit 1
    else
        log_success "All modules validated successfully!"
    fi
}

# Validate parent POM
validate_parent_pom() {
    log_info "Checking parent POM structure..."
    
    # Check if pom.xml exists
    if [[ ! -f "pom.xml" ]]; then
        log_error "Parent pom.xml not found"
        return 1
    fi
    
    # Validate POM syntax
    if ! mvn help:effective-pom -q > /dev/null 2>&1; then
        log_error "Parent POM has syntax errors"
        return 1
    fi
    
    # Check for required properties
    local required_properties=(
        "java.version"
        "spring-boot.version"
        "maven.compiler.source"
        "maven.compiler.target"
    )
    
    for prop in "${required_properties[@]}"; do
        if ! grep -q "<${prop}>" pom.xml; then
            log_error "Missing required property: ${prop}"
            return 1
        fi
    done
    
    return 0
}

# Validate individual module
validate_module() {
    local module="$1"
    
    cd "${PROJECT_ROOT}/${module}"
    
    # Check if pom.xml exists
    if [[ ! -f "pom.xml" ]]; then
        log_error "Module pom.xml not found: ${module}"
        return 1
    fi
    
    # Check for main application class (for Spring Boot modules)
    if grep -q "spring-boot-starter" pom.xml; then
        if ! find src/main/java -name "*Application.java" | grep -q .; then
            log_error "Spring Boot module missing Application class: ${module}"
            return 1
        fi
    fi
    
    # Validate compilation
    log_info "Compiling ${module}..."
    if ! mvn clean compile --batch-mode --no-transfer-progress -q; then
        log_error "Compilation failed for ${module}"
        return 1
    fi
    
    # Validate test compilation
    log_info "Compiling tests for ${module}..."
    if ! mvn test-compile --batch-mode --no-transfer-progress -q; then
        log_error "Test compilation failed for ${module}"
        return 1
    fi
    
    cd "${PROJECT_ROOT}"
    return 0
}

# Run main function
main "$@"