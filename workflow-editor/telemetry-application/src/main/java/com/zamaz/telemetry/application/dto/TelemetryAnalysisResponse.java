package com.zamaz.telemetry.application.dto;

import com.zamaz.telemetry.domain.entity.TelemetryAnalysis;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class TelemetryAnalysisResponse {
    private String analysisId;
    private Instant startTime;
    private Instant endTime;
    private long dataPointCount;
    private Map<String, Double> averageMetrics;
    private Map<String, Double> minMetrics;
    private Map<String, Double> maxMetrics;
    private Map<String, Double> standardDeviationMetrics;
    private double dataQualityScore;
    private long durationSeconds;
    private double dataRate;
    
    public static TelemetryAnalysisResponse from(TelemetryAnalysis analysis) {
        Duration duration = analysis.getAnalysisDuration();
        
        return TelemetryAnalysisResponse.builder()
                .analysisId(analysis.getAnalysisId())
                .startTime(analysis.getStartTime())
                .endTime(analysis.getEndTime())
                .dataPointCount(analysis.getDataPointCount())
                .averageMetrics(analysis.getAverageMetrics())
                .minMetrics(analysis.getMinMetrics())
                .maxMetrics(analysis.getMaxMetrics())
                .standardDeviationMetrics(analysis.getStandardDeviationMetrics())
                .dataQualityScore(analysis.getDataQualityScore())
                .durationSeconds(duration.getSeconds())
                .dataRate(analysis.getDataRate())
                .build();
    }
}