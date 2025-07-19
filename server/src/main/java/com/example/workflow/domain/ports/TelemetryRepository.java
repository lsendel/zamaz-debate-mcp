package com.example.workflow.domain.ports;

import com.example.workflow.domain.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Repository port for telemetry data persistence operations
 * Supports time-series storage, spatial operations, and high-frequency data ingestion
 */
public interface TelemetryRepository {
    
    // Basic persistence operations
    void saveTimeSeries(TelemetryData data);
    void saveSpatialData(TelemetryData data);
    void saveBatch(List<TelemetryData> dataList);
    
    // Time-series queries
    Stream<TelemetryData> queryTimeSeries(String deviceId, Instant start, Instant end);
    Stream<TelemetryData> queryTimeSeries(List<DeviceId> deviceIds, Instant start, Instant end);
    Stream<TelemetryData> queryTimeSeriesWithMetrics(String deviceId, Instant start, Instant end, List<String> metrics);
    Stream<TelemetryData> queryRecentData(String organizationId, Duration duration);
    Stream<TelemetryData> queryRealTimeData(String organizationId);
    
    // Spatial queries
    List<TelemetryData> querySpatial(BoundingBox boundingBox);
    List<TelemetryData> querySpatialWithTime(BoundingBox boundingBox, Instant start, Instant end);
    List<TelemetryData> queryByRadius(GeoLocation center, double radiusKm);
    List<TelemetryData> queryByRadiusWithTime(GeoLocation center, double radiusKm, Instant start, Instant end);
    
    // Metric-based queries
    List<TelemetryData> queryByMetric(String metricName, Object value);
    List<TelemetryData> queryByMetricRange(String metricName, double minValue, double maxValue);
    List<TelemetryData> queryByMetricThreshold(String metricName, double threshold, ThresholdComparison comparison);
    
    // Complex queries using TelemetryQuery
    TelemetryQueryResult query(TelemetryQuery query);
    Stream<TelemetryData> queryStream(TelemetryQuery query);
    
    // Aggregation operations
    List<TelemetryAggregation> aggregate(TelemetryQuery query, AggregationType aggregationType, Duration interval);
    Map<String, Double> getMetricStatistics(String deviceId, String metricName, Instant start, Instant end);
    List<DeviceMetricSummary> getDeviceSummaries(String organizationId, Instant start, Instant end);
    
    // Data management operations
    void deleteOldData(String organizationId, Instant beforeTime);
    void deleteByDevice(DeviceId deviceId);
    long countByOrganization(String organizationId, Instant start, Instant end);
    long countByDevice(DeviceId deviceId, Instant start, Instant end);
    
    // Performance and monitoring
    TelemetryRepositoryStats getRepositoryStats(String organizationId);
    List<DeviceId> getActiveDevices(String organizationId, Duration recentDuration);
    Instant getLatestTimestamp(DeviceId deviceId);
    Instant getEarliestTimestamp(DeviceId deviceId);
    
    /**
     * Telemetry query result with pagination
     */
    record TelemetryQueryResult(
        List<TelemetryData> data,
        long totalCount,
        boolean hasMore,
        String nextPageToken
    ) {}
    
    /**
     * Telemetry aggregation result
     */
    record TelemetryAggregation(
        Instant timestamp,
        String metricName,
        double value,
        long count,
        AggregationType aggregationType
    ) {}
    
    /**
     * Device metric summary
     */
    record DeviceMetricSummary(
        DeviceId deviceId,
        Map<String, MetricStatistics> metricStats,
        long dataPointCount,
        Instant firstTimestamp,
        Instant lastTimestamp
    ) {}
    
    /**
     * Metric statistics
     */
    record MetricStatistics(
        double min,
        double max,
        double avg,
        double sum,
        long count,
        double stdDev
    ) {}
    
    /**
     * Repository performance statistics
     */
    record TelemetryRepositoryStats(
        long totalDataPoints,
        long dataPointsLast24h,
        double averageIngestionRate,
        long uniqueDevices,
        Map<String, Long> metricCounts,
        Instant oldestData,
        Instant newestData
    ) {}
    
    /**
     * Threshold comparison operators
     */
    enum ThresholdComparison {
        GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, EQUAL, NOT_EQUAL
    }
}