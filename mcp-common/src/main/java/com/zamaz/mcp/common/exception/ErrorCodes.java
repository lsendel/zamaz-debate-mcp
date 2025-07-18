package com.zamaz.mcp.common.exception;

/**
 * Standardized error codes for consistent error handling across all microservices.
 * Error codes follow the pattern: [SERVICE]_[CATEGORY]_[SPECIFIC_ERROR]
 */
public final class ErrorCodes {

    // Common error codes (used across all services)
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    public static final String MISSING_PARAMETER = "MISSING_PARAMETER";
    public static final String PARAMETER_TYPE_MISMATCH = "PARAMETER_TYPE_MISMATCH";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String EXTERNAL_SERVICE_ERROR = "EXTERNAL_SERVICE_ERROR";

    // Authentication & Authorization
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH_TOKEN_EXPIRED";
    public static final String AUTH_TOKEN_INVALID = "AUTH_TOKEN_INVALID";
    public static final String AUTH_INSUFFICIENT_PERMISSIONS = "AUTH_INSUFFICIENT_PERMISSIONS";
    public static final String AUTH_USER_NOT_FOUND = "AUTH_USER_NOT_FOUND";
    public static final String AUTH_USER_DISABLED = "AUTH_USER_DISABLED";

    // Organization Service
    public static final String ORG_NOT_FOUND = "ORG_NOT_FOUND";
    public static final String ORG_ALREADY_EXISTS = "ORG_ALREADY_EXISTS";
    public static final String ORG_INVALID_DOMAIN = "ORG_INVALID_DOMAIN";
    public static final String ORG_SUBSCRIPTION_EXPIRED = "ORG_SUBSCRIPTION_EXPIRED";
    public static final String ORG_MEMBER_LIMIT_EXCEEDED = "ORG_MEMBER_LIMIT_EXCEEDED";
    public static final String ORG_MEMBER_NOT_FOUND = "ORG_MEMBER_NOT_FOUND";
    public static final String ORG_MEMBER_ALREADY_EXISTS = "ORG_MEMBER_ALREADY_EXISTS";

    // LLM Service
    public static final String LLM_PROVIDER_NOT_FOUND = "LLM_PROVIDER_NOT_FOUND";
    public static final String LLM_PROVIDER_DISABLED = "LLM_PROVIDER_DISABLED";
    public static final String LLM_MODEL_NOT_FOUND = "LLM_MODEL_NOT_FOUND";
    public static final String LLM_MODEL_NOT_AVAILABLE = "LLM_MODEL_NOT_AVAILABLE";
    public static final String LLM_API_KEY_INVALID = "LLM_API_KEY_INVALID";
    public static final String LLM_API_KEY_MISSING = "LLM_API_KEY_MISSING";
    public static final String LLM_RATE_LIMIT_EXCEEDED = "LLM_RATE_LIMIT_EXCEEDED";
    public static final String LLM_QUOTA_EXCEEDED = "LLM_QUOTA_EXCEEDED";
    public static final String LLM_REQUEST_TOO_LARGE = "LLM_REQUEST_TOO_LARGE";
    public static final String LLM_INVALID_PARAMETERS = "LLM_INVALID_PARAMETERS";

    // Debate Service
    public static final String DEBATE_NOT_FOUND = "DEBATE_NOT_FOUND";
    public static final String DEBATE_ALREADY_STARTED = "DEBATE_ALREADY_STARTED";
    public static final String DEBATE_ALREADY_ENDED = "DEBATE_ALREADY_ENDED";
    public static final String DEBATE_INVALID_STATE = "DEBATE_INVALID_STATE";
    public static final String DEBATE_PARTICIPANT_LIMIT_EXCEEDED = "DEBATE_PARTICIPANT_LIMIT_EXCEEDED";
    public static final String DEBATE_PARTICIPANT_NOT_FOUND = "DEBATE_PARTICIPANT_NOT_FOUND";
    public static final String DEBATE_PARTICIPANT_ALREADY_EXISTS = "DEBATE_PARTICIPANT_ALREADY_EXISTS";
    public static final String DEBATE_ROUND_LIMIT_EXCEEDED = "DEBATE_ROUND_LIMIT_EXCEEDED";
    public static final String DEBATE_TURN_TIMEOUT = "DEBATE_TURN_TIMEOUT";
    public static final String DEBATE_TURN_TOO_LONG = "DEBATE_TURN_TOO_LONG";
    public static final String DEBATE_TURN_TOO_SHORT = "DEBATE_TURN_TOO_SHORT";

