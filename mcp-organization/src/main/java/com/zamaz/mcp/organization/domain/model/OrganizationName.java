package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import java.util.Objects;

/**
 * Value object representing an organization name.
 * Enforces business rules for organization naming.
 */
public record OrganizationName(String value) implements ValueObject {
    
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;
    private static final String VALID_PATTERN = "^[a-zA-Z0-9\\s\\-_.]+$";
    
    public OrganizationName {
        Objects.requireNonNull(value, "Organization name cannot be null");
        
        var trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new DomainRuleViolationException(
                "organization.name.empty",
                "Organization name cannot be empty"
            );
        }
        
        if (trimmed.length() < MIN_LENGTH) {
            throw new DomainRuleViolationException(
                "organization.name.tooShort",
                "Organization name must be at least " + MIN_LENGTH + " characters"
            );
        }
        
        if (trimmed.length() > MAX_LENGTH) {
            throw new DomainRuleViolationException(
                "organization.name.tooLong",
                "Organization name cannot exceed " + MAX_LENGTH + " characters"
            );
        }
        
        if (!trimmed.matches(VALID_PATTERN)) {
            throw new DomainRuleViolationException(
                "organization.name.invalidCharacters",
                "Organization name contains invalid characters. Only letters, numbers, spaces, hyphens, underscores, and dots are allowed"
            );
        }
        
        value = trimmed;
    }
    
    /**
     * Creates an OrganizationName from a string.
     * 
     * @param value the organization name
     * @return a validated OrganizationName
     */
    public static OrganizationName from(String value) {
        return new OrganizationName(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}