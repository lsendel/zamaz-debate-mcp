# MCP-Common Library Documentation

The MCP-Common library provides shared components, utilities, and models used across all services in the Zamaz Debate MCP system.

## Overview

The MCP-Common library contains common code that is reused across multiple microservices in the MCP system. It includes shared data models, utility classes, exception handling, configuration, and other cross-cutting concerns. Using this shared library ensures consistency across services and reduces code duplication.

## Features

- **Shared Data Models**: Common domain models used across services
- **Exception Handling**: Standardized exception classes and handlers
- **Utility Classes**: Helper methods for common operations
- **Configuration**: Shared configuration properties
- **Security**: Common security utilities and models
- **Validation**: Reusable validation logic
- **Logging**: Consistent logging framework
- **API Responses**: Standardized API response formats
- **MCP Protocol**: Common MCP protocol implementation

## Components

### Shared Data Models

The library includes common data models used across services:

```java
package com.zamaz.mcp.common.model;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class BaseEntity {
    private UUID id;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
```

### Exception Handling

Standardized exception classes and handlers:

```java
package com.zamaz.mcp.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
    private final Object details;
    
    public ApiException(String message, HttpStatus status, String errorCode, Object details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details;
    }
    
    // Factory methods for common exceptions
    public static ApiException notFound(String message) {
        return new ApiException(message, HttpStatus.NOT_FOUND, "NOT_FOUND", null);
    }
    
    public static ApiException badRequest(String message) {
        return new ApiException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST", null);
    }
    
    public static ApiException unauthorized(String message) {
        return new ApiException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", null);
    }
    
    public static ApiException forbidden(String message) {
        return new ApiException(message, HttpStatus.FORBIDDEN, "FORBIDDEN", null);
    }
}
```

### Utility Classes

Helper methods for common operations:

```java
package com.zamaz.mcp.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class ValidationUtils {
    public static void requireNonNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }
    
    public static void requireNonEmpty(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    public static void requireNonEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    public static void requireNonEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
    
    public static UUID parseUUID(String id, String message) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(message);
        }
    }
}
```

### API Responses

Standardized API response formats:

```java
package com.zamaz.mcp.common.api;

import lombok.Data;
import java.time.Instant;

@Data
public class ApiResponse<T> {
    private T data;
    private ApiError error;
    private Instant timestamp;
    private String requestId;
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setData(data);
        response.setTimestamp(Instant.now());
        return response;
    }
    
    public static <T> ApiResponse<T> error(ApiError error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setError(error);
        response.setTimestamp(Instant.now());
        return response;
    }
}

@Data
public class ApiError {
    private String code;
    private String message;
    private Object details;
    
    public static ApiError of(String code, String message) {
        ApiError error = new ApiError();
        error.setCode(code);
        error.setMessage(message);
        return error;
    }
    
    public static ApiError of(String code, String message, Object details) {
        ApiError error = new ApiError();
        error.setCode(code);
        error.setMessage(message);
        error.setDetails(details);
        return error;
    }
}
```

### MCP Protocol

Common MCP protocol implementation:

```java
package com.zamaz.mcp.common.mcp;

import lombok.Data;
import java.util.Map;

@Data
public class McpToolRequest {
    private String toolName;
    private Map<String, Object> parameters;
}

@Data
public class McpToolResponse {
    private Object result;
    private McpError error;
    
    public static McpToolResponse success(Object result) {
        McpToolResponse response = new McpToolResponse();
        response.setResult(result);
        return response;
    }
    
    public static McpToolResponse error(String code, String message) {
        McpToolResponse response = new McpToolResponse();
        McpError error = new McpError();
        error.setCode(code);
        error.setMessage(message);
        response.setError(error);
        return response;
    }
}

@Data
public class McpError {
    private String code;
    private String message;
    private Object details;
}
```

## Usage

### Maven Dependency

To use the MCP-Common library in a service, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.zamaz.mcp</groupId>
    <artifactId>mcp-common</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Using Shared Models

```java
import com.zamaz.mcp.common.model.BaseEntity;

public class Debate extends BaseEntity {
    private String name;
    private String format;
    private List<Participant> participants;
    // Additional fields
}
```

### Using Exception Handling

```java
import com.zamaz.mcp.common.exception.ApiException;
import com.zamaz.mcp.common.exception.GlobalExceptionHandler;

@RestController
public class DebateController {
    @GetMapping("/{id}")
    public Debate getDebate(@PathVariable String id) {
        Debate debate = debateService.findById(id);
        if (debate == null) {
            throw ApiException.notFound("Debate not found with id: " + id);
        }
        return debate;
    }
}
```

### Using Utility Classes

```java
import com.zamaz.mcp.common.util.ValidationUtils;

public class DebateService {
    public Debate createDebate(Debate debate) {
        ValidationUtils.requireNonNull(debate, "Debate cannot be null");
        ValidationUtils.requireNonEmpty(debate.getName(), "Debate name cannot be empty");
        ValidationUtils.requireNonEmpty(debate.getParticipants(), "Debate must have participants");
        
        // Create debate
        return debateRepository.save(debate);
    }
}
```

### Using API Responses

```java
import com.zamaz.mcp.common.api.ApiResponse;

@RestController
public class DebateController {
    @GetMapping("/{id}")
    public ApiResponse<Debate> getDebate(@PathVariable String id) {
        try {
            Debate debate = debateService.findById(id);
            return ApiResponse.success(debate);
        } catch (Exception e) {
            return ApiResponse.error(ApiError.of("DEBATE_NOT_FOUND", e.getMessage()));
        }
    }
}
```

## Key Components

### Organization Context

Utilities for handling organization context:

