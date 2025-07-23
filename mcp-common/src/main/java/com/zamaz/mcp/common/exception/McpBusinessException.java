package com.zamaz.mcp.common.exception;

import java.util.Map;

/**
 * Standardized business exception for MCP services.
 * This exception should be used for all business logic errors that are expected
 * and should be handled gracefully by the client.
 * 
 * Extends BusinessException to maintain compatibility with existing error
 * handling
 * while providing MCP-specific naming and functionality.
 */
public class McpBusinessException extends BusinessException {

    public McpBusinessException(String message, String errorCode) {
        super(message, errorCode);
    }

    public McpBusinessException(String message, String errorCode, Map<String, Object> errorDetails) {
        super(message, errorCode, errorDetails);
    }

    public McpBusinessException(String message, String errorCode, Throwable cause) {
        super(message, errorCode);
        initCause(cause);
    }

    public McpBusinessException(String message, String errorCode, Map<String, Object> errorDetails, Throwable cause) {
        super(message, errorCode, errorDetails);
        initCause(cause);
    }

    /**
     * Add additional error detail with fluent interface
     */
    @Override
    public McpBusinessException withDetail(String key, Object value) {
        addDetail(key, value);
        return this;
    }

    /**
     * Add multiple error details with fluent interface
     */
    @Override
    public McpBusinessException withDetails(Map<String, Object> details) {
        addDetails(details);
        return this;
    }

    // MCP-specific factory methods

    /**
     * Create exception for organization-related errors
     */
    public static McpBusinessException organizationNotFound(String organizationId) {
        return new McpBusinessException(
                String.format("Organization not found with id: %s", organizationId),
                ErrorCodes.ORG_NOT_FOUND).withDetail("organizationId", organizationId);
    }

    /**
     * Create exception for organization access denied
     */
    public static McpBusinessException organizationAccessDenied(String organizationId, String userId) {
        return new McpBusinessException(
                String.format("Access denied to organization %s for user %s", organizationId, userId),
                ErrorCodes.AUTH_INSUFFICIENT_PERMISSIONS).withDetail("organizationId", organizationId)
                .withDetail("userId", userId);
    }

    /**
     * Create exception for LLM provider errors
     */
    public static McpBusinessException llmProviderError(String provider, String reason) {
        return new McpBusinessException(
                String.format("LLM provider '%s' error: %s", provider, reason),
                ErrorCodes.LLM_PROVIDER_NOT_FOUND).withDetail("provider", provider)
                .withDetail("reason", reason);
    }

    /**
     * Create exception for debate-related errors
     */
    public static McpBusinessException debateInvalidState(String debateId, String currentState, String expectedState) {
        return new McpBusinessException(
                String.format("Debate %s is in invalid state. Current: %s, Expected: %s", debateId, currentState,
                        expectedState),
                ErrorCodes.DEBATE_INVALID_STATE).withDetail("debateId", debateId)
                .withDetail("currentState", currentState)
                .withDetail("expectedState", expectedState);
    }

    /**
     * Create exception for RAG-related errors
     */
    public static McpBusinessException ragDocumentError(String documentId, String reason) {
        return new McpBusinessException(
                String.format("RAG document error for %s: %s", documentId, reason),
                ErrorCodes.RAG_DOCUMENT_NOT_FOUND).withDetail("documentId", documentId)
                .withDetail("reason", reason);
    }

    /**
     * Create exception for template-related errors
     */
    public static McpBusinessException templateError(String templateId, String reason) {
        return new McpBusinessException(
                String.format("Template error for %s: %s", templateId, reason),
                ErrorCodes.TEMPLATE_NOT_FOUND).withDetail("templateId", templateId)
                .withDetail("reason", reason);
    }

    /**
     * Create exception for context-related errors
     */
    public static McpBusinessException contextError(String contextId, String reason) {
        return new McpBusinessException(
                String.format("Context error for %s: %s", contextId, reason),
                ErrorCodes.CONTEXT_NOT_FOUND).withDetail("contextId", contextId)
                .withDetail("reason", reason);
    }

    /**
     * Create exception for rate limiting
     */
    public static McpBusinessException rateLimitExceeded(String resource, String limit) {
        return new McpBusinessException(
                String.format("Rate limit exceeded for %s. Limit: %s", resource, limit),
                ErrorCodes.RATE_LIMIT_EXCEEDED).withDetail("resource", resource)
                .withDetail("limit", limit);
    }

    /**
     * Create exception for external service errors
     */
    public static McpBusinessException externalServiceError(String service, String reason) {
        return new McpBusinessException(
                String.format("External service '%s' error: %s", service, reason),
                ErrorCodes.EXTERNAL_SERVICE_ERROR).withDetail("service", service)
                .withDetail("reason", reason);
    }
}