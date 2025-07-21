package com.zamaz.mcp.controller.domain.recommendation;

import lombok.*;

/**
 * Context information about a participant used for flow recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantContext {
    private String name;
    private String modelProvider;
    private String modelName;
    private ParticipantRole role;
    private double temperature;
    private int maxTokens;
    
    public boolean hasLargeModel() {
        return modelName != null && 
            (modelName.toLowerCase().contains("gpt-4") ||
             modelName.toLowerCase().contains("claude") ||
             modelName.toLowerCase().contains("large"));
    }
    
    public boolean isHighTemperature() {
        return temperature > 0.8;
    }
    
    public boolean isLowTemperature() {
        return temperature < 0.3;
    }
    
    public boolean hasHighTokenLimit() {
        return maxTokens >= 2000;
    }
}