package com.zamaz.mcp.controller.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Metrics for argument quality analysis
 */
@Data
@Builder
public class ArgumentQualityMetrics {
    
    private double logicalStrength;
    private double evidenceQuality;
    private double clarityScore;
    private double relevanceScore;
    private double originalityScore;
    private List<String> logicalFallacies;
    private ArgumentStructure argumentStructure;
}