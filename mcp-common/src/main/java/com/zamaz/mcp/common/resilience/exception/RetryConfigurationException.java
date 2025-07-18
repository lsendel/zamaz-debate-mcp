package com.zamaz.mcp.common.resilience.exception;

/**
 * Exception thrown when retry configuration is invalid.
 */
public class RetryConfigurationException extends RuntimeException {
    
    public RetryConfigurationException(String message) {
        super(message);
    }
    
    public RetryConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}