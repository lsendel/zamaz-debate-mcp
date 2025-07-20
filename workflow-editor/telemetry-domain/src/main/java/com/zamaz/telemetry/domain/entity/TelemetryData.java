package com.zamaz.telemetry.domain.entity;

import com.zamaz.telemetry.domain.valueobject.TelemetryId;
import com.zamaz.telemetry.domain.valueobject.DeviceId;
import com.zamaz.telemetry.domain.valueobject.GeoLocation;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class TelemetryData {
    @NonNull
    private final TelemetryId id;
    
    @NonNull
    private final DeviceId deviceId;
    
    @NonNull
    private final Instant timestamp;
    
    @NonNull
    @Builder.Default
    private final Map<String, Object> metrics = new HashMap<>();
    
    private final GeoLocation location;
    
    private final String dataSource;
    
    @Builder.Default
    private final int qualityScore = 100;
    
    public static TelemetryData create(DeviceId deviceId, Map<String, Object> metrics) {
        return TelemetryData.builder()
                .id(TelemetryId.generate())
                .deviceId(deviceId)
                .timestamp(Instant.now())
                .metrics(new HashMap<>(metrics))
                .build();
    }
    
    public static TelemetryData createWithLocation(DeviceId deviceId, Map<String, Object> metrics, GeoLocation location) {
        return TelemetryData.builder()
                .id(TelemetryId.generate())
                .deviceId(deviceId)
                .timestamp(Instant.now())
                .metrics(new HashMap<>(metrics))
                .location(location)
                .build();
    }
    
    public Map<String, Object> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }
    
    public Object getMetric(String key) {
        return metrics.get(key);
    }
    
    public <T> T getMetricAs(String key, Class<T> type) {
        Object value = metrics.get(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException("Metric " + key + " is not of type " + type.getName());
        }
        return type.cast(value);
    }
    
    public boolean hasLocation() {
        return location != null;
    }
    
    public boolean isHighQuality() {
        return qualityScore >= 80;
    }
    
    public boolean isWithinTimeWindow(Instant start, Instant end) {
        return !timestamp.isBefore(start) && !timestamp.isAfter(end);
    }
}