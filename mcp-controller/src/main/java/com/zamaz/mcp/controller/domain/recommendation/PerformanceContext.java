package com.zamaz.mcp.controller.domain.recommendation;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import lombok.*;

/**
 * Current performance context for adaptive recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceContext {
    private AgenticFlowType currentFlowType;
    private int executionCount;
    private double averageConfidence;
    private double responseChangeRate;
    private long averageExecutionTime; // milliseconds
    private int recentErrors;
    private double recentSuccessRate;
    
    public boolean hasPerformanceIssues() {
        return averageConfidence < 60 || 
               responseChangeRate > 0.3 || 
               recentErrors > 2 ||
               recentSuccessRate < 0.7;
    }
    
    public boolean isPerformingWell() {
        return averageConfidence >= 80 && 
               responseChangeRate < 0.1 && 
               recentErrors == 0 &&
               recentSuccessRate >= 0.9;
    }
}