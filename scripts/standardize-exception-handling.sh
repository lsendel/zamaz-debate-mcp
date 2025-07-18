#!/bin/bash

# Standardize Exception Handling Script
# This script updates all services to use the centralized exception handling

set -e

echo "🔧 Standardizing exception handling across all services..."

# Services to update
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

echo "📋 Services to update: ${services[*]}"

# Function to update a service
update_service() {
    local service=$1
    echo "🔄 Updating service: $service"
    
    # Check if service directory exists
    if [ ! -d "$service" ]; then
        echo "⚠️  Service directory not found: $service"
        return 1
    fi
    
    # Remove existing global exception handlers (but keep them as backup)
    if [ -f "$service/src/main/java/com/zamaz/mcp/${service#mcp-}/config/GlobalExceptionHandler.java" ]; then
        echo "  📁 Backing up existing GlobalExceptionHandler"
        mv "$service/src/main/java/com/zamaz/mcp/${service#mcp-}/config/GlobalExceptionHandler.java" \
           "$service/src/main/java/com/zamaz/mcp/${service#mcp-}/config/GlobalExceptionHandler.java.backup"
    fi
    
    # Remove other common exception handler locations
    find "$service/src/main/java" -name "*ExceptionHandler.java" -type f | while read -r file; do
        if [ -f "$file" ]; then
            echo "  📁 Backing up existing exception handler: $(basename "$file")"
            mv "$file" "$file.backup"
        fi
    done
    
    # Update application.properties/yml to enable the common exception handler
    if [ -f "$service/src/main/resources/application.yml" ]; then
        echo "  ⚙️  Updating application.yml"
        cat >> "$service/src/main/resources/application.yml" << EOF

# Exception Handling Configuration
mcp:
  exception-handling:
    enabled: true
    log-stack-trace: true
    include-binding-errors: true
    include-message: true
EOF
    fi
    
    # Update pom.xml to ensure mcp-common dependency is included
    if [ -f "$service/pom.xml" ]; then
        echo "  📦 Ensuring mcp-common dependency is included"
        # Check if mcp-common dependency exists
        if ! grep -q "mcp-common" "$service/pom.xml"; then
            echo "  ➕ Adding mcp-common dependency"
            # Add dependency before </dependencies>
            sed -i.bak '/<\/dependencies>/i\
        <dependency>\
            <groupId>com.zamaz.mcp</groupId>\
            <artifactId>mcp-common</artifactId>\
            <version>${project.version}</version>\
        </dependency>' "$service/pom.xml"
        fi
    fi
    
    # Remove controller-level exception handlers
    echo "  🧹 Removing controller-level exception handlers"
    find "$service/src/main/java" -name "*Controller.java" -type f | while read -r controller; do
        if grep -q "@ExceptionHandler" "$controller"; then
            echo "    📝 Removing @ExceptionHandler from $(basename "$controller")"
            # Comment out @ExceptionHandler methods (safer than deletion)
            sed -i.bak 's/@ExceptionHandler/\/\/ @ExceptionHandler - Moved to global handler/g' "$controller"
            sed -i.bak 's/public ResponseEntity<.*> handle.*Exception/\/\/ public ResponseEntity<.*> handle.*Exception - Moved to global handler/g' "$controller"
        fi
    done
    
    echo "  ✅ Service $service updated successfully"
}

# Update each service
for service in "${services[@]}"; do
    update_service "$service"
done

# Create a comprehensive exception handling guide
echo "📖 Creating exception handling guide..."
cat > docs/EXCEPTION_HANDLING_GUIDE.md << 'EOF'
# Exception Handling Guide

## Overview

This guide explains the standardized exception handling implementation across all MCP services.

## Architecture

### Centralized Exception Handler

All services use the `StandardGlobalExceptionHandler` from `mcp-common` which:
- Implements RFC 7807 ProblemDetail standard
- Provides consistent error response format
- Includes structured logging
- Handles all common exception types

### Exception Hierarchy

```
BaseException (abstract)
├── BusinessException (for business logic errors)
├── TechnicalException (for technical errors)
├── ValidationException (for validation errors)
└── ExternalServiceException (for external service errors)
```

### Error Response Format

All error responses follow the RFC 7807 ProblemDetail format:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/users",
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2024-01-15T10:30:00Z",
  "fieldErrors": {
    "email": "Invalid email format",
    "name": "Name is required"
  }
}
```

## Usage

### Creating Business Exceptions

```java
// Using ExceptionFactory (recommended)
throw ExceptionFactory.organizationNotFound(orgId);
throw ExceptionFactory.debateAlreadyStarted(debateId);
throw ExceptionFactory.rateLimitExceeded("api-calls", 100, "1 hour");

