package com.zamaz.mcp.controller.dto;

import com.zamaz.mcp.controller.ai.DebateStructureAnalysis;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object for debate analysis results
 */
@Data
@Builder
public class DebateAnalysisDto {
    
    private String debateId;
    private double overallQualityScore;
    private String qualityGrade;
    private Map<String, Double> argumentQualityScores;
    private Map<String, Double> coherenceScores;
    private Map<String, Object> sentimentAnalysis;
    private Map<String, Double> factualityScores;
    private DebateStructureAnalysis structureAnalysis;
    private Map<String, Object> llmInsights;
    private List<String> recommendations;
    private List<String> strengths;
    private List<String> weaknesses;
    private Map<String, Object> participantAnalysis;
    private LocalDateTime analysisTimestamp;
    private String analysisVersion;
    
    // Performance metrics
    private long analysisTimeMs;
    private String modelUsed;
    
    // Quality breakdown
    private QualityBreakdown qualityBreakdown;
    
    @Data
    @Builder
    public static class QualityBreakdown {
        private double argumentStrength;
        private double logicalCoherence;
        private double evidenceQuality;
        private double factualAccuracy;
        private double rhetoricalEffectiveness;
        private double engagement;
        private double civility;
        private double originalityScore;
    }
}