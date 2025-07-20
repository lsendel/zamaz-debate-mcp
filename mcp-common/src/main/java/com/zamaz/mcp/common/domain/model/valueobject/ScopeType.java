package com.zamaz.mcp.common.domain.model.valueobject;

/**
 * Enumeration representing the different types of scope that can be assigned to debates.
 * This determines which entity (organization, application, or both) the debate is associated with.
 */
public enum ScopeType {
    /**
     * Debate is scoped to an organization only.
     * Access is controlled based on organization membership.
     */
    ORGANIZATION("Organization"),
    
    /**
     * Debate is scoped to a specific application within an organization.
     * Access is controlled based on application membership.
     */
    APPLICATION("Application"),
    
    /**
     * Debate is scoped to both organization and application.
     * Access requires membership in both the organization and the specific application.
     */
    BOTH("Both");
    
    private final String displayName;
    
    ScopeType(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets the human-readable display name for this scope type.
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Checks if this scope type requires organization access.
     */
    public boolean requiresOrganizationAccess() {
        return this == ORGANIZATION || this == BOTH;
    }
    
    /**
     * Checks if this scope type requires application access.
     */
    public boolean requiresApplicationAccess() {
        return this == APPLICATION || this == BOTH;
    }
    
    /**
     * Gets the scope type from a string value (case-insensitive).
     * 
     * @param value the string value
     * @return the corresponding ScopeType
     * @throws IllegalArgumentException if the value is not valid
     */
    public static ScopeType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ScopeType value cannot be null or empty");
        }
        
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ScopeType value: " + value);
        }
    }
}