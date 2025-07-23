package com.zamaz.mcp.common.patterns;

import com.zamaz.mcp.common.audit.Auditable;
import com.zamaz.mcp.common.audit.AuditEvent;
import com.zamaz.mcp.common.infrastructure.logging.StructuredLogger;
import com.zamaz.mcp.common.infrastructure.logging.StructuredLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Base controller class providing common functionality for all REST
 * controllers.
 * Implements standard patterns for CRUD operations, pagination, and logging.
 */
public abstract class BaseController {

    protected final StructuredLogger logger;

    @Autowired
    private HttpServletRequest request;

    protected BaseController(StructuredLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(this.getClass());
    }

    /**
     * Create a successful response with data.
     */
    protected <T> ResponseEntity<T> ok(T data) {
        return ResponseEntity.ok(data);
    }

    /**
     * Create a successful response with data and custom headers.
     */
    protected <T> ResponseEntity<T> ok(T data, HttpHeaders headers) {
        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    /**
     * Create a created response (201) with data.
     */
    protected <T> ResponseEntity<T> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(data);
    }

    /**
     * Create a created response (201) with data and location header.
     */
    protected <T> ResponseEntity<T> created(T data, String location) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, location)
                .body(data);
    }

    /**
     * Create a no content response (204).
     */
    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Create a not found response (404).
     */
    protected ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }

    /**
     * Create a response from Optional - 200 if present, 404 if empty.
     */
    protected <T> ResponseEntity<T> fromOptional(Optional<T> optional) {
        return optional.map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a paginated response with pagination headers.
     */
    protected <T> ResponseEntity<List<T>> paginated(Page<T> page) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(page.getTotalElements()));
        headers.add("X-Page-Number", String.valueOf(page.getNumber()));
        headers.add("X-Page-Size", String.valueOf(page.getSize()));
        headers.add("X-Total-Pages", String.valueOf(page.getTotalPages()));
        headers.add("X-Has-Next", String.valueOf(page.hasNext()));
        headers.add("X-Has-Previous", String.valueOf(page.hasPrevious()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(page.getContent());
    }

    /**
     * Get the current user ID from the request context.
     */
    protected String getCurrentUserId() {
        // Extract from JWT token or session
        String userId = request.getHeader("X-User-ID");
        if (userId == null) {
            userId = "anonymous";
        }
        return userId;
    }

    /**
     * Get the current organization ID from the request context.
     */
    protected String getCurrentOrganizationId() {
        // Extract from JWT token, header, or path parameter
        String orgId = request.getHeader("X-Organization-ID");
        if (orgId == null) {
            // Try to extract from path
            String path = request.getRequestURI();
            if (path.contains("/organizations/")) {
                String[] parts = path.split("/");
                for (int i = 0; i < parts.length - 1; i++) {
                    if ("organizations".equals(parts[i]) && i + 1 < parts.length) {
                        orgId = parts[i + 1];
                        break;
                    }
                }
            }
        }
        return orgId;
    }

    /**
     * Get the current request ID for tracing.
     */
    protected String getCurrentRequestId() {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }

    /**
     * Log a controller action with structured logging.
     */
    @Auditable(action = AuditEvent.AuditAction.READ, resourceType = "Controller", description = "Controller action executed")
    protected void logAction(String action, Object... params) {
        var logBuilder = logger.info("Controller action: " + action)
                .field("controller", this.getClass().getSimpleName())
                .field("action", action)
                .userId(getCurrentUserId())
                .organizationId(getCurrentOrganizationId())
                .requestId(getCurrentRequestId())
                .field("requestUri", request.getRequestURI())
                .field("httpMethod", request.getMethod())
                .field("userAgent", request.getHeader("User-Agent"))
                .field("sourceIp", getClientIpAddress());

        // Add parameters as fields
        for (int i = 0; i < params.length; i += 2) {
            if (i + 1 < params.length) {
                logBuilder.field(params[i].toString(), params[i + 1]);
            }
        }

        logBuilder.log();
    }

    /**
     * Log a controller error with structured logging.
     */
    @Auditable(action = AuditEvent.AuditAction.READ, resourceType = "Controller", description = "Controller error occurred", riskLevel = AuditEvent.RiskLevel.HIGH, auditOnlyFailures = true)
    protected void logError(String action, Exception exception, Object... params) {
        var logBuilder = logger.error("Controller error: " + action, exception)
                .field("controller", this.getClass().getSimpleName())
                .field("action", action)
                .userId(getCurrentUserId())
                .organizationId(getCurrentOrganizationId())
                .requestId(getCurrentRequestId())
                .field("requestUri", request.getRequestURI())
                .field("httpMethod", request.getMethod())
                .field("userAgent", request.getHeader("User-Agent"))
                .field("sourceIp", getClientIpAddress())
                .exception(exception);

        // Add parameters as fields
        for (int i = 0; i < params.length; i += 2) {
            if (i + 1 < params.length) {
                logBuilder.field(params[i].toString(), params[i + 1]);
            }
        }

        logBuilder.log();
    }

    /**
     * Log request start with timing.
     */
    protected long logRequestStart(String operation) {
        long startTime = System.currentTimeMillis();

        logger.info("Request started")
                .field("controller", this.getClass().getSimpleName())
                .operation(operation)
                .userId(getCurrentUserId())
                .organizationId(getCurrentOrganizationId())
                .requestId(getCurrentRequestId())
                .field("requestUri", request.getRequestURI())
                .field("httpMethod", request.getMethod())
                .field("startTime", startTime)
                .log();

        return startTime;
    }

    /**
     * Log request completion with timing.
     */
    protected void logRequestComplete(String operation, long startTime, Object result) {
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Request completed")
                .field("controller", this.getClass().getSimpleName())
                .operation(operation)
                .userId(getCurrentUserId())
                .organizationId(getCurrentOrganizationId())
                .requestId(getCurrentRequestId())
                .field("requestUri", request.getRequestURI())
                .field("httpMethod", request.getMethod())
                .duration(duration)
                .field("resultType", result != null ? result.getClass().getSimpleName() : "null")
                .log();
    }

    /**
     * Validate that the current user has access to the specified organization.
     */
    protected void validateOrganizationAccess(String organizationId) {
        String currentOrgId = getCurrentOrganizationId();
        if (currentOrgId != null && !currentOrgId.equals(organizationId)) {
            throw new SecurityException("Access denied to organization: " + organizationId);
        }
    }

    /**
     * Create standard validation headers for caching.
     */
    protected HttpHeaders createCacheHeaders(long maxAgeSeconds) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("public, max-age=" + maxAgeSeconds);
        return headers;
    }

    /**
     * Create no-cache headers for sensitive data.
     */
    protected HttpHeaders createNoCacheHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);
        return headers;
    }

    /**
     * Get client IP address from request.
     */
    protected String getClientIpAddress() {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Execute an operation with automatic request/response logging and error
     * handling.
     */
    protected <T> ResponseEntity<T> executeWithLogging(String operation,
            java.util.function.Supplier<ResponseEntity<T>> supplier) {
        long startTime = logRequestStart(operation);

        try {
            ResponseEntity<T> result = supplier.get();
            logRequestComplete(operation, startTime, result.getBody());
            return result;
        } catch (Exception e) {
            logError(operation, e);
            throw e;
        }
    }

    /**
     * Handle McpBusinessException with proper error response.
     */
    protected ResponseEntity<Map<String, Object>> handleBusinessException(
            com.zamaz.mcp.common.exception.McpBusinessException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getErrorCode());
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("details", ex.getErrorDetails());
        errorResponse.put("timestamp", java.time.Instant.now().toString());
        errorResponse.put("requestId", getCurrentRequestId());

        logError("business_exception", ex, "errorCode", ex.getErrorCode());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Validate required parameters and throw McpBusinessException if missing.
     */
    protected void validateRequired(String paramName, Object value) {
        if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
            throw com.zamaz.mcp.common.exception.McpBusinessException.validationFailed(
                    paramName, "Required parameter is missing or empty");
        }
    }

    /**
     * Validate organization access for the current request.
     */
    protected void validateOrganizationAccess(String organizationId) {
        String currentOrgId = getCurrentOrganizationId();
        if (currentOrgId != null && !currentOrgId.equals(organizationId)) {
            throw com.zamaz.mcp.common.exception.McpBusinessException.organizationAccessDenied(
                    organizationId, getCurrentUserId());
        }
    }
}