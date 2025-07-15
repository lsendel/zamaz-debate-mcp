package com.zamaz.mcp.common.exception;

import java.util.Map;

/**
 * Exception for business logic errors.
 * These are expected errors that should be handled gracefully by the client.
 */
public class BusinessException extends BaseException {
    
    public BusinessException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    public BusinessException(String message, String errorCode, Map<String, Object> errorDetails) {
        super(message, errorCode, errorDetails);
    }
    
    /**
     * Add additional error detail with fluent interface
     */
    public BusinessException withDetail(String key, Object value) {
        addDetail(key, value);
        return this;
    }
    
    /**
     * Add multiple error details with fluent interface
     */
    public BusinessException withDetails(Map<String, Object> details) {
        addDetails(details);
        return this;
    }
    
    // Common business exceptions
    
    public static BusinessException notFound(String resource, String id) {
        return new BusinessException(
            String.format("%s not found with id: %s", resource, id),
            "RESOURCE_NOT_FOUND"
        ).withDetail("resource", resource).withDetail("id", id);
    }
    
    public static BusinessException alreadyExists(String resource, String field, String value) {
        return new BusinessException(
            String.format("%s already exists with %s: %s", resource, field, value),
            "RESOURCE_ALREADY_EXISTS"
        ).withDetail("resource", resource).withDetail("field", field).withDetail("value", value);
    }
    
    public static BusinessException invalidState(String resource, String currentState, String expectedState) {
        return new BusinessException(
            String.format("%s is in invalid state. Current: %s, Expected: %s", resource, currentState, expectedState),
            "INVALID_STATE"
        ).withDetail("resource", resource)
         .withDetail("currentState", currentState)
         .withDetail("expectedState", expectedState);
    }
    
    public static BusinessException validationFailed(String field, String reason) {
        return new BusinessException(
            String.format("Validation failed for field '%s': %s", field, reason),
            "VALIDATION_FAILED"
        ).withDetail("field", field).withDetail("reason", reason);
    }
    
    public static BusinessException accessDenied(String resource, String action) {
        return new BusinessException(
            String.format("Access denied for action '%s' on resource '%s'", action, resource),
            "ACCESS_DENIED"
        ).withDetail("resource", resource).withDetail("action", action);
    }
}