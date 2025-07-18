package com.zamaz.mcp.context.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.time.Instant;

/**
 * Domain event raised when a Context is deleted.
 */
public class ContextDeletedEvent extends AbstractDomainEvent {
    
    private final String contextId;
    
    public ContextDeletedEvent(String contextId, Instant occurredOn) {
        super(occurredOn);
        this.contextId = contextId;
    }
    
    public String getContextId() {
        return contextId;
    }
    
    @Override
    public String getEventType() {
        return "context.deleted";
    }
    
    @Override
    public String getAggregateId() {
        return contextId;
    }
}