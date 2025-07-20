package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

import java.util.Objects;

/**
 * Value object representing a team description.
 * Team descriptions are optional but provide additional context about the team's purpose.
 */
public final class TeamDescription extends ValueObject {
    
    private static final int MAX_LENGTH = 1000;
    
    private final String value;
    
    /**
     * Creates a new TeamDescription.
     * 
     * @param value the team description (can be null or empty)
     * @throws IllegalArgumentException if the description is too long
     */
    public TeamDescription(String value) {
        if (value == null) {
            this.value = "";
        } else {
            String trimmedValue = value.trim();
            if (trimmedValue.length() > MAX_LENGTH) {
                throw new IllegalArgumentException(
                    "Team description cannot exceed " + MAX_LENGTH + " characters"
                );
            }
            this.value = trimmedValue;
        }
    }
    
    /**
     * Creates a TeamDescription from a string value.
     * 
     * @param value the string value (can be null)
     * @return new TeamDescription instance
     */
    public static TeamDescription of(String value) {
        return new TeamDescription(value);
    }
    
    /**
     * Creates an empty TeamDescription.
     * 
     * @return empty TeamDescription instance
     */
    public static TeamDescription empty() {
        return new TeamDescription("");
    }
    
    /**
     * Gets the string value of this team description.
     * 
     * @return the team description
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
        TeamDescription that = (TeamDescription) obj;
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