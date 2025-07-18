package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Debate Response.
 */
public record ResponseId(UUID value) implements ValueObject {
    
    public ResponseId {
        Objects.requireNonNull(value, "Response ID cannot be null");
    }
    
    public static ResponseId generate() {
        return new ResponseId(UUID.randomUUID());
    }
    
    public static ResponseId from(String value) {
        Objects.requireNonNull(value, "Response ID string cannot be null");
        try {
            return new ResponseId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Response ID format: " + value, e);
        }
    }
    
    public static ResponseId from(UUID value) {
        return new ResponseId(value);
    }
    
    public String asString() {
        return value.toString();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}