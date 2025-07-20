package com.zamaz.workflow.telemetry.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class TelemetryData {
    @NonNull
    private final String id;
    
    @NonNull
    private final String deviceId;
    
    @NonNull
    private final Instant timestamp;
    
    @NonNull
    private final GeoLocation location;
    
    @NonNull
    private final Map<String, Object> metrics;
    
    private final TelemetryMetadata metadata;
}

@Data
@Builder
class GeoLocation {
    private final double latitude;
    private final double longitude;
    private final double altitude;
}

@Data
@Builder
class TelemetryMetadata {
    private final String source;
    private final String quality;
    private final Long processingTime;
    private final java.util.List<String> tags;
}