package com.zamaz.mcp.common.application.service;

import java.util.List;
import java.util.Map;

/**
 * Application service for validation.
 * Provides validation capabilities for use cases and commands.
 */
public interface ValidationService {
    
    /**
     * Validates an object and returns validation errors.
     * 
     * @param object the object to validate
     * @return map of field names to error messages, empty if valid
     */
    Map<String, List<String>> validate(Object object);
    
    /**
     * Validates an object and throws exception if invalid.
     * 
     * @param object the object to validate
     * @throws ValidationException if validation fails
     */
    void validateOrThrow(Object object);
    
    /**
     * Checks if an object is valid.
     * 
     * @param object the object to validate
     * @return true if valid, false otherwise
     */
    boolean isValid(Object object);
    
    /**
     * Exception thrown when validation fails.
     */
    class ValidationException extends RuntimeException {
        private final Map<String, List<String>> errors;
        
        public ValidationException(Map<String, List<String>> errors) {
            super("Validation failed: " + errors);
            this.errors = errors;
        }
        
        public Map<String, List<String>> getErrors() {
            return errors;
        }
    }
}