package com.zamaz.mcp.organization.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.util.UUID;

/**
 * Domain event raised when a new application is created within an organization.
 */
public class ApplicationCreatedEvent extends AbstractDomainEvent {
    
    private final UUID organizationId;
    private final String name;
    private final String description;
    private final UUID creatorUserId;
    
    public ApplicationCreatedEvent(UUID applicationId, UUID organizationId, 
                                  String name, String description, UUID creatorUserId) {
        super(applicationId.toString());
        this.organizationId = organizationId;
        this.name = name;
        this.description = description;
        this.creatorUserId = creatorUserId;
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
    
    public UUID getCreatorUserId() {
        return creatorUserId;
    }
    
    @Override
    public String getEventType() {
        return "application.created";
    }
}