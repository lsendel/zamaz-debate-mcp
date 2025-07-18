package com.zamaz.mcp.context.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Message.
 * Immutable and self-validating.
 */
public record MessageId(UUID value) implements ValueObject {
    
    public MessageId {
        Objects.requireNonNull(value, "Message ID cannot be null");
    }
    
    public static MessageId generate() {
        return new MessageId(UUID.randomUUID());
    }
    
    public static MessageId from(String value) {
        Objects.requireNonNull(value, "Message ID string cannot be null");
        try {
            return new MessageId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Message ID format: " + value, e);
        }
    }
    
    public static MessageId from(UUID value) {
        return new MessageId(value);
    }
    
    public String asString() {
        return value.toString();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}