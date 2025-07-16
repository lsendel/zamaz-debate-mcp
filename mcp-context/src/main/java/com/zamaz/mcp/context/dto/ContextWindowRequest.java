package com.zamaz.mcp.context.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for retrieving a context window.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextWindowRequest {
    
    @Min(value = 1, message = "Window size must be at least 1")
    @Max(value = 100000, message = "Window size cannot exceed 100000 tokens")
    @Builder.Default
    private Integer maxTokens = 4096;
    
    @Min(value = 0, message = "Message limit must be non-negative")
    @Max(value = 1000, message = "Message limit cannot exceed 1000")
    private Integer messageLimit;
    
    @Builder.Default
    private Boolean includeSystemMessages = true;
    
    @Builder.Default
    private Boolean preserveMessageBoundaries = true;
}