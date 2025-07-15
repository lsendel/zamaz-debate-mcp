package com.zamaz.mcp.common.config;

import com.zamaz.mcp.common.constant.ErrorCode;
import com.zamaz.mcp.common.dto.ApiResponse;
import com.zamaz.mcp.common.exception.BaseException;
import com.zamaz.mcp.common.exception.BusinessException;
import com.zamaz.mcp.common.exception.TechnicalException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for all MCP services.
 * Provides consistent error responses across all endpoints.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle business exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        String requestId = generateRequestId();
        log.warn("Business exception occurred. RequestId: {}, Path: {}, Error: {}", 
            requestId, request.getRequestURI(), ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .error(ex.getMessage())
            .errorCode(ex.getErrorCode())
            .errorDetails(ex.getErrorDetails())
            .requestId(requestId)
            .build();
            
        return ResponseEntity.status(getHttpStatusForBusinessError(ex.getErrorCode()))
            .body(response);
    }
    
    /**
     * Handle technical exceptions
     */
    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ApiResponse<Void>> handleTechnicalException(
            TechnicalException ex, HttpServletRequest request) {
        String requestId = generateRequestId();
        log.error("Technical exception occurred. RequestId: {}, Path: {}, Error: {}", 
            requestId, request.getRequestURI(), ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .error("An internal error occurred. Please try again later.")
            .errorCode(ex.getErrorCode())
            .requestId(requestId)
            .build();
            
        // Don't expose technical details to clients
        if (isDebugMode()) {
            response.setErrorDetails(ex.getErrorDetails());
        }
            
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
    
    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = generateRequestId();
        log.warn("Validation exception occurred. RequestId: {}, Path: {}", 
            requestId, request.getRequestURI());
        
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .error("Validation failed")
            .errorCode(ErrorCode.VALIDATION_FAILED)
            .errorDetails(errors)
            .requestId(requestId)
            .build();
            
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String requestId = generateRequestId();
        log.warn("Type mismatch exception occurred. RequestId: {}, Path: {}, Parameter: {}", 
            requestId, request.getRequestURI(), ex.getName());
        
        String error = String.format("Invalid value for parameter '%s': %s", 
            ex.getName(), ex.getValue());
            
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .error(error)
            .errorCode(ErrorCode.INVALID_REQUEST)
            .requestId(requestId)
            .build();
            
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle JSON parse exceptions
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleJsonParseException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String requestId = generateRequestId();
        log.warn("JSON parse exception occurred. RequestId: {}, Path: {}", 
            requestId, request.getRequestURI());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .error("Invalid JSON format in request body")
            .errorCode(ErrorCode.INVALID_REQUEST)
            .requestId(requestId)
            .build();
            
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        String requestId = generateRequestId();
        log.error("Unexpected exception occurred. RequestId: {}, Path: {}, Error: {}", 
            requestId, request.getRequestURI(), ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .error("An unexpected error occurred. Please try again later.")
            .errorCode(ErrorCode.INTERNAL_SERVER_ERROR)
            .requestId(requestId)
            .build();
            
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Generate a unique request ID for tracking
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Map business error codes to HTTP status codes
     */
    private HttpStatus getHttpStatusForBusinessError(String errorCode) {
        return switch (errorCode) {
            case ErrorCode.RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCode.RESOURCE_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case ErrorCode.ACCESS_DENIED, ErrorCode.INSUFFICIENT_PERMISSIONS -> HttpStatus.FORBIDDEN;
            case ErrorCode.AUTHENTICATION_FAILED -> HttpStatus.UNAUTHORIZED;
            case ErrorCode.RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
    
    /**
     * Check if debug mode is enabled
     */
    private boolean isDebugMode() {
        // This should be configured via application properties
        return false;
    }
}