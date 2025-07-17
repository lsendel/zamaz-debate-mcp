package com.zamaz.mcp.common.application.exception;

/**
 * Base exception for all application layer errors.
 * This represents use case failures and application logic errors.
 * This is part of the application layer in hexagonal architecture.
 */
public abstract class ApplicationException extends RuntimeException {
    
    protected ApplicationException(String message) {
        super(message);
    }
    
    protected ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}