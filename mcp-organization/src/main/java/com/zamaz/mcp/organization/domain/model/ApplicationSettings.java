package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

import java.util.Objects;

/**
 * Value object representing application-specific settings and configuration.
 * Contains configurable limits and preferences for an application.
 */
public final class ApplicationSettings extends ValueObject {
    
    private final Integer maxTeams;
    private final Integer maxMembersPerTeam;
    private final boolean allowPublicDebates;
    private final boolean requireTeamApproval;
    private final boolean enableNotifications;
    
    /**
     * Creates new ApplicationSettings with specified values.
     * 
     * @param maxTeams maximum number of teams allowed (null for unlimited)
     * @param maxMembersPerTeam maximum members per team (null for unlimited)
     * @param allowPublicDebates whether to allow public debates
     * @param requireTeamApproval whether team membership requires approval
     * @param enableNotifications whether to enable notifications
     */
    public ApplicationSettings(Integer maxTeams, Integer maxMembersPerTeam,
                              boolean allowPublicDebates, boolean requireTeamApproval,
                              boolean enableNotifications) {
        if (maxTeams != null && maxTeams < 0) {
            throw new IllegalArgumentException("Max teams cannot be negative");
        }
        if (maxMembersPerTeam != null && maxMembersPerTeam < 1) {
            throw new IllegalArgumentException("Max members per team must be at least 1");
        }
        
        this.maxTeams = maxTeams;
        this.maxMembersPerTeam = maxMembersPerTeam;
        this.allowPublicDebates = allowPublicDebates;
        this.requireTeamApproval = requireTeamApproval;
        this.enableNotifications = enableNotifications;
    }
    
    /**
     * Creates default application settings.
     * 
     * @return ApplicationSettings with default values
     */
    public static ApplicationSettings defaultSettings() {
        return new ApplicationSettings(
            null,        // unlimited teams
            null,        // unlimited members per team
            true,        // allow public debates
            false,       // no team approval required
            true         // notifications enabled
        );
    }
    
    /**
     * Creates application settings for a small team.
     * 
     * @return ApplicationSettings optimized for small teams
     */
    public static ApplicationSettings forSmallTeam() {
        return new ApplicationSettings(
            5,           // max 5 teams
            10,          // max 10 members per team
            false,       // no public debates
            true,        // team approval required
            true         // notifications enabled
        );
    }
    
    /**
     * Creates application settings for an enterprise.
     * 
     * @return ApplicationSettings optimized for enterprise use
     */
    public static ApplicationSettings forEnterprise() {
        return new ApplicationSettings(
            null,        // unlimited teams
            50,          // max 50 members per team
            true,        // allow public debates
            true,        // team approval required
            true         // notifications enabled
        );
    }
    
    /**
     * Creates a copy of these settings with updated max teams.
     * 
     * @param maxTeams the new max teams value
     * @return new ApplicationSettings instance
     */
    public ApplicationSettings withMaxTeams(Integer maxTeams) {
        return new ApplicationSettings(
            maxTeams, maxMembersPerTeam, allowPublicDebates,
            requireTeamApproval, enableNotifications
        );
    }
    
    /**
     * Creates a copy of these settings with updated max members per team.
     * 
     * @param maxMembersPerTeam the new max members per team value
     * @return new ApplicationSettings instance
     */
    public ApplicationSettings withMaxMembersPerTeam(Integer maxMembersPerTeam) {
        return new ApplicationSettings(
            maxTeams, maxMembersPerTeam, allowPublicDebates,
            requireTeamApproval, enableNotifications
        );
    }
    
    /**
     * Creates a copy of these settings with updated public debates setting.
     * 
     * @param allowPublicDebates whether to allow public debates
     * @return new ApplicationSettings instance
     */
    public ApplicationSettings withAllowPublicDebates(boolean allowPublicDebates) {
        return new ApplicationSettings(
            maxTeams, maxMembersPerTeam, allowPublicDebates,
            requireTeamApproval, enableNotifications
        );
    }
    
    // Getters
    
    public Integer getMaxTeams() {
        return maxTeams;
    }
    
    public Integer getMaxMembersPerTeam() {
        return maxMembersPerTeam;
    }
    
    public boolean isAllowPublicDebates() {
        return allowPublicDebates;
    }
    
    public boolean isRequireTeamApproval() {
        return requireTeamApproval;
    }
    
    public boolean isEnableNotifications() {
        return enableNotifications;
    }
    
    /**
     * Checks if teams are limited.
     * 
     * @return true if there's a limit on number of teams
     */
    public boolean hasTeamLimit() {
        return maxTeams != null;
    }
    
    /**
     * Checks if team membership is limited.
     * 
     * @return true if there's a limit on team size
     */
    public boolean hasTeamMemberLimit() {
        return maxMembersPerTeam != null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ApplicationSettings that = (ApplicationSettings) obj;
        return allowPublicDebates == that.allowPublicDebates &&
               requireTeamApproval == that.requireTeamApproval &&
               enableNotifications == that.enableNotifications &&
               Objects.equals(maxTeams, that.maxTeams) &&
               Objects.equals(maxMembersPerTeam, that.maxMembersPerTeam);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(maxTeams, maxMembersPerTeam, allowPublicDebates,
                           requireTeamApproval, enableNotifications);
    }
    
    @Override
    public String toString() {
        return "ApplicationSettings{" +
               "maxTeams=" + maxTeams +
               ", maxMembersPerTeam=" + maxMembersPerTeam +
               ", allowPublicDebates=" + allowPublicDebates +
               ", requireTeamApproval=" + requireTeamApproval +
               ", enableNotifications=" + enableNotifications +
               '}';
    }
}