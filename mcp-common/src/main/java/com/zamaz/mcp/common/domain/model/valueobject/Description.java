package com.zamaz.mcp.common.domain.model.valueobject;

import com.zamaz.mcp.common.domain.model.ValueObject;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import java.util.Objects;

/**
 * Value object representing a description text.
 * Allows null/empty but validates length when provided.
 */
public record Description(String value) implements ValueObject {
    
    private static final int MAX_LENGTH = 2000;
    
    public Description {
        if (value != null) {
            var trimmed = value.trim();
            if (trimmed.length() > MAX_LENGTH) {
                throw new DomainRuleViolationException(
                    "description.tooLong",
                    "Description cannot exceed " + MAX_LENGTH + " characters"
                );
            }
            value = trimmed.isEmpty() ? null : trimmed;
        }
    }
    
    /**
     * Creates a Description from a string.
     * 
     * @param value the description string (can be null)
     * @return a Description
     */
    public static Description from(String value) {
        return new Description(value);
    }
    
    /**
     * Creates an empty Description.
     * 
     * @return an empty Description
     */
    public static Description empty() {
        return new Description(null);
    }
    
    /**
     * Checks if the description is empty.
     * 
     * @return true if empty or null
     */
    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }
    
    @Override
    public String toString() {
        return value != null ? value : "";
    }
}