package com.zamaz.mcp.controller.application.exception;

/**
 * Exception thrown when a debate is not found.
 */
public class DebateNotFoundException extends RuntimeException {
    
    public DebateNotFoundException(String message) {
        super(message);
    }
    
    public DebateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}