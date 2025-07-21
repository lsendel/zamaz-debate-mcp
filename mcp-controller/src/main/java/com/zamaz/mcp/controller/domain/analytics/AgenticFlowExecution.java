package com.zamaz.mcp.controller.domain.analytics;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Domain entity representing the execution of an agentic flow.
 * Captures execution details and performance metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticFlowExecution {
    private UUID id;
    private UUID flowId;
    private UUID debateId;
    private UUID organizationId;
    private AgenticFlowType flowType;
    private Duration executionTime;
    private AgenticFlowStatus status;
    private Double confidence;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    
    // Response improvement tracking
    private Double improvementScore;
    private String improvementReason;
    
    public boolean isSuccessful() {
        return AgenticFlowStatus.SUCCESS.equals(status);
    }
    
    public boolean hasHighConfidence() {
        return confidence != null && confidence >= 80.0;
    }
}