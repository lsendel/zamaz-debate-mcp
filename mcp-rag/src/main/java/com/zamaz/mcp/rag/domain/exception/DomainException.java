package com.zamaz.mcp.rag.domain.exception;

/**
 * Base exception for all domain-related exceptions in the RAG system.
 * All domain exceptions should extend this class.
 */
public abstract class DomainException extends RuntimeException {
    
    private final String errorCode;
    
    protected DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}