package com.zamaz.mcp.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard API response wrapper for all MCP services.
 * Provides consistent response structure across all endpoints.
 *
 * @param <T> Type of the response data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * Indicates if the request was successful
     */
    private boolean success;
    
    /**
     * Response data (only present on success)
     */
    private T data;
    
    /**
     * Error message (only present on failure)
     */
    private String error;
    
    /**
     * Error code for client handling
     */
    private String errorCode;
    
    /**
     * Additional error details
     */
    private Map<String, Object> errorDetails;
    
    /**
     * Request timestamp
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Request tracking ID
     */
    private String requestId;
    
    /**
     * Creates a successful response
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }
    
    /**
     * Creates a successful response with request ID
     */
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .requestId(requestId)
                .build();
    }
    
    /**
     * Creates an error response
     */
    public static <T> ApiResponse<T> error(String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .build();
    }
    
    /**
     * Creates an error response with error code
     */
    public static <T> ApiResponse<T> error(String error, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .errorCode(errorCode)
                .build();
    }
    
    /**
     * Creates an error response with full details
     */
    public static <T> ApiResponse<T> error(String error, String errorCode, Map<String, Object> errorDetails) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .build();
    }
}