package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

import java.util.Objects;

/**
 * Value object representing a team name.
 * Team names must be unique within an application or organization.
 */
public final class TeamName extends ValueObject {
    
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 255;
    
    private final String value;
    
    /**
     * Creates a new TeamName.
     * 
     * @param value the team name
     * @throws IllegalArgumentException if the name is invalid
     */
    public TeamName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Team name cannot be null");
        }
        
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be empty");
        }
        
        if (trimmedValue.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                "Team name must be at least " + MIN_LENGTH + " characters long"
            );
        }
        
        if (trimmedValue.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Team name cannot exceed " + MAX_LENGTH + " characters"
            );
        }
        
        // Check for valid characters (alphanumeric, spaces, hyphens, underscores)
        if (!trimmedValue.matches("^[a-zA-Z0-9\\s\\-_]+$")) {
            throw new IllegalArgumentException(
                "Team name can only contain letters, numbers, spaces, hyphens, and underscores"
            );
        }
        
        this.value = trimmedValue;
    }
    
    /**
     * Creates a TeamName from a string value.
     * 
     * @param value the string value
     * @return new TeamName instance
     */
    public static TeamName of(String value) {
        return new TeamName(value);
    }
    
    /**
     * Gets the string value of this team name.
     * 
     * @return the team name
     */
    public String value() {
        return value;
    }
    
    /**
     * Checks if this team name is equal to another string.
     * 
     * @param other the other string
     * @return true if equal (case-insensitive)
     */
    public boolean equalsIgnoreCase(String other) {
        return other != null && value.equalsIgnoreCase(other.trim());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TeamName that = (TeamName) obj;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}