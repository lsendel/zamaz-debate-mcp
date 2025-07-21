package com.zamaz.mcp.controller.domain.recommendation;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Real-time adaptive flow recommendations based on current performance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveFlowRecommendation {
    private UUID debateId;
    private UUID participantId;
    private AgenticFlowType currentFlowType;
    private PerformanceAnalysis performanceAnalysis;
    private List<FlowTypeRecommendation> recommendations;
    private boolean shouldSwitch;
    private SwitchUrgency switchUrgency;
    private LocalDateTime timestamp;
    
    public boolean requiresImmediateAction() {
        return shouldSwitch && switchUrgency == SwitchUrgency.HIGH;
    }
    
    public FlowTypeRecommendation getBestAlternative() {
        return recommendations.isEmpty() ? null : recommendations.get(0);
    }
}