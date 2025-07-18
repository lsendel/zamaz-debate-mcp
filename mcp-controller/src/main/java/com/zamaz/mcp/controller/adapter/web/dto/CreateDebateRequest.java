package com.zamaz.mcp.controller.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Request DTO for creating a debate.
 */
@Schema(description = "Request to create a new debate")
public record CreateDebateRequest(
    
    @Schema(description = "The topic or question to debate", example = "Should artificial intelligence be regulated?")
    @NotBlank(message = "Topic cannot be blank")
    @Size(max = 1000, message = "Topic cannot exceed 1000 characters")
    String topic,
    
    @Schema(description = "Configuration for the debate")
    @Valid
    @NotNull(message = "Config cannot be null")
    DebateConfigDto config
) {
    
    public static CreateDebateRequest of(String topic, DebateConfigDto config) {
        return new CreateDebateRequest(topic, config);
    }
    
    public static CreateDebateRequest withDefaults(String topic) {
        return new CreateDebateRequest(topic, DebateConfigDto.defaultConfig());
    }
}