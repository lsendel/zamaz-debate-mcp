package com.zamaz.mcp.common.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.zamaz.mcp.common.architecture.exception.BaseException;
import com.zamaz.mcp.common.architecture.exception.BusinessException;
import com.zamaz.mcp.common.architecture.exception.TechnicalException;
import com.zamaz.mcp.common.architecture.exception.ValidationException;
import com.zamaz.mcp.common.architecture.exception.ExternalServiceException;
import com.zamaz.mcp.common.infrastructure.logging.StructuredLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for all microservices using RFC 7807 ProblemDetail.
 * This handler provides consistent error responses across the entire application.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class StandardGlobalExceptionHandler {

    private final StructuredLogger structuredLogger;

    /**
     * Handle custom business exceptions.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        ProblemDetail problem = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request.getRequestURI(),
            ex.getErrorCode(),
            ex.getDetails()
        );
        
        structuredLogger.warn("Business exception occurred")
            .field("errorCode", ex.getErrorCode())
            .field("message", ex.getMessage())
            .field("requestUri", request.getRequestURI())
            .field("details", ex.getDetails())
            .log();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handle technical exceptions.
     */
    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ProblemDetail> handleTechnicalException(
            TechnicalException ex, HttpServletRequest request) {
        
        ProblemDetail problem = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "A technical error occurred",
            request.getRequestURI(),
            ex.getErrorCode(),
            Map.of("originalMessage", ex.getMessage())
        );
        
        structuredLogger.error("Technical exception occurred", ex)
            .field("errorCode", ex.getErrorCode())
            .field("message", ex.getMessage())
            .field("requestUri", request.getRequestURI())
            .field("details", ex.getDetails())
            .log();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
        ProblemDetail problem = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request.getRequestURI(),
            ex.getErrorCode(),
            ex.getDetails()
        );
        
        structuredLogger.warn("Validation exception occurred")
            .field("errorCode", ex.getErrorCode())
            .field("message", ex.getMessage())
            .field("requestUri", request.getRequestURI())
            .field("violations", ex.getDetails())
            .log();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handle external service exceptions.
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ProblemDetail> handleExternalServiceException(
            ExternalServiceException ex, HttpServletRequest request) {
        
        ProblemDetail problem = createProblemDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "External service temporarily unavailable",
            request.getRequestURI(),
            "EXTERNAL_SERVICE_ERROR",
            Map.of(
                "service", ex.getServiceName(),
                "originalMessage", ex.getMessage()
            )
        );
        
        structuredLogger.error("External service exception occurred", ex)
            .field("service", ex.getServiceName())
            .field("message", ex.getMessage())
            .field("requestUri", request.getRequestURI())
            .log();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    /**
     * Handle method argument validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        
        ProblemDetail problem = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            request.getRequestURI(),
            "VALIDATION_ERROR",
            Map.of("fieldErrors", fieldErrors)
        );
        
        structuredLogger.warn("Method argument validation failed")
            .field("fieldErrors", fieldErrors)
            .field("requestUri", request.getRequestURI())
            .log();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handle constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        Map<String, String> violations = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            violations.put(propertyPath, violation.getMessage());
        }
        
        ProblemDetail problem = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Constraint validation failed",
            request.getRequestURI(),
            "CONSTRAINT_VIOLATION",
            Map.of("violations", violations)
        );
        
        structuredLogger.warn("Constraint validation failed")
            .field("violations", violations)
            .field("requestUri", request.getRequestURI())
            .log();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        ProblemDetail problem = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Missing required parameter: " + ex.getParameterName(),
            request.getRequestURI(),
            "MISSING_PARAMETER",
            Map.of(
                "parameterName", ex.getParameterName(),
                "parameterType", ex.getParameterType()
            )
        );
        
        structuredLogger.warn("Missing request parameter")
            .field("parameterName", ex.getParameterName())
            .field("parameterType", ex.getParameterType())
            .field("requestUri", request.getRequestURI())
            .log();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handle method argument type mismatches.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        
        ProblemDetail problem = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Invalid parameter type for: " + ex.getName(),
            request.getRequestURI(),
            "PARAMETER_TYPE_MISMATCH",
            Map.of(
                "parameterName", ex.getName(),
                "providedValue", ex.getValue() != null ? ex.getValue().toString() : "null",
                "expectedType", expectedType
            )
        );
        
        structuredLogger.warn("Method argument type mismatch")
            .field("parameterName", ex.getName())
            .field("providedValue", ex.getValue())
            .field("expectedType", expectedType)
            .field("requestUri", request.getRequestURI())
            .log();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handle malformed JSON requests.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String detailMessage = "Malformed JSON request";
        Map<String, Object> details = new HashMap<>();
        
        if (ex.getCause() instanceof InvalidFormatException formatEx) {
            detailMessage = "Invalid format for field: " + formatEx.getPath().stream()
                .map(ref -> ref.getFieldName())
                .collect(Collectors.joining("."));
            details.put("field", formatEx.getPath().stream()
                .map(ref -> ref.getFieldName())
                .collect(Collectors.joining(".")));
            details.put("providedValue", formatEx.getValue());
            details.put("expectedType", formatEx.getTargetType().getSimpleName());
        } else if (ex.getCause() instanceof JsonMappingException mappingEx) {
            detailMessage = "JSON mapping error: " + mappingEx.getOriginalMessage();
            details.put("path", mappingEx.getPath().stream()
                .map(ref -> ref.getFieldName())
                .collect(Collectors.joining(".")));
        }
        
        ProblemDetail problem = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            detailMessage,
            request.getRequestURI(),
            "MALFORMED_REQUEST",
            details
        );
        
        structuredLogger.warn("Malformed request body")
            .field("details", details)
            .field("requestUri", request.getRequestURI())
            .log();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handle 404 - Not Found errors.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        ProblemDetail problem = createProblemDetail(
            HttpStatus.NOT_FOUND,
            "No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL(),
            request.getRequestURI(),
            "NOT_FOUND",
            Map.of(
                "method", ex.getHttpMethod(),
                "url", ex.getRequestURL()
            )
        );
        
        structuredLogger.warn("No handler found")
            .field("method", ex.getHttpMethod())
            .field("url", ex.getRequestURL())
            .field("requestUri", request.getRequestURI())
            .log();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        ProblemDetail problem = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            request.getRequestURI(),
            "INTERNAL_SERVER_ERROR",
            Map.of("exceptionType", ex.getClass().getSimpleName())
        );
        
        structuredLogger.error("Unexpected exception occurred", ex)
            .field("exceptionType", ex.getClass().getSimpleName())
            .field("message", ex.getMessage())
            .field("requestUri", request.getRequestURI())
            .log();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    /**
     * Create a standardized ProblemDetail instance.
     */
    private ProblemDetail createProblemDetail(
            HttpStatus status,
            String detail,
            String instance,
            String errorCode,
            Map<String, Object> properties) {
        
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(getDefaultTitle(status));
        problem.setDetail(detail);
        problem.setInstance(URI.create(instance));
        
        // Add custom properties
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now().toString());
        
        if (properties != null && !properties.isEmpty()) {
            properties.forEach(problem::setProperty);
        }
        
        return problem;
    }

    /**
     * Get default title for HTTP status.
     */
    private String getDefaultTitle(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Bad Request";
            case NOT_FOUND -> "Not Found";
            case INTERNAL_SERVER_ERROR -> "Internal Server Error";
            case SERVICE_UNAVAILABLE -> "Service Unavailable";
            default -> status.getReasonPhrase();
        };
    }
}