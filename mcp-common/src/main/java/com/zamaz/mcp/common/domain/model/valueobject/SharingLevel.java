package com.zamaz.mcp.common.domain.model.valueobject;

/**
 * Enumeration representing the different levels of sharing for debates.
 * This determines who can view or access the debate within the assigned scope.
 */
public enum SharingLevel {
    /**
     * The debate is private and only visible to the creator.
     */
    ME_ONLY("Me Only", "Private debate visible only to the creator"),
    
    /**
     * The debate is shared with all members of the organization.
     */
    ORGANIZATION("Organization", "Shared with all organization members"),
    
    /**
     * The debate is public within the application, visible to all users of the app.
     */
    APPLICATION_ALL("Application: All", "Public within the application to all users"),
    
    /**
     * The debate is shared only with a specific team within the application.
     */
    APPLICATION_TEAM("Application: Team", "Shared with specific team members only"),
    
    /**
     * The debate is shared within the application but only with the creator.
     * Similar to ME_ONLY but tracked within the application context.
     */
    APPLICATION_ME("Application: Me", "Private within application context");
    
    private final String displayName;
    private final String description;
    
    SharingLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Gets the human-readable display name for this sharing level.
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the description explaining what this sharing level means.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this sharing level is private (only creator can access).
     */
    public boolean isPrivate() {
        return this == ME_ONLY || this == APPLICATION_ME;
    }
    
    /**
     * Checks if this sharing level requires organization membership.
     */
    public boolean requiresOrganizationMembership() {
        return this == ORGANIZATION;
    }
    
    /**
     * Checks if this sharing level requires application access.
     */
    public boolean requiresApplicationAccess() {
        return this == APPLICATION_ALL || this == APPLICATION_TEAM || this == APPLICATION_ME;
    }
    
    /**
     * Checks if this sharing level requires team membership.
     */
    public boolean requiresTeamMembership() {
        return this == APPLICATION_TEAM;
    }
    
    /**
     * Checks if this sharing level is compatible with the given scope type.
     * 
     * @param scopeType the scope type to check compatibility with
     * @return true if compatible, false otherwise
     */
    public boolean isCompatibleWith(ScopeType scopeType) {
        switch (this) {
            case ME_ONLY:
                return true; // Compatible with all scope types
            case ORGANIZATION:
                return scopeType.requiresOrganizationAccess();
            case APPLICATION_ALL:
            case APPLICATION_TEAM:
            case APPLICATION_ME:
                return scopeType.requiresApplicationAccess();
            default:
                return false;
        }
    }
    
    /**
     * Gets the sharing level from a string value (case-insensitive).
     * 
     * @param value the string value
     * @return the corresponding SharingLevel
     * @throws IllegalArgumentException if the value is not valid
     */
    public static SharingLevel fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("SharingLevel value cannot be null or empty");
        }
        
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid SharingLevel value: " + value);
        }
    }
    
    /**
     * Gets all sharing levels that are compatible with the given scope type.
     * 
     * @param scopeType the scope type to filter by
     * @return array of compatible sharing levels
     */
    public static SharingLevel[] getCompatibleLevels(ScopeType scopeType) {
        return java.util.Arrays.stream(values())
            .filter(level -> level.isCompatibleWith(scopeType))
            .toArray(SharingLevel[]::new);
    }
}