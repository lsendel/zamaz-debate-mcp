package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

import java.util.Objects;

/**
 * Value object representing an application name.
 * Application names must be unique within an organization.
 */
public final class ApplicationName extends ValueObject {
    
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 255;
    
    private final String value;
    
    /**
     * Creates a new ApplicationName.
     * 
     * @param value the application name
     * @throws IllegalArgumentException if the name is invalid
     */
    public ApplicationName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Application name cannot be null");
        }
        
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be empty");
        }
        
        if (trimmedValue.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                "Application name must be at least " + MIN_LENGTH + " characters long"
            );
        }
        
        if (trimmedValue.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Application name cannot exceed " + MAX_LENGTH + " characters"
            );
        }
        
        // Check for valid characters (alphanumeric, spaces, hyphens, underscores)
        if (!trimmedValue.matches("^[a-zA-Z0-9\\s\\-_]+$")) {
            throw new IllegalArgumentException(
                "Application name can only contain letters, numbers, spaces, hyphens, and underscores"
            );
        }
        
        this.value = trimmedValue;
    }
    
    /**
     * Creates an ApplicationName from a string value.
     * 
     * @param value the string value
     * @return new ApplicationName instance
     */
    public static ApplicationName of(String value) {
        return new ApplicationName(value);
    }
    
    /**
     * Gets the string value of this application name.
     * 
     * @return the application name
     */
    public String value() {
        return value;
    }
    
    /**
     * Checks if this application name is equal to another string.
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
        ApplicationName that = (ApplicationName) obj;
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