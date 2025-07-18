package com.zamaz.mcp.context.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.time.Instant;
import java.util.Map;

/**
 * Domain event raised when Context metadata is updated.
 */
public class ContextMetadataUpdatedEvent extends AbstractDomainEvent {
    
    private final String contextId;
    private final Map<String, Object> metadata;
    
    public ContextMetadataUpdatedEvent(
            String contextId,
            Map<String, Object> metadata,
            Instant occurredOn
    ) {
        super(occurredOn);
        this.contextId = contextId;
        this.metadata = Map.copyOf(metadata);
    }
    
    public String getContextId() {
        return contextId;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    @Override
    public String getEventType() {
        return "context.metadata.updated";
    }
    
    @Override
    public String getAggregateId() {
        return contextId;
    }
}