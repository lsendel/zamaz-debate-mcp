package com.zamaz.mcp.common.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception class for all MCP services.
 * Provides structured error information for API responses.
 */
@Getter
public abstract class BaseException extends RuntimeException {
    
    private final String errorCode;
    private final Map<String, Object> errorDetails;
    
    protected BaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetails = new HashMap<>();
    }
    
    protected BaseException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorDetails = new HashMap<>();
    }
    
    protected BaseException(String message, String errorCode, Map<String, Object> errorDetails) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetails = errorDetails != null ? new HashMap<>(errorDetails) : new HashMap<>();
    }
    
    protected BaseException(String message, String errorCode, Map<String, Object> errorDetails, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorDetails = errorDetails != null ? new HashMap<>(errorDetails) : new HashMap<>();
    }
    
    /**
     * Add additional error detail
     */
    protected void addDetail(String key, Object value) {
        this.errorDetails.put(key, value);
    }
    
    /**
     * Add multiple error details
     */
    protected void addDetails(Map<String, Object> details) {
        if (details != null) {
            this.errorDetails.putAll(details);
        }
    }
}