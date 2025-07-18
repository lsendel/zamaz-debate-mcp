package com.zamaz.mcp.controller.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Request DTO for joining a debate.
 */
@Schema(description = "Request to join a debate as a participant")
public record JoinDebateRequest(
    
    @Schema(description = "Name of the participant", example = "Alice")
    @NotBlank(message = "Participant name cannot be blank")
    @Size(max = 255, message = "Participant name cannot exceed 255 characters")
    String participantName,
    
    @Schema(description = "Type of participant", example = "human", allowableValues = {"human", "ai"})
    @NotBlank(message = "Participant type cannot be blank")
    String participantType,
    
    @Schema(description = "Position in the debate", example = "pro")
    @NotBlank(message = "Position cannot be blank")
    @Size(max = 1000, message = "Position cannot exceed 1000 characters")
    String position,
    
    @Schema(description = "LLM provider for AI participants", example = "claude")
    String provider,
    
    @Schema(description = "Provider configuration for AI participants")
    @Valid
    ProviderConfigDto providerConfig
) {
    
    public static JoinDebateRequest human(String participantName, String position) {
        return new JoinDebateRequest(participantName, "human", position, null, null);
    }
    
    public static JoinDebateRequest ai(String participantName, String position, 
                                     String provider, ProviderConfigDto config) {
        return new JoinDebateRequest(participantName, "ai", position, provider, config);
    }
    
    public boolean isHuman() {
        return "human".equalsIgnoreCase(participantType);
    }
    
    public boolean isAI() {
        return "ai".equalsIgnoreCase(participantType);
    }
}