package com.zamaz.mcp.common.architecture.exception;

/**
 * Thrown when a persistence operation fails.
 * This could be due to database connectivity, constraint violations, etc.
 * This is part of the adapter layer in hexagonal architecture.
 */
public class PersistenceException extends AdapterException {
    
    public PersistenceException(String message) {
        super(message);
    }
    
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}