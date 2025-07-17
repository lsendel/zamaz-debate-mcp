package com.zamaz.mcp.organization.application.exception;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;

/**
 * Exception thrown when a user is not found.
 */
public class UserNotFoundException extends ResourceNotFoundException {
    
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}