package com.zamaz.mcp.debateengine.adapter.web.dto;

import javax.validation.constraints.*;
import java.util.Map;

/**
 * Request DTO for creating a debate.
 */
public record CreateDebateRequest(
    @NotBlank(message = "Topic is required")
    @Size(min = 10, max = 500, message = "Topic must be between 10 and 500 characters")
    String topic,
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    String description,
    
    @Min(value = 2, message = "Must have at least 2 participants")
    @Max(value = 10, message = "Cannot have more than 10 participants")
    Integer maxParticipants,
    
    @Min(value = 1, message = "Must have at least 1 round")
    @Max(value = 20, message = "Cannot have more than 20 rounds")
    Integer maxRounds,
    
    @Min(value = 60000, message = "Round timeout must be at least 1 minute")
    @Max(value = 3600000, message = "Round timeout cannot exceed 1 hour")
    Long roundTimeoutMs,
    
    @Pattern(regexp = "PUBLIC|PRIVATE|ORGANIZATION", message = "Invalid visibility")
    String visibility,
    
    Map<String, Object> settings
) {
    
    /**
     * Create with defaults.
     */
    public static CreateDebateRequest withDefaults(String topic, String description) {
        return new CreateDebateRequest(
            topic,
            description,
            2,
            5,
            300000L, // 5 minutes
            "PRIVATE",
            Map.of()
        );
    }
}