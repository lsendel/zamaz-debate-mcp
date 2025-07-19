package com.example.workflow.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing an edge identifier
 */
public record EdgeId(String value) {
    
    public EdgeId {
        Objects.requireNonNull(value, "Edge ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Edge ID value cannot be empty");
        }
    }
    
    /**
     * Generate a new random edge ID
     */
    public static EdgeId generate() {
        return new EdgeId(UUID.randomUUID().toString());
    }
    
    /**
     * Create edge ID from string
     */
    public static EdgeId of(String value) {
        return new EdgeId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}