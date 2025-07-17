package com.zamaz.mcp.common.domain.model.valueobject;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a user identifier.
 * This is used across all services to identify users.
 */
public record UserId(UUID value) implements ValueObject {
    
    public UserId {
        Objects.requireNonNull(value, "User ID cannot be null");
    }
    
    /**
     * Creates a new random UserId.
     * 
     * @return a new UserId
     */
    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }
    
    /**
     * Creates a UserId from a string representation.
     * 
     * @param value the string UUID
     * @return a UserId
     */
    public static UserId from(String value) {
        return new UserId(UUID.fromString(value));
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}