package com.zamaz.mcp.context.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for appending a message to a context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppendMessageRequest {
    
    @NotNull(message = "Role is required")
    private String role;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    private Map<String, Object> metadata;
}