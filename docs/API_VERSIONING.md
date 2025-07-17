# API Versioning Strategy

## Overview

This document describes the comprehensive API versioning strategy implemented for the MCP system. The versioning system provides multiple strategies for API versioning, backward compatibility, deprecation management, and seamless migration paths.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     Client      │────▶│   API Gateway   │────▶│   Controller    │
│   (Version X)   │     │   (Versioning)  │     │   (Handler)     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
                        ┌──────┴──────┬─────────┬──────────┐
                        │             │         │          │
                  Version Resolver  Metrics  Interceptor  Documentation
```

## Versioning Strategies

### 1. Header-Based Versioning (Default)
```http
GET /api/users
X-API-Version: 2
```

**Advantages:**
- Clean URLs
- Easy to implement
- Doesn't affect caching
- Backward compatible

### 2. Path-Based Versioning
```http
GET /api/v2/users
```

**Advantages:**
- Explicit and visible
- Easy to understand
- Can be bookmarked
- Router-friendly

### 3. Query Parameter Versioning
```http
GET /api/users?version=2
```

**Advantages:**
- Simple implementation
- URL-based specification
- Easy testing

### 4. Content Negotiation
```http
GET /api/users
Accept: application/vnd.mcp.v2+json
```

**Advantages:**
- REST-compliant
- Rich metadata
- Content-type specific

### 5. Multiple Strategy Support
Supports all strategies simultaneously with configurable priority.

## Configuration

### Basic Configuration
```yaml
mcp:
  api:
    versioning:
      enabled: true
      default-version: "1"
      current-version: "2"
      supported-versions: ["1", "2", "3"]
      deprecated-versions: ["1"]
      strategy: HEADER
      version-header: "X-API-Version"
```

### Advanced Configuration
```yaml
mcp:
  api:
    versioning:
      # Validation and behavior
      enforce-version-validation: true
      default-version-behavior: USE_DEFAULT
      
      # Response configuration
      include-version-in-response: true
      include-deprecation-warnings: true
      
      # Metrics and monitoring
      enable-version-metrics: true
      
      # Strategy-specific settings
      version-parameter: "version"
      path-prefix: "/api/v"
      media-type-template: "application/vnd.mcp.v{version}+json"
```

## Implementation

### 1. Controller Annotation
```java
@RestController
@ApiVersioning({"1", "2"})
public class UserController {
    
    @GetMapping("/users")
    @ApiVersioning({"1", "2"})
    public List<User> getUsers() {
        // Version-specific logic
    }
    
    @GetMapping("/users/{id}")
    @ApiVersioning(value = {"1"}, deprecated = true, 
                  deprecationMessage = "Use /users/details/{id} instead")
    public User getUserById(@PathVariable Long id) {
        // Legacy implementation
    }
    
    @GetMapping("/users/details/{id}")
    @ApiVersioning({"2"})
    public UserDetails getUserDetails(@PathVariable Long id) {
        // New implementation
    }
}
```

### 2. Version-Specific Logic
```java
@GetMapping("/users")
@ApiVersioning({"1", "2"})
public ResponseEntity<?> getUsers() {
    String version = versionUtils.getCurrentVersion();
    
    if ("1".equals(version)) {
        return ResponseEntity.ok(userService.getUsersV1());
    } else if ("2".equals(version)) {
        return ResponseEntity.ok(userService.getUsersV2());
    }
    
    throw new UnsupportedApiVersionException("Version not supported");
}
```

### 3. Version Range Support
```java
@GetMapping("/analytics")
@ApiVersioning(min = "2", max = "3")
public AnalyticsData getAnalytics() {
    // Available in versions 2.0 to 3.x
}
```

## Migration Management

### 1. Deprecation Process
```java
@GetMapping("/legacy-endpoint")
@ApiVersioning(
    value = {"1"},
    deprecated = true,
    deprecationMessage = "This endpoint is deprecated. Use /new-endpoint instead.",
    removedInVersion = "3"
)
public LegacyResponse getLegacyData() {
    // Implementation
}
```

### 2. Migration Documentation
The system automatically generates migration guides:
- Breaking changes documentation
- Code examples for each version
- Migration timeline
- Deprecation warnings

### 3. Version Metrics
```java
// Automatic metrics collection
- api.version.usage (by version and source)
- api.version.deprecated.usage
- api.version.response.time
- api.version.errors
- api.version.transitions
```

## Client Integration

### 1. JavaScript/TypeScript
```javascript
// Header-based
const response = await fetch('/api/users', {
    headers: {
        'X-API-Version': '2',
        'Content-Type': 'application/json'
    }
});

