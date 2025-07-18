package com.zamaz.mcp.common.error;

import com.zamaz.mcp.common.security.McpSecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized MCP error handler for consistent error responses across all services.
 */
@Component
@Slf4j
public class McpErrorHandler {
    
    /**
     * Creates a standardized error response for MCP tools.
     */
    public ResponseEntity<McpErrorResponse> createErrorResponse(
            Exception exception, 
            String toolName, 
            String requestId) {
        
        McpErrorCode errorCode = mapExceptionToErrorCode(exception);
        HttpStatus httpStatus = mapErrorCodeToHttpStatus(errorCode);
        
        McpErrorResponse errorResponse = McpErrorResponse.builder()
                .code(errorCode.getCode())
                .message(getSafeErrorMessage(exception, errorCode))
                .requestId(requestId != null ? requestId : UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .toolName(toolName)
                .status(httpStatus.value())
                .success(false)
                .build();
        
        // Log error details for debugging (but don't expose in response)
        log.error("MCP Tool Error [{}] in tool '{}': {} - {}", 
                errorCode.getCode(), toolName, exception.getClass().getSimpleName(), exception.getMessage(), exception);
        
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }
    
    /**
     * Creates a success response for MCP tools.
     */
    public ResponseEntity<Map<String, Object>> createSuccessResponse(Object data, String requestId) {
        Map<String, Object> response = Map.of(
                "success", true,
                "data", data,
                "requestId", requestId != null ? requestId : UUID.randomUUID().toString(),
                "timestamp", Instant.now()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Maps exceptions to appropriate MCP error codes.
     */
    private McpErrorCode mapExceptionToErrorCode(Exception exception) {
        return switch (exception) {
            case McpSecurityException ignored -> McpErrorCode.ACCESS_DENIED;
            case AuthenticationException ignored -> McpErrorCode.AUTHENTICATION_REQUIRED;
            case AuthenticationCredentialsNotFoundException ignored -> McpErrorCode.AUTHENTICATION_REQUIRED;
            case AccessDeniedException ignored -> McpErrorCode.ACCESS_DENIED;
            case IllegalArgumentException ignored -> McpErrorCode.INVALID_PARAMETER;
            case NullPointerException ignored -> McpErrorCode.MISSING_PARAMETER;
            case SecurityException ignored -> McpErrorCode.ACCESS_DENIED;
            default -> McpErrorCode.INTERNAL_ERROR;
        };
    }
    
    /**
     * Maps error codes to HTTP status codes.
     */
    private HttpStatus mapErrorCodeToHttpStatus(McpErrorCode errorCode) {
        return switch (errorCode) {
            case AUTHENTICATION_REQUIRED, INVALID_TOKEN, TOKEN_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED, INSUFFICIENT_PERMISSIONS, ORGANIZATION_ACCESS_DENIED, 
                 CROSS_TENANT_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case INVALID_REQUEST, MISSING_PARAMETER, INVALID_PARAMETER, 
                 PARAMETER_TYPE_MISMATCH -> HttpStatus.BAD_REQUEST;
            case TOOL_NOT_FOUND, RESOURCE_NOT_FOUND, ORGANIZATION_NOT_FOUND, 
                 CONTEXT_NOT_FOUND, DOCUMENT_NOT_FOUND, DEBATE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RESOURCE_CONFLICT -> HttpStatus.CONFLICT;
            case RATE_LIMIT_EXCEEDED, TOOL_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case SERVICE_UNAVAILABLE, CIRCUIT_BREAKER_OPEN -> HttpStatus.SERVICE_UNAVAILABLE;
            case TIMEOUT, TOOL_TIMEOUT -> HttpStatus.REQUEST_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    /**
     * Gets a safe error message that doesn't expose sensitive information.
     */
    private String getSafeErrorMessage(Exception exception, McpErrorCode errorCode) {
        // For security exceptions, always use the error code's default message
        if (exception instanceof McpSecurityException || 
            exception instanceof AuthenticationException ||
            exception instanceof AccessDeniedException) {
            return errorCode.getDefaultMessage();
        }
        
        // For validation errors, we can expose some details
        if (errorCode == McpErrorCode.INVALID_PARAMETER || 
            errorCode == McpErrorCode.MISSING_PARAMETER) {
            String message = exception.getMessage();
            // Sanitize the message to avoid exposing sensitive information
            if (message != null && message.length() < 200 && !containsSensitiveInfo(message)) {
                return message;
            }
        }
        
        // For all other errors, use the default message to avoid information disclosure
        return errorCode.getDefaultMessage();
    }
    
    /**
     * Checks if a message contains potentially sensitive information.
     */
    private boolean containsSensitiveInfo(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("password") ||
               lowerMessage.contains("token") ||
               lowerMessage.contains("secret") ||
               lowerMessage.contains("key") ||
               lowerMessage.contains("sql") ||
               lowerMessage.contains("database") ||
               lowerMessage.contains("exception") ||
               lowerMessage.contains("stack");
    }
}