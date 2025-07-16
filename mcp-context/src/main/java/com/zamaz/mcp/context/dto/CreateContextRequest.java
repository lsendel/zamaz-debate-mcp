package com.zamaz.mcp.context.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating a new context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContextRequest {
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotBlank(message = "Context name is required")
    @Size(max = 255, message = "Context name must not exceed 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private Map<String, Object> metadata;
}