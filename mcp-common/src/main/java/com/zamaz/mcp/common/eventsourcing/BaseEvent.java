package com.zamaz.mcp.common.eventsourcing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Base implementation of Event interface
 */
@Data
@Builder
public class BaseEvent implements Event {
    
    private final UUID eventId;
    private final String eventType;
    private final String aggregateId;
    private final String aggregateType;
    private final long version;
    private final LocalDateTime timestamp;
    private final String userId;
    private final String organizationId;
    private final String correlationId;
    private final Object payload;
    private final Object metadata;
    
    @JsonCreator
    public BaseEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("aggregateType") String aggregateType,
            @JsonProperty("version") long version,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("userId") String userId,
            @JsonProperty("organizationId") String organizationId,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("payload") Object payload,
            @JsonProperty("metadata") Object metadata) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.version = version;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.userId = userId;
        this.organizationId = organizationId;
        this.correlationId = correlationId;
        this.payload = payload;
        this.metadata = metadata;
    }
    
    /**
     * Create a new event builder with default values
     */
    public static BaseEventBuilder builder() {
        return new BaseEventBuilder()
            .eventId(UUID.randomUUID())
            .timestamp(LocalDateTime.now());
    }
}