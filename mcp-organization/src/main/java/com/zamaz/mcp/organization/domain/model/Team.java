package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.AggregateRoot;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import com.zamaz.mcp.common.domain.model.valueobject.ApplicationId;
import com.zamaz.mcp.common.domain.model.valueobject.TeamId;
import com.zamaz.mcp.organization.domain.event.TeamCreatedEvent;
import com.zamaz.mcp.organization.domain.event.TeamUpdatedEvent;
import com.zamaz.mcp.organization.domain.event.TeamMemberAddedEvent;
import com.zamaz.mcp.organization.domain.event.TeamMemberRemovedEvent;
import com.zamaz.mcp.organization.domain.event.TeamDeactivatedEvent;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Team aggregate root representing a group of users that can collaborate on debates.
 * Teams can belong to an application or directly to an organization.
 * This is a pure domain object with no framework dependencies.
 */
public class Team extends AggregateRoot<TeamId> {
    
    private TeamName name;
    private TeamDescription description;
    private OrganizationId organizationId;
    private ApplicationId applicationId; // Optional - can be null for org-level teams
    private boolean active;
    private Integer maxMembers;
    private final Map<UserId, TeamMember> members;
    
    /**
     * Creates a new team within an application.
     * 
     * @param id the team ID
     * @param name the team name
     * @param description the team description
     * @param organizationId the organization this team belongs to
     * @param applicationId the application this team belongs to (optional)
     * @param creatorUserId the ID of the user creating the team
     */
    public Team(TeamId id, TeamName name, TeamDescription description,
               OrganizationId organizationId, ApplicationId applicationId,
               UserId creatorUserId) {
        super(id);
        this.name = Objects.requireNonNull(name, "Team name is required");
        this.description = description != null ? description : TeamDescription.empty();
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID is required");
        this.applicationId = applicationId; // Can be null for org-level teams
        this.active = true;
        this.maxMembers = null; // No limit by default
        this.members = new HashMap<>();
        
        // Add creator as admin
        addMember(creatorUserId, TeamRole.ADMIN);
        
        // Raise domain event
        registerEvent(new TeamCreatedEvent(
            id.value(),
            organizationId.value(),
            applicationId != null ? applicationId.value() : null,
            name.value(),
            description.value(),
            creatorUserId.value()
        ));
    }
    
    /**
     * Reconstructs a team from persistence.
     * Used by repository when loading from database.
     */
    public Team(TeamId id, TeamName name, TeamDescription description,
               OrganizationId organizationId, ApplicationId applicationId,
               boolean active, Integer maxMembers, Map<UserId, TeamMember> members,
               LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.applicationId = applicationId;
        this.active = active;
        this.maxMembers = maxMembers;
        this.members = new HashMap<>(members);
    }
    
    /**
     * Updates the team details.
     */
    public void update(TeamName newName, TeamDescription newDescription) {
        if (!active) {
            throw new DomainRuleViolationException(
                "team.inactive",
                "Cannot update inactive team"
            );
        }
        
        boolean changed = false;
        
        if (!this.name.equals(newName)) {
            this.name = Objects.requireNonNull(newName);
            changed = true;
        }
        
        if (!this.description.equals(newDescription)) {
            this.description = newDescription != null ? newDescription : TeamDescription.empty();
            changed = true;
        }
        
        if (changed) {
            markUpdated();
            registerEvent(new TeamUpdatedEvent(
                id.value(),
                organizationId.value(),
                applicationId != null ? applicationId.value() : null,
                name.value(),
                description.value()
            ));
        }
    }
    
    /**
     * Sets the maximum number of members for this team.
     */
    public void setMaxMembers(Integer maxMembers) {
        if (!active) {
            throw new DomainRuleViolationException(
                "team.inactive",
                "Cannot update inactive team"
            );
        }
        
        if (maxMembers != null && maxMembers < 1) {
            throw new DomainRuleViolationException(
                "team.maxMembers.invalid",
                "Max members must be at least 1"
            );
        }
        
        if (maxMembers != null && members.size() > maxMembers) {
            throw new DomainRuleViolationException(
                "team.maxMembers.tooLow",
                "Cannot set max members below current member count: " + members.size()
            );
        }
        
        this.maxMembers = maxMembers;
        markUpdated();
    }
    
    /**
     * Adds a user to the team.
     */
    public void addUser(UserId userId, TeamRole role) {
        if (!active) {
            throw new DomainRuleViolationException(
                "team.inactive",
                "Cannot add users to inactive team"
            );
        }
        
        if (members.containsKey(userId)) {
            throw new DomainRuleViolationException(
                "team.user.alreadyMember",
                "User is already a member of this team"
            );
        }
        
        validateMemberLimits();
        addMember(userId, role);
        
        registerEvent(new TeamMemberAddedEvent(
            id.value(),
            organizationId.value(),
            applicationId != null ? applicationId.value() : null,
            userId.value(),
            role.name()
        ));
    }
    
