package com.zamaz.mcp.controller.ai;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Metrics for sentiment analysis
 */
@Data
@Builder
public class SentimentMetrics {
    
    private double sentiment; // -1 (negative) to 1 (positive)
    private double confidence; // 0 to 1
    private Map<String, Double> emotions; // emotion name -> intensity
    private double toxicityScore; // 0 to 1
    private double professionalismScore; // 0 to 1
    private double polarityScore; // 0 to 1 (how extreme)
    private double subjectivityScore; // 0 (objective) to 1 (subjective)
}