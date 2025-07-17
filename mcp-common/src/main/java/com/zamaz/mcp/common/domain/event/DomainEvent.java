package com.zamaz.mcp.common.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events in hexagonal architecture.
 * Domain events represent something that happened in the domain.
 * This is a pure domain interface with no framework dependencies.
 */
public interface DomainEvent {
    
    /**
     * Gets the unique identifier of this event.
     * 
     * @return the event ID
     */
    UUID getEventId();
    
    /**
     * Gets when this event occurred.
     * 
     * @return the event timestamp
     */
    Instant getOccurredAt();
    
    /**
     * Gets the ID of the aggregate that raised this event.
     * 
     * @return the aggregate ID
     */
    String getAggregateId();
    
    /**
     * Gets the type of the aggregate that raised this event.
     * 
     * @return the aggregate type
     */
    String getAggregateType();
    
    /**
     * Gets the event type name.
     * 
     * @return the event type
     */
    String getEventType();
}