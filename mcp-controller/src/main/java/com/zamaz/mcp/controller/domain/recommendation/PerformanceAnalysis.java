package com.zamaz.mcp.controller.domain.recommendation;

import lombok.*;

import java.util.List;

/**
 * Analysis of current performance for adaptive recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceAnalysis {
    private List<String> issues;
    private List<String> strengths;
    private double overallScore; // 0.0 to 1.0
    private boolean hasSignificantIssues;
    
    public boolean isPerformanceGood() {
        return overallScore >= 0.7 && !hasSignificantIssues;
    }
    
    public boolean requiresIntervention() {
        return hasSignificantIssues || overallScore < 0.4;
    }
    
    public String getPerformanceGrade() {
        if (overallScore >= 0.9) return "Excellent";
        if (overallScore >= 0.7) return "Good";
        if (overallScore >= 0.5) return "Fair";
        if (overallScore >= 0.3) return "Poor";
        return "Critical";
    }
}