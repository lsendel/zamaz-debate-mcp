package com.zamaz.mcp.debateengine.domain.port;

import com.zamaz.mcp.common.domain.DomainEvent;

/**
 * Port for publishing domain events.
 */
public interface DomainEventPublisher {
    
    /**
     * Publish a domain event.
     */
    void publish(DomainEvent event);
    
    /**
     * Publish multiple domain events.
     */
    void publishAll(Iterable<DomainEvent> events);
}