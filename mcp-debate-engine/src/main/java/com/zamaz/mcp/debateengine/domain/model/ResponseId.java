package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a response identifier.
 */
public record ResponseId(UUID value) implements ValueObject {
    
    public ResponseId {
        Objects.requireNonNull(value, "Response ID cannot be null");
    }
    
    /**
     * Create from string representation.
     */
    public static ResponseId from(String value) {
        return new ResponseId(UUID.fromString(value));
    }
    
    /**
     * Generate new random ID.
     */
    public static ResponseId generate() {
        return new ResponseId(UUID.randomUUID());
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}