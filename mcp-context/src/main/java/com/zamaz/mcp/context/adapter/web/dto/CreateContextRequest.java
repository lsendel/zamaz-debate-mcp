package com.zamaz.mcp.context.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Web DTO for creating a new context.
 */
public record CreateContextRequest(
    @NotBlank(message = "Name is required")
    String name,
    
    Map<String, Object> metadata
) {
    public CreateContextRequest {
        // Ensure metadata is never null
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}