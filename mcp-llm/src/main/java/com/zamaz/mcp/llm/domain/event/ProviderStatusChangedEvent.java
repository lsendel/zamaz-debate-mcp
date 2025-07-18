package com.zamaz.mcp.llm.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.time.Instant;

/**
 * Domain event raised when a provider's status changes.
 */
public class ProviderStatusChangedEvent extends AbstractDomainEvent {
    
    private final String providerId;
    private final String oldStatus;
    private final String newStatus;
    private final String message;
    
    public ProviderStatusChangedEvent(
            String providerId,
            String oldStatus,
            String newStatus,
            String message,
            Instant occurredOn
    ) {
        super(occurredOn);
        this.providerId = providerId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.message = message;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public String getOldStatus() {
        return oldStatus;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public String getMessage() {
        return message;
    }
    
    @Override
    public String getEventType() {
        return "provider.status.changed";
    }
    
    @Override
    public String getAggregateId() {
        return providerId;
    }
}