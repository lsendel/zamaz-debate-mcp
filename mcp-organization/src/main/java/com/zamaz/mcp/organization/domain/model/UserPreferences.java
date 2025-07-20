package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.AggregateRoot;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import com.zamaz.mcp.common.domain.model.valueobject.ApplicationId;
import com.zamaz.mcp.common.domain.model.valueobject.TeamId;
import com.zamaz.mcp.common.domain.model.valueobject.ScopeType;
import com.zamaz.mcp.common.domain.model.valueobject.SharingLevel;
import com.zamaz.mcp.organization.domain.event.UserPreferencesUpdatedEvent;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * UserPreferences aggregate root representing a user's default settings for debate creation.
 * This stores the user's preferred scope type, sharing level, and default organization/application/team.
 * This is a pure domain object with no framework dependencies.
 */
public class UserPreferences extends AggregateRoot<UserId> {
    
    private ScopeType defaultScopeType;
    private SharingLevel defaultSharingLevel;
    private OrganizationId defaultOrganizationId;
    private ApplicationId defaultApplicationId;
    private TeamId defaultTeamId;
    
    /**
     * Creates new user preferences with default values.
     * 
     * @param userId the user ID
     * @param defaultOrganizationId the default organization
     */
    public UserPreferences(UserId userId, OrganizationId defaultOrganizationId) {
        super(userId);
        this.defaultScopeType = ScopeType.ORGANIZATION;
        this.defaultSharingLevel = SharingLevel.ORGANIZATION;
        this.defaultOrganizationId = Objects.requireNonNull(defaultOrganizationId, 
            "Default organization ID is required");
        this.defaultApplicationId = null;
        this.defaultTeamId = null;
    }
    
    /**
     * Reconstructs user preferences from persistence.
     * Used by repository when loading from database.
     */
    public UserPreferences(UserId userId, ScopeType defaultScopeType, 
                          SharingLevel defaultSharingLevel, OrganizationId defaultOrganizationId,
                          ApplicationId defaultApplicationId, TeamId defaultTeamId,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(userId, createdAt, updatedAt);
        this.defaultScopeType = Objects.requireNonNull(defaultScopeType);
        this.defaultSharingLevel = Objects.requireNonNull(defaultSharingLevel);
        this.defaultOrganizationId = Objects.requireNonNull(defaultOrganizationId);
        this.defaultApplicationId = defaultApplicationId;
        this.defaultTeamId = defaultTeamId;
    }
    
    /**
     * Updates the default scope type and validates compatibility with sharing level.
     * 
     * @param newScopeType the new default scope type
     */
    public void updateDefaultScopeType(ScopeType newScopeType) {
        Objects.requireNonNull(newScopeType, "Scope type cannot be null");
        
        // Validate compatibility with current sharing level
        if (!defaultSharingLevel.isCompatibleWith(newScopeType)) {
            throw new DomainRuleViolationException(
                "userPreferences.scopeType.incompatible",
                "Scope type " + newScopeType + " is not compatible with sharing level " + defaultSharingLevel
            );
        }
        
        if (!this.defaultScopeType.equals(newScopeType)) {
            this.defaultScopeType = newScopeType;
            
            // Clear application/team defaults if they're no longer applicable
            if (!newScopeType.requiresApplicationAccess()) {
                this.defaultApplicationId = null;
                this.defaultTeamId = null;
            }
            
            markUpdatedAndPublishEvent();
        }
    }
    
    /**
     * Updates the default sharing level and validates compatibility with scope type.
     * 
     * @param newSharingLevel the new default sharing level
     */
    public void updateDefaultSharingLevel(SharingLevel newSharingLevel) {
        Objects.requireNonNull(newSharingLevel, "Sharing level cannot be null");
        
        // Validate compatibility with current scope type
        if (!newSharingLevel.isCompatibleWith(defaultScopeType)) {
            throw new DomainRuleViolationException(
                "userPreferences.sharingLevel.incompatible",
                "Sharing level " + newSharingLevel + " is not compatible with scope type " + defaultScopeType
            );
        }
        
        if (!this.defaultSharingLevel.equals(newSharingLevel)) {
            this.defaultSharingLevel = newSharingLevel;
            
            // Clear team default if team sharing is no longer required
            if (!newSharingLevel.requiresTeamMembership()) {
                this.defaultTeamId = null;
            }
            
            markUpdatedAndPublishEvent();
        }
    }
    
    /**
     * Updates the default organization.
     * 
     * @param newOrganizationId the new default organization ID
     */
    public void updateDefaultOrganization(OrganizationId newOrganizationId) {
        Objects.requireNonNull(newOrganizationId, "Organization ID cannot be null");
        
        if (!this.defaultOrganizationId.equals(newOrganizationId)) {
            this.defaultOrganizationId = newOrganizationId;
            // Clear application and team defaults when organization changes
            this.defaultApplicationId = null;
            this.defaultTeamId = null;
            markUpdatedAndPublishEvent();
        }
    }
    
