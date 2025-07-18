package com.zamaz.mcp.common.security;

/**
 * Security exception specific to MCP tool operations.
 * Thrown when security validation fails for MCP endpoints.
 */
public class McpSecurityException extends RuntimeException {
    
    public McpSecurityException(String message) {
        super(message);
    }
    
    public McpSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}