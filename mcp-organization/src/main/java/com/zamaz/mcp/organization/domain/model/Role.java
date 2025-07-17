package com.zamaz.mcp.organization.domain.model;

/**
 * Enum representing user roles within an organization.
 * Roles are hierarchical with OWNER having all permissions.
 */
public enum Role {
    /**
     * Owner has full control over the organization.
     * Can manage all aspects including other owners.
     */
    OWNER(3),
    
    /**
     * Admin can manage most organization settings and users.
     * Cannot manage owners or delete the organization.
     */
    ADMIN(2),
    
    /**
     * Regular member with basic access rights.
     * Can participate in debates and view organization content.
     */
    MEMBER(1),
    
    /**
     * Guest with limited read-only access.
     * Can only view public content.
     */
    GUEST(0);
    
    private final int level;
    
    Role(int level) {
        this.level = level;
    }
    
    /**
     * Checks if this role has permission equal to or greater than the specified role.
     * 
     * @param requiredRole the minimum required role
     * @return true if this role has sufficient permissions
     */
    public boolean hasPermission(Role requiredRole) {
        return this.level >= requiredRole.level;
    }
    
    /**
     * Checks if this role can manage users with the specified role.
     * 
     * @param targetRole the role to be managed
     * @return true if this role can manage the target role
     */
    public boolean canManage(Role targetRole) {
        // Owners can manage everyone
        if (this == OWNER) {
            return true;
        }
        // Admins can manage members and guests
        if (this == ADMIN) {
            return targetRole.level < this.level;
        }
        // Others cannot manage anyone
        return false;
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
     * @return the Role enum value
     * @throws IllegalArgumentException if the value is not a valid role
     */
    public static Role fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role value cannot be null");
        }
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + value);
        }
    }
}