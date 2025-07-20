package com.zamaz.telemetry.domain.valueobject;

import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
public class TelemetryId {
    @NonNull
    String value;
    
    private TelemetryId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("TelemetryId cannot be null or empty");
        }
        this.value = value;
    }
    
    public static TelemetryId of(String value) {
        return new TelemetryId(value);
    }
    
    public static TelemetryId generate() {
        return new TelemetryId("tel-" + UUID.randomUUID().toString());
    }
    
    @Override
    public String toString() {
        return value;
    }
}