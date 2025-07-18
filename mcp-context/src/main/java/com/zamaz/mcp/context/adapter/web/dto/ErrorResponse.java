package com.zamaz.mcp.context.adapter.web.dto;

import java.time.Instant;

/**
 * Standard error response DTO for consistent error handling across the API.
 */
public record ErrorResponse(
    String code,
    String message,
    Instant timestamp,
    String path
) {
    /**
     * Creates an error response with the current timestamp.
     */
    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, Instant.now(), path);
    }
    
    /**
     * Creates an error response for validation errors.
     */
    public static ErrorResponse validation(String message, String path) {
        return of("VALIDATION_ERROR", message, path);
    }
    
    /**
     * Creates an error response for not found errors.
     */
    public static ErrorResponse notFound(String message, String path) {
        return of("NOT_FOUND", message, path);
    }
    
    /**
     * Creates an error response for unauthorized access.
     */
    public static ErrorResponse unauthorized(String message, String path) {
        return of("UNAUTHORIZED", message, path);
    }
    
    /**
     * Creates an error response for internal server errors.
     */
    public static ErrorResponse internal(String message, String path) {
        return of("INTERNAL_ERROR", message, path);
    }
}