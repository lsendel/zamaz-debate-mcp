package com.zamaz.telemetry.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class TelemetryAnalysis {
    private final String analysisId;
    private final Instant startTime;
    private final Instant endTime;
    private final long dataPointCount;
    private final Map<String, Double> averageMetrics;
    private final Map<String, Double> minMetrics;
    private final Map<String, Double> maxMetrics;
    private final Map<String, Double> standardDeviationMetrics;
    private final double dataQualityScore;
    
    public Duration getAnalysisDuration() {
        return Duration.between(startTime, endTime);
    }
    
    public double getDataRate() {
        long seconds = getAnalysisDuration().getSeconds();
        return seconds > 0 ? (double) dataPointCount / seconds : 0;
    }
}