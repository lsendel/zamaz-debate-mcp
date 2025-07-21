package com.zamaz.mcp.controller.domain.analytics;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import lombok.*;

import java.time.Duration;

/**
 * Represents a trending agentic flow type based on usage and performance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingFlowType {
    private AgenticFlowType flowType;
    private int usageCount;
    private double averageConfidence;
    private double successRate;
    private Duration averageExecutionTime;
    private double trendScore; // 0.0 to 1.0
    
    public boolean isHighPerforming() {
        return averageConfidence >= 80.0 && successRate >= 0.8;
    }
    
    public String getTrendCategory() {
        if (trendScore >= 0.8) return "Hot";
        if (trendScore >= 0.6) return "Rising";
        if (trendScore >= 0.4) return "Stable";
        return "Declining";
    }
}