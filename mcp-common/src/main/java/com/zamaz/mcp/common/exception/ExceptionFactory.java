package com.zamaz.mcp.common.exception;

import com.zamaz.mcp.common.architecture.exception.BusinessException;
import com.zamaz.mcp.common.architecture.exception.ExternalServiceException;
import com.zamaz.mcp.common.architecture.exception.TechnicalException;
import com.zamaz.mcp.common.architecture.exception.ValidationException;

import java.util.Map;

/**
 * Factory class for creating standardized exceptions with consistent error codes.
 * This class provides convenience methods for creating common exception types.
 */
public final class ExceptionFactory {

    // Authentication & Authorization
    public static BusinessException invalidCredentials() {
        return new BusinessException(
            ErrorCodes.AUTH_INVALID_CREDENTIALS,
            "Invalid username or password"
        );
    }

    public static BusinessException tokenExpired() {
        return new BusinessException(
            ErrorCodes.AUTH_TOKEN_EXPIRED,
            "Authentication token has expired"
        );
    }

    public static BusinessException insufficientPermissions() {
        return new BusinessException(
            ErrorCodes.AUTH_INSUFFICIENT_PERMISSIONS,
            "Insufficient permissions for this operation"
        );
    }

    public static BusinessException userNotFound(String userId) {
        return new BusinessException(
            ErrorCodes.AUTH_USER_NOT_FOUND,
            "User not found: " + userId,
            Map.of("userId", userId)
        );
    }

    // Organization Service
    public static BusinessException organizationNotFound(String orgId) {
        return new BusinessException(
            ErrorCodes.ORG_NOT_FOUND,
            "Organization not found: " + orgId,
            Map.of("organizationId", orgId)
        );
    }

    public static BusinessException organizationAlreadyExists(String domain) {
        return new BusinessException(
            ErrorCodes.ORG_ALREADY_EXISTS,
            "Organization already exists for domain: " + domain,
            Map.of("domain", domain)
        );
    }

    public static BusinessException memberLimitExceeded(String orgId, int limit) {
        return new BusinessException(
            ErrorCodes.ORG_MEMBER_LIMIT_EXCEEDED,
            "Member limit exceeded for organization: " + orgId,
            Map.of("organizationId", orgId, "limit", limit)
        );
    }

    // LLM Service
    public static BusinessException providerNotFound(String provider) {
        return new BusinessException(
            ErrorCodes.LLM_PROVIDER_NOT_FOUND,
            "LLM provider not found: " + provider,
            Map.of("provider", provider)
        );
    }

    public static BusinessException modelNotFound(String model) {
        return new BusinessException(
            ErrorCodes.LLM_MODEL_NOT_FOUND,
            "LLM model not found: " + model,
            Map.of("model", model)
        );
    }

    public static BusinessException rateLimitExceeded(String provider, int limit) {
        return new BusinessException(
            ErrorCodes.LLM_RATE_LIMIT_EXCEEDED,
            "Rate limit exceeded for provider: " + provider,
            Map.of("provider", provider, "limit", limit)
        );
    }

    public static BusinessException apiKeyInvalid(String provider) {
        return new BusinessException(
            ErrorCodes.LLM_API_KEY_INVALID,
            "Invalid API key for provider: " + provider,
            Map.of("provider", provider)
        );
    }

    public static BusinessException requestTooLarge(int size, int maxSize) {
        return new BusinessException(
            ErrorCodes.LLM_REQUEST_TOO_LARGE,
            "Request size exceeds maximum allowed size",
            Map.of("size", size, "maxSize", maxSize)
        );
    }

    // Debate Service
    public static BusinessException debateNotFound(String debateId) {
        return new BusinessException(
            ErrorCodes.DEBATE_NOT_FOUND,
            "Debate not found: " + debateId,
            Map.of("debateId", debateId)
        );
    }

    public static BusinessException debateAlreadyStarted(String debateId) {
        return new BusinessException(
            ErrorCodes.DEBATE_ALREADY_STARTED,
            "Debate already started: " + debateId,
            Map.of("debateId", debateId)
        );
    }

