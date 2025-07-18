package com.zamaz.mcp.context.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Web DTO for appending a message to a context.
 */
public record AppendMessageRequest(
    @NotNull(message = "Role is required")
    @Pattern(regexp = "user|assistant|system|function", message = "Invalid role")
    String role,
    
    @NotBlank(message = "Content is required")
    String content,
    
    String model
) {}