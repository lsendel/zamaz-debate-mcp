#!/bin/bash

# Maven Build Issues Fix Script
# Fixes common Maven build issues in the project

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

# Main function
main() {
    log_info "Starting Maven build issues fix..."
    
    cd "${PROJECT_ROOT}"
    
    # Fix 1: Clean all Maven artifacts
    fix_clean_maven_artifacts
    
    # Fix 2: Validate and fix POM files
    fix_pom_issues
    
    # Fix 3: Fix dependency issues
    fix_dependency_issues
    
    # Fix 4: Fix compilation issues
    fix_compilation_issues
    
    # Fix 5: Validate the fixes
    validate_fixes
    
    log_success "Maven build issues fix completed!"
}

# Fix 1: Clean all Maven artifacts
fix_clean_maven_artifacts() {
    log_info "Cleaning Maven artifacts..."
    
    # Clean all target directories
    find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
    
    # Clean Maven repository cache for this project
    if [[ -d ~/.m2/repository/com/zamaz/mcp ]]; then
        rm -rf ~/.m2/repository/com/zamaz/mcp
        log_info "Cleaned project artifacts from local Maven repository"
    fi
    
    # Clean any corrupted Maven metadata
    find ~/.m2/repository -name "*.lastUpdated" -delete 2>/dev/null || true
    
    log_success "Maven artifacts cleaned"
}

# Fix 2: Validate and fix POM files
fix_pom_issues() {
    log_info "Fixing POM file issues..."
    
    # Check parent POM
    if ! mvn help:effective-pom -q > /dev/null 2>&1; then
        log_error "Parent POM has issues. Please check pom.xml syntax."
        return 1
    fi
    
    # Find all POM files and validate them
    local pom_files
    pom_files=$(find . -name "pom.xml" -not -path "./target/*" -not -path "./.m2/*")
    
    while IFS= read -r pom_file; do
        local dir
        dir=$(dirname "${pom_file}")
        
        log_info "Validating POM: ${pom_file}"
        
        cd "${PROJECT_ROOT}/${dir}"
        
        if ! mvn help:effective-pom -q > /dev/null 2>&1; then
            log_error "POM validation failed: ${pom_file}"
            cd "${PROJECT_ROOT}"
            return 1
        fi
        
        cd "${PROJECT_ROOT}"
    done <<< "${pom_files}"
    
    log_success "POM files validated"
}

# Fix 3: Fix dependency issues
fix_dependency_issues() {
    log_info "Fixing dependency issues..."
    
    # Force update dependencies
    mvn dependency:purge-local-repository --batch-mode --no-transfer-progress -q || true
    
    # Resolve dependencies
    mvn dependency:resolve --batch-mode --no-transfer-progress -q || {
        log_warn "Some dependencies could not be resolved, continuing..."
    }
    
    # Build common modules first (dependency order)
    local core_modules=("mcp-common" "mcp-security")
    
    for module in "${core_modules[@]}"; do
        if [[ -d "${module}" ]]; then
            log_info "Building core module: ${module}"
            cd "${PROJECT_ROOT}/${module}"
            
            if mvn clean install -DskipTests --batch-mode --no-transfer-progress -q; then
                log_success "Core module built: ${module}"
            else
                log_error "Failed to build core module: ${module}"
                cd "${PROJECT_ROOT}"
                return 1
            fi
            
            cd "${PROJECT_ROOT}"
        fi
    done
    
    log_success "Dependency issues fixed"
}

# Fix 4: Fix compilation issues
fix_compilation_issues() {
    log_info "Fixing compilation issues..."
    
    # Set Java version explicitly
    export JAVA_HOME="${JAVA_HOME:-$(readlink -f /usr/bin/java | sed 's:/bin/java::')}"
    export MAVEN_OPTS="-Xmx2048m -XX:MaxMetaspaceSize=512m"
    
    # Try to compile the entire project
    log_info "Attempting full project compilation..."
    
    if mvn clean compile --batch-mode --no-transfer-progress -T 2C; then
        log_success "Full project compilation successful"
    else
        log_warn "Full compilation failed, trying module by module..."
        
        # Try each module individually
        local modules
        modules=$(find . -maxdepth 2 -name "pom.xml" -not -path "./pom.xml" -not -path "./target/*" | sed 's|./||' | sed 's|/pom.xml||')
        
        while IFS= read -r module; do
            if [[ -d "${module}" ]]; then
                log_info "Compiling module: ${module}"
                cd "${PROJECT_ROOT}/${module}"
                
                if mvn clean compile --batch-mode --no-transfer-progress; then
                    log_success "Module compiled: ${module}"
                else
                    log_error "Module compilation failed: ${module}"
                    # Continue with other modules
                fi
                
                cd "${PROJECT_ROOT}"
            fi
        done <<< "${modules}"
    fi
    
    log_success "Compilation issues addressed"
}

# Fix 5: Validate the fixes
validate_fixes() {
    log_info "Validating fixes..."
    
    # Run the validation script if it exists
    if [[ -x "scripts/validation/validate-maven-build.sh" ]]; then
        log_info "Running Maven build validation..."
        if scripts/validation/validate-maven-build.sh; then
            log_success "Maven build validation passed"
        else
            log_error "Maven build validation failed"
            return 1
        fi
    else
        log_warn "Maven build validation script not found, skipping..."
    fi
    
    # Quick smoke test
    log_info "Running quick smoke test..."
    if mvn clean compile -T 2C --batch-mode --no-transfer-progress -q; then
        log_success "Smoke test passed"
    else
        log_error "Smoke test failed"
        return 1
    fi
    
    log_success "All fixes validated"
}

# Run main function
main "$@"