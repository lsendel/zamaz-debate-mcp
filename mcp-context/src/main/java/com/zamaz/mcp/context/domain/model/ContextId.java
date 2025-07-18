package com.zamaz.mcp.context.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Context.
 * Immutable and self-validating.
 */
public record ContextId(UUID value) implements ValueObject {
    
    public ContextId {
        Objects.requireNonNull(value, "Context ID cannot be null");
    }
    
    public static ContextId generate() {
        return new ContextId(UUID.randomUUID());
    }
    
    public static ContextId from(String value) {
        Objects.requireNonNull(value, "Context ID string cannot be null");
        try {
            return new ContextId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Context ID format: " + value, e);
        }
    }
    
    public static ContextId from(UUID value) {
        return new ContextId(value);
    }
    
    public String asString() {
        return value.toString();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}