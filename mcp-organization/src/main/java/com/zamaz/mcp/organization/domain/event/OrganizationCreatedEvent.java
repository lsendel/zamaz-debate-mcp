package com.zamaz.mcp.organization.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.util.UUID;

/**
 * Domain event raised when a new organization is created.
 */
public class OrganizationCreatedEvent extends AbstractDomainEvent {
    
    private final String name;
    private final String description;
    private final UUID creatorUserId;
    
    public OrganizationCreatedEvent(UUID organizationId, String name, 
                                   String description, UUID creatorUserId) {
        super(organizationId.toString());
        this.name = name;
        this.description = description;
        this.creatorUserId = creatorUserId;
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
        return "organization.created";
    }
}