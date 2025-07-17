package com.zamaz.mcp.common.application.exception;

/**
 * Thrown when a use case cannot be executed due to business logic constraints.
 * This is part of the application layer in hexagonal architecture.
 */
public class UseCaseException extends ApplicationException {
    
    private final String useCase;
    
    public UseCaseException(String useCase, String message) {
        super(message);
        this.useCase = useCase;
    }
    
    public UseCaseException(String useCase, String message, Throwable cause) {
        super(message, cause);
        this.useCase = useCase;
    }
    
    public String getUseCase() {
        return useCase;
    }
}