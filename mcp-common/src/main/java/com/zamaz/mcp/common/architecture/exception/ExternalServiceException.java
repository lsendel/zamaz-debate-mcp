package com.zamaz.mcp.common.architecture.exception;

/**
 * Thrown when an external service call fails.
 * This could be due to network issues, API errors, timeouts, etc.
 * This is part of the adapter layer in hexagonal architecture.
 */
public class ExternalServiceException extends AdapterException {
    
    private final String service;
    private final int statusCode;
    
    public ExternalServiceException(String service, String message) {
        super(message);
        this.service = service;
        this.statusCode = -1;
    }
    
    public ExternalServiceException(String service, int statusCode, String message) {
        super(message);
        this.service = service;
        this.statusCode = statusCode;
    }
    
    public ExternalServiceException(String service, String message, Throwable cause) {
        super(message, cause);
        this.service = service;
        this.statusCode = -1;
    }
    
    public String getService() {
        return service;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}