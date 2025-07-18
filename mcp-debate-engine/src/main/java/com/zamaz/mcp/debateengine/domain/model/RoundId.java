package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a round identifier.
 */
public record RoundId(UUID value) implements ValueObject {
    
    public RoundId {
        Objects.requireNonNull(value, "Round ID cannot be null");
    }
    
    /**
     * Create from string representation.
     */
    public static RoundId from(String value) {
        return new RoundId(UUID.fromString(value));
    }
    
    /**
     * Generate new random ID.
     */
    public static RoundId generate() {
        return new RoundId(UUID.randomUUID());
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}