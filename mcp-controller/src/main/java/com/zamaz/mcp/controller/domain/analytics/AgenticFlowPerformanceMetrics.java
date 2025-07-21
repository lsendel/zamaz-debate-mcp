package com.zamaz.mcp.controller.domain.analytics;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain entity for detailed performance metrics of an agentic flow execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticFlowPerformanceMetrics {
    private UUID id;
    private UUID executionId;
    private Long llmResponseTime; // milliseconds
    private Long toolCallTime; // milliseconds
    private Integer totalTokens;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long memoryUsage; // bytes
    private LocalDateTime timestamp;
    
    public double getTokenEfficiency() {
        if (totalTokens == null || totalTokens == 0) {
            return 0.0;
        }
        return (double) completionTokens / totalTokens;
    }
    
    public long getTotalProcessingTime() {
        long total = 0;
        if (llmResponseTime != null) total += llmResponseTime;
        if (toolCallTime != null) total += toolCallTime;
        return total;
    }
}