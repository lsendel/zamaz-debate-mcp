package com.zamaz.mcp.common.resilience.exception;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/**
 * Exception thrown when circuit breaker execution fails.
 */
public class CircuitBreakerExecutionException extends RuntimeException {
    
    private final String circuitBreakerName;
    private final CircuitBreaker.State circuitBreakerState;
    private final Throwable originalException;
    
    public CircuitBreakerExecutionException(String circuitBreakerName, 
                                          CircuitBreaker.State circuitBreakerState, 
                                          Throwable originalException) {
        super(String.format("Circuit breaker '%s' execution failed (state: %s): %s", 
              circuitBreakerName, circuitBreakerState, originalException.getMessage()), originalException);
        this.circuitBreakerName = circuitBreakerName;
        this.circuitBreakerState = circuitBreakerState;
        this.originalException = originalException;
    }
    
    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }
    
    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreakerState;
    }
    
    public Throwable getOriginalException() {
        return originalException;
    }
}