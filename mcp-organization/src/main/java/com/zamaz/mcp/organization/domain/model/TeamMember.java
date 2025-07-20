package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.DomainEntity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a team member - a user's membership in a team.
 * This is a pure domain object with no framework dependencies.
 */
public class TeamMember extends DomainEntity {
    
    private final UserId userId;
    private TeamRole role;
    private final LocalDateTime joinedAt;
    private boolean active;
    
    /**
     * Creates a new team member.
     * 
     * @param userId the user ID
     * @param role the initial role
     * @param joinedAt when the user joined the team
     */
    public TeamMember(UserId userId, TeamRole role, LocalDateTime joinedAt) {
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        this.role = Objects.requireNonNull(role, "Role is required");
        this.joinedAt = Objects.requireNonNull(joinedAt, "Joined date is required");
        this.active = true;
    }
    
    /**
     * Reconstructs a team member from persistence.
     * Used by repository when loading from database.
     */
    public TeamMember(UserId userId, TeamRole role, LocalDateTime joinedAt, 
                     boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(createdAt, updatedAt);
        this.userId = Objects.requireNonNull(userId);
        this.role = Objects.requireNonNull(role);
        this.joinedAt = Objects.requireNonNull(joinedAt);
        this.active = active;
    }
    
    /**
     * Changes the member's role in the team.
     * 
     * @param newRole the new role
     */
    public void changeRole(TeamRole newRole) {
        Objects.requireNonNull(newRole, "Role cannot be null");
        if (this.role != newRole) {
            this.role = newRole;
            markUpdated();
        }
    }
    
    /**
     * Deactivates this team membership.
     */
    public void deactivate() {
        if (active) {
            this.active = false;
            markUpdated();
        }
    }
    
    /**
     * Reactivates this team membership.
     */
    public void reactivate() {
        if (!active) {
            this.active = true;
            markUpdated();
        }
    }
    
    /**
     * Checks if this member has the specified role or higher.
     * 
     * @param minimumRole the minimum required role
     * @return true if the member has sufficient role
     */
    public boolean hasRole(TeamRole minimumRole) {
        return active && role.hasPermission(minimumRole);
    }
    
    /**
     * Checks if this member can manage another member.
     * 
     * @param targetMember the member to potentially manage
     * @return true if this member can manage the target
     */
    public boolean canManage(TeamMember targetMember) {
        return active && targetMember.active && role.canManage(targetMember.role);
    }
    
    /**
     * Checks if this member can invite new members.
     * 
     * @return true if this member can invite
     */
    public boolean canInviteMembers() {
        return active && role.canInviteMembers();
    }
    
    /**
     * Checks if this member can remove other members.
     * 
     * @return true if this member can remove others
     */
    public boolean canRemoveMembers() {
        return active && role.canRemoveMembers();
    }
    
    /**
     * Checks if this member can modify team settings.
     * 
     * @return true if this member can modify settings
     */
    public boolean canModifySettings() {
        return active && role.canModifySettings();
    }
    
    /**
     * Gets the user ID of this member.
     * 
     * @return the user ID
     */
    public UserId getUserId() {
        return userId;
    }
    
    /**
     * Gets the role of this member.
     * 
     * @return the role
     */
    public TeamRole getRole() {
        return role;
    }
    
    /**
     * Gets when this member joined the team.
     * 
     * @return the join date
     */
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    /**
     * Checks if this member is active.
     * 
     * @return true if active
     */
    public boolean isActive() {
        return active;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TeamMember that = (TeamMember) obj;
        return Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
    
    @Override
    public String toString() {
        return "TeamMember{" +
               "userId=" + userId +
               ", role=" + role +
               ", joinedAt=" + joinedAt +
               ", active=" + active +
               '}';
    }
}