package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;

/**
 * Value object representing an organization description.
 * Allows null/empty but validates length when provided.
 */
public record OrganizationDescription(String value) implements ValueObject {
    
    private static final int MAX_LENGTH = 500;
    
    public OrganizationDescription {
        if (value != null) {
            var trimmed = value.trim();
            if (trimmed.length() > MAX_LENGTH) {
                throw new DomainRuleViolationException(
                    "organization.description.tooLong",
                    "Organization description cannot exceed " + MAX_LENGTH + " characters"
                );
            }
            value = trimmed.isEmpty() ? null : trimmed;
        }
    }
    
    /**
     * Creates an OrganizationDescription from a string.
     * 
     * @param value the description string (can be null)
     * @return an OrganizationDescription
     */
    public static OrganizationDescription from(String value) {
        return new OrganizationDescription(value);
    }
    
    /**
     * Creates an empty OrganizationDescription.
     * 
     * @return an empty OrganizationDescription
     */
    public static OrganizationDescription empty() {
        return new OrganizationDescription(null);
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