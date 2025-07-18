package com.zamaz.mcp.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standard API response wrapper for consistent response format across all services.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class StandardApiResponse<T> {

    @Schema(description = "Response data", nullable = true)
    private T data;

    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response timestamp", example = "2024-01-15T10:30:00Z")
    private Instant timestamp;

    @Schema(description = "Request ID for tracing", example = "abc-123-def")
    private String requestId;

    @Schema(description = "API version", example = "1.0.0")
    private String apiVersion;

    @Schema(description = "Service name", example = "mcp-organization")
    private String serviceName;

    @Schema(description = "Additional metadata", nullable = true)
    private Map<String, Object> metadata;

    @Schema(description = "Pagination information", nullable = true)
    private PaginationInfo pagination;

    @Schema(description = "Validation errors", nullable = true)
    private List<ValidationError> errors;

    @Schema(description = "Warning messages", nullable = true)
    private List<String> warnings;

    /**
     * Create a successful response with data.
     */
    public static <T> StandardApiResponse<T> success(T data) {
        return StandardApiResponse.<T>builder()
            .data(data)
            .message("Success")
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create a successful response with data and message.
     */
    public static <T> StandardApiResponse<T> success(T data, String message) {
        return StandardApiResponse.<T>builder()
            .data(data)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create a successful response with pagination.
     */
    public static <T> StandardApiResponse<T> success(T data, PaginationInfo pagination) {
        return StandardApiResponse.<T>builder()
            .data(data)
            .message("Success")
            .timestamp(Instant.now())
            .pagination(pagination)
            .build();
    }

    /**
     * Create an error response.
     */
    public static <T> StandardApiResponse<T> error(String message) {
        return StandardApiResponse.<T>builder()
            .message(message)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create an error response with validation errors.
     */
    public static <T> StandardApiResponse<T> error(String message, List<ValidationError> errors) {
        return StandardApiResponse.<T>builder()
            .message(message)
            .timestamp(Instant.now())
            .errors(errors)
            .build();
    }

    /**
     * Create a response with warnings.
     */
    public static <T> StandardApiResponse<T> withWarnings(T data, List<String> warnings) {
        return StandardApiResponse.<T>builder()
            .data(data)
            .message("Success with warnings")
            .timestamp(Instant.now())
            .warnings(warnings)
            .build();
    }

    /**
     * Add metadata to the response.
     */
    public StandardApiResponse<T> withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Add request ID to the response.
     */
    public StandardApiResponse<T> withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Add API version to the response.
     */
    public StandardApiResponse<T> withApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    /**
     * Add service name to the response.
     */
    public StandardApiResponse<T> withServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    /**
     * Pagination information.
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Pagination information")
    public static class PaginationInfo {
        
        @Schema(description = "Current page number (0-based)", example = "0")
        private int page;
        
        @Schema(description = "Number of items per page", example = "20")
        private int size;
        
        @Schema(description = "Total number of items", example = "100")
        private long totalElements;
        
        @Schema(description = "Total number of pages", example = "5")
        private int totalPages;
        
        @Schema(description = "Whether there is a next page", example = "true")
        private boolean hasNext;
        
        @Schema(description = "Whether there is a previous page", example = "false")
        private boolean hasPrevious;
        
        @Schema(description = "Whether this is the first page", example = "true")
        private boolean isFirst;
        
        @Schema(description = "Whether this is the last page", example = "false")
        private boolean isLast;
    }

    /**
     * Validation error information.
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Validation error information")
    public static class ValidationError {
        
        @Schema(description = "Field name", example = "email")
        private String field;
        
        @Schema(description = "Error message", example = "Invalid email format")
        private String message;
        
        @Schema(description = "Error code", example = "INVALID_FORMAT")
        private String code;
        
        @Schema(description = "Rejected value", example = "invalid-email")
        private Object rejectedValue;
    }
}