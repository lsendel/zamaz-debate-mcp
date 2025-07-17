package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a user identifier in the organization context.
 * This is an alias for the common UserId but specific to the organization domain.
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
    
    /**
     * Creates a UserId from common UserId.
     * 
     * @param commonUserId the common user ID
     * @return a UserId
     */
    public static UserId from(com.zamaz.mcp.common.domain.model.valueobject.UserId commonUserId) {
        return new UserId(commonUserId.value());
    }
    
    /**
     * Converts to common UserId.
     * 
     * @return common UserId
     */
    public com.zamaz.mcp.common.domain.model.valueobject.UserId toCommon() {
        return new com.zamaz.mcp.common.domain.model.valueobject.UserId(value);
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}