package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Debate.
 */
public record DebateId(UUID value) implements ValueObject {
    
    public DebateId {
        Objects.requireNonNull(value, "Debate ID cannot be null");
    }
    
    public static DebateId generate() {
        return new DebateId(UUID.randomUUID());
    }
    
    public static DebateId from(String value) {
        Objects.requireNonNull(value, "Debate ID string cannot be null");
        try {
            return new DebateId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Debate ID format: " + value, e);
        }
    }
    
    public static DebateId from(UUID value) {
        return new DebateId(value);
    }
    
    public String asString() {
        return value.toString();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}