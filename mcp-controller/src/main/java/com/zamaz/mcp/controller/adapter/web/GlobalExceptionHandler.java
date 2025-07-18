package com.zamaz.mcp.controller.adapter.web;

import com.zamaz.mcp.controller.application.exception.DebateNotFoundException;
import com.zamaz.mcp.controller.application.exception.ParticipantNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the debate controller.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(DebateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDebateNotFound(DebateNotFoundException ex) {
        logger.warn("Debate not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "DEBATE_NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            Instant.now()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(ParticipantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleParticipantNotFound(ParticipantNotFoundException ex) {
        logger.warn("Participant not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "PARTICIPANT_NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            Instant.now()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            Instant.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        logger.warn("Invalid state: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "INVALID_STATE",
            ex.getMessage(),
            HttpStatus.CONFLICT.value(),
            Instant.now()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ValidationErrorResponse error = new ValidationErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            HttpStatus.BAD_REQUEST.value(),
            Instant.now(),
            errors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        logger.warn("Constraint violation: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "CONSTRAINT_VIOLATION",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            Instant.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        logger.error("Unexpected error", ex);
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            Instant.now()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    public record ErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp
    ) {}
    
    public record ValidationErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp,
        Map<String, String> fieldErrors
    ) {}
}