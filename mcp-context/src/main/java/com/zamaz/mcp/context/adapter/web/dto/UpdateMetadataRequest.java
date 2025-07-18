package com.zamaz.mcp.context.adapter.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Web DTO for updating context metadata.
 */
public record UpdateMetadataRequest(
    @NotNull(message = "Metadata is required")
    Map<String, Object> metadata
) {
    public UpdateMetadataRequest {
        // Ensure metadata is never null
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}