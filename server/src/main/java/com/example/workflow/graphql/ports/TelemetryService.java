package com.example.workflow.graphql.ports;

import com.example.workflow.domain.TelemetryData;
import com.example.workflow.domain.TelemetryQuery;
import com.example.workflow.domain.TelemetryAnalysis;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Inbound port for telemetry operations
 * Handles real-time telemetry data processing at 10Hz
 */
public interface TelemetryService {
    
    /**
     * Process incoming telemetry data stream
     */
    Mono<Void> processTelemetryStream(Flux<TelemetryData> dataStream);
    
    /**
     * Get real-time telemetry data for a device
     */
    Flux<TelemetryData> getRealtimeTelemetry(String deviceId);
    
    /**
     * Query historical telemetry data
     */
    Flux<TelemetryData> queryTelemetry(TelemetryQueryInput query);
    
    /**
     * Get telemetry data for spatial region
     */
    Flux<TelemetryData> getSpatialTelemetry(SpatialQueryInput query);
    
    /**
     * Analyze telemetry data
     */
    Mono<TelemetryAnalysis> analyzeTelemetry(TelemetryAnalysisInput input);
    
    /**
     * Subscribe to telemetry alerts
     */
    Flux<TelemetryAlert> subscribeToAlerts(String organizationId);
    
    /**
     * Get telemetry statistics
     */
    Mono<TelemetryStats> getTelemetryStats(String organizationId, Instant from, Instant to);
}

/**
 * Input objects for telemetry operations
 */
record TelemetryQueryInput(
    String deviceId,
    Instant from,
    Instant to,
    List<String> metrics,
    String aggregation
) {}

record SpatialQueryInput(
    double minLat,
    double maxLat,
    double minLng,
    double maxLng,
    Instant from,
    Instant to
) {}

record TelemetryAnalysisInput(
    List<String> deviceIds,
    Instant from,
    Instant to,
    String analysisType
) {}

record TelemetryAlert(
    String id,
    String deviceId,
    String metric,
    double value,
    double threshold,
    String condition,
    Instant timestamp
) {}

record TelemetryStats(
    long totalDataPoints,
    long activeDevices,
    double averageFrequency,
    List<MetricStats> metricStats
) {}

record MetricStats(
    String metric,
    double min,
    double max,
    double average,
    double stdDev
) {}