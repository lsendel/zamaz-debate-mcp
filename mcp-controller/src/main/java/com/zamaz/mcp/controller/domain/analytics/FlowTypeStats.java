package com.zamaz.mcp.controller.domain.analytics;

import lombok.*;

import java.time.Duration;

/**
 * Statistical data for a specific flow type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowTypeStats {
    private int usageCount;
    private double averageConfidence;
    private double successRate;
    private Duration averageExecutionTime;
    
    public double getEfficiencyScore() {
        // Combine confidence and speed into efficiency metric
        double speedScore = Math.max(0, 1.0 - (averageExecutionTime.toMillis() / 10000.0));
        return (averageConfidence / 100.0) * 0.7 + speedScore * 0.3;
    }
}