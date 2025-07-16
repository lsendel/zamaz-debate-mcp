package com.zamaz.mcp.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of template validation.
 * Contains validation status and any errors found.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateValidationResult {
    
    private boolean valid;
    private List<ValidationError> errors;
    private String message;
    
    /**
     * Create a valid result.
     */
    public static TemplateValidationResult valid() {
        return TemplateValidationResult.builder()
                .valid(true)
                .message("Template is valid")
                .build();
    }
    
    /**
     * Create an invalid result with message.
     */
    public static TemplateValidationResult invalid(String message) {
        return TemplateValidationResult.builder()
                .valid(false)
                .message(message)
                .build();
    }
    
    /**
     * Create an invalid result with errors.
     */
    public static TemplateValidationResult invalid(List<ValidationError> errors) {
        return TemplateValidationResult.builder()
                .valid(false)
                .errors(errors)
                .message("Template validation failed")
                .build();
    }
    
    /**
     * Validation error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String code;
        private String message;
        private Object value;
    }
}