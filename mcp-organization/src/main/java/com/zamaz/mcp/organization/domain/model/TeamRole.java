package com.zamaz.mcp.organization.domain.model;

/**
 * Enum representing user roles within a team.
 * Team roles are specific to team collaboration and management.
 */
public enum TeamRole {
    /**
     * Team admin has full control over the team.
     * Can manage team settings, members, and permissions.
     */
    ADMIN(2),
    
    /**
     * Team lead has management responsibilities.
     * Can manage team members and moderate team activities.
     */
    LEAD(1),
    
    /**
     * Regular team member with standard participation rights.
     * Can participate in team activities and debates.
     */
    MEMBER(0);
    
    private final int level;
    
    TeamRole(int level) {
        this.level = level;
    }
    
    /**
     * Checks if this role has permission equal to or greater than the specified role.
     * 
     * @param requiredRole the minimum required role
     * @return true if this role has sufficient permissions
     */
    public boolean hasPermission(TeamRole requiredRole) {
        return this.level >= requiredRole.level;
    }
    
    /**
     * Checks if this role can manage users with the specified role.
     * 
     * @param targetRole the role to be managed
     * @return true if this role can manage the target role
     */
    public boolean canManage(TeamRole targetRole) {
        // Admins can manage everyone
        if (this == ADMIN) {
            return true;
        }
        // Leads can manage members
        if (this == LEAD) {
            return targetRole == MEMBER;
        }
        // Members cannot manage anyone
        return false;
    }
    
    /**
     * Checks if this role can invite new members to the team.
     * 
     * @return true if this role can invite members
     */
    public boolean canInviteMembers() {
        return this == ADMIN || this == LEAD;
    }
    
    /**
     * Checks if this role can remove members from the team.
     * 
     * @return true if this role can remove members
     */
    public boolean canRemoveMembers() {
        return this == ADMIN || this == LEAD;
    }
    
    /**
     * Checks if this role can modify team settings.
     * 
     * @return true if this role can modify settings
     */
    public boolean canModifySettings() {
        return this == ADMIN;
    }
    
    /**
     * Gets the role level for comparison.
     * 
     * @return the role level
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Parses a role from a string, case-insensitive.
     * 
     * @param value the role string
     * @return the TeamRole enum value
     * @throws IllegalArgumentException if the value is not a valid role
     */
    public static TeamRole fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("TeamRole value cannot be null");
        }
        try {
            return TeamRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid team role: " + value);
        }
    }
    
    /**
     * Gets a human-readable display name for this role.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        switch (this) {
            case ADMIN:
                return "Team Admin";
            case LEAD:
                return "Team Lead";
            case MEMBER:
                return "Team Member";
            default:
                return this.name();
        }
    }
}