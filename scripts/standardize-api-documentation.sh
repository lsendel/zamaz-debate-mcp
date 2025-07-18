#!/bin/bash

# Standardize API Documentation Script
# This script updates all services to use consistent API versioning and documentation

set -e

echo "🔧 Standardizing API documentation and versioning across all services..."

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
    
    # Add SpringDoc OpenAPI dependency if not present
    if [ -f "$service/pom.xml" ]; then
        echo "  📦 Adding SpringDoc OpenAPI dependency"
        if ! grep -q "springdoc-openapi-starter-webmvc-ui" "$service/pom.xml"; then
            # Add dependency before </dependencies>
            sed -i.bak '/<\/dependencies>/i\
        <dependency>\
            <groupId>org.springdoc</groupId>\
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>\
            <version>2.2.0</version>\
        </dependency>' "$service/pom.xml"
        fi
    fi
    
    # Update application.yml with OpenAPI configuration
    if [ -f "$service/src/main/resources/application.yml" ]; then
        echo "  ⚙️  Updating application.yml with OpenAPI configuration"
        cat >> "$service/src/main/resources/application.yml" << EOF

# OpenAPI Configuration
springdoc:
  api-docs:
    enabled: true
    path: /api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
    try-it-out-enabled: true
  packages-to-scan: com.zamaz.mcp.${service#mcp-}
  paths-to-match: /api/**
  show-actuator: false

# API Configuration
api:
  version: 1.0.0
  description: "${service^} Service API"
  title: "${service^} API"
  contact:
    name: MCP Development Team
    email: dev@mcp.com
    url: https://mcp.com/contact
  license:
    name: MIT License
    url: https://opensource.org/licenses/MIT
EOF
    fi
    
    # Update controllers to use API versioning annotations
    echo "  🎯 Updating controllers with API versioning"
    find "$service/src/main/java" -name "*Controller.java" -type f | while read -r controller; do
        if grep -q "@RestController" "$controller"; then
            echo "    📝 Updating $(basename "$controller")"
            
            # Add imports if not present
            if ! grep -q "import com.zamaz.mcp.common.api.ApiVersioning" "$controller"; then
                sed -i.bak '/^import/a\
import com.zamaz.mcp.common.api.ApiVersioning;\
import com.zamaz.mcp.common.api.StandardApiResponse;\
import io.swagger.v3.oas.annotations.Operation;\
import io.swagger.v3.oas.annotations.Parameter;\
import io.swagger.v3.oas.annotations.media.Content;\
import io.swagger.v3.oas.annotations.media.Schema;\
import io.swagger.v3.oas.annotations.responses.ApiResponse;\
import io.swagger.v3.oas.annotations.responses.ApiResponses;\
import io.swagger.v3.oas.annotations.tags.Tag;' "$controller"
            fi
            
            # Add @V1 annotation to controllers that don't have API versioning
            if ! grep -q "@V1\|@V2\|@V3" "$controller"; then
                sed -i.bak '/^@RestController/i\
@ApiVersioning.V1\
@Tag(name = "'"$(basename "$controller" .java | sed 's/Controller//')"'", description = "'"$(basename "$controller" .java | sed 's/Controller//')"' management operations")' "$controller"
            fi
            
            # Add OpenAPI annotations to methods
            sed -i.bak '/public ResponseEntity/i\
    @Operation(summary = "Operation summary", description = "Operation description")\
    @ApiResponses(value = {\
        @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = StandardApiResponse.class))),\
        @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = StandardApiResponse.class))),\
        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = StandardApiResponse.class)))\
    })' "$controller"
        fi
    done
    
    # Create API documentation configuration
    echo "  📖 Creating API documentation configuration"
    config_dir="$service/src/main/java/com/zamaz/mcp/${service#mcp-}/config"
    mkdir -p "$config_dir"
    
    cat > "$config_dir/ApiDocumentationConfig.java" << EOF
package com.zamaz.mcp.${service#mcp-}.config;

import com.zamaz.mcp.common.api.OpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * API documentation configuration for ${service} service.
 */
@Configuration
@Import(OpenApiConfig.class)
public class ApiDocumentationConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("${service^} Service API")
                .description("API documentation for ${service} service")
                .version("1.0.0"));
    }
}
EOF
    
    echo "  ✅ Service $service updated successfully"
}