    /**
     * Updates a user's role in the team.
     */
    public void updateUserRole(UserId userId, TeamRole newRole) {
        if (!active) {
            throw new DomainRuleViolationException(
                "team.inactive",
                "Cannot update user roles in inactive team"
            );
        }
        
        TeamMember member = members.get(userId);
        if (member == null) {
            throw new DomainRuleViolationException(
                "team.user.notMember",
                "User is not a member of this team"
            );
        }
        
        // Ensure at least one admin remains
        if (member.getRole() == TeamRole.ADMIN && newRole != TeamRole.ADMIN) {
            long adminCount = members.values().stream()
                .filter(m -> m.getRole() == TeamRole.ADMIN && m.isActive())
                .count();
            if (adminCount <= 1) {
                throw new DomainRuleViolationException(
                    "team.admin.lastAdmin",
                    "Cannot remove the last admin from team"
                );
            }
        }
        
        member.changeRole(newRole);
        markUpdated();
    }
    
    /**
     * Removes a user from the team.
     */
    public void removeUser(UserId userId) {
        if (!active) {
            throw new DomainRuleViolationException(
                "team.inactive",
                "Cannot remove users from inactive team"
            );
        }
        
        TeamMember member = members.get(userId);
        if (member == null) {
            throw new DomainRuleViolationException(
                "team.user.notMember",
                "User is not a member of this team"
            );
        }
        
        // Ensure at least one admin remains
        if (member.getRole() == TeamRole.ADMIN) {
            long adminCount = members.values().stream()
                .filter(m -> m.getRole() == TeamRole.ADMIN && m.isActive())
                .count();
            if (adminCount <= 1) {
                throw new DomainRuleViolationException(
                    "team.admin.lastAdmin",
                    "Cannot remove the last admin from team"
                );
            }
        }
        
        members.remove(userId);
        markUpdated();
        
        registerEvent(new TeamMemberRemovedEvent(
            id.value(),
            organizationId.value(),
            applicationId != null ? applicationId.value() : null,
            userId.value()
        ));
    }
    
    /**
     * Deactivates the team.
     */
    public void deactivate() {
        if (!active) {
            return; // Already inactive
        }
        
        this.active = false;
        markUpdated();
        
        registerEvent(new TeamDeactivatedEvent(
            id.value(),
            organizationId.value(),
            applicationId != null ? applicationId.value() : null,
            name.value()
        ));
    }
    
    /**
     * Reactivates the team.
     */
    public void reactivate() {
        if (active) {
            return; // Already active
        }
        
        this.active = true;
        markUpdated();
    }
    
    /**
     * Checks if a user is a member of the team.
     */
    public boolean isMember(UserId userId) {
        TeamMember member = members.get(userId);
        return member != null && member.isActive();
    }
    
    /**
     * Gets a user's role in the team.
     */
    public Optional<TeamRole> getUserRole(UserId userId) {
        TeamMember member = members.get(userId);
        return (member != null && member.isActive()) ? 
            Optional.of(member.getRole()) : Optional.empty();
    }
    
    /**
     * Checks if a user has a specific role or higher.
     */
    public boolean hasRole(UserId userId, TeamRole minimumRole) {
        return getUserRole(userId)
            .map(role -> role.hasPermission(minimumRole))
            .orElse(false);
    }
    
    /**
     * Checks if this team belongs to an application.
     */
    public boolean belongsToApplication() {
        return applicationId != null;
    }
    
    /**
     * Checks if this team can accommodate another member.
     */
    public boolean canAddMember() {
        if (!active) {
            return false;
        }
        
        if (maxMembers == null) {
            return true; // No limit
        }
        
        return members.size() < maxMembers;
    }
    
    @Override
    public void validateInvariants() {
        if (name == null || name.value().isEmpty()) {
            throw new DomainRuleViolationException(
                "team.name.required",
                "Team must have a name"
            );
        }
        
        if (organizationId == null) {
            throw new DomainRuleViolationException(
                "team.organization.required",
                "Team must belong to an organization"
            );
        }
        
        // Ensure at least one admin
        boolean hasAdmin = members.values().stream()
            .anyMatch(m -> m.getRole() == TeamRole.ADMIN && m.isActive());
        if (!hasAdmin && !members.isEmpty()) {
            throw new DomainRuleViolationException(
                "team.admin.required",
                "Team must have at least one admin"
            );
        }
        
        if (maxMembers != null && members.size() > maxMembers) {
            throw new DomainRuleViolationException(
                "team.members.limitExceeded",
                "Team has exceeded maximum member limit: " + maxMembers
            );
        }
    }
    
    // Private helper methods
    
    private void addMember(UserId userId, TeamRole role) {
        var member = new TeamMember(userId, role, LocalDateTime.now());
        members.put(userId, member);
        markUpdated();
    }
    
    private void validateMemberLimits() {
        if (maxMembers != null && members.size() >= maxMembers) {
            throw new DomainRuleViolationException(
                "team.members.limitExceeded",
                "Team has reached maximum member limit: " + maxMembers
            );
        }
    }
    
    // Getters (no setters for immutability)
    
    public TeamName getName() {
        return name;
    }
    
    public TeamDescription getDescription() {
        return description;
    }
    
    public OrganizationId getOrganizationId() {
        return organizationId;
    }
    
    public Optional<ApplicationId> getApplicationId() {
        return Optional.ofNullable(applicationId);
    }
    
    public boolean isActive() {
        return active;
    }
    
    public Optional<Integer> getMaxMembers() {
        return Optional.ofNullable(maxMembers);
    }
    
    public Set<TeamMember> getMembers() {
        return new HashSet<>(members.values());
    }
    
    public int getMemberCount() {
        return (int) members.values().stream().filter(TeamMember::isActive).count();
    }
    
    public int getActiveMemberCount() {
        return getMemberCount();
    }
}