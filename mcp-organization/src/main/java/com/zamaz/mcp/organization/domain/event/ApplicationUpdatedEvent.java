package com.zamaz.mcp.organization.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.util.UUID;

/**
 * Domain event raised when an application is updated.
 */
public class ApplicationUpdatedEvent extends AbstractDomainEvent {
    
    private final UUID organizationId;
    private final String name;
    private final String description;
    
    public ApplicationUpdatedEvent(UUID applicationId, UUID organizationId,
                                  String name, String description) {
        super(applicationId.toString());
        this.organizationId = organizationId;
        this.name = name;
        this.description = description;
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
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getEventType() {
        return "application.updated";
    }
}