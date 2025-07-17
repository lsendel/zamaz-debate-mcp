package com.zamaz.mcp.common.eventsourcing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base interface for all domain events in the event sourcing system
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrganizationEvent.class, name = "organization"),
    @JsonSubTypes.Type(value = UserEvent.class, name = "user"),
    @JsonSubTypes.Type(value = DebateEvent.class, name = "debate"),
    @JsonSubTypes.Type(value = ArgumentEvent.class, name = "argument"),
    @JsonSubTypes.Type(value = VoteEvent.class, name = "vote")
})
public interface Event {
    
    /**
     * Unique identifier for the event
     */
    UUID getEventId();
    
    /**
     * Type of the event
     */
    String getEventType();
    
    /**
     * ID of the aggregate root this event belongs to
     */
    String getAggregateId();
    
    /**
     * Type of the aggregate root
     */
    String getAggregateType();
    
    /**
     * Version of the aggregate after this event
     */
    long getVersion();
    
    /**
     * Timestamp when the event occurred
     */
    LocalDateTime getTimestamp();
    
    /**
     * User who triggered the event
     */
    String getUserId();
    
    /**
     * Organization context
     */
    String getOrganizationId();
    
    /**
     * Correlation ID for tracing related events
     */
    String getCorrelationId();
    
    /**
     * Event payload containing the actual data
     */
    Object getPayload();
    
    /**
     * Additional metadata
     */
    Object getMetadata();
}