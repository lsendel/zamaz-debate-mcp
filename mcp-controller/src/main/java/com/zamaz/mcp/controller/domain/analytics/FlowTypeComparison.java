package com.zamaz.mcp.controller.domain.analytics;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Comparison analytics between multiple agentic flow types.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowTypeComparison {
    private Set<AgenticFlowType> flowTypes;
    private Map<String, Object> comparisons;
    private String recommendation;
    private LocalDateTime timestamp;
    
    public AgenticFlowType getBestByConfidence() {
        return (AgenticFlowType) comparisons.get("bestByConfidence");
    }
    
    public AgenticFlowType getFastest() {
        return (AgenticFlowType) comparisons.get("fastestFlowType");
    }
    
    public AgenticFlowType getMostReliable() {
        return (AgenticFlowType) comparisons.get("mostReliableFlowType");
    }
}