    /**
     * Updates the default application.
     * 
     * @param newApplicationId the new default application ID (can be null)
     */
    public void updateDefaultApplication(ApplicationId newApplicationId) {
        if (!Objects.equals(this.defaultApplicationId, newApplicationId)) {
            this.defaultApplicationId = newApplicationId;
            // Clear team default when application changes
            this.defaultTeamId = null;
            markUpdatedAndPublishEvent();
        }
    }
    
    /**
     * Updates the default team.
     * 
     * @param newTeamId the new default team ID (can be null)
     */
    public void updateDefaultTeam(TeamId newTeamId) {
        if (!Objects.equals(this.defaultTeamId, newTeamId)) {
            this.defaultTeamId = newTeamId;
            markUpdatedAndPublishEvent();
        }
    }
    
    /**
     * Updates all preferences at once with validation.
     */
    public void updateAllPreferences(ScopeType scopeType, SharingLevel sharingLevel,
                                   OrganizationId organizationId, ApplicationId applicationId,
                                   TeamId teamId) {
        // Validate compatibility
        if (!sharingLevel.isCompatibleWith(scopeType)) {
            throw new DomainRuleViolationException(
                "userPreferences.incompatible",
                "Sharing level " + sharingLevel + " is not compatible with scope type " + scopeType
            );
        }
        
        // Validate application/team requirements
        if (scopeType.requiresApplicationAccess() && applicationId == null) {
            throw new DomainRuleViolationException(
                "userPreferences.application.required",
                "Application is required for scope type " + scopeType
            );
        }
        
        if (sharingLevel.requiresTeamMembership() && teamId == null) {
            throw new DomainRuleViolationException(
                "userPreferences.team.required",
                "Team is required for sharing level " + sharingLevel
            );
        }
        
        boolean changed = false;
        
        if (!this.defaultScopeType.equals(scopeType)) {
            this.defaultScopeType = scopeType;
            changed = true;
        }
        
        if (!this.defaultSharingLevel.equals(sharingLevel)) {
            this.defaultSharingLevel = sharingLevel;
            changed = true;
        }
        
        if (!this.defaultOrganizationId.equals(organizationId)) {
            this.defaultOrganizationId = organizationId;
            changed = true;
        }
        
        if (!Objects.equals(this.defaultApplicationId, applicationId)) {
            this.defaultApplicationId = applicationId;
            changed = true;
        }
        
        if (!Objects.equals(this.defaultTeamId, teamId)) {
            this.defaultTeamId = teamId;
            changed = true;
        }
        
        if (changed) {
            markUpdatedAndPublishEvent();
        }
    }
    
    /**
     * Checks if the current preferences are valid for creating a debate.
     * 
     * @return true if preferences are consistent and complete
     */
    public boolean isValidForDebateCreation() {
        try {
            validateInvariants();
            return true;
        } catch (DomainRuleViolationException e) {
            return false;
        }
    }
    
    @Override
    public void validateInvariants() {
        if (defaultScopeType == null) {
            throw new DomainRuleViolationException(
                "userPreferences.scopeType.required",
                "Default scope type is required"
            );
        }
        
        if (defaultSharingLevel == null) {
            throw new DomainRuleViolationException(
                "userPreferences.sharingLevel.required",
                "Default sharing level is required"
            );
        }
        
        if (defaultOrganizationId == null) {
            throw new DomainRuleViolationException(
                "userPreferences.organization.required",
                "Default organization is required"
            );
        }
        
        // Validate compatibility
        if (!defaultSharingLevel.isCompatibleWith(defaultScopeType)) {
            throw new DomainRuleViolationException(
                "userPreferences.incompatible",
                "Default sharing level is not compatible with default scope type"
            );
        }
        
        // Validate application requirement
        if (defaultScopeType.requiresApplicationAccess() && defaultApplicationId == null) {
            throw new DomainRuleViolationException(
                "userPreferences.application.required",
                "Default application is required for scope type " + defaultScopeType
            );
        }
        
        // Validate team requirement
        if (defaultSharingLevel.requiresTeamMembership() && defaultTeamId == null) {
            throw new DomainRuleViolationException(
                "userPreferences.team.required",
                "Default team is required for sharing level " + defaultSharingLevel
            );
        }
    }
    
    private void markUpdatedAndPublishEvent() {
        markUpdated();
        registerEvent(new UserPreferencesUpdatedEvent(
            id.value(),
            defaultScopeType.name(),
            defaultSharingLevel.name(),
            defaultOrganizationId.value(),
            defaultApplicationId != null ? defaultApplicationId.value() : null,
            defaultTeamId != null ? defaultTeamId.value() : null
        ));
    }
    
    // Getters (no setters for immutability)
    
    public ScopeType getDefaultScopeType() {
        return defaultScopeType;
    }
    
    public SharingLevel getDefaultSharingLevel() {
        return defaultSharingLevel;
    }
    
    public OrganizationId getDefaultOrganizationId() {
        return defaultOrganizationId;
    }
    
    public Optional<ApplicationId> getDefaultApplicationId() {
        return Optional.ofNullable(defaultApplicationId);
    }
    
    public Optional<TeamId> getDefaultTeamId() {
        return Optional.ofNullable(defaultTeamId);
    }
}