package com.zamaz.mcp.controller.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * Metrics for factuality analysis
 */
@Data
@Builder
public class FactualityMetrics {
    
    private double factualAccuracy;
    private double sourceReliability;
    private double evidenceStrength;
    private double claimCertainty;
    private double logicalSoundness;
    private double verifiability;
    private Set<String> evidenceTypes;
    private List<String> redFlags;
    private List<String> factualClaims;
}