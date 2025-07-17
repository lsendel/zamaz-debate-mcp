package com.zamaz.mcp.controller.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Metrics for coherence analysis
 */
@Data
@Builder
public class CoherenceMetrics {
    
    private double logicalFlow;
    private double structuralCoherence;
    private double transitionQuality;
    private double topicConsistency;
    private double argumentProgression;
    private double cohesionScore;
    private double clarityScore;
}