package com.zamaz.mcp.context.exception;

import com.zamaz.mcp.common.exception.BusinessException;

/**
 * Exception thrown when unauthorized access is attempted.
 */
public class UnauthorizedAccessException extends BusinessException {
    
    public UnauthorizedAccessException(String message) {
        super(message, "UNAUTHORIZED_ACCESS");
    }
}