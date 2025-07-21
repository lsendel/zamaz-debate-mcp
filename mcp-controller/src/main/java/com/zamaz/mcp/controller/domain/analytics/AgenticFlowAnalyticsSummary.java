package com.zamaz.mcp.controller.domain.analytics;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import lombok.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Aggregated analytics summary for a specific agentic flow type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticFlowAnalyticsSummary {
    private AgenticFlowType flowType;
    private int executionCount;
    private double averageConfidence;
    private double successRate;
    private Duration averageExecutionTime;
    private Map<String, Object> metrics;
    private LocalDateTime timestamp;
    
    public static AgenticFlowAnalyticsSummary empty(AgenticFlowType flowType) {
        return AgenticFlowAnalyticsSummary.builder()
            .flowType(flowType)
            .executionCount(0)
            .averageConfidence(0.0)
            .successRate(0.0)
            .averageExecutionTime(Duration.ZERO)
            .metrics(new HashMap<>())
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public boolean hasSignificantData() {
        return executionCount >= 10;
    }
    
    public String getPerformanceGrade() {
        double score = (averageConfidence / 100.0) * 0.5 + successRate * 0.5;
        if (score >= 0.9) return "A";
        if (score >= 0.8) return "B";
        if (score >= 0.7) return "C";
        if (score >= 0.6) return "D";
        return "F";
    }
}