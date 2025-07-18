package com.zamaz.mcp.debateengine.adapter.web.dto;

import javax.validation.constraints.*;

/**
 * Request DTO for submitting a response in a debate round.
 */
public record SubmitResponseRequest(
    @NotBlank(message = "Participant ID is required")
    String participantId,
    
    @NotBlank(message = "Content is required")
    @Size(min = 10, max = 10000, message = "Response must be between 10 and 10000 characters")
    String content,
    
    @Min(value = 0, message = "Response time cannot be negative")
    long responseTimeMs,
    
    @Min(value = 0, message = "Token count cannot be negative")
    int tokenCount
) {
}