    public static BusinessException debateInvalidState(String debateId, String currentState, String expectedState) {
        return new BusinessException(
            ErrorCodes.DEBATE_INVALID_STATE,
            "Debate is in invalid state. Expected: " + expectedState + ", Current: " + currentState,
            Map.of("debateId", debateId, "currentState", currentState, "expectedState", expectedState)
        );
    }

    public static BusinessException participantLimitExceeded(String debateId, int limit) {
        return new BusinessException(
            ErrorCodes.DEBATE_PARTICIPANT_LIMIT_EXCEEDED,
            "Participant limit exceeded for debate: " + debateId,
            Map.of("debateId", debateId, "limit", limit)
        );
    }

    public static BusinessException turnTooLong(String debateId, int length, int maxLength) {
        return new BusinessException(
            ErrorCodes.DEBATE_TURN_TOO_LONG,
            "Turn exceeds maximum length",
            Map.of("debateId", debateId, "length", length, "maxLength", maxLength)
        );
    }

    // RAG Service
    public static BusinessException documentNotFound(String documentId) {
        return new BusinessException(
            ErrorCodes.RAG_DOCUMENT_NOT_FOUND,
            "Document not found: " + documentId,
            Map.of("documentId", documentId)
        );
    }

    public static BusinessException documentTooLarge(String filename, long size, long maxSize) {
        return new BusinessException(
            ErrorCodes.RAG_DOCUMENT_TOO_LARGE,
            "Document size exceeds maximum allowed size",
            Map.of("filename", filename, "size", size, "maxSize", maxSize)
        );
    }

    public static TechnicalException embeddingFailed(String text, String provider, Exception cause) {
        return new TechnicalException(
            ErrorCodes.RAG_EMBEDDING_FAILED,
            "Failed to generate embeddings using provider: " + provider,
            Map.of("provider", provider, "textLength", text.length()),
            cause
        );
    }

    public static TechnicalException vectorStoreUnavailable(String store, Exception cause) {
        return new TechnicalException(
            ErrorCodes.RAG_VECTOR_STORE_UNAVAILABLE,
            "Vector store is unavailable: " + store,
            Map.of("store", store),
            cause
        );
    }

    // Template Service
    public static BusinessException templateNotFound(String templateId) {
        return new BusinessException(
            ErrorCodes.TEMPLATE_NOT_FOUND,
            "Template not found: " + templateId,
            Map.of("templateId", templateId)
        );
    }

    public static BusinessException templateInvalidSyntax(String templateId, String error) {
        return new BusinessException(
            ErrorCodes.TEMPLATE_INVALID_SYNTAX,
            "Template has invalid syntax: " + error,
            Map.of("templateId", templateId, "syntaxError", error)
        );
    }

    public static BusinessException templateVariableMissing(String templateId, String variable) {
        return new BusinessException(
            ErrorCodes.TEMPLATE_VARIABLE_MISSING,
            "Required template variable missing: " + variable,
            Map.of("templateId", templateId, "variable", variable)
        );
    }

    // External Services
    public static ExternalServiceException externalServiceTimeout(String service, long timeout) {
        return new ExternalServiceException(
            service,
            "External service timeout after " + timeout + "ms"
        );
    }

    public static ExternalServiceException externalServiceUnavailable(String service) {
        return new ExternalServiceException(
            service,
            "External service is unavailable"
        );
    }

    public static ExternalServiceException externalServiceAuthFailed(String service) {
        return new ExternalServiceException(
            service,
            "Authentication failed with external service"
        );
    }

    // Validation
    public static ValidationException validationFailed(String message, Map<String, String> fieldErrors) {
        return new ValidationException(
            ErrorCodes.VALIDATION_ERROR,
            message,
            fieldErrors
        );
    }

    public static ValidationException constraintViolation(String message, Map<String, String> violations) {
        return new ValidationException(
            ErrorCodes.CONSTRAINT_VIOLATION,
            message,
            violations
        );
    }

    // Rate Limiting
    public static BusinessException rateLimitExceeded(String resource, int limit, String window) {
        return new BusinessException(
            ErrorCodes.RATE_LIMIT_EXCEEDED,
            "Rate limit exceeded for resource: " + resource,
            Map.of("resource", resource, "limit", limit, "window", window)
        );
    }

    private ExceptionFactory() {
        // Utility class - prevent instantiation
    }
}