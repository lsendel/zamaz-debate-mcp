package com.example.workflow.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a device identifier
 */
public record DeviceId(String value) {
    
    public DeviceId {
        Objects.requireNonNull(value, "Device ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID value cannot be empty");
        }
    }
    
    /**
     * Generate a new random device ID
     */
    public static DeviceId generate() {
        return new DeviceId("device-" + UUID.randomUUID().toString());
    }
    
    /**
     * Create device ID from string
     */
    public static DeviceId of(String value) {
        return new DeviceId(value);
    }
    
    /**
     * Create device ID for Stamford address sample
     */
    public static DeviceId forStamfordAddress(int addressIndex) {
        return new DeviceId("stamford-addr-" + String.format("%02d", addressIndex));
    }
    
    @Override
    public String toString() {
        return value;
    }
}