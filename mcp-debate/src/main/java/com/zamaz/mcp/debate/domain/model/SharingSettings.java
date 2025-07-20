package com.zamaz.mcp.debate.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import com.zamaz.mcp.common.domain.model.valueobject.SharingLevel;
import com.zamaz.mcp.common.domain.model.valueobject.TeamId;
import com.zamaz.mcp.common.domain.model.valueobject.UserId;

import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing the sharing settings of a debate.
 * Encapsulates the sharing level and associated team/user information.
 */
public final class SharingSettings extends ValueObject {
    
    private final SharingLevel sharingLevel;
    private final TeamId teamId; // Required for APPLICATION_TEAM sharing
    private final UserId createdByUserId; // Always required for access control
    
    /**
     * Creates sharing settings with the specified level and creator.
     * 
     * @param sharingLevel the sharing level
     * @param createdByUserId the user who created the debate
     */
    public SharingSettings(SharingLevel sharingLevel, UserId createdByUserId) {
        this.sharingLevel = Objects.requireNonNull(sharingLevel, "Sharing level is required");
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "Creator user ID is required");
        
        if (sharingLevel.requiresTeamMembership()) {
            throw new IllegalArgumentException("Team ID is required for sharing level " + sharingLevel);
        }
        
        this.teamId = null;
    }
    
    /**
     * Creates sharing settings with team-based sharing.
     * 
     * @param sharingLevel the sharing level (must be APPLICATION_TEAM)
     * @param teamId the team ID
     * @param createdByUserId the user who created the debate
     */
    public SharingSettings(SharingLevel sharingLevel, TeamId teamId, UserId createdByUserId) {
        this.sharingLevel = Objects.requireNonNull(sharingLevel, "Sharing level is required");
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "Creator user ID is required");
        
        if (sharingLevel.requiresTeamMembership()) {
            this.teamId = Objects.requireNonNull(teamId, "Team ID is required for sharing level " + sharingLevel);
        } else if (teamId != null) {
            throw new IllegalArgumentException("Team ID should not be provided for sharing level " + sharingLevel);
        } else {
            this.teamId = null;
        }
    }
    
    /**
     * Creates private sharing settings (ME_ONLY).
     * 
     * @param createdByUserId the user who created the debate
     * @return new SharingSettings instance
     */
    public static SharingSettings privateSharing(UserId createdByUserId) {
        return new SharingSettings(SharingLevel.ME_ONLY, createdByUserId);
    }
    
    /**
     * Creates organization-wide sharing settings.
     * 
     * @param createdByUserId the user who created the debate
     * @return new SharingSettings instance
     */
    public static SharingSettings organizationSharing(UserId createdByUserId) {
        return new SharingSettings(SharingLevel.ORGANIZATION, createdByUserId);
    }
    
    /**
     * Creates application-wide sharing settings.
     * 
     * @param createdByUserId the user who created the debate
     * @return new SharingSettings instance
     */
    public static SharingSettings applicationSharing(UserId createdByUserId) {
        return new SharingSettings(SharingLevel.APPLICATION_ALL, createdByUserId);
    }
    
    /**
     * Creates team-based sharing settings.
     * 
     * @param teamId the team ID
     * @param createdByUserId the user who created the debate
     * @return new SharingSettings instance
     */
    public static SharingSettings teamSharing(TeamId teamId, UserId createdByUserId) {
        return new SharingSettings(SharingLevel.APPLICATION_TEAM, teamId, createdByUserId);
    }
    
    /**
     * Creates application-private sharing settings.
     * 
     * @param createdByUserId the user who created the debate
     * @return new SharingSettings instance
     */
    public static SharingSettings applicationPrivateSharing(UserId createdByUserId) {
        return new SharingSettings(SharingLevel.APPLICATION_ME, createdByUserId);
    }
    
    /**
     * Checks if the sharing is private (only creator can access).
     * 
     * @return true if private
     */
    public boolean isPrivate() {
        return sharingLevel.isPrivate();
    }
    
    /**
     * Checks if the sharing requires organization membership.
     * 
     * @return true if organization membership is required
     */
    public boolean requiresOrganizationMembership() {
        return sharingLevel.requiresOrganizationMembership();
    }
    
    /**
     * Checks if the sharing requires application access.
     * 
     * @return true if application access is required
     */
    public boolean requiresApplicationAccess() {
        return sharingLevel.requiresApplicationAccess();
    }
    
    /**
     * Checks if the sharing requires team membership.
     * 
     * @return true if team membership is required
     */
    public boolean requiresTeamMembership() {
        return sharingLevel.requiresTeamMembership();
    }
    
    /**
     * Checks if a user can access based on these sharing settings.
     * Note: This is a basic check - full access control requires context from other services.
     * 
     * @param userId the user ID to check
     * @return true if the user is the creator (for private sharing)
     */
    public boolean canUserAccess(UserId userId) {
        if (isPrivate()) {
            return createdByUserId.equals(userId);
        }
        // For non-private sharing, additional context is needed from organization/team services
        return true; // Delegate to higher-level access control
    }
    
    /**
     * Checks if this sharing is compatible with the given scope.
     * 
     * @param scope the debate scope
     * @return true if compatible
     */
    public boolean isCompatibleWith(DebateScope scope) {
        return sharingLevel.isCompatibleWith(scope.getScopeType());
    }
    
    /**
     * Creates a copy with updated sharing level.
     * 
     * @param newSharingLevel the new sharing level
     * @return new SharingSettings instance
     */
    public SharingSettings withSharingLevel(SharingLevel newSharingLevel) {
        if (newSharingLevel.requiresTeamMembership() && teamId == null) {
            throw new IllegalArgumentException("Team ID is required for sharing level " + newSharingLevel);
        }
        
        if (newSharingLevel.requiresTeamMembership()) {
            return new SharingSettings(newSharingLevel, teamId, createdByUserId);
        } else {
            return new SharingSettings(newSharingLevel, createdByUserId);
        }
    }
    
    /**
     * Creates a copy with updated team.
     * 
     * @param newTeamId the new team ID
     * @return new SharingSettings instance
     */
    public SharingSettings withTeam(TeamId newTeamId) {
        if (!sharingLevel.requiresTeamMembership()) {
            throw new IllegalArgumentException("Team cannot be set for sharing level " + sharingLevel);
        }
        
        return new SharingSettings(sharingLevel, newTeamId, createdByUserId);
    }
    
    // Getters
    
    public SharingLevel getSharingLevel() {
        return sharingLevel;
    }
    
    public Optional<TeamId> getTeamId() {
        return Optional.ofNullable(teamId);
    }
    
    public UserId getCreatedByUserId() {
        return createdByUserId;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SharingSettings that = (SharingSettings) obj;
        return sharingLevel == that.sharingLevel &&
               Objects.equals(teamId, that.teamId) &&
               Objects.equals(createdByUserId, that.createdByUserId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sharingLevel, teamId, createdByUserId);
    }
    
    @Override
    public String toString() {
        return "SharingSettings{" +
               "sharingLevel=" + sharingLevel +
               ", teamId=" + teamId +
               ", createdByUserId=" + createdByUserId +
               '}';
    }
}