package com.zamaz.mcp.organization.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.util.UUID;

/**
 * Domain event raised when a team is deactivated.
 */
public class TeamDeactivatedEvent extends AbstractDomainEvent {
    
    private final UUID organizationId;
    private final UUID applicationId; // Can be null for org-level teams
    private final String name;
    
    public TeamDeactivatedEvent(UUID teamId, UUID organizationId, UUID applicationId,
                               String name) {
        super(teamId.toString());
        this.organizationId = organizationId;
        this.applicationId = applicationId;
        this.name = name;
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
    
    public boolean isApplicationTeam() {
        return applicationId != null;
    }
    
    @Override
    public String getEventType() {
        return "team.deactivated";
    }
}