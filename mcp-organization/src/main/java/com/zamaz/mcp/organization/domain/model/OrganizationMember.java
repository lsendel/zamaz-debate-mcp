package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object representing a member of an organization.
 * Tracks user membership with role and join date.
 */
public class OrganizationMember implements ValueObject {
    
    private final UserId userId;
    private Role role;
    private final LocalDateTime joinedAt;
    
    public OrganizationMember(UserId userId, Role role, LocalDateTime joinedAt) {
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        this.role = Objects.requireNonNull(role, "Role is required");
        this.joinedAt = Objects.requireNonNull(joinedAt, "Joined date is required");
    }
    
    /**
     * Changes the member's role.
     * This method is package-private to ensure it's only called by the Organization aggregate.
     * 
     * @param newRole the new role
     */
    void changeRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "New role is required");
    }
    
    /**
     * Checks if this member has a specific role or higher.
     * 
     * @param minimumRole the minimum required role
     * @return true if the member has sufficient permissions
     */
    public boolean hasPermission(Role minimumRole) {
        return role.hasPermission(minimumRole);
    }
    
    /**
     * Checks if this member can manage another member.
     * 
     * @param targetMember the member to be managed
     * @return true if this member can manage the target
     */
    public boolean canManage(OrganizationMember targetMember) {
        return role.canManage(targetMember.getRole());
    }
    
    // Getters
    
    public UserId getUserId() {
        return userId;
    }
    
    public Role getRole() {
        return role;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationMember that = (OrganizationMember) o;
        return Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
    
    @Override
    public String toString() {
        return "OrganizationMember{" +
                "userId=" + userId +
                ", role=" + role +
                ", joinedAt=" + joinedAt +
                '}';
    }
}