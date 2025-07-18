package com.zamaz.mcp.context.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.time.Instant;

/**
 * Domain event raised when a Context is archived.
 */
public class ContextArchivedEvent extends AbstractDomainEvent {
    
    private final String contextId;
    
    public ContextArchivedEvent(String contextId, Instant occurredOn) {
        super(occurredOn);
        this.contextId = contextId;
    }
    
    public String getContextId() {
        return contextId;
    }
    
    @Override
    public String getEventType() {
        return "context.archived";
    }
    
    @Override
    public String getAggregateId() {
        return contextId;
    }
}