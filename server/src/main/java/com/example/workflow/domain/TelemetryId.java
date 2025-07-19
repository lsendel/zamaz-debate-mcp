package com.example.workflow.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a telemetry data identifier
 */
public record TelemetryId(String value) {
    
    public TelemetryId {
        Objects.requireNonNull(value, "Telemetry ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Telemetry ID value cannot be empty");
        }
    }
    
    /**
     * Generate a new random telemetry ID
     */
    public static TelemetryId generate() {
        return new TelemetryId(UUID.randomUUID().toString());
    }
    
    /**
     * Create telemetry ID from string
     */
    public static TelemetryId of(String value) {
        return new TelemetryId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}