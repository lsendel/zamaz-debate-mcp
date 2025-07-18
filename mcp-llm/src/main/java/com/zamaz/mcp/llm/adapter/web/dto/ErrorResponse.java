package com.zamaz.mcp.llm.adapter.web.dto;

import java.time.Instant;

/**
 * Standard error response DTO.
 */
public record ErrorResponse(
    String code,
    String message,
    String details,
    Instant timestamp,
    String path
) {
    
    public static ErrorResponse validation(String message, String path) {
        return new ErrorResponse(
            "VALIDATION_ERROR",
            message,
            null,
            Instant.now(),
            path
        );
    }
    
    public static ErrorResponse notFound(String message, String path) {
        return new ErrorResponse(
            "NOT_FOUND",
            message,
            null,
            Instant.now(),
            path
        );
    }
    
    public static ErrorResponse providerError(String message, String details, String path) {
        return new ErrorResponse(
            "PROVIDER_ERROR",
            message,
            details,
            Instant.now(),
            path
        );
    }
    
    public static ErrorResponse rateLimited(String message, String path) {
        return new ErrorResponse(
            "RATE_LIMITED",
            message,
            null,
            Instant.now(),
            path
        );
    }
    
    public static ErrorResponse internal(String message, String path) {
        return new ErrorResponse(
            "INTERNAL_ERROR",
            message,
            null,
            Instant.now(),
            path
        );
    }
}