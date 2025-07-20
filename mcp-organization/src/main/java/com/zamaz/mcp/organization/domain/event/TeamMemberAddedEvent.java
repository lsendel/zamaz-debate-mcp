package com.zamaz.mcp.organization.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.util.UUID;

/**
 * Domain event raised when a user is added to a team.
 */
public class TeamMemberAddedEvent extends AbstractDomainEvent {
    
    private final UUID organizationId;
    private final UUID applicationId; // Can be null for org-level teams
    private final UUID userId;
    private final String role;
    
    public TeamMemberAddedEvent(UUID teamId, UUID organizationId, UUID applicationId,
                               UUID userId, String role) {
        super(teamId.toString());
        this.organizationId = organizationId;
        this.applicationId = applicationId;
        this.userId = userId;
        this.role = role;
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
    
    public UUID getUserId() {
        return userId;
    }
    
    public String getRole() {
        return role;
    }
    
    public boolean isApplicationTeam() {
        return applicationId != null;
    }
    
    @Override
    public String getEventType() {
        return "team.member.added";
    }
}