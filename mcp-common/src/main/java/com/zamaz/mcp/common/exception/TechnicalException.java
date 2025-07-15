package com.zamaz.mcp.common.exception;

import java.util.Map;

/**
 * Exception for technical/system errors.
 * These are unexpected errors that indicate system issues.
 */
public class TechnicalException extends BaseException {
    
    public TechnicalException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    public TechnicalException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
    
    /**
     * Add additional error detail with fluent interface
     */
    public TechnicalException withDetail(String key, Object value) {
        addDetail(key, value);
        return this;
    }
    
    /**
     * Add multiple error details with fluent interface
     */
    public TechnicalException withDetails(Map<String, Object> details) {
        addDetails(details);
        return this;
    }
    
    // Common technical exceptions
    
    public static TechnicalException databaseError(String operation, Throwable cause) {
        return new TechnicalException(
            String.format("Database error during operation: %s", operation),
            "DATABASE_ERROR",
            cause
        ).withDetail("operation", operation);
    }
    
    public static TechnicalException externalServiceError(String service, String operation, Throwable cause) {
        return new TechnicalException(
            String.format("External service error. Service: %s, Operation: %s", service, operation),
            "EXTERNAL_SERVICE_ERROR",
            cause
        ).withDetail("service", service).withDetail("operation", operation);
    }
    
    public static TechnicalException configurationError(String property, String reason) {
        return new TechnicalException(
            String.format("Configuration error for property '%s': %s", property, reason),
            "CONFIGURATION_ERROR"
        ).withDetail("property", property).withDetail("reason", reason);
    }
    
    public static TechnicalException serializationError(String operation, Throwable cause) {
        return new TechnicalException(
            String.format("Serialization error during: %s", operation),
            "SERIALIZATION_ERROR",
            cause
        ).withDetail("operation", operation);
    }
}