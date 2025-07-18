package com.zamaz.mcp.controller.application.exception;

/**
 * Exception thrown when a participant is not found.
 */
public class ParticipantNotFoundException extends RuntimeException {
    
    public ParticipantNotFoundException(String message) {
        super(message);
    }
    
    public ParticipantNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}