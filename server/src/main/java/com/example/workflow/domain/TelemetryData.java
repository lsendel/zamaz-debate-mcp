package com.example.workflow.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Telemetry data domain entity
 * Represents sensor data with temporal and spatial components
 */
public class TelemetryData {
    
    private final TelemetryId id;
    private final DeviceId deviceId;
    private final Instant timestamp;
    private final Map<String, MetricValue> metrics;
    private final GeoLocation location;
    private final String organizationId;
    
    public TelemetryData(TelemetryId id, DeviceId deviceId, Instant timestamp, 
                        Map<String, MetricValue> metrics, GeoLocation location, String organizationId) {
        this.id = Objects.requireNonNull(id, "Telemetry ID cannot be null");
        this.deviceId = Objects.requireNonNull(deviceId, "Device ID cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.metrics = Objects.requireNonNull(metrics, "Metrics cannot be null");
        this.location = location; // Can be null for non-spatial data
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        
        validateTelemetryData();
    }
    
    /**
     * Get metric value by name
     */
    public MetricValue getMetric(String metricName) {
        return metrics.get(metricName);
    }
    
    /**
     * Get numeric metric value
     */
    public Double getNumericMetric(String metricName) {
        MetricValue metric = metrics.get(metricName);
        return metric != null && metric.isNumeric() ? metric.getNumericValue() : null;
    }
    
    /**
     * Get string metric value
     */
    public String getStringMetric(String metricName) {
        MetricValue metric = metrics.get(metricName);
        return metric != null && metric.isString() ? metric.getStringValue() : null;
    }
    
    /**
     * Get boolean metric value
     */
    public Boolean getBooleanMetric(String metricName) {
        MetricValue metric = metrics.get(metricName);
        return metric != null && metric.isBoolean() ? metric.getBooleanValue() : null;
    }
    
    /**
     * Check if telemetry has spatial data
     */
    public boolean hasSpatialData() {
        return location != null;
    }
    
    /**
     * Check if telemetry is recent (within last minute)
     */
    public boolean isRecent() {
        return timestamp.isAfter(Instant.now().minusSeconds(60));
    }
    
    /**
     * Check if telemetry is real-time (within last 10 seconds)
     */
    public boolean isRealTime() {
        return timestamp.isAfter(Instant.now().minusSeconds(10));
    }
    
    /**
     * Get age of telemetry data in seconds
     */
    public long getAgeInSeconds() {
        return Instant.now().getEpochSecond() - timestamp.getEpochSecond();
    }
    
    /**
     * Check if metric exists
     */
    public boolean hasMetric(String metricName) {
        return metrics.containsKey(metricName);
    }
    
    /**
     * Get all metric names
     */
    public java.util.Set<String> getMetricNames() {
        return metrics.keySet();
    }
    
    /**
     * Create telemetry data for testing/emulation
     */
    public static TelemetryData createEmulated(DeviceId deviceId, String organizationId, GeoLocation location) {
        return new TelemetryData(
            TelemetryId.generate(),
            deviceId,
            Instant.now(),
            generateEmulatedMetrics(),
            location,
            organizationId
        );
    }
    
    /**
     * Generate emulated metrics for testing
     */
    private static Map<String, MetricValue> generateEmulatedMetrics() {
        return Map.of(
            "temperature", MetricValue.numeric(20.0 + Math.random() * 15.0),
            "humidity", MetricValue.numeric(40.0 + Math.random() * 40.0),
            "motion", MetricValue.bool(Math.random() > 0.7),
            "air_quality", MetricValue.numeric(50.0 + Math.random() * 100.0),
            "status", MetricValue.string(Math.random() > 0.9 ? "warning" : "normal")
        );
    }
    
    /**
     * Validate telemetry data
     */
    private void validateTelemetryData() {
        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("Telemetry data must have at least one metric");
        }
        
        if (timestamp.isAfter(Instant.now().plusSeconds(60))) {
            throw new IllegalArgumentException("Telemetry timestamp cannot be in the future");
        }
        
        if (organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization ID cannot be empty");
        }
    }
    
    // Getters
    public TelemetryId getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, MetricValue> getMetrics() { return Map.copyOf(metrics); }
    public GeoLocation getLocation() { return location; }
    public String getOrganizationId() { return organizationId; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelemetryData that = (TelemetryData) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "TelemetryData{" +
                "id=" + id +
                ", deviceId=" + deviceId +
                ", timestamp=" + timestamp +
                ", metricsCount=" + metrics.size() +
                ", hasLocation=" + (location != null) +
                ", organizationId='" + organizationId + '\'' +
                '}';
    }
}