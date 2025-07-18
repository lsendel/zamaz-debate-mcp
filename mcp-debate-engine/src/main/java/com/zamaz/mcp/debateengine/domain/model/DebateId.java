package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a debate identifier.
 */
public record DebateId(UUID value) implements ValueObject {
    
    public DebateId {
        Objects.requireNonNull(value, "Debate ID cannot be null");
    }
    
    /**
     * Create from string representation.
     */
    public static DebateId from(String value) {
        return new DebateId(UUID.fromString(value));
    }
    
    /**
     * Generate new random ID.
     */
    public static DebateId generate() {
        return new DebateId(UUID.randomUUID());
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}