    // RAG Service
    public static final String RAG_DOCUMENT_NOT_FOUND = "RAG_DOCUMENT_NOT_FOUND";
    public static final String RAG_DOCUMENT_TOO_LARGE = "RAG_DOCUMENT_TOO_LARGE";
    public static final String RAG_DOCUMENT_INVALID_FORMAT = "RAG_DOCUMENT_INVALID_FORMAT";
    public static final String RAG_EMBEDDING_FAILED = "RAG_EMBEDDING_FAILED";
    public static final String RAG_VECTOR_STORE_UNAVAILABLE = "RAG_VECTOR_STORE_UNAVAILABLE";
    public static final String RAG_SEARCH_FAILED = "RAG_SEARCH_FAILED";
    public static final String RAG_INDEX_NOT_FOUND = "RAG_INDEX_NOT_FOUND";

    // Template Service
    public static final String TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";
    public static final String TEMPLATE_INVALID_SYNTAX = "TEMPLATE_INVALID_SYNTAX";
    public static final String TEMPLATE_VARIABLE_MISSING = "TEMPLATE_VARIABLE_MISSING";
    public static final String TEMPLATE_COMPILATION_FAILED = "TEMPLATE_COMPILATION_FAILED";
    public static final String TEMPLATE_RENDERING_FAILED = "TEMPLATE_RENDERING_FAILED";

    // Context Service
    public static final String CONTEXT_NOT_FOUND = "CONTEXT_NOT_FOUND";
    public static final String CONTEXT_TOO_LARGE = "CONTEXT_TOO_LARGE";
    public static final String CONTEXT_INVALID_FORMAT = "CONTEXT_INVALID_FORMAT";
    public static final String CONTEXT_EXPIRED = "CONTEXT_EXPIRED";

    // Rate Limiting
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String RATE_LIMIT_QUOTA_EXCEEDED = "RATE_LIMIT_QUOTA_EXCEEDED";

    // External Services
    public static final String EXTERNAL_SERVICE_TIMEOUT = "EXTERNAL_SERVICE_TIMEOUT";
    public static final String EXTERNAL_SERVICE_UNAVAILABLE = "EXTERNAL_SERVICE_UNAVAILABLE";
    public static final String EXTERNAL_SERVICE_AUTH_FAILED = "EXTERNAL_SERVICE_AUTH_FAILED";
    public static final String EXTERNAL_SERVICE_RATE_LIMITED = "EXTERNAL_SERVICE_RATE_LIMITED";

    // Database
    public static final String DATABASE_CONNECTION_FAILED = "DATABASE_CONNECTION_FAILED";
    public static final String DATABASE_CONSTRAINT_VIOLATION = "DATABASE_CONSTRAINT_VIOLATION";
    public static final String DATABASE_TIMEOUT = "DATABASE_TIMEOUT";

    // Cache
    public static final String CACHE_UNAVAILABLE = "CACHE_UNAVAILABLE";
    public static final String CACHE_EVICTION_FAILED = "CACHE_EVICTION_FAILED";

    // Configuration
    public static final String CONFIG_INVALID = "CONFIG_INVALID";
    public static final String CONFIG_MISSING = "CONFIG_MISSING";

    private ErrorCodes() {
        // Utility class - prevent instantiation
    }
}