package com.zamaz.mcp.controller.domain.analytics;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Analytics summary for all agentic flows used in a specific debate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateAgenticFlowAnalytics {
    private UUID debateId;
    private int totalExecutions;
    private Map<AgenticFlowType, AgenticFlowAnalyticsSummary> flowTypeSummaries;
    private double averageConfidence;
    private double successRate;
    private LocalDateTime timestamp;
    
    public AgenticFlowType getMostUsedFlowType() {
        return flowTypeSummaries.entrySet().stream()
            .max((a, b) -> Integer.compare(
                a.getValue().getExecutionCount(), 
                b.getValue().getExecutionCount()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    public AgenticFlowType getBestPerformingFlowType() {
        return flowTypeSummaries.entrySet().stream()
            .max((a, b) -> Double.compare(
                a.getValue().getAverageConfidence(), 
                b.getValue().getAverageConfidence()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}