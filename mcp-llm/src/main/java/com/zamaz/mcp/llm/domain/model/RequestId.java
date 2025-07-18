package com.zamaz.mcp.llm.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for an LLM completion request.
 */
public record RequestId(UUID value) implements ValueObject {
    
    public RequestId {
        Objects.requireNonNull(value, "Request ID cannot be null");
    }
    
    public static RequestId generate() {
        return new RequestId(UUID.randomUUID());
    }
    
    public static RequestId from(String value) {
        Objects.requireNonNull(value, "Request ID string cannot be null");
        try {
            return new RequestId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Request ID format: " + value, e);
        }
    }
    
    public static RequestId from(UUID value) {
        return new RequestId(value);
    }
    
    public String asString() {
        return value.toString();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}