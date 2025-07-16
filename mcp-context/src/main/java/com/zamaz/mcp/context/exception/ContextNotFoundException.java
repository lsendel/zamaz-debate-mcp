package com.zamaz.mcp.context.exception;

import com.zamaz.mcp.common.exception.BusinessException;

/**
 * Exception thrown when a context is not found.
 */
public class ContextNotFoundException extends BusinessException {
    
    public ContextNotFoundException(String message) {
        super(message, "CONTEXT_NOT_FOUND");
    }
}