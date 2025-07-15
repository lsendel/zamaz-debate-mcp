package com.zamaz.mcp.common.constant;

/**
 * Centralized error codes for all MCP services.
 * Provides consistent error identification across the system.
 */
public final class ErrorCode {
    
    private ErrorCode() {
        // Prevent instantiation
    }
    
    // Business Errors (4xx)
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String RESOURCE_ALREADY_EXISTS = "RESOURCE_ALREADY_EXISTS";
    public static final String INVALID_STATE = "INVALID_STATE";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String INSUFFICIENT_PERMISSIONS = "INSUFFICIENT_PERMISSIONS";
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
    
    // Technical Errors (5xx)
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String EXTERNAL_SERVICE_ERROR = "EXTERNAL_SERVICE_ERROR";
    public static final String CONFIGURATION_ERROR = "CONFIGURATION_ERROR";
    public static final String SERIALIZATION_ERROR = "SERIALIZATION_ERROR";
    public static final String TIMEOUT_ERROR = "TIMEOUT_ERROR";
    public static final String CIRCUIT_BREAKER_OPEN = "CIRCUIT_BREAKER_OPEN";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    
    // MCP Specific Errors
    public static final String MCP_PROTOCOL_ERROR = "MCP_PROTOCOL_ERROR";
    public static final String MCP_TOOL_NOT_FOUND = "MCP_TOOL_NOT_FOUND";
    public static final String MCP_RESOURCE_NOT_FOUND = "MCP_RESOURCE_NOT_FOUND";
    public static final String MCP_INVALID_PARAMETERS = "MCP_INVALID_PARAMETERS";
    
    // LLM Specific Errors
    public static final String LLM_PROVIDER_ERROR = "LLM_PROVIDER_ERROR";
    public static final String LLM_TOKEN_LIMIT_EXCEEDED = "LLM_TOKEN_LIMIT_EXCEEDED";
    public static final String LLM_INVALID_MODEL = "LLM_INVALID_MODEL";
    public static final String LLM_QUOTA_EXCEEDED = "LLM_QUOTA_EXCEEDED";
}