# Update each service
for service in "${services[@]}"; do
    update_service "$service"
done

# Create comprehensive API documentation guide
echo "📖 Creating API documentation guide..."
cat > docs/API_DOCUMENTATION_GUIDE.md << 'EOF'
# API Documentation Guide

## Overview

This guide explains the standardized API documentation and versioning implementation across all MCP services.

## API Versioning

### Version Strategy

All APIs follow semantic versioning with URL-based versioning:
- `/api/v1/` - Version 1 (stable)
- `/api/v2/` - Version 2 (stable)
- `/api/v3/` - Version 3 (future)

### Annotations

Use the standardized versioning annotations:

```java
@ApiVersioning.V1
@RestController
@Tag(name = "Organizations", description = "Organization management operations")
public class OrganizationController {
    
    @ApiVersioning.V2
    @GetMapping("/organizations")
    public ResponseEntity<List<Organization>> getOrganizations() {
        // Version 2 implementation
    }
    
    @ApiVersioning.Deprecated(
        removedInVersion = "v3",
        useInstead = "/api/v2/organizations",
        message = "Use v2 endpoint for better performance"
    )
    @GetMapping("/organizations/legacy")
    public ResponseEntity<List<Organization>> getLegacyOrganizations() {
        // Deprecated implementation
    }
}
```

## OpenAPI Documentation

### Configuration

All services automatically include OpenAPI documentation through:
- SpringDoc OpenAPI integration
- Swagger UI at `/swagger-ui.html`
- API docs at `/api-docs`

### Annotations

Use comprehensive OpenAPI annotations:

```java
@Operation(
    summary = "Get organization by ID",
    description = "Retrieves a specific organization by its unique identifier"
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "Organization found",
        content = @Content(schema = @Schema(implementation = StandardApiResponse.class))
    ),
    @ApiResponse(
        responseCode = "404",
        description = "Organization not found",
        content = @Content(schema = @Schema(implementation = StandardApiResponse.class))
    )
})
@GetMapping("/organizations/{id}")
public ResponseEntity<StandardApiResponse<Organization>> getOrganization(
    @Parameter(description = "Organization ID", required = true)
    @PathVariable String id
) {
    // Implementation
}
```

## Standard Response Format

All APIs use the `StandardApiResponse` wrapper:

```java
// Success response
StandardApiResponse.success(data);

// Success with message
StandardApiResponse.success(data, "Operation completed successfully");

// Success with pagination
StandardApiResponse.success(data, paginationInfo);

// Error response
StandardApiResponse.error("Validation failed", validationErrors);

// Response with warnings
StandardApiResponse.withWarnings(data, warnings);
```

### Response Structure

```json
{
  "data": { /* Response data */ },
  "message": "Operation completed successfully",
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "abc-123-def",
  "apiVersion": "1.0.0",
  "serviceName": "mcp-organization",
  "metadata": {
    "processingTime": "125ms",
    "cacheHit": true
  },
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false,
    "isFirst": true,
    "isLast": false
  },
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format",
      "code": "INVALID_FORMAT",
      "rejectedValue": "invalid-email"
    }
  ],
  "warnings": [
    "Feature is in beta"
  ]
}
```

## Controller Best Practices

### Base Controller

Extend `BaseController` for common functionality:

```java
@ApiVersioning.V1
@RestController
@Tag(name = "Organizations", description = "Organization management")
public class OrganizationController extends BaseController {
    
    public OrganizationController(StructuredLoggerFactory loggerFactory) {
        super(loggerFactory);
    }
    
    @Operation(summary = "Create organization")
    @PostMapping("/organizations")
    public ResponseEntity<StandardApiResponse<Organization>> createOrganization(
        @RequestBody @Valid CreateOrganizationRequest request
    ) {
        logAction("createOrganization", "name", request.getName());
        
        Organization org = organizationService.create(request);
        
        return created(StandardApiResponse.success(org, "Organization created successfully"));
    }
}
```

### Validation and Error Handling

