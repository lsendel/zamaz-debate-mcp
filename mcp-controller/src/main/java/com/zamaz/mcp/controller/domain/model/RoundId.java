package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Debate Round.
 */
public record RoundId(UUID value) implements ValueObject {
    
    public RoundId {
        Objects.requireNonNull(value, "Round ID cannot be null");
    }
    
    public static RoundId generate() {
        return new RoundId(UUID.randomUUID());
    }
    
    public static RoundId from(String value) {
        Objects.requireNonNull(value, "Round ID string cannot be null");
        try {
            return new RoundId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Round ID format: " + value, e);
        }
    }
    
    public static RoundId from(UUID value) {
        return new RoundId(value);
    }
    
    public String asString() {
        return value.toString();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}