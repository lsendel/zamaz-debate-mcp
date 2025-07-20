package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

import java.util.Objects;

/**
 * Value object representing an application description.
 * Application descriptions are optional but provide additional context.
 */
public final class ApplicationDescription extends ValueObject {
    
    private static final int MAX_LENGTH = 1000;
    
    private final String value;
    
    /**
     * Creates a new ApplicationDescription.
     * 
     * @param value the application description (can be null or empty)
     * @throws IllegalArgumentException if the description is too long
     */
    public ApplicationDescription(String value) {
        if (value == null) {
            this.value = "";
        } else {
            String trimmedValue = value.trim();
            if (trimmedValue.length() > MAX_LENGTH) {
                throw new IllegalArgumentException(
                    "Application description cannot exceed " + MAX_LENGTH + " characters"
                );
            }
            this.value = trimmedValue;
        }
    }
    
    /**
     * Creates an ApplicationDescription from a string value.
     * 
     * @param value the string value (can be null)
     * @return new ApplicationDescription instance
     */
    public static ApplicationDescription of(String value) {
        return new ApplicationDescription(value);
    }
    
    /**
     * Creates an empty ApplicationDescription.
     * 
     * @return empty ApplicationDescription instance
     */
    public static ApplicationDescription empty() {
        return new ApplicationDescription("");
    }
    
    /**
     * Gets the string value of this application description.
     * 
     * @return the application description
     */
    public String value() {
        return value;
    }
    
    /**
     * Checks if this description is empty.
     * 
     * @return true if the description is empty or null
     */
    public boolean isEmpty() {
        return value.isEmpty();
    }
    
    /**
     * Checks if this description is not empty.
     * 
     * @return true if the description contains text
     */
    public boolean isPresent() {
        return !isEmpty();
    }
    
    /**
     * Gets the length of the description.
     * 
     * @return the character count
     */
    public int length() {
        return value.length();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ApplicationDescription that = (ApplicationDescription) obj;
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