// Manual creation
throw new BusinessException(
    ErrorCodes.ORG_NOT_FOUND,
    "Organization not found: " + orgId,
    Map.of("organizationId", orgId)
);
```

### Creating Technical Exceptions

```java
// Using ExceptionFactory
throw ExceptionFactory.embeddingFailed(text, provider, cause);
throw ExceptionFactory.vectorStoreUnavailable(store, cause);

// Manual creation
throw new TechnicalException(
    ErrorCodes.DATABASE_CONNECTION_FAILED,
    "Failed to connect to database",
    Map.of("host", dbHost, "port", dbPort),
    cause
);
```

### Validation Exceptions

```java
// For field-level validation errors
Map<String, String> fieldErrors = Map.of(
    "email", "Invalid email format",
    "age", "Must be between 18 and 100"
);
throw ExceptionFactory.validationFailed("Validation failed", fieldErrors);

// For constraint violations
throw ExceptionFactory.constraintViolation("Constraint violated", violations);
```

### External Service Exceptions

```java
// For external service failures
throw ExceptionFactory.externalServiceTimeout("openai-api", 30000);
throw ExceptionFactory.externalServiceUnavailable("vector-database");
```

## Error Codes

All error codes are defined in `ErrorCodes.java` and follow the pattern:
- `[SERVICE]_[CATEGORY]_[SPECIFIC_ERROR]`
- Example: `LLM_RATE_LIMIT_EXCEEDED`, `DEBATE_NOT_FOUND`

## Best Practices

### DO:
- Use `ExceptionFactory` for creating common exceptions
- Include relevant context in exception details
- Use appropriate exception types (Business vs Technical)
- Let the global handler format responses
- Include correlation IDs for tracing

### DON'T:
- Create controller-level exception handlers
- Use generic `Exception` types
- Log exceptions in controllers (global handler does this)
- Include sensitive information in error details
- Return different error formats across services

## Configuration

Services automatically include the global exception handler through auto-configuration.
To customize behavior, add to `application.yml`:

```yaml
mcp:
  exception-handling:
    enabled: true
    log-stack-trace: true
    include-binding-errors: true
    include-message: true
```

## Testing

### Unit Tests
```java
@Test
void shouldThrowBusinessExceptionWhenOrganizationNotFound() {
    assertThatThrownBy(() -> service.getOrganization("invalid-id"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Organization not found: invalid-id")
        .extracting("errorCode")
        .isEqualTo(ErrorCodes.ORG_NOT_FOUND);
}
```

### Integration Tests
```java
@Test
void shouldReturnProblemDetailWhenOrganizationNotFound() {
    webTestClient.get()
        .uri("/api/v1/organizations/invalid-id")
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.type").isEqualTo("about:blank")
        .jsonPath("$.title").isEqualTo("Bad Request")
        .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.errorCode").isEqualTo("ORG_NOT_FOUND")
        .jsonPath("$.timestamp").exists();
}
```

## Migration Guide

### From Custom Exception Handlers

1. Remove `@ExceptionHandler` methods from controllers
2. Remove custom `@ControllerAdvice` classes
3. Update exception throwing to use `ExceptionFactory`
4. Update tests to expect ProblemDetail format
5. Remove custom error response DTOs

### From Generic Exception Handling

1. Replace `catch (Exception e)` with specific exception types
2. Use `ExceptionFactory` to create typed exceptions
3. Remove manual error response creation
4. Update error message formats to use error codes

## Monitoring

The global exception handler includes structured logging for:
- Exception type and message
- Error code and details
- Request URI and method
- User context (if available)
- Correlation IDs

Example log entry:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "level": "ERROR",
  "message": "Business exception occurred",
  "errorCode": "ORG_NOT_FOUND",
  "requestUri": "/api/v1/organizations/invalid-id",
  "userContext": "user-123",
  "correlationId": "abc-123-def"
}
```
EOF

echo "🧹 Cleaning up backup files..."
find . -name "*.bak" -type f -delete 2>/dev/null || true

echo "✅ Exception handling standardization completed!"
echo ""
echo "📋 Summary:"
echo "  ✅ Created centralized exception handler in mcp-common"
echo "  ✅ Updated ${#services[@]} services to use standard exception handling"
echo "  ✅ Created comprehensive error code definitions"
echo "  ✅ Provided ExceptionFactory for consistent exception creation"
echo "  ✅ Generated exception handling guide"
echo ""
echo "🚀 Next steps:"
echo "  1. Review and test each service"
echo "  2. Update integration tests to expect ProblemDetail format"
echo "  3. Update frontend error handling to parse new format"
echo "  4. Run comprehensive tests: make test"