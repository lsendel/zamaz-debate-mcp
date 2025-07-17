package com.zamaz.mcp.organization.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.util.UUID;

/**
 * Domain event raised when an organization's details are updated.
 */
public class OrganizationUpdatedEvent extends AbstractDomainEvent {
    
    private final String name;
    private final String description;
    
    public OrganizationUpdatedEvent(UUID organizationId, String name, String description) {
        super(organizationId.toString());
        this.name = name;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getEventType() {
        return "organization.updated";
    }
}