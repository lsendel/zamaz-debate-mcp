package com.zamaz.mcp.llm.adapter.web.dto;

import jakarta.validation.constraints.*;
import java.util.Optional;

/**
 * Web DTO for completion requests.
 */
public record CompletionRequest(
    @NotBlank(message = "Prompt is required")
    @Size(min = 1, max = 500000, message = "Prompt must be between 1 and 500,000 characters")
    String prompt,
    
    @Min(value = 1, message = "Max tokens must be at least 1")
    @Max(value = 100000, message = "Max tokens cannot exceed 100,000")
    int maxTokens,
    
    @DecimalMin(value = "0.0", message = "Temperature cannot be negative")
    @DecimalMax(value = "2.0", message = "Temperature cannot exceed 2.0")
    double temperature,
    
    String model,
    String provider,
    
    Boolean enableCaching,
    Boolean streaming
) {
    
    public CompletionRequest {
        // Set defaults for optional fields
        if (enableCaching == null) {
            enableCaching = true;
        }
        if (streaming == null) {
            streaming = false;
        }
    }
    
    public Optional<String> getModel() {
        return Optional.ofNullable(model);
    }
    
    public Optional<String> getProvider() {
        return Optional.ofNullable(provider);
    }
    
    public boolean isEnableCaching() {
        return Boolean.TRUE.equals(enableCaching);
    }
    
    public boolean isStreaming() {
        return Boolean.TRUE.equals(streaming);
    }
}