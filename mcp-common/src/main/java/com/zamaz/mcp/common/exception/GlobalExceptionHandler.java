package com.zamaz.mcp.common.exception;

import com.zamaz.mcp.gateway.exception.AuthenticationException;
import com.zamaz.mcp.gateway.exception.OAuth2AuthenticationException;
import com.zamaz.mcp.gateway.exception.UserManagementException;
import com.zamaz.mcp.controller.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;
import java.time.Instant;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.of(problemDetail).build();
    }

    @ExceptionHandler(UserManagementException.class)
    public ResponseEntity<ProblemDetail> handleUserManagementException(UserManagementException ex) {
        log.warn("User management error: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("User Management Error");
        problemDetail.setType(URI.create("/errors/user-management"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.of(problemDetail).build();
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication error: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setTitle("Authentication Error");
        problemDetail.setType(URI.create("/errors/authentication"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.of(problemDetail).build();
    }

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleOAuth2AuthenticationException(OAuth2AuthenticationException ex) {
        log.warn("OAuth2 authentication error: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setTitle("OAuth2 Authentication Error");
        problemDetail.setType(URI.create("/errors/oauth2-authentication"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.of(problemDetail).build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("/errors/validation"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return ResponseEntity.of(problemDetail).build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("/errors/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.of(problemDetail).build();
    }

    @ExceptionHandler(ToolCallException.class)
    public ResponseEntity<ProblemDetail> handleToolCallException(ToolCallException ex) {
        log.warn("Tool call error: {}", ex.getMessage());
        return ResponseEntity.of(ex.getProblemDetail()).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("/errors/internal-server-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.of(problemDetail).build();
    }
}
