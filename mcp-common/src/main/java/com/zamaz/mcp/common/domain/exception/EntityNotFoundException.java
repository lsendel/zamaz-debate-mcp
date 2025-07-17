package com.zamaz.mcp.common.domain.exception;

/**
 * Thrown when a domain entity cannot be found.
 * This is a pure domain class with no framework dependencies.
 */
public class EntityNotFoundException extends DomainException {
    
    public EntityNotFoundException(String entityType, Object id) {
        super(String.format("%s not found with id: %s", entityType, id));
    }
    
    public EntityNotFoundException(String message) {
        super(message);
    }
}