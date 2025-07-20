package com.zamaz.mcp.rag.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for searching documents.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {
    
    @NotBlank(message = "Organization ID is required")
    private String organizationId;
    
    @NotBlank(message = "Query is required")
    private String query;
    
    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer limit = 10;
    
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Builder.Default
    private Double similarityThreshold = 0.7;
    
    private Boolean includeContent;
    private Boolean includeMetadata;
}