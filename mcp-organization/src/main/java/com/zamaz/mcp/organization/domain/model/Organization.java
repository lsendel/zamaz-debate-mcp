package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.AggregateRoot;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import com.zamaz.mcp.common.domain.model.valueobject.TenantId;
import com.zamaz.mcp.organization.domain.event.OrganizationCreatedEvent;
import com.zamaz.mcp.organization.domain.event.OrganizationUpdatedEvent;
import com.zamaz.mcp.organization.domain.event.UserAddedToOrganizationEvent;
import com.zamaz.mcp.organization.domain.event.UserRemovedFromOrganizationEvent;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Organization aggregate root representing a tenant in the system.
 * This is a pure domain object with no framework dependencies.
 */
public class Organization extends AggregateRoot<OrganizationId> {
    
    private OrganizationName name;
    private OrganizationDescription description;
    private OrganizationSettings settings;
    private boolean active;
    private final Map<UserId, OrganizationMember> members;
    
    /**
     * Creates a new organization.
     * 
     * @param id the organization ID
     * @param name the organization name
     * @param description the organization description
     * @param creatorUserId the ID of the user creating the organization
     */
    public Organization(OrganizationId id, OrganizationName name, 
                       OrganizationDescription description, UserId creatorUserId) {
        super(id);
        this.name = Objects.requireNonNull(name, "Organization name is required");
        this.description = description != null ? description : OrganizationDescription.empty();
        this.settings = OrganizationSettings.defaultSettings();
        this.active = true;
        this.members = new HashMap<>();
        
        // Add creator as owner
        addMember(creatorUserId, Role.OWNER);
        
        // Raise domain event
        registerEvent(new OrganizationCreatedEvent(
            id.toTenantId(),
            name.value(),
            description.value(),
            creatorUserId.value()
        ));
    }
    
    /**
     * Reconstructs an organization from persistence.
     * Used by repository when loading from database.
     */
    public Organization(OrganizationId id, OrganizationName name,
                       OrganizationDescription description, OrganizationSettings settings,
                       boolean active, Map<UserId, OrganizationMember> members,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.settings = Objects.requireNonNull(settings);
        this.active = active;
        this.members = new HashMap<>(members);
    }
    
    /**
     * Updates the organization details.
     */
    public void update(OrganizationName newName, OrganizationDescription newDescription) {
        if (!active) {
            throw new DomainRuleViolationException(
                "organization.inactive",
                "Cannot update inactive organization"
            );
        }
        
        boolean changed = false;
        
        if (!this.name.equals(newName)) {
            this.name = Objects.requireNonNull(newName);
            changed = true;
        }
        
        if (!this.description.equals(newDescription)) {
            this.description = newDescription != null ? newDescription : OrganizationDescription.empty();
            changed = true;
        }
        
        if (changed) {
            markUpdated();
            registerEvent(new OrganizationUpdatedEvent(
                id.toTenantId(),
                name.value(),
                description.value()
            ));
        }
    }
    
    /**
     * Updates organization settings.
     */
    public void updateSettings(OrganizationSettings newSettings) {
        if (!active) {
            throw new DomainRuleViolationException(
                "organization.inactive",
                "Cannot update settings for inactive organization"
            );
        }
        
        this.settings = Objects.requireNonNull(newSettings);
        markUpdated();
    }
    
    /**
     * Adds a user to the organization.
     */
    public void addUser(UserId userId, Role role) {
        if (!active) {
            throw new DomainRuleViolationException(
                "organization.inactive",
                "Cannot add users to inactive organization"
            );
        }
        
        if (members.containsKey(userId)) {
            throw new DomainRuleViolationException(
                "organization.user.alreadyMember",
                "User is already a member of this organization"
            );
        }
        
        validateMemberLimits();
        addMember(userId, role);
        
        registerEvent(new UserAddedToOrganizationEvent(
            id.toTenantId(),
            userId.value(),
            role.name()
        ));
    }
    
