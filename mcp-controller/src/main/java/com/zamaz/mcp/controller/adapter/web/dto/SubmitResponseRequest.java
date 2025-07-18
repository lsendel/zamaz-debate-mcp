package com.zamaz.mcp.controller.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Min;

/**
 * Request DTO for submitting a response to a debate round.
 */
@Schema(description = "Request to submit a response to the current round")
public record SubmitResponseRequest(
    
    @Schema(description = "ID of the participant submitting the response")
    @NotBlank(message = "Participant ID cannot be blank")
    String participantId,
    
    @Schema(description = "Content of the response", example = "I believe that...")
    @NotBlank(message = "Content cannot be blank")
    @Size(min = 10, max = 50000, message = "Content must be between 10 and 50,000 characters")
    String content,
    
    @Schema(description = "Response time in seconds (optional)", example = "45")
    @Min(value = 0, message = "Response time cannot be negative")
    Long responseTimeSeconds
) {
    
    public static SubmitResponseRequest of(String participantId, String content) {
        return new SubmitResponseRequest(participantId, content, null);
    }
    
    public static SubmitResponseRequest of(String participantId, String content, Long responseTimeSeconds) {
        return new SubmitResponseRequest(participantId, content, responseTimeSeconds);
    }
    
    public boolean hasResponseTime() {
        return responseTimeSeconds != null;
    }
}