```java
package com.zamaz.mcp.common.context;

import lombok.Data;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class OrganizationContext {
    private static final String ORGANIZATION_ID_HEADER = "X-Organization-ID";
    private static final ThreadLocal<String> currentOrganizationId = new ThreadLocal<>();
    
    public static String getCurrentOrganizationId() {
        String organizationId = currentOrganizationId.get();
        if (organizationId != null) {
            return organizationId;
        }
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader(ORGANIZATION_ID_HEADER);
        }
        
        return null;
    }
    
    public static void setCurrentOrganizationId(String organizationId) {
        currentOrganizationId.set(organizationId);
    }
    
    public static void clear() {
        currentOrganizationId.remove();
    }
}
```

### Security Utilities

Common security utilities:

```java
package com.zamaz.mcp.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtils {
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long expiration;
    
    public String generateToken(String subject) {
        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }
    
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Audit Logging

Common audit logging utilities:

```java
package com.zamaz.mcp.common.audit;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuditLogger {
    private static final Logger logger = LoggerFactory.getLogger("AUDIT");
    
    public void logEvent(String eventType, String userId, String organizationId, String resourceType, String resourceId, Map<String, Object> details) {
        AuditEvent event = new AuditEvent();
        event.setEventType(eventType);
        event.setUserId(userId);
        event.setOrganizationId(organizationId);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setDetails(details);
        event.setTimestamp(System.currentTimeMillis());
        
        logger.info("AUDIT: {}", event);
    }
}

@Data
public class AuditEvent {
    private String eventType;
    private String userId;
    private String organizationId;
    private String resourceType;
    private String resourceId;
    private Map<String, Object> details;
    private long timestamp;
}
```

### Configuration Properties

Common configuration properties:

```java
package com.zamaz.mcp.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mcp.common")
public class McpCommonProperties {
    private Security security = new Security();
    private Logging logging = new Logging();
    private Api api = new Api();
    
    @Data
    public static class Security {
        private boolean enabled = true;
        private String jwtSecret;
        private long jwtExpirationMs = 3600000; // 1 hour
    }
    
    @Data
    public static class Logging {
        private boolean auditEnabled = true;
        private String auditLoggerName = "AUDIT";
    }
    
    @Data
    public static class Api {
        private String defaultVersion = "v1";
        private boolean validateRequests = true;
    }
}
```

## Development

### Building the Library

```bash
cd mcp-common
mvn clean install
```

### Running Tests

```bash
cd mcp-common
mvn test
```

### Adding New Components

To add new components to the library:

1. Create new classes in the appropriate package
2. Add unit tests for the new components
3. Update documentation
4. Build and install the library
5. Update version in dependent services

## Best Practices

### When to Use MCP-Common

- For code that is used by multiple services
- For standardizing cross-cutting concerns
- For shared data models and DTOs
- For common utilities and helpers

### When Not to Use MCP-Common

- For service-specific logic
- For experimental or rapidly changing code
- For code with many dependencies
- For large, complex components that would create tight coupling

### Versioning

The library follows semantic versioning:

- **Major version**: Breaking changes
- **Minor version**: New features, non-breaking changes
- **Patch version**: Bug fixes, non-breaking changes

### Dependency Management

- Keep external dependencies to a minimum
- Use Spring Boot dependencies when possible
- Avoid conflicting dependencies
- Use dependency management to control versions

## Advanced Features

### Custom Annotations

The library includes custom annotations for common patterns:

```java
package com.zamaz.mcp.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresOrganization {
    boolean required() default true;
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {
    String action();
    String resourceType();
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 100;
    int duration() default 60; // seconds
}
```

### Aspect-Oriented Programming

The library includes aspects for cross-cutting concerns:

```java
package com.zamaz.mcp.common.aspect;

import com.zamaz.mcp.common.annotation.AuditAction;
import com.zamaz.mcp.common.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditLogger auditLogger;
    
    @Around("@annotation(com.zamaz.mcp.common.annotation.AuditAction)")
    public Object auditAction(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditAction auditAction = method.getAnnotation(AuditAction.class);
        
        String userId = getCurrentUserId();
        String organizationId = getCurrentOrganizationId();
        String resourceId = extractResourceId(joinPoint.getArgs());
        
        Map<String, Object> details = new HashMap<>();
        details.put("method", method.getName());
        details.put("args", joinPoint.getArgs());
        
        try {
            Object result = joinPoint.proceed();
            auditLogger.logEvent(auditAction.action(), userId, organizationId, auditAction.resourceType(), resourceId, details);
            return result;
        } catch (Exception e) {
            details.put("exception", e.getMessage());
            auditLogger.logEvent(auditAction.action() + "_FAILED", userId, organizationId, auditAction.resourceType(), resourceId, details);
            throw e;
        }
    }
    
    // Helper methods
    private String getCurrentUserId() {
        // Implementation
    }
    
    private String getCurrentOrganizationId() {
        // Implementation
    }
    
    private String extractResourceId(Object[] args) {
        // Implementation
    }
}
```

### Extension Points

The library provides extension points for service-specific customization:

```java
package com.zamaz.mcp.common.extension;

public interface McpExtensionPoint<T, R> {
    R extend(T input);
}

@Component
public class ExtensionRegistry {
    private final Map<Class<?>, List<McpExtensionPoint<?, ?>>> extensions = new HashMap<>();
    
    public <T, R> void registerExtension(Class<T> extensionPoint, McpExtensionPoint<T, R> extension) {
        extensions.computeIfAbsent(extensionPoint, k -> new ArrayList<>()).add(extension);
    }
    
    @SuppressWarnings("unchecked")
    public <T, R> List<McpExtensionPoint<T, R>> getExtensions(Class<T> extensionPoint) {
        return (List<McpExtensionPoint<T, R>>) (List<?>) extensions.getOrDefault(extensionPoint, Collections.emptyList());
    }
}
```
