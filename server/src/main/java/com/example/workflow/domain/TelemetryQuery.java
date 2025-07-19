package com.example.workflow.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Domain object for telemetry data queries
 * Supports time-series and spatial filtering
 */
public class TelemetryQuery {
    
    private final String organizationId;
    private final List<DeviceId> deviceIds;
    private final Instant fromTime;
    private final Instant toTime;
    private final List<String> metrics;
    private final GeoLocation centerLocation;
    private final Double radiusKm;
    private final BoundingBox boundingBox;
    private final AggregationType aggregation;
    private final Integer limit;
    
    private TelemetryQuery(Builder builder) {
        this.organizationId = Objects.requireNonNull(builder.organizationId, "Organization ID cannot be null");
        this.deviceIds = builder.deviceIds;
        this.fromTime = builder.fromTime;
        this.toTime = builder.toTime;
        this.metrics = builder.metrics;
        this.centerLocation = builder.centerLocation;
        this.radiusKm = builder.radiusKm;
        this.boundingBox = builder.boundingBox;
        this.aggregation = builder.aggregation;
        this.limit = builder.limit;
        
        validateQuery();
    }
    
    /**
     * Create query builder
     */
    public static Builder builder(String organizationId) {
        return new Builder(organizationId);
    }
    
    /**
     * Check if query has time range filter
     */
    public boolean hasTimeRange() {
        return fromTime != null || toTime != null;
    }
    
    /**
     * Check if query has spatial filter
     */
    public boolean hasSpatialFilter() {
        return (centerLocation != null && radiusKm != null) || boundingBox != null;
    }
    
    /**
     * Check if query has device filter
     */
    public boolean hasDeviceFilter() {
        return deviceIds != null && !deviceIds.isEmpty();
    }
    
    /**
     * Check if query has metric filter
     */
    public boolean hasMetricFilter() {
        return metrics != null && !metrics.isEmpty();
    }
    
    /**
     * Check if query has aggregation
     */
    public boolean hasAggregation() {
        return aggregation != null;
    }
    
    /**
     * Check if location is within query spatial bounds
     */
    public boolean isLocationWithinBounds(GeoLocation location) {
        if (!hasSpatialFilter()) {
            return true;
        }
        
        if (centerLocation != null && radiusKm != null) {
            return location.isWithinRadius(centerLocation, radiusKm);
        }
        
        if (boundingBox != null) {
            return boundingBox.contains(location);
        }
        
        return true;
    }
    
    /**
     * Check if timestamp is within query time range
     */
    public boolean isTimestampWithinRange(Instant timestamp) {
        if (!hasTimeRange()) {
            return true;
        }
        
        if (fromTime != null && timestamp.isBefore(fromTime)) {
            return false;
        }
        
        if (toTime != null && timestamp.isAfter(toTime)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate query parameters
     */
    private void validateQuery() {
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new IllegalArgumentException("From time cannot be after to time");
        }
        
        if (centerLocation != null && radiusKm == null) {
            throw new IllegalArgumentException("Radius must be specified when center location is provided");
        }
        
        if (centerLocation == null && radiusKm != null) {
            throw new IllegalArgumentException("Center location must be specified when radius is provided");
        }
        
        if (radiusKm != null && radiusKm <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }
        
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
    }
    
    // Getters
    public String getOrganizationId() { return organizationId; }
    public List<DeviceId> getDeviceIds() { return deviceIds; }
    public Instant getFromTime() { return fromTime; }
    public Instant getToTime() { return toTime; }
    public List<String> getMetrics() { return metrics; }
    public GeoLocation getCenterLocation() { return centerLocation; }
    public Double getRadiusKm() { return radiusKm; }
    public BoundingBox getBoundingBox() { return boundingBox; }
    public AggregationType getAggregation() { return aggregation; }
    public Integer getLimit() { return limit; }
    
    /**
     * Builder for TelemetryQuery
     */
    public static class Builder {
        private final String organizationId;
        private List<DeviceId> deviceIds;
        private Instant fromTime;
        private Instant toTime;
        private List<String> metrics;
        private GeoLocation centerLocation;
        private Double radiusKm;
        private BoundingBox boundingBox;
        private AggregationType aggregation;
        private Integer limit;
        
        private Builder(String organizationId) {
            this.organizationId = organizationId;
        }
        
        public Builder deviceIds(List<DeviceId> deviceIds) {
            this.deviceIds = deviceIds;
            return this;
        }
        
        public Builder timeRange(Instant from, Instant to) {
            this.fromTime = from;
            this.toTime = to;
            return this;
        }
        
        public Builder metrics(List<String> metrics) {
            this.metrics = metrics;
            return this;
        }
        
        public Builder spatialRadius(GeoLocation center, double radiusKm) {
            this.centerLocation = center;
            this.radiusKm = radiusKm;
            return this;
        }
        
        public Builder spatialBounds(BoundingBox boundingBox) {
            this.boundingBox = boundingBox;
            return this;
        }
        
        public Builder aggregation(AggregationType aggregation) {
            this.aggregation = aggregation;
            return this;
        }
        
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }
        
        public TelemetryQuery build() {
            return new TelemetryQuery(this);
        }
    }
    
    @Override
    public String toString() {
        return "TelemetryQuery{" +
                "organizationId='" + organizationId + '\'' +
                ", deviceCount=" + (deviceIds != null ? deviceIds.size() : 0) +
                ", hasTimeRange=" + hasTimeRange() +
                ", hasSpatialFilter=" + hasSpatialFilter() +
                ", hasAggregation=" + hasAggregation() +
                ", limit=" + limit +
                '}';
    }
}