```java
@PostMapping("/organizations")
public ResponseEntity<StandardApiResponse<Organization>> createOrganization(
    @RequestBody @Valid CreateOrganizationRequest request
) {
    // Validation is handled by global exception handler
    // Business logic validation
    ValidationUtils.validateMultiple(
        new ValidationUtils.ValidationBuilder()
            .requireNonEmpty(request.getName(), "name")
            .validateEmail(request.getContactEmail(), "contactEmail")
            .custom("domain", "Domain already exists", !domainExists(request.getDomain()))
    );
    
    // Service call
    Organization org = organizationService.create(request);
    
    return created(StandardApiResponse.success(org));
}
```

## Testing API Documentation

### Unit Tests

```java
@Test
void shouldGenerateOpenApiDocumentation() {
    // Test that OpenAPI docs are generated correctly
    webTestClient.get()
        .uri("/api-docs")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.openapi").isEqualTo("3.0.1")
        .jsonPath("$.info.title").isEqualTo("Organization Service API")
        .jsonPath("$.info.version").isEqualTo("1.0.0");
}
```

### Integration Tests

```java
@Test
void shouldReturnStandardApiResponse() {
    webTestClient.get()
        .uri("/api/v1/organizations")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.data").isArray()
        .jsonPath("$.message").isEqualTo("Success")
        .jsonPath("$.timestamp").exists()
        .jsonPath("$.apiVersion").isEqualTo("1.0.0")
        .jsonPath("$.serviceName").isEqualTo("mcp-organization");
}
```

## Migration Guide

### From Existing APIs

1. **Add Versioning Annotations**:
   ```java
   @ApiVersioning.V1  // Add to controllers
   @RestController
   public class ExistingController {
   ```

2. **Update Response Format**:
   ```java
   // Old
   return ResponseEntity.ok(data);
   
   // New
   return ok(StandardApiResponse.success(data));
   ```

3. **Add OpenAPI Annotations**:
   ```java
   @Operation(summary = "Description")
   @ApiResponses(...)
   @GetMapping("/endpoint")
   public ResponseEntity<StandardApiResponse<Data>> getEndpoint() {
   ```

4. **Update Dependencies**:
   ```xml
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
       <version>2.2.0</version>
   </dependency>
   ```

## Service-Specific Documentation

Each service provides documentation at:
- Swagger UI: `http://localhost:{port}/swagger-ui.html`
- OpenAPI JSON: `http://localhost:{port}/api-docs`
- OpenAPI YAML: `http://localhost:{port}/api-docs.yaml`

## Best Practices

### DO:
- Use semantic versioning for APIs
- Include comprehensive OpenAPI annotations
- Use StandardApiResponse for all responses
- Document all parameters and responses
- Include examples in documentation
- Use meaningful operation summaries

### DON'T:
- Mix versioning strategies
- Return different response formats
- Skip error response documentation
- Use generic descriptions
- Forget to update documentation when changing APIs

## Monitoring and Analytics

API documentation includes:
- Request/response schemas
- Error codes and messages
- Authentication requirements
- Rate limiting information
- Deprecation notices
- Example requests and responses

## Automation

The standardization includes:
- Automatic OpenAPI generation
- Consistent response wrapping
- Validation error formatting
- Request/response logging
- Performance metrics collection
EOF

echo "🧹 Cleaning up backup files..."
find . -name "*.bak" -type f -delete 2>/dev/null || true

echo "✅ API documentation and versioning standardization completed!"
echo ""
echo "📋 Summary:"
echo "  ✅ Added SpringDoc OpenAPI to all services"
echo "  ✅ Created standardized API versioning annotations"
echo "  ✅ Implemented StandardApiResponse wrapper"
echo "  ✅ Added OpenAPI configuration to all services"
echo "  ✅ Updated controllers with proper annotations"
echo "  ✅ Generated comprehensive API documentation guide"
echo ""
echo "🚀 Access API documentation:"
echo "  📖 Swagger UI: http://localhost:{port}/swagger-ui.html"
echo "  📄 OpenAPI JSON: http://localhost:{port}/api-docs"
echo "  📄 OpenAPI YAML: http://localhost:{port}/api-docs.yaml"
echo ""
echo "🔗 Next steps:"
echo "  1. Review and customize API descriptions"
echo "  2. Test API documentation endpoints"
echo "  3. Update integration tests to use StandardApiResponse"
echo "  4. Consider API versioning strategy for breaking changes"