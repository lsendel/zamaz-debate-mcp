package com.zamaz.mcp.common.domain.model.valueobject;

import com.zamaz.mcp.common.domain.model.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a Team identifier.
 * Teams are groups of users within applications that can collaborate on debates.
 */
public final class TeamId extends ValueObject {
    
    private final UUID value;
    
    /**
     * Creates a new TeamId with the given UUID value.
     * 
     * @param value the UUID value
     * @throws IllegalArgumentException if value is null
     */
    public TeamId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("TeamId value cannot be null");
        }
        this.value = value;
    }
    
    /**
     * Creates a new TeamId from a string representation.
     * 
     * @param value the string UUID value
     * @return new TeamId instance
     * @throws IllegalArgumentException if value is null, empty, or not a valid UUID
     */
    public static TeamId fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("TeamId string value cannot be null or empty");
        }
        
        try {
            return new TeamId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid TeamId format: " + value, e);
        }
    }
    
    /**
     * Generates a new random TeamId.
     * 
     * @return new TeamId with random UUID
     */
    public static TeamId generate() {
        return new TeamId(UUID.randomUUID());
    }
    
    /**
     * Gets the UUID value of this TeamId.
     * 
     * @return the UUID value
     */
    public UUID value() {
        return value;
    }
    
    /**
     * Gets the string representation of this TeamId.
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
        TeamId that = (TeamId) obj;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}