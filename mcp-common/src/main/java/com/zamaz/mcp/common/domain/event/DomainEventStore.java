package com.zamaz.mcp.common.domain.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain service for storing and retrieving domain events.
 * This interface enables event sourcing patterns and audit logging.
 * Implementation belongs in the infrastructure layer.
 */
public interface DomainEventStore {
    
    /**
     * Stores a domain event.
     * 
     * @param event the domain event to store
     */
    void store(DomainEvent event);
    
    /**
     * Retrieves all events for a specific aggregate.
     * 
     * @param aggregateId the ID of the aggregate
     * @return list of domain events for the aggregate
     */
    List<DomainEvent> findByAggregateId(String aggregateId);
    
    /**
     * Retrieves events for an aggregate within a time range.
     * 
     * @param aggregateId the ID of the aggregate
     * @param from start of time range (inclusive)
     * @param to end of time range (inclusive)
     * @return list of domain events within the time range
     */
    List<DomainEvent> findByAggregateIdAndTimeRange(
        String aggregateId, 
        LocalDateTime from, 
        LocalDateTime to
    );
    
    /**
     * Retrieves all events of a specific type.
     * 
     * @param eventType the fully qualified class name of the event type
     * @return list of domain events of the specified type
     */
    List<DomainEvent> findByEventType(String eventType);
    
    /**
     * Retrieves events after a specific event ID (for event replay).
     * 
     * @param eventId the ID of the event to start after
     * @param limit maximum number of events to retrieve
     * @return list of domain events after the specified event
     */
    List<DomainEvent> findAfterEventId(UUID eventId, int limit);
}