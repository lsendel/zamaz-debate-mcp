package com.zamaz.mcp.common.client;

import com.zamaz.mcp.common.error.McpErrorCode;
import lombok.Getter;

/**
 * Exception thrown when MCP client operations fail.
 * Provides structured error information for inter-service communication failures.
 */
@Getter
public class McpClientException extends RuntimeException {

    private final McpErrorCode errorCode;
    private final String requestId;
    private final String serviceUrl;
    private final String toolName;

    /**
     * Create an MCP client exception with error code and request ID.
     */
    public McpClientException(McpErrorCode errorCode, String message, String requestId) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.serviceUrl = null;
        this.toolName = null;
    }

    /**
     * Create an MCP client exception with error code, request ID, and cause.
     */
    public McpClientException(McpErrorCode errorCode, String message, String requestId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.serviceUrl = null;
        this.toolName = null;
    }

    /**
     * Create an MCP client exception with full context information.
     */
    public McpClientException(McpErrorCode errorCode, String message, String requestId, 
                             String serviceUrl, String toolName) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.serviceUrl = serviceUrl;
        this.toolName = toolName;
    }

    /**
     * Create an MCP client exception with full context information and cause.
     */
    public McpClientException(McpErrorCode errorCode, String message, String requestId, 
                             String serviceUrl, String toolName, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.serviceUrl = serviceUrl;
        this.toolName = toolName;
    }

    /**
     * Get a descriptive error message including context.
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorCode.getCode()).append("] ").append(getMessage());
        
        if (requestId != null) {
            sb.append(" (Request ID: ").append(requestId).append(")");
        }
        
        if (serviceUrl != null) {
            sb.append(" [Service: ").append(serviceUrl).append("]");
        }
        
        if (toolName != null) {
            sb.append(" [Tool: ").append(toolName).append("]");
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return "McpClientException{" +
                "errorCode=" + errorCode +
                ", requestId='" + requestId + '\'' +
                ", serviceUrl='" + serviceUrl + '\'' +
                ", toolName='" + toolName + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}