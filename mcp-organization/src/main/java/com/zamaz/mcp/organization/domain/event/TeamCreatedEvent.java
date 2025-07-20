package com.zamaz.mcp.organization.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.util.UUID;

/**
 * Domain event raised when a new team is created within an organization or application.
 */
public class TeamCreatedEvent extends AbstractDomainEvent {
    
    private final UUID organizationId;
    private final UUID applicationId; // Can be null for org-level teams
    private final String name;
    private final String description;
    private final UUID creatorUserId;
    
    public TeamCreatedEvent(UUID teamId, UUID organizationId, UUID applicationId,
                           String name, String description, UUID creatorUserId) {
        super(teamId.toString());
        this.organizationId = organizationId;
        this.applicationId = applicationId;
        this.name = name;
        this.description = description;
        this.creatorUserId = creatorUserId;
    }
    
    public UUID getTeamId() {
        return UUID.fromString(getAggregateId());
    }
    
    public UUID getOrganizationId() {
        return organizationId;
    }
    
    public UUID getApplicationId() {
        return applicationId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public UUID getCreatorUserId() {
        return creatorUserId;
    }
    
    public boolean isApplicationTeam() {
        return applicationId != null;
    }
    
    @Override
    public String getEventType() {
        return "team.created";
    }
}