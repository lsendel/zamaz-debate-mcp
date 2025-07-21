package com.zamaz.mcp.controller.domain.recommendation;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import lombok.*;

import java.util.List;

/**
 * Individual flow type recommendation with scoring and reasoning.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowTypeRecommendation {
    private AgenticFlowType flowType;
    private double score; // 0.0 to 1.0
    private List<String> reasons;
    private List<String> expectedBenefits;
    private List<String> potentialDrawbacks;
    
    public boolean isHighlyRecommended() {
        return score >= 0.8;
    }
    
    public boolean isRecommended() {
        return score >= 0.6;
    }
    
    public String getRecommendationLevel() {
        if (score >= 0.8) return "Highly Recommended";
        if (score >= 0.6) return "Recommended";
        if (score >= 0.4) return "Suitable";
        return "Not Recommended";
    }
}