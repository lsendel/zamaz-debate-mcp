package com.zamaz.mcp.rag.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for document search operations.
 */
public record SearchRequest(
    @NotBlank(message = "Query cannot be blank")
    String query,
    
    @Min(value = 1, message = "Max results must be at least 1")
    @Max(value = 100, message = "Max results cannot exceed 100")
    Integer maxResults,
    
    @Min(value = 0, message = "Min similarity must be between 0 and 1")
    @Max(value = 1, message = "Min similarity must be between 0 and 1")
    Double minSimilarity,
    
    String[] documentIds,
    
    String[] tags
) {
}