package com.zamaz.mcp.controller.domain.recommendation;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a set of agentic flow recommendations for a debate or participant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowRecommendation {
    private DebateContext context;
    private ParticipantContext participantContext;
    private List<FlowTypeRecommendation> recommendations;
    private String reasoning;
    private LocalDateTime timestamp;
    
    public FlowTypeRecommendation getTopRecommendation() {
        return recommendations.isEmpty() ? null : recommendations.get(0);
    }
    
    public boolean hasHighConfidenceRecommendations() {
        return recommendations.stream()
            .anyMatch(r -> r.getScore() >= 0.8);
    }
}