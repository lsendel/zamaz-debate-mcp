#!/bin/bash

# Validate Implementation Script
# This script validates that all best practices have been correctly implemented

set -e

echo "🔍 Validating Best Practices Implementation..."
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Function to log test results
log_test() {
    local test_name="$1"
    local status="$2"
    local message="$3"
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓ PASS${NC} $test_name: $message"
        ((PASSED++))
    elif [ "$status" = "FAIL" ]; then
        echo -e "${RED}✗ FAIL${NC} $test_name: $message"
        ((FAILED++))
    else
        echo -e "${YELLOW}⚠ WARN${NC} $test_name: $message"
        ((WARNINGS++))
    fi
}

# Function to check if a file exists
check_file() {
    local file="$1"
    local description="$2"
    
    if [ -f "$file" ]; then
        log_test "File Check" "PASS" "$description exists"
        return 0
    else
        log_test "File Check" "FAIL" "$description missing: $file"
        return 1
    fi
}

# Function to check if a directory exists
check_directory() {
    local dir="$1"
    local description="$2"
    
    if [ -d "$dir" ]; then
        log_test "Directory Check" "PASS" "$description exists"
        return 0
    else
        log_test "Directory Check" "FAIL" "$description missing: $dir"
        return 1
    fi
}

# Function to check for pattern in file
check_pattern() {
    local file="$1"
    local pattern="$2"
    local description="$3"
    local should_exist="$4"
    
    if [ ! -f "$file" ]; then
        log_test "Pattern Check" "FAIL" "$description - file not found: $file"
        return 1
    fi
    
    if grep -q "$pattern" "$file"; then
        if [ "$should_exist" = "true" ]; then
            log_test "Pattern Check" "PASS" "$description found in $file"
            return 0
        else
            log_test "Pattern Check" "FAIL" "$description should not exist in $file"
            return 1
        fi
    else
        if [ "$should_exist" = "true" ]; then
            log_test "Pattern Check" "FAIL" "$description not found in $file"
            return 1
        else
            log_test "Pattern Check" "PASS" "$description correctly removed from $file"
            return 0
        fi
    fi
}

echo "🔧 1. Configuration Management Validation"
echo "----------------------------------------"

# Check .env file exists and has required variables
check_file ".env" "Main environment configuration file"

if [ -f ".env" ]; then
    # Check for required environment variables
    required_vars=(
        "ORGANIZATION_SERVICE_URL"
        "LLM_SERVICE_URL"
        "CONTROLLER_SERVICE_URL"
        "RAG_SERVICE_URL"
        "TEMPLATE_SERVICE_URL"
        "CONTEXT_SERVICE_URL"
        "SECURITY_SERVICE_URL"
        "GATEWAY_SERVICE_URL"
        "WEBSOCKET_URL"
        "CORS_ALLOWED_ORIGINS"
        "MCP_ORGANIZATION_PORT"
        "MCP_LLM_PORT"
        "MCP_CONTROLLER_PORT"
        "MCP_RAG_PORT"
        "MCP_TEMPLATE_PORT"
        "MCP_CONTEXT_PORT"
        "MCP_SECURITY_PORT"
        "MCP_GATEWAY_PORT"
    )
    
    for var in "${required_vars[@]}"; do
        if grep -q "^$var=" ".env"; then
            log_test "Environment Variable" "PASS" "$var is defined"
        else
            log_test "Environment Variable" "FAIL" "$var is missing"
        fi
    done
fi

# Check for hardcoded localhost URLs in Java files
echo ""
echo "🔍 2. Hardcoded Values Validation"
echo "--------------------------------"

java_files_with_localhost=$(find . -name "*.java" -type f -exec grep -l "localhost:[0-9]" {} \; 2>/dev/null || true)
if [ -z "$java_files_with_localhost" ]; then
    log_test "Hardcoded URLs" "PASS" "No hardcoded localhost URLs found in Java files"
