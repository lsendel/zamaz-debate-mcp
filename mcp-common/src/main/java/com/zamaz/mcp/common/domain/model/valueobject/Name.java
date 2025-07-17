package com.zamaz.mcp.common.domain.model.valueobject;

import com.zamaz.mcp.common.domain.model.ValueObject;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import java.util.Objects;

/**
 * Value object representing a name (for users, organizations, etc.).
 * Ensures name validation at the domain level.
 */
public record Name(String value) implements ValueObject {
    
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 255;
    
    public Name {
        Objects.requireNonNull(value, "Name cannot be null");
        
        var trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new DomainRuleViolationException(
                "name.empty",
                "Name cannot be empty"
            );
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new DomainRuleViolationException(
                "name.tooLong",
                "Name cannot exceed " + MAX_LENGTH + " characters"
            );
        }
        value = trimmed;
    }
    
    /**
     * Creates a Name from a string, validating the format.
     * 
     * @param value the name string
     * @return a validated Name
     */
    public static Name from(String value) {
        return new Name(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}