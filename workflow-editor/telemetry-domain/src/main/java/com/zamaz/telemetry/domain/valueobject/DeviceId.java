package com.zamaz.telemetry.domain.valueobject;

import lombok.NonNull;
import lombok.Value;

@Value
public class DeviceId {
    @NonNull
    String value;
    
    private DeviceId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("DeviceId cannot be null or empty");
        }
        this.value = value;
    }
    
    public static DeviceId of(String value) {
        return new DeviceId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}