else
    log_test "Hardcoded URLs" "FAIL" "Found hardcoded localhost URLs in: $java_files_with_localhost"
fi

# Check for System.out.println usage
system_out_files=$(find . -name "*.java" -type f -exec grep -l "System\.out\.println\|System\.err\.println" {} \; 2>/dev/null | grep -v "/test/" | grep -v "LintingCLI.java" || true)
if [ -z "$system_out_files" ]; then
    log_test "System.out.println" "PASS" "No System.out.println found in production code"
else
    log_test "System.out.println" "FAIL" "Found System.out.println in: $system_out_files"
fi

echo ""
echo "📊 3. Exception Handling Validation"
echo "-----------------------------------"

# Check for StandardGlobalExceptionHandler
check_file "mcp-common/src/main/java/com/zamaz/mcp/common/exception/StandardGlobalExceptionHandler.java" "Standard Global Exception Handler"

# Check for ErrorCodes
check_file "mcp-common/src/main/java/com/zamaz/mcp/common/exception/ErrorCodes.java" "Error Codes"

# Check for ExceptionFactory
check_file "mcp-common/src/main/java/com/zamaz/mcp/common/exception/ExceptionFactory.java" "Exception Factory"

# Check for spring.factories
check_file "mcp-common/src/main/resources/META-INF/spring.factories" "Spring Boot Auto-configuration"

echo ""
echo "🔧 4. Shared Libraries Validation"
echo "--------------------------------"

# Check for shared pattern libraries
check_file "mcp-common/src/main/java/com/zamaz/mcp/common/patterns/BaseController.java" "Base Controller"
check_file "mcp-common/src/main/java/com/zamaz/mcp/common/patterns/BaseService.java" "Base Service"
check_file "mcp-common/src/main/java/com/zamaz/mcp/common/patterns/BaseRepository.java" "Base Repository"
check_file "mcp-common/src/main/java/com/zamaz/mcp/common/patterns/BaseEntity.java" "Base Entity"
check_file "mcp-common/src/main/java/com/zamaz/mcp/common/patterns/ValidationUtils.java" "Validation Utils"

echo ""
echo "🎯 5. API Standardization Validation"
echo "-----------------------------------"

# Check for API versioning
check_file "mcp-common/src/main/java/com/zamaz/mcp/common/api/ApiVersioning.java" "API Versioning"
check_file "mcp-common/src/main/java/com/zamaz/mcp/common/api/OpenApiConfig.java" "OpenAPI Configuration"
check_file "mcp-common/src/main/java/com/zamaz/mcp/common/api/StandardApiResponse.java" "Standard API Response"

# Check for updated debateClient.ts
check_pattern "debate-ui/src/api/debateClient.ts" "/api/v1" "API v1 versioning in debate client" "true"

echo ""
echo "🤖 6. Automation Scripts Validation"
echo "----------------------------------"

# Check for automation scripts
scripts=(
    "scripts/fix-hardcoded-values.sh"
    "scripts/validate-configuration.sh"
    "scripts/setup-environment.sh"
    "scripts/standardize-exception-handling.sh"
    "scripts/standardize-api-documentation.sh"
)

for script in "${scripts[@]}"; do
    if [ -f "$script" ]; then
        if [ -x "$script" ]; then
            log_test "Automation Script" "PASS" "$script exists and is executable"
        else
            log_test "Automation Script" "WARN" "$script exists but is not executable"
        fi
    else
        log_test "Automation Script" "FAIL" "$script is missing"
    fi
done

echo ""
echo "📖 7. Documentation Validation"
echo "-----------------------------"

# Check for documentation files
docs=(
    "IMPLEMENTATION_SUMMARY.md"
    "BEST_PRACTICES_REPORT.md"
    "docs/EXCEPTION_HANDLING_GUIDE.md"
    "docs/API_DOCUMENTATION_GUIDE.md"
)

for doc in "${docs[@]}"; do
    check_file "$doc" "Documentation: $(basename "$doc")"
done

