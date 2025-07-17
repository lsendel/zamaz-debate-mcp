package com.zamaz.mcp.common.domain.event;

import java.util.List;

/**
 * Domain service for publishing domain events.
 * This is a domain service interface that should be implemented by the infrastructure layer.
 * The domain uses this interface to publish events without knowing about the infrastructure.
 */
public interface DomainEventPublisher {
    
    /**
     * Publishes a single domain event.
     * 
     * @param event the domain event to publish
     */
    void publish(DomainEvent event);
    
    /**
     * Publishes multiple domain events in order.
     * 
     * @param events the domain events to publish
     */
    void publishAll(List<DomainEvent> events);
}