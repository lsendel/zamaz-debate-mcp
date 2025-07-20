package com.zamaz.mcp.organization.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.util.UUID;

/**
 * Domain event raised when an application is deactivated.
 */
public class ApplicationDeactivatedEvent extends AbstractDomainEvent {
    
    private final UUID organizationId;
    private final String name;
    
    public ApplicationDeactivatedEvent(UUID applicationId, UUID organizationId, String name) {
        super(applicationId.toString());
        this.organizationId = organizationId;
        this.name = name;
    }
    
    public UUID getApplicationId() {
        return UUID.fromString(getAggregateId());
    }
    
    public UUID getOrganizationId() {
        return organizationId;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String getEventType() {
        return "application.deactivated";
    }
}