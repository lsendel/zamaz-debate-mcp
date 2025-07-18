package com.zamaz.mcp.debateengine.adapter.web.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Map;

/**
 * Request DTO for adding a participant to a debate.
 */
public record AddParticipantRequest(
    String userId,
    
    AIModelRequest aiModel,
    
    @NotNull(message = "Position is required")
    @Pattern(regexp = "PRO|CON|MODERATOR|JUDGE|OBSERVER", message = "Invalid position")
    String position
) {
    
    /**
     * AI model configuration request.
     */
    public record AIModelRequest(
        @NotNull(message = "Provider is required")
        String provider,
        
        @NotNull(message = "Model name is required")
        String name,
        
        Map<String, Object> config
    ) {}
    
    /**
     * Validate that either userId or aiModel is provided.
     */
    public boolean isValid() {
        return (userId != null && aiModel == null) || 
               (userId == null && aiModel != null);
    }
}