package com.zamaz.mcp.context.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.time.Instant;

/**
 * Domain event raised when a new Context is created.
 */
public class ContextCreatedEvent extends AbstractDomainEvent {
    
    private final String contextId;
    private final String organizationId;
    private final String userId;
    private final String name;
    
    public ContextCreatedEvent(
            String contextId,
            String organizationId,
            String userId,
            String name,
            Instant occurredOn
    ) {
        super(occurredOn);
        this.contextId = contextId;
        this.organizationId = organizationId;
        this.userId = userId;
        this.name = name;
    }
    
    public String getContextId() {
        return contextId;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String getEventType() {
        return "context.created";
    }
    
    @Override
    public String getAggregateId() {
        return contextId;
    }
}