package com.zamaz.mcp.common.application.exception;

/**
 * Thrown when a requested resource cannot be found.
 * This is typically used in use cases when querying for data.
 * This is part of the application layer in hexagonal architecture.
 */
public class ResourceNotFoundException extends ApplicationException {
    
    public ResourceNotFoundException(String resourceType, Object id) {
        super(String.format("%s not found with id: %s", resourceType, id));
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}