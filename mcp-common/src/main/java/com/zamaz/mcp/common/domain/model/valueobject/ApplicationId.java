package com.zamaz.mcp.common.domain.model.valueobject;

import com.zamaz.mcp.common.domain.model.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing an Application identifier.
 * Applications are logical groupings within organizations that can contain teams and debates.
 */
public final class ApplicationId extends ValueObject {
    
    private final UUID value;
    
    /**
     * Creates a new ApplicationId with the given UUID value.
     * 
     * @param value the UUID value
     * @throws IllegalArgumentException if value is null
     */
    public ApplicationId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("ApplicationId value cannot be null");
        }
        this.value = value;
    }
    
    /**
     * Creates a new ApplicationId from a string representation.
     * 
     * @param value the string UUID value
     * @return new ApplicationId instance
     * @throws IllegalArgumentException if value is null, empty, or not a valid UUID
     */
    public static ApplicationId fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ApplicationId string value cannot be null or empty");
        }
        
        try {
            return new ApplicationId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ApplicationId format: " + value, e);
        }
    }
    
    /**
     * Generates a new random ApplicationId.
     * 
     * @return new ApplicationId with random UUID
     */
    public static ApplicationId generate() {
        return new ApplicationId(UUID.randomUUID());
    }
    
    /**
     * Gets the UUID value of this ApplicationId.
     * 
     * @return the UUID value
     */
    public UUID value() {
        return value;
    }
    
    /**
     * Gets the string representation of this ApplicationId.
     * 
     * @return string representation of the UUID
     */
    public String toString() {
        return value.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ApplicationId that = (ApplicationId) obj;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}