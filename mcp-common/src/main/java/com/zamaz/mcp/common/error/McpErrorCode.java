package com.zamaz.mcp.common.error;

/**
 * Standard MCP error codes following MCP protocol specifications.
 */
public enum McpErrorCode {
    
    // Authentication & Authorization Errors
    AUTHENTICATION_REQUIRED("AUTH_001", "Authentication required"),
    ACCESS_DENIED("AUTH_002", "Access denied"),
    INVALID_TOKEN("AUTH_003", "Invalid authentication token"),
    TOKEN_EXPIRED("AUTH_004", "Authentication token expired"),
    INSUFFICIENT_PERMISSIONS("AUTH_005", "Insufficient permissions"),
    
    // Request Validation Errors
    INVALID_REQUEST("REQ_001", "Invalid request format"),
    MISSING_PARAMETER("REQ_002", "Required parameter missing"),
    INVALID_PARAMETER("REQ_003", "Invalid parameter value"),
    PARAMETER_TYPE_MISMATCH("REQ_004", "Parameter type mismatch"),
    
    // Tool Errors
    TOOL_NOT_FOUND("TOOL_001", "Tool not found"),
    TOOL_EXECUTION_FAILED("TOOL_002", "Tool execution failed"),
    TOOL_TIMEOUT("TOOL_003", "Tool execution timeout"),
    TOOL_RATE_LIMITED("TOOL_004", "Tool rate limit exceeded"),
    
    // Resource Errors
    RESOURCE_NOT_FOUND("RES_001", "Resource not found"),
    RESOURCE_CONFLICT("RES_002", "Resource conflict"),
    RESOURCE_LOCKED("RES_003", "Resource is locked"),
    
    // Organization & Multi-tenant Errors
    ORGANIZATION_NOT_FOUND("ORG_001", "Organization not found"),
    ORGANIZATION_ACCESS_DENIED("ORG_002", "Organization access denied"),
    CROSS_TENANT_ACCESS_DENIED("ORG_003", "Cross-tenant access denied"),
    
    // Context Service Errors
    CONTEXT_NOT_FOUND("CTX_001", "Context not found"),
    CONTEXT_LIMIT_EXCEEDED("CTX_002", "Context limit exceeded"),
    MESSAGE_TOO_LARGE("CTX_003", "Message too large"),
    
    // LLM Service Errors
    PROVIDER_NOT_AVAILABLE("LLM_001", "LLM provider not available"),
    MODEL_NOT_SUPPORTED("LLM_002", "Model not supported"),
    COMPLETION_FAILED("LLM_003", "Completion generation failed"),
    RATE_LIMIT_EXCEEDED("LLM_004", "Rate limit exceeded"),
    
    // RAG Service Errors
    DOCUMENT_NOT_FOUND("RAG_001", "Document not found"),
    DOCUMENT_TOO_LARGE("RAG_002", "Document too large"),
    EMBEDDING_FAILED("RAG_003", "Embedding generation failed"),
    SEARCH_FAILED("RAG_004", "Document search failed"),
    
    // Debate Service Errors
    DEBATE_NOT_FOUND("DEB_001", "Debate not found"),
    DEBATE_FULL("DEB_002", "Debate is full"),
    DEBATE_ENDED("DEB_003", "Debate has ended"),
    INVALID_TURN("DEB_004", "Invalid turn submission"),
    
    // System Errors
    INTERNAL_ERROR("SYS_001", "Internal server error"),
    SERVICE_UNAVAILABLE("SYS_002", "Service temporarily unavailable"),
    TIMEOUT("SYS_003", "Request timeout"),
    CIRCUIT_BREAKER_OPEN("SYS_004", "Service circuit breaker is open");
    
    private final String code;
    private final String defaultMessage;
    
    McpErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
}