// Path-based
const response = await fetch('/api/v2/users');

// Query parameter
const response = await fetch('/api/users?version=2');
```

### 2. Java/Spring
```java
@RestTemplate
public class ApiClient {
    
    @Autowired
    @Qualifier("versionedRestTemplate")
    private RestTemplate restTemplate;
    
    public List<User> getUsers(String version) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Version", version);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        return restTemplate.exchange(
            "/api/users",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<User>>() {}
        ).getBody();
    }
}
```

### 3. cURL Examples
```bash
# Header-based
curl -H "X-API-Version: 2" https://api.example.com/users

# Path-based
curl https://api.example.com/api/v2/users

# Query parameter
curl "https://api.example.com/api/users?version=2"

# Content negotiation
curl -H "Accept: application/vnd.mcp.v2+json" https://api.example.com/users
```

## Version Lifecycle

### 1. Version Introduction
1. Implement new version alongside existing
2. Update documentation
3. Announce new version
4. Provide migration guide

### 2. Version Deprecation
1. Mark version as deprecated
2. Add deprecation warnings
3. Update documentation
4. Notify clients

### 3. Version Removal
1. Remove deprecated version
2. Update supported versions list
3. Return 410 Gone for removed versions
4. Update documentation

## Monitoring and Analytics

### 1. Version Usage Dashboard
```yaml
# Prometheus metrics
api_version_usage_total{version="1",source="header"} 1234
api_version_usage_total{version="2",source="path"} 5678
api_version_deprecated_usage_total{version="1"} 123
api_version_response_time_seconds{version="2"} 0.045
```

### 2. Health Checks
```json
{
  "status": "UP",
  "components": {
    "apiVersioning": {
      "status": "UP",
      "details": {
        "currentVersion": "2",
        "supportedVersions": ["1", "2", "3"],
        "deprecatedVersions": ["1"],
        "strategy": "HEADER"
      }
    }
  }
}
```

## Best Practices

### 1. Version Design
- Use semantic versioning (1.0, 1.1, 2.0)
- Major versions for breaking changes
- Minor versions for new features
- Patch versions for bug fixes

### 2. Backward Compatibility
- Maintain compatibility within major versions
- Deprecate before removing
- Provide migration paths
- Give advance notice

### 3. Documentation
- Document all supported versions
- Provide clear migration guides
- Include version-specific examples
- Maintain changelog

### 4. Testing
- Test all supported versions
- Validate deprecation warnings
- Test version resolution
- Monitor version metrics

## Error Handling

### 1. Unsupported Version
```json
{
  "error": "unsupported_version",
  "message": "API version '5' is not supported. Supported versions: [1, 2, 3]",
  "supported_versions": ["1", "2", "3"],
  "latest_version": "3"
}
```

### 2. Missing Version
```json
{
  "error": "missing_version",
  "message": "API version is required but not specified",
  "default_version": "1",
  "header_name": "X-API-Version"
}
```

### 3. Deprecated Version
```http
HTTP/1.1 200 OK
X-API-Version: 1
X-API-Deprecated: true
X-API-Deprecation-Warning: API version 1 is deprecated. Please upgrade to version 2.
```

## Security Considerations

### 1. Version Validation
- Validate version format
- Sanitize version input
- Rate limit version requests
- Log version usage

### 2. Access Control
- Version-specific permissions
- Deprecation enforcement
- Admin-only version management
- Audit version changes

## Performance Optimization

### 1. Caching
- Cache version resolution
- Version-specific response caching
- CDN configuration for versioned resources

### 2. Routing
- Efficient version matching
- Pre-compiled version patterns
- Fast version lookup

## Future Enhancements

### 1. Dynamic Versioning
- Runtime version configuration
- A/B testing with versions
- Feature flags integration
- Gradual rollout support

### 2. Advanced Features
- Version-specific rate limiting
- Version-based load balancing
- Automatic version negotiation
- Smart version recommendation

## Troubleshooting

### Common Issues

1. **Version not resolved**
   - Check version header/parameter
   - Verify strategy configuration
   - Review interceptor registration

2. **Deprecation warnings not showing**
   - Check `include-deprecation-warnings` setting
   - Verify version is marked as deprecated
   - Check response headers

3. **Version metrics not collected**
   - Ensure `enable-version-metrics` is true
   - Check Micrometer configuration
   - Verify metric registration

### Debug Information

Enable debug logging:
```yaml
logging:
  level:
    com.zamaz.mcp.common.versioning: DEBUG
```

Access version information:
```http
GET /api/versions
GET /api/versions/documentation
GET /api/versions/supported
```