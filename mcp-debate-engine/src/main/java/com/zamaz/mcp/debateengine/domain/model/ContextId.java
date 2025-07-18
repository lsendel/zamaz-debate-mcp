package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a context identifier.
 */
public record ContextId(UUID value) implements ValueObject {
    
    public ContextId {
        Objects.requireNonNull(value, "Context ID cannot be null");
    }
    
    /**
     * Create from string representation.
     */
    public static ContextId from(String value) {
        return new ContextId(UUID.fromString(value));
    }
    
    /**
     * Generate new random ID.
     */
    public static ContextId generate() {
        return new ContextId(UUID.randomUUID());
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}