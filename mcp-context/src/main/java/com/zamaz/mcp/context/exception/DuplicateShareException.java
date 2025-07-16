package com.zamaz.mcp.context.exception;

import com.zamaz.mcp.common.exception.BusinessException;

/**
 * Exception thrown when attempting to create a duplicate share.
 */
public class DuplicateShareException extends BusinessException {
    
    public DuplicateShareException(String message) {
        super(message, "DUPLICATE_SHARE");
    }
}