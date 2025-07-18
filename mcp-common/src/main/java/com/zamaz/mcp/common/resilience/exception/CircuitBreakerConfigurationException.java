package com.zamaz.mcp.common.resilience.exception;

/**
 * Exception thrown when circuit breaker configuration is invalid.
 */
public class CircuitBreakerConfigurationException extends RuntimeException {
    
    public CircuitBreakerConfigurationException(String message) {
        super(message);
    }
    
    public CircuitBreakerConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}