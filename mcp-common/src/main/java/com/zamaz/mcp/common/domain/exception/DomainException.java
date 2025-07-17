package com.zamaz.mcp.common.domain.exception;

/**
 * Base exception for all domain-related errors.
 * This represents business rule violations and domain logic errors.
 * This is a pure domain class with no framework dependencies.
 */
public abstract class DomainException extends RuntimeException {
    
    protected DomainException(String message) {
        super(message);
    }
    
    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}