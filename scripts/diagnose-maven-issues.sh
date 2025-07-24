#!/bin/bash

# Maven Issues Diagnostic Script
# Quickly identifies common Maven build issues

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

# Main diagnostic function
main() {
    log_info "Starting Maven build diagnostics..."
    
    cd "${PROJECT_ROOT}"
    
    # Check 1: Environment
    check_environment
    
    # Check 2: Parent POM
    check_parent_pom
    
    # Check 3: Missing modules
    check_missing_modules
    
    # Check 4: Module POMs
    check_module_poms
    
    # Check 5: Main classes
    check_main_classes
    
    # Check 6: Dependencies
    check_dependencies
    
    # Check 7: Quick compilation test
    quick_compilation_test
    
    log_info "Maven diagnostics completed!"
}

# Check 1: Environment
check_environment() {
    log_info "Checking build environment..."
    
    # Java version
    if command -v java &> /dev/null; then
        local java_version
        java_version=$(java -version 2>&1 | head -n1)
        log_info "Java version: ${java_version}"
        
        local java_major
        java_major=$(java -version 2>&1 | head -n1 | grep -oE '[0-9]+' | head -1)
        if [[ "${java_major}" -ge 21 ]]; then
            log_success "Java version is compatible (${java_major})"
        else
            log_error "Java version is too old (${java_major}). Need Java 21+"
        fi
    else
        log_error "Java not found in PATH"
    fi
    
    # Maven version
    if command -v mvn &> /dev/null; then
        local maven_version
        maven_version=$(mvn -version | head -n1)
        log_info "Maven version: ${maven_version}"
        log_success "Maven is available"
    else
        log_error "Maven not found in PATH"
    fi
    
    # JAVA_HOME
    if [[ -n "${JAVA_HOME:-}" ]]; then
        log_info "JAVA_HOME: ${JAVA_HOME}"
    else
        log_warn "JAVA_HOME not set"
    fi
}

# Check 2: Parent POM
check_parent_pom() {
    log_info "Checking parent POM..."
    
    if [[ ! -f "pom.xml" ]]; then
        log_error "Parent pom.xml not found"
        return 1
    fi
    
    # Validate POM syntax
    if mvn help:effective-pom -q > /dev/null 2>&1; then
        log_success "Parent POM syntax is valid"
    else
        log_error "Parent POM has syntax errors"
        mvn help:effective-pom 2>&1 | head -20
    fi
    
    # Check for required properties
    local required_props=("java.version" "spring-boot.version" "maven.compiler.source" "maven.compiler.target")
    for prop in "${required_props[@]}"; do
        if grep -q "<${prop}>" pom.xml; then
            log_success "Found required property: ${prop}"
        else
            log_error "Missing required property: ${prop}"
        fi
    done
}

# Check 3: Missing modules
check_missing_modules() {
    log_info "Checking for missing modules..."
    
    # Extract modules from parent POM
    local declared_modules
    declared_modules=$(grep -o '<module>[^<]*</module>' pom.xml | sed 's/<module>//g' | sed 's/<\/module>//g')
    
    local missing_count=0
    while IFS= read -r module; do
        if [[ -n "${module}" ]]; then
            if [[ -d "${module}" ]]; then
                log_success "Module exists: ${module}"
            else
                log_error "Missing module directory: ${module}"
                ((missing_count++))
            fi
        fi
    done <<< "${declared_modules}"
    
    if [[ ${missing_count} -eq 0 ]]; then
        log_success "All declared modules exist"
    else
        log_error "Found ${missing_count} missing modules"
    fi
}

# Check 4: Module POMs
check_module_poms() {
    log_info "Checking module POMs..."
    
    local pom_files
    pom_files=$(find . -name "pom.xml" -not -path "./pom.xml" -not -path "./target/*")
    
    local error_count=0
    while IFS= read -r pom_file; do
        if [[ -n "${pom_file}" ]]; then
            local dir
            dir=$(dirname "${pom_file}")
            
            log_info "Validating: ${pom_file}"
            cd "${PROJECT_ROOT}/${dir}"
            
            if mvn help:effective-pom -q > /dev/null 2>&1; then
                log_success "Valid POM: ${pom_file}"
            else
                log_error "Invalid POM: ${pom_file}"
                ((error_count++))
            fi
            
            cd "${PROJECT_ROOT}"
        fi
    done <<< "${pom_files}"
    
    if [[ ${error_count} -eq 0 ]]; then
        log_success "All module POMs are valid"
    else
        log_error "Found ${error_count} invalid module POMs"
    fi
}

# Check 5: Main classes
check_main_classes() {
    log_info "Checking for main classes in Spring Boot modules..."
    
    local spring_boot_modules
    spring_boot_modules=$(find . -name "pom.xml" -not -path "./pom.xml" -not -path "./target/*" -exec grep -l "spring-boot-starter" {} \;)
    
    local missing_main_count=0
    while IFS= read -r pom_file; do
        if [[ -n "${pom_file}" ]]; then
            local dir
            dir=$(dirname "${pom_file}")
            local module_name
            module_name=$(basename "${dir}")
            
            log_info "Checking main class for Spring Boot module: ${module_name}"
            
            if find "${dir}/src/main/java" -name "*Application.java" 2>/dev/null | grep -q .; then
                log_success "Main class found for: ${module_name}"
            else
                log_error "Missing main class for Spring Boot module: ${module_name}"
                ((missing_main_count++))
            fi
        fi
    done <<< "${spring_boot_modules}"
    
    if [[ ${missing_main_count} -eq 0 ]]; then
        log_success "All Spring Boot modules have main classes"
    else
        log_error "Found ${missing_main_count} Spring Boot modules without main classes"
    fi
}

# Check 6: Dependencies
check_dependencies() {
    log_info "Checking dependency resolution..."
    
    if mvn dependency:resolve --batch-mode --no-transfer-progress -q > /dev/null 2>&1; then
        log_success "All dependencies can be resolved"
    else
        log_error "Dependency resolution failed"
        log_info "Running dependency:resolve with verbose output..."
        mvn dependency:resolve --batch-mode --no-transfer-progress 2>&1 | tail -20
    fi
}

# Check 7: Quick compilation test
quick_compilation_test() {
    log_info "Running quick compilation test..."
    
    # Try to compile just the parent
    if mvn clean compile -N --batch-mode --no-transfer-progress -q > /dev/null 2>&1; then
        log_success "Parent compilation successful"
    else
        log_error "Parent compilation failed"
    fi
    
    # Try to compile core modules
    local core_modules=("mcp-common" "mcp-security")
    for module in "${core_modules[@]}"; do
        if [[ -d "${module}" ]]; then
            log_info "Testing compilation for core module: ${module}"
            cd "${PROJECT_ROOT}/${module}"
            
            if mvn clean compile --batch-mode --no-transfer-progress -q > /dev/null 2>&1; then
                log_success "Core module compilation successful: ${module}"
            else
                log_error "Core module compilation failed: ${module}"
                # Show last few lines of error
                mvn clean compile --batch-mode --no-transfer-progress 2>&1 | tail -10
            fi
            
            cd "${PROJECT_ROOT}"
        fi
    done
}

# Run main function
main "$@"