echo ""
echo "🏗️ 8. Service Configuration Validation"
echo "--------------------------------------"

# Check services for proper configuration
services=(
    "mcp-organization"
    "mcp-llm"
    "mcp-controller"
    "mcp-rag"
    "mcp-context"
    "mcp-template"
    "mcp-debate-engine"
    "mcp-gateway"
)

for service in "${services[@]}"; do
    if [ -d "$service" ]; then
        log_test "Service Directory" "PASS" "$service directory exists"
        
        # Check for application.yml
        if [ -f "$service/src/main/resources/application.yml" ]; then
            log_test "Service Config" "PASS" "$service has application.yml"
        else
            log_test "Service Config" "WARN" "$service missing application.yml"
        fi
        
        # Check for pom.xml
        if [ -f "$service/pom.xml" ]; then
            log_test "Service Build" "PASS" "$service has pom.xml"
        else
            log_test "Service Build" "WARN" "$service missing pom.xml"
        fi
    else
        log_test "Service Directory" "FAIL" "$service directory missing"
    fi
done

echo ""
echo "🧪 9. Frontend Configuration Validation"
echo "--------------------------------------"

# Check frontend configuration
check_file "debate-ui/.env" "Frontend environment configuration"
check_file "debate-ui/vite.config.js" "Vite configuration"

# Check for proper API client structure
check_pattern "debate-ui/src/api/debateClient.ts" "BaseApiClient" "BaseApiClient usage" "true"

echo ""
echo "🔧 10. Build and Dependency Validation"
echo "-------------------------------------"

# Check for Maven wrapper
if [ -f "mvnw" ]; then
    log_test "Build Tool" "PASS" "Maven wrapper exists"
else
    log_test "Build Tool" "WARN" "Maven wrapper missing"
fi

# Check for package.json in UI
if [ -f "debate-ui/package.json" ]; then
    log_test "Frontend Build" "PASS" "Frontend package.json exists"
else
    log_test "Frontend Build" "FAIL" "Frontend package.json missing"
fi

# Check for Makefile
if [ -f "Makefile" ]; then
    log_test "Build Automation" "PASS" "Makefile exists"
else
    log_test "Build Automation" "WARN" "Makefile missing"
fi

echo ""
echo "======================================================"
echo "📊 VALIDATION SUMMARY"
echo "======================================================"
echo -e "${GREEN}✓ Passed: $PASSED${NC}"
echo -e "${RED}✗ Failed: $FAILED${NC}"
echo -e "${YELLOW}⚠ Warnings: $WARNINGS${NC}"

total=$((PASSED + FAILED + WARNINGS))
if [ $total -gt 0 ]; then
    pass_rate=$((PASSED * 100 / total))
    echo -e "📈 Pass Rate: ${GREEN}$pass_rate%${NC}"
else
    echo -e "📈 Pass Rate: ${GREEN}100%${NC}"
fi

echo ""
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL CRITICAL TESTS PASSED!${NC}"
    echo -e "${GREEN}✅ Best practices implementation is valid${NC}"
    
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠️  $WARNINGS warnings found - review recommended${NC}"
    fi
    
    echo ""
    echo "🚀 Next Steps:"
    echo "1. Run services: make dev"
    echo "2. Test API endpoints: curl http://localhost:8080/actuator/health"
    echo "3. Check API documentation: http://localhost:8080/swagger-ui.html"
    echo "4. Validate configuration: ./scripts/validate-configuration.sh"
    
    exit 0
else
    echo -e "${RED}❌ $FAILED CRITICAL TESTS FAILED!${NC}"
    echo -e "${RED}🔧 Implementation needs attention${NC}"
    
    echo ""
    echo "🔍 Recommended Actions:"
    echo "1. Review failed tests above"
    echo "2. Fix missing files or configurations"
    echo "3. Re-run validation: ./scripts/validate-implementation.sh"
    echo "4. Check implementation guides in docs/"
    
    exit 1
fi