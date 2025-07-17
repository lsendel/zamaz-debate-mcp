package com.zamaz.mcp.common.architecture.exception;

/**
 * Base exception for all adapter layer errors.
 * This represents infrastructure failures and external system errors.
 * This is part of the adapter layer in hexagonal architecture.
 */
public abstract class AdapterException extends RuntimeException {
    
    protected AdapterException(String message) {
        super(message);
    }
    
    protected AdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}