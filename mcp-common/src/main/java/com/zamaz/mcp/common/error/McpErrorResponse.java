package com.zamaz.mcp.common.error;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized MCP error response format across all services.
 * Follows MCP protocol standards for error reporting.
 */
@Data
@Builder
public class McpErrorResponse {
    
    /**
     * Error code following MCP protocol standards
     */
    private String code;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * Unique request identifier for tracking
     */
    private String requestId;
    
    /**
     * Timestamp when error occurred
     */
    private Instant timestamp;
    
    /**
     * Additional error details (optional)
     */
    private Map<String, Object> details;
    
    /**
     * Success flag (always false for errors)
     */
    @Builder.Default
    private boolean success = false;
    
    /**
     * MCP tool name that caused the error
     */
    private String toolName;
    
    /**
     * HTTP status code
     */
    private int status;
}