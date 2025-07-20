package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.AggregateRoot;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import com.zamaz.mcp.common.domain.model.valueobject.ApplicationId;
import com.zamaz.mcp.common.domain.model.valueobject.TeamId;
import com.zamaz.mcp.organization.domain.event.ApplicationCreatedEvent;
import com.zamaz.mcp.organization.domain.event.ApplicationUpdatedEvent;
import com.zamaz.mcp.organization.domain.event.ApplicationDeactivatedEvent;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Application aggregate root representing a logical grouping within an organization.
 * Applications can contain teams and are used to scope debates and access control.
 * This is a pure domain object with no framework dependencies.
 */
public class Application extends AggregateRoot<ApplicationId> {
    
    private ApplicationName name;
    private ApplicationDescription description;
    private OrganizationId organizationId;
    private ApplicationSettings settings;
    private boolean active;
    private final Set<TeamId> teams;
    
    /**
     * Creates a new application.
     * 
     * @param id the application ID
     * @param name the application name
     * @param description the application description
     * @param organizationId the organization this application belongs to
     * @param creatorUserId the ID of the user creating the application
     */
    public Application(ApplicationId id, ApplicationName name, 
                      ApplicationDescription description, OrganizationId organizationId,
                      UserId creatorUserId) {
        super(id);
        this.name = Objects.requireNonNull(name, "Application name is required");
        this.description = description != null ? description : ApplicationDescription.empty();
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID is required");
        this.settings = ApplicationSettings.defaultSettings();
        this.active = true;
        this.teams = new HashSet<>();
        
        // Raise domain event
        registerEvent(new ApplicationCreatedEvent(
            id.value(),
            organizationId.value(),
            name.value(),
            description.value(),
            creatorUserId.value()
        ));
    }
    
    /**
     * Reconstructs an application from persistence.
     * Used by repository when loading from database.
     */
    public Application(ApplicationId id, ApplicationName name,
                      ApplicationDescription description, OrganizationId organizationId,
                      ApplicationSettings settings, boolean active, Set<TeamId> teams,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.settings = Objects.requireNonNull(settings);
        this.active = active;
        this.teams = new HashSet<>(teams);
    }
    
    /**
     * Updates the application details.
     */
    public void update(ApplicationName newName, ApplicationDescription newDescription) {
        if (!active) {
            throw new DomainRuleViolationException(
                "application.inactive",
                "Cannot update inactive application"
            );
        }
        
        boolean changed = false;
        
        if (!this.name.equals(newName)) {
            this.name = Objects.requireNonNull(newName);
            changed = true;
        }
        
        if (!this.description.equals(newDescription)) {
            this.description = newDescription != null ? newDescription : ApplicationDescription.empty();
            changed = true;
        }
        
        if (changed) {
            markUpdated();
            registerEvent(new ApplicationUpdatedEvent(
                id.value(),
                organizationId.value(),
                name.value(),
                description.value()
            ));
        }
    }
    
    /**
     * Updates application settings.
     */
    public void updateSettings(ApplicationSettings newSettings) {
        if (!active) {
            throw new DomainRuleViolationException(
                "application.inactive",
                "Cannot update settings for inactive application"
            );
        }
        
        // Validate new settings against current state
        validateSettingsChange(newSettings);
        
        this.settings = Objects.requireNonNull(newSettings);
        markUpdated();
    }
    
    /**
     * Adds a team to this application.
     */
    public void addTeam(TeamId teamId) {
        if (!active) {
            throw new DomainRuleViolationException(
                "application.inactive",
                "Cannot add teams to inactive application"
            );
        }
        
        if (teams.contains(teamId)) {
            throw new DomainRuleViolationException(
                "application.team.alreadyExists",
                "Team is already part of this application"
            );
        }
        
        validateTeamLimits();
        teams.add(teamId);
        markUpdated();
    }
    
    /**
     * Removes a team from this application.
     */
    public void removeTeam(TeamId teamId) {
        if (!active) {
            throw new DomainRuleViolationException(
                "application.inactive",
                "Cannot remove teams from inactive application"
            );
        }
        
        if (!teams.contains(teamId)) {
            throw new DomainRuleViolationException(
                "application.team.notFound",
                "Team is not part of this application"
            );
        }
        
        teams.remove(teamId);
        markUpdated();
    }
    
    /**
     * Deactivates the application.
     */
    public void deactivate() {
        if (!active) {
            return; // Already inactive
        }
        
        this.active = false;
        markUpdated();
        
        registerEvent(new ApplicationDeactivatedEvent(
            id.value(),
            organizationId.value(),
            name.value()
        ));
    }
    
    /**
     * Reactivates the application.
     */
    public void reactivate() {
        if (active) {
            return; // Already active
        }
        
        this.active = true;
        markUpdated();
    }
    
    /**
     * Checks if a team belongs to this application.
     */
    public boolean hasTeam(TeamId teamId) {
        return teams.contains(teamId);
    }
    
    /**
     * Checks if this application can accommodate another team.
     */
    public boolean canAddTeam() {
        if (!active) {
            return false;
        }
        
        if (!settings.hasTeamLimit()) {
            return true; // No limit
        }
        
        return teams.size() < settings.getMaxTeams();
    }
    
    /**
     * Gets the organization this application belongs to.
     */
    public OrganizationId getOrganizationId() {
        return organizationId;
    }
    
    @Override
    public void validateInvariants() {
        if (name == null || name.value().isEmpty()) {
            throw new DomainRuleViolationException(
                "application.name.required",
                "Application must have a name"
            );
        }
        
        if (organizationId == null) {
            throw new DomainRuleViolationException(
                "application.organization.required",
                "Application must belong to an organization"
            );
        }
        
        if (settings.hasTeamLimit() && teams.size() > settings.getMaxTeams()) {
            throw new DomainRuleViolationException(
                "application.teams.limitExceeded",
                "Application has exceeded maximum team limit: " + settings.getMaxTeams()
            );
        }
    }
    
    // Private helper methods
    
    private void validateTeamLimits() {
        if (settings.hasTeamLimit() && teams.size() >= settings.getMaxTeams()) {
            throw new DomainRuleViolationException(
                "application.teams.limitExceeded",
                "Application has reached maximum team limit: " + settings.getMaxTeams()
            );
        }
    }
    
    private void validateSettingsChange(ApplicationSettings newSettings) {
        // Check if new team limit would violate current state
        if (newSettings.hasTeamLimit() && teams.size() > newSettings.getMaxTeams()) {
            throw new DomainRuleViolationException(
                "application.settings.teamLimitTooLow",
                "Cannot set team limit below current team count: " + teams.size()
            );
        }
    }
    
    // Getters (no setters for immutability)
    
    public ApplicationName getName() {
        return name;
    }
    
    public ApplicationDescription getDescription() {
        return description;
    }
    
    public ApplicationSettings getSettings() {
        return settings;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public Set<TeamId> getTeams() {
        return new HashSet<>(teams);
    }
    
    public int getTeamCount() {
        return teams.size();
    }
}