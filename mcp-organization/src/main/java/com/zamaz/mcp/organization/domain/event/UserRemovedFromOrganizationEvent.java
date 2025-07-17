package com.zamaz.mcp.organization.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.util.UUID;

/**
 * Domain event raised when a user is removed from an organization.
 */
public class UserRemovedFromOrganizationEvent extends AbstractDomainEvent {
    
    private final UUID userId;
    
    public UserRemovedFromOrganizationEvent(UUID organizationId, UUID userId) {
        super(organizationId.toString());
        this.userId = userId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    @Override
    public String getEventType() {
        return "organization.user.removed";
    }
}