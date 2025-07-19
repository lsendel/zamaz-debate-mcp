package com.example.workflow.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Domain object representing telemetry data analysis results
 */
public class TelemetryAnalysis {
    
    private final String organizationId;
    private final Instant analysisTime;
    private final Instant fromTime;
    private final Instant toTime;
    private final List<DeviceId> analyzedDevices;
    private final Map<String, MetricAnalysis> metricAnalyses;
    private final List<TelemetryAnomaly> anomalies;
    private final List<TelemetryTrend> trends;
    private final AnalysisStatistics statistics;
    
    public TelemetryAnalysis(String organizationId, Instant fromTime, Instant toTime,
                           List<DeviceId> analyzedDevices, Map<String, MetricAnalysis> metricAnalyses,
                           List<TelemetryAnomaly> anomalies, List<TelemetryTrend> trends,
                           AnalysisStatistics statistics) {
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.analysisTime = Instant.now();
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.analyzedDevices = Objects.requireNonNull(analyzedDevices, "Analyzed devices cannot be null");
        this.metricAnalyses = Objects.requireNonNull(metricAnalyses, "Metric analyses cannot be null");
        this.anomalies = Objects.requireNonNull(anomalies, "Anomalies cannot be null");
        this.trends = Objects.requireNonNull(trends, "Trends cannot be null");
        this.statistics = Objects.requireNonNull(statistics, "Statistics cannot be null");
    }
    
    /**
     * Get analysis for specific metric
     */
    public MetricAnalysis getMetricAnalysis(String metricName) {
        return metricAnalyses.get(metricName);
    }
    
    /**
     * Check if analysis has anomalies
     */
    public boolean hasAnomalies() {
        return !anomalies.isEmpty();
    }
    
    /**
     * Check if analysis has trends
     */
    public boolean hasTrends() {
        return !trends.isEmpty();
    }
    
    /**
     * Get anomalies for specific device
     */
    public List<TelemetryAnomaly> getAnomaliesForDevice(DeviceId deviceId) {
        return anomalies.stream()
            .filter(anomaly -> anomaly.getDeviceId().equals(deviceId))
            .toList();
    }
    
    /**
     * Get trends for specific metric
     */
    public List<TelemetryTrend> getTrendsForMetric(String metricName) {
        return trends.stream()
            .filter(trend -> trend.getMetricName().equals(metricName))
            .toList();
    }
    
    /**
     * Get analysis duration in seconds
     */
    public long getAnalysisDurationSeconds() {
        if (fromTime == null || toTime == null) {
            return 0;
        }
        return toTime.getEpochSecond() - fromTime.getEpochSecond();
    }
    
    // Getters
    public String getOrganizationId() { return organizationId; }
    public Instant getAnalysisTime() { return analysisTime; }
    public Instant getFromTime() { return fromTime; }
    public Instant getToTime() { return toTime; }
    public List<DeviceId> getAnalyzedDevices() { return List.copyOf(analyzedDevices); }
    public Map<String, MetricAnalysis> getMetricAnalyses() { return Map.copyOf(metricAnalyses); }
    public List<TelemetryAnomaly> getAnomalies() { return List.copyOf(anomalies); }
    public List<TelemetryTrend> getTrends() { return List.copyOf(trends); }
    public AnalysisStatistics getStatistics() { return statistics; }
    
    @Override
    public String toString() {
        return "TelemetryAnalysis{" +
                "organizationId='" + organizationId + '\'' +
                ", analysisTime=" + analysisTime +
                ", deviceCount=" + analyzedDevices.size() +
                ", metricCount=" + metricAnalyses.size() +
                ", anomalyCount=" + anomalies.size() +
                ", trendCount=" + trends.size() +
                '}';
    }
}

/**
 * Analysis results for a specific metric
 */
record MetricAnalysis(
    String metricName,
    double min,
    double max,
    double average,
    double stdDev,
    long dataPointCount,
    List<Double> percentiles
) {}

/**
 * Represents a telemetry anomaly
 */
record TelemetryAnomaly(
    DeviceId deviceId,
    String metricName,
    double value,
    double expectedValue,
    double deviationScore,
    Instant timestamp,
    AnomalyType type
) {}

/**
 * Represents a telemetry trend
 */
record TelemetryTrend(
    String metricName,
    TrendDirection direction,
    double slope,
    double confidence,
    Instant startTime,
    Instant endTime
) {}

/**
 * Overall analysis statistics
 */
record AnalysisStatistics(
    long totalDataPoints,
    long processedDataPoints,
    long skippedDataPoints,
    double processingTimeMs,
    double dataQualityScore
) {}

/**
 * Types of anomalies
 */
enum AnomalyType {
    SPIKE,
    DIP,
    OUTLIER,
    MISSING_DATA,
    SENSOR_FAILURE
}

/**
 * Trend directions
 */
enum TrendDirection {
    INCREASING,
    DECREASING,
    STABLE,
    VOLATILE
}