    /**
     * Updates a user's role in the organization.
     */
    public void updateUserRole(UserId userId, Role newRole) {
        if (!active) {
            throw new DomainRuleViolationException(
                "organization.inactive",
                "Cannot update user roles in inactive organization"
            );
        }
        
        OrganizationMember member = members.get(userId);
        if (member == null) {
            throw new DomainRuleViolationException(
                "organization.user.notMember",
                "User is not a member of this organization"
            );
        }
        
        // Ensure at least one owner remains
        if (member.getRole() == Role.OWNER && newRole != Role.OWNER) {
            long ownerCount = members.values().stream()
                .filter(m -> m.getRole() == Role.OWNER)
                .count();
            if (ownerCount <= 1) {
                throw new DomainRuleViolationException(
                    "organization.owner.lastOwner",
                    "Cannot remove the last owner from organization"
                );
            }
        }
        
        member.changeRole(newRole);
        markUpdated();
    }
    
    /**
     * Removes a user from the organization.
     */
    public void removeUser(UserId userId) {
        if (!active) {
            throw new DomainRuleViolationException(
                "organization.inactive",
                "Cannot remove users from inactive organization"
            );
        }
        
        OrganizationMember member = members.get(userId);
        if (member == null) {
            throw new DomainRuleViolationException(
                "organization.user.notMember",
                "User is not a member of this organization"
            );
        }
        
        // Ensure at least one owner remains
        if (member.getRole() == Role.OWNER) {
            long ownerCount = members.values().stream()
                .filter(m -> m.getRole() == Role.OWNER)
                .count();
            if (ownerCount <= 1) {
                throw new DomainRuleViolationException(
                    "organization.owner.lastOwner",
                    "Cannot remove the last owner from organization"
                );
            }
        }
        
        members.remove(userId);
        markUpdated();
        
        registerEvent(new UserRemovedFromOrganizationEvent(
            id.toTenantId(),
            userId.value()
        ));
    }
    
    /**
     * Deactivates the organization.
     */
    public void deactivate() {
        if (!active) {
            return; // Already inactive
        }
        
        this.active = false;
        markUpdated();
    }
    
    /**
     * Reactivates the organization.
     */
    public void reactivate() {
        if (active) {
            return; // Already active
        }
        
        this.active = true;
        markUpdated();
    }
    
    /**
     * Checks if a user is a member of the organization.
     */
    public boolean isMember(UserId userId) {
        return members.containsKey(userId);
    }
    
    /**
     * Gets a user's role in the organization.
     */
    public Optional<Role> getUserRole(UserId userId) {
        OrganizationMember member = members.get(userId);
        return member != null ? Optional.of(member.getRole()) : Optional.empty();
    }
    
    /**
     * Checks if a user has a specific role or higher.
     */
    public boolean hasRole(UserId userId, Role minimumRole) {
        return getUserRole(userId)
            .map(role -> role.hasPermission(minimumRole))
            .orElse(false);
    }
    
    @Override
    public void validateInvariants() {
        if (name == null || name.value().isEmpty()) {
            throw new DomainRuleViolationException(
                "organization.name.required",
                "Organization must have a name"
            );
        }
        
        // Ensure at least one owner
        boolean hasOwner = members.values().stream()
            .anyMatch(m -> m.getRole() == Role.OWNER);
        if (!hasOwner && !members.isEmpty()) {
            throw new DomainRuleViolationException(
                "organization.owner.required",
                "Organization must have at least one owner"
            );
        }
    }
    
    // Private helper methods
    
    private void addMember(UserId userId, Role role) {
        var member = new OrganizationMember(userId, role, LocalDateTime.now());
        members.put(userId, member);
        markUpdated();
    }
    
    private void validateMemberLimits() {
        Integer maxMembers = settings.getMaxMembers();
        if (maxMembers != null && members.size() >= maxMembers) {
            throw new DomainRuleViolationException(
                "organization.members.limitExceeded",
                "Organization has reached maximum member limit: " + maxMembers
            );
        }
    }
    
    // Getters (no setters for immutability)
    
    public OrganizationName getName() {
        return name;
    }
    
    public OrganizationDescription getDescription() {
        return description;
    }
    
    public OrganizationSettings getSettings() {
        return settings;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public Set<OrganizationMember> getMembers() {
        return new HashSet<>(members.values());
    }
    
    public int getMemberCount() {
